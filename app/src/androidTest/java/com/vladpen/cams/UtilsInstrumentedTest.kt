package com.vladpen.cams

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vladpen.Utils
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UtilsInstrumentedTest {
    @Test fun transcode() {
        val str = "test"
        val encoded = Utils.encodeString(str)
        val decoded = Utils.decodeString(encoded)
        Assert.assertEquals(str, decoded)
    }

    @Test fun decodeUrl() {
        val plainPassword = "password:with/invalid@chars"
        val encodedPassword = Utils.encodeString(plainPassword)
        val encodedUrl = "user:$encodedPassword@ip"
        val decodedUrl = Utils.decodeUrl(encodedUrl)
        Assert.assertEquals("user:$plainPassword@ip", decodedUrl)
    }
}