import pypdfium2 as p, pypdfium2.raw as R, sys, numpy as np
from PIL import Image
S=float(sys.argv[1])
d=p.PdfDocument('plannen.pdf')
for i in range(8):
    pg=d[i]
    for o in list(pg.get_objects()):
        if o.type==R.FPDF_PAGEOBJ_TEXT: pg.remove_obj(o)
    pg.gen_content()
    im=pg.render(scale=S).to_pil().convert('RGB')
    im.save(f'notext_{i}.png')
