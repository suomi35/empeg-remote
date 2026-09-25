#!/bin/bash
# Read-only crawler for ghostwheel.de/empeg XML playlists.
# Follows playlist FID links only, never command URLs. Depth/count guards, 1s delay.
BASE="https://www.ghostwheel.de/empeg"
OUT="playlists"
queue=("101:0")
MAXDEPTH=5
MAXCOUNT=40
count=0

while [ ${#queue[@]} -gt 0 ] && [ $count -lt $MAXCOUNT ]; do
  item="${queue[0]}"; queue=("${queue[@]:1}")
  fid="${item%%:*}"; d="${item##*:}"
  file="$OUT/FID_${fid}.xml"
  if [ -f "$file" ] && [ -s "$file" ]; then continue; fi
  count=$((count+1))
  curl -s -m 20 "$BASE/?FID=${fid}&EXT=.xml" -o "$file"
  echo "fetched FID=$fid depth=$d -> $file ($(wc -c < "$file") bytes)"
  sleep 1
  for tagfid in $(tr -d '\r' < "$file" | awk '/<type>playlist<\/type>/{p=1;next} p&&/<tagfid>/{sub(/.*<tagfid>/,""); sub(/<\/tagfid>.*/,""); print; p=0}'); do
    nd=$((d+1))
    if [ $nd -le $MAXDEPTH ] && [ ! -f "$OUT/FID_${tagfid}.xml" ]; then
      queue+=("${tagfid}:${nd}")
    fi
  done
done
echo "done, fetched $count playlists"