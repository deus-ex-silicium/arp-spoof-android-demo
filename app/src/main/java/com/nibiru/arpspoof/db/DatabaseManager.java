package com.nibiru.arpspoof.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by nibiru on 3/22/18.
 */

public class DatabaseManager {
    /**************************************CLASS FIELDS********************************************/
    protected final String TAG = getClass().getSimpleName();
    private AtomicInteger mOpenCounter = new AtomicInteger();
    private static DatabaseManager instance;
    private static LogDbHelper mDatabaseHelper;
    private SQLiteDatabase mDatabase;
    /**************************************CLASS METHODS*******************************************/

    // for client log
    // select timestamp, host from log where mac="dc:85:de:8d:56:b5"
    // for request details
    // select count(host) from log where mac="dc:85:de:8d:56:b5" and host="joemonster.org"

    // To prevent someone from accidentally instantiating the DatabaseManager class,
    // make the constructor private.
    private DatabaseManager(){
    }

    public static synchronized void initializeInstance(LogDbHelper helper) {
        if (instance == null) {
            instance = new DatabaseManager();
            mDatabaseHelper = helper;
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException(DatabaseManager.class.getSimpleName() +
                    " is not initialized, call initialize(..) method first.");
        }
        return instance;
    }

    public synchronized void cleanDatabase(){
        mDatabaseHelper.onClear(mDatabase);
    }

    public synchronized void openDatabase() {
        if(mOpenCounter.incrementAndGet() == 1) {
            // Opening new database
            mDatabase = mDatabaseHelper.getWritableDatabase();
        }
    }

    public synchronized void closeDatabase() {
        if(mOpenCounter.decrementAndGet() == 0) {
            // Closing database
            mDatabase.close();
        }
    }


    public List<DnsEntry> getDnsLog(){
        // Define a projection that specifies which columns from the database we want to get
        String[] projection = {
                LogDbContract.LogEntry._ID,
                LogDbContract.LogEntry.COLUMN_NAME_TIMESTAMP,
                LogDbContract.LogEntry.COLUMN_NAME_DOMAIN
        };
        // Filter results WHERE "MAC" = client's mac
        //String selection = LogDbContract.LogEntry.COLUMN_NAME_MAC + " = ?";
        //String[] selectionArgs = { c.getMac() };
        // Sort resulting Cursor by date and time
        String sortOrder = "datetime("+LogDbContract.LogEntry.COLUMN_NAME_TIMESTAMP+") DESC";

        Cursor cursor = mDatabase.query(
                LogDbContract.LogEntry.TABLE_NAME,        // The table to query
                projection,                               // The columns to return
                null,                             // The columns for the WHERE clause
                null,                          // The values for the WHERE clause
                null,                             // don't group the rows
                null,                               // don't filter by row groups
                sortOrder                                 // The sort order
        );

        //read and return results
        List<DnsEntry> logEntries = new ArrayList<>();
        while(cursor.moveToNext()) {
            long itemId = cursor.getLong(
                    cursor.getColumnIndexOrThrow(LogDbContract.LogEntry._ID));
            String itemTimestamp = cursor.getString(cursor.getColumnIndexOrThrow(LogDbContract
                    .LogEntry.COLUMN_NAME_TIMESTAMP));
            String itemDomain = cursor.getString(cursor.getColumnIndexOrThrow(LogDbContract
                    .LogEntry.COLUMN_NAME_DOMAIN));

            DnsEntry entry = new DnsEntry(itemId, itemTimestamp, itemDomain);
            logEntries.add(entry);
        }
        cursor.close();
        return logEntries;
    }
}