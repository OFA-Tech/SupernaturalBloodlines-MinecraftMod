"""python grid.py out.png cols label1=file1.png label2=file2.png ...  -> labelled contact sheet, every frame cropped to the shared bbox."""
import sys
from PIL import Image, ImageChops, ImageDraw

BG = (206, 212, 222)
out, cols = sys.argv[1], int(sys.argv[2])
items = [a.split('=', 1) for a in sys.argv[3:]]


def flat(f):
    im = Image.open(f).convert('RGBA')
    bg = Image.new('RGBA', im.size, BG + (255,))
    bg.alpha_composite(im)
    return bg.convert('RGB')


ims = [(l, flat(f)) for l, f in items]
box = None
for _, im in ims:
    b = ImageChops.difference(im, Image.new('RGB', im.size, BG)).getbbox()
    if b:
        box = b if box is None else (min(box[0], b[0]), min(box[1], b[1]), max(box[2], b[2]), max(box[3], b[3]))
pad = 8
box = (max(0, box[0] - pad), max(0, box[1] - pad), min(ims[0][1].width, box[2] + pad), min(ims[0][1].height, box[3] + pad))
crops = [(l, im.crop(box)) for l, im in ims]
w, h = crops[0][1].size
rows = (len(crops) + cols - 1) // cols
sheet = Image.new('RGB', (w * cols, h * rows), BG)
d = ImageDraw.Draw(sheet)
for i, (l, c) in enumerate(crops):
    x, y = (i % cols) * w, (i // cols) * h
    sheet.paste(c, (x, y))
    d.text((x + 6, y + 4), l, fill=(20, 20, 30))
sheet.save(out)
print(sheet.size)
