package com.hyperpixelacity.app

import android.content.Intent
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.camera.view.PreviewView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hyperpixelacity.app.core.camera.CameraStudio
import com.hyperpixelacity.app.core.model.StudioSettings
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraSwitchTest {
 @Test fun repeatedFlipsReuseModelAndDeliverFrames() {
  val instrumentation=InstrumentationRegistry.getInstrumentation()
  val context=instrumentation.targetContext
  ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("pm grant ${context.packageName} android.permission.CAMERA")).use { it.readBytes() }
  val activity=instrumentation.startActivitySync(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
  var cleanup:CameraStudio?=null
  lateinit var camera:CameraStudio
  lateinit var view:PreviewView
  try {
   instrumentation.runOnMainSync {
    view=PreviewView(activity).apply { implementationMode=PreviewView.ImplementationMode.COMPATIBLE }
    activity.setContentView(view)
    camera=CameraStudio(activity)
    cleanup=camera
    camera.configure(StudioSettings(quality=0))
   }
   for(front in listOf(true,false,true,false)) {
    instrumentation.runOnMainSync {camera.bind(activity,view,front)}
    val deadline=SystemClock.elapsedRealtime()+15_000
    while(!camera.status.value.ready && SystemClock.elapsedRealtime()<deadline) SystemClock.sleep(50)
    assertTrue("Camera did not deliver frames: ${camera.status.value.error}",camera.status.value.ready)
    assertEquals("Flip must not recreate the model",1,camera.status.value.modelStarts)
    assertFalse(camera.status.value.switching)
    android.util.Log.i("CameraSwitchTest","front=$front firstFrameMs=${camera.status.value.switchMs}")
   }
  } finally {
   instrumentation.runOnMainSync { cleanup?.close();activity.finish() }
  }
 }
}
