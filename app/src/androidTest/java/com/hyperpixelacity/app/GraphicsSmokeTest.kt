package com.hyperpixelacity.app

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES30
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GraphicsSmokeTest {
 @Test fun proceduralEffectShaderCompilesOnGles3() {
  val display=EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
  assertTrue(EGL14.eglInitialize(display,IntArray(2),0,IntArray(2),1))
  val configs=arrayOfNulls<EGLConfig>(1)
  val count=IntArray(1)
  assertTrue(EGL14.eglChooseConfig(display,intArrayOf(EGL14.EGL_RENDERABLE_TYPE,0x40,EGL14.EGL_SURFACE_TYPE,EGL14.EGL_PBUFFER_BIT,EGL14.EGL_RED_SIZE,8,EGL14.EGL_GREEN_SIZE,8,EGL14.EGL_BLUE_SIZE,8,EGL14.EGL_NONE),0,configs,0,1,count,0))
  val config=requireNotNull(configs[0])
  val context=EGL14.eglCreateContext(display,config,EGL14.EGL_NO_CONTEXT,intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION,3,EGL14.EGL_NONE),0)
  val surface=EGL14.eglCreatePbufferSurface(display,config,intArrayOf(EGL14.EGL_WIDTH,16,EGL14.EGL_HEIGHT,16,EGL14.EGL_NONE),0)
  try {
   assertTrue(EGL14.eglMakeCurrent(display,surface,surface,context))
   val source=InstrumentationRegistry.getInstrumentation().targetContext.assets.open("magic.frag").bufferedReader().use { it.readText() }
   val shader=GLES30.glCreateShader(GLES30.GL_FRAGMENT_SHADER)
   GLES30.glShaderSource(shader,source);GLES30.glCompileShader(shader)
   val status=IntArray(1);GLES30.glGetShaderiv(shader,GLES30.GL_COMPILE_STATUS,status,0)
   val log=GLES30.glGetShaderInfoLog(shader)
   GLES30.glDeleteShader(shader)
   assertEquals(log,1,status[0])
  } finally {
   EGL14.eglMakeCurrent(display,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT)
   EGL14.eglDestroySurface(display,surface);EGL14.eglDestroyContext(display,context);EGL14.eglTerminate(display)
  }
 }
}
