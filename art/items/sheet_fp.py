"""First-person contact sheet: py art/items/sheet_fp.py <out.png> [names...]; the two fp_ref_* sprites come first for comparison."""
import sys
from pathlib import Path

from PIL import Image, ImageDraw

PREV = Path(__file__).resolve().parents[2] / "art/previews/items"
CW, CH, COLS = 300, 169, 5


def main():
    out = sys.argv[1]
    names = sys.argv[2:] or ["fp_ref_gen", "fp_ref_tool"] + sorted(p.name[:-7] for p in PREV.glob("*_fp.png") if not p.name.startswith("fp_ref"))
    rows = (len(names) + COLS - 1) // COLS
    sheet = Image.new("RGB", (CW * COLS, (CH + 12) * rows), (40, 42, 48))
    dr = ImageDraw.Draw(sheet)
    for i, n in enumerate(names):
        x, y = (i % COLS) * CW, (i // COLS) * (CH + 12)
        sheet.paste(Image.open(PREV / f"{n}_fp.png").convert("RGB").resize((CW - 2, CH)), (x, y))
        dr.text((x + 3, y + CH), n, fill=(255, 255, 255))
    sheet.save(out)
    print(out, sheet.size, len(names))


main()
