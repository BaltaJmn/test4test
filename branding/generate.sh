#!/usr/bin/env bash
# Regenera todo el arte a partir de los SVG de esta carpeta.
# Requiere rsvg-convert (brew install librsvg).
set -euo pipefail
cd "$(dirname "$0")"
RES=../androidApp/src/main/res

# Iconos legacy: los usa API 24 y 25, por debajo del adaptive icon.
for d in mdpi:48 hdpi:72 xhdpi:96 xxhdpi:144 xxxhdpi:192; do
  dpi=${d%%:*}; px=${d##*:}
  rsvg-convert -w "$px" -h "$px" icon-legacy.svg -o "$RES/mipmap-$dpi/ic_launcher.png"
  rsvg-convert -w "$px" -h "$px" icon-round.svg  -o "$RES/mipmap-$dpi/ic_launcher_round.png"
done

# Ficha de Play. PNG de 32 bits, 512x512, sin transparencia.
rsvg-convert -w 512 -h 512 icon-play.svg -o out/icon-512.png
rsvg-convert -w 1024 -h 500 feature-graphic.svg -o out/feature-graphic-1024x500.png
echo "listo"
