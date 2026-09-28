package com.hyperpixelacity.app.core.tracking
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.hyperpixelacity.app.core.model.*
import java.util.concurrent.atomic.AtomicBoolean

/** Input buffer coordinates are retained for precise mapping through sensor space. */
class HandTracker(context: Context, private val onResult: (Long,List<Hand>,Matrix,Int,Int)->Unit, private val onError: (String)->Unit) : AutoCloseable {
 private val busy=AtomicBoolean(false)
 private var timestamp=0L
 private var matrix=Matrix()
 private var width=1
 private var height=1
 private var rotation=0
 private var bitmap: Bitmap?=null
 private var image: com.google.mediapipe.framework.image.MPImage?=null
 private val task=HandLandmarker.createFromOptions(context,HandLandmarker.HandLandmarkerOptions.builder()
  .setBaseOptions(BaseOptions.builder().setModelAssetPath("hand_landmarker.task").build())
  .setRunningMode(RunningMode.LIVE_STREAM).setNumHands(2)
  .setMinHandDetectionConfidence(.6f).setMinHandPresenceConfidence(.6f).setMinTrackingConfidence(.6f)
  .setResultListener { result,_ ->
   try {
    val hands=result.landmarks().mapIndexed { i,ps ->
     fun inverse(x:Float,y:Float):Point = when(rotation) { 90->Point(y,1-x);180->Point(1-x,1-y);270->Point(1-y,x);else->Point(x,y) }
     Hand(result.handedness()[i].first().categoryName(),ps.map { inverse(it.x(),it.y()).copy(z=it.z()) },result.handedness()[i].first().score(),result.worldLandmarks()[i].map { Point(it.x(),it.y(),it.z()) })
    }
    onResult(result.timestampMs(),hands,Matrix(matrix),width,height)
   } finally { releaseInput();busy.set(false) }
  }.setErrorListener { releaseInput();busy.set(false);onError("Hand tracking stopped. Reopen the studio to retry.") }.build())
 private fun releaseInput() { image?.close();image=null;bitmap?.recycle();bitmap=null }
 fun analyze(proxy:ImageProxy) {
  if(!busy.compareAndSet(false,true)) { proxy.close();return }
  try {
   width=proxy.width;height=proxy.height;rotation=proxy.imageInfo.rotationDegrees
   matrix=Matrix(proxy.imageInfo.sensorToBufferTransformMatrix)
   val raw=proxy.toBitmap()
   bitmap=if(rotation==0) raw else Bitmap.createBitmap(raw,0,0,width,height,Matrix().apply { postRotate(rotation.toFloat()) },false).also { if(it!==raw) raw.recycle() }
   timestamp=maxOf(timestamp+1,proxy.imageInfo.timestamp/1_000_000)
   image=BitmapImageBuilder(bitmap!!).build()
   task.detectAsync(image!!,timestamp)
  } catch(e:Exception) { releaseInput();busy.set(false);onError("Could not process this camera frame.") }
  finally { proxy.close() }
 }
 override fun close() { task.close();releaseInput() }
}
