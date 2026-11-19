"""Builds every 16x16 texture: pixel grids in src/*.txt (boss icons, 3D atlases), icons.py (drawn icons), compass recolour.

Run from the repo root: py art/items/gen_textures.py [--sheet out.png]
"""
import re
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).parent))
from icons import ICONS  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "art/items/src"
OUT = ROOT / "src/main/resources/assets/supernaturalbloodlines/textures/item"
OUT3D = OUT / "3d"
COMPASS = {(0x96, 0x96, 0xA5): (0xB8, 0x90, 0x3C), (0x2A, 0x14, 0x34): (0xE6, 0xDC, 0xC2),
           (0xCD, 0x1E, 0x2D): (0xB0, 0x1C, 0x24), (0xEB, 0xEB, 0xEB): (0x3A, 0x2A, 0x1C)}


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


def recolor_compass():
    for f in OUT.glob("vampire_compass_*.png"):
        im = Image.open(f).convert("RGBA")
        for y in range(16):
            for x in range(16):
                p = im.getpixel((x, y))
                if p[:3] in COMPASS:
                    im.putpixel((x, y), COMPASS[p[:3]] + (p[3],))
        im.save(f)


if __name__ == "__main__":
    OUT3D.mkdir(exist_ok=True)
    imgs = {}
    for name, pal, rows in parse(SRC / "icons.txt"):
        imgs[name] = render(pal, rows)
    for name, cv in ICONS.items():
        imgs[name] = cv.img()
    for n, im in imgs.items():
        im.save(OUT / f"{n}.png")
    for name, pal, rows in parse(SRC / "models.txt"):
        render(pal, rows).save(OUT3D / f"{name}.png")
    recolor_compass()
    print(len(imgs), "icons written")
    if "--sheet" in sys.argv:
        from PIL import ImageDraw
        names = sorted(imgs) + ["vampire_compass_00", "vampire_compass_08"]
        for n in ("vampire_compass_00", "vampire_compass_08"):
            imgs[n] = Image.open(OUT / f"{n}.png").convert("RGBA")
        cols, W = 8, 112
        sheet = Image.new("RGB", (W * cols, (W + 14) * ((len(names) + cols - 1) // cols)), (120, 126, 138))
        dr = ImageDraw.Draw(sheet)
        for i, n in enumerate(names):
            x, y = (i % cols) * W, (i // cols) * (W + 14)
            sheet.paste(imgs[n].resize((96, 96), Image.NEAREST), (x + 8, y), imgs[n].resize((96, 96), Image.NEAREST))
            dr.text((x + 2, y + 98), n[:18], fill=(255, 255, 255))
        sheet.save(sys.argv[sys.argv.index("--sheet") + 1])
