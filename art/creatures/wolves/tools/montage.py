"""python montage.py out.png cols cell_px in1.png in2.png ...  (labels each cell with the file name)"""
import sys, os
from PIL import Image, ImageDraw

out, cols, cell = sys.argv[1], int(sys.argv[2]), int(sys.argv[3])
files = sys.argv[4:]
rows = (len(files) + cols - 1) // cols
sheet = Image.new('RGB', (cols * cell, rows * cell), (235, 238, 242))
d = ImageDraw.Draw(sheet)
for i, f in enumerate(files):
    im = Image.open(f).convert('RGBA')
    bg = Image.new('RGBA', im.size, (235, 238, 242, 255))
    bg.alpha_composite(im)
    im = bg.convert('RGB').resize((cell, cell), Image.LANCZOS)
    x, y = (i % cols) * cell, (i // cols) * cell
    sheet.paste(im, (x, y))
    d.text((x + 6, y + 4), os.path.splitext(os.path.basename(f))[0], fill=(20, 20, 20))
sheet.save(out)
