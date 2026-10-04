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
  float closest=10.;float fade=0.;
  vec2 metric=vCamera*vec2(uAspect,1.);
  for(int i=1;i<32;i++){
   if(i>=uTrailCount)break;
   vec2 a=uTrail[i-1].xy*vec2(uAspect,1.);vec2 b=uTrail[i].xy*vec2(uAspect,1.);
   // Reject pixels outside each segment's glow bounds before distance work.
   if(any(lessThan(metric,min(a,b)-vec2(.055))) || any(greaterThan(metric,max(a,b)+vec2(.055))))continue;
   float dist=segment(metric,a,b)/mix(.4,1.,uTrail[i].z);
   if(dist<closest){closest=dist;fade=uTrail[i].z;}
  }
  float ribbon=fade*halo(closest,.004+uGlow*.007);
  float core=fade*halo(closest,.0015);
  fx=(uColor*ribbon*.85+vec3(core)*.9)*uIntensity;
 }else if(power>.001 && d<r*4.) {
 if(uEffect>=1 && uEffect<=3){
  float noise=sin(a*9.+uTime*3.)*sin(a*13.-uTime*2.)*.04;
  float shell=halo(d-r*(.8+noise),r*.07);
  fx=uColor*(halo(d,r*.6)*uGlow+shell*1.8)+vec3(halo(d,r*.14))*2.;
  if(uEffect==2)fx+=vec3(1.,.3,.03)*halo(d-r*(.9+.12*sin(a*7.+uTime*6.)),r*.08);
  if(uEffect==3)fx+=uColor*halo(abs(p.y+.15*sin(p.x*22.+uTime)*r)-r*.3,r*.025)*halo(d,r);
  // Layered flowing bands give the sphere depth without a mesh or extra textures.
  float band=sin(a*5.+d/r*12.-uTime*2.2)*sin(a*3.-d/r*7.+uTime);
  float rim=halo(d-r*.87,r*.022);
  float surface=clamp(1.-d*d/(r*r),0.,1.);
  float curved=sin(a*7.+sqrt(surface)*9.-uTime*2.)*sin(a*4.-uTime*1.3);
  float veins=pow(max(0.,curved),6.)*(1.-smoothstep(r*.4,r,d));
  fx+=uColor*(.18+.22*band)*halo(d-r*.55,r*.25)+vec3(rim)*.55+mix(uColor,vec3(1.),.5)*veins*.8;
  fx*=power;
 }else if(uEffect==4){
  for(int i=0;i<6;i++){float angle=float(i)*1.047+uTime*.12;vec2 q=rot(angle)*p;float jag=.08*r*sin(q.x/r*25.+uTime*12.)+.04*r*sin(q.x/r*59.-uTime*8.);fx+=(uColor*halo(q.y-jag,r*.045)+vec3(halo(q.y-jag,r*.012)))*step(0.,q.x)*step(q.x,r*1.8);}
  fx*=power;
 }else if(uEffect==5 || uEffect==8){
  float ring=halo(d-r,r*.025)+halo(d-r*.76,r*.018);
  float spokes=halo(sin(a*6.+uTime*.6)*d,r*.025)*step(r*.76,d)*step(d,r);
  if(uEffect==8)ring+=halo(d,r*.75)*(.2+.15*sin(p.x/r*32.)*sin(p.y/r*32.));
  float engraving=halo(sin(a*12.-uTime*.35)*d,r*.013)*step(r*.82,d)*step(d,r*.96);
  float outer=halo(d-r*1.16,r*.012)*(.45+.55*pow(abs(cos(a*8.+uTime*.4)),8.));
  fx=(uColor*(ring+spokes+engraving*.7+outer)+vec3(halo(d-r,r*.008))*.6)*power;
 }else if(uEffect==7){
  float edge=halo(d-r*(1.+.03*sin(a*18.+uTime*2.)),r*.06);
  camera*=1.-.9*clamp(power,0.,1.)*(1.-smoothstep(r*.8,r,d));
  fx=(uColor*edge*2.+vec3(halo(d-r,r*.013)))*power;
 }else{
  vec3 origin=vec3(p/r,3.);vec3 ray=vec3(0.,0.,-1.);float t=0.;bool hit=false;
  for(int i=0;i<32;i++){if(uQuality==0 && i>=16)break;float sd=crystal(origin+ray*t);if(sd<.006){hit=true;break;}t+=sd;if(t>6.)break;}
  if(hit){vec3 q=origin+ray*t;float e=.008;vec3 n=normalize(vec3(crystal(q+vec3(e,0,0))-crystal(q-vec3(e,0,0)),crystal(q+vec3(0,e,0))-crystal(q-vec3(0,e,0)),crystal(q+vec3(0,0,e))-crystal(q-vec3(0,0,e))));float fresnel=pow(1.-abs(n.z),3.);float spec=pow(max(dot(n,normalize(vec3(-.4,-.6,1.))),0.),24.);float facets=.5+.5*sin(dot(q,vec3(8.,11.,6.))+uTime*.6);fx=(uColor*(.3+.6*max(dot(n,normalize(vec3(-1.,-1.,2.))),0.)+.3*facets)+vec3(spec)*1.8+mix(uColor,vec3(1.),.55)*fresnel)*power;camera*=1.-.6*clamp(power,0.,1.);}
  fx+=uColor*halo(d,r*.55)*.25*uGlow*power;
 }
 if(uEffect!=0 && power>.0 && uQuality>0){
  for(int i=0;i<16;i++){if(float(i)>uDensity*16.)break;float n=float(i);float ang=n*2.4+uTime*(.2+n*.02);float rr=r*(1.1+fract(uTime*.2+n*.17)*1.4);vec2 sp=vec2(cos(ang),sin(ang))*rr;fx+=uColor*halo(length(p-sp),r*.018)*power*.6;}
 }
 }
 for(int i=0;i<42;i++){if(i>=uLandmarkCount)break;float dd=length((vCamera-uLandmarks[i])*vec2(uAspect,1.));fx+=vec3(.3,1.,.65)*step(dd,.004);}
 // Compress the emissive contribution so colored detail survives bright cores.
 vec3 emission=1.-exp(-max(fx,vec3(0.)));
 fragColor=vec4(clamp(camera+emission*(1.-camera*.72),0.,1.),1.);
}
