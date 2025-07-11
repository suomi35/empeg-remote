package com.chasinglemons.empeg;

public class Playlist {
	private String pName = "";
	private String pStreamURL = "";
	private String pPlayURL = "";
	private String pInsertURL = "";
	private String pAppendURL = "";
	private String pEnqueueURL = "";
	private String pURL = "";
	private String pLength = "";
	private String pType = "";
	private String pArtist = "";
	private String pSource = "";

    public Playlist(String pName, String pStreamURL, String pPlayURL, String pInsertURL, String pEnqueueURL, String pAppendURL, String pURL, String pLength, String pType, String pArtist, String pSource) {
    	this.pName = pName;
    	this.pStreamURL = pStreamURL;
    	this.pPlayURL = pPlayURL;
    	this.pInsertURL = pInsertURL;
    	this.pAppendURL = pAppendURL;
    	this.pEnqueueURL = pEnqueueURL;
    	this.pURL = pURL;
    	this.pLength = pLength;
    	this.pType = pType;
    	this.pArtist = pArtist;
    	this.pSource = pSource;
    	
    }
    public void setpName(String pName) {
        this.pName = pName;
    }
    public String getpName() {
        return pName;
    }
    public void setpStreamURL(String pStreamURL) {
        this.pStreamURL = pStreamURL;
    }
    public String getpStreamURL() {
        return pStreamURL;
    }
    public void setpPlayURL(String pPlayURL) {
        this.pPlayURL = pPlayURL;
    }
    public String getpPlayURL() {
        return pPlayURL;
    }
    public void setpInsertURL(String pInsertURL) {
        this.pInsertURL = pInsertURL;
    }
    public String getpInsertURL() {
        return pInsertURL;
    }
    public void setpEnqueueURL(String pEnqueueURL) {
        this.pEnqueueURL = pEnqueueURL;
    }
    public String getpEnqueueURL() {
        return pEnqueueURL;
    }
    public void setpAppendURL(String pAppendURL) {
        this.pAppendURL = pAppendURL;
    }
    public String getpAppendURL() {
        return pAppendURL;
    }
    public void setpURL(String pURL) {
        this.pURL = pURL;
    }
    public String getpURL() {
        return pURL;
    }
    public void setpLength(String pLength) {
        this.pLength = pLength;
    }
    public String getpLength() {
        return pLength;
    }
    public void setpType(String pType) {
        this.pType = pType;
    }
    public String getpType() {
        return pType;
    }
    public void setpArtist(String pArtist) {
        this.pArtist = pArtist;
    }
    public String getpArtist() {
        return pArtist;
    }
    public void setpSource(String pSource) {
        this.pSource = pSource;
    }
    public String getpSource() {
        return pSource;
    }
}