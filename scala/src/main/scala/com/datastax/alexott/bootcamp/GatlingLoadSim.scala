package com.datastax.alexott.bootcamp

import java.net.InetAddress

import java.nio.ByteBuffer
import java.util.{UUID,Date}
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ThreadLocalRandom, TimeUnit}
import scala.concurrent.duration._

import com.datastax.driver.core.ConsistencyLevel._
import com.datastax.driver.core.policies.{ConstantSpeculativeExecutionPolicy, DCAwareRoundRobinPolicy, TokenAwarePolicy, LatencyAwarePolicy}
import com.datastax.driver.core.{Session => _, _}
import com.datastax.driver.core.utils.UUIDs
import com.datastax.driver.core.LocalDate
import com.datastax.gatling.plugin.DsePredef._
import com.datastax.driver.dse.{DseSession, DseCluster}


import io.gatling.core.Predef._ // 2
import io.gatling.http.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.core.structure.ChainBuilder

import scala.concurrent.duration.{Duration, FiniteDuration}

class GatlingLoadSim extends Simulation
{
  
  val itemFormat = "f5c03dd1-2e78-11e8-8d2e-%012d";
	val userFormat = "6a23b0f0-2e77-11e8-8d2e-%012d";
	val shopFormat = "f2a6cb00-2734-11e8-ad05-%012d";
	
	val countries = 10; // for web shops
	val shopCount = 500 + countries;
	
	val usersCount = 10000000;
	val itemsCount = 1000000
	
	val mainPageItems = 20
	
	def getUUID(format: String,  value: Int): UUID = {
	   UUID.fromString(format.format(value))
	}
  
  val clusterBuilder = new DseCluster.Builder
  
  clusterBuilder.
    withSocketOptions(new SocketOptions().
      setKeepAlive(true).
      setTcpNoDelay(true)).
    withQueryOptions(new QueryOptions().
      setDefaultIdempotence(true).
      setPrepareOnAllHosts(true).
      setReprepareOnUp(true).
      setConsistencyLevel(LOCAL_ONE)).
    withPoolingOptions(new PoolingOptions().
      setCoreConnectionsPerHost(HostDistance.LOCAL, 1).
      setMaxConnectionsPerHost(HostDistance.LOCAL, 2).
      setNewConnectionThreshold(HostDistance.LOCAL, 3000).
      setMaxRequestsPerConnection(HostDistance.LOCAL, 10000))

    
  System.getProperty("workload.contact-points", "127.0.0.1").split(",").foreach(clusterBuilder.addContactPoint)
  val cluster = clusterBuilder.build()
  val dseSession = cluster.connect()
  
  val cqlConfig = cql.session(dseSession)
  
  def rand: ThreadLocalRandom =
  {
    ThreadLocalRandom.current()
  }

  val random = new util.Random

  val feeder = Iterator.continually( 
      // this feader will "feed" random data into our Sessions
      Map(
          "user_id" -> getUUID(userFormat, random.nextInt(usersCount)),
          "mpage_item_start" -> random.nextInt(itemsCount)
          ))
    
  val insertCL = ConsistencyLevel.LOCAL_QUORUM
  val readCL = ConsistencyLevel.LOCAL_QUORUM

  val getItemsCountPrepared = dseSession.prepare("select items_count, updated from atwaters_usa.cart where user_id = ? limit 1;")
  val getMainPageItemPrepared = dseSession.prepare("select title, price, urls, rating, rating_count from atwaters_inventory.inventory where sku = ? AND country = 'US';")
  
  def getItem(item: Int): ChainBuilder = {
    group("getMainPageItem")(
        exec(cql("getMainPageItem")
          .executePrepared(getMainPageItemPrepared)
          .withParams(getUUID(itemFormat, item))
          .consistencyLevel(ConsistencyLevel.LOCAL_ONE))
     )
  }
  
  def doLoadMainPage(): ChainBuilder = {
      group("LoadMainPage")(
         exec(cql("getItemsCount")
           .executePrepared(getItemsCountPrepared)
           .withParams(List("user_id"))
           .consistencyLevel(ConsistencyLevel.LOCAL_ONE))
           .repeat(mainPageItems)(getItem(random.nextInt(itemsCount)))
       )
    }
  
  val scnNotBuyingUser = scenario("NotBuyingUser").repeat(1)
  {
    feed(feeder)
      .group("NotBuyingUser")(
          exec(doLoadMainPage())
      )
  }
  
  val rampUpTime = FiniteDuration(java.lang.Long.getLong("rampUpTime", 1), TimeUnit.MINUTES)
  val testDuration = FiniteDuration(java.lang.Long.getLong("testDuration", 5), TimeUnit.MINUTES)
  val concurrentSessionCount: Int = Integer.getInteger("concurrentSessionCount", 100)
  val usersPerSecond = concurrentSessionCount.asInstanceOf[Double]
  val rampUpPerSec = concurrentSessionCount.asInstanceOf[Double] / 10

  
  setUp(scnNotBuyingUser.inject(
    constantUsersPerSec(usersPerSecond) during testDuration
  )
  ).protocols(cqlConfig)

  after(cluster.close())

}