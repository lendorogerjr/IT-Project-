# Bouwt het navigatiegraaf van campus Kaai uit de vectorplannen (PDF -> raster -> ruimtes, deuren, gangskelet).
import numpy as np, json, cv2, math, re, sys
from PIL import Image, ImageDraw
from scipy import ndimage as ndi
from skimage.morphology import skeletonize
S=5; MPT=0.0254/72*500   # meter per PDF-punt bij schaal 1/500
MPX=MPT/S
PAG={-1:0,0:1,1:2,2:3,3:4,4:5,5:6}
LETTERS={'A':(213.0,1009.0),'B':(510.4,814.6),'C':(640.0,281.0),'D':(180.0,580.6)}
labels=json.load(open('labels.json'))
AFGEWEZEN=[]
def m(p): return round(p*MPX,2)
def obstakels(i):
    a=np.array(Image.open(f'notext_{i}.png').convert('L'))
    dark=(a<200)
    thick=cv2.morphologyEx((a<=110).astype(np.uint8),cv2.MORPH_OPEN,np.ones((4,4),np.uint8))>0
    nabij=cv2.dilate(thick.astype(np.uint8),np.ones((9,9),np.uint8))>0
    frac=cv2.blur(((a<250)&(a>=120)&~nabij).astype(np.float32),(15,15))
    hatch=cv2.morphologyEx((frac>0.25).astype(np.uint8),cv2.MORPH_OPEN,np.ones((9,9),np.uint8))>0
    obs=cv2.dilate((dark|hatch).astype(np.uint8),np.ones((2,2),np.uint8))>0
    return a,obs,hatch,thick
def gebouw(x,y):
    return min(LETTERS,key=lambda k:(LETTERS[k][0]-x)**2+(LETTERS[k][1]-y)**2)
def los(mask,p,q):
    n=int(max(abs(p[0]-q[0]),abs(p[1]-q[1])))+1
    xs=np.linspace(p[0],q[0],n).astype(int); ys=np.linspace(p[1],q[1],n).astype(int)
    return mask[ys,xs].mean()>0.97
def verdieping(niv):
    i=PAG[niv]; a,obs,hatch,thick=obstakels(i)
    n,lab=cv2.connectedComponents((~obs).astype(np.uint8),connectivity=4)
    area=np.bincount(lab.ravel())
    L=labels[str(niv)]
    ruimte_lab={}  # comp -> [labels]
    trappen=[]
    for l in L:
        X,Y=int(l['x']*S),int(l['y']*S)
        if re.fullmatch(r'[IVX]+-\d',l['t'] or ''): trappen.append(l); continue
        if hatch[Y,X]: continue
        c=lab[Y,X]
        if c==0:
            w=lab[Y-25:Y+26,X-25:X+26]; ys,xs=np.nonzero(w)
            if len(ys):
                k=np.argmin((ys-25)**2+(xs-25)**2); c=w[ys[k],xs[k]]
        if c==0: print('label zonder ruimte',niv,l); continue
        ruimte_lab.setdefault(int(c),[]).append(l)
    kand=set(ruimte_lab)|{c for c in range(1,n) if area[c]*MPX*MPX>=1.0}
    kand={c for c in kand if area[c]*MPX*MPX<20000}  # buitenruimte weg
    # buitenruimte: component die de rand raakt
    rand=set(np.unique(np.concatenate([lab[0],lab[-1],lab[:,0],lab[:,-1]])))
    kand-=rand
    weg=[(l['t'] or l['naam']) for c in ruimte_lab if c not in kand for l in ruimte_lab[c]]
    if weg: print('  labels in buitenruimte:',niv,weg)
    for c in [c for c in ruimte_lab if c not in kand]: del ruimte_lab[c]
    g=np.where(np.isin(lab,list(kand)),lab,0).astype(np.int32)
    bogen=json.load(open('bogen.json'))[str(i)]
    def comp_bij(p,R=12):
        X,Y=int(round(p[0])),int(round(p[1]))
        if lab[Y,X] in kand: return int(lab[Y,X])
        w=lab[Y-R:Y+R+1,X-R:X+R+1]; ok=np.isin(w,list(kand)); ys,xs=np.nonzero(ok)
        if not len(ys): return 0
        k=np.argmin((ys-R)**2+(xs-R)**2); return int(w[ys[k],xs[k]])
    def donker(p,q):
        n=40; xs=np.linspace(p[0],q[0],n)[5:-5].astype(int); ys=np.linspace(p[1],q[1],n)[5:-5].astype(int)
        return obs[ys,xs].mean()
    deuren=[]
    for bg in bogen:
        c=np.array(bg['c'])*S; A=np.array(bg['a'])*S; B_=np.array(bg['b'])*S; r=bg['r']*S
        ua=(A-c)/r; ub=(B_-c)/r; mid=(ua+ub)/np.linalg.norm(ua+ub)
        # deuropening = straal zonder getekende lijn; de andere straal is het deurblad
        if donker(c,A)<donker(c,B_): jamb,blad=A,B_
        else: jamb,blad=B_,A
        ub_=(blad-c)/r; pm=(c+jamb)/2
        def eerste(pts):
            for q in pts:
                k=comp_bij(q,6)
                if k and (k in ruimte_lab or area[k]*MPX*MPX>=2.0): return k
            return 0
        achter=eerste([pm-ub_*(f/MPX) for f in (0.35,0.6,0.9,1.3)])
        voor=eerste([c+f*r*mid for f in (1.2,1.5,1.9,2.4)])
        if not achter or not voor or achter==voor:
            AFGEWEZEN.append((niv,float(c[0]),float(c[1]),int(achter),int(voor))); continue
        binnen,buiten=achter,voor
        deuren.append(dict(a=binnen,b=buiten,x=float(pm[0]),y=float(pm[1]),ext=r*MPX))
    # dubbele deuren samenvoegen
    samen=[]
    for d in deuren:
        if any({d['a'],d['b']}=={e['a'],e['b']} and math.hypot(d['x']-e['x'],d['y']-e['y'])*MPX<2.2 for e in samen):
            continue
        samen.append(d)
    deuren=samen
    # onbenoemde ruimtes met <2 buren iteratief weg
    while True:
        bur={}
        for d in deuren: bur.setdefault(d['a'],set()).add(d['b']); bur.setdefault(d['b'],set()).add(d['a'])
        weg={c for c in kand if c not in ruimte_lab and len(bur.get(c,()))<2}
        if not weg: break
        kand-=weg; deuren=[d for d in deuren if d['a'] in kand and d['b'] in kand]
    return dict(a=a,lab=lab,area=area,kand=kand,ruimte_lab=ruimte_lab,deuren=deuren,trappen=trappen,thick=thick,hatch=hatch,obs=obs,bur=bur)

def skelet(mask,step_m=3.0):
    """Gangskelet als lijst knopen (x,y in px) + randen."""
    ys,xs=np.nonzero(mask); y0,x0=ys.min(),xs.min()
    sub=mask[y0:ys.max()+1,x0:xs.max()+1]
    k=4; small=cv2.resize(sub.astype(np.uint8),(max(1,sub.shape[1]//k),max(1,sub.shape[0]//k)),interpolation=cv2.INTER_AREA)>0
    small=cv2.morphologyEx(small.astype(np.uint8),cv2.MORPH_CLOSE,np.ones((3,3),np.uint8))>0
    # kleine eilanden (kolommen, meubels, tafels) opvullen zodat het skelet er niet rond kronkelt
    gaten,ng=ndi.label(~small)
    if ng:
        opp=np.bincount(gaten.ravel())*(k*MPX)**2
        randlab=set(np.unique(np.concatenate([gaten[0],gaten[-1],gaten[:,0],gaten[:,-1]])))
        vul=[j for j in range(1,ng+1) if opp[j]<12 and j not in randlab]
        small=small|np.isin(gaten,vul)
    sk=skeletonize(small)
    P=set(zip(*np.nonzero(sk)))
    if not P: 
        cy,cx=ys.mean(),xs.mean(); return [(cx,cy)],[]
    nb=lambda p:[(p[0]+dy,p[1]+dx) for dy in (-1,0,1) for dx in (-1,0,1) if (dy or dx) and (p[0]+dy,p[1]+dx) in P]
    # korte uitlopers snoeien (< 1.2 m)
    for _ in range(3):
        ends=[p for p in P if len(nb(p))==1]
        for e in ends:
            path=[e]; cur=e; prev=None
            while True:
                n=[q for q in nb(cur) if q!=prev and q not in path]
                if len(n)!=1 or len(nb(cur))>2: break
                prev=cur; cur=n[0]; path.append(cur)
            if len(path)*k*MPX<1.2 and len(nb(cur))>2:
                for q in path[:-1]: P.discard(q)
    if not P:
        cy,cx=ys.mean(),xs.mean(); return [(cx,cy)],[]
    deg={p:len(nb(p)) for p in P}
    knoop={p for p in P if deg[p]!=2}
    if not knoop: knoop={next(iter(P))}
    stap=max(1,int(step_m/(k*MPX)))
    randen=set(); bezocht=set()
    for s0 in list(knoop):
        for n0 in nb(s0):
            if (s0,n0) in bezocht: continue
            path=[s0,n0]; cur=n0; prev=s0
            while cur not in knoop:
                n=[q for q in nb(cur) if q!=prev]
                if not n: break
                prev,cur=cur,n[0]; path.append(cur)
                if len(path)>100000: break
            bezocht.add((path[-1],path[-2]))
            # tussenknopen
            sel=[path[0]]+path[stap:-1:stap][: ]+[path[-1]] if len(path)>stap*1.5 else [path[0],path[-1]]
            for q in sel[1:-1]: knoop.add(q)
            for u,v in zip(sel,sel[1:]):
                if u!=v: randen.add((min(u,v),max(u,v)))
    lijst=sorted(knoop); idx={p:j for j,p in enumerate(lijst)}
    pts=[(x0+p[1]*k+k/2,y0+p[0]*k+k/2) for p in lijst]
    return pts,[(idx[u],idx[v]) for u,v in randen if u in idx and v in idx]

def bouw(niv,r,knopen,verb,overlay):
    lab=r['lab']; kand=r['kand']; RL=r['ruimte_lab']; deuren=r['deuren']; area=r['area']
    bur={}
    for d in deuren: bur.setdefault(d['a'],set()).add(d['b']); bur.setdefault(d['b'],set()).add(d['a'])
    def is_gang(c): return (c not in RL) or len(bur.get(c,()))>=3 or area[c]*MPX*MPX>250
    pos={}
    def add(id,naam,soort,p,**kw):
        k=dict(id=id,naam=naam,soort=soort,verdieping=niv,x=m(p[0]),z=m(p[1])); k.update(kw); knopen.append(k); pos[id]=p; overlay.append(('n',soort,p,id))
    def link(u,v,**kw):
        if u!=v: verb.append(dict(van=u,naar=v,**kw)); overlay.append(('e',pos[u],pos[v]))
    gid=[0]
    def gnaam(): gid[0]+=1; return f'G{niv}-{gid[0]}'
    # gangen: skelet
    skel={}
    for c in kand:
        if not is_gang(c): continue
        mask=(lab==c)
        if r['area'][c]*MPX*MPX<6:
            ys,xs=np.nonzero(mask); pts=[(xs.mean(),ys.mean())]; ed=[]
        else: pts,ed=skelet(mask)
        ids=[]
        nm=('Gang '+RL[c][0]['t']) if c in RL and RL[c][0]['t'] else 'Gang'
        for p in pts: i=gnaam(); add(i,nm,'gang',p); ids.append(i)
        for u,v in ed: link(ids[u],ids[v])
        skel[c]=(ids,pts,mask)
    def dichtste(c,p):
        ids,pts,mask=skel[c]
        order=sorted(range(len(pts)),key=lambda j:(pts[j][0]-p[0])**2+(pts[j][1]-p[1])**2)
        for j in order[:8]:
            if los(mask|~r['obs'],p,pts[j]): return ids[j]
        return ids[order[0]]
    gebruikt={}
    def lid(l,c):
        t=l['t']
        if t and re.fullmatch(r'[A-Z]\d{3}',t): base=t
        elif t: base=gebouw(l['x'],l['y'])+t
        else: base=gebouw(l['x'],l['y'])+'-'+l['naam'].replace(' ','')
        if base in gebruikt and gebruikt[base]!=c:
            j=2
            while f'{base}-{j}' in gebruikt: j+=1
            base=f'{base}-{j}'
        gebruikt[base]=c; return base
    def naam(l,id):
        return (l['naam'].capitalize()+(' ('+id+')' if l['t'] else '')) if l['naam'] else id
    # lokalen
    hoofd={}  # c -> (deur index, node id)
    for c in kand:
        if c not in RL: continue
        if is_gang(c):
            for l in RL[c]:
                p=(l['x']*S,l['y']*S); i=lid(l,c)
                if i in pos: continue
                add(i,naam(l,i),'lokaal',p); link(i,dichtste(c,p))
            continue
        ds=[(j,d) for j,d in enumerate(deuren) if c in (d['a'],d['b'])]
        l0=RL[c][0]; i=lid(l0,c)
        if not ds:
            add(i,naam(l0,i),'lokaal',(l0['x']*S,l0['y']*S)); print('  geen deur:',i); hoofd[c]=(None,i); continue
        def score(jd):
            o=jd[1]['b'] if jd[1]['a']==c else jd[1]['a']
            return (is_gang(o), area[o])
        j,d=max(ds,key=score)
        o=d['b'] if d['a']==c else d['a']
        # normaal: van ruimte c naar andere ruimte o
        p=np.array([d['x'],d['y']]); R=30
        y0,x0=int(p[1])-R,int(p[0])-R
        w=lab[y0:y0+2*R+1,x0:x0+2*R+1]
        yc,xc=np.nonzero(w==c); yo,xo=np.nonzero(w==o)
        if len(yc) and len(yo):
            v=np.array([xo.mean()-xc.mean(),yo.mean()-yc.mean()]); a_=math.atan2(v[1],v[0]); a_=round(a_/(math.pi/4))*(math.pi/4)
            nrm=[round(math.cos(a_),4)+0.0,round(math.sin(a_),4)+0.0]
        else: nrm=[0.0,1.0]
        extra=dict(qr=dict(code='BINNENNAV:'+i,normaal=nrm))
        add(i,naam(l0,i),'lokaal',(d['x'],d['y']),**extra); hoofd[c]=(j,i)
        for l in RL[c][1:]:   # tweede label in dezelfde ruimte -> zelfde deur
            i2=lid(l,c)
            if i2 not in pos: add(i2,naam(l,i2),'lokaal',(d['x']+3,d['y']+3)); link(i2,i)
    # deuren verbinden
    for j,d in enumerate(deuren):
        a,b=d['a'],d['b']; p=(d['x'],d['y'])
        node=None
        for c in (a,b):
            if c in hoofd and hoofd[c][0]==j: node=hoofd[c][1]
        if node is None: node=gnaam(); add(node,'Deur','gang',p)
        for c in (a,b):
            if c in skel: link(node,dichtste(c,p))
            elif c in hoofd and hoofd[c][1]!=node: link(node,hoofd[c][1])
    # losse stukken van de graaf overbruggen (schuifdeuren, glasdeuren, drempels, stippellijnen)
    import networkx as nx
    vrij=~(r['thick']|r['hatch'])
    G=nx.Graph(); G.add_nodes_from(pos)
    for e in verb:
        if e['van'] in pos and e['naar'] in pos: G.add_edge(e['van'],e['naar'])
    soort={k['id']:k['soort'] for k in knopen if k['verdieping']==niv}
    r['te_controleren']=[]
    from skimage.graph import MCP_Geometric
    K=4
    vrijk=cv2.resize(vrij.astype(np.uint8),(vrij.shape[1]//K,vrij.shape[0]//K),interpolation=cv2.INTER_NEAREST)>0
    vrijk=cv2.erode(vrijk.astype(np.uint8),np.ones((2,2),np.uint8))>0  # min. ~0.3 m breed
    def pad(pu,pv,maxm):
        x0=int(min(pu[0],pv[0])/K)-60; x1=int(max(pu[0],pv[0])/K)+60
        y0=int(min(pu[1],pv[1])/K)-60; y1=int(max(pu[1],pv[1])/K)+60
        x0,y0=max(x0,0),max(y0,0)
        sub=vrijk[y0:y1,x0:x1]
        cost=np.where(sub,1.0,np.inf)
        su=(int(pu[1]/K)-y0,int(pu[0]/K)-x0); sv=(int(pv[1]/K)-y0,int(pv[0]/K)-x0)
        for (yy,xx) in (su,sv):
            cost[max(yy-2,0):yy+3,max(xx-2,0):xx+3]=1.0
        mcp=MCP_Geometric(cost); cum,_=mcp.find_costs([su],[sv])
        L=cum[sv]*K*MPX
        if not np.isfinite(L) or L>maxm: return None
        P=np.array(mcp.traceback(sv),np.int32)[:,::-1].reshape(-1,1,2)
        P=cv2.approxPolyDP(P,max(1,int(0.4/(K*MPX))),False).reshape(-1,2)
        return L,[((x+x0)*K+K/2,(y+y0)*K+K/2) for x,y in P]
    for ronde in range(80):
      gelukt=False
      for (DMAX,RAT) in ((12,1.6),(32,1.4)):
          cs=sorted(nx.connected_components(G),key=len,reverse=True)
          if len(cs)<2: break
          cid={n:j for j,c in enumerate(cs) for n in c}
          gelukt=False
          for j,c in enumerate(cs[1:],1):
              paren=[]
              for u in c:
                  if soort[u]=='trap': continue
                  pu=pos[u]
                  for v,pv in pos.items():
                      if cid[v]==j or soort[v]=='trap': continue
                      d=math.hypot(pu[0]-pv[0],pu[1]-pv[1])*MPX
                      if d<DMAX: paren.append((d,u,v))
              paren.sort()
              for d,u,v in paren[:6]:
                  res=pad(pos[u],pos[v],min(RAT*d+1.5,DMAX*1.5))
                  if res is None: continue
                  L,P=res; vorige=u
                  for q in P[1:-1]:
                      n_=gnaam(); add(n_,'Gang','gang',q); soort[n_]='gang'; G.add_node(n_); link(vorige,n_); G.add_edge(vorige,n_); vorige=n_
                  link(vorige,v); G.add_edge(vorige,v)
                  r['te_controleren'].append((u,v,round(L,1))); gelukt=True; break
              if gelukt: break
          if gelukt: break
      if not gelukt: break
    # trappen
    for l in r['trappen']:
        nr=l['t'].split('-')[0]; p=(l['x']*S,l['y']*S); i=f'TRAP-{nr}-{niv}'
        add(i,f'Trap {nr}','trap',p)
        kands=[(k,q) for k,q in pos.items() if k!=i and not k.startswith('TRAP') and (q[0]-p[0])**2+(q[1]-p[1])**2<(10/MPX)**2]
        kands.sort(key=lambda kq:(kq[1][0]-p[0])**2+(kq[1][1]-p[1])**2)
        ok=[k for k,q in kands if los(~r['thick'],p,q)]
        if ok: link(i,ok[0])
        elif kands: link(i,kands[0][0]); print('  trap zonder zichtlijn',i)
        else: print('  trap los',i)
    return pos

if __name__=='__main__':
    knopen=[]; verb=[]; alle={}
    for niv in (-1,0,1,2,3,4,5):
        r=verdieping(niv); ov=[]
        print(niv,'ruimtes',len(r['kand']),'benoemd',len(r['ruimte_lab']),'deuren',len(r['deuren']))
        pos=bouw(niv,r,knopen,verb,ov); alle[niv]=pos
        im=Image.fromarray(r['a']).convert('RGB'); dr=ImageDraw.Draw(im)
        for o in ov:
            if o[0]=='e': dr.line([o[1],o[2]],fill=(220,0,0),width=6)
        for o in ov:
            if o[0]=='n':
                p=o[2]; c={'lokaal':(0,90,255),'gang':(90,90,90),'trap':(240,140,0),'lift':(150,0,200)}[o[1]]; R=12 if o[1]!='gang' else 7
                dr.ellipse([p[0]-R,p[1]-R,p[0]+R,p[1]+R],fill=c)
        for k in knopen:
            if k['verdieping']==niv and 'qr' in k:
                p=alle[niv][k['id']]; nn=k['qr']['normaal']; dr.line([p,(p[0]+nn[0]*40,p[1]+nn[1]*40)],fill=(0,170,0),width=6)
        im.resize((im.width//2,im.height//2)).save(f'controle_{niv}.png')
    # trappen tussen verdiepingen
    tr={}
    for k in knopen:
        if k['soort']=='trap': tr.setdefault(k['id'].split('-')[1],[]).append(k['verdieping'])
    for nr,vs in tr.items():
        vs=sorted(vs)
        for u,v in zip(vs,vs[1:]):
            if v-u==1: verb.append(dict(van=f'TRAP-{nr}-{u}',naar=f'TRAP-{nr}-{v}',kost=12))
    json.dump(dict(knopen=knopen,verb=verb),open('graaf_ruw.json','w'),ensure_ascii=False)
    json.dump(AFGEWEZEN,open('afgewezen.json','w'))
    print(len(knopen),'knopen',len(verb),'verbindingen')
