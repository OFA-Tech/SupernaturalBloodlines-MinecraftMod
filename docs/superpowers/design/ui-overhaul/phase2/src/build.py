"""Draws the phase 2 art kit. Run: python build.py [kit|icons|portraits|font|logo|all]

Writes the PNGs into src/main/resources, the contact sheets and manifest.json next to this folder, then checks the result.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import px  # noqa: E402


def run_kit():
    import kit
    names = None
    for k in (kit.Grimoire(), kit.Moon(), kit.Tensura()):
        sprites = kit.build_theme(k)
        kit.kit_sheet(k, sprites, os.path.join(px.PHASE2, 'kit-%s.png' % k.id))
        got = [n for n, _, _ in sprites]
        assert names is None or names == got, 'the themes must have the same sprite names'
        names = got
        print(k.id, len(sprites), 'sprites')
    shared = kit.build_shared()
    px.sheet(os.path.join(px.PHASE2, 'kit-shared.png'), [(n, c) for n, c in shared if c.h < 20]
             + [(n + ' frame %d' % f, frame(c, f)) for n, c in shared if c.h >= 20 for f in ((4, 12, 20, 28, 32) if c.w == 16 else (0, 6, 15, 24))], px.rgba('#5C9A3C'), zoom=4)
    print('shared', len(shared))


def frame(cv, i):
    f = px.Cv(cv.w, cv.w)
    for y in range(cv.w):
        for x in range(cv.w):
            f.d[y * cv.w + x] = cv.d[(i * cv.w + y) * cv.w + x]
    return f


def run_icons():
    import icons
    icons.build()


def run_portraits():
    import portraits
    portraits.build()


def run_font():
    import font
    font.build()


def run_logo():
    import logo
    logo.build()


def check():
    """The self-check of the plan's task 7."""
    bad = []
    for m in px.MANIFEST:
        cv = m['_cv']
        if not m['glow'] and any(0 < px.A(c) < 255 for c in cv.d if c):
            bad.append('partly transparent pixel in ' + m['path'])
        if '/icons/' in m['path']:
            b = cv.bounds()
            if b is None or (cv.w > 12 and (b[0] < 1 or b[1] < 1 or b[2] > cv.w - 2 or b[3] > cv.h - 2)):
                bad.append('icon touches its canvas edge: ' + m['path'])
        if m['slice']:
            l, t, r, b = m['slice']
            if l + r >= cv.w + (1 if l + r == 0 else 0) or t + b >= cv.h + (1 if t + b == 0 else 0):
                bad.append('slice leaves no centre: ' + m['path'])
    paths = {m['path'] for m in px.MANIFEST}
    for p in paths:
        if '/icons/32/' in p and p.replace('/icons/32/', '/icons/16/') not in paths:
            bad.append('no 16 px icon for ' + p)
    assert not bad, '\n'.join(bad)


def write_manifest(parts):
    path = os.path.join(px.PHASE2, 'manifest.json')
    old = {}
    if os.path.exists(path):
        with open(path, encoding='utf8') as f:
            old = {m['path']: m for m in json.load(f)['sprites']}
    for m in px.MANIFEST:
        old[m['path']] = {k: v for k, v in m.items() if k != '_cv'}
    with open(path, 'w', encoding='utf8', newline='\n') as f:
        json.dump({'note': 'Every sprite of the phase 2 art kit. Paths are under assets/supernaturalbloodlines/textures/gui/ unless they start with assets/ or logo. '
                           'slice = left, top, right, bottom of a nine-slice (a zero pair means a three-slice along the other axis). glow = has stepped alpha by design.',
                   'sprites': [old[k] for k in sorted(old)]}, f, indent=1)
        f.write('\n')
    print('manifest:', len(old), 'sprites')


if __name__ == '__main__':
    what = sys.argv[1:] or ['all']
    parts = ['kit', 'icons', 'portraits', 'font', 'logo'] if 'all' in what else what
    for p in parts:
        globals()['run_' + p]()
    check()
    write_manifest(parts)
