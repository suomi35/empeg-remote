#!/bin/bash
# Fetch the player's own font files from your empeg over hijack's FTP server.
# Fonts live at /empeg/lib/fonts/*.bf ("EFNT" format) and are what the real
# player renders on the VFD. Drop the results into fixtures/<name>/fonts/
# and the simulator will use them for its /proc/empeg_screen.{gif,png}.
#
# No player on the LAN? The same files ship inside official firmware images -
# extract them offline with:
#   tools/extract_player_fonts_from_upgrade.py <firmware>.upgrade fixtures/<name>/fonts
#
# Usage: tools/fetch_player_fonts.sh <player-host-or-ip> [fixtures-name]
# e.g.:  tools/fetch_player_fonts.sh ghostwheel.local ghostwheel
set -euo pipefail
HOST="${1:?usage: fetch_player_fonts.sh <player-host-or-ip> [fixtures-name]}"
NAME="${2:-ghostwheel}"
DEST="fixtures/$NAME/fonts"
mkdir -p "$DEST"
# hijack's FTP accepts any login; try anonymous variants.
for USERPASS in "anonymous:empeg@localhost" "ftp:ftp" "root:root"; do
  if curl -sf --user "$USERPASS" "ftp://$HOST/empeg/lib/fonts/" -l -m 30 -o /tmp/emp_fonts.list 2>/dev/null; then
    FOUND=1; break
  fi
  FOUND=0
done
if [ "$FOUND" != 1 ]; then
  echo "Could not list ftp://$HOST/empeg/lib/fonts/ - check the player's IP and that hijack FTP is enabled" >&2
  exit 1
fi
echo "Fonts on player:"; cat /tmp/emp_fonts.list
for f in $(grep -i '\.bf$' /tmp/emp_fonts.list); do
  curl -sf --user "$USERPASS" "ftp://$HOST/empeg/lib/fonts/$f" -o "$DEST/$f" -m 60
  echo "fetched $f ($(wc -c < "$DEST/$f") bytes)"
done
echo "Done. Run the simulator with --fixtures=fixtures/$NAME and it will pick them up."
