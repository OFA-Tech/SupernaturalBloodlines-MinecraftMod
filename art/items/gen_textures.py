"""Builds the 16x16 item textures from the pixel grids in art/items/src/*.txt.

Run from the repo root: py art/items/gen_textures.py [--sheet out.png]
"""
import re
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "art/items/src"
OUT = ROOT / "src/main/resources/assets/supernaturalbloodlines/textures/item"


def parse(path):
    parts = re.split(r"^\[(\w+)\]\s*$", path.read_text(), flags=re.M)
    for name, body in zip(parts[1::2], parts[2::2]):
        pal, rows = {}, []
        for line in body.strip("\n").splitlines():
            if re.fullmatch(r"\w=#[0-9a-fA-F]{6}", line):
                pal[line[0]] = tuple(int(line[3 + i:5 + i], 16) for i in (0, 2, 4)) + (255,)
            elif line.strip():
                rows.append(line)
        assert len(rows) == 16, f"{name}: {len(rows)} rows"
        for i, r in enumerate(rows):
            assert len(r) == 16, f"{name} row {i}: {len(r)} chars"
            assert all(c == "." or c in pal for c in r), f"{name} row {i}: unknown colour"
        yield name, pal, rows


def render(pal, rows):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, r in enumerate(rows):
        for x, c in enumerate(r):
            if c != ".":
                im.putpixel((x, y), pal[c])
    return im


if __name__ == "__main__":
    imgs = {}
    for f in sorted(SRC.glob("*.txt")):
        for name, pal, rows in parse(f):
            imgs[name] = render(pal, rows)
            imgs[name].save(OUT / f"{name}.png")
    print(len(imgs), "textures written")
    if "--sheet" in sys.argv:
        sheet = Image.new("RGBA", (len(imgs) * 136, 136), (70, 70, 80, 255))
        for i, (n, im) in enumerate(imgs.items()):
            sheet.alpha_composite(im.resize((128, 128), Image.NEAREST), (i * 136 + 4, 4))
        sheet.save(sys.argv[sys.argv.index("--sheet") + 1])
