package com.basya.gramm.model;

public class Message {
    public String msgId;
    public String from;
    public String text;
    public String type;
    public String timestamp;
    public String fileUrl;
    public boolean isOwn;
    public boolean read;
    public long duration; // for voice messages in ms
}
