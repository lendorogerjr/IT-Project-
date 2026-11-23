import pdfplumber, re, json
NIV={0:-1,1:0,2:1,3:2,4:3,5:4,6:5}
out={}
with pdfplumber.open('plannen.pdf') as d:
    for i,pg in enumerate(d.pages):
        if i not in NIV: continue
        ws=[w for w in pg.extract_words(x_tolerance=1.0, extra_attrs=['size']) if abs(w['size']-6.3)<0.3]
        W=[]
        for w in ws:
            t=w['text']; c=dict(t=t,x0=w['x0'],x1=w['x1'],top=w['top'],bot=w['bottom'])
            if re.fullmatch(r'\d{6}',t) and t[0::2]==t[1::2]: c['t']=t[0::2]; W.append(c)
            elif re.fullmatch(r'\d{6}',t):
                m=(w['x0']+w['x1'])/2; W.append(dict(c,t=t[:3],x1=m)); W.append(dict(c,t=t[3:],x0=m))
            else: W.append(c)
        used=set(); L=[]
        def cx(c): return (c['x0']+c['x1'])/2
        for a,w in enumerate(W):
            if a in used or not re.fullmatch(r'[a-z]+',w['t']): continue
            used.add(a); naam=w['t']; top=w['top']; bot=w['bot']; x0,x1=w['x0'],w['x1']; code=None
            for b,v in enumerate(W):  # zelfde regel
                if b not in used and abs(v['top']-w['top'])<1 and 0<=v['x0']-w['x1']<4: naam+=' '+v['t']; used.add(b); x1=v['x1']
            for _ in range(2):
                for b,v in enumerate(W):
                    if b not in used and -0.5<=v['top']-bot<6 and abs(cx(v)-(x0+x1)/2)<18:
                        used.add(b); bot=v['bot']
                        if re.fullmatch(r'\d{3}[a-zA-Z]?',v['t']) and not (naam.startswith('auditorium') and len(naam.split())==1): code=v['t']
                        else: naam+=' '+v['t']
                        break
            L.append(dict(t=code, naam=naam, x=round((x0+x1)/2,2), y=round((top+bot)/2,2)))
        for a,w in enumerate(W):
            if a in used: continue
            t=w['t']
            if t=='VI' and i==1: t='VI-0'
            if re.fullmatch(r'\d{3}',t):
                for b,v in enumerate(W):
                    if b not in used and b!=a and re.fullmatch(r'[A-Z]',v['t']) and abs(v['top']-w['top'])<1 and 0<=v['x0']-w['x1']<4: t+=v['t']; used.add(b)
            if re.fullmatch(r'\d{3}[a-zA-Z]?|[IVX]+-\d|[A-Z]\d{3}',t):
                L.append(dict(t=t, naam=None, x=round(cx(w),2), y=round((w['top']+w['bot'])/2,2)))
            else: print('genegeerd',i,t,round(cx(w)),round(w['top']))
        out[NIV[i]]=L
json.dump(out,open('labels.json','w'),indent=0)
for k,L in out.items(): print(k,len(L),[ (l['t'],l['naam']) for l in L if l['naam']])
