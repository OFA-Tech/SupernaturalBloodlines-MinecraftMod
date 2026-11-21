# Measures Klaus's wolf in a pose of klaus_hybrid.animation.json (default: death) the way GeckoLib poses it (x and y rotations and the x pivot are negated, ZYX order).
# Needs numpy. Usage: python tools/measure_wolf_lying.py src/main/resources/assets/supernaturalbloodlines death
# Prints the bounds in blocks from the entity's feet (render space, front is -z); LyingBody.BEAST_* come from it.
import json,sys,math
import numpy as np
r=sys.argv[1]; pose=sys.argv[2]
g=json.load(open(r+'/geo/entity/klaus_hybrid.geo.json'))['minecraft:geometry'][0]
a={} if pose=='none' else json.load(open(r+'/animations/entity/klaus_hybrid.animation.json'))['animations']['animation.klaus_hybrid.'+pose]
bones={b['name']:b for b in g['bones']}
def last(v):
    if isinstance(v,dict) and v and all(k.replace('.','',1).isdigit() for k in v):
        k=sorted(v,key=float)[-1]; v=v[k]
    if isinstance(v,dict): v=v.get('post',v.get('vector',v.get('pre')))
    return v
def anim(name,ch):
    b=a.get('bones',{}).get(name,{})
    if ch not in b: return [0,0,0]
    v=last(b[ch]); return [float(x) for x in v]
def T(x,y,z):
    m=np.eye(4); m[:3,3]=[x,y,z]; return m
def Rx(t): c,s=math.cos(t),math.sin(t); return np.array([[1,0,0,0],[0,c,-s,0],[0,s,c,0],[0,0,0,1]])
def Ry(t): c,s=math.cos(t),math.sin(t); return np.array([[c,0,s,0],[0,1,0,0],[-s,0,c,0],[0,0,0,1]])
def Rz(t): c,s=math.cos(t),math.sin(t); return np.array([[c,-s,0,0],[s,c,0,0],[0,0,1,0],[0,0,0,1]])
pts={}
def walk(name,parentM):
    b=bones[name]
    piv=b.get('pivot',[0,0,0]); pr=np.array([-piv[0],piv[1],piv[2]])/16
    geor=b.get('rotation',[0,0,0])
    ar=anim(name,'rotation'); ap=anim(name,'position') if pose!='none' else [0,0,0]
    rx=math.radians(-ar[0])+math.radians(-geor[0]); ry=math.radians(-ar[1])+math.radians(-geor[1]); rz=math.radians(ar[2])+math.radians(geor[2])
    M=parentM@T(-ap[0]/16,ap[1]/16,ap[2]/16)@T(*pr)@Rz(rz)@Ry(ry)@Rx(rx)@T(*(-pr))
    for c in b.get('cubes',[]):
        o=c['origin']; s=c['size']
        cr=c.get('rotation',[0,0,0]); cp=c.get('pivot',[0,0,0]); cpr=np.array([-cp[0],cp[1],cp[2]])/16; CM=T(*cpr)@Rz(math.radians(cr[2]))@Ry(math.radians(-cr[1]))@Rx(math.radians(-cr[0]))@T(*(-cpr))
        for ix in (0,1):
            for iy in (0,1):
                for iz in (0,1):
                    p=np.array([-(o[0]+ix*s[0]),o[1]+iy*s[1],o[2]+iz*s[2],1])/np.array([16,16,16,1])
                    p[3]=1
                    q=M@CM@p
                    pts.setdefault(name,[]).append(q[:3])
    for n,bb in bones.items():
        if bb.get('parent')==name: walk(n,M)
walk('root',np.eye(4))
allp=np.array([p for v in pts.values() for p in v])
print('ALL min',allp.min(0).round(3),'max',allp.max(0).round(3),'radius xz',np.sqrt((allp[:,0]**2+allp[:,2]**2)).max().round(3))
core=np.array([p for k,v in pts.items() if not k.startswith(('tail','ear','hackles')) for p in v])
print('NO tail/ears min',core.min(0).round(3),'max',core.max(0).round(3),'radius xz',np.sqrt((core[:,0]**2+core[:,2]**2)).max().round(3))
for k in ('head','jaw','neck','body','tail3'):
    p=np.array(pts[k]); print(k,'min',p.min(0).round(3),'max',p.max(0).round(3),'centre',((p.min(0)+p.max(0))/2).round(3))


