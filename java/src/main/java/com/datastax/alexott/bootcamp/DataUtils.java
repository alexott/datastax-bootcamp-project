package com.datastax.alexott.bootcamp;

import java.util.UUID;

public class DataUtils {
	final static public String itemFormat = "f5c03dd1-2e78-11e8-8d2e-%012d";
	final static public String userFormat = "6a23b0f0-2e77-11e8-8d2e-%012d";
	final static public String shopFormat = "f2a6cb00-2734-11e8-ad05-%012d";
	
	public static int countries = 10; // for web shops
	public static int shopCount = 500 + countries;
	
	public static int usersCount = 10000000;
	public static int itemsCount = 1000000;
	
	public static UUID getUUID(final String format, int value) {
		return UUID.fromString(String.format(format, value));
	}
	
	
}
