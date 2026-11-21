// Tensura kit: a status window. Deep blue panels, notched corners, teal brackets, hex gems. 1 texel = 1 GUI px.
const TP = {
  bg: rgba('#0D2035'), void: rgba('#06121F'), panel: rgba('#12293F'), panelH: rgba('#1B3C57'), panelD: rgba('#0F2338'), lit: rgba('#17374E'), edge: rgba('#2A5370'), edgeH: rgba('#3F7694'),
  teal: rgba('#3FD0D6'), tealH: rgba('#A8F0F3'), tealD: rgba('#1F6A74'), tealDD: rgba('#0F3A44'), text: rgba('#E6F3FB'), mut: rgba('#8FB2C4'), gold: rgba('#E8B33A'), goldD: rgba('#8A6418'),
  good: rgba('#5EE08A'), warn: rgba('#F0646A'), btnText: rgba('#06222A'), glow: rgba('#7CF4F8'), ready: rgba('#3FD0D6'), focusD: rgba('#06121F'), focusL: rgba('#FFFFFF'),
};
const TR = {
  panel: ramp(TP.edge, TP.panelH, TP.panel, TP.panelD),
  inset: ramp(TP.panelH, TP.void, rgba('#0A1827'), rgba('#12293F')),
  pri: ramp(TP.tealD, TP.tealH, TP.teal, rgba('#2AA0A8'), rgba('#7CE4E8'), rgba('#2E8E96')),
  priHot: ramp(TP.tealH, rgba('#E4FEFF'), rgba('#68E4EA'), rgba('#3FB4BA')),
  priDn: ramp(TP.tealDD, TP.tealDD, TP.tealD, rgba('#2AA0A8')),
  priOff: ramp(rgba('#243E52'), rgba('#1B3347'), rgba('#172C3F'), rgba('#12253A')),
  sec: ramp(TP.teal, TP.panelH, TP.panel, TP.panelD),
  secHot: ramp(TP.tealH, rgba('#24506C'), TP.lit, TP.panel),
  secDn: ramp(TP.tealD, TP.void, TP.panelD, TP.panel),
  secOff: ramp(rgba('#243E52'), rgba('#12293F'), rgba('#10243A'), rgba('#0E2033')),
  hex: ramp(TP.teal, TP.tealH, rgba('#16384E'), rgba('#0A1F30')),
  gold: ramp(TP.goldD, rgba('#FFE79A'), TP.gold, rgba('#B98A22')),
};
const TK = {
  id: 'tensura', name: 'Tensura', paper: TP.panel, ink: TP.text, muted: TP.mut, accent: TP.teal, good: TP.good, dark: TP.void, glow: TP.glow, ready: TP.teal,
  focusD: TP.focusD, focusL: TP.focusL,
  brackets(cv, x, y, w, h, c) { // four corner brackets
    for (const [bx, by, fx, fy] of [[x, y, 0, 0], [x + w - 6, y, 1, 0], [x, y + h - 6, 0, 1], [x + w - 6, y + h - 6, 1, 1]]) {
      const b = new Cv(6, 6); b.hl(0, 0, 6, c); b.vl(0, 0, 6, c); cv.blit(b, bx, by, !!fx, !!fy);
    }
  },
  panel(cv, x, y, w, h) {
    cv.bevel(notch(w, h, 3, true, false, false, true), TR.panel, x, y);
    cv.hl(x + 4, y + h - 2, w - 8, withA(TP.void, 120));
    for (let i = 3; i < h - 3; i += 2) cv.hl(x + 1, y + i, w - 2, withA(0xFFFFFFFF, 5)); // faint scanlines
    for (const [bx, by, fx, fy] of [[x + w - 7, y + 1, 1, 0], [x + 1, y + h - 7, 0, 1]]) { const b = new Cv(6, 6); b.hl(0, 0, 6, TP.teal); b.vl(0, 0, 6, TP.teal); cv.blit(b, bx, by, !!fx, !!fy); }
  },
  inset(cv, x, y, w, h) { cv.bevel(notch(w, h, 2, true, false, false, true), TR.inset, x, y); },
  divider(cv, x, y, w) {
    cv.hl(x, y, w, TP.edge); cv.rect(x, y - 1, 3, 3, TP.teal); cv.rect(x + w - 3, y - 1, 3, 3, TP.teal);
    const cx = x + (w >> 1), d = new Cv(7, 7); d.poly([[3.5, 0], [7, 3.5], [3.5, 7], [0, 3.5]], 1); cv.rect(cx - 6, y - 1, 12, 3, TP.panel); cv.bevel(d, ramp(TP.tealD, TP.tealH, TP.teal, rgba('#2AA0A8')), cx - 3, y - 3);
  },
  scroll(cv, x, y, h, pos, th) {
    cv.bevel(solid(3, h), ramp(TP.edge, TP.void, TP.void, TP.panelD), x + 1, y);
    const ty = y + 1 + Math.round(pos * (h - 2 - th)); cv.bevel(notch(5, th, 1, true, false, false, true), TR.pri, x, ty);
  },
  button(cv, x, y, w, h, st, pri) {
    const m = notch(w, h, 2, true, false, false, true);
    let r = pri ? TR.pri : TR.sec;
    if (st === 'hover') r = pri ? TR.priHot : TR.secHot; if (st === 'pressed') r = pri ? TR.priDn : TR.secDn; if (st === 'disabled') r = pri ? TR.priOff : TR.secOff;
    if (st === 'hover') cv.bevel(notch(w + 2, h + 2, 2, true, false, false, true), ramp(0, withA(TP.glow, 55), withA(TP.glow, 55), withA(TP.glow, 55)), x - 1, y - 1);
    cv.bevel(m, r, x, y, pri && st !== 'disabled');
    return { dy: st === 'pressed' ? 1 : 0, text: pri ? (st === 'pressed' ? TP.text : st === 'disabled' ? rgba('#5F7C8E') : TP.btnText) : (st === 'disabled' ? rgba('#5F7C8E') : TP.text) };
  },
  tab(cv, x, y, w, h, st) { // a page entry of the left list
    const sel = st === 'selected';
    cv.bevel(notch(w, h, 2, true, false, false, true), sel ? ramp(TP.teal, TP.edgeH, TP.lit, TP.panel) : st === 'hover' ? ramp(TP.edgeH, TP.panelH, TP.lit, TP.panel) : st === 'disabled' ? ramp(rgba('#243E52'), rgba('#12293F'), rgba('#10243A'), rgba('#0E2033')) : ramp(TP.edge, TP.panelH, TP.panel, TP.panelD), x, y);
    if (sel) cv.rect(x + 1, y + 2, 2, h - 4, TP.teal);
  },
  bar(cv, x, y, w, h, f) {
    cv.bevel(solid(w, h), ramp(TP.edge, TP.void, TP.void, TP.panelD), x, y);
    const fw = Math.round((w - 2) * f); if (fw > 0) { cv.bevel(solid(fw, h - 2), ramp(0, TP.tealH, TP.teal, rgba('#2AA0A8')), x + 1, y + 1); }
    for (let i = 6; i < w - 1; i += 6) cv.vl(x + i, y + 1, h - 2, withA(TP.void, 130));
  },
  focus(cv, x, y, w, h) { cv.box(x - 2, y - 2, w + 4, h + 4, TP.focusD); cv.box(x - 1, y - 1, w + 2, h + 2, TP.focusL); },
  cover(cv, x, y, w, h) { // the window itself
    cv.bevel(notch(w, h, 4, true, false, false, true), ramp(TP.void, TP.edge, TP.bg, rgba('#0A192A')), x, y);
    cv.box(x + 2, y + 2, w - 4, h - 4, withA(TP.edge, 150));
    TK.brackets(cv, x + 3, y + 3, w - 6, h - 6, TP.teal);
  },
  title(cv, x, y, w) { cv.rect(x, y, w, 12, TP.lit); cv.hl(x, y + 12, w, TP.edge); cv.rect(x + 3, y + 3, 6, 6, 0); const d = new Cv(7, 7); d.poly([[3.5, 0], [7, 3.5], [3.5, 7], [0, 3.5]], 1); cv.bevel(d, TR.pri, x + 3, y + 2); },
  halo(cv, cx, cy, r, c, steps = 3) { for (let i = steps; i >= 1; i--) cv.disc(cx, cy, r + i * 2, r + i * 2, withA(c, Math.round(44 / i))); },
  seal(cv, x, y, d, st) { // launcher tile
    const on = st === 'hover' || st === 'selected'; cv.bevel(notch(d, d, 2, true, false, false, true), on ? ramp(TP.teal, TP.edgeH, TP.lit, TP.panel) : TR.panel, x, y);
  },
  gem(cv, x, y, n, c) { const m = hexMask(n + 2, n, Math.round(n / 3)); cv.bevel(m, ramp(TP.tealD, mix(c, 0xFFFFFFFF, 0.55), c, mix(c, 0xFF000000, 0.45)), x, y); cv.px(x + 3, y + 2, 0xFFFFFFFF); },
  curl(cv, x, y, flipX, flipY, c) { const b = new Cv(9, 9); b.bmp(['KKKKKKK..', 'K........', 'K..KKKK..', 'K..K.....', 'K..K..KK.', 'K..K..K..'], { K: c }); cv.blit(b, x, y, flipX, flipY); },
  plate(cv, x, y, w, h) { cv.bevel(notch(w, h, 2, true, false, false, true), TR.panel, x, y); cv.rect(x + 2, y + 2, 2, h - 4, TP.teal); },
  slot(cv, x, y, n, st) { // hex slot, n wide (height ~ n*0.9)
    const sel = st === 'active', h = Math.round(n * 0.9);
    if (sel) cv.bevel(hexMask(n + 6, h + 6, Math.round(n / 4) + 1), ramp(0, withA(TP.glow, 60), withA(TP.glow, 60), withA(TP.glow, 60)), x - 3, y - 3);
    cv.bevel(hexMask(n, h, Math.round(n / 4)), ramp(sel ? TP.tealH : TP.teal, TP.edgeH, TP.panel, TP.panelD), x, y);
    cv.bevel(hexMask(n - 6, h - 6, Math.round(n / 4) - 2), ramp(0, TP.panelD, rgba('#0A1827'), TP.panelD), x + 3, y + 3);
  },
  keyseal(cv, x, y) { cv.bevel(notch(9, 9, 2, true, false, false, true), ramp(TP.tealD, TP.tealH, TP.teal, rgba('#2AA0A8')), x, y); },
};
