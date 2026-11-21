"""The three theme kits (Grimoire, Blood Moon, Tensura) and the shared HUD pieces.

Recipes are ported from phase1/src/kit-*.js, the approved style guide; each sprite is drawn on its own small canvas.
A sprite that scales is a nine-slice: its corners stay fixed, its edges and centre tile.
"""
import math

from px import (BLACK, WHITE, Cv, Pal, diamond, disc_mask, emit, hex_mask, load_palettes, mix, nine, notch, pie, pill, ramp, rgba, rng, rr,
                sheet, solid, with_a)

PALS = load_palettes()
GEMS = {'red': rgba('#C0283A'), 'blue': rgba('#3F86D8'), 'gold': rgba('#E8B33A'), 'green': rgba('#3FA35A'), 'violet': rgba('#8A5AD6')}
CHECK = ['......#', '.....##', '#...##.', '##.##..', '.###...', '..#....']
CHECK_S = ['....#', '#..#.', '.##..']
STATES = ('normal', 'hover', 'pressed', 'disabled', 'focus')
GLOW = (153, 82, 41, 20)  # the stepped glow: 60 / 32 / 16 / 8 %


def focus_ring(cv, x, y, w, h, P):
    cv.box(x - 2, y - 2, w + 4, h + 4, P.focusD).box(x - 1, y - 1, w + 2, h + 2, P.focusL)


def glow_boxes(cv, x, y, w, h, c):
    cv.box(x - 1, y - 1, w + 2, h + 2, with_a(c, GLOW[0])).box(x - 2, y - 2, w + 4, h + 4, with_a(c, GLOW[2]))


class Kit:
    """What every theme draws. A method returns the sprite, or (sprite, slice)."""
    id = ''
    slot_tint = {}

    def gem(self, color):
        raise NotImplementedError

    # ---- pieces that only differ by material ----
    def focus(self):
        cv = Cv(12, 12)
        focus_ring(cv, 2, 2, 8, 8, self.P)
        return cv, (3, 3, 3, 3)

    def button_focus(self, pri):
        cv, sl = self.button('normal', pri)
        focus_ring(cv, 2, 2, 24, 16, self.P)
        return cv, sl

    def mastery_track(self):
        a, b = self.mastery
        return Cv(18, 4).rect(0, 0, 18, 4, a).rect(1, 1, 16, 2, b), (2, 0, 2, 0)

    def mastery_fill(self, kind):
        hi, base = {'progress': self.mastery_fill_c, 'learning': (rgba('#B4B0CC'), rgba('#8E8AA8')), 'mastered': (rgba('#FFF0B0'), rgba('#FFD36B'))}[kind]
        return Cv(2, 2).rect(0, 0, 2, 1, hi).rect(0, 1, 2, 1, base)

    def boss_bar_tick(self):
        return Cv(1, 7).rect(0, 0, 1, 7, with_a(BLACK, 90))

    def boss_fill(self, r, shifting):
        cv = Cv(8, 7)  # a tile: highlight row, second band, three rows of base, second shade, shade
        for y, c in enumerate((r.hi, r.hi2 or r.b, r.b, r.b, r.b, r.sh2 or r.b, r.sh)):
            cv.hl(0, y, 8, c)
        if shifting:
            cv.rect(2, 3, 3, 1, mix(r.hi, WHITE, 0.5))
        return cv


# =====================================================================================================================
class Grimoire(Kit):
    id, name = 'grimoire', 'Grimoire'

    def __init__(self):
        P = self.P = Pal(PALS['grimoire'])
        self.R = Pal(
            page=ramp(P.bord, P.paH, P.pa, P.paS), inset=ramp(P.bord, P.paD, '#DCCD9E', P.paH),
            btn=ramp(P.bord, P.paH, '#D8C896', '#B49E6A'), btnHot=ramp(P.br, P.brHH, '#EADFAE', '#C9B070'), btnDn=ramp(P.bord, '#A58F5E', '#C6B27C', P.paH),
            btnOff=ramp('#A89A78', '#E6DEC8', '#D5CBB0', '#C3B896'),
            pri=ramp('#2E0A0C', '#C2373B', '#8E1C22', '#5E0F14', '#A8282C', '#741519'), priHot=ramp(P.br, '#E4565A', '#AE2A30', '#6E1519', '#C8383D', '#8A1D22'),
            priDn=ramp('#2E0A0C', '#5E0F14', '#741519', '#A8282C'), priOff=ramp('#A89A78', '#E6DEC8', '#CFC4A6', '#BDB08C'),
            brass=ramp(P.brD, P.brHH, P.br, P.brS, P.brH, P.brS), leather=ramp(P.leD, P.leH, P.le, '#33100F', P.leM),
            wax=ramp(P.waxD, P.waxH, P.wax, P.waxM, '#B53539'), well=ramp(P.bord, P.paD, '#2A1A18', '#3A2622'))
        self.mastery = (rgba('#1A1008'), rgba('#4A3A28'))
        self.mastery_fill_c = (rgba('#F08A7E'), rgba('#E0554A'))
        self.sheet_bg, self.sheet_bg2 = P.le, P.pa

    def panel(self):
        P = self.P
        cv = Cv(32, 32).bevel(rr(30, 30, 2), self.R.page, 1, 1)
        for i in range(5, 27, 2):  # the pricked border of a page
            cv.px(i, 4, P.paS).px(i, 27, P.paS).px(4, i, P.paS).px(27, i, P.paS)
        return cv, (6, 6, 6, 6)

    def panel_texture(self):
        P, cv, r = self.P, Cv(64, 64), rng(11)
        for _ in range(26):  # paper fibres and a few foxing specks
            x, y = int(r() * 62), int(r() * 64)
            cv.px(x, y, with_a(P.paD, 110))
            if r() < 0.35:
                cv.px(x + 1, y, with_a(P.paD, 110))
        for _ in range(5):
            cv.px(int(r() * 64), int(r() * 64), with_a(P.paDD, 90))
        return cv

    def inset(self):
        return Cv(16, 16).bevel(rr(14, 14, 1), self.R.inset, 1, 1), (3, 3, 3, 3)

    def tooltip(self):
        P = self.P
        cv = Cv(24, 24).bevel(rr(22, 22, 2), self.R.leather, 1, 1, True)
        cv.box(3, 3, 18, 18, P.brS)
        for x, y in ((3, 3), (20, 3), (3, 20), (20, 20)):
            cv.px(x, y, P.brH)
        return cv, (5, 5, 5, 5)

    def divider(self):
        P = self.P
        cv = Cv(16, 7)
        cv.hl(1, 3, 14, P.bord).hl(2, 4, 12, P.paH).px(0, 3, P.paD).px(15, 3, P.paD)
        cv.px(1, 2, P.bord).px(14, 2, P.bord)
        return cv, (4, 0, 4, 0)

    def divider_gem(self):
        P = self.P
        cv = Cv(15, 9)
        cv.hl(0, 4, 15, P.bord).hl(0, 5, 15, P.paH)
        cv.rect(2, 2, 11, 5, P.pa).bevel(diamond(7), self.R.brass, 4, 1)
        cv.px(2, 4, P.bord).px(12, 4, P.bord).px(0, 4, P.paD).px(14, 4, P.paD)
        return cv

    def scroll_track(self):
        return Cv(7, 16).bevel(rr(5, 14, 1), ramp(self.P.bord, self.P.paD, '#CBB985', self.P.paS), 1, 1), (0, 3, 0, 3)

    def scroll_thumb(self, hover):
        P = self.P
        cv = Cv(7, 14)
        cv.bevel(rr(5, 12, 1), ramp(P.brD if hover else P.leD, P.leH, P.leM if hover else P.le, '#33100F'), 1, 1)
        cv.hl(2, 2, 3, P.brH if hover else P.br).hl(2, 11, 3, P.brS).px(3, 6, P.brH).px(3, 7, P.brS)
        return cv, (0, 4, 0, 4)

    def tab(self, st):
        P, sel = self.P, st == 'selected'
        w = 30 if sel else 27
        m = solid(w, 14)
        for y in range(14):  # the outer end of an index tab is chamfered
            for x in range(w - 3, w):
                if (w - 1 - x) + min(y, 13 - y) < 2:
                    m.d[y * w + x] = 0
        r = {'selected': ramp(P.brD, P.redH, '#A8282C', '#6E1519', '#BF3A3E'), 'hover': ramp(P.brD, P.leH, P.leM, P.le),
             'disabled': ramp('#4A4343', '#6A6060', '#554C4C', '#3E3636')}.get(st, self.R.leather)
        x0 = 1 if sel else 4
        cv = Cv(32, 16).bevel(m, r, x0, 1)
        if sel:
            cv.hl(x0 + 1, 2, w - 3, P.brH).hl(x0 + 1, 13, w - 3, P.br)
        cv.px(x0 + 1, 2, P.brD)
        return cv, (6, 4, 5, 4)

    def button(self, st, pri):
        P, R = self.P, self.R
        r = {'normal': R.pri if pri else R.btn, 'hover': R.priHot if pri else R.btnHot, 'pressed': R.priDn if pri else R.btnDn, 'disabled': R.priOff if pri else R.btnOff}[st]
        cv = Cv(28, 20).bevel(rr(24, 16, 2), r, 2, 2, pri and st != 'disabled')
        if pri and st != 'disabled':
            cv.px(4, 10, P.brH).px(23, 10, P.brH)
        if st == 'pressed':
            cv.hl(4, 3, 20, mix(r.hi, BLACK, 0.25))
        return cv, (6, 6, 6, 6)

    def bar_track(self):
        return Cv(16, 8).bevel(rr(14, 6, 1), ramp(self.P.bord, self.P.paDD, '#CBB985', self.P.paH), 1, 1), (3, 2, 3, 2)

    def bar_fill(self):
        return Cv(8, 4).bevel(solid(8, 4), ramp(0, '#C2373B', '#8E1C22', '#5E0F14'), 0, 0).hl(0, 0, 8, rgba('#C2373B')).vl(0, 1, 2, rgba('#8E1C22')).vl(7, 1, 2, rgba('#8E1C22')), (1, 0, 1, 0)

    def plate(self):
        return Cv(24, 14).bevel(pill(22, 12), self.R.page, 1, 1), (7, 0, 7, 0)

    def strip(self, h):
        P = self.P
        cv = Cv(24, h).bevel(rr(22, h - 2, 2), self.R.leather, 1, 1, True)
        cv.box(3, 3, 18, h - 6, mix(P.le, P.br, 0.78)).px(4, h // 2, P.brHH).px(19, h // 2, P.brHH)
        return cv, (7, 4, 7, 4)

    def chip(self, st):
        P = self.P
        r = {'on': self.R.wax, 'shifting': ramp(P.brD, P.brHH, P.br, P.brS)}.get(st, ramp(P.bord, P.paH, '#E2D6B0', P.paS))
        return Cv(16, 12).bevel(rr(14, 10, 2), r, 1, 1), (4, 4, 4, 4)

    def wax_seal(self, d, st):
        P = self.P
        m = Cv(d, d).disc(d / 2, d / 2, d / 2, d / 2, 1)
        m.px(1, d - 5, 1).px(0, d - 6, 1).px(d - 2, 4, 1).px(d - 1, 5, 1)  # wax that ran past the die
        r = {'hover': ramp(P.waxD, '#E86B6C', '#B52A30', P.waxM, '#D04548'), 'pressed': ramp(P.waxD, P.waxM, '#7C151A', '#5A0F13')}.get(st, self.R.wax)
        cv = Cv(d + 2, d + 2).bevel(m, r, 1, 1, True)
        if d >= 14:
            cv.ring(1 + d / 2, 1 + d / 2, d / 2 - 3, d / 2 - 3, 1, P.waxD if st == 'pressed' else P.waxM)
        return cv

    def seal(self, st):
        return self.wax_seal(18, st)

    def gem(self, color):
        c = GEMS[color]
        cv = Cv(11, 11).bevel(diamond(9), ramp(self.P.brD, mix(c, WHITE, 0.45), c, mix(c, BLACK, 0.35)), 1, 1)
        return cv.px(4, 4, WHITE)

    def corner(self):
        P = self.P
        m = Cv(12, 12).rect(0, 0, 12, 4, 1).rect(0, 0, 4, 12, 1)
        m.clr(11, 0).clr(0, 0).clr(11, 3).clr(0, 11).clr(3, 11)
        cv = Cv(12, 12).bevel(m, self.R.brass, 0, 0, True)
        cv.px(1, 1, P.brHH).px(2, 1, P.br).px(1, 2, P.br).px(2, 2, P.brD)  # the rivet
        cv.px(4, 4, P.brS).px(5, 4, P.brD)
        return cv

    def flourish(self):
        rows = ['KKKKKKKKKK.', 'K.........K', 'K.KKKKKK..K', 'K.K....K..K', 'K.K.KK.K..K', 'K.K.K..K.K.', 'K.K.KKK.KK.', 'K.K........', 'K..KKKKK...', 'K..........', 'KKKKKKKK...']
        return Cv(11, 11).bmp(rows, {'K': self.P.mut})

    def flourish_title(self):
        P = self.P
        cv = Cv(48, 7)
        cv.hl(6, 3, 36, P.bord).dash(0, 3, 6, P.bord, 1, 1).dash(43, 3, 5, P.bord, 1, 1)
        cv.rect(19, 1, 10, 5, 0)
        for x in range(19, 29):
            cv.clr(x, 3)
        cv.bevel(diamond(5), self.R.brass, 21, 1)
        for x in (12, 35):
            cv.px(x, 2, P.bord).px(x, 4, P.bord).px(x - 1, 3, P.br).px(x, 3, P.brH).px(x + 1, 3, P.br)
        return cv

    def stamp(self, lit):
        P = self.P
        cv = Cv(16, 16)
        c = P.red if lit else P.paD
        cv.ring(8, 8, 7.5, 7.5, 1.2, c).ring(8, 8, 5.4, 5.4, 0.9, P.redH if lit else P.paS)
        cv.bmp(CHECK, {'#': c}, 5, 5)
        if lit:  # an ink stamp never prints whole
            for x, y in ((3, 2), (12, 13), (13, 11), (2, 5)):
                cv.clr(x, y)
        return cv

    def rung(self, status):
        P = self.P
        r = {'reached': self.R.wax, 'current': ramp(P.br, P.brHH, P.paH, P.paS), 'locked': ramp(P.paDD, P.paS, '#CDBF94', P.paD)}[status]
        cv = Cv(12, 12).bevel(disc_mask(10), r, 1, 1)
        if status == 'reached':
            cv.bmp(CHECK_S, {'#': P.paH}, 4, 5)
        elif status == 'current':
            cv.rect(5, 5, 2, 2, P.red).px(5, 5, P.redH)
        else:
            cv.rect(5, 4, 2, 2, P.paDD).px(5, 6, P.paDD).px(6, 6, P.paDD).rect(5, 7, 2, 1, P.paDD)
        return cv

    def task(self, state):
        P = self.P
        r = {'claimable': ramp(P.br, P.brHH, P.paH, P.paS), 'open': self.R.inset, 'claimed': ramp(P.good, P.paH, P.pa, P.paS),
             'frozen': ramp('#5A6A7A', '#E4ECEE', '#C8D4D8', '#A8B8C0'), 'unavailable': ramp('#A89A78', '#D5CBB0', '#CFC4A6', '#BDB08C')}[state]
        cv = Cv(10, 10).bevel(rr(8, 8, 1), r, 1, 1)
        if state == 'claimable':
            cv.rect(4, 4, 2, 2, P.red).px(4, 4, P.redH)
        elif state == 'claimed':
            cv.bmp(CHECK_S, {'#': P.good}, 3, 4)
        elif state == 'frozen':
            cv.line(3, 6, 6, 3, rgba('#5A6A7A')).px(3, 3, rgba('#5A6A7A')).px(6, 6, rgba('#5A6A7A'))
        elif state == 'unavailable':
            cv.hl(3, 5, 4, rgba('#8A7C5C'))
        return cv

    def pip(self, on):
        P = self.P
        return Cv(5, 5).bevel(diamond(3), ramp(P.waxD, P.waxH, P.wax, P.waxM) if on else ramp(P.paDD, P.paS, P.paD, P.paD), 1, 1)

    def node(self, st):
        P = self.P
        cv = Cv(34, 34)
        ring = {'selected': P.br, 'on': P.red, 'off': P.paDD}[st]
        if st == 'selected':
            cv.disc(17, 17, 17, 17, with_a(P.ready, GLOW[1])).disc(17, 17, 15, 15, with_a(P.ready, GLOW[0]))
        cv.bevel(disc_mask(26), ramp(ring, P.paH, P.pa, P.paS), 4, 4)
        if st == 'selected':
            cv.ring(17, 17, 12, 12, 1, ring)
        cv.bevel(disc_mask(20), ramp(0, P.paD, '#2A1A18', '#3A2622'), 7, 7)
        return cv

    def cover(self):
        P = self.P
        w = 50
        cv = Cv(w, w).rect(0, 0, w, w, P.le).box(0, 0, w, w, P.leD)
        cv.hl(1, 1, w - 2, P.leH).vl(1, 1, w - 2, P.leH).hl(1, w - 2, w - 2, rgba('#33100F')).vl(w - 2, 1, w - 2, rgba('#33100F'))
        cv.box(3, 3, w - 6, w - 6, P.br).hl(4, 4, w - 8, P.brD).vl(4, 4, w - 8, P.brD)
        for i in range(7, w - 7, 3):  # the stitching
            for a, b in ((i, 6), (i, w - 7), (6, i), (w - 7, i)):
                cv.px(a, b, P.brS)
            for a, b in ((i + 1, 6), (i + 1, w - 7), (6, i + 1), (w - 7, i + 1)):
                cv.px(a, b, P.brS)
        cap = self.corner()
        for fx, fy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            cv.blit(cap, w - 12 if fx else 0, w - 12 if fy else 0, bool(fx), bool(fy))
        return cv, (13, 13, 13, 13)

    def cover_art(self):  # the boss of the cover: a brass lozenge with a wax-red stone
        P = self.P
        cv = Cv(32, 32).bevel(diamond(29), self.R.brass, 1, 1, True)
        cv.bevel(diamond(15), ramp(P.brD, P.waxH, P.wax, P.waxM, '#B53539', '#7C151A'), 8, 8, True)
        cv.px(13, 13, WHITE).px(14, 12, P.waxH)
        for x, y in ((15, 4), (4, 15), (26, 15), (15, 26)):
            cv.px(x, y, P.brD).px(x + 1, y, P.brHH)
        return cv

    def slot(self, active):
        P = self.P
        cv = Cv(28, 28)
        if active:
            glow_boxes(cv, 3, 3, 22, 22, P.ready)
        cv.bevel(rr(22, 22, 2), ramp(P.br, P.brHH, P.pa, P.paS) if active else self.R.page, 3, 3)
        return cv.bevel(rr(16, 16, 1), self.R.well, 6, 6)

    def slot_square(self, active):  # Tensura mode: brass corners on the same tile
        P = self.P
        cv = self.slot(active)
        for x, y in ((3, 3), (24, 3), (3, 24), (24, 24)):
            cv.px(x + (1 if x == 3 else -1), y, P.br).px(x, y + (1 if y == 3 else -1), P.br)
        cv.box(6, 6, 16, 16, P.brS if not active else P.br)
        return cv

    def key_badge(self, wide):
        if not wide:
            return self.wax_seal(9, 'normal')
        return Cv(17, 11).bevel(pill(15, 9), self.R.wax, 1, 1)

    def hud_label(self):
        return Cv(16, 16).bevel(rr(14, 14, 1), self.R.page, 1, 1), (3, 3, 3, 3)

    def hud_pill(self):
        return self.strip(14)

    def face_plate(self):
        return Cv(20, 20).bevel(rr(18, 18, 1), ramp(self.P.bord, self.P.brHH, self.P.br, self.P.brS), 1, 1)

    def boss_frame(self):
        P, (cv, _) = self.P, self.panel()
        for x, y in ((2, 2), (27, 2), (2, 27), (27, 27)):  # brass studs
            cv.rect(x, y, 3, 3, P.br).px(x, y, P.brHH).px(x + 2, y + 2, P.brD).px(x + 1, y + 1, P.brH)
        return cv, (6, 6, 6, 6)

    def boss_plate(self, shifting):
        P = self.P
        cv = Cv(42, 42)
        if shifting:
            glow_boxes(cv, 3, 3, 36, 36, P.ready)
        cv.bevel(rr(34, 34, 2), self.R.brass, 4, 4, True).rect(7, 7, 28, 28, P.leD)
        for a, b in ((5, 5), (36, 5), (5, 36), (36, 36)):
            cv.px(a, b, P.brD)
        if shifting:
            cv.box(7, 7, 28, 28, P.ready)
        for y in range(9, 33):
            for x in range(9, 33):
                cv.clr(x, y)
        return cv

    def boss_name_plate(self):
        return self.strip(13)

    def boss_bar_track(self):
        P = self.P
        cv = Cv(18, 11).bevel(rr(16, 9, 1), ramp(self.P.bord, self.P.paDD, '#CBB985', self.P.paH), 1, 1)
        return cv, (3, 2, 3, 2)

    def boss_bar_fill(self, shifting):
        P = self.P
        return self.boss_fill(ramp(0, P.brHH, P.br, P.brS, P.brH, P.brS) if shifting else ramp(0, '#C2373B', '#8E1C22', '#5E0F14', '#A8282C', '#741519'), shifting)


# =====================================================================================================================
class Moon(Kit):
    id, name = 'bloodmoon', 'Blood Moon'

    def __init__(self):
        P = self.P = Pal(PALS['bloodmoon'])
        self.R = Pal(
            card=ramp(P.edge, P.cardH, P.card, P.cardD), inset=ramp(P.void, P.void, P.cardD, '#241119'),
            pri=ramp(P.redDD, P.redH, P.red, P.redD, '#D14438', '#A32A24'), priHot=ramp(P.hot, '#FF9588', '#D9453B', P.redD, '#EE6A5E', '#B33028'),
            priDn=ramp(P.redDD, P.redD, '#8E2420', '#BF332B'), priOff=ramp('#3A2229', '#2A1920', '#22121A', '#1C0E14'),
            sec=ramp(P.edgeH, P.cardH, '#22111A', P.cardD), secHot=ramp(P.hot, '#3A1A24', '#2E1620', P.cardD), secDn=ramp(P.edge, P.void, P.cardD, '#241119'),
            secOff=ramp('#2E1A20', '#1C0E14', '#170A10', '#12070C'), chip=ramp(P.edge, P.cardH, P.card, P.cardD), chipOn=ramp(P.redDD, P.redH, P.red, P.redD),
            gold=ramp('#5A3A10', '#FFF0B0', P.gold, P.goldD))
        self.bands = [P.moon, P.moon2, P.moon3, P.moon4, P.moon5, P.moon6]
        self.mastery = (P.void, P.cardH)
        self.mastery_fill_c = (P.hot, rgba('#E2493F'))
        self.sheet_bg, self.sheet_bg2 = rgba('#12060A'), P.sky

    def panel(self):
        P = self.P
        cv = Cv(32, 32).bevel(rr(30, 30, 2), self.R.card, 1, 1)
        cv.hl(6, 0, 9, P.red).hl(6, 1, 9, mix(P.cardH, P.red, 0.3)).px(5, 0, P.redD).px(15, 0, P.redD)  # the blood line on the top edge
        return cv, (17, 6, 6, 6)

    def panel_texture(self):  # the stars of the sky behind the cards
        P, cv, r = self.P, Cv(64, 64), rng(77)
        for _ in range(11):
            cv.px(int(r() * 64), int(r() * 64), with_a(P.moon3, 200) if r() < 0.3 else with_a(P.moon, 90))
        return cv

    def inset(self):
        return Cv(16, 16).bevel(rr(14, 14, 1), self.R.inset, 1, 1), (3, 3, 3, 3)

    def tooltip(self):
        P = self.P
        cv = Cv(24, 24).bevel(rr(22, 22, 2), ramp(P.edgeH, P.cardD, P.void, P.void), 1, 1)
        cv.hl(3, 1, 18, P.red).hl(4, 2, 16, P.redDD)
        return cv, (5, 5, 5, 5)

    def divider(self):
        P = self.P
        return Cv(16, 7).hl(1, 3, 14, P.edge).px(0, 3, P.redD).px(15, 3, P.redD), (4, 0, 4, 0)

    def crescent(self, d, cut=(1.5, -0.5, -0.5)):
        c = disc_mask(d)
        return c.cut(Cv(d, d).disc(d / 2 + cut[0], d / 2 + cut[1], d / 2 + cut[2], d / 2 + cut[2], 1))

    def divider_gem(self):
        P = self.P
        cv = Cv(15, 9)
        cv.hl(0, 4, 15, P.edge)
        for x in range(1, 14):
            cv.clr(x, 4)
        cv.bevel(self.crescent(7), ramp(0, P.moon, P.moon3, P.moon5), 4, 1)
        cv.px(1, 4, P.red).px(13, 4, P.red)
        return cv

    def scroll_track(self):
        return Cv(7, 16).bevel(rr(3, 14, 1), ramp(self.P.edge, self.P.void, self.P.cardD, self.P.cardH), 2, 1), (0, 3, 0, 3)

    def scroll_thumb(self, hover):
        cv = Cv(7, 14).bevel(rr(5, 12, 2), self.R.priHot if hover else self.R.pri, 1, 1)
        return cv.px(3, 3, self.P.moon), (0, 5, 0, 5)

    def tab(self, st):
        P = self.P
        r = {'selected': self.R.chipOn, 'hover': ramp(P.red, P.cardH, '#2E1620', P.cardD), 'disabled': ramp('#2E1A20', '#1C0E14', '#170A10', '#12070C')}.get(st, self.R.chip)
        return Cv(32, 16).bevel(pill(30, 14), r, 1, 1, st == 'selected'), (9, 0, 9, 0)

    def button(self, st, pri):
        P, R = self.P, self.R
        r = {'normal': R.pri if pri else R.sec, 'hover': R.priHot if pri else R.secHot, 'pressed': R.priDn if pri else R.secDn, 'disabled': R.priOff if pri else R.secOff}[st]
        cv = Cv(28, 20)
        if st == 'hover':
            cv.stamp(1, 1, pill(26, 18), with_a(P.glow, GLOW[1]))
        cv.bevel(pill(24, 16), r, 2, 2, pri and st != 'disabled')
        if not pri and st != 'disabled':
            cv.hl(10, 2, 8, P.hot if st == 'hover' else P.red)
        return cv, (10, 0, 10, 0)

    def bar_track(self):
        return Cv(16, 8).bevel(pill(14, 6), ramp(self.P.void, self.P.cardD, self.P.cardD, self.P.cardH), 1, 1), (4, 0, 4, 0)

    def bar_fill(self):
        return Cv(8, 4).bevel(pill(8, 4), ramp(0, self.P.redH, self.P.red, self.P.redD), 0, 0), (2, 0, 2, 0)

    def plate(self):
        return Cv(24, 14).bevel(pill(22, 12), ramp(self.P.edgeH, self.P.cardH, self.P.card, self.P.cardD), 1, 1), (7, 0, 7, 0)

    def strip(self, h):
        P = self.P
        cv = Cv(24, h).bevel(pill(22, h - 2), ramp(P.edgeH, P.cardH, P.card, P.cardD), 1, 1)
        cv.hl(1 + (h - 2) // 2, 1, 22 - (h - 2), P.red)
        return cv, (8, 0, 8, 0)

    def chip(self, st):
        return Cv(16, 12).bevel(pill(14, 10), {'on': self.R.pri, 'shifting': self.R.gold}.get(st, self.R.chip), 1, 1), (6, 0, 6, 0)

    def seal(self, st):
        P, on = self.P, st == 'hover'
        cv = Cv(20, 20).bevel(disc_mask(18), ramp(P.hot if on else P.edge, P.cardH, P.cardD if st == 'pressed' else P.card, P.cardD), 1, 1)
        return cv.ring(10, 10, 6, 6, 1, P.red if on else P.redDD if st == 'pressed' else P.edge)

    def gem(self, color):
        c = GEMS[color]
        cv = Cv(11, 11).bevel(disc_mask(9), ramp(self.P.void, mix(c, WHITE, 0.5), c, mix(c, BLACK, 0.4)), 1, 1)
        return cv.px(3, 3, WHITE).px(4, 3, mix(c, WHITE, 0.5))

    def corner(self):  # a red hairline that turns the corner round a small crescent
        P = self.P
        cv = Cv(12, 12)
        cv.hl(3, 0, 9, P.red).vl(0, 3, 9, P.red).px(1, 2, P.red).px(2, 1, P.red).px(1, 1, P.redDD)
        cv.hl(4, 1, 6, P.redDD).vl(1, 4, 6, P.redDD)
        cv.bevel(self.crescent(5, (1.2, -0.4, -0.3)), ramp(0, P.moon, P.moon3, P.moon4), 4, 4)
        return cv

    def flourish(self):  # a four-point star with two sparks
        rows = ['....K......', '....K......', '...KMK.....', 'KKKMMMKKK..', '...KMK.....', '....K...K..', '....K..KMK.', '........K..', '.K.........', 'KMK........', '.K.........']
        return Cv(11, 11).bmp(rows, {'K': self.P.red, 'M': self.P.moon2})

    def flourish_title(self):
        P = self.P
        cv = Cv(48, 7)
        cv.hl(8, 3, 32, P.edgeH).dash(0, 3, 8, P.edge, 1, 1).dash(41, 3, 7, P.edge, 1, 1)
        for x in range(18, 30):
            cv.clr(x, 3)
        cv.bevel(self.crescent(7), ramp(0, P.moon, P.moon3, P.moon5), 20, 0)
        cv.px(18, 3, P.red).px(29, 3, P.red).px(13, 3, P.hot).px(34, 3, P.hot)
        return cv

    def stamp(self, lit):
        P = self.P
        cv = Cv(16, 16)
        cv.bevel(disc_mask(14), self.R.gold if lit else ramp(P.edge, P.cardH, P.card, P.cardD), 1, 1)
        return cv.bmp(CHECK, {'#': rgba('#3A2206') if lit else P.edge}, 5, 5)

    def rung(self, status):
        P = self.P
        r = {'reached': self.R.pri, 'current': ramp(P.gold, P.cardH, P.card, P.cardD), 'locked': ramp(P.edge, P.cardH, P.cardD, P.void)}[status]
        cv = Cv(12, 12).bevel(disc_mask(10), r, 1, 1)
        if status == 'reached':
            cv.bmp(CHECK_S, {'#': P.moon}, 4, 5)
        elif status == 'current':
            cv.rect(5, 5, 2, 2, P.gold).px(5, 5, rgba('#FFF0B0'))
        else:
            cv.rect(5, 4, 2, 2, P.edge).rect(5, 6, 2, 2, P.edge)
        return cv

    def task(self, state):
        P = self.P
        r = {'claimable': ramp(P.gold, P.cardH, P.card, P.cardD), 'open': ramp(P.edgeH, P.void, P.cardD, '#241119'), 'claimed': ramp(P.good, P.cardH, P.card, P.cardD),
             'frozen': ramp('#4A6A8A', '#243446', '#182430', '#101820'), 'unavailable': ramp('#2E1A20', '#1C0E14', '#170A10', '#12070C')}[state]
        cv = Cv(10, 10).bevel(rr(8, 8, 2), r, 1, 1)
        if state == 'claimable':
            cv.rect(4, 4, 2, 2, P.gold).px(4, 4, rgba('#FFF0B0'))
        elif state == 'claimed':
            cv.bmp(CHECK_S, {'#': P.good}, 3, 4)
        elif state == 'frozen':
            cv.line(3, 6, 6, 3, rgba('#8AB4D8')).px(3, 3, rgba('#8AB4D8')).px(6, 6, rgba('#8AB4D8'))
        elif state == 'unavailable':
            cv.hl(3, 5, 4, P.edge)
        return cv

    def pip(self, on):
        P = self.P
        return Cv(5, 5).bevel(disc_mask(3), ramp(P.redDD, P.hot, P.red, P.redD) if on else ramp(P.edge, P.cardH, P.card, P.cardD), 1, 1)

    def node(self, st):
        P = self.P
        cv = Cv(34, 34)
        ring = {'selected': P.gold, 'on': P.red, 'off': P.edge}[st]
        if st == 'selected':
            cv.disc(17, 17, 17, 17, with_a(P.gold, GLOW[2])).disc(17, 17, 15, 15, with_a(P.gold, GLOW[1]))
        cv.bevel(disc_mask(26), ramp(ring, P.cardH, P.card, P.cardD), 4, 4)
        if st != 'off':
            cv.ring(17, 17, 12, 12, 1, ring)
        cv.stamp(7, 7, disc_mask(20), P.cardD)
        return cv

    def cover(self):  # the banded night: eight flat bands with a dashed seam; tiles sideways, the last band fills what is left
        cols = [rgba(c) for c in ('#2E0C16', '#270A13', '#20080F', '#19060C', '#130509', '#0E0408', '#0A0307')] + [self.P.void]
        cv, bh = Cv(12, 272), 34
        for i, c in enumerate(cols):
            cv.rect(0, i * bh, 12, bh, c)
            if i:
                cv.dash(0, i * bh, 12, cols[i - 1], 3, 3).dash(3, i * bh + 1, 9, cols[i - 1], 3, 3)
        return cv, None

    def moon_disc(self, d):
        cv, r = Cv(d, d), d / 2
        bands, n = self.bands, len(self.bands)
        for j in range(d):
            for i in range(d):
                dx, dy = (i + 0.5 - r) / r, (j + 0.5 - r) / r
                if dx * dx + dy * dy > 1:
                    continue
                t = min(0.999, math.hypot(dx + 0.28, dy + 0.28) / 1.25)
                band = t * (n - 1)
                b0, fr = int(band), band - int(band)
                bayer = [[0, 2], [3, 1]][j & 1][i & 1] / 4 + 0.125  # a 2 x 2 ordered dither at each seam
                cv.px(i, j, bands[min(n - 1, b0 + (0 if fr < 0.38 else 1 if fr > 0.62 else (1 if fr > bayer else 0)))])
        P = self.P
        for ax, ay, ar in ((-0.35, -0.2, 0.17), (0.3, -0.35, 0.1), (0.12, 0.3, 0.2), (-0.4, 0.35, 0.09), (0.5, 0.15, 0.08)):  # craters
            x, y, rad = r + ax * r, r + ay * r, ar * r
            cv.disc(x, y, rad, rad * 0.9, mix(P.moon4, P.moon6, 0.6)).disc(x - 0.6, y - 0.6, rad * 0.62, rad * 0.55, mix(P.moon3, P.moon4, 0.5))
        return cv.keep(disc_mask(d))

    def cover_art(self):  # the moon, in a corona of four flat steps
        cv = Cv(64, 64)
        for i, a in zip((4, 3, 2, 1), reversed(GLOW)):
            cv.disc(32, 32, 24 + i * 2, 24 + i * 2, with_a(self.P.glow, a))
        return cv.blit(self.moon_disc(48), 8, 8)

    def slot(self, active):
        P = self.P
        cv = Cv(28, 28)
        if active:
            cv.disc(14, 14, 14, 14, with_a(P.glow, GLOW[2])).disc(14, 14, 13, 13, with_a(P.glow, GLOW[1]))
        cv.bevel(disc_mask(22), ramp(P.hot if active else P.edge, P.edgeH, P.card, P.cardD), 3, 3)
        return cv.stamp(6, 6, disc_mask(16), P.cardD)

    def slot_square(self, active):
        P = self.P
        cv = Cv(28, 28)
        if active:
            glow_boxes(cv, 3, 3, 22, 22, P.glow)
        cv.bevel(rr(22, 22, 2), ramp(P.hot if active else P.edge, P.edgeH, P.card, P.cardD), 3, 3)
        return cv.bevel(rr(16, 16, 1), ramp(0, P.void, P.cardD, P.cardD), 6, 6)

    def key_badge(self, wide):
        P = self.P
        w = 15 if wide else 9
        return Cv(w + 2, 11).bevel(rr(w, 9, 1), ramp(P.edge, P.cardH, P.cardD, P.void), 1, 1)

    def hud_label(self):
        return Cv(16, 16).bevel(rr(14, 14, 2), self.R.card, 1, 1), (4, 4, 4, 4)

    def hud_pill(self):
        return self.strip(14)

    def face_plate(self):
        return Cv(20, 20).bevel(rr(18, 18, 2), ramp(self.P.edgeH, self.P.edgeH, self.P.edge, self.P.cardD), 1, 1)

    def boss_frame(self):
        P = self.P
        cv = Cv(32, 32).bevel(rr(30, 30, 2), self.R.card, 1, 1)
        cv.hl(3, 0, 26, P.red).hl(3, 1, 26, mix(P.cardH, P.red, 0.3)).px(2, 0, P.redD).px(29, 0, P.redD)
        return cv, (6, 6, 6, 6)

    def boss_plate(self, shifting):
        P = self.P
        cv = Cv(42, 42)
        c = P.gold if shifting else P.red
        if shifting:
            cv.disc(21, 21, 21, 21, with_a(P.gold, GLOW[2])).disc(21, 21, 19.5, 19.5, with_a(P.gold, GLOW[1]))
        cv.bevel(disc_mask(34), ramp(c, P.edgeH, P.card, P.cardD), 4, 4).stamp(7, 7, disc_mask(28), P.cardD)
        cv.ring(21, 21, 15.5, 15.5, 1, c)
        well = disc_mask(28)
        for y in range(24):
            for x in range(24):
                if well.get(x + 2, y + 2):
                    cv.clr(9 + x, 9 + y)
        return cv

    def boss_name_plate(self):
        return self.strip(13)

    def boss_bar_track(self):
        P = self.P
        return Cv(18, 11).bevel(pill(16, 9), ramp(P.void, P.cardD, P.cardD, P.cardH), 1, 1), (5, 0, 5, 0)

    def boss_bar_fill(self, shifting):
        return self.boss_fill(ramp(0, '#FFF0B0', self.P.gold, self.P.goldD) if shifting else self.R.pri, shifting)


# =====================================================================================================================
class Tensura(Kit):
    id, name = 'tensura', 'Tensura'

    def __init__(self):
        P = self.P = Pal(PALS['tensura'])
        self.R = Pal(
            panel=ramp(P.edge, P.panelH, P.panel, P.panelD), inset=ramp(P.panelH, P.void, '#0A1827', '#12293F'),
            pri=ramp(P.tealD, P.tealH, P.teal, '#2AA0A8', '#7CE4E8', '#2E8E96'), priHot=ramp(P.tealH, '#E4FEFF', '#68E4EA', '#3FB4BA'),
            priDn=ramp(P.tealDD, P.tealDD, P.tealD, '#2AA0A8'), priOff=ramp('#243E52', '#1B3347', '#172C3F', '#12253A'),
            sec=ramp(P.teal, P.panelH, P.panel, P.panelD), secHot=ramp(P.tealH, '#24506C', P.lit, P.panel), secDn=ramp(P.tealD, P.void, P.panelD, P.panel),
            secOff=ramp('#243E52', '#12293F', '#10243A', '#0E2033'), gold=ramp(P.goldD, '#FFE79A', P.gold, '#B98A22'), teal=ramp(P.tealD, P.tealH, P.teal, '#2AA0A8'))
        self.mastery = (P.void, P.edge)
        self.mastery_fill_c = (P.tealH, P.teal)
        self.sheet_bg, self.sheet_bg2 = P.void, P.bg

    def cut(self, w, h, r=2):  # the window shape: top-left and bottom-right corners cut
        return notch(w, h, r, True, False, False, True)

    def bracket(self, cv, x, y, fx, fy, c, n=6):
        cv.blit(Cv(n, n).hl(0, 0, n, c).vl(0, 0, n, c), x, y, fx, fy)

    def panel(self):
        P = self.P
        cv = Cv(32, 32).bevel(self.cut(30, 30, 3), self.R.panel, 1, 1)
        cv.hl(5, 29, 22, mix(P.panelD, P.void, 0.5))
        self.bracket(cv, 24, 2, True, False, P.teal)
        self.bracket(cv, 2, 24, False, True, P.teal)
        return cv, (8, 8, 8, 8)

    def panel_texture(self):  # scanlines
        cv = Cv(64, 64)
        for y in range(1, 64, 2):
            cv.hl(0, y, 64, with_a(WHITE, 10))
        return cv

    def inset(self):
        return Cv(16, 16).bevel(self.cut(14, 14), self.R.inset, 1, 1), (3, 3, 3, 3)

    def tooltip(self):
        P = self.P
        cv = Cv(24, 24).bevel(self.cut(22, 22, 3), ramp(P.teal, P.panelD, P.void, P.void), 1, 1)
        cv.rect(3, 3, 2, 2, P.teal).rect(19, 19, 2, 2, P.tealD)
        return cv, (6, 6, 6, 6)

    def divider(self):
        P = self.P
        return Cv(16, 7).hl(0, 3, 16, P.edge).rect(0, 2, 3, 3, P.teal).rect(13, 2, 3, 3, P.teal).px(0, 2, P.tealH).px(13, 2, P.tealH), (4, 0, 4, 0)

    def divider_gem(self):
        P = self.P
        cv = Cv(15, 9)
        cv.hl(0, 4, 15, P.edge)
        for x in range(2, 13):
            cv.clr(x, 4)
        return cv.bevel(diamond(7), self.R.teal, 4, 1)

    def scroll_track(self):
        return Cv(7, 16).bevel(solid(3, 14), ramp(self.P.edge, self.P.void, self.P.void, self.P.panelD), 2, 1), (0, 3, 0, 3)

    def scroll_thumb(self, hover):
        return Cv(7, 14).bevel(self.cut(5, 12, 1), self.R.priHot if hover else self.R.pri, 1, 1), (0, 4, 0, 4)

    def tab(self, st):
        P = self.P
        r = {'selected': ramp(P.teal, P.edgeH, P.lit, P.panel), 'hover': ramp(P.edgeH, P.panelH, P.lit, P.panel), 'disabled': ramp('#243E52', '#12293F', '#10243A', '#0E2033')}.get(st, self.R.panel)
        cv = Cv(32, 16).bevel(self.cut(30, 14), r, 1, 1)
        if st == 'selected':
            cv.rect(2, 3, 2, 10, P.teal).px(2, 3, P.tealH)
        return cv, (6, 4, 4, 4)

    def button(self, st, pri):
        P, R = self.P, self.R
        r = {'normal': R.pri if pri else R.sec, 'hover': R.priHot if pri else R.secHot, 'pressed': R.priDn if pri else R.secDn, 'disabled': R.priOff if pri else R.secOff}[st]
        cv = Cv(28, 20)
        if st == 'hover':
            cv.stamp(1, 1, self.cut(26, 18), with_a(P.glow, GLOW[1]))
        return cv.bevel(self.cut(24, 16), r, 2, 2, pri and st != 'disabled'), (6, 6, 6, 6)

    def bar_track(self):
        return Cv(16, 8).bevel(solid(14, 6), ramp(self.P.edge, self.P.void, self.P.void, self.P.panelD), 1, 1), (2, 2, 2, 2)

    def bar_fill(self):
        P = self.P
        cv = Cv(6, 4).rect(0, 0, 6, 1, P.tealH).rect(0, 1, 6, 2, P.teal).rect(0, 3, 6, 1, rgba('#2AA0A8'))
        return cv.vl(5, 0, 4, mix(P.void, P.teal, 0.5)), None  # a tile: one segment and its gap

    def plate(self):
        return Cv(24, 14).bevel(self.cut(22, 12), self.R.panel, 1, 1), (4, 4, 4, 4)

    def strip(self, h):
        P = self.P
        cv = Cv(24, h).bevel(self.cut(22, h - 2), self.R.panel, 1, 1)
        return cv.rect(3, 3, 2, h - 6, P.teal).px(3, 3, P.tealH), (6, 4, 4, 4)

    def chip(self, st):
        P = self.P
        r = {'on': self.R.pri, 'shifting': self.R.gold}.get(st, ramp(P.edgeH, P.panelH, P.lit, P.panel))
        return Cv(16, 12).bevel(self.cut(14, 10), r, 1, 1), (4, 4, 4, 4)

    def seal(self, st):
        P = self.P
        r = {'hover': ramp(P.teal, P.edgeH, P.lit, P.panel), 'pressed': ramp(P.tealD, P.void, P.panelD, P.panel)}.get(st, self.R.panel)
        cv = Cv(20, 20).bevel(self.cut(18, 18), r, 1, 1)
        self.bracket(cv, 14, 3, True, False, P.teal if st == 'hover' else P.edgeH, 3)
        self.bracket(cv, 3, 14, False, True, P.teal if st == 'hover' else P.edgeH, 3)
        return cv

    def gem(self, color):
        c = GEMS[color]
        cv = Cv(11, 11).bevel(hex_mask(9, 7, 2), ramp(self.P.void, mix(c, WHITE, 0.55), c, mix(c, BLACK, 0.45)), 1, 2)
        return cv.px(4, 4, WHITE)

    def corner(self):
        P = self.P
        cv = Cv(12, 12)
        cv.rect(0, 0, 12, 2, P.teal).rect(0, 0, 2, 12, P.teal).hl(0, 0, 12, P.tealH).vl(0, 0, 12, P.tealH)
        cv.px(11, 1, P.tealD).px(1, 11, P.tealD).rect(4, 4, 3, 3, P.teal).px(4, 4, P.tealH).px(6, 6, P.tealD)
        cv.clr(0, 0)
        return cv

    def flourish(self):  # a circuit trace
        rows = ['KKKKKKK....', 'K..........', 'K..KKKKK...', 'K..K.......', 'K..K..KK...', 'K..K..KK...', 'K..K.......', '...K.......', '.......K...', '...........', '...........']
        return Cv(11, 11).bmp(rows, {'K': self.P.tealD})

    def flourish_title(self):
        P = self.P
        cv = Cv(48, 7)
        cv.hl(3, 3, 42, P.edgeH).rect(0, 2, 3, 3, P.teal).rect(45, 2, 3, 3, P.teal)
        for x in range(18, 30):
            cv.clr(x, 3)
        cv.bevel(hex_mask(9, 5, 2), self.R.teal, 19, 1)
        cv.hl(8, 5, 8, P.tealD).hl(32, 5, 8, P.tealD)
        return cv

    def stamp(self, lit):
        P = self.P
        cv = Cv(16, 16)
        cv.bevel(self.cut(14, 14, 3), self.R.pri if lit else ramp(P.edge, P.panelH, P.panel, P.panelD), 1, 1)
        return cv.bmp(CHECK, {'#': P.btnText if lit else P.edge}, 5, 5)

    def rung(self, status):
        P = self.P
        r = {'reached': self.R.teal, 'current': ramp(P.gold, P.panelH, P.lit, P.panel), 'locked': ramp(P.edge, P.panelH, P.panelD, P.void)}[status]
        cv = Cv(12, 12).bevel(hex_mask(10, 10, 3), r, 1, 1)
        if status == 'reached':
            cv.bmp(CHECK_S, {'#': P.btnText}, 4, 5)
        elif status == 'current':
            cv.rect(5, 5, 2, 2, P.gold).px(5, 5, rgba('#FFE79A'))
        else:
            cv.rect(5, 4, 2, 2, P.edge).rect(5, 6, 2, 2, P.edge)
        return cv

    def task(self, state):
        P = self.P
        r = {'claimable': ramp(P.gold, P.panelH, P.lit, P.panel), 'open': ramp(P.edgeH, P.void, '#0A1827', '#12293F'), 'claimed': ramp(P.good, P.panelH, P.panel, P.panelD),
             'frozen': ramp('#5A7A9A', '#2A4258', '#1E3246', '#162638'), 'unavailable': ramp('#243E52', '#12293F', '#10243A', '#0E2033')}[state]
        cv = Cv(10, 10).bevel(solid(8, 8), r, 1, 1)
        if state == 'claimable':
            cv.rect(4, 4, 2, 2, P.gold).px(4, 4, rgba('#FFE79A'))
        elif state == 'claimed':
            cv.bmp(CHECK_S, {'#': P.good}, 3, 4)
        elif state == 'frozen':
            cv.line(3, 6, 6, 3, rgba('#A8C8E8')).px(3, 3, rgba('#A8C8E8')).px(6, 6, rgba('#A8C8E8'))
        elif state == 'unavailable':
            cv.hl(3, 5, 4, P.edge)
        return cv

    def pip(self, on):
        P = self.P
        return Cv(5, 5).bevel(solid(3, 3), self.R.teal if on else ramp(P.edge, P.panelH, P.panel, P.panelD), 1, 1)

    def node(self, st):
        P = self.P
        cv = Cv(34, 34)
        ring = {'selected': P.gold, 'on': P.teal, 'off': P.edge}[st]
        if st == 'selected':
            cv.stamp(0, 1, hex_mask(34, 33, 10), with_a(P.glow, GLOW[2])).stamp(2, 2, hex_mask(30, 30, 9), with_a(P.glow, GLOW[1]))
        cv.bevel(hex_mask(26, 25, 7), ramp(ring, P.panelH, P.panel, P.panelD), 4, 5)
        return cv.stamp(7, 8, hex_mask(20, 19, 5), rgba('#0A1827'))

    def cover(self):
        P = self.P
        cv = Cv(32, 32).bevel(self.cut(30, 30, 4), ramp(P.void, P.edge, P.bg, '#0A192A'), 1, 1)
        cv.box(3, 3, 26, 26, mix(P.bg, P.edge, 0.6))
        for x, y, fx, fy in ((4, 4, 0, 0), (22, 4, 1, 0), (4, 22, 0, 1), (22, 22, 1, 1)):
            self.bracket(cv, x, y, bool(fx), bool(fy), P.teal)
        return cv, (10, 10, 10, 10)

    def cover_art(self):  # the title bar of the window: fixed left end with the gem, the rest tiles
        P = self.P
        cv = Cv(24, 13).rect(0, 0, 24, 12, P.lit).hl(0, 12, 24, P.edge).hl(0, 0, 24, P.panelH)
        cv.bevel(diamond(7), self.R.pri, 3, 2)
        cv.hl(13, 5, 2, P.edgeH).hl(13, 7, 2, P.edgeH)
        return cv

    def slot(self, active):
        P = self.P
        cv = Cv(28, 28)
        if active:
            cv.stamp(0, 1, hex_mask(28, 26, 8), with_a(P.glow, GLOW[2])).stamp(1, 2, hex_mask(26, 24, 7), with_a(P.glow, GLOW[1]))
        cv.bevel(hex_mask(22, 20, 6), ramp(P.tealH if active else P.teal, P.edgeH, P.panel, P.panelD), 3, 4)
        return cv.bevel(hex_mask(16, 14, 4), ramp(0, P.panelD, '#0A1827', P.panelD), 6, 7)

    def slot_square(self, active):
        P = self.P
        cv = Cv(28, 28)
        if active:
            glow_boxes(cv, 3, 3, 22, 22, P.glow)
        cv.bevel(self.cut(22, 22), ramp(P.tealH if active else P.teal, P.edgeH, P.panel, P.panelD), 3, 3)
        return cv.bevel(solid(16, 16), ramp(0, P.panelD, '#0A1827', P.panelD), 6, 6)

    def key_badge(self, wide):
        w = 15 if wide else 9
        return Cv(w + 2, 11).bevel(self.cut(w, 9), self.R.teal, 1, 1)

    def hud_label(self):
        return Cv(16, 16).bevel(self.cut(14, 14), self.R.panel, 1, 1), (4, 4, 4, 4)

    def hud_pill(self):
        return self.strip(14)

    def face_plate(self):
        return Cv(20, 20).bevel(self.cut(18, 18), ramp(self.P.teal, self.P.teal, self.P.edgeH, self.P.edge), 1, 1)

    def boss_frame(self):
        P = self.P
        cv = Cv(32, 32).bevel(self.cut(30, 30, 3), self.R.panel, 1, 1)
        for x, y, fx, fy in ((2, 2, 0, 0), (24, 2, 1, 0), (2, 24, 0, 1), (24, 24, 1, 1)):
            self.bracket(cv, x, y, bool(fx), bool(fy), P.teal)
        cv.clr(1, 1)
        return cv, (8, 8, 8, 8)

    def boss_plate(self, shifting):
        P = self.P
        cv = Cv(42, 42)
        if shifting:
            cv.stamp(0, 0, hex_mask(42, 42, 9), with_a(P.glow, GLOW[2])).stamp(2, 2, hex_mask(38, 38, 8), with_a(P.glow, GLOW[1]))
        cv.bevel(hex_mask(34, 34, 7), ramp(P.gold if shifting else P.teal, P.edgeH, P.panel, P.panelD), 4, 4)
        well = self.cut(26, 26, 3)
        cv.stamp(8, 8, well, rgba('#0A1827'))
        win = self.cut(24, 24, 2)
        for y in range(24):
            for x in range(24):
                if win.d[y * 24 + x]:
                    cv.clr(9 + x, 9 + y)
        return cv

    def boss_name_plate(self):
        return self.strip(13)

    def boss_bar_track(self):
        P = self.P
        return Cv(18, 11).bevel(solid(16, 9), ramp(P.edge, P.void, P.void, P.panelD), 1, 1), (2, 2, 2, 2)

    def boss_bar_fill(self, shifting):
        return self.boss_fill(self.R.gold if shifting else self.R.pri, shifting)


# =====================================================================================================================
def build_theme(k):
    """Every sprite of one theme, in the order of the plan's table A. Returns [(name, Cv, slice)]."""
    out = []

    def add(name, got, use, glow=False):
        cv, sl = got if isinstance(got, tuple) else (got, None)
        emit(cv, 'kit/%s/%s.png' % (k.id, name), use, sl, glow)
        out.append((name, cv, sl))

    add('panel', k.panel(), 'page panels, cards, the status card')
    add('panel_texture', k.panel_texture(), 'a tile laid over a panel or the cover', True)
    add('inset', k.inset(), 'wells, list rows, text fields')
    add('tooltip', k.tooltip(), 'tooltips and the detail popover')
    add('divider', k.divider(), 'rules')
    add('divider_gem', k.divider_gem(), 'the ornament at the middle of a rule')
    add('scroll_track', k.scroll_track(), 'scroll bar track')
    add('scroll_thumb', k.scroll_thumb(False), 'scroll bar thumb')
    add('scroll_thumb_hover', k.scroll_thumb(True), 'scroll bar thumb, hovered or dragged')
    for st in ('normal', 'hover', 'selected', 'disabled'):
        add('tab' if st == 'normal' else 'tab_' + st, k.tab(st), 'page tab, ' + st)
    for pri in (False, True):
        for st in STATES:
            got = k.button_focus(pri) if st == 'focus' else k.button(st, pri)
            add('button_%s%s' % ('primary_' if pri else '', st), got, ('primary' if pri else 'secondary') + ' button, ' + st, st == 'hover' and k.id != 'grimoire')
    add('focus', k.focus(), 'the 2 px focus ring')
    add('bar_track', k.bar_track(), 'progress bar track')
    add('bar_fill', k.bar_fill(), 'progress bar fill')
    add('plate', k.plate(), 'counters and small name plates')
    for st in ('off', 'on', 'shifting'):
        add('chip' if st == 'off' else 'chip_' + st, k.chip(st), 'rank and source tags, boss form chips: ' + st)
    for st in ('normal', 'hover', 'pressed'):
        add('seal' if st == 'normal' else 'seal_' + st, k.seal(st), 'rail seals and launcher tiles, ' + st)
    for c in GEMS:
        add('gem_' + c, k.gem(c), 'gem')
    cn, fl = k.corner(), k.flourish()
    for nm, fx, fy in (('tl', 0, 0), ('tr', 1, 0), ('bl', 0, 1), ('br', 1, 1)):
        add('corner_' + nm, cn.flipped(bool(fx), bool(fy)), 'corner fitting')
    for nm, fx, fy in (('tl', 0, 0), ('tr', 1, 0), ('bl', 0, 1), ('br', 1, 1)):
        add('flourish_' + nm, fl.flipped(bool(fx), bool(fy)), 'corner filigree')
    add('flourish_title', k.flourish_title(), 'the flourish under a page title')
    add('stamp', k.stamp(False), 'the stamp of a quest, not claimed')
    add('stamp_lit', k.stamp(True), 'the stamp of a quest, claimed')
    for st in ('reached', 'current', 'locked'):
        add('rung_' + st, k.rung(st), 'rung mark, ' + st)
    for st in ('claimable', 'open', 'claimed', 'frozen', 'unavailable'):
        add('task_' + st, k.task(st), 'task box, ' + st)
    add('pip_on', k.pip(True), 'pip, filled')
    add('pip_off', k.pip(False), 'pip, empty')
    for st in ('off', 'on', 'selected'):
        add('node_' + st, k.node(st), 'skill node and medallion, ' + st, st == 'selected')
    add('cover', k.cover(), 'the hub background')
    add('cover_art', k.cover_art(), "the theme's signature piece (cover boss, moon, title bar)", k.id == 'bloodmoon')
    add('slot', k.slot(False), 'HUD slot')
    add('slot_active', k.slot(True), 'HUD slot, active', True)
    add('slot_square', k.slot_square(False), 'HUD slot in Tensura mode')
    add('slot_square_active', k.slot_square(True), 'HUD slot in Tensura mode, active', True)
    add('key_badge', k.key_badge(False), 'key badge, one character')
    add('key_badge_wide', k.key_badge(True), 'key badge, two characters')
    add('hud_label', k.hud_label(), 'the Alt label box')
    add('hud_pill', k.hud_pill(), 'the mode pill')
    add('face_plate', k.face_plate(), "the frame of the player's face")
    add('mastery_track', k.mastery_track(), 'mastery bar track')
    add('mastery_fill', k.mastery_fill('progress'), 'mastery bar fill (tile)')
    add('mastery_fill_learning', k.mastery_fill('learning'), 'mastery bar fill while learning (tile)')
    add('mastery_fill_mastered', k.mastery_fill('mastered'), 'mastery bar fill when mastered (tile)')
    add('boss_frame', k.boss_frame(), 'the boss bar card')
    add('boss_portrait_plate', k.boss_plate(False), 'drawn over the 24 x 24 portrait')
    add('boss_portrait_plate_shifting', k.boss_plate(True), 'the plate during a shift', True)
    add('boss_name_plate', k.boss_name_plate(), 'the boss name')
    add('boss_bar_track', k.boss_bar_track(), 'boss health track')
    add('boss_bar_fill', k.boss_bar_fill(False), 'boss health fill (tile)')
    add('boss_bar_fill_shifting', k.boss_bar_fill(True), 'boss health fill during a shift (tile)')
    add('boss_bar_tick', k.boss_bar_tick(), 'ticks at 25, 50 and 75 %', True)
    return out


def build_shared():
    out, T = [], Pal(PALS['tensura'])
    sweep = Cv(16, 16 * 33)
    for k in range(1, 33):
        f = Cv(16, 16)
        pie(f, 8, 8, 10, k / 32, with_a(BLACK, 175))
        sweep.blit(f, 0, k * 16)
    out.append(('cooldown_sweep', emit(sweep, 'kit/shared/cooldown_sweep.png', 'cooldown pie, 33 frames of 16 x 16 (frame 0 empty)', None, True)))
    ring = Cv(32, 32 * 25)
    for step in range(25):
        lit = step * 72 // 24
        for i in range(72):
            a = 2 * math.pi * i / 72
            ring.rect(16 + round(math.sin(a) * 12.5) - 1, step * 32 + 16 - round(math.cos(a) * 12.5) - 1, 2, 2, rgba('#D8182E') if i < lit else rgba('#5A1018'))
    out.append(('blood_ring', emit(ring, 'kit/shared/blood_ring.png', 'the ring of blood round Bite, 25 frames of 32 x 32')))
    mark = Cv(7, 7).bevel(diamond(5), ramp(T.tealD, T.tealH, T.teal, '#2AA0A8'), 1, 1)
    out.append(('mode_mark', emit(mark, 'kit/shared/mode_mark.png', 'the corner diamond of Tensura mode')))
    full = ['....K....', '...KHK...', '...KRK...', '..KHRRK..', '.KHRRRDK.', '.KRWRRDK.', '.KRRRRDK.', '..KDDDK..', '...KKK...']
    pal = {'K': '#3A0A10', 'H': '#FF6A60', 'R': '#C01830', 'D': '#7A0E1E', 'W': '#FFC8C2'}
    out.append(('blood_full', emit(Cv(9, 9).bmp(full, pal), 'kit/shared/blood_full.png', 'a blood point in the food bar, 9 x 9')))
    empty = {'K': '#3A0A10', 'H': '#2A0A10', 'R': '#1C060A', 'D': '#14040A', 'W': '#2A0A10'}
    out.append(('blood_empty', emit(Cv(9, 9).bmp(full, empty), 'kit/shared/blood_empty.png', 'an empty blood point, 9 x 9')))
    return out


def kit_sheet(k, sprites, path):
    """Contact sheet of one theme: every sprite at 1x and 4x, then the nine-slices stretched the way the game will."""
    cells = [(n, c) for n, c, _ in sprites if c.h <= 80]
    by = {n: (c, s) for n, c, s in sprites}
    demo = Cv(424, 150)
    pc, ps = by['panel']
    demo.blit(nine(pc, 204, 96, ps), 0, 0)
    ic, isl = by['inset']
    demo.blit(nine(ic, 60, 30, isl), 8, 10)
    x = 8
    for st in ('normal', 'hover', 'pressed', 'disabled', 'focus'):
        for row, pri in enumerate((False, True)):
            c, s = by['button_%s%s' % ('primary_' if pri else '', st)]
            demo.blit(nine(c, 28 if st != 'normal' else 64, 20, s), x - 2, 44 + row * 22)
        x += 68 if st == 'normal' else 30
    tc, ts = by['tooltip']
    demo.blit(nine(tc, 90, 40, ts), 212, 2)
    dc, ds = by['divider']
    demo.blit(nine(dc, 120, 7, ds), 212, 50).blit(by['divider_gem'][0], 212 + 52, 49)
    x = 212
    for st in ('tab', 'tab_hover', 'tab_selected', 'tab_disabled'):
        c, s = by[st]
        demo.blit(nine(c, 44, 16, s), x, 62)
        x += 46 if st != 'tab_selected' else 46
    bc, bs = by['boss_frame']
    demo.blit(nine(bc, 250, 46, bs), 0, 100)
    demo.blit(by['boss_portrait_plate'][0], 1, 102)
    nc, ns = by['boss_name_plate']
    demo.blit(nine(nc, 200, 13, ns), 45, 105)
    tr, trs = by['boss_bar_track']
    demo.blit(nine(tr, 202, 11, trs), 44, 120)
    f = by['boss_bar_fill'][0]
    for i in range(0, 120, 8):
        demo.blit(f, 46 + i, 122)
    x = 250 - 8
    for st in ('chip', 'chip_on', 'chip_shifting'):
        c, s = by[st]
        x -= 44
        demo.blit(nine(c, 42, 12, s), x, 132)
    sc, ss = by['scroll_track']
    demo.blit(nine(sc, 7, 90, ss), 410, 4)
    th, ths = by['scroll_thumb']
    demo.blit(nine(th, 7, 28, ths), 410, 20)
    cells.append(('nine-slice preview: panel, inset, buttons, tooltip, rule, tabs, boss bar, scroll bar', demo))
    return sheet(path, cells, k.sheet_bg, zoom=4)
