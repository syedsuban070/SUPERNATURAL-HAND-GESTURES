package com.hyperpixelacity.app
import com.hyperpixelacity.app.core.model.*
import com.hyperpixelacity.app.core.tracking.GestureEngine
import org.junit.Assert.*
import org.junit.Test

class GestureEngineTest {
 private fun hand(id:String="Left",x:Float=.35f,pinch:Float=.8f):Hand {
  val p=MutableList(21){Point(x,.6f)}
  p[0]=Point(x,.8f)
  p[5]=Point(x-.08f,.6f);p[17]=Point(x+.08f,.6f)
  p[9]=Point(x-.025f,.57f);p[13]=Point(x+.025f,.57f)
  for(i in listOf(8,12,16,20)){p[i]=Point(x+(i-14)*.01f,.3f);p[i-2]=Point(x+(i-14)*.01f,.5f)}
  p[4]=Point(p[8].x+pinch*.16f,p[8].y)
  return Hand(id,p,.99f)
 }
 @Test fun emptyFramesNeverCharge() {
  val engine=GestureEngine()
  for(t in 100L..1500L step 50)assertEquals(0f,engine.update(t,emptyList(),StudioSettings(effect=1)).charge,0f)
 }
 @Test fun pinchRequiresDwellAndReleaseHysteresis() {
  val e=GestureEngine();val s=StudioSettings(effect=6)
  assertFalse(e.update(100,listOf(hand(pinch=.15f)),s).pinched)
  assertFalse(e.update(150,listOf(hand(pinch=.15f)),s).pinched)
  assertTrue(e.update(200,listOf(hand(pinch=.15f)),s).pinched)
  assertTrue(e.update(250,listOf(hand(pinch=.3f)),s).pinched)
  var result=e.update(300,listOf(hand(pinch=.7f)),s)
  for(t in 350L..600L step 50)result=e.update(t,listOf(hand(pinch=.7f)),s)
  assertFalse(result.pinched)
 }
 @Test fun lossCancelsPinchAndReacquisitionNeedsDwell() {
  val e=GestureEngine();val s=StudioSettings(effect=6)
  e.update(100,listOf(hand(pinch=.1f)),s);assertTrue(e.update(220,listOf(hand(pinch=.1f)),s).pinched)
  assertFalse(e.update(250,emptyList(),s).pinched)
  assertFalse(e.update(280,listOf(hand(pinch=.1f)),s).pinched)
 }
 @Test fun staleTimestampDoesNotChangeState() {
  val e=GestureEngine();val a=e.update(100,listOf(hand()),StudioSettings())
  assertEquals(a,e.update(90,emptyList(),StudioSettings()))
 }
 @Test fun orbRequiresTwoStableOpenHands() {
  val e=GestureEngine();val s=StudioSettings(effect=1)
  val pair=listOf(hand(),hand("Right",.7f))
  assertEquals(OrbState.CANDIDATE,e.update(100,pair,s).state)
  assertEquals(OrbState.CHARGING,e.update(250,pair,s).state)
  for(t in 300L..700L step 50)e.update(t,pair,s)
  val held=e.update(750,pair,s)
  assertEquals(OrbState.HELD,held.state);assertEquals(1f,held.charge,0f)
  assertEquals(OrbState.LOST,e.update(800,listOf(hand()),s).state)
  e.update(900,emptyList(),s);e.update(1000,emptyList(),s)
  assertEquals(OrbState.IDLE,e.update(1100,emptyList(),s).state)
 }
 @Test fun invalidCoordinatesAreRejected() {
  val h=hand();val broken=h.copy(points=h.points.map { Point(Float.NaN,it.y) })
  assertTrue(GestureEngine().update(100,listOf(broken),StudioSettings()).hands.isEmpty())
 }
 @Test fun trailIsBoundedAndClearsOnLoss() {
  val e=GestureEngine();val s=StudioSettings(trailSeconds=2f)
  var scene=Scene()
  for(i in 1..100)scene=e.update(i*20L,listOf(hand(x=.2f+(i%30)*.01f)),s)
  assertTrue(scene.trail.size<=32)
  assertTrue(e.update(2100,emptyList(),s).trail.isEmpty())
 }
 @Test fun resetRemovesAllHeldState() {
  val e=GestureEngine();e.update(100,listOf(hand(pinch=.1f)),StudioSettings(effect=6));e.update(220,listOf(hand(pinch=.1f)),StudioSettings(effect=6));e.reset()
  assertFalse(e.update(300,listOf(hand(pinch=.1f)),StudioSettings(effect=6)).pinched)
 }

 @Test fun longGapRequiresFreshPinchDwell() {
  val e=GestureEngine();val s=StudioSettings(effect=6)
  e.update(100,listOf(hand(pinch=.1f)),s)
  assertTrue(e.update(210,listOf(hand(pinch=.1f)),s).pinched)
  assertFalse(e.update(700,listOf(hand(pinch=.1f)),s).pinched)
 }
 @Test fun changingEffectResetsChargedObject() {
  val e=GestureEngine();val pair=listOf(hand(),hand("Right",.7f))
  for(t in 100L..900L step 50)e.update(t,pair,StudioSettings(effect=1))
  assertEquals(OrbState.CANDIDATE,e.update(950,pair,StudioSettings(effect=2)).state)
 }
 @Test fun partialPalmDoesNotCharge() {
  val e=GestureEngine();val h=hand();val p=h.points.toMutableList();p[20]=Point(1.05f,.3f)
  for(t in 100L..1200L step 50)assertEquals(0f,e.update(t,listOf(h.copy(points=p),hand("Right",.7f)),StudioSettings(effect=1)).charge,0f)
 }
 @Test fun adaptiveFilterReducesStationaryNoise() {
  val filter=com.hyperpixelacity.app.core.tracking.AdaptiveLandmarkFilter()
  var error=0f
  for(i in 0..99) { val x=.5f+if(i%2==0).01f else -.01f
   val p=filter.update(List(21){Point(x,.5f)},.033f)[0]
   if(i>20)error+=kotlin.math.abs(p.x-.5f)
  }
  assertTrue(error/79<.005f)
 }
 @Test fun adaptiveFilterFollowsFastMovement() {
  val filter=com.hyperpixelacity.app.core.tracking.AdaptiveLandmarkFilter()
  filter.update(List(21){Point(.2f,.5f)},.033f)
  var p=Point(.2f,.5f)
  repeat(6){p=filter.update(List(21){Point(.8f,.5f)},.033f)[0]}
  assertTrue(p.x>.72f)
 }
}
