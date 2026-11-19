"""Paints the 16x16 texture of Sebastien Valet's tricorn (item model sebastien_tricorn.json).
Run: python paint_tricorn.py <out.png>

Layout (1 texel = 1 model unit = 1 skin pixel when worn):
  (0,0)-(10,10)   crown top (the raised cap uses the lighter 6x6 middle)
  (10,0)-(13,4)   cockade: black ribbon, gold loop and button
  (10,4)-(16,10)  plain felt (brim plate)
  (0,10)-(16,14)  cocked brim wall: gold lace on the top row
  (0,12)-(10,16)  crown side, hat band on the bottom row
Colours: felt and ribbon sampled from the Met tricorne 26.56.88; lace is the brass of his coat buttons
(French hats were laced in the button colour) alternated with the Met braid highlight."""
import sys
from PIL import Image

FELT_D, FELT, FELT_L = (23, 24, 19), (28, 29, 23), (34, 34, 28)
GOLD, GOLD_L = (192, 156, 66), (182, 165, 114)
BRAID = tuple((a + b) // 2 for a, b in zip(GOLD, GOLD_L))
RIB, RIB_L = (43, 44, 33), (63, 63, 44)

im = Image.new('RGBA', (16, 16), FELT + (255,))
px = im.load()
for y in range(10):
    for x in range(10):
        edge = x in (0, 9) or y in (0, 9)
        px[x, y] = (FELT_D if edge else FELT_L if 2 <= x < 8 and 2 <= y < 8 else FELT) + (255,)
for x in range(16):
    px[x, 10] = (GOLD if x % 2 == 0 else BRAID) + (255,)
    px[x, 13] = FELT_D + (255,)
for x in range(10):
    px[x, 15] = RIB + (255,)
COCKADE = ['rRr',
           'RgR',
           'rgr',
           '.b.']
KEY = {'r': RIB, 'R': RIB_L, 'g': GOLD_L, 'b': GOLD}
for y, row in enumerate(COCKADE):
    for x, k in enumerate(row):
        px[10 + x, y] = KEY[k] + (255,) if k in KEY else (0, 0, 0, 0)
assert im.size == (16, 16)
im.save(sys.argv[1])
