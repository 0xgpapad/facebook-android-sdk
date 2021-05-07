package com.facebook.appevents.internal

import android.app.Activity
import android.app.Application
import com.facebook.FacebookPowerMockTestCase
import com.facebook.appevents.aam.MetadataIndexer
import com.facebook.appevents.codeless.CodelessManager
import com.facebook.appevents.suggestedevents.SuggestedEventsManager
import com.facebook.internal.FeatureManager
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.spy
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(
    FeatureManager::class,
    CodelessManager::class,
    MetadataIndexer::class,
    SuggestedEventsManager::class)
class ActivityLifecycleTrackerTest : FacebookPowerMockTestCase() {

  private lateinit var mockApplication: Application
  private lateinit var mockActivity: Activity
  private lateinit var mockScheduledExecutor: FacebookSerialThreadPoolMockExecutor

  private val appID = "123"

  @Before
  fun `init`() {
    mockApplication = PowerMockito.mock(Application::class.java)
    mockActivity = PowerMockito.mock(Activity::class.java)
    PowerMockito.mockStatic(FeatureManager::class.java)
    PowerMockito.mockStatic(CodelessManager::class.java)
    PowerMockito.mockStatic(MetadataIndexer::class.java)
    PowerMockito.mockStatic(SuggestedEventsManager::class.java)

    mockScheduledExecutor = spy(FacebookSerialThreadPoolMockExecutor(1))
    Whitebox.setInternalState(
        ActivityLifecycleTracker::class.java, "singleThreadExecutor", mockScheduledExecutor)
  }

  @Test
  fun `test start tracking`() {
    ActivityLifecycleTracker.startTracking(mockApplication, appID)
    Mockito.verify(mockApplication, Mockito.times(1)).registerActivityLifecycleCallbacks(any())
  }

  @Test
  fun `test create activity`() {
    ActivityLifecycleTracker.onActivityCreated(mockActivity)
    Mockito.verify(mockScheduledExecutor).execute(ArgumentMatchers.any(Runnable::class.java))
  }

  @Test
  fun `test resume activity`() {
    var codelessManagerCounter = 0
    var metadataIndexerCounter = 0
    var suggestedEventsManagerCounter = 0

    PowerMockito.`when`(CodelessManager.onActivityResumed(eq(mockActivity))).thenAnswer {
      codelessManagerCounter++
    }
    PowerMockito.`when`(MetadataIndexer.onActivityResumed(eq(mockActivity))).thenAnswer {
      metadataIndexerCounter++
    }
    PowerMockito.`when`(SuggestedEventsManager.trackActivity(eq(mockActivity))).thenAnswer {
      suggestedEventsManagerCounter++
    }
    ActivityLifecycleTracker.onActivityResumed(mockActivity)
    assertEquals(1, codelessManagerCounter)
    assertEquals(1, metadataIndexerCounter)
    assertEquals(1, suggestedEventsManagerCounter)

    Mockito.verify(mockScheduledExecutor).execute(ArgumentMatchers.any(Runnable::class.java))

    assertEquals(mockActivity, ActivityLifecycleTracker.getCurrentActivity())
  }
}
