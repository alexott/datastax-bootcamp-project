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

import com.github.javafaker.Faker

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
	val numSuggestions = 3
	val itemsPerSearch = 10
	val numSearches = 10
	
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
      setMaxRequestsPerConnection(HostDistance.LOCAL, 30000))

    
  System.getProperty("workload.contact-points", "127.0.0.1").split(",").foreach(clusterBuilder.addContactPoint)
  val cluster = clusterBuilder.build()
  val dseSession = cluster.connect()
  
  val cqlConfig = cql.session(dseSession)
  
  def rand: ThreadLocalRandom =
  {
    ThreadLocalRandom.current()
  }

  val random = new util.Random

  val faker = new Faker()
  
  def getProductName(): String = {
    faker.commerce().productName().split(' ')(1)
  }
  
  def getProductNamePart(): String = {
    val s = getProductName()
    s.substring(0, Math.min(4, s.length()))
  }

  val feeder = Iterator.continually( 
      // this feader will "feed" random data into our Sessions
      Map(
          "user_id" -> getUUID(userFormat, rand.nextInt(usersCount)),
          "mpage_item_start" -> random.nextInt(itemsCount),
          "item_id" -> getUUID(itemFormat, random.nextInt(itemsCount)),
          "shop_id" -> getUUID(shopFormat, rand.nextInt(usersCount)),
          "prod_name_part" -> getProductNamePart(),
          "prod_name_query" -> ("{\"q\":\"title:\\\"" + getProductName() + "\\\"\",\"fq\":\"country:US\"}"),
          "country" -> "US"
          ))
    
  val insertCL = ConsistencyLevel.LOCAL_QUORUM
  val readCL = ConsistencyLevel.LOCAL_QUORUM

  val getItemsCountPrepared = dseSession.prepare("select items_count, updated from atwaters_usa.cart where user_id = ? limit 1;")
  val getItemPrepared = dseSession.prepare("select title, price, urls, rating, rating_count from atwaters_inventory.inventory where sku = ? limit 1;")
  val getSearchItemPrepared = dseSession.prepare("select title, price, urls, rating, rating_count from atwaters_inventory.inventory where solr_query = ? limit 10")
  val getCommentsPrepared = dseSession.prepare("select * from atwaters_inventory.comments where base_sku = ? limit 100")
  val getItemAvailabilityPrepared = dseSession.prepare("select * from atwaters_inventory.inv_counters where sku = ? and shop = ?")
  val addItemToBusketPrepared = dseSession.prepare("insert into atwaters_usa.cart(user_id, sku, updated, title, currency, price) " 
      + " values(?, ?, toTimestamp(now()), ?, 'USD', ?) if not exists")
  
  val httpConf = http
    .baseURL("http://127.0.0.1:8983")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0")
    .maxConnectionsPerHost(500)
  
  def getItem(item: Int): ChainBuilder = {
        exec(cql("getItemInfoShort")
          .executePrepared(getItemPrepared)
          .withParams(getUUID(itemFormat, item))
          .consistencyLevel(ConsistencyLevel.LOCAL_ONE))
  }
  
  def doLoadMainPage(): ChainBuilder = {
      group("LoadMainPage")(
         exec(cql("getItemsCount")
           .executePrepared(getItemsCountPrepared)
           .withParams(List("user_id"))
           .consistencyLevel(ConsistencyLevel.LOCAL_ONE))
           .exec(Iterator.fill(mainPageItems)(getItem(random.nextInt(itemsCount))))
       )
    }
    
  def getRandomMs(limit: Int): FiniteDuration = {
    FiniteDuration(50*random.nextInt(limit), TimeUnit.MILLISECONDS)
  }
  
  def getItemPage(item: Int): ChainBuilder = {
    val uuid = getUUID(itemFormat, item)
    group("getItemPage")(
        exec(cql("getItemInfo")
          .executePrepared(getItemPrepared)
          .withParams(uuid)
          .consistencyLevel(ConsistencyLevel.LOCAL_ONE))
         .exec(cql("getItemsCount")
           .executePrepared(getItemsCountPrepared)
           .withParams(List("user_id"))
           .consistencyLevel(ConsistencyLevel.LOCAL_ONE))
         .exec(cql("getComments")
          .executePrepared(getCommentsPrepared)
          .withParams(uuid)
          .consistencyLevel(ConsistencyLevel.LOCAL_ONE))
     )
  }
  
  def doAddItemToBusket(item: Int): ChainBuilder = {
    val uuid = getUUID(itemFormat, item)
    val user_id = SessionExpression(s => s("user_id").as[UUID])
        exec(cql("addItemToBusket")
          .executePrepared(addItemToBusketPrepared)
          .withParams(user_id, uuid, faker.commerce().productName(), 
              java.math.BigDecimal.valueOf(random.nextDouble()*100))
          .consistencyLevel(ConsistencyLevel.LOCAL_ONE))
  }
  
  def checkItemAvailable(item: Int): ChainBuilder = {
    val uuid = getUUID(itemFormat, item)
    val shop_id = SessionExpression(s => s("shop_id").as[UUID])
    
    exec(cql("checkItemAvailable")
      .executePrepared(getItemAvailabilityPrepared)
      .withParams(uuid, shop_id)
      .consistencyLevel(ConsistencyLevel.LOCAL_ONE))
     
  }
  
  def doSearch(doBuy: Boolean): ChainBuilder = {
    group("DoSearch")(
      repeat(numSuggestions)(
        exec(http("suggestion")
         .get("/solr/atwaters_inventory.inventory/suggest?suggest=true&suggest.dictionary=titleSuggester&suggest.q="
             + random.nextString(4) + "&suggest.cfq=US&wt=json"))
//         .pause(getRandomMs(2))
        )
//       .pause(getRandomMs(3))
       .exec(cql("GetSearchItems")
              .executePrepared(getSearchItemPrepared)
              .withParams(List("prod_name_query"))
              .consistencyLevel(ConsistencyLevel.LOCAL_ONE))
       .exec(Iterator.fill(itemsPerSearch)(checkItemAvailable(random.nextInt(itemsCount))))
       .exec(cql("getItemsCount")
           .executePrepared(getItemsCountPrepared)
           .withParams(List("user_id"))
           .consistencyLevel(ConsistencyLevel.LOCAL_ONE))
//       .pause(getRandomMs(5))
       )
       .exec(Iterator.fill(itemsPerSearch)(getItemPage(random.nextInt(itemsCount))))
       .doIf(SessionExpression(s => doBuy && random.nextInt(10) < 4))(doAddItemToBusket(random.nextInt(itemsCount)))
  }
  
  def doCheckout(): ChainBuilder = {
    group("DoCheckout")(
        pause(FiniteDuration(1, TimeUnit.MILLISECONDS))
    )
  }
  
  val scnNotBuyingUser = scenario("NotBuyingUser").repeat(20)
  {
    feed(feeder)
      .group("NotBuyingUser")(
          exec(doLoadMainPage())
          .repeat(numSearches/2)(doSearch(false))
      )
  }
  
  val scnBuyingUser = scenario("BuyingUser").repeat(20)
  {
    feed(feeder)
      .group("BuyingUser")(
          exec(doLoadMainPage())
          .repeat(numSearches)(doSearch(true))
          .exec(doCheckout)
      )
  }
  
  val scnSearchOnlyUser = scenario("SearchOnlyUser").repeat(20)
  {
    feed(feeder)
      .group("SearchOnly")(
          exec(cql("GetSearchItems")
              .executePrepared(getSearchItemPrepared)
              .withParams(List("prod_name_query")) //SessionExpression(s => s("prod_name_query").as[String])
              .consistencyLevel(ConsistencyLevel.LOCAL_ONE))
      )
  }
  
  val scnOnlySuggestionsUser = scenario("OnlySuggestionsUser").repeat(10)
  {
    feed(feeder)
      .group("SuggestionsOnly")(
        exec(http("suggestion")
         .get("/solr/atwaters_inventory.inventory/suggest?suggest=true&suggest.dictionary=titleSuggester&suggest.q=" +
             SessionExpression(s => s("prod_name_part").as[String]) + "&suggest.cfq=US&wt=json"))
      )
  }
  
  val scnLoadMainPageOnly = scenario("LoadMainPageOnly").repeat(100)
  {
    feed(feeder)
      .group("LoadMainPageOnly")(
          doLoadMainPage()
      )
  }
  
  val scnItemOnly = scenario("ItemOnly").repeat(100)
  {
    feed(feeder)
      .group("GetOnly")(
          exec(cql("getItemInfo")
          .executePrepared(getItemPrepared)
          .withParams(List("item_id"))
          .consistencyLevel(ConsistencyLevel.LOCAL_ONE))
      )
  }
  
  val scnCheckItemOnly = scenario("CheckItemOnly").repeat(100)
  {
    feed(feeder)
      .group("CheckItemOnly")(
          checkItemAvailable(random.nextInt(itemsCount))
      )
  }
  
  val scnAddItemOnly = scenario("AddItemOnly").repeat(50)
  {
    feed(feeder)
      .group("AddItemOnly")(
          doAddItemToBusket(random.nextInt(itemsCount))
      )
  } 
  
  val rampUpTime = FiniteDuration(java.lang.Long.getLong("rampUpTime", 1), TimeUnit.MINUTES)
  val testDuration = FiniteDuration(java.lang.Long.getLong("testDuration", 5), TimeUnit.MINUTES)
  val concurrentSessionCount: Int = Integer.getInteger("concurrentSessionCount", 30)
  val notBuyingUsersPerSecond = concurrentSessionCount.asInstanceOf[Double]
  val buyingUsersPerSecond = notBuyingUsersPerSecond / 10
  val rampUpPerSec = concurrentSessionCount.asInstanceOf[Double] / 10
  
  setUp(
/*      scnNotBuyingUser.inject(
      rampUsersPerSec(notBuyingUsersPerSecond/10) to notBuyingUsersPerSecond during rampUpTime, 
      nothingFor(20 seconds),
      constantUsersPerSec(notBuyingUsersPerSecond) during testDuration
    ),
    scnBuyingUser.inject(
      rampUsersPerSec(buyingUsersPerSecond/10) to buyingUsersPerSecond during rampUpTime, 
      nothingFor(20 seconds),
      constantUsersPerSec(buyingUsersPerSecond) during testDuration
    )*/
//    scnSearchOnlyUser.inject(
//      rampUsersPerSec(notBuyingUsersPerSecond/10) to notBuyingUsersPerSecond during rampUpTime, 
//      nothingFor(20 seconds),
//      constantUsersPerSec(notBuyingUsersPerSecond) during testDuration
//    )
/*    scnOnlySuggestionsUser.inject(
      constantUsersPerSec(buyingUsersPerSecond) during testDuration
    )*/
/*    scnLoadMainPageOnly.inject(
      constantUsersPerSec(buyingUsersPerSecond) during testDuration
    )*/
/*    scnCheckItemOnly.inject(
      constantUsersPerSec(buyingUsersPerSecond) during testDuration
    )*/
/*   scnAddItemOnly.inject(
      constantUsersPerSec(buyingUsersPerSecond) during testDuration
    )*/
    scnItemOnly.inject(
      constantUsersPerSec(buyingUsersPerSecond) during testDuration
    )
  ).protocols(cqlConfig).protocols(httpConf)

  after(cluster.close())

}