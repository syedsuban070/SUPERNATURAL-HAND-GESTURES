package com.hyperpixelacity.app.feature.studio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.hyperpixelacity.app.core.model.Effect
import kotlin.math.cos
import kotlin.math.sin

/** Original vector illustrations, drawn locally with no image downloads. */
@Composable fun EffectArtwork(effect:Effect,modifier:Modifier=Modifier) {
 Canvas(modifier.fillMaxWidth().height(112.dp)) {
  val c=Color(effect.color);val mid=center;val r=size.minDimension*.31f
  drawRect(Brush.radialGradient(listOf(c.copy(alpha=.16f),Color(0xFF171A1D)),center=mid,radius=size.width*.65f))
  when(effect.id) {
   0 -> {
    val path=Path().apply {moveTo(mid.x-r*1.4f,mid.y+r*.45f);cubicTo(mid.x-r,mid.y-r*1.9f,mid.x+r*.2f,mid.y+r*1.8f,mid.x+r*1.4f,mid.y-r*.5f)}
    drawPath(path,c.copy(alpha=.15f),style=Stroke(18f));drawPath(path,c,style=Stroke(5f));drawPath(path,Color.White,style=Stroke(1.5f))
   }
   6,9 -> {
    val path=Path().apply {moveTo(mid.x,mid.y-r);lineTo(mid.x+r*.65f,mid.y);lineTo(mid.x,mid.y+r);lineTo(mid.x-r*.65f,mid.y);close()}
    drawPath(path,Brush.linearGradient(listOf(Color.White,c,c.copy(alpha=.25f)),Offset(mid.x-r,mid.y-r),Offset(mid.x+r,mid.y+r)))
    drawLine(Color.White.copy(alpha=.6f),Offset(mid.x,mid.y-r),Offset(mid.x,mid.y+r),1.5f)
   }
   4 -> {
    for(i in 0..5) {
     val a=i*1.047f;val path=Path().apply {moveTo(mid.x,mid.y)
      for(j in 1..5){val d=j*r*.25f;val t=a+if(j%2==0).16f else -.12f;lineTo(mid.x+cos(t)*d,mid.y+sin(t)*d)}}
     drawPath(path,c.copy(alpha=.2f),style=Stroke(9f));drawPath(path,c,style=Stroke(2f))
    }
   }
   else -> {
    if(effect.id in 1..3) drawCircle(Brush.radialGradient(listOf(Color.White,c,c.copy(alpha=.04f)),mid,r),r,mid)
    drawCircle(c,r,mid,style=Stroke(2f));drawCircle(c.copy(alpha=.4f),r*1.2f,mid,style=Stroke(1f))
    if(effect.id==5 || effect.id==8) {drawCircle(c,r*.72f,mid,style=Stroke(1f));for(i in 0..5) {val a=i*1.047f;val z=a+2.094f;drawLine(c.copy(alpha=.6f),Offset(mid.x+cos(a)*r,mid.y+sin(a)*r),Offset(mid.x+cos(z)*r,mid.y+sin(z)*r),1f)}}
    if(effect.id==7) drawCircle(Color(0xFF0B0D11),r*.8f,mid)
   }
  }
  for(i in 0..7){val a=i*2.4f;drawCircle(c.copy(alpha=.65f),if(i%3==0)2f else 1f,Offset(mid.x+cos(a)*r*1.5f,mid.y+sin(a)*r*1.35f))}
 }
}
