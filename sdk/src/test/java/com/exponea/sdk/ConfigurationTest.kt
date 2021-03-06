package com.exponea.sdk

import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.util.currentTimeSeconds
import org.bouncycastle.util.Times
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.sql.Timestamp
import java.text.DateFormat
import java.util.*
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ConfigurationTest {

    private fun initializeExponeaWithoutConfig() {
        val context = RuntimeEnvironment.application
        try {
            Exponea.init(context, "")
        } catch (e: Exception) {

        }
    }

    private fun setupConfigurationWithStruct() {
        val context = RuntimeEnvironment.application
        val configuration = ExponeaConfiguration()
        configuration.projectToken = "projectToken"
        configuration.authorization = "projectAuthorization"
        Exponea.init(context, configuration)
    }

    private fun setupConfigurationWithFile() {
        val context = RuntimeEnvironment.application
        Exponea.init(context, "config_valid.xml")
    }

    @Test
    fun InstantiateSDKWithoutConfig_ShouldFail() {
        initializeExponeaWithoutConfig()
        assertEquals(Exponea.isInitialized, true)
    }

    @Test
    fun InstantiateSDKWithConfig_ShouldPass() {
        setupConfigurationWithStruct()
        assertEquals(Exponea.isInitialized, true)
    }

    @Test
    fun TestData() {
        val timestamp = currentTimeSeconds()
        println("Timestamp: ${timestamp}")
        println("Timestamp: ${Calendar.getInstance().timeInMillis / 1000}")

    }

//    @Test
//    TODO: Finish test loading from file.
//    fun InstantiateSDKWithFile_ShouldPass() {
//        setupConfigurationWithFile()
//        assertEquals(Exponea.isInitialized, true)
//    }
}