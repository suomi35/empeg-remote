package com.chasinglemons.empeg.empegapi

import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.path
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A player is normally addressed by bare IP (port 80), but a simulator or a
 * hijack on a non-standard port needs "host:port". These tests pin down how
 * that spec has to be turned into URLs.
 */
class HostSpecTest {

    @Test
    fun `ktor accepts host with port embedded`() {
        val url = URLBuilder().apply {
            protocol = URLProtocol.HTTP
            host = "192.168.1.20:8099"
            path("/")
        }.buildString()
        assertEquals("http://192.168.1.20:8099/", url)
    }
}
