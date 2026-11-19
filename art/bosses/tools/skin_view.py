"""Flat front/back/side views of a 64x64 skin (both layers) for quick checks without Blockbench."""
from PIL import Image


def _face(im, s, part, side):
    x, y, w, h = s.face(part, side)
    return im.crop((x, y, x + w, y + h))


def _layer(im, s, base, over, side, flip=False):
    out = _face(im, s, base, side).copy()
    o = _face(im, s, over, side)
    out.alpha_composite(o)
    if flip:
        out = out.transpose(Image.FLIP_LEFT_RIGHT)
    return out


def views(s, scale=8, bg=(206, 212, 222)):
    im = s.im
    parts = [('head', 'hat'), ('body', 'jacket'), ('rarm', 'rsleeve'), ('larm', 'lsleeve'), ('rleg', 'rpants'), ('lleg', 'lpants')]
    W = 16 + 2 + 16 + 2 + 8 + 2 + 8
    sheet = Image.new('RGBA', (W, 32), bg + (255,))

    def paste(img, x, y):
        sheet.alpha_composite(img.convert('RGBA'), (x, y))

    # front
    paste(_layer(im, s, 'head', 'hat', 'front'), 4, 0)
    paste(_layer(im, s, 'body', 'jacket', 'front'), 4, 8)
    paste(_layer(im, s, 'rarm', 'rsleeve', 'front'), 4 - s.face('rarm', 'front')[2], 8)
    paste(_layer(im, s, 'larm', 'lsleeve', 'front'), 12, 8)
    paste(_layer(im, s, 'rleg', 'rpants', 'front'), 4, 20)
    paste(_layer(im, s, 'lleg', 'lpants', 'front'), 8, 20)
    # back
    ox = 18
    paste(_layer(im, s, 'head', 'hat', 'back'), ox + 4, 0)
    paste(_layer(im, s, 'body', 'jacket', 'back'), ox + 4, 8)
    paste(_layer(im, s, 'larm', 'lsleeve', 'back'), ox + 4 - s.face('larm', 'back')[2], 8)
    paste(_layer(im, s, 'rarm', 'rsleeve', 'back'), ox + 12, 8)
    paste(_layer(im, s, 'lleg', 'lpants', 'back'), ox + 4, 20)
    paste(_layer(im, s, 'rleg', 'rpants', 'back'), ox + 8, 20)
    # right and left side
    for k, side in enumerate(('right', 'left')):
        ox = 36 + k * 10
        paste(_layer(im, s, 'head', 'hat', side), ox, 0)
        paste(_layer(im, s, 'body', 'jacket', side), ox + 2, 8)
        arm = ('rarm', 'rsleeve') if side == 'right' else ('larm', 'lsleeve')
        leg = ('rleg', 'rpants') if side == 'right' else ('lleg', 'lpants')
        paste(_layer(im, s, arm[0], arm[1], side), ox + 2, 8)
        paste(_layer(im, s, leg[0], leg[1], side), ox + 2, 20)
    return sheet.resize((sheet.width * scale, sheet.height * scale), Image.NEAREST)
