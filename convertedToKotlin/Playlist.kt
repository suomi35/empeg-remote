package com.chasinglemons.empeg

class Playlist(
    pName: String,
    pStreamURL: String,
    pPlayURL: String,
    pInsertURL: String,
    pEnqueueURL: String,
    pAppendURL: String,
    pURL: String,
    pLength: String,
    pType: String,
    pArtist: String,
    pSource: String
) {
    private var pName = ""
    private var pStreamURL = ""
    private var pPlayURL = ""
    private var pInsertURL = ""
    private var pAppendURL = ""
    private var pEnqueueURL = ""
    private var pURL = ""
    private var pLength = ""
    private var pType = ""
    private var pArtist = ""
    private var pSource = ""

    init {
        this.pName = pName
        this.pStreamURL = pStreamURL
        this.pPlayURL = pPlayURL
        this.pInsertURL = pInsertURL
        this.pAppendURL = pAppendURL
        this.pEnqueueURL = pEnqueueURL
        this.pURL = pURL
        this.pLength = pLength
        this.pType = pType
        this.pArtist = pArtist
        this.pSource = pSource
    }

    fun setpName(pName: String) {
        this.pName = pName
    }

    fun getpName(): String {
        return pName
    }

    fun setpStreamURL(pStreamURL: String) {
        this.pStreamURL = pStreamURL
    }

    fun getpStreamURL(): String {
        return pStreamURL
    }

    fun setpPlayURL(pPlayURL: String) {
        this.pPlayURL = pPlayURL
    }

    fun getpPlayURL(): String {
        return pPlayURL
    }

    fun setpInsertURL(pInsertURL: String) {
        this.pInsertURL = pInsertURL
    }

    fun getpInsertURL(): String {
        return pInsertURL
    }

    fun setpEnqueueURL(pEnqueueURL: String) {
        this.pEnqueueURL = pEnqueueURL
    }

    fun getpEnqueueURL(): String {
        return pEnqueueURL
    }

    fun setpAppendURL(pAppendURL: String) {
        this.pAppendURL = pAppendURL
    }

    fun getpAppendURL(): String {
        return pAppendURL
    }

    fun setpURL(pURL: String) {
        this.pURL = pURL
    }

    fun getpURL(): String {
        return pURL
    }

    fun setpLength(pLength: String) {
        this.pLength = pLength
    }

    fun getpLength(): String {
        return pLength
    }

    fun setpType(pType: String) {
        this.pType = pType
    }

    fun getpType(): String {
        return pType
    }

    fun setpArtist(pArtist: String) {
        this.pArtist = pArtist
    }

    fun getpArtist(): String {
        return pArtist
    }

    fun setpSource(pSource: String) {
        this.pSource = pSource
    }

    fun getpSource(): String {
        return pSource
    }
}