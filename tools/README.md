# tools

Helpers for collecting the real player's assets so the simulator (and the app)
work against authentic data. Nothing here writes to a player; both scripts are
read-only.

## `extract_player_fonts_from_upgrade.py` — fonts from a firmware image

The player's VFD fonts live in `/empeg/lib/fonts/*.bf` on the player's disk,
and every official car2 `.upgrade` image carries that disk image. This pulls
them out with nothing but Python 3 (no player, no ext2 tools):

```bash
tools/extract_player_fonts_from_upgrade.py <image.upgrade> [outdir]
# outdir defaults to fixtures/ghostwheel/fonts/
```

It understands the `.upgrade` container (chunk list, `crc32`), gunzips the
drive-image chunk (`CHUNK_PUMPHDA*`), walks the ext2 filesystem inside, and
validates every `EFNT` header before writing. Duplicates across images are
skipped, so pointing it at a whole firmware directory is safe:

```bash
tools/extract_player_fonts_from_upgrade.py \
    ~/Development/empeg/original-player-firmware-main/player-firmware/car-v2.00
```

Verified output from car2 v2.00: `small.bf` (6px), `medium.bf` (9px),
`large.bf` (18px), `graphics.bf`, `graphics_large.bf`, `wait.bf`,
`timecode.bf` — see `fixtures/ghostwheel/README.md` for the table.

## `fetch_player_fonts.sh` — fonts from a player on the LAN

For a real player with hijack's FTP server enabled:

```bash
tools/fetch_player_fonts.sh <player-host-or-ip> [fixtures-name]
```

It tries the usual hijack logins (`anonymous`, `ftp`, `root`) and copies
`/empeg/lib/fonts/*.bf` into `fixtures/<fixtures-name>/fonts/`.
