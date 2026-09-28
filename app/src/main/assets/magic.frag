#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require
precision highp float;
uniform samplerExternalOES uCamera;
uniform vec2 uCenter;
uniform float uAspect,uRadius,uTime,uCharge,uIntensity,uGlow,uDensity;
uniform vec3 uColor;
uniform int uEffect,uQuality,uTrailCount,uLandmarkCount;
uniform vec3 uTrail[32];
uniform vec2 uLandmarks[42];
in vec2 vCamera;
out vec4 fragColor;
float segment(vec2 p,vec2 a,vec2 b){vec2 v=b-a;return length(p-a-v*clamp(dot(p-a,v)/max(dot(v,v),0.000001),0.,1.));}
float halo(float d,float width){return exp(-abs(d)/max(width,.0001));}
mat2 rot(float t){return mat2(cos(t),-sin(t),sin(t),cos(t));}
float crystal(vec3 p){p.xz=rot(uTime*.5)*p.xz;return (abs(p.x)+abs(p.y)*.6+abs(p.z)-.72)*.577;}
void main(){
 vec3 camera=texture(uCamera,vCamera).rgb;
 vec2 p=(vCamera-uCenter)*vec2(uAspect,1.);
 float r=max(uRadius*uAspect,.008);
 float d=length(p),a=atan(p.y,p.x);
 vec3 fx=vec3(0.);
 float power=uCharge*uIntensity;
 if(uEffect==0){
  for(int i=1;i<32;i++){
   if(i>=uTrailCount)break;
   float dist=segment(vCamera*vec2(uAspect,1.),uTrail[i-1].xy*vec2(uAspect,1.),uTrail[i].xy*vec2(uAspect,1.));
   fx+=uTrail[i].z*(uColor*halo(dist,.006+uGlow*.005)*.45+vec3(halo(dist,.0018))*.65)*uIntensity;
  }
 }else if(power>.001 && d<r*4.) {
 if(uEffect>=1 && uEffect<=3){
  float noise=sin(a*9.+uTime*3.)*sin(a*13.-uTime*2.)*.04;
  float shell=halo(d-r*(.8+noise),r*.07);
  fx=uColor*(halo(d,r*.6)*uGlow+shell*1.8)+vec3(halo(d,r*.14))*2.;
  if(uEffect==2)fx+=vec3(1.,.3,.03)*halo(d-r*(.9+.12*sin(a*7.+uTime*6.)),r*.08);
  if(uEffect==3)fx+=uColor*halo(abs(p.y+.15*sin(p.x*22.+uTime)*r)-r*.3,r*.025)*halo(d,r);
  fx*=power;
 }else if(uEffect==4){
  for(int i=0;i<6;i++){float angle=float(i)*1.047+uTime*.12;vec2 q=rot(angle)*p;float jag=.08*r*sin(q.x/r*25.+uTime*12.)+.04*r*sin(q.x/r*59.-uTime*8.);fx+=(uColor*halo(q.y-jag,r*.045)+vec3(halo(q.y-jag,r*.012)))*step(0.,q.x)*step(q.x,r*1.8);}
  fx*=power;
 }else if(uEffect==5 || uEffect==8){
  float ring=halo(d-r,r*.025)+halo(d-r*.76,r*.018);
  float spokes=halo(sin(a*6.+uTime*.6)*d,r*.025)*step(r*.76,d)*step(d,r);
  if(uEffect==8)ring+=halo(d,r*.75)*(.2+.15*sin(p.x/r*32.)*sin(p.y/r*32.));
  fx=uColor*(ring+spokes)*power;
 }else if(uEffect==7){
  float edge=halo(d-r*(1.+.03*sin(a*18.+uTime*2.)),r*.06);
  camera*=1.-.9*power*(1.-smoothstep(r*.8,r,d));
  fx=(uColor*edge*2.+vec3(halo(d-r,r*.013)))*power;
 }else{
  vec3 origin=vec3(p/r,3.);vec3 ray=vec3(0.,0.,-1.);float t=0.;bool hit=false;
  for(int i=0;i<32;i++){float sd=crystal(origin+ray*t);if(sd<.006){hit=true;break;}t+=sd;if(t>6.)break;}
  if(hit){vec3 q=origin+ray*t;float e=.008;vec3 n=normalize(vec3(crystal(q+vec3(e,0,0))-crystal(q-vec3(e,0,0)),crystal(q+vec3(0,e,0))-crystal(q-vec3(0,e,0)),crystal(q+vec3(0,0,e))-crystal(q-vec3(0,0,e))));fx=(uColor*(.3+.7*max(dot(n,normalize(vec3(-1.,-1.,2.))),0.))+vec3(pow(max(n.z,0.),12.)))*power;camera*=1.-.6*power;}
  fx+=uColor*halo(d,r*.55)*.25*uGlow*power;
 }
 if(uEffect!=0 && power>.0 && uQuality>0){
  for(int i=0;i<16;i++){if(float(i)>uDensity*16.)break;float n=float(i);float ang=n*2.4+uTime*(.2+n*.02);float rr=r*(1.1+fract(uTime*.2+n*.17)*1.4);vec2 sp=vec2(cos(ang),sin(ang))*rr;fx+=uColor*halo(length(p-sp),r*.018)*power*.6;}
 }
 }
 for(int i=0;i<42;i++){if(i>=uLandmarkCount)break;float dd=length((vCamera-uLandmarks[i])*vec2(uAspect,1.));fx+=vec3(.3,1.,.65)*step(dd,.004);}
 fragColor=vec4(clamp(camera+fx,0.,1.),1.);
}
