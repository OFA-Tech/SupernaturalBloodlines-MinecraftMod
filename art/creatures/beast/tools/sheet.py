"""python sheet.py out.png cols cell_w cell_h label=file [label=file ...]  -> labelled contact sheet."""
import sys
from PIL import Image, ImageDraw, ImageFont

out, cols, cw, ch = sys.argv[1], int(sys.argv[2]), int(sys.argv[3]), int(sys.argv[4])
items = [a.split("=", 1) for a in sys.argv[5:]]
rows = (len(items) + cols - 1) // cols
try:
    font = ImageFont.truetype("arial.ttf", max(12, ch // 22))
except OSError:
    font = ImageFont.load_default()
sheet = Image.new("RGB", (cols * cw, rows * ch), (24, 26, 30))
draw = ImageDraw.Draw(sheet)
for i, (label, path) in enumerate(items):
    im = Image.open(path).convert("RGBA")
    im.thumbnail((cw, ch), Image.LANCZOS)
    x, y = (i % cols) * cw, (i // cols) * ch
    cell = Image.new("RGBA", (cw, ch), (58, 62, 70, 255))
    cell.alpha_composite(im, ((cw - im.width) // 2, (ch - im.height) // 2))
    sheet.paste(cell.convert("RGB"), (x, y))
    draw.text((x + 8, y + 6), label, fill=(255, 235, 120), font=font, stroke_width=2, stroke_fill=(0, 0, 0))
sheet.save(out)
print(out, sheet.size)
