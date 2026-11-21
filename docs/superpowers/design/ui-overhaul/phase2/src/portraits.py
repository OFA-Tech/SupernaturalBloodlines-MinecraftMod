"""Boss portraits, 24 x 24, one per form. Drawn from the boss skins (hair, skin and eye colours, beard, glasses, the
clothes at the collar) and from the geo textures of the wolves (fur and eye colours). They are drawings, not renders.
"""
import os

import kit
from icons import IR, Ic, wolf
from px import PHASE2, WHITE, Cv, emit, mix, ramp, rgba, sheet, with_a

SKIN = {'pale': ('#E6D2C2', '#D0B8A6', '#B49A88'), 'fair': ('#F0D8C4', '#D8B8A0', '#BE9A84'), 'warm': ('#E2C1A7', '#C8A48A', '#AE8A72'),
        'rose': ('#F4E2D4', '#DCC4B4', '#C4A898'), 'tan': ('#D9A07A', '#BC8460', '#9A6848'), 'brown': ('#97694C', '#7E543C', '#684430')}
INK = '#17121A'


def bust(o):
    """A head and shoulders, lit from the top-left. `o` holds the colours and the features that tell the bosses apart."""
    c = Cv(24, 24).rect(0, 0, 24, 24, rgba(o['bg']))
    c.rect(0, 0, 24, 1, mix(rgba(o['bg']), WHITE, 0.08)).rect(0, 22, 24, 2, mix(rgba(o['bg']), 0xFF000000, 0.25))
    s1, s2, s3 = [rgba(v) for v in SKIN[o['skin']]]
    h1, h2 = rgba(o['hair'][0]), rgba(o['hair'][1])
    coat, coat2 = rgba(o['coat'][0]), rgba(o['coat'][1])
    # shoulders and collar
    c.rect(2, 20, 20, 4, coat).rect(3, 19, 18, 1, coat).rect(18, 20, 4, 4, coat2).hl(3, 19, 15, mix(coat, WHITE, 0.18))
    c.rect(9, 17, 6, 3, s2).hl(9, 19, 6, s3)
    shirt = rgba(o.get('shirt', '#E4DDCB'))
    c.bmp(['#....#', '##..##', '.####.', '..##..'], {'#': shirt}, 9, 19) if o.get('collar', 'v') == 'v' else c.rect(9, 20, 6, 4, shirt)
    if 'tie' in o:
        c.rect(11, 21, 2, 3, rgba(o['tie']))
    if 'trim' in o:
        c.vl(8, 20, 4, rgba(o['trim'])).vl(15, 20, 4, rgba(o['trim']))
    # head
    c.rect(5, 4, 14, 14, s1).rect(18, 5, 1, 12, s2).hl(6, 17, 12, s2).vl(17, 13, 4, s2)
    c.px(5, 17, rgba(o['bg'])).px(18, 17, rgba(o['bg'])).px(5, 16, s2).px(18, 16, s3)
    c.rect(4, 10, 1, 2, s2).rect(19, 10, 1, 2, s3)  # ears
    # hair
    style = o.get('style', 'short')
    c.rect(5, 2, 14, 3, h1).hl(6, 1, 12, h1).hl(5, 5, 14, h2).px(5, 2, rgba(o['bg'])).px(18, 2, rgba(o['bg']))
    c.hl(7, 2, 6, mix(h1, WHITE, 0.25))
    c.vl(5, 5, 4, h1).vl(18, 5, 4, h2)
    if style == 'curls':
        for x, y in ((6, 5), (7, 6), (9, 5), (10, 6), (12, 5), (14, 6), (15, 5), (17, 6), (4, 4), (4, 6), (19, 4), (19, 6), (8, 0), (11, 0), (15, 0)):
            c.px(x, y, h1)
        for x, y in ((8, 6), (13, 6), (16, 6), (6, 3), (11, 3), (15, 3)):
            c.px(x, y, h2)
    elif style == 'slick':
        c.hl(5, 5, 14, s1).hl(5, 4, 14, h2).hl(13, 2, 5, h2).px(12, 3, h2)
        c.vl(5, 5, 3, h2).vl(18, 5, 3, h2)
    elif style == 'long':
        c.rect(3, 3, 2, 17, h1).rect(19, 3, 2, 17, h2).vl(3, 8, 12, mix(h1, WHITE, 0.2)).rect(5, 5, 2, 9, h1).rect(17, 5, 2, 9, h2)
        c.hl(7, 5, 5, s1).px(12, 5, h1).px(13, 5, h1)  # a side parting
        c.px(3, 20, h2).px(20, 20, h2)
    elif style == 'messy':
        for x, y in ((6, 6), (7, 6), (8, 7), (10, 6), (11, 6), (12, 7), (14, 6), (4, 5), (19, 5), (9, 0), (13, 0), (16, 1)):
            c.px(x, y, h1)
        c.px(9, 6, h2).px(13, 6, h2)
    elif style == 'receding':
        c.hl(5, 5, 14, s1).hl(7, 4, 10, s1).hl(8, 4, 3, h2).hl(13, 4, 3, h2).px(5, 4, h1).px(18, 4, h2)
        c.vl(5, 5, 3, h2).vl(18, 5, 3, h2)
    elif style == 'medium':
        c.rect(4, 4, 2, 9, h1).rect(18, 4, 2, 9, h2).px(4, 13, h2).px(19, 13, h2).hl(6, 6, 3, h1).hl(14, 6, 3, h1)
    # brows, eyes, nose, mouth
    brow = rgba(o.get('brow', o['hair'][1]))
    c.hl(7, 8, 3, brow).hl(14, 8, 3, brow)
    if o.get('stern'):
        c.px(9, 9, brow).px(14, 9, brow).px(7, 8, s1).px(16, 8, s1).px(7, 7, brow).px(16, 7, brow)
    eye = rgba(o['eye'])
    c.px(7, 10, WHITE).px(8, 10, eye).px(9, 10, mix(eye, 0xFF000000, 0.45)).px(14, 10, mix(eye, 0xFF000000, 0.45)).px(15, 10, eye).px(16, 10, WHITE)
    c.hl(7, 9, 3, mix(s2, 0xFF000000, 0.35)).hl(14, 9, 3, mix(s2, 0xFF000000, 0.35)) if not o.get('stern') else None
    c.px(7, 11, s2).px(16, 11, s2)
    c.vl(12, 10, 3, s2).px(11, 13, s3).px(12, 13, s3).px(11, 12, s1)
    mouth = rgba(o.get('mouth', '#A8604E'))
    c.hl(10, 15, 4, mouth).px(13, 15, mix(mouth, 0xFF000000, 0.3))
    if o.get('smirk'):
        c.px(14, 14, mix(mouth, s1, 0.5)).px(10, 15, s1)
    if o.get('lips'):
        c.hl(10, 15, 4, rgba(o['lips'])).hl(11, 16, 2, mix(rgba(o['lips']), 0xFF000000, 0.25)).px(11, 15, mix(rgba(o['lips']), WHITE, 0.35))
    else:
        c.hl(11, 16, 2, s2)
    beard = o.get('beard')
    if beard:
        b = rgba(o.get('beard_c', o['hair'][1]))
        if beard == 'stubble':
            for x, y in ((6, 14), (6, 16), (7, 15), (7, 17), (8, 16), (9, 17), (10, 16), (11, 17), (13, 17), (13, 16), (14, 17), (15, 16), (16, 17), (16, 15), (17, 16), (17, 14), (9, 14), (14, 14)):
                c.px(x, y, mix(c.get(x, y), b, 0.42))
        else:
            c.rect(6, 14, 12, 4, b).rect(5, 12, 2, 4, b).rect(17, 12, 2, 4, mix(b, 0xFF000000, 0.25)).hl(9, 13, 6, b).hl(7, 18, 10, mix(b, 0xFF000000, 0.25))
            c.hl(10, 15, 4, mouth).px(6, 17, rgba(o['bg'])).px(17, 17, rgba(o['bg']))
            c.rect(9, 17, 6, 2, mix(b, 0xFF000000, 0.25)).hl(8, 14, 8, mix(b, WHITE, 0.12))
    if o.get('glasses'):
        g = rgba('#101018')
        c.rect(6, 9, 5, 3, g).rect(13, 9, 5, 3, g).hl(11, 9, 2, g).px(5, 9, g).px(18, 9, g)
        c.px(7, 9, rgba('#5A6272')).px(14, 9, rgba('#5A6272')).hl(6, 8, 5, s1).hl(13, 8, 5, s1).hl(7, 8, 3, brow).hl(14, 8, 3, brow)
    if o.get('scar'):
        c.line(15, 6, 17, 12, rgba('#B87868'))
    return c


def beast(r, eye, bg, mouth=False, scar=None, muzzle=None, glow=None, nose=INK):
    I = Ic(24)
    I.cv.rect(0, 0, 24, 24, rgba(bg)).rect(0, 0, 24, 1, mix(rgba(bg), WHITE, 0.08)).rect(0, 22, 24, 2, mix(rgba(bg), 0xFF000000, 0.25))
    if glow:
        for x, y in ((5, 9), (10, 9), (13, 9), (18, 9), (6, 13), (17, 13)):
            I.cv.px(x, y, rgba(glow))
    wolf(I, 16, 16.4, 4 / 3, r, eye, mouth=mouth, muzzle=muzzle, nose=nose, line=nose == INK)
    c = I.cv
    if scar:
        c.line(15, 6, 18, 12, rgba(scar)).px(18, 13, rgba(scar)).px(16, 6, rgba(scar))
    return c


def morph(a, b):
    """Mid-shift: the man on the left, the beast on the right, a jagged bright seam between them."""
    c, jag = Cv(24, 24), (0, 1, -1, 1, 0, -1)
    for y in range(24):
        s = 12 + jag[y % 6]
        for x in range(24):
            c.d[y * 24 + x] = (a if x < s else b).d[y * 24 + x]
        c.px(s, y, rgba('#FFE8A0'))
        if y % 3 == 0:
            c.px(s - 1, y, rgba('#FFF6D8'))
    return c


TAN = ramp('#26221C', '#A29682', '#766B58', '#4F483B', '#867A67', '#353027')
PETER = ramp('#050608', '#55505C', '#211F26', '#0C0B0E', '#34313A', '#141317')
GEVAUDAN = ramp('#070809', '#56646E', '#262E36', '#121418', '#3C474F', '#191B20')


def all_portraits():
    klaus = bust(dict(bg='#3A2A2C', skin='fair', hair=('#AC8F61', '#86683E'), style='curls', eye='#386870', beard='stubble', beard_c='#9A7850',
                      coat=('#2A2420', '#1A1614'), shirt='#96282C', collar='v', trim='#AAAAB0', smirk=True))
    klaus_wolf = beast(TAN, '#D6A014', '#3A2A2C')
    return [
        ('mikael', bust(dict(bg='#2A2A32', skin='pale', hair=('#A89978', '#84765A'), style='receding', eye='#96BAD4', brow='#6A5E48', stern=True,
                             coat=('#202026', '#141418'), shirt='#DADADF', tie='#17171B', mouth='#9A6A60'))),
        ('klaus_human', klaus),
        ('klaus_shifting', morph(klaus, klaus_wolf)),
        ('klaus_wolf', klaus_wolf),
        ('elijah', bust(dict(bg='#22262E', skin='warm', hair=('#583F2C', '#3A2818'), style='slick', eye='#7A5E34', coat=('#323A4E', '#222838'),
                             shirt='#E8E8EC', tie='#1A1C22'))),
        ('rebekah', bust(dict(bg='#3A2A34', skin='rose', hair=('#F4E6A8', '#D4BA72'), style='long', eye='#8CB8DE', brow='#B89E5E', lips='#C66A70',
                              coat=('#FCEBB8', '#DCC88E'), shirt='#E9E5DE', collar='flat', trim='#CCAC54'))),
        ('kol', bust(dict(bg='#2A2430', skin='warm', hair=('#563D2C', '#38261A'), style='messy', eye='#5A4030', smirk=True, coat=('#222A3E', '#161C2C'),
                          shirt='#C8C8CE'))),
        ('finn', bust(dict(bg='#2E2E30', skin='warm', hair=('#503B2A', '#34261A'), style='medium', eye='#6E8054', coat=('#56585E', '#3E4046'),
                           shirt='#464650', collar='flat', stern=True))),
        ('deucalion_human', bust(dict(bg='#22262E', skin='tan', hair=('#543E2E', '#382A1E'), eye='#1A1A1E', beard='stubble', beard_c='#5A4636', glasses=True,
                                      coat=('#707277', '#54565C'), shirt='#2A2C33', collar='flat'))),
        ('deucalion_demon_wolf', beast('demon', '#FF3A2A', '#1A1420', mouth=True, glow='#3A1420')),
        ('peter_human', bust(dict(bg='#2C2620', skin='warm', hair=('#221A17', '#120E0C'), eye='#608C98', beard='full', beard_c='#3E3028', coat=('#CEA674', '#A88252'),
                                  shirt='#96969E', smirk=True))),
        ('peter_wolf', beast(PETER, '#FF3A2A', '#2C2620', mouth=True, scar='#B0625C')),
        ('beast_host', bust(dict(bg='#2A1A18', skin='brown', hair=('#241A17', '#140E0C'), eye='#2E2018', coat=('#96684C', '#74503A'), shirt='#BDB190',
                                 mouth='#7A4438', stern=True))),
        ('beast_beast', beast(GEVAUDAN, '#FF9A2A', '#2A1A18', mouth=True, scar='#C8B8A0', glow='#4A1A1F')),
        ('beast_spirit', beast('ice', '#17707A', '#0E2630', muzzle='#FFFFFF', glow='#2A5A6A', nose='#58A0B2')),
    ]


def build():
    cells = []
    for name, cv in all_portraits():
        assert cv.w == 24 and cv.h == 24 and all(v for v in cv.d), name
        emit(cv, 'boss/%s.png' % name, 'boss portrait, 24 x 24')
        cells.append((name, cv))
    # each portrait under each theme's plate, the way the boss bar shows it
    for k in (kit.Grimoire(), kit.Moon(), kit.Tensura()):
        for name, cv in cells[:15:2]:
            framed = Cv(42, 42).blit(cv, 9, 9).blit(k.boss_plate('shifting' in name), 0, 0)
            cells.append(('%s, %s' % (name, k.id), framed))
    sheet(os.path.join(PHASE2, 'portraits.png'), cells, rgba('#17171C'), zoom=5, max_w=1500)
    print('portraits', 15)
