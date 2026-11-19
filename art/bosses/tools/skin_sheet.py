"""python skin_sheet.py out.png name1 name2 ...  (reads PNGs from the boss texture dir)"""
import sys, os
from PIL import Image, ImageDraw
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from skin_lib import Skin
from skin_view import views

TEX = os.path.join(os.path.dirname(__file__), '..', '..', '..', 'src', 'main', 'resources', 'assets', 'supernaturalbloodlines', 'textures', 'entity', 'boss')
out, names = sys.argv[1], sys.argv[2:]
tiles = []
for n in names:
    s = Skin(slim=(n == 'rebekah'))
    s.im = Image.open(os.path.join(TEX, n + '.png')).convert('RGBA'); s.px = s.im.load()
    v = views(s, 10)
    ImageDraw.Draw(v).text((4, 4), n, fill=(0, 0, 0, 255))
    tiles.append(v)
cols = 2
w, h = tiles[0].size
sheet = Image.new('RGBA', (w * cols, h * ((len(tiles) + cols - 1) // cols)), (206, 212, 222, 255))
for i, t in enumerate(tiles):
    sheet.paste(t, ((i % cols) * w, (i // cols) * h))
sheet.convert('RGB').save(out)
print(sheet.size)

