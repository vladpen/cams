package com.vladpen.cams

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vladpen.Utils
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UtilsInstrumentedTest {
    @Test fun transcode() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val str = "test"
        val encoded = Utils.encodeString(context, str)
        val decoded = Utils.decodeString(context, encoded)
        Assert.assertEquals(str, decoded)
    }

    @Test fun decodeUrl() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val plainPassword = "test"
        val encodedPassword = Utils.encodeString(context, plainPassword)
        val encodedUrl = "user:$encodedPassword@ip"
        val decodedUrl = Utils.decodeUrl(context, encodedUrl)
        Assert.assertEquals("user:$plainPassword@ip", decodedUrl)
    }
}