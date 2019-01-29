package com.datastax.alexott.bootcamp;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.dse.DseCluster;
import com.datastax.driver.dse.DseSession;
import com.datastax.driver.dse.geometry.Point;
import com.datastax.driver.extras.codecs.jdk8.InstantCodec;
import com.github.javafaker.Address;
import com.github.javafaker.Faker;
import com.google.common.collect.Sets;

public class DataLoader {

	private static String[] TAGS = { "book", "electronics", "parfum", "clothes", "pharmacy", "children", "computer",
			"home" };
	private static int MAX_URLS = 10;
	private static String BASE_URL = "http://cdn.images.atwaters.com/";

	public static void main(String[] args) throws InterruptedException {
		DseCluster cluster = DseCluster.builder().addContactPoint(System.getProperty("contactPoint", "127.0.0.1")).build();
		cluster.getConfiguration().getCodecRegistry().register(InstantCodec.instance);
		
		PoolingOptions poolingOptions = cluster.getConfiguration().getPoolingOptions();
		poolingOptions.setMaxRequestsPerConnection(HostDistance.LOCAL, 32768)
				.setMaxRequestsPerConnection(HostDistance.REMOTE, 2000);

		DseSession session = cluster.connect("atwaters_inventory");

		SessionLimiter sl = new SessionLimiter(session);

		PreparedStatement shopInsert = session
				.prepare("insert into shops (id, url, address, city, state, zip, country, phone, location)"
						+ "   values(?, ?, ?, ?, ?, ?, ?, ?, ?);");

		Faker faker = new Faker();

		Set<String> tset = new TreeSet<String>();
		tset.add("USD");
		while (tset.size() < DataUtils.countries) {
			tset.add(faker.currency().code());
		}
		List<String> currencies = new ArrayList<String>(tset);
		tset.clear();
		tset.add("US");
		while (tset.size() < DataUtils.countries) {
			tset.add(faker.address().countryCode());
		}
		List<String> countries = new ArrayList<String>(tset);
		List<UUID> shopIds = new ArrayList<UUID>(DataUtils.shopCount);

		Random random = new Random(System.currentTimeMillis());

		// create shops
		System.out.println("Going to generate shops");
		for (int i = 0; i < DataUtils.shopCount; i++) {
			Address addr = faker.address();
			UUID shopId = DataUtils.getUUID(DataUtils.shopFormat, i);
			BoundStatement stmt = shopInsert.bind(shopId,
					String.format("http://atwaters.%s/shop%d", faker.internet().domainSuffix(), i),
					addr.streetAddress(), addr.city(), addr.stateAbbr(), addr.zipCode(),
					countries.get(random.nextInt(countries.size())),
					Sets.newHashSet(faker.phoneNumber().phoneNumber(), faker.phoneNumber().cellPhone()),
					new Point(random.nextDouble() * 180 - 90, random.nextDouble() * 180 - 90));
			sl.executeAsync(stmt);
			shopIds.add(shopId);
		}

		// create products
		PreparedStatement productInsert = session.prepare("insert into inventory (base_sku, sku, upc, available, "
				+ "tags, urls, rating, rating_count, title, description, country, price, buy_price, currency)"
				+ "values (?, ?, ?, true, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");

		PreparedStatement commentInsert = session.prepare("insert into comments(base_sku, sku, user_id, posted, "
				+ "user_name, comment, rating) values (?, ?, ?, ?, ?, ?, ?);");

		PreparedStatement counterUpdate = session.prepare("insert into inv_counters(cnt, sku, shop) values(?,?,?)");

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
			int ratingCnt = 0;
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
				sl.executeAsync(cstmt);
				ratingCnt += 1;
				totalRating += rating;
			}

			// TODO: combine 2 different names + " " + faker.commerce().productName() &
			// shuffle the words?
			String product = faker.commerce().productName() + " " + faker.commerce().color() + " "
					+ faker.commerce().material();
			String description = product + "\n\n"
					+ StringUtils.join(faker.lorem().paragraphs(random.nextInt(4) + 1), "\n\n");
			float rating = totalRating / ratingCnt;
			String upc = faker.number().digits(12);
			for (int j = 0; j < DataUtils.countries; j++) {
				double price = Double.valueOf(faker.commerce().price(10, 1000));
				BoundStatement pstmt = productInsert.bind(id, id, upc, tags, urls, rating, ratingCnt, product,
						description, countries.get(j), BigDecimal.valueOf(price), BigDecimal.valueOf(price * 1.15),
						currencies.get(j));
				sl.executeAsync(pstmt);
			}

			for (UUID shopId : shopIds) {
				BoundStatement ustmt = counterUpdate.bind(random.nextInt(100) + 1, id, shopId);
				sl.executeAsync(ustmt);
			}

			if ((i + 1) % 100 == 0) {
				long end = System.currentTimeMillis();
				System.out.println("Processed " + (i + 1) + " items in " + (end - start) + "ms");
				start = end;
			}
		}

		System.out.println("Loading is finished...");
		sl.waitForFinish();

		session.close();
		cluster.close();
	}

}
