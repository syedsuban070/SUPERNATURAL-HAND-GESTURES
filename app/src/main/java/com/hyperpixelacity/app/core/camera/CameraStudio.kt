package com.hyperpixelacity.app.core.camera
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Size
import android.view.Surface
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.hyperpixelacity.app.core.graphics.MagicProcessor
import com.hyperpixelacity.app.core.model.*
import com.hyperpixelacity.app.core.tracking.*
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CameraStatus(val ready:Boolean=false,val trackingHz:Int=0,val inferenceMs:Long=0,val charge:Float=0f,val hands:Int=0,val gesture:String="Show your hands",val recording:Boolean=false,val finalizing:Boolean=false,val duration:Long=0,val error:String?=null,val saved:Uri?=null)
class CameraStudio(private val context:Context) : AutoCloseable {
 private val main=ContextCompat.getMainExecutor(context)
 private val analysis=Executors.newSingleThreadExecutor()
 private val mutable=MutableStateFlow(CameraStatus())
 val status=mutable.asStateFlow()
 private var provider:ProcessCameraProvider?=null
 private var preview:Preview?=null
 private var analyzer:ImageAnalysis?=null
 private var capture:VideoCapture<Recorder>?=null
 private var recorder:Recording?=null
 private var processor:MagicProcessor?=null
 private var tracker:HandTracker?=null
 private var engine=GestureEngine()
 @Volatile private var settings=StudioSettings()
 @Volatile private var closed=false
 private var generation=0
 private var lastAnalyzed=0L
 private var lastEffect=-1
 private var lastResult=0L
 private var lastStatus=0L
 private var trackingHz=0f
 fun configure(s:StudioSettings) { settings=s;processor?.configure(s) }
 private fun fail(message:String) { main.execute { mutable.value=mutable.value.copy(error=message) } }
 @androidx.annotation.OptIn(markerClass = [ExperimentalMirrorMode::class])
 fun bind(owner:LifecycleOwner,view:PreviewView,front:Boolean) {
  val ticket=++generation
  val future=ProcessCameraProvider.getInstance(context)
  future.addListener({
   if(closed || ticket!=generation)return@addListener
   try {
    provider=future.get()
    val selector=if(front)CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
    if(!provider!!.hasCamera(selector)) { fail("This camera is unavailable.");return@addListener }
    processor=MagicProcessor(context,::fail)
    analysis.execute {
     try {
      tracker=HandTracker(context,{ time,hands,matrix,w,h,latency ->
       if(lastEffect!=settings.effect) { engine.reset();lastEffect=settings.effect }
       val scene=engine.update(time,hands,settings)
       processor?.update(scene,settings,matrix,w,h)
       if(lastResult>0 && time>lastResult) trackingHz=trackingHz*.8f+(.2f*1000f/(time-lastResult))
       lastResult=time
       if(time-lastStatus>=100) {
        lastStatus=time
        val message=when {
         scene.hands.isEmpty()->"Show your hands in the frame"
         settings.effect in 1..3 && scene.hands.size<2->"Bring both open palms into view"
         scene.state==OrbState.CHARGING->"Hold steady · charging"
         scene.state==OrbState.HELD->"Energy ready · move together to launch"
         scene.state==OrbState.PROJECTILE->"Launched"
         scene.state==OrbState.COOLDOWN->"Recharging"
         scene.pinched->"Pinch held · move your crystal"
         settings.effect==0->"Point your index finger to draw"
         settings.effect==6 || settings.effect==9->"Touch thumb and index to summon"
         scene.charge>0->"Power active"
         else->"Open your palm and hold steady"
        }
        main.execute { if(!closed) mutable.value=mutable.value.copy(hands=scene.hands.size,gesture=message,trackingHz=trackingHz.toInt(),inferenceMs=latency,charge=scene.charge) }
       }
      },::fail)
     } catch(e:Exception) { fail("The hand model could not start on this device.") }
     catch(e:LinkageError) { fail("Hand tracking is not supported by this device CPU.") }
    }
    val mirror=if(settings.mirror) MirrorMode.MIRROR_MODE_ON_FRONT_ONLY else MirrorMode.MIRROR_MODE_OFF
    val rotation=view.display?.rotation?:Surface.ROTATION_0
    preview=Preview.Builder().setTargetRotation(rotation).setMirrorMode(mirror).build().also { it.setSurfaceProvider(view.surfaceProvider) }
    analyzer=ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
     .setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(ResolutionStrategy(Size(640,480),ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)).build())
     .setTargetRotation(rotation).build().also { use -> use.setAnalyzer(analysis) { image ->
      val time=image.imageInfo.timestamp/1_000_000
      val interval=when(settings.quality) { 0->66;2->33;else->42 }
      if(closed || time-lastAnalyzed<interval || tracker==null) image.close() else { lastAnalyzed=time;tracker?.analyze(image) }
     } }
    val desired=if(settings.quality==0) Quality.SD else Quality.HD
    val rec=Recorder.Builder().setQualitySelector(QualitySelector.from(desired,FallbackStrategy.lowerQualityOrHigherThan(desired))).build()
    capture=VideoCapture.Builder(rec).setTargetRotation(rotation).setMirrorMode(mirror).build()
    fun group(withVideo:Boolean)=UseCaseGroup.Builder().addUseCase(preview!!).addUseCase(analyzer!!).addEffect(processor!!.effect).apply { if(withVideo)addUseCase(capture!!) }.build()
    try { provider!!.bindToLifecycle(owner,selector,group(true)) }
    catch(e:Exception) {
     provider!!.unbind(preview!!,analyzer!!,capture!!)
     capture=null
     provider!!.bindToLifecycle(owner,selector,group(false))
     fail("Live effects available; this camera cannot combine tracking and recording.")
    }
    mutable.value=mutable.value.copy(ready=true)
   } catch(e:Exception) { fail("Camera could not start. Close other camera apps and retry.") }
  },main)
 }
 fun record() {
  if(closed || mutable.value.recording || mutable.value.finalizing)return
  val video=capture?:run { fail("Recording is unavailable for this camera configuration.");return }
  try {
   val values=ContentValues().apply {
    put(MediaStore.Video.Media.DISPLAY_NAME,"Hyperpixelacity_${System.currentTimeMillis()}.mp4")
    put(MediaStore.Video.Media.MIME_TYPE,"video/mp4")
    put(MediaStore.Video.Media.RELATIVE_PATH,"Movies/Hyperpixelacity")
   }
   val output=MediaStoreOutputOptions.Builder(context.contentResolver,MediaStore.Video.Media.EXTERNAL_CONTENT_URI).setContentValues(values).setDurationLimitMillis(60_000).build()
   mutable.value=mutable.value.copy(recording=true,duration=0,error=null,saved=null)
   recorder=video.output.prepareRecording(context,output).start(main) { event ->
    when(event) {
     is VideoRecordEvent.Status -> mutable.value=mutable.value.copy(duration=event.recordingStats.recordedDurationNanos/1_000_000_000)
     is VideoRecordEvent.Finalize -> {
      recorder=null
      val uri=event.outputResults.outputUri
      val valid=uri!=Uri.EMPTY && (event.error==VideoRecordEvent.Finalize.ERROR_NONE || event.error==VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED)
      if(!valid && uri!=Uri.EMPTY) runCatching { context.contentResolver.delete(uri,null,null) }
      mutable.value=mutable.value.copy(recording=false,finalizing=false,saved=if(valid)uri else null,error=if(valid)null else "Recording failed (${event.error}). Try Low quality or another camera.")
     }
    }
   }
  } catch(e:Exception) { mutable.value=mutable.value.copy(recording=false,finalizing=false,error="Unable to start recording. Check available storage.") }
 }
 fun stop() { if(recorder!=null) { mutable.value=mutable.value.copy(recording=false,finalizing=true);recorder?.stop() } }
 override fun close() {
  if(closed)return
  closed=true;generation++;stop();analyzer?.clearAnalyzer()
  listOfNotNull(preview,analyzer,capture).takeIf { it.isNotEmpty() }?.let { provider?.unbind(*it.toTypedArray()) }
  analysis.execute { tracker?.close();tracker=null }
  analysis.shutdown();processor?.close();processor=null
 }
}
