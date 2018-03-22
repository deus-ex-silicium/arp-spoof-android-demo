package com.nibiru.arpspoof.db;

/**
 * Created by nibiru on 3/22/18.
 */

public class DnsEntry {
    /**************************************CLASS FIELDS********************************************/
    private long mId;
    private String timestamp;
    private String domain;
    /**************************************CLASS METHODS*******************************************/
    public DnsEntry(long id, String timestamp, String domain){
        this.mId = id;
        this.timestamp = timestamp;
        this.domain = domain;

    }

    public long getmId() {
        return mId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getDomain() {
        return domain;
    }
}
