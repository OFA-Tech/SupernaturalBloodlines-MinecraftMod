"""Contact sheet of previews: py art/items/sheet.py <out.png> [names...]  (gui | third person | ground per item)."""
import sys
from pathlib import Path

from PIL import Image, ImageDraw

PREV = Path(__file__).resolve().parents[2] / "art/previews/items"
CELL, COLS = 150, 4


def crop(im, box):
    return im.convert("RGB").crop(box).resize((CELL, CELL), Image.NEAREST)


def main():
    out = sys.argv[1]
    names = sys.argv[2:] or sorted(p.name[:-8] for p in PREV.glob("*_gui.png"))
    rows = (len(names) + COLS - 1) // COLS
    W, H = CELL * 3 + 8, CELL + 16
    sheet = Image.new("RGB", (W * COLS, H * rows), (60, 64, 72))
    dr = ImageDraw.Draw(sheet)
    for i, n in enumerate(names):
        x, y = (i % COLS) * W, (i // COLS) * H
        gui = PREV / f"{n}_gui.png"
        tp = PREV / f"{n}_tp.png"
        gr = PREV / f"{n}_ground.png"
        if gui.exists():
            sheet.paste(crop(Image.open(gui), (32, 32, 480, 480)), (x, y))
        if tp.exists():
            sheet.paste(crop(Image.open(tp), (130, 110, 450, 430)), (x + CELL, y))
        if gr.exists():
            sheet.paste(crop(Image.open(gr), (190, 20, 510, 340)), (x + CELL * 2, y))
        dr.text((x + 3, y + CELL + 2), n, fill=(255, 255, 255))
    sheet.save(out)
    print(out, sheet.size, len(names))


main()

