package com.datastax.alexott.bootcamp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.Statement;
import com.datastax.driver.dse.DseCluster;
import com.datastax.driver.dse.DseSession;
import com.datastax.driver.extras.codecs.jdk8.InstantCodec;
import com.github.javafaker.Address;
import com.github.javafaker.Faker;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.MoreExecutors;

public class DataLoader {

	private static String[] TAGS = { "book", "electronics", "parfum", "clothes", "pharmacy", "children", "computer",
			"home" };
	private static int MAX_URLS = 10;
	private static String BASE_URL = "http://cdn.images.atwaters.com/";
	final static AtomicLong runningQueries = new AtomicLong(0);

	static void waitForQueries() {
		int cnt = 0;
		while(runningQueries.get() > 0) {
			cnt += 1;
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
			if (cnt > 100) {
				System.out.println("We're waiting already more than " + cnt + " retries");
				throw new RuntimeException("Waiting too long");
			}
		}
	}
	
	public static void main(String[] args) {
		DseCluster cluster = DseCluster.builder().addContactPoint("127.0.0.1").build();
		cluster.getConfiguration().getCodecRegistry().register(InstantCodec.instance);

		DseSession session = cluster.connect("atwaters_inventory");

		PreparedStatement shopInsert = session
				.prepare("insert into shops (id, url, address, city, state, zip, country, phone)"
						+ "   values(?, ?, ?, ?, ?, ?, ?, ?);");

		Faker faker = new Faker();

		Set<String> tset = new TreeSet<String>();
		while (tset.size() < DataUtils.countries) {
			tset.add(faker.currency().code());
		}
		List<String> currencies = new ArrayList<String>(tset);
		tset.clear();
		while (tset.size() < DataUtils.countries) {
			tset.add(faker.address().countryCode());
		}
		List<String> countries = new ArrayList<String>(tset);
		List<UUID> shopIds = new ArrayList<UUID>(DataUtils.shopCount);

		Executor executor = MoreExecutors.directExecutor();
		
		// create shops
		System.out.println("Going to generate shops");
		for (int i = 0; i < DataUtils.shopCount; i++) {
			Address addr = faker.address();
			UUID shopId = DataUtils.getUUID(DataUtils.shopFormat, i);
			BoundStatement stmt = shopInsert.bind(shopId,
					String.format("http://atwaters.%s/shop%d", faker.internet().domainSuffix(), i),
					addr.streetAddress(), addr.city(), addr.stateAbbr(), addr.zipCode(), addr.country(),
					Sets.newHashSet(faker.phoneNumber().phoneNumber(), faker.phoneNumber().cellPhone()));
			runningQueries.incrementAndGet();
			session.executeAsync(stmt).addListener(() -> runningQueries.decrementAndGet(), executor);
			shopIds.add(shopId);
		}
		waitForQueries();
		
		Random random = new Random(System.currentTimeMillis());

		// create products
		PreparedStatement productInsert = session.prepare("insert into inventory (base_sku, sku, upc, available, "
				+ "tags, urls, rating, title, description, country, price, buy_price, currency)"
				+ "values (?, ?, ?, true, ?, ?, ?, ?, ?, ?, ?, ?, ?);");

		PreparedStatement commentInsert = session.prepare("insert into comments(base_sku, sku, user_id, posted, "
				+ "user_name, comment, rating) values (?, ?, ?, ?, ?, ?, ?);");

		PreparedStatement counterUpdate = session
				.prepare("UPDATE inv_counters SET cnt = cnt + ? WHERE sku = ? AND shop = ?");

		Set<String> tags = new HashSet<String>(TAGS.length);
		List<String> urls = new ArrayList<String>(MAX_URLS);

		System.out.println("Going to generate products");
		long start = System.currentTimeMillis();
		for (int i = 0; i < DataUtils.itemsCount; i++) {
			tags.clear();
			urls.clear();
			String strId = String.format(DataUtils.itemFormat, i);
			UUID id = UUID.fromString(strId);

			for (int j = 0; j < random.nextInt(TAGS.length) + 1; j++) {
				tags.add(TAGS[j]);
			}
			for (int j = 0; j < random.nextInt(MAX_URLS) + 1; j++) {
				urls.add(BASE_URL + strId + "/" + (j + 1));
			}

			// generate from 5 to 10 comments per item
			int cnt = 0;
			float totalRating = (float) 0.0;
			for (int j = 0; j < random.nextInt(6) + 5; j++) {
				int rating = random.nextInt(5) + 1;
				// base_sku, sku, user_id, posted, user_name, comment, rating
				BoundStatement cstmt = commentInsert.bind(id, id,
						DataUtils.getUUID(DataUtils.userFormat, random.nextInt(DataUtils.usersCount)),
						faker.date().past(random.nextInt(1000) + 1, TimeUnit.DAYS).toInstant()
								.plusSeconds(random.nextInt(86400)),
						faker.name().fullName(),
						StringUtils.join(faker.lorem().paragraphs(random.nextInt(4) + 1), "\n\n"), rating);
				runningQueries.incrementAndGet();
				session.executeAsync(cstmt).addListener(() -> runningQueries.decrementAndGet(), executor);
				cnt += 1;
			}
//			waitForQueries();

			String product = faker.commerce().productName();
			String description = product + "\n\n"
					+ StringUtils.join(faker.lorem().paragraphs(random.nextInt(4) + 1), "\n\n");
			float rating = totalRating / cnt;
			String upc = faker.number().digits(12);
			for (int j = 0; j < DataUtils.countries; j++) {
				double price = Double.valueOf(faker.commerce().price(10, 1000));
				BoundStatement pstmt = productInsert.bind(id, id, upc, tags, urls, rating, product, description,
						countries.get(j), price, price * 1.15, currencies.get(j));
				runningQueries.incrementAndGet();
				session.executeAsync(pstmt).addListener(() -> runningQueries.decrementAndGet(), executor);
			}
//			waitForQueries();

			for (UUID shopId : shopIds) {
				BoundStatement ustmt = counterUpdate.bind((long) random.nextInt(100) + 1, id, shopId);
				runningQueries.incrementAndGet();
				session.executeAsync(ustmt).addListener(() -> runningQueries.decrementAndGet(), executor);
			}
			waitForQueries();

			if ((i + 1) % 100 == 0) {
				long end = System.currentTimeMillis();
				System.out.println("Processed " + (i + 1) + " items in " + (end - start) + "ms");
				start = end;
			}
		}

		System.out.println("Loading is finished...");

		session.close();
		cluster.close();
	}

}
