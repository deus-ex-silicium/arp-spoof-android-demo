package com.nibiru.arpspoof.db;

import android.provider.BaseColumns;

/**
 * Created by nibiru on 3/22/18.
 */

public class LogDbContract {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private LogDbContract() {}

    /* Inner class that defines the table contents */
    public static class LogEntry implements BaseColumns {
        public static final String TABLE_NAME = "dns";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_DOMAIN = "domain";
    }

    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + LogEntry.TABLE_NAME + " (" +
                    LogEntry._ID + " INTEGER PRIMARY KEY," +
                    LogEntry.COLUMN_NAME_TIMESTAMP + " TEXT," +
                    LogEntry.COLUMN_NAME_DOMAIN + " TEXT)";

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + LogEntry.TABLE_NAME;

    public static final String SQL_CLEAR =
            "DELETE FROM " + LogEntry.TABLE_NAME;

}