"""UV layout of the Werewolves mod beast model (WerewolfBeastModel, 128x64).

Extracted from the 1.21-2.0.3.4 jar by parsing the bytecode of createBodyLayer()
(see art/bosses/README.md). Provides per-pixel face info for texture painting.
"""
import json, os

HERE = os.path.dirname(os.path.abspath(__file__))
MODEL = json.load(open(os.path.join(HERE, 'beast_model.json')))
W, H = 128, 64


def face_rects(tex, box, mirror):
    u, v = int(tex[0]), int(tex[1])
    w, h, d = int(box[3]), int(box[4]), int(box[5])
    f = {
        'up': (u + d, v, w, d),
        'down': (u + d + w, v, w, d),
        'east': (u, v + d, d, h),
        'north': (u + d, v + d, w, h),
        'west': (u + d + w, v + d, d, h),
        'south': (u + 2 * d + w, v + d, w, h),
    }
    if mirror:
        f['east'], f['west'] = f['west'], f['east']
    return f


def build_info():
    """info[(x, y)] = (part, face, lx, ly, fw, fh); later cubes win overlaps."""
    info = {}
    for name in MODEL['order']:
        for c in MODEL['parts'][name]['cubes']:
            for face, (x, y, w, h) in face_rects(c['tex'], c['box'], c['mirror']).items():
                for yy in range(h):
                    for xx in range(w):
                        if 0 <= x + xx < W and 0 <= y + yy < H:
                            info[(x + xx, y + yy)] = (name, face, xx, yy, w, h)
    return info


if __name__ == '__main__':
    print(len(build_info()), 'used pixels')
