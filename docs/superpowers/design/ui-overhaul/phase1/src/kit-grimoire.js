// Grimoire kit: leather cover, brass, parchment pages, wax. Pixel recipes, 1 texel = 1 GUI px.

const GP = {
  leD: rgba('#260A0A'), le: rgba('#4A1717'), leM: rgba('#5E2020'), leH: rgba('#7C3030'),
  brD: rgba('#3A280A'), brS: rgba('#8A6A22'), br: rgba('#B8903A'), brH: rgba('#E3C16F'), brHH: rgba('#F6E2A0'),
  pa: rgba('#E9DDB8'), paH: rgba('#F5ECCF'), paS: rgba('#D3C390'), paD: rgba('#BBA874'), paDD: rgba('#9B8656'),
  bord: rgba('#7A5A32'), ink: rgba('#2A1A10'), mut: rgba('#6A5236'), red: rgba('#7A1218'), redH: rgba('#A82A2C'), good: rgba('#2F6B3A'),
  wax: rgba('#9A1C22'), waxD: rgba('#4E0C10'), waxH: rgba('#D2494A'), waxM: rgba('#6E1217'),
  glow: rgba('#FFD58A'), ready: rgba('#F2C14E'), dark: rgba('#120707'), focusD: rgba('#1A1008'), focusL: rgba('#FFD36B'),
};
const GR = {
  page: ramp(GP.bord, GP.paH, GP.pa, GP.paS),
  inset: ramp(GP.bord, GP.paD, rgba('#DCCD9E'), GP.paH),
  btn: ramp(GP.bord, GP.paH, rgba('#D8C896'), rgba('#B49E6A')),
  btnHot: ramp(GP.br, GP.brHH, rgba('#EADFAE'), rgba('#C9B070')),
  btnDn: ramp(GP.bord, rgba('#A58F5E'), rgba('#C6B27C'), GP.paH),
  btnOff: ramp(rgba('#A89A78'), rgba('#E6DEC8'), rgba('#D5CBB0'), rgba('#C3B896')),
  pri: ramp(rgba('#2E0A0C'), rgba('#C2373B'), rgba('#8E1C22'), rgba('#5E0F14'), rgba('#A8282C'), rgba('#741519')),
  priHot: ramp(GP.br, rgba('#E4565A'), rgba('#AE2A30'), rgba('#6E1519'), rgba('#C8383D'), rgba('#8A1D22')),
  priDn: ramp(rgba('#2E0A0C'), rgba('#5E0F14'), rgba('#741519'), rgba('#A8282C')),
  priOff: ramp(rgba('#A89A78'), rgba('#E6DEC8'), rgba('#CFC4A6'), rgba('#BDB08C')),
  brass: ramp(GP.brD, GP.brHH, GP.br, GP.brS, GP.brH),
  leather: ramp(GP.leD, GP.leH, GP.le, rgba('#33100F'), GP.leM),
  wax: ramp(GP.waxD, GP.waxH, GP.wax, GP.waxM, rgba('#B53539')),
};
const GK = {
  id: 'grimoire', name: 'Grimoire', paper: GP.pa, ink: GP.ink, muted: GP.mut, accent: GP.red, good: GP.good, dark: GP.dark, glow: GP.glow, ready: GP.ready,
  focusD: GP.focusD, focusL: GP.focusL,
  panel(cv, x, y, w, h) {
    cv.bevel(rr(w, h, 2), GR.page, x, y);
    cv.grain(x + 2, y + 2, w - 4, h - 4, withA(GP.paD, 110), 0.006, 11 + w, 1);
    for (let i = 4; i < w - 4; i += 2) { cv.px(x + i, y + 3, GP.paS); cv.px(x + i, y + h - 4, GP.paS); }
    for (let j = 4; j < h - 4; j += 2) { cv.px(x + 3, y + j, GP.paS); cv.px(x + w - 4, y + j, GP.paS); }
  },
  inset(cv, x, y, w, h) { cv.bevel(rr(w, h, 1), GR.inset, x, y); },
  divider(cv, x, y, w) {
    cv.hl(x, y, w, GP.bord); cv.hl(x + 1, y + 1, w - 2, withA(GP.paH, 200));
    const cx = x + (w >> 1);
    cv.rect(cx - 7, y - 2, 14, 5, GP.pa); cv.hl(cx - 7, y, 14, GP.pa);
    const d = new Cv(7, 7); d.poly([[3.5, 0], [7, 3.5], [3.5, 7], [0, 3.5]], 1);
    cv.bevel(d, GR.brass, cx - 3, y - 3);
    cv.px(cx - 6, y, GP.bord); cv.px(cx + 5, y, GP.bord); cv.px(cx - 8, y, GP.paD); cv.px(cx + 7, y, GP.paD);
  },
  scroll(cv, x, y, h, pos, th) { // 5 wide: ink slot + leather thumb with brass caps
    cv.bevel(rr(5, h, 1), ramp(GP.bord, GP.paD, rgba('#CBB985'), GP.paS), x, y);
    const ty = y + 1 + Math.round(pos * (h - 2 - th)), m = rr(5, th, 1);
    cv.bevel(m, GR.leather, x, ty); cv.hl(x + 1, ty + 1, 3, GP.br); cv.hl(x + 1, ty + th - 2, 3, GP.brS);
    cv.px(x + 2, ty + (th >> 1), GP.brH);
  },
  button(cv, x, y, w, h, st, pri) {
    const m = rr(w, h, 2);
    let r = pri ? GR.pri : GR.btn;
    if (st === 'hover') r = pri ? GR.priHot : GR.btnHot; if (st === 'pressed') r = pri ? GR.priDn : GR.btnDn; if (st === 'disabled') r = pri ? GR.priOff : GR.btnOff;
    cv.bevel(m, r, x, y, pri && st !== 'disabled');
    if (pri && st !== 'disabled') { cv.px(x + 2, y + (h >> 1), GP.brH); cv.px(x + w - 3, y + (h >> 1), GP.brH); }
    if (st === 'pressed') cv.hl(x + 2, y + 1, w - 4, withA(0xFF000000, 60));
    return { dy: st === 'pressed' ? 1 : 0, text: pri ? (st === 'disabled' ? rgba('#7C6F52') : GP.paH) : (st === 'disabled' ? rgba('#7C6F52') : GP.ink) };
  },
  tab(cv, x, y, w, h, st) { // index tab on the right edge of the book; selected sits 3 px further out
    const sel = st === 'selected', xo = sel ? -3 : 0, m = rr(w + (sel ? 3 : 0), h, 0);
    for (let j = 0; j < m.h; j++) for (let i = m.w - 3; i < m.w; i++) { const dx = m.w - 1 - i, dy = Math.min(j, m.h - 1 - j); if (dx + dy < 2) m.d[j * m.w + i] = 0; }
    const r = sel ? ramp(GP.brD, GP.redH, rgba('#A8282C'), rgba('#6E1519'), rgba('#BF3A3E')) : st === 'hover' ? ramp(GP.brD, GP.leH, GP.leM, GP.le) : st === 'disabled' ? ramp(rgba('#4a4343'), rgba('#6a6060'), rgba('#554C4C'), rgba('#3E3636')) : GR.leather;
    cv.bevel(m, r, x + xo, y);
    if (sel) { cv.hl(x + xo + 1, y + 1, m.w - 3, GP.brH); cv.hl(x + xo + 1, y + h - 2, m.w - 3, GP.br); }
    cv.px(x + xo + 1, y + 1, GP.brD);
  },
  bar(cv, x, y, w, h, f) {
    cv.bevel(rr(w, h, 1), ramp(GP.bord, GP.paDD, rgba('#CBB985'), GP.paH), x, y);
    const fw = Math.round((w - 2) * f); if (fw > 0) { cv.bevel(solid(fw, h - 2), ramp(0, rgba('#C2373B'), rgba('#8E1C22'), rgba('#5E0F14')), x + 1, y + 1); }
    for (let i = 8; i < w - 1; i += 8) cv.vl(x + i, y + 1, h - 2, withA(GP.bord, 90));
  },
  focus(cv, x, y, w, h) { cv.box(x - 2, y - 2, w + 4, h + 4, GP.focusD); cv.box(x - 1, y - 1, w + 2, h + 2, GP.focusL); },
  cover(cv, x, y, w, h) {
    cv.rect(x, y, w, h, GP.le); cv.box(x, y, w, h, GP.leD);
cv.grain(x + 1, y + 1, w - 2, h - 2, withA(GP.leM, 160), 0.02, 5 + w, 1); cv.grain(x + 1, y + 1, w - 2, h - 2, withA(GP.leD, 120), 0.012, 9 + h, 1);
    cv.hl(x + 1, y + 1, w - 2, GP.leH); cv.vl(x + 1, y + 1, h - 2, GP.leH);
    cv.box(x + 3, y + 3, w - 6, h - 6, GP.br);
    for (let i = 7; i < w - 7; i += 3) { cv.px(x + i, y + 6, GP.brS); cv.px(x + i, y + h - 7, GP.brS); cv.px(x + i + 1, y + 6, GP.brS); cv.px(x + i + 1, y + h - 7, GP.brS); }
    for (let j = 7; j < h - 7; j += 3) { cv.px(x + 6, y + j, GP.brS); cv.px(x + w - 7, y + j, GP.brS); cv.px(x + 6, y + j + 1, GP.brS); cv.px(x + w - 7, y + j + 1, GP.brS); }
    for (const [fx, fy] of [[0, 0], [1, 0], [0, 1], [1, 1]]) GK.cap(cv, fx ? x + w - 12 : x, fy ? y + h - 12 : y, fx, fy);
  },
  cap(cv, x, y, fx, fy) { // 12x12 brass corner plate with rivet
    const m = new Cv(12, 12); m.rect(0, 0, 12, 4, 1); m.rect(0, 0, 4, 12, 1); m.clr(11, 0); m.clr(0, 0); m.clr(11, 3);
    const c = new Cv(12, 12); c.bevel(m, GR.brass, 0, 0, true);
    c.px(1, 1, GP.brHH); c.px(2, 1, GP.br); c.px(1, 2, GP.br); c.px(2, 2, GP.brD);
    cv.blit(c, x, y, !!fx, !!fy);
  },
  seal(cv, x, y, d, st) { // wax seal, d = 16 or 20
    const m = new Cv(d, d); m.disc(d / 2, d / 2, d / 2, d / 2, 1); m.px(1, d - 5, 1); m.px(0, d - 6, 1); m.px(d - 2, 4, 1); m.px(d - 1, 5, 1);
    const r = st === 'hover' ? ramp(GP.waxD, rgba('#E86B6C'), rgba('#B52A30'), GP.waxM, rgba('#D04548')) : st === 'pressed' ? ramp(GP.waxD, GP.waxM, rgba('#7C151A'), rgba('#5A0F13')) : GR.wax;
    cv.bevel(m, r, x, y, true);
    cv.ring(x + d / 2, y + d / 2, d / 2 - 3, d / 2 - 3, 1, st === 'pressed' ? GP.waxD : GP.waxM);
  },
  gem(cv, x, y, n, c) { // diamond n x n (odd)
    const m = new Cv(n, n), h = (n - 1) / 2; m.poly([[h + 0.5, 0], [n, h + 0.5], [h + 0.5, n], [0, h + 0.5]], 1);
    const lo = mix(c, 0xFF000000, 0.35), hi = mix(c, 0xFFFFFFFF, 0.45);
    cv.bevel(m, ramp(GP.brD, hi, c, lo), x, y); cv.px(x + h - 1, y + h - 1, 0xFFFFFFFF);
  },
  curl(cv, x, y, flipX, flipY, c) { // ink filigree corner, 11 x 11
    const b = new Cv(11, 11);
    b.bmp(['KKKKKKKKKK.', 'K.........K', 'K.KKKKKK..K', 'K.K....K..K', 'K.K.KK.K..K', 'K.K.K..K.K.', 'K.K.KKK.KK.', 'K.K........', 'K..KKKKK...', 'K.........', 'KKKKKKKK..'].map((r) => r.slice(0, 11)), { K: c });
    cv.blit(b, x, y, flipX, flipY);
  },
  plate(cv, x, y, w, h) { // name plate: brass-edged leather strip
    cv.bevel(rr(w, h, 2), GR.leather, x, y, true); cv.box(x + 2, y + 2, w - 4, h - 4, withA(GP.br, 200));
    for (const sx of [x + 3, x + w - 4]) { cv.px(sx, y + (h >> 1), GP.brHH); }
  },
  slot(cv, x, y, n, st) { // HUD slot, n x n: inked tile
    const sel = st === 'active';
    cv.bevel(rr(n, n, 2), sel ? ramp(GP.br, GP.brHH, GP.pa, GP.paS) : GR.page, x, y);
    cv.bevel(rr(n - 6, n - 6, 1), ramp(GP.bord, GP.paD, rgba('#2A1A18'), rgba('#3A2622')), x + 3, y + 3);
    if (sel) { cv.box(x - 1, y - 1, n + 2, n + 2, withA(GP.ready, 150)); cv.box(x - 2, y - 2, n + 4, n + 4, withA(GP.ready, 60)); }
  },
  keyseal(cv, x, y) { GK.seal(cv, x, y, 9, 'normal'); },
};
