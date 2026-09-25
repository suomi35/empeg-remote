package com.chasinglemons.empeg.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DiscoveryResponseTest {

    @Test
    fun parseStandardPlayerResponse() {
        val empeg = DiscoveryResponse.parse("name=EmpegCar", "192.168.1.50")
        assertEquals("EmpegCar", empeg?.name)
        assertEquals("192.168.1.50", empeg?.ip)
    }

    @Test
    fun parseSimulatorResponseWithPort() {
        val empeg = DiscoveryResponse.parse("name=EmpegSim port=8099", "192.168.1.50")
        assertEquals("EmpegSim", empeg?.name)
        assertEquals("192.168.1.50:8099", empeg?.ip)
    }

    @Test
    fun parseStandardPortOmittedFromIp() {
        val empeg = DiscoveryResponse.parse("name=EmpegCar port=80", "192.168.1.50")
        assertEquals("EmpegCar", empeg?.name)
        assertEquals("192.168.1.50", empeg?.ip)
    }

    @Test
    fun ignoresProbeEchoAndEmpty() {
        assertNull(DiscoveryResponse.parse("?", "192.168.1.50"))
        assertNull(DiscoveryResponse.parse("", "192.168.1.50"))
        assertNull(DiscoveryResponse.parse("   ", "192.168.1.50"))
    }
}
