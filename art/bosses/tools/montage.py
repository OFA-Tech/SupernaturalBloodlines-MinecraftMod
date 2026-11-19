"""python montage.py out.png in1.png in2.png ...  -> crops all views to one shared box and tiles them."""
import sys
from PIL import Image, ImageChops

BG = (206, 212, 222)


def flat(f):
    im = Image.open(f).convert('RGBA')
    bg = Image.new('RGBA', im.size, BG + (255,))
    bg.alpha_composite(im)
    return bg.convert('RGB')


out, files = sys.argv[1], sys.argv[2:]
ims = [flat(f) for f in files]
box = None
for im in ims:
    b = ImageChops.difference(im, Image.new('RGB', im.size, BG)).getbbox()
    if b:
        box = b if box is None else (min(box[0], b[0]), min(box[1], b[1]), max(box[2], b[2]), max(box[3], b[3]))
pad = 12
box = (max(0, box[0] - pad), max(0, box[1] - pad), min(ims[0].width, box[2] + pad), min(ims[0].height, box[3] + pad))
crops = [im.crop(box) for im in ims]
w, h = crops[0].size
sheet = Image.new('RGB', (w * len(crops), h), BG)
for i, c in enumerate(crops):
    sheet.paste(c, (i * w, 0))
sheet.save(out)
print(sheet.size)
