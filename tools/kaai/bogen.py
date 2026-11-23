import pdfplumber, numpy as np, json
out={}
with pdfplumber.open('plannen.pdf') as d:
    for i in range(7):
        pg=d.pages[i]; B=[]
        for cv in pg.curves:
            p=cv.get('path') or []
            if cv.get('fill') or len(p)!=2 or p[0][0]!='m' or p[1][0]!='c': continue
            P0=np.array(p[0][1]); C1,C2,P3=map(np.array,p[1][1:])
            t0=C1-P0; t3=P3-C2
            if np.linalg.norm(t0)<1e-6 or np.linalg.norm(t3)<1e-6: continue
            n0=np.array([-t0[1],t0[0]]); n3=np.array([-t3[1],t3[0]])
            A=np.c_[n0,-n3]
            if abs(np.linalg.det(A))<1e-9: continue
            s=np.linalg.solve(A,P3-P0); c=P0+s[0]*n0
            r0=np.linalg.norm(P0-c); r3=np.linalg.norm(P3-c)
            if abs(r0-r3)>0.05*r0 or not (3.0<r0<12.0): continue
            u0=(P0-c)/r0; u3=(P3-c)/r3
            if abs(np.dot(u0,u3))>0.1: continue   # 90 graden
            B.append(dict(c=c.tolist(),a=P0.tolist(),b=P3.tolist(),r=float(r0)))
        out[i]=B; print(i,len(B))
json.dump(out,open('bogen.json','w'))
