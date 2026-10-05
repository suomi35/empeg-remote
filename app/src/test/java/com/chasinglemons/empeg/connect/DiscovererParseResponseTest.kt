package com.chasinglemons.empeg.connect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.InetAddress

class DiscovererParseResponseTest {

    private val address: InetAddress = InetAddress.getByName("192.168.1.50")

    @Test
    fun simulatorReplyIncludesHttpPort() {
        val empeg = Discoverer.parseResponse("name=EmpegSim port=8099", address)
        assertEquals("EmpegSim", empeg?.name)
        assertEquals("192.168.1.50:8099", empeg?.ip)
    }

    @Test
    fun realPlayerReplyStaysBareHost() {
        val empeg = Discoverer.parseResponse("name=EmpegCar", address)
        assertEquals("EmpegCar", empeg?.name)
        assertEquals("192.168.1.50", empeg?.ip)
    }

    @Test
    fun portEightyStaysBareHost() {
        val empeg = Discoverer.parseResponse("name=Sim port=80", address)
        assertEquals("192.168.1.50", empeg?.ip)
    }

    @Test
    fun invalidPortFallsBackToBareHost() {
        val empeg = Discoverer.parseResponse("name=Sim port=notanumber", address)
        assertEquals("192.168.1.50", empeg?.ip)
    }

    @Test
    fun missingNameIsIgnored() {
        assertNull(Discoverer.parseResponse("port=8099", address))
    }

    @Test
    fun probeEchoIsIgnored() {
        assertNull(Discoverer.parseResponse("?", address))
    }
}
