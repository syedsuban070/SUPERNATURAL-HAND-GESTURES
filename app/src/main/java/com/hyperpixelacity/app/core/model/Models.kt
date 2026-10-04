package com.hyperpixelacity.app.core.model
import kotlin.math.hypot

data class Point(val x: Float, val y: Float, val z: Float = 0f) {
    fun distance(p: Point) = hypot(x-p.x,y-p.y)
    fun mix(p: Point, a: Float) = Point(x+(p.x-x)*a,y+(p.y-y)*a,z+(p.z-z)*a)
}
data class Hand(val id: String, val points: List<Point>, val handednessScore: Float, val world: List<Point> = emptyList(), val aspect: Float = 1f) {
    val palm get() = listOf(0,5,9,13,17).map { points[it] }.let { ps -> Point(ps.sumOf { it.x.toDouble() }.toFloat()/5,ps.sumOf { it.y.toDouble() }.toFloat()/5) }
    fun distance(a: Point, b: Point) = hypot(a.x-b.x, (a.y-b.y)/aspect.coerceAtLeast(.01f))
    val width get() = distance(points[5], points[17]).coerceAtLeast(0.001f)
    val pinchRatio get() = distance(points[4],points[8])/width
    val open get() = points.all { it.x in 0f..1f && it.y in 0f..1f } && listOf(8,12,16,20).count { distance(points[it],points[0]) > distance(points[it-2],points[0])*1.15f } >= 4
    val indexExtended get() = distance(points[8],points[0]) > distance(points[6],points[0])*1.15f
    val valid get() = points.size == 21 && points.all { it.x.isFinite() && it.y.isFinite() && it.z.isFinite() && it.x in -0.1f..1.1f && it.y in -0.1f..1.1f }
}
enum class OrbState { IDLE, CANDIDATE, CHARGING, HELD, PROJECTILE, COOLDOWN, LOST }
data class Scene(val timeMs: Long = 0, val hands: List<Hand> = emptyList(), val center: Point = Point(.5f,.5f), val radius: Float = .1f, val charge: Float = 0f, val state: OrbState = OrbState.IDLE, val pinched: Boolean = false, val trail: List<Point> = emptyList(), val trailTimes: List<Long> = emptyList())
data class Effect(val id: Int, val name: String, val category: String, val color: Long, val guide: String)
object Effects {
 val all = listOf(
 Effect(0,"White Light Trail","Trails",0xFFEDEFFF,"Extend your index finger and draw in the air."),
 Effect(1,"Blue Plasma Orb","Orbs",0xFF65B6FF,"Hold both palms open to charge. Flick to launch."),
 Effect(2,"Orange Fire Orb","Orbs",0xFFFF9A50,"Hold both palms open to charge. Flick to launch."),
 Effect(3,"Purple Cosmic Sphere","Orbs",0xFFB49AFF,"Hold both palms open to charge. Flick to launch."),
 Effect(4,"Cyan Lightning Hand","Elements",0xFF70E9E5,"Open your palm and hold it steady."),
 Effect(5,"Golden Magic Circle","Elements",0xFFEBC575,"Open your palm to reveal a rotating seal."),
 Effect(6,"Ice Crystal Summon","Objects",0xFFB4E7FF,"Pinch to summon and move the crystal."),
 Effect(7,"Dark Portal","Portals",0xFFAB91D9,"Open your palm to reveal a portal."),
 Effect(8,"Energy Shield","Elements",0xFF9CD4B9,"Open your palm to summon a shield."),
 Effect(9,"Simple Summoned Crystal","Objects",0xFFDDB8DE,"Pinch to summon and move the crystal.")
 )
}
data class StudioSettings(val effect: Int = 0, val intensity: Float = 1f, val size: Float = 1f, val glow: Float = 1f, val density: Float = .5f, val trailSeconds: Float = 1f, val quality: Int = 1, val mirror: Boolean = true, val reduceMotion: Boolean = false, val debug: Boolean = false, val countdown: Int = 0, val hue: Long = 0L, val onboarded: Boolean = false, val favorites: Set<String> = emptySet())
