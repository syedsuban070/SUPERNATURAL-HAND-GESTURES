package com.hyperpixelacity.app.feature.studio

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.camera.view.PreviewView
import androidx.navigation.compose.*
import com.hyperpixelacity.app.core.camera.CameraStudio
import com.hyperpixelacity.app.core.model.*
import com.hyperpixelacity.app.core.media.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HyperAppScreen(vm:StudioViewModel) {
 val settings by vm.settings.collectAsStateWithLifecycle()
 val nav=rememberNavController()
 Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
  NavHost(nav,startDestination="studio") {
   composable("studio") {
    if(!settings.onboarded) Welcome { vm.save(settings.copy(onboarded=true)) }
    else Studio(settings,vm::save,{ nav.navigate(it) })
   }
   composable("effects") { EffectLibrary(settings,vm::save) { nav.popBackStack() } }
   composable("editor") { Editor(settings,vm::save) { nav.popBackStack() } }
   composable("settings") { Preferences(settings,vm::save) { nav.popBackStack() } }
   composable("recordings") { Gallery { nav.popBackStack() } }
  }
 }
}
@Composable private fun Welcome(enter:()->Unit) {
 Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(28.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
  Spacer(Modifier.height(32.dp))
  Text("H / P",style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.primary)
  Text("A little magic.\nMade by you.",style=MaterialTheme.typography.displaySmall)
  Text("Turn a gesture into a cinematic moment. Your camera is the canvas.",style=MaterialTheme.typography.bodyLarge)
  Guide("01", "Trace the air", "Extend your index finger to draw a light trail.")
  Guide("02", "Hold the energy", "Show two open palms to charge an orb. Flick together to launch.")
  Guide("03", "Make it yours", "Pinch to summon a crystal. Explore colors and effects.")
  Text("Your camera and hand tracking stay on this phone. No account, uploads or microphone access. These are fictional digital effects.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
  Button(enter,Modifier.fillMaxWidth().heightIn(min=56.dp)) { Text("Enter the studio") }
 }
}
@Composable private fun Guide(number:String,title:String,body:String) {
 Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
  Text(number,color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.titleMedium)
  Column { Text(title,style=MaterialTheme.typography.titleMedium);Text(body,color=MaterialTheme.colorScheme.onSurfaceVariant) }
 }
}
@Composable private fun Studio(s:StudioSettings,save:(StudioSettings)->Unit,navigate:(String)->Unit) {
 val context=LocalContext.current
 val owner=LocalLifecycleOwner.current
 var allowed by remember { mutableStateOf(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) }
 var denied by rememberSaveable { mutableStateOf(false) }
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed=it;denied=!it }
 DisposableEffect(owner) {
  val observer=LifecycleEventObserver { _,event -> if(event==Lifecycle.Event.ON_RESUME) allowed=ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED }
  owner.lifecycle.addObserver(observer);onDispose { owner.lifecycle.removeObserver(observer) }
 }
 if(!allowed) {
  Column(Modifier.fillMaxSize().safeDrawingPadding().padding(28.dp),verticalArrangement=Arrangement.Center) {
   Text("Your hands, in focus.",style=MaterialTheme.typography.headlineLarge)
   Spacer(Modifier.height(16.dp));Text("Allow camera access for live, on-device hand effects. Nothing is uploaded.")
   Spacer(Modifier.height(24.dp));Button({ permission.launch(Manifest.permission.CAMERA) }) { Text("Allow camera") }
   if(denied)TextButton({ context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${context.packageName}"))) }) { Text("Open app permissions") }
   TextButton({ navigate("effects") }) { Text("Explore effects") }
  };return
 }
 var focus by rememberSaveable { mutableStateOf(false) }
 var front by rememberSaveable { mutableStateOf(true) }
 var retry by remember { mutableIntStateOf(0) }
 val camera=remember(retry) { CameraStudio(context) }
 val preview=remember(camera) { PreviewView(context).apply { implementationMode=PreviewView.ImplementationMode.COMPATIBLE;scaleType=PreviewView.ScaleType.FIT_CENTER } }
 val status by camera.status.collectAsStateWithLifecycle()
 val scope=rememberCoroutineScope()
 var count by remember { mutableIntStateOf(0) }
 var countdownJob by remember { mutableStateOf<Job?>(null) }
 LaunchedEffect(s,camera) { camera.configure(s) }
 DisposableEffect(camera,owner) {
  camera.configure(s)
  val observer=LifecycleEventObserver { _,event -> if(event==Lifecycle.Event.ON_STOP) { countdownJob?.cancel();count=0;camera.stop() } }
  owner.lifecycle.addObserver(observer)
  onDispose { countdownJob?.cancel();count=0;owner.lifecycle.removeObserver(observer);camera.close() }
 }
 LaunchedEffect(camera,owner,front,s.mirror,s.quality) { camera.bind(owner,preview,front) }
 val busy=status.switching || status.recording || status.finalizing || count>0
 val header: @Composable () -> Unit = {
   Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
     Surface(color=Color(0xDD202326),shape=RoundedCornerShape(24.dp)) { Text("HYPERPIXELACITY",Modifier.padding(14.dp),style=MaterialTheme.typography.labelLarge) }
     TextButton({ focus=!focus }) { Text(if(focus)"Show controls" else "Focus",color=Color.White) }
    }
    Surface(color=Color(0xDD202326),shape=RoundedCornerShape(16.dp)) {
     Text(if(status.recording) "REC  ${status.duration}s / 60s" else if(status.finalizing) "Saving video…" else status.gesture,Modifier.padding(12.dp),style=MaterialTheme.typography.labelMedium)
    }
    if(s.effect in 1..3 && status.charge>0 && !status.recording) LinearProgressIndicator(progress={status.charge},modifier=Modifier.width(180.dp))
    status.error?.let { error -> Surface(color=MaterialTheme.colorScheme.errorContainer,shape=RoundedCornerShape(16.dp)) {
     Column(Modifier.padding(12.dp)) { Text(error,color=MaterialTheme.colorScheme.onErrorContainer);if(!busy)TextButton({ retry++ }) { Text("Retry studio") } }
    } }
    if(s.debug) Surface(color=Color(0xDD202326)) { Text("Flip ${status.switchMs} ms · ${status.trackingHz} updates/s · ${status.inferenceMs} ms processing\n${status.hands} hands · ${listOf("Low","Balanced","High")[s.quality]} · depth unavailable",Modifier.padding(10.dp),style=MaterialTheme.typography.labelSmall) }
   }
 }
 val controls: @Composable () -> Unit = {
   Surface(color=Color(0xEC202326),shape=RoundedCornerShape(28.dp)) {
    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
     Text(Effects.all[s.effect].name,style=MaterialTheme.typography.titleLarge)
     if(!focus) Text(Effects.all[s.effect].guide,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
     if(!focus) LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { items(Effects.all) { e -> FilterChip(s.effect==e.id,{save(s.copy(effect=e.id,hue=0))},{Text(e.name)},enabled=!busy) } }
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically) {
      TextButton({ front=!front },enabled=!busy) { Text(if(status.switching)"Switching…" else "Flip") }
      Button({
       if(count>0) { countdownJob?.cancel();count=0 }
       else if(status.recording)camera.stop()
       else countdownJob=scope.launch { count=s.countdown;while(count>0){delay(1000);count--};camera.record() }
      },Modifier.heightIn(min=64.dp).widthIn(min=116.dp),enabled=status.ready && !status.finalizing) { Text(if(status.recording)"Stop" else if(count>0)"Cancel" else "Record") }
      TextButton({navigate("editor")},enabled=!busy) { Text("Tune") }
     }
     if(!focus) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
      TextButton({navigate("effects")},enabled=!busy) { Text("Effects") }
      TextButton({navigate("settings")},enabled=!busy) { Text("Settings") }
      TextButton({navigate("recordings")},enabled=!busy) { Text(if(status.saved!=null)"View saved clip" else "My clips") }
     }
     Text("Fictional digital effects · Silent video",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
   } }
 BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
  key(camera) { AndroidView({ preview },Modifier.fillMaxSize()) }
  if(maxWidth > maxHeight) {
   Row(Modifier.fillMaxSize().safeDrawingPadding().padding(12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { header();if(count>0)Text(count.toString(),style=MaterialTheme.typography.displayLarge) }
    Column(Modifier.width(320.dp).fillMaxHeight().verticalScroll(rememberScrollState())) { controls() }
   }
  } else {
   Column(Modifier.fillMaxSize().safeDrawingPadding().padding(12.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.SpaceBetween) {
    header()
    if(count>0)Text(count.toString(),Modifier.align(Alignment.CenterHorizontally),style=MaterialTheme.typography.displayLarge,color=Color.White)
    Spacer(Modifier.height(24.dp))
    controls()
   }
  }
 }
}

@Composable private fun Page(title:String,back:()->Unit,content:@Composable ColumnScope.()->Unit) {
 Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal=20.dp)) {
  Row(Modifier.fillMaxWidth().padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
   TextButton(back) { Text("Back") };Text(title,style=MaterialTheme.typography.headlineSmall)
  };content()
 }
}
@Composable private fun EffectLibrary(s:StudioSettings,save:(StudioSettings)->Unit,back:()->Unit) {
 var category by rememberSaveable { mutableStateOf("All") }
 Page("Discover effects",back) {
  Text("Small gestures. Extraordinary moments.",color=MaterialTheme.colorScheme.onSurfaceVariant)
  LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { items(listOf("All","Favorites","Trails","Orbs","Elements","Portals","Objects")) { c-> FilterChip(category==c,{category=c},{Text(c)}) } }
  LazyVerticalGrid(GridCells.Adaptive(150.dp),verticalArrangement=Arrangement.spacedBy(12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
   items(Effects.all.filter { category=="All" || it.category==category || category=="Favorites" && it.id.toString() in s.favorites }) { e ->
    Card(Modifier.fillMaxWidth()) {
     Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
      EffectArtwork(e)
      Text(e.name,style=MaterialTheme.typography.titleMedium)
      Text(e.category,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
      Button({save(s.copy(effect=e.id,hue=0));back()},Modifier.fillMaxWidth()) {Text("Use effect")}
      TextButton({val key=e.id.toString();save(s.copy(favorites=if(key in s.favorites)s.favorites-key else s.favorites+key))}) { Text(if(e.id.toString() in s.favorites)"Unfavorite" else "Favorite") }
     }
    }
   }
  }
 }
}
@Composable private fun Editor(s:StudioSettings,save:(StudioSettings)->Unit,back:()->Unit) {
 var draft by remember { mutableStateOf(s) }
 Page("Tune your effect",back) {
  Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
   Text(Effects.all[draft.effect].name,style=MaterialTheme.typography.titleLarge)
   EffectArtwork(Effects.all[draft.effect])
   Text("Start with a look, then make it yours.",color=MaterialTheme.colorScheme.onSurfaceVariant)
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    OutlinedButton({draft=draft.copy(intensity=.65f,glow=.5f,density=.2f,size=.8f)}) {Text("Soft")}
    OutlinedButton({draft=draft.copy(intensity=1f,glow=1f,density=.5f,size=1f)}) {Text("Studio")}
    OutlinedButton({draft=draft.copy(intensity=1.4f,glow=1.5f,density=.8f,size=1.2f)}) {Text("Epic")}
   }
   Adjust("Intensity",draft.intensity,.2f..2f){draft=draft.copy(intensity=it)}
   Adjust("Size",draft.size,.4f..2f){draft=draft.copy(size=it)}
   Adjust("Glow",draft.glow,0f..2f){draft=draft.copy(glow=it)}
   Adjust("Spark density",draft.density,0f..1f){draft=draft.copy(density=it)}
   if(draft.effect==0)Adjust("Trail length (seconds)",draft.trailSeconds,.3f..2f){draft=draft.copy(trailSeconds=it)}
   Text("Color",style=MaterialTheme.typography.titleMedium)
   LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp)) {items(Effects.all.take(6)) { e->
    Box(Modifier.size(52.dp).clip(CircleShape).background(Color(e.color)).clickable{draft=draft.copy(hue=e.color)}.semantics{contentDescription="Use ${e.name} color"})
   } }
   Text("Gesture: ${Effects.all[draft.effect].guide}",color=MaterialTheme.colorScheme.onSurfaceVariant)
   Button({save(draft);back()},Modifier.fillMaxWidth().heightIn(min=52.dp)) {Text("Save adjustments")}
   TextButton({draft=draft.copy(intensity=1f,size=1f,glow=1f,density=.5f,trailSeconds=1f,hue=0)}){Text("Reset adjustments")}
   Spacer(Modifier.height(20.dp))
  }
 }
}
@Composable private fun Adjust(label:String,value:Float,range:ClosedFloatingPointRange<Float>,change:(Float)->Unit) {
 Column { Text("$label  ${"%.1f".format(value)}");Slider(value,change,valueRange=range,modifier=Modifier.semantics {contentDescription=label}) }
}
@Composable private fun Preferences(s:StudioSettings,save:(StudioSettings)->Unit,back:()->Unit) {
 Page("Settings",back) {
  Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)) {
   Text("Performance",style=MaterialTheme.typography.titleLarge)
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("Low","Balanced","High").forEachIndexed { i,label-> FilterChip(s.quality==i,{save(s.copy(quality=i))},{Text(label)}) }}
   Text("Low reduces tracking frequency and disables sparks. Recording uses a supported 720p format where available.",style=MaterialTheme.typography.bodySmall)
   Toggle("Mirror selfie preview and video",s.mirror){save(s.copy(mirror=it))}
   Toggle("Reduce effect animation",s.reduceMotion){save(s.copy(reduceMotion=it))}
   Text("Adaptive hand smoothing steadies slow gestures and follows faster movements. Keep your full hand visible for palm effects.",style=MaterialTheme.typography.bodySmall)
   Toggle("Tracking debug overlay",s.debug){save(s.copy(debug=it))}
   Text("Record countdown",style=MaterialTheme.typography.titleMedium)
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf(0,3,5).forEach { n->FilterChip(s.countdown==n,{save(s.copy(countdown=n))},{Text(if(n==0)"Off" else "$n seconds")}) } }
   HorizontalDivider()
   Text("Private by design",style=MaterialTheme.typography.titleLarge)
   Text("Hand tracking runs on this phone. This app has no network or microphone permission. Videos are saved to Movies/Hyperpixelacity. Only Share sends a clip to an app you choose.")
   Text("Depth and finger occlusion are not available in this preview. Effects may appear in front of your hands. Camera motion and dim light can reduce tracking quality.")
   HorizontalDivider();Text("Hyperpixelacity • 0.2.1",style=MaterialTheme.typography.titleMedium)
   Text("Create cinematic powers with your hands.\nMade for Evidence Of One.\nFictional digital effects. Not supernatural abilities.")
   Text("Built with AndroidX, Kotlin, Hilt and MediaPipe. Third-party notices are included in the source repository.",style=MaterialTheme.typography.bodySmall)
   Spacer(Modifier.height(28.dp))
  }
 }
}
@Composable private fun Toggle(label:String,value:Boolean,change:(Boolean)->Unit) {
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {Text(label,Modifier.weight(1f));Switch(value,change,modifier=Modifier.semantics{contentDescription=label})}
}
@Composable private fun Gallery(back:()->Unit) {
 val context=LocalContext.current
 val repo=remember { Recordings(context) }
 val scope=rememberCoroutineScope()
 var clips by remember {mutableStateOf<List<Clip>>(emptyList())}
 var selected by remember {mutableStateOf<Clip?>(null)}
 var deleting by remember {mutableStateOf<Clip?>(null)}
 var error by remember {mutableStateOf<String?>(null)}
 var rename by remember {mutableStateOf("")}
 LaunchedEffect(Unit) {runCatching{repo.list()}.onSuccess{clips=it}.onFailure{error="Could not load your clips."}}
 Page("My creations",back) {
  error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
  val clip=selected
  if(clip!=null) {
   val player=remember(clip.uri) {VideoView(context).apply {setVideoURI(clip.uri);setMediaController(MediaController(context));setOnPreparedListener{it.isLooping=true;start()}}}
   DisposableEffect(player) {onDispose{player.stopPlayback()}}
   AndroidView({player},Modifier.fillMaxWidth().weight(1f))
   OutlinedTextField(rename,{rename=it},label={Text("Clip title")},modifier=Modifier.fillMaxWidth())
   Row {TextButton({scope.launch {runCatching{repo.rename(clip.uri,rename);clips=repo.list();selected=null}.onFailure{error="Could not rename this clip."}}}){Text("Save title")}
    TextButton({runCatching{repo.share(clip.uri)}.onFailure{error="No sharing app available."}}){Text("Share")}
    TextButton({deleting=clip}){Text("Delete")}}
   TextButton({selected=null}){Text("All clips")}
  } else if(clips.isEmpty()) {Spacer(Modifier.height(40.dp));Text("Your first creation belongs here.",style=MaterialTheme.typography.headlineSmall);Text("Record a moment in the studio. Saved clips appear automatically.")}
  else LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)) {items(clips,key={it.uri.toString()}) { c-> Card(Modifier.fillMaxWidth().clickable{selected=c;rename=c.name.removeSuffix(".mp4")}) {Column(Modifier.padding(20.dp)){Text(c.name,style=MaterialTheme.typography.titleMedium);Text("${c.duration/1000}s · Tap to play",color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}
 }
 deleting?.let {c-> AlertDialog(onDismissRequest={deleting=null},title={Text("Delete this clip?")},text={Text("This removes the saved video from your phone.")},confirmButton={TextButton({scope.launch{runCatching{repo.delete(c.uri);clips=repo.list();selected=null;deleting=null}.onFailure{error="Could not delete this clip.";deleting=null}}}){Text("Delete")}},dismissButton={TextButton({deleting=null}){Text("Keep")}}) }
}
