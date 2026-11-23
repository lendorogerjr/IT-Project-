#!/usr/bin/env bash
# Bouwt data/kaai/kaai.json opnieuw uit de PDF met de plannen van campus Kaai.
# Gebruik: tools/kaai/bouw.sh pad/naar/plannen.pdf
# De plannen zelf blijven buiten git (map plannen/ staat in .gitignore).
set -euo pipefail
HIER=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$HIER/../.." && pwd)
WERK="$ROOT/plannen/werk"
mkdir -p "$WERK/uit/plannen" "$ROOT/plannen/kaai"
cp "$1" "$WERK/plannen.pdf"
cd "$WERK"
python3 "$HIER/render.py" 5        # planbeelden zonder tekst (obstakels)
python3 "$HIER/labels.py"          # lokaalnummers, namen en trappen uit de PDF-tekst
python3 "$HIER/bogen.py"           # deurbogen uit de PDF-vectoren
python3 "$HIER/graaf.py"           # ruimtes, deuren, gangskelet, trappen -> graaf_ruw.json
python3 "$HIER/export.py"          # -> uit/kaai.json + planbeelden per verdieping
python3 "$HIER/qr_pdf.py" uit/kaai.json uit/QR_kaai.pdf
cp uit/kaai.json uit/QR_kaai.pdf "$ROOT/data/kaai/"
cp uit/plannen/*.png controle_*.png "$ROOT/plannen/kaai/"
echo "Klaar: data/kaai/kaai.json (plannen en controlebeelden in plannen/kaai/)"
