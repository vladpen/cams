package com.vladpen.cams

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivitiesInstrumentedTest {
    @Test fun mainActivity() {
        val scenario = launch(MainActivity::class.java)
        scenario.moveToState(Lifecycle.State.CREATED)
        assert(true)
        scenario.close()
    }

    @Test fun editActivity() {
        val scenario = launch(EditActivity::class.java)
        scenario.moveToState(Lifecycle.State.CREATED)
        assert(true)
        scenario.close()
    }
}