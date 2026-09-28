package com.hyperpixelacity.app.core.graphics

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.opengl.*
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import androidx.camera.core.*
import com.hyperpixelacity.app.core.model.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference

/** A single GL owner composites the same scene into preview and encoder outputs. */
class MagicProcessor(context:Context, private val error:(String)->Unit) : SurfaceProcessor, AutoCloseable {
 private val thread=HandlerThread("MagicGL").apply { start() }
 private val handler=Handler(thread.looper)
 val executor=Executor { handler.post(it) }
 val effect=object:CameraEffect(PREVIEW or VIDEO_CAPTURE,executor,this,{ error("Camera compositor failed. Reopen the studio.") }) {}
 private val fragment=context.assets.open("magic.frag").bufferedReader().use { it.readText() }
 private var display=EGL14.EGL_NO_DISPLAY
 private var eglContext=EGL14.EGL_NO_CONTEXT
 private var config:EGLConfig?=null
 private var pbuffer=EGL14.EGL_NO_SURFACE
 private var program=0
 private var stopped=false
 private var inputs=0
 private val outputs=linkedMapOf<SurfaceOutput,EGLSurface>()
 private data class Packet(val scene:Scene,val settings:StudioSettings,val sensorToAnalysis:Matrix,val width:Int,val height:Int)
 private val pending=AtomicReference<Packet?>(null)
 private var scene=Scene()
 private var settings=StudioSettings()
 private var analysisToSensor=Matrix()
 private var analysisWidth=1
 private var analysisHeight=1
 private val vertices=ByteBuffer.allocateDirect(8*4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(floatArrayOf(-1f,-1f,1f,-1f,-1f,1f,1f,1f));position(0) }
 private val trailBuffer=FloatArray(32*3)
 private val landmarks=FloatArray(42*2)
 private val original=FloatArray(16)
 private val transformed=FloatArray(16)
 private val mapped=FloatArray(2)
 private val uniforms=mutableMapOf<String,Int>()
 private fun u(name:String)=uniforms.getOrPut(name) { GLES30.glGetUniformLocation(program,name) }
 fun update(next:Scene,s:StudioSettings,sensorToAnalysis:Matrix,w:Int,h:Int) {
  // Replace stale tracking snapshots rather than queuing them behind GPU work.
  pending.set(Packet(next,s,Matrix(sensorToAnalysis),w,h))
 }
 fun configure(s:StudioSettings) { executor.execute { settings=s } }
 private fun init() {
  if(display!=EGL14.EGL_NO_DISPLAY) return
  display=EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
  check(EGL14.eglInitialize(display,IntArray(2),0,IntArray(2),1)) { "EGL initialization" }
  val configs=arrayOfNulls<EGLConfig>(1)
  check(EGL14.eglChooseConfig(display,intArrayOf(EGL14.EGL_RENDERABLE_TYPE,0x40,EGL14.EGL_SURFACE_TYPE,EGL14.EGL_WINDOW_BIT or EGL14.EGL_PBUFFER_BIT,EGL14.EGL_RED_SIZE,8,EGL14.EGL_GREEN_SIZE,8,EGL14.EGL_BLUE_SIZE,8,0x3142,1,EGL14.EGL_NONE),0,configs,0,1,IntArray(1),0))
  config=checkNotNull(configs[0])
  eglContext=EGL14.eglCreateContext(display,config,EGL14.EGL_NO_CONTEXT,intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION,3,EGL14.EGL_NONE),0)
  check(eglContext!=EGL14.EGL_NO_CONTEXT)
  pbuffer=EGL14.eglCreatePbufferSurface(display,config,intArrayOf(EGL14.EGL_WIDTH,1,EGL14.EGL_HEIGHT,1,EGL14.EGL_NONE),0)
  current(pbuffer)
  val vs=shader(GLES30.GL_VERTEX_SHADER,"""#version 300 es
layout(location=0) in vec2 aPosition;
uniform mat4 uTransform;
out vec2 vCamera;
void main(){ gl_Position=vec4(aPosition,0.0,1.0);vCamera=(uTransform*vec4(aPosition*0.5+0.5,0.0,1.0)).xy; }
""")
  val fs=shader(GLES30.GL_FRAGMENT_SHADER,fragment)
  program=GLES30.glCreateProgram();GLES30.glAttachShader(program,vs);GLES30.glAttachShader(program,fs);GLES30.glLinkProgram(program)
  val ok=IntArray(1);GLES30.glGetProgramiv(program,GLES30.GL_LINK_STATUS,ok,0)
  check(ok[0]!=0) { GLES30.glGetProgramInfoLog(program) }
  GLES30.glDeleteShader(vs);GLES30.glDeleteShader(fs)
 }
 private fun shader(type:Int,source:String):Int {
  val id=GLES30.glCreateShader(type);GLES30.glShaderSource(id,source);GLES30.glCompileShader(id)
  val ok=IntArray(1);GLES30.glGetShaderiv(id,GLES30.GL_COMPILE_STATUS,ok,0);check(ok[0]!=0) { GLES30.glGetShaderInfoLog(id) };return id
 }
 private fun current(surface:EGLSurface) { check(EGL14.eglMakeCurrent(display,surface,surface,eglContext)) { "EGL context unavailable" } }
 override fun onInputSurface(request:SurfaceRequest) {
  if(stopped) { request.willNotProvideSurface();return }
  try {
   init();current(pbuffer)
   val names=IntArray(1);GLES30.glGenTextures(1,names,0)
   GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,names[0])
   GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES30.GL_TEXTURE_MIN_FILTER,GLES30.GL_LINEAR)
   GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES30.GL_TEXTURE_MAG_FILTER,GLES30.GL_LINEAR)
   GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES30.GL_TEXTURE_WRAP_S,GLES30.GL_CLAMP_TO_EDGE)
   GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES30.GL_TEXTURE_WRAP_T,GLES30.GL_CLAMP_TO_EDGE)
   val texture=SurfaceTexture(names[0]);val size=request.resolution
   texture.setDefaultBufferSize(size.width,size.height)
   val surface=Surface(texture)
   val sensorToInput=Matrix()
   request.setTransformationInfoListener(executor) { sensorToInput.set(it.sensorToBufferTransform) }
   inputs++
   request.provideSurface(surface,executor) {
    texture.setOnFrameAvailableListener(null);texture.release();surface.release()
    current(pbuffer);GLES30.glDeleteTextures(1,names,0);inputs--;disposeIfIdle()
   }
   texture.setOnFrameAvailableListener({
    if(!stopped) try {
     current(pbuffer);texture.updateTexImage();texture.getTransformMatrix(original)
     draw(texture,names[0],size.width,size.height,sensorToInput)
    } catch(e:Exception) { error("Graphics interrupted. Reopen the studio.") }
   },handler)
  } catch(e:Exception) { request.willNotProvideSurface();error("OpenGL ES 3.0 initialization failed.") }
 }
 override fun onOutputSurface(output:SurfaceOutput) {
  if(stopped) { output.close();return }
  try {
   init()
   val surface=output.getSurface(executor) {
    outputs.remove(output)?.let { current(pbuffer);EGL14.eglDestroySurface(display,it) };output.close();disposeIfIdle()
   }
   val egl=EGL14.eglCreateWindowSurface(display,config,surface,intArrayOf(EGL14.EGL_NONE),0)
   check(egl!=EGL14.EGL_NO_SURFACE);outputs[output]=egl
  } catch(e:Exception) { output.close();error("Unable to create camera output.") }
 }
 private fun map(p:Point,matrix:Matrix,w:Int,h:Int):Point {
  mapped[0]=p.x*analysisWidth;mapped[1]=p.y*analysisHeight
  analysisToSensor.mapPoints(mapped);matrix.mapPoints(mapped)
  return Point(mapped[0]/w,mapped[1]/h)
 }
 private fun draw(texture:SurfaceTexture,textureName:Int,w:Int,h:Int,matrix:Matrix) {
  pending.getAndSet(null)?.let { packet ->
   scene=packet.scene;settings=packet.settings;analysisWidth=packet.width;analysisHeight=packet.height
   if(!packet.sensorToAnalysis.invert(analysisToSensor)) scene=Scene()
  }
  val time=texture.timestamp/1_000_000
  val fresh=time-scene.timeMs in -40..200
  val center=map(scene.center,matrix,w,h)
  val edge=map(Point(scene.center.x+scene.radius,scene.center.y),matrix,w,h)
  val radius=center.distance(edge).coerceIn(.01f,.35f)*settings.size
  var count=0
  for(i in scene.trail.indices) {
   val age=(time-scene.trailTimes[i])/1000f
   if(age>=0 && age<settings.trailSeconds && count<32) {
    val p=map(scene.trail[i],matrix,w,h);trailBuffer[count*3]=p.x;trailBuffer[count*3+1]=p.y;trailBuffer[count*3+2]=1-age/settings.trailSeconds;count++
   }
  }
  var lc=0
  if(settings.debug && fresh) for(hand in scene.hands) for(p in hand.points) {
   val mp=map(p,matrix,w,h);landmarks[lc*2]=mp.x;landmarks[lc*2+1]=mp.y;lc++
  }
  for((out,surface) in outputs) {
   current(surface);GLES30.glViewport(0,0,out.size.width,out.size.height)
   GLES30.glUseProgram(program);out.updateTransformMatrix(transformed,original)
   GLES30.glUniformMatrix4fv(u("uTransform"),1,false,transformed,0)
   GLES30.glActiveTexture(GLES30.GL_TEXTURE0);GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,textureName);GLES30.glUniform1i(u("uCamera"),0)
   GLES30.glUniform2f(u("uCenter"),center.x,center.y)
   GLES30.glUniform1f(u("uAspect"),w.toFloat()/h)
   GLES30.glUniform1f(u("uRadius"),radius)
   GLES30.glUniform1f(u("uTime"),if(settings.reduceMotion) 0f else (time%60000)/1000f)
   GLES30.glUniform1f(u("uCharge"),if(fresh) scene.charge else 0f)
   GLES30.glUniform1f(u("uIntensity"),settings.intensity)
   GLES30.glUniform1f(u("uGlow"),settings.glow)
   GLES30.glUniform1f(u("uDensity"),settings.density)
   GLES30.glUniform1i(u("uEffect"),settings.effect)
   GLES30.glUniform1i(u("uQuality"),settings.quality)
   val color=if(settings.hue!=0L) settings.hue else Effects.all[settings.effect].color
   GLES30.glUniform3f(u("uColor"),((color shr 16) and 255)/255f,((color shr 8) and 255)/255f,(color and 255)/255f)
   GLES30.glUniform1i(u("uTrailCount"),if(fresh) count else 0)
   GLES30.glUniform3fv(u("uTrail"),32,trailBuffer,0)
   GLES30.glUniform1i(u("uLandmarkCount"),lc)
   GLES30.glUniform2fv(u("uLandmarks"),42,landmarks,0)
   vertices.position(0);GLES30.glEnableVertexAttribArray(0);GLES30.glVertexAttribPointer(0,2,GLES30.GL_FLOAT,false,0,vertices)
   GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP,0,4)
   EGLExt.eglPresentationTimeANDROID(display,surface,texture.timestamp)
   check(EGL14.eglSwapBuffers(display,surface)) { "Camera surface lost" }
  }
 }
 override fun close() { executor.execute {
  stopped=true
  outputs.forEach { (out,s) -> if(display!=EGL14.EGL_NO_DISPLAY) { current(pbuffer);EGL14.eglDestroySurface(display,s) };out.close() };outputs.clear();disposeIfIdle()
 } }
 private fun disposeIfIdle() {
  if(!stopped || inputs>0 || outputs.isNotEmpty()) return
  if(display!=EGL14.EGL_NO_DISPLAY) {
   current(pbuffer);if(program!=0) GLES30.glDeleteProgram(program)
   EGL14.eglMakeCurrent(display,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT)
   EGL14.eglDestroySurface(display,pbuffer);EGL14.eglDestroyContext(display,eglContext);EGL14.eglTerminate(display);display=EGL14.EGL_NO_DISPLAY
  }
  thread.quitSafely()
 }
}
