package com.datastax.alexott.bootcamp;

import java.util.UUID;

public class DataUtils {
	final static public String itemFormat = "f5c03dd1-2e78-11e8-8d2e-%010d";
	final static public String userFormat = "6a23b0f0-2e77-11e8-8d2e-%010d";
	final static public String shopFormat = "f2a6cb00-2734-11e8-ad05-%010d";
	
	public static int countries = 30; // for web shops
	public static int shopCount = 2000 + countries;
	
	public static int usersCount = 70000000;
	public static int itemsCount = 10000;
	
	public static UUID getUUID(final String format, int value) {
		return UUID.fromString(String.format(format, value));
	}
	
	
}
