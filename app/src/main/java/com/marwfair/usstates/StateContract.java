package com.marwfair.usstates;

import android.provider.BaseColumns;

public class StateContract {
	
	public StateContract() { };
	
	public abstract class StateEntry implements BaseColumns {
		public final static String TABLE_NAME = "states";
		public final static String COLUMN_NAME_NAME = "name";
		public final static String COLUMN_NAME_ABBREVIATION = "abbreviation";
		public final static String COLUMN_NAME_CAPITAL = "capital";
		public final static String COLUMN_NAME_POP_CITY = "most_populous_city";
		public final static String COLUMN_NAME_POPULATION = "population";
		public final static String COLUMN_NAME_SQUARE_MILES = "square_miles";
		public final static String COLUMN_NAME_TIME_ZONE_1 = "time_zone_1";
		public final static String COLUMN_NAME_TIME_ZONE_2 = "time_zone_2";
		public final static String COLUMN_NAME_DST = "dst";
		public final static String COLUMN_NAME_FLAG_URL = "flag_url";
		public final static String COLUMN_NAME_LATITUDE = "latitude";
		public final static String COLUMN_NAME_LONGITUDE = "longitude";
		public static final String TEXT_TYPE = " TEXT ";
		public static final String INTEGER_TYPE = " INTEGER ";
		public static final String COMMA = ", ";
		public static final String SQL_CREATE_ENTRIES =
				"CREATE TABLE " + TABLE_NAME + " (" +
				_ID + " INTEGER PRIMARY KEY" + COMMA +
				COLUMN_NAME_NAME + TEXT_TYPE + COMMA +
				COLUMN_NAME_ABBREVIATION + TEXT_TYPE + COMMA +
				COLUMN_NAME_CAPITAL + TEXT_TYPE + COMMA +
				COLUMN_NAME_POP_CITY + TEXT_TYPE + COMMA +
				COLUMN_NAME_POPULATION + TEXT_TYPE + COMMA +
				COLUMN_NAME_SQUARE_MILES + TEXT_TYPE + COMMA +
				COLUMN_NAME_TIME_ZONE_1 + TEXT_TYPE + COMMA +
				COLUMN_NAME_TIME_ZONE_2 + TEXT_TYPE + COMMA +
				COLUMN_NAME_DST + TEXT_TYPE + COMMA +
				COLUMN_NAME_FLAG_URL + TEXT_TYPE + COMMA +
				COLUMN_NAME_LATITUDE + TEXT_TYPE + COMMA +
				COLUMN_NAME_LONGITUDE + TEXT_TYPE + ")";
		public static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + TABLE_NAME;
		
	}

}
