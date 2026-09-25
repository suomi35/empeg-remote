#!/usr/bin/env python3
"""Extract the player's own .bf fonts from an official empeg .upgrade file.

The player renders its VFD with "EFNT" font files kept at /empeg/lib/fonts/
on the player's disk (car2 v2.00 and later ship graphics.bf, medium.bf and
small.bf). You do not need a player to get them: the official firmware and
hard-drive-builder images embed the whole disk image.

How the .upgrade container is laid out (see emptool's GPL sources,
lib/protocol/upgrader.cpp + upgrader.h):

    int32   claimedLength       (bytes of payload that follow)
    int32   crc32(payload)
    then, repeatedly, up to claimedLength:
        uint32 chunkType
        uint32 chunkLength
        byte   chunkLength[payload]

Chunk types that carry files:

    0x05      CHUNK_UNTARDRIVE0 - plain tar (drive0 contents)
    0x20-0x28 CHUNK_PUMPHDA*    - gzip stream holding the drive image
    0x30-0x38 CHUNK_PUMPHDB*    - same, for drive1

The hard-drive-builder images (hard-drive-builder/*.upgrade) and the "JE"
car releases are the ones whose drive image contains /empeg/lib/fonts; the
plain car upgrades only replace flash (kernel + ramdisk), so no fonts come
out of those.

The gzip payload unpacks to a raw ext2 filesystem, walked here without any
external tools. Fonts are validated against the EFNT header (declared size
must match the computed layout) and de-duplicated by content, so running
this over several images is safe.

Every car2 image from v2.00 onward carries the drive filesystem, and the
fonts found there are:

    small.bf         6px  - menu/status text, 231 glyphs
    medium.bf        9px  - menu text, 231 glyphs
    large.bf        18px  - now-playing text, 231 glyphs
    graphics.bf      9px  - digits/punctuation only (14-21 glyphs)
    graphics_large.bf 16px - big digits/graphics (30 glyphs)
    wait.bf         15px  - the six letters A-F (spinner segments)
    timecode.bf     21px  - digits/colon (14 glyphs)

The v2.00 and v2.01 images carry exactly this classic set; the v3.00-alpha
images also ship an EFNT "version 2" variant for small/medium/large.bf whose
layout this extractor does not decode (it rejects them with a clear message
rather than writing garbage), so use a v2.00/v2.01 image for fonts.

Usage:
    tools/extract_player_fonts_from_upgrade.py <upgrade-file-or-dir> [outdir]

outdir defaults to fixtures/ghostwheel/fonts/ relative to the repo root,
which is where the simulator picks fonts up from.
"""

import gzip
import hashlib
import io
import os
import struct
import sys
import tarfile

CHUNK_TAR_DRIVE0 = 0x05
GZIP_MAGIC = b"\x1f\x8b"
EFNT_HEADER = 32  # bytes


# --------------------------------------------------------------- container


def read_chunks(path):
    """Yield (chunk_type, payload) for every chunk in an .upgrade file."""
    data = open(path, "rb").read()
    if len(data) < 8:
        raise ValueError(f"{path}: too short to be an upgrade file")
    claimed = struct.unpack("<i", data[:4])[0]
    if not 0 < claimed <= len(data):
        raise ValueError(f"{path}: implausible claimed length {claimed}")
    pos = 4
    while pos + 8 <= claimed:
        ctype, clen = struct.unpack("<II", data[pos:pos + 8])
        if pos + 8 + clen > len(data):
            break  # truncated/padding chunk; nothing sane left to read
        if clen:
            # Zero-length chunks (e.g. CHUNK_PUMPHDA = 0x20) are separators
            # between the real drive-image chunks, so pass over them.
            yield ctype, data[pos + 8:pos + 8 + clen]
        pos += 8 + clen


# ------------------------------------------------------------------- tar


def fonts_from_tar(blob):
    """Yield (name, bytes) for every .bf file inside a tar blob."""
    try:
        tf = tarfile.open(fileobj=io.BytesIO(blob))
    except tarfile.TarError:
        return
    for member in tf.getmembers():
        if member.isfile() and member.name.lower().endswith(".bf"):
            fh = tf.extractfile(member)
            if fh:
                yield os.path.basename(member.name), fh.read()


# ------------------------------------------------------------------ ext2


class Ext2:
    """Read-only walker for the ext2 images the firmware ships. Only the parts
    needed to pull small files (fonts) out of the drive filesystem."""

    def __init__(self, data):
        self.data = data
        sb = data[1024:2048]
        if struct.unpack("<H", sb[56:58])[0] != 0xEF53:
            raise ValueError("not an ext2 filesystem")
        self.block_size = 1024 << struct.unpack("<I", sb[24:28])[0]
        blocks = struct.unpack("<I", sb[4:8])[0]
        self.inodes_per_group = struct.unpack("<I", sb[40:44])[0]
        self.inode_size = struct.unpack("<H", sb[88:90])[0] or 128
        groups = (blocks + self.inodes_per_group - 1) // self.inodes_per_group
        gd_start = self.block_size * (2 if self.block_size == 1024 else 1)
        self.gds = data[gd_start:gd_start + 32 * groups]

    def block(self, n):
        off = n * self.block_size
        return self.data[off:off + self.block_size]

    def inode(self, ino):
        # Inodes past the first group (all of /empeg on these images) live in
        # another group's inode table, hence the group descriptor lookup.
        group, index = divmod(ino - 1, self.inodes_per_group)
        table = struct.unpack("<I", self.gds[group * 32 + 8:group * 32 + 12])[0]
        off = table * self.block_size + index * self.inode_size
        return self.data[off:off + self.inode_size]

    def _indirect(self, block_no, depth, out):
        if depth == 0 or not block_no:
            return
        entries = struct.unpack("<%dI" % (self.block_size // 4), self.block(block_no))
        for n in entries:
            if n:
                if depth == 1:
                    out += self.block(n)
                else:
                    self._indirect(n, depth - 1, out)

    def read(self, inode):
        size = struct.unpack("<I", inode[4:8])[0]
        ptrs = struct.unpack("<15I", inode[40:100])
        out = bytearray()
        for n in ptrs[:12]:
            if n:
                out += self.block(n)
        for depth, ptr in ((1, ptrs[12]), (2, ptrs[13])):
            if size > len(out) and ptr:
                self._indirect(ptr, depth, out)
        return bytes(out[:size])

    def walk(self, inode_number=2, prefix=""):
        """Yield (path, bytes) for every regular file in the tree."""
        data = self.read(self.inode(inode_number))
        pos = 0
        while pos + 8 <= len(data):
            ino, rec_len, name_len, _ = struct.unpack("<IHBB", data[pos:pos + 8])
            if rec_len < 8:
                break
            name = data[pos + 8:pos + 8 + name_len].decode("latin-1")
            if name not in (".", "..") and ino:
                child = self.inode(ino)
                mode = struct.unpack("<H", child[:2])[0]
                path = f"{prefix}{name}"
                if (mode & 0o170000) == 0o040000:
                    yield from self.walk(ino, path + "/")
                elif (mode & 0o170000) == 0o100000:
                    yield path, self.read(child)
            pos += rec_len


# ------------------------------------------------------------------ fonts


def validate_font(blob):
    """Validate an EFNT header; returns a description or raises ValueError."""
    if len(blob) < EFNT_HEADER or blob[:4] != b"EFNT":
        raise ValueError("missing EFNT signature")
    _, file_size, version, max_width, _, height, first_index, num_chars = struct.unpack(
        "<4s7i", blob[:EFNT_HEADER])
    expected = EFNT_HEADER + (4 + 4 * height) * num_chars
    if version == 2:
        num_actual, _ = struct.unpack("<ii", blob[EFNT_HEADER:EFNT_HEADER + 8])
        expected += 8 + 2 * num_actual
    if expected != file_size or len(blob) < file_size:
        if version == 2:
            raise ValueError(
                f"unrecognised EFNT v2 layout (declared {file_size}, computed "
                f"{expected}) - the v3.00-alpha fonts use a newer variant this "
                f"extractor does not decode; use a v2.00/v2.01 image")
        raise ValueError(
            f"inconsistent header (declared {file_size}, computed {expected}, "
            f"have {len(blob)})")
    return (f"v{version}, {height}px tall, max {max_width}px wide, "
            f"chars {first_index}..{first_index + num_chars - 1}")


# ------------------------------------------------------------------- main


def main(argv):
    if not 2 <= len(argv) <= 3:
        print("usage: extract_player_fonts_from_upgrade.py <upgrade-file-or-dir> [outdir]",
              file=sys.stderr)
        return 2

    src = argv[1]
    repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    outdir = argv[2] if len(argv) == 3 else os.path.join(repo_root, "fixtures", "ghostwheel", "fonts")

    if os.path.isdir(src):
        files = sorted(
            os.path.join(root, f)
            for root, _dirs, names in os.walk(src)
            for f in names if f.endswith(".upgrade")
        )
    else:
        files = [src]
    if not files:
        print(f"no .upgrade files found in {src}", file=sys.stderr)
        return 1

    os.makedirs(outdir, exist_ok=True)
    seen_hashes = {}
    saved = []

    for path in files:
        print(f"== {os.path.basename(path)}")
        try:
            chunks = list(read_chunks(path))
        except ValueError as exc:
            print(f"   skipped: {exc}")
            continue
        for ctype, payload in chunks:
            if ctype == CHUNK_TAR_DRIVE0:
                candidates = [(f"chunk 0x{ctype:02x} tar", list(fonts_from_tar(payload)))]
            elif payload[:2] == GZIP_MAGIC:
                try:
                    image = gzip.decompress(payload)
                except OSError as exc:
                    print(f"   chunk 0x{ctype:02x}: gunzip failed: {exc}")
                    continue
                fonts = []
                try:
                    fs = Ext2(image)
                except ValueError:
                    continue
                for member, content in fs.walk():
                    if member.lower().endswith(".bf") and "/fonts/" in member.lower():
                        fonts.append((os.path.basename(member), content))
                candidates = [(f"chunk 0x{ctype:02x} ext2", fonts)]
            else:
                continue

            for label, fonts in candidates:
                for name, content in fonts:
                    digest = hashlib.sha256(content).hexdigest()
                    if digest in seen_hashes:
                        print(f"   {label}: {name} (duplicate of {seen_hashes[digest]})")
                        continue
                    try:
                        desc = validate_font(content)
                    except ValueError as exc:
                        print(f"   {label}: {name} rejected: {exc}")
                        continue
                    seen_hashes[digest] = name
                    dest = os.path.join(outdir, name)
                    with open(dest, "wb") as fh:
                        fh.write(content)
                    saved.append(dest)
                    print(f"   {label}: {name} -> {dest} ({len(content)} bytes; {desc})")

    if not saved:
        print("\nNo fonts extracted - no chunk in these files carried a drive "
              "filesystem with /empeg/lib/fonts.")
        return 1

    print(f"\n{len(saved)} font file(s) written to {outdir}")
    print("Point the simulator at the fixtures directory holding them, e.g.:")
    print(f'  ./gradlew :simulator:run --args="--port=8091 --fixtures={os.path.dirname(outdir)}"')
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
