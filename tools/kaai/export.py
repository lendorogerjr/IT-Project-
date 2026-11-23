# Zet de ruwe graaf om naar het binnennav-graaf formaat (versie 1) + planbeelden + rapport.
import json, math, networkx as nx, pypdfium2 as p
from PIL import Image
MPT=0.0254/72*500           # meter per PDF-punt bij schaal 1/500
PS=3                         # planbeelden op 3 px per punt
NAMEN={-1:'Kelder',0:'Gelijkvloers',1:'1ste verdieping',2:'2de verdieping',3:'3de verdieping',4:'4de verdieping',5:'5de verdieping'}
PAG={-1:0,0:1,1:2,2:3,3:4,4:5,5:6}
g=json.load(open('graaf_ruw.json'))
d=p.PdfDocument('plannen.pdf'); verd=[]
for niv in sorted(NAMEN):
    im=d[PAG[niv]].render(scale=PS).to_pil().convert('RGB')
    f=f'kaai_{niv}.png'; im.save('uit/plannen/'+f,optimize=True)
    verd.append(dict(niveau=niv,naam=NAMEN[niv],plan=dict(bestand=f,breedtePx=im.width,hoogtePx=im.height,meterPerPixel=round(MPT/PS,8),oorsprongPx=[0,0])))
doc=dict(formaat='binnennav-graaf',versie=1,gebouw=dict(id='ehb-kaai',naam='EhB Campus Kaai'),verdiepingen=verd,
         knopen=g['knopen'],verbindingen=g['verb'])
json.dump(doc,open('uit/kaai.json','w'),ensure_ascii=False,indent=1)
K={k['id']:k for k in g['knopen']}
G=nx.Graph(); G.add_nodes_from(K)
for e in g['verb']: G.add_edge(e['van'],e['naar'])
cs=sorted(nx.connected_components(G),key=len,reverse=True)
los=[k for k in K.values() if k['soort']=='lokaal' and k['id'] not in cs[0]]
json.dump(dict(niet_bereikbaar=sorted([(k['verdieping'],k['id']) for k in los])),open('uit/stats.json','w'))
print(len(K),'knopen',len(g['verb']),'verbindingen',len(los),'lokalen los')
