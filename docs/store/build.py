"""Store art from the finished logo (UI overhaul phase 8). Run from anywhere: python docs/store/build.py

Everything here is the hand-drawn pixel art of phase 2 (src/main/resources: logo.png and the kit/shared logo_* sprites) put on the tile colour and
enlarged by a WHOLE number (nearest neighbour), so every drawn texel stays one crisp square. Nothing is redrawn, smoothed or generated.

  avatar.png         512 x 512  the 64 px tile, 8x                         CurseForge project avatar (square, at least 400 x 400)
  banner.png        1536 x 432  the emblem and wordmark lockup, 6x         wide header / gallery banner
  readme-header.png  672 x 204  the same lockup, 3x                        README and wiki header

CurseForge only states the avatar size (square, 400 x 400 or more); it states no size for a header banner, so the banner is simply a wide 32:9 picture
in whole-number art pixels.
"""
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
RES = os.path.join(ROOT, 'src', 'main', 'resources')
SHARED = os.path.join(RES, 'assets', 'supernaturalbloodlines', 'textures', 'gui', 'sprites', 'kit', 'shared')
TILE = (0x12, 0x06, 0x0A, 255)  # the tile colour of logo.py: the lockup is drawn for it

# name -> (source, art-pixel canvas or None for the picture itself, whole-number scale)
JOBS = {
    'avatar.png': (os.path.join(RES, 'logo.png'), None, 8),
    'banner.png': (os.path.join(SHARED, 'logo_lockup.png'), (256, 72), 6),
    'readme-header.png': (os.path.join(SHARED, 'logo_lockup.png'), (224, 68), 3),
}


def export(source, canvas, scale):
    art = Image.open(source).convert('RGBA')
    if canvas is not None:
        w, h = canvas
        base = Image.new('RGBA', (w, h), TILE)
        base.alpha_composite(art, ((w - art.width) // 2, (h - art.height) // 2))
        art = base
    return art.resize((art.width * scale, art.height * scale), Image.NEAREST)


if __name__ == '__main__':
    for name, (source, canvas, scale) in JOBS.items():
        out = export(source, canvas, scale)
        out.save(os.path.join(HERE, name), optimize=True)
        print(name, out.size)
