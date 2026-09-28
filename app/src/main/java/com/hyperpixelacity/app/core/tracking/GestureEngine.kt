package com.hyperpixelacity.app.core.tracking
import com.hyperpixelacity.app.core.model.*
import kotlin.math.exp
import kotlin.math.hypot

/** Pure Kotlin. All thresholds use monotonic milliseconds, not frame counts. */
class GestureEngine {
 private var previous = Scene()
 private var entered = 0L
 private var pinchCandidate = 0L
 private var pinchExit = 0L
 private var pinched = false
 private var velocity = Point(0f,0f)
 private var launch = Point(.5f,.5f)
 private var trail = ArrayDeque<Pair<Point,Long>>()
 private var openSince = 0L
 fun reset() { previous=Scene(); entered=0; pinchCandidate=0; pinchExit=0; pinched=false; trail.clear(); openSince=0; velocity=Point(0f,0f) }
 fun update(time: Long, input: List<Hand>, settings: StudioSettings): Scene {
  if(time <= previous.timeMs) return previous
  val dt=(time-previous.timeMs).coerceIn(1,100)/1000f
  val hands=input.filter { it.valid }.sortedBy { it.id }.take(2).map { h ->
   val old=previous.hands.find { it.id==h.id }
   if(old==null || time-previous.timeMs>200 || old.palm.distance(h.palm)>.3f) h else h.copy(points=h.points.mapIndexed { i,p -> old.points[i].mix(p,1-exp(-dt*24)) })
  }
  val h=hands.firstOrNull()
  if(h?.id!=previous.hands.firstOrNull()?.id || (h!=null && previous.hands.isNotEmpty() && h.palm.distance(previous.hands.first().palm)>.3f)) { trail.clear();pinched=false;pinchCandidate=0;pinchExit=0;velocity=Point(0f,0f) }
  if(h==null) { pinched=false; pinchCandidate=0; pinchExit=0; openSince=0 }
  else {
   if(!pinched) {
    if(h.pinchRatio<.25f) { if(pinchCandidate==0L) pinchCandidate=time; if(time-pinchCandidate>=90) pinched=true } else pinchCandidate=0
   } else {
    if(h.pinchRatio>.38f) { if(pinchExit==0L) pinchExit=time; if(time-pinchExit>=70) { pinched=false; pinchCandidate=0 } } else pinchExit=0
   }
   if(h.open) { if(openSince==0L) openSince=time } else openSince=0
  }
  var state=previous.state
  var center=previous.center
  var radius=previous.radius
  var charge=0f
  fun transition(next: OrbState) { state=next; entered=time }
  if(state==OrbState.PROJECTILE) {
   val age=(time-entered)/1000f
   center=Point(launch.x+velocity.x*age,launch.y+velocity.y*age)
   charge=(1-age/1.2f).coerceIn(0f,1f)
   radius=(previous.radius*.98f).coerceAtLeast(.025f)
   if(age>=1.2f) transition(OrbState.COOLDOWN)
  } else if(state==OrbState.COOLDOWN) { if(time-entered>=800) transition(OrbState.IDLE) }
  else if(settings.effect in 1..3) {
   val pair=hands.size==2 && hands.all { it.open }
   if(pair) {
    val a=hands[0].palm;val b=hands[1].palm
    val next=Point((a.x+b.x)/2,(a.y+b.y)/2)
    radius=(hands[0].distance(a,b)*.38f).coerceIn(.035f,.22f)
    if(state==OrbState.IDLE || state==OrbState.LOST) transition(OrbState.CANDIDATE)
    if(state==OrbState.CANDIDATE && time-entered>=150) transition(OrbState.CHARGING)
    if(state==OrbState.CHARGING) { charge=((time-entered)/450f).coerceIn(0f,1f);if(charge>=1) transition(OrbState.HELD) }
    if(state==OrbState.HELD) {
     charge=1f
     val v=Point((next.x-center.x)/dt,(next.y-center.y)/dt)
     velocity=velocity.mix(v,.4f)
     if(hypot(velocity.x,velocity.y)>1.6f && time-entered>=200) { launch=next;transition(OrbState.PROJECTILE) }
    }
    center=next
   } else {
    if(state!=OrbState.IDLE && state!=OrbState.LOST) transition(OrbState.LOST)
    if(state==OrbState.LOST && time-entered>250) transition(OrbState.IDLE)
   }
  } else {
   state=OrbState.IDLE
   if(h!=null) {
    center=if(settings.effect==6 || settings.effect==9) h.points[4].mix(h.points[8],.5f) else h.palm
    radius=h.width*.75f
    charge=if(settings.effect==6 || settings.effect==9) { if(pinched) 1f else 0f } else if(openSince>0 && time-openSince>=120) 1f else 0f
   }
  }
  while(trail.isNotEmpty() && time-trail.first().second>settings.trailSeconds*1000) trail.removeFirst()
  if(settings.effect==0 && h?.indexExtended==true) {
   val p=h.points[8]
   if(trail.isEmpty() || trail.last().first.distance(p)>.003f) trail.addLast(p to time)
  } else if(h==null) trail.clear()
  while(trail.size>32) trail.removeFirst()
  previous=Scene(time,hands,center,radius,charge,state,pinched,trail.map { it.first },trail.map { it.second })
  return previous
 }
}
