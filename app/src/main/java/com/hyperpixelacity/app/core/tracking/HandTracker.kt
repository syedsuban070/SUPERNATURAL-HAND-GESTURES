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
class HandTracker(context: Context, private val onResult: (Long,List<Hand>,Matrix,Int,Int,Long,Int)->Unit, private val onError: (String)->Unit) : AutoCloseable {
 private val busy=AtomicBoolean(false)
 private var timestamp=0L
 private var cameraTimestamp=0L
 private var epoch=0
 private var started=0L
 private var matrix=Matrix()
 private var width=1
 private var height=1
 private var rotation=0
 private var bitmap: Bitmap?=null
 private var image: com.google.mediapipe.framework.image.MPImage?=null
 private val task=HandLandmarker.createFromOptions(context,HandLandmarker.HandLandmarkerOptions.builder()
  .setBaseOptions(BaseOptions.builder().setModelAssetPath("hand_landmarker.task").build())
  .setRunningMode(RunningMode.LIVE_STREAM).setNumHands(2)
  .setMinHandDetectionConfidence(.5f).setMinHandPresenceConfidence(.5f).setMinTrackingConfidence(.5f)
  .setResultListener { result,_ ->
   try {
    val hands=result.landmarks().mapIndexed { i,ps ->
     fun inverse(x:Float,y:Float):Point = when(rotation) { 90->Point(y,1-x);180->Point(1-x,1-y);270->Point(1-y,x);else->Point(x,y) }
     Hand(result.handedness()[i].first().categoryName(),ps.map { inverse(it.x(),it.y()).copy(z=it.z()) },result.handedness()[i].first().score(),result.worldLandmarks()[i].map { Point(it.x(),it.y(),it.z()) },width.toFloat()/height)
    }
    onResult(cameraTimestamp,hands,Matrix(matrix),width,height,android.os.SystemClock.elapsedRealtime()-started,epoch)
   } finally { releaseInput();busy.set(false) }
  }.setErrorListener { releaseInput();busy.set(false);onError("Hand tracking stopped. Reopen the studio to retry.") }.build())
 @Synchronized private fun releaseInput() { image?.close();image=null;bitmap?.recycle();bitmap=null }
 fun analyze(proxy:ImageProxy,session:Int=0) {
  if(!busy.compareAndSet(false,true)) { proxy.close();return }
  try {
   epoch=session
   cameraTimestamp=proxy.imageInfo.timestamp/1_000_000
   started=android.os.SystemClock.elapsedRealtime()
   width=proxy.width;height=proxy.height;rotation=proxy.imageInfo.rotationDegrees
   matrix=Matrix(proxy.imageInfo.sensorToBufferTransformMatrix)
   val raw=proxy.toBitmap()
   bitmap=if(rotation==0) raw else Bitmap.createBitmap(raw,0,0,width,height,Matrix().apply { postRotate(rotation.toFloat()) },false).also { if(it!==raw) raw.recycle() }
   timestamp=maxOf(timestamp+1,android.os.SystemClock.uptimeMillis())
   image=BitmapImageBuilder(bitmap!!).build()
   task.detectAsync(image!!,timestamp)
  } catch(e:Exception) { releaseInput();busy.set(false);onError("Could not process this camera frame.") }
  finally { proxy.close() }
 }
 override fun close() { task.close();releaseInput() }
}
