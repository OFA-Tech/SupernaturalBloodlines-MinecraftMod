"""Contact sheet of the group-type icons (docs/superpowers/design/ui-overhaul/group-icons.png).

The paw and the three new icons at 32 and 16 px, 8x with nearest neighbour, on each theme's own colour, then a mock nav
row (the real tab sprites) and the silhouettes. Run: python group_icons_sheet.py [extra output path ...]
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from px import GUI, PHASE2  # noqa: E402

Z = 8
SPR = os.path.join(GUI, 'sprites')
NAMES = ['pack', 'coven', 'clan', 'brotherhood']
NEIGHBOURS = (['quests', 'rites', 'bloodline'], ['guide', 'loadout'])
# the paper each theme draws on (phase1/palettes.json): Grimoire pa, Blood Moon card, Tensura panel
THEMES = [('Grimoire', 'grimoire', '#E9DDB8', '#2A1A10'), ('Blood Moon', 'bloodmoon', '#1D0B13', '#FFF1EA'), ('Tensura', 'tensura', '#12293F', '#E6F3FB')]
# tab sprites: (left, top, right, bottom) borders of the nine slice in each tab.png.mcmeta
BORDER = {'grimoire': (6, 4, 5, 4), 'bloodmoon': (9, 0, 9, 0), 'tensura': (6, 4, 4, 4)}
FONT = ImageFont.truetype(r'C:\Windows\Fonts\consola.ttf', 22)
BIG = ImageFont.truetype(r'C:\Windows\Fonts\consolab.ttf', 28)


def icon(name, n):
    return Image.open(os.path.join(SPR, 'icons', str(n), name + '.png')).convert('RGBA')


def up(im, k=Z):
    return im.resize((im.width * k, im.height * k), Image.NEAREST)


def nine(im, w, h, b):
    """The nine slice GUI would draw: the corners as they are, the rest stretched."""
    l, t, r, bt = b
    xs, ys = [0, l, im.width - r, im.width], [0, t, im.height - bt, im.height]
    xd, yd = [0, l, w - r, w], [0, t, h - bt, h]
    out = Image.new('RGBA', (w, h))
    for j in range(3):
        for i in range(3):
            sw, sh, dw, dh = xs[i + 1] - xs[i], ys[j + 1] - ys[j], xd[i + 1] - xd[i], yd[j + 1] - yd[j]
            if sw and sh and dw and dh:
                out.alpha_composite(im.crop((xs[i], ys[j], xs[i + 1], ys[j + 1])).resize((dw, dh), Image.NEAREST), (xd[i], yd[j]))
    return out


def tab(theme, name, selected):
    t = Image.open(os.path.join(SPR, 'kit', theme, 'tab_selected.png' if selected else 'tab.png')).convert('RGBA')
    out = nine(t, 30, 24, BORDER[theme])
    out.alpha_composite(icon(name, 16), (7, 4))
    return out


def silhouette(im, c=(16, 16, 20, 255)):
    out = Image.new('RGBA', im.size, c)
    out.putalpha(im.getchannel('A').point(lambda a: 255 if a else 0))
    return out


def build(paths):
    pad, gap = 28, 56
    cell32, cell16 = 32 * Z + 24, 16 * Z + 24
    row_w = pad * 2 + 4 * cell32 + gap + 4 * cell16
    tabs_w = pad * 2 + 9 * (30 * Z + 24)
    W = max(row_w, tabs_w)
    block_h = 44 + 32 * Z + 36 + 30 + 24 * Z + 30 + 20
    sil_h = 44 + 32 * Z + 36 + 20
    sheet = Image.new('RGBA', (W, block_h * 3 + sil_h), (23, 23, 28, 255))
    d = ImageDraw.Draw(sheet)
    y = 0
    for title, theme, bg, fg in THEMES:
        d.rectangle([0, y, W, y + block_h], fill=bg)
        d.text((pad, y + 8), title + ' (' + bg + ')', fill=fg, font=BIG)
        oy = y + 44
        x = pad
        for n in (32, 16):
            for name in NAMES:
                im = up(icon(name, n))
                sheet.alpha_composite(im, (x + 12, oy + (32 * Z - n * Z) // 2 + 0))
                d.text((x + 12, oy + 32 * Z + 6), '%s %d' % (name, n), fill=fg, font=FONT)
                x += cell32 if n == 32 else cell16
            x += gap
        ty = oy + 32 * Z + 36 + 30
        d.text((pad, ty - 28), 'nav tabs, 16 px icon (the Group tab selected; the paw is today)', fill=fg, font=FONT)
        row = [(nm, False) for nm in NEIGHBOURS[0]] + [(nm, True) for nm in NAMES] + [(nm, False) for nm in NEIGHBOURS[1]]
        x = pad
        for nm, sel in row:
            sheet.alpha_composite(up(tab(theme, nm, sel)), (x, ty))
            x += 30 * Z + 24
        y += block_h
    d.rectangle([0, y, W, y + sil_h], fill='#E8E8EC')
    d.text((pad, y + 8), 'Silhouettes (shape first, colour second)', fill='#17171C', font=BIG)
    x = pad
    for n in (32, 16):
        for name in NAMES:
            sheet.alpha_composite(up(silhouette(icon(name, n))), (x + 12, y + 44 + (32 * Z - n * Z) // 2))
            d.text((x + 12, y + 44 + 32 * Z + 6), '%s %d' % (name, n), fill='#17171C', font=FONT)
            x += cell32 if n == 32 else cell16
        x += gap
    for p in paths:
        os.makedirs(os.path.dirname(os.path.abspath(p)), exist_ok=True)
        sheet.convert('RGB').save(p, optimize=True)
        print(p, sheet.size)


if __name__ == '__main__':
    build([os.path.join(PHASE2, '..', 'group-icons.png')] + sys.argv[1:])
