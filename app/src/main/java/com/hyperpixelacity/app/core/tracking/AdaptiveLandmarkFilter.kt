package com.hyperpixelacity.app.core.tracking

import com.hyperpixelacity.app.core.model.Point
import kotlin.math.PI
import kotlin.math.abs

/** Speed-adaptive low-pass filtering (One Euro method, Casiez et al., CHI 2012).
 * Coordinates are normalized; derivative filtering prevents noise from opening the cutoff. */
class AdaptiveLandmarkFilter {
 private class Axis {
  var raw=0f; var value=0f; var derivative=0f; var initialized=false
  fun update(next:Float,dt:Float):Float {
   if(!initialized) { raw=next;value=next;initialized=true;return next }
   fun alpha(cutoff:Float)=1f/(1f+1f/(2f*PI.toFloat()*cutoff*dt))
   derivative+=alpha(1f)*((next-raw)/dt-derivative)
   raw=next
   value+=alpha(1.6f+4f*abs(derivative))*(next-value)
   return value
  }
 }
 private val axes=Array(21){Array(3){Axis()}}
 fun update(points:List<Point>,dt:Float):List<Point> = points.mapIndexed { i,p ->
  val a=axes[i]; val step=dt.coerceIn(.001f,.1f)
  Point(a[0].update(p.x,step),a[1].update(p.y,step),a[2].update(p.z,step))
 }
}
