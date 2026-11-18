"""python previews.py FRAMES_DIR OUT_DIR  -> beast_front/side/three_quarter/head.png and roar/rear_up/attack_bite 3-frame sheets."""
import sys
from PIL import Image, ImageChops

BG = (206, 212, 222)
frames, out = sys.argv[1], sys.argv[2]


def flat(f):
    im = Image.open(f).convert('RGBA')
    bg = Image.new('RGBA', im.size, BG + (255,))
    bg.alpha_composite(im)
    return bg.convert('RGB')


def bbox(ims, pad=14):
    box = None
    for im in ims:
        b = ImageChops.difference(im, Image.new('RGB', im.size, BG)).getbbox()
        if b:
            box = b if box is None else (min(box[0], b[0]), min(box[1], b[1]), max(box[2], b[2]), max(box[3], b[3]))
    w, h = ims[0].size
    return (max(0, box[0] - pad), max(0, box[1] - pad), min(w, box[2] + pad), min(h, box[3] + pad))


def single(name, dst):
    im = flat(f'{frames}/f_{name}.png')
    im.crop(bbox([im])).save(f'{out}/{dst}.png')


def sheet(names, dst):
    ims = [flat(f'{frames}/f_{n}.png') for n in names]
    box = bbox(ims)
    crops = [i.crop(box) for i in ims]
    w, h = crops[0].size
    s = Image.new('RGB', (w * len(crops), h), BG)
    for i, c in enumerate(crops):
        s.paste(c, (i * w, 0))
    s.save(f'{out}/{dst}.png')


single('idle_front', 'beast_front')
single('idle_side', 'beast_side')
single('idle_q', 'beast_three_quarter')
single('idle_head', 'beast_head')
sheet(['roar_0.5', 'roar_0.8', 'roar_1.4'], 'beast_roar')
sheet(['rear_0.5', 'rear_1.3', 'rear_2.1'], 'beast_rear_up')
sheet(['bite_0.32', 'bite_0.42', 'bite_0.5'], 'beast_attack_bite')
