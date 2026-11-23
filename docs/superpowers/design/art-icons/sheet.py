"""The contact sheet of the plan 18 icons, from the PNGs that ship: every icon at 1x, 2x and 4x in the slot of each look and on its hub ground.

Run from the repo root, after `py docs/superpowers/design/ui-overhaul/phase2/src/build.py icons` and the item texture: py docs/superpowers/design/art-icons/sheet.py
"""
import json
import os

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..', '..', '..', '..'))
TEX = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'supernaturalbloodlines', 'textures')
SPR = os.path.join(TEX, 'gui', 'sprites')
with open(os.path.join(HERE, '..', 'ui-overhaul', 'phase1', 'palettes.json'), encoding='utf8') as f:
    PAL = json.load(f)
# the look, its kit folder, the ground the hub draws an icon on
LOOKS = [('Grimoire', 'grimoire', PAL['grimoire']['pa']), ('Blood Moon', 'bloodmoon', PAL['bloodmoon']['card']), ('Tensura', 'tensura', PAL['tensura']['panel'])]
# the row's name, the 32 and 16 px icon, the HUD action (None: a skill with no slot)
ROWS = [('Rip Heart', 's-vam-8', 'rip_heart'), ('Neck Snap', 's-vam-9', 'neck_snap'), ('Bottle my blood', 'bottle_blood', 'bottle_blood'), ('Hybrid Leap', 's-hyb-3', 'hybrid_leap'),
        ('Garlic Ward', 's-aug-1', None), ('Blessed Skin', 's-aug-2', None)]
ZOOMS, PAD, LABEL = (1, 2, 4), 6, 150


def png(*rel):
    return Image.open(os.path.join(*rel)).convert('RGBA')


def rgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5)) + (255,)


def strip(sheet, x, y, im, ground=None):
    """`im` at each zoom, left to right, on `ground` when it has one. Returns the x after it."""
    for z in ZOOMS:
        big = im.resize((im.width * z, im.height * z), Image.NEAREST)
        if ground:
            ImageDraw.Draw(sheet).rectangle([x - 2, y - 2, x + big.width + 1, y + big.height + 1], fill=ground)
        sheet.alpha_composite(big, (x, y))
        x += big.width + PAD
    return x


def slot(kit, shape, icon):
    """A HUD slot as SlotArt draws it: the frame, and the icon's pale-outlined twin three texels in."""
    s = png(SPR, 'kit', kit, shape + '.png')
    s.alpha_composite(icon, (3, 3))
    return s


def main():
    slot_w = sum(28 * z + PAD for z in ZOOMS)
    hub_w = sum(32 * z + PAD for z in ZOOMS)
    look_w = slot_w * 2 + hub_w + PAD * 3
    row_h = 32 * 4 + PAD * 2
    extra = [('Wes\'s syringe (item, 16 px)', png(TEX, 'item', 'augustine_syringe.png')), ('Snapped (mob effect, 18 px)', png(TEX, 'mob_effect', 'snapped.png'))]
    W, H = LABEL + look_w * 3 + PAD, 34 + row_h * (len(ROWS) + len(extra)) + PAD
    sheet = Image.new('RGBA', (W, H), (34, 34, 40, 255))
    d = ImageDraw.Draw(sheet)
    for i, (look, kit, ground) in enumerate(LOOKS):
        x = LABEL + i * look_w
        d.text((x, 4), look, fill=(240, 240, 245, 255))
        d.text((x, 18), 'slot 1x 2x 4x', fill=(150, 150, 160, 255))
        d.text((x + slot_w + PAD, 18), 'square slot (Tensura mode)', fill=(150, 150, 160, 255))
        d.text((x + slot_w * 2 + PAD * 2, 18), 'hub 32 px (and 16)', fill=(150, 150, 160, 255))
    for r, (name, icon, action) in enumerate(ROWS):
        y = 34 + r * row_h
        d.text((6, y + 4), name, fill=(240, 240, 245, 255))
        d.text((6, y + 18), icon + (' / ' + action if action and action != icon else ''), fill=(150, 150, 160, 255))
        big, small = png(SPR, 'icons', '32', icon + '.png'), png(SPR, 'icons', '16', icon + '.png')
        for i, (look, kit, ground) in enumerate(LOOKS):
            x = LABEL + i * look_w
            if action:
                twin = png(SPR, 'icons', 'hud', action + '.png')
                strip(sheet, x, y, slot(kit, 'slot', twin))
                strip(sheet, x + slot_w + PAD, y, slot(kit, 'slot_square', twin))
            else:
                d.text((x, y + 4), 'no HUD slot', fill=(110, 110, 120, 255))
            hub = x + slot_w * 2 + PAD * 2
            strip(sheet, hub, y, big, rgb(ground))
            for z, sx in ((1, hub), (2, hub + 32 + PAD)):  # the 16 px icon as the hub's lists and the Loadout tab draw it, under the two small copies
                d.rectangle([sx - 2, y + 74, sx + 16 * z + 1, y + 77 + 16 * z], fill=rgb(ground))
                sheet.alpha_composite(small.resize((16 * z, 16 * z), Image.NEAREST), (sx, y + 76))
    for r, (name, im) in enumerate(extra):
        y = 34 + (len(ROWS) + r) * row_h
        d.text((6, y + 4), name, fill=(240, 240, 245, 255))
        x = LABEL
        for ground in ('#8B8B8B', '#C6C6C6', '#17171C', PAL['grimoire']['pa']):  # an inventory slot, the inventory, a dark and a light screen
            x = strip(sheet, x, y, im, rgb(ground)) + PAD * 3
    out = os.path.join(HERE, 'contact-sheet.png')
    sheet.convert('RGB').save(out, optimize=True)
    print(out, sheet.size)


if __name__ == '__main__':
    main()
