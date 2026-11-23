# Maakt een A4-PDF met een QR-code per lokaal (inhoud "BINNENNAV:<id>"), 12 per pagina.
import json, sys, io, qrcode
from reportlab.lib.pagesizes import A4
from reportlab.lib.units import mm
from reportlab.pdfgen import canvas
from reportlab.lib.utils import ImageReader
bron, doel = sys.argv[1], sys.argv[2]
d = json.load(open(bron))
namen = {v['niveau']: v['naam'] for v in d['verdiepingen']}
lok = sorted([k for k in d['knopen'] if k.get('qr')], key=lambda k: (k['verdieping'], k['id']))
c = canvas.Canvas(doel, pagesize=A4); W, H = A4
kol, rij, cel_b, cel_h = 3, 4, W / 3, H / 4
for n, k in enumerate(lok):
    if n and n % (kol * rij) == 0: c.showPage()
    j = n % (kol * rij); x = (j % kol) * cel_b; y = H - (j // kol + 1) * cel_h
    img = qrcode.make(k['qr'].get('code') or 'BINNENNAV:' + k['id'], box_size=8, border=2)
    b = io.BytesIO(); img.save(b, format='PNG'); b.seek(0)
    s = 45 * mm
    c.drawImage(ImageReader(b), x + (cel_b - s) / 2, y + 20 * mm, s, s)
    c.setFont('Helvetica-Bold', 13); c.drawCentredString(x + cel_b / 2, y + 13 * mm, k['naam'])
    c.setFont('Helvetica', 8); c.drawCentredString(x + cel_b / 2, y + 8 * mm, f"{namen.get(k['verdieping'], k['verdieping'])} · naast de deur, ± 1,40 m hoog")
    c.setDash(2, 3); c.setStrokeGray(0.7); c.rect(x + 3 * mm, y + 3 * mm, cel_b - 6 * mm, cel_h - 6 * mm); c.setDash()
c.save(); print(len(lok), 'QR-codes')
