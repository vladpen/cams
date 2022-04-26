package com.vladpen.cams

import com.vladpen.Utils
import org.junit.Test
import org.junit.Assert.*

class UtilsUnitTest {
    @Test
    fun parseUrl_full() {
        val url = "scheme://user:password@host.name.or.ip:1234/some/path/"
        val actual = Utils.parseUrl(url)
        assertEquals("scheme", actual?.scheme)
        assertEquals("user", actual?.user)
        assertEquals("password", actual?.password)
        assertEquals("host.name.or.ip", actual?.host)
        assertEquals(1234, actual?.port)
        assertEquals("/some/path/", actual?.path)
    }

    @Test
    fun parseUrl2() {
        val url = "user:password@host.name.or.ip:1234/some/path/"
        val actual = Utils.parseUrl(url)
        assertEquals("", actual?.scheme)
        assertEquals("user", actual?.user)
        assertEquals("password", actual?.password)
        assertEquals("host.name.or.ip", actual?.host)
        assertEquals(1234, actual?.port)
        assertEquals("/some/path/", actual?.path)
    }

    @Test
    fun parseUrl3() {
        val url = "user@host.name.or.ip:1234/some/path/"
        val actual = Utils.parseUrl(url)
        assertEquals("", actual?.scheme)
        assertEquals("user", actual?.user)
        assertEquals("", actual?.password)
        assertEquals("host.name.or.ip", actual?.host)
        assertEquals(1234, actual?.port)
        assertEquals("/some/path/", actual?.path)
    }

    @Test
    fun parseUrl4() {
        val url = "host.name.or.ip:1234/some/path/"
        val actual = Utils.parseUrl(url)
        assertEquals("", actual?.scheme)
        assertEquals("", actual?.user)
        assertEquals("", actual?.password)
        assertEquals("host.name.or.ip", actual?.host)
        assertEquals(1234, actual?.port)
        assertEquals("/some/path/", actual?.path)
    }

    @Test
    fun parseUrl5() {
        val url = "host.name.or.ip/some/path/"
        val actual = Utils.parseUrl(url)
        assertEquals("", actual?.scheme)
        assertEquals("", actual?.user)
        assertEquals("", actual?.password)
        assertEquals("host.name.or.ip", actual?.host)
        assertEquals(554, actual?.port)
        assertEquals("/some/path/", actual?.path)
    }

    @Test
    fun parseUrl6() {
        val url = "host.name.or.ip"
        val actual = Utils.parseUrl(url)
        assertEquals("", actual?.scheme)
        assertEquals("", actual?.user)
        assertEquals("", actual?.password)
        assertEquals("host.name.or.ip", actual?.host)
        assertEquals(554, actual?.port)
        assertEquals("/", actual?.path)
    }

    @Test
    fun replacePassword() {
        val url = "scheme://user:password@host.name.or.ip:1234/some/path/"
        val actual = Utils.replacePassword(url, "***")
        assertEquals("scheme://user:***@host.name.or.ip:1234/some/path/", actual)
    }
}