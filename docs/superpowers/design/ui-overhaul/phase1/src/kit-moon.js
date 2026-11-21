// Blood Moon kit: night sky, obsidian cards with blood-red hairlines, pills, a pixel-banded moon. 1 texel = 1 GUI px.
const MP = {
  void: rgba('#07040A'), sky: rgba('#2A0B14'), card: rgba('#1D0B13'), cardH: rgba('#2E1520'), cardD: rgba('#10060A'), edge: rgba('#5E2A38'), edgeH: rgba('#8A4050'),
  red: rgba('#BF332B'), redH: rgba('#E8574C'), redD: rgba('#8A211D'), redDD: rgba('#4A1012'), hot: rgba('#FF7A6A'),
  moon: rgba('#FFF1EA'), moon2: rgba('#FFD9CF'), moon3: rgba('#F6B1A5'), moon4: rgba('#E8786A'), moon5: rgba('#C8423A'), moon6: rgba('#8A2424'),
  gold: rgba('#FFD36B'), goldD: rgba('#B8862A'), text: rgba('#FFF1EA'), mut: rgba('#C9A7A5'), good: rgba('#6FE08A'),
  focusD: rgba('#07040A'), focusL: rgba('#FFD36B'), glow: rgba('#FF5A4A'), ready: rgba('#FFD36B'),
};
const MR = {
  card: ramp(MP.edge, MP.cardH, MP.card, MP.cardD),
  inset: ramp(MP.void, MP.void, MP.cardD, rgba('#241119')),
  pri: ramp(MP.redDD, MP.redH, MP.red, MP.redD, rgba('#D14438'), rgba('#A32A24')),
  priHot: ramp(MP.hot, rgba('#FF9588'), rgba('#D9453B'), MP.redD, rgba('#EE6A5E'), rgba('#B33028')),
  priDn: ramp(MP.redDD, MP.redD, rgba('#8E2420'), rgba('#BF332B')),
  priOff: ramp(rgba('#3A2229'), rgba('#2A1920'), rgba('#22121A'), rgba('#1C0E14')),
  sec: ramp(MP.edgeH, MP.cardH, rgba('#22111A'), MP.cardD),
  secHot: ramp(MP.hot, rgba('#3A1A24'), rgba('#2E1620'), MP.cardD),
  secDn: ramp(MP.edge, MP.void, MP.cardD, rgba('#241119')),
  secOff: ramp(rgba('#2E1A20'), rgba('#1C0E14'), rgba('#170A10'), rgba('#12070C')),
  chip: ramp(MP.edge, MP.cardH, MP.card, MP.cardD),
  chipOn: ramp(MP.redDD, MP.redH, MP.red, MP.redD),
  gold: ramp(rgba('#5A3A10'), rgba('#FFF0B0'), MP.gold, MP.goldD),
};
const moonBands = [MP.moon, MP.moon2, MP.moon3, MP.moon4, MP.moon5, MP.moon6];
const MK = {
  id: 'moon', name: 'Blood Moon', paper: MP.card, ink: MP.text, muted: MP.mut, accent: MP.red, good: MP.good, dark: MP.void, glow: MP.glow, ready: MP.gold,
  focusD: MP.focusD, focusL: MP.focusL,
  panel(cv, x, y, w, h) {
    cv.bevel(rr(w, h, 2), MR.card, x, y);
    cv.hl(x + 5, y, 9, MP.red); cv.hl(x + 5, y + 1, 9, withA(MP.red, 70)); // the blood-line tick on the top edge
  },
  inset(cv, x, y, w, h) { cv.bevel(rr(w, h, 1), MR.inset, x, y); },
  divider(cv, x, y, w) {
    cv.hl(x, y, w, MP.edge); const cx = x + (w >> 1);
    cv.rect(cx - 8, y - 3, 16, 7, MP.card);
    const c = new Cv(7, 7); c.disc(3.5, 3.5, 3.5, 3.5, 1); const cut = new Cv(7, 7); cut.disc(5, 3, 3, 3, 1);
    for (let i = 0; i < 49; i++) if (cut.d[i]) c.d[i] = 0;
    cv.bevel(c, ramp(0, MP.moon, MP.moon3, MP.moon5), cx - 3, y - 3);
    cv.px(cx - 7, y, MP.red); cv.px(cx + 6, y, MP.red);
  },
  scroll(cv, x, y, h, pos, th) {
    cv.bevel(rr(3, h, 1), ramp(MP.edge, MP.void, MP.cardD, MP.cardH), x + 1, y);
    const ty = y + 1 + Math.round(pos * (h - 2 - th)); cv.bevel(rr(5, th, 2), MR.pri, x, ty); cv.px(x + 2, ty + 2, MP.moon);
  },
  button(cv, x, y, w, h, st, pri) {
    const m = pill(w, h);
    let r = pri ? MR.pri : MR.sec;
    if (st === 'hover') r = pri ? MR.priHot : MR.secHot; if (st === 'pressed') r = pri ? MR.priDn : MR.secDn; if (st === 'disabled') r = pri ? MR.priOff : MR.secOff;
    if (st === 'hover') { cv.bevel(pill(w + 2, h + 2), ramp(0, withA(MP.glow, 60), withA(MP.glow, 60), withA(MP.glow, 60)), x - 1, y - 1); }
    cv.bevel(m, r, x, y, pri && st !== 'disabled');
    if (!pri && st !== 'disabled') { cv.hl(x + (h >> 1), y, w - h, st === 'hover' ? MP.hot : MP.red); }
    return { dy: st === 'pressed' ? 1 : 0, text: st === 'disabled' ? rgba('#8A7071') : MP.text };
  },
  tab(cv, x, y, w, h, st) {
    const sel = st === 'selected', r = sel ? MR.chipOn : st === 'hover' ? ramp(MP.red, MP.cardH, rgba('#2E1620'), MP.cardD) : st === 'disabled' ? ramp(rgba('#2E1A20'), rgba('#1C0E14'), rgba('#170A10'), rgba('#12070C')) : MR.chip;
    cv.bevel(pill(w, h), r, x, y, sel);
  },
  bar(cv, x, y, w, h, f) {
    cv.bevel(pill(w, h), ramp(MP.void, MP.cardD, MP.cardD, MP.cardH), x, y);
    const fw = Math.max(h - 2, Math.round((w - 2) * f)); if (f > 0) cv.bevel(pill(fw, h - 2), MR.pri, x + 1, y + 1);
  },
  focus(cv, x, y, w, h) { cv.box(x - 2, y - 2, w + 4, h + 4, MP.focusD); cv.box(x - 1, y - 1, w + 2, h + 2, MP.focusL); },
  cover(cv, x, y, w, h) { MK.sky(cv, x, y, w, h); },
  sky(cv, x, y, w, h) { // banded night: 8 flat bands, dithered at every seam, a few stars
    const cols = [rgba('#2E0C16'), rgba('#270A13'), rgba('#20080F'), rgba('#19060C'), rgba('#130509'), rgba('#0E0408'), rgba('#0A0307'), MP.void];
    const bh = Math.ceil(h / cols.length);
    cols.forEach((c, i) => { cv.rect(x, y + i * bh, w, bh, c); if (i) { cv.dash(x, y + i * bh, w, cols[i - 1], 3, 3); cv.dash(x + 3, y + i * bh + 1, w - 3, cols[i - 1], 3, 3); } });
    const r = rng(77); for (let i = 0; i < Math.floor(w * h / 900); i++) { const sx = x + Math.floor(r() * w), sy = y + Math.floor(r() * h * 0.7); cv.px(sx, sy, r() < 0.3 ? withA(MP.moon3, 200) : withA(MP.moon, 90)); }
  },
  halo(cv, cx, cy, r, c, steps = 4) { // stepped glow: concentric alpha bands, no smooth gradient
    for (let i = steps; i >= 1; i--) cv.disc(cx, cy, r + i * 3, r + i * 3, withA(c, Math.round(46 / i)));
  },
  moon(cv, cx, cy, d) {
    const r = d / 2, rg = rng(5);
    for (let j = 0; j < d; j++) for (let i = 0; i < d; i++) {
      const dx = (i + 0.5 - r) / r, dy = (j + 0.5 - r) / r; if (dx * dx + dy * dy > 1) continue;
      const lx = dx + 0.28, ly = dy + 0.28, t = Math.min(0.999, Math.sqrt(lx * lx + ly * ly) / 1.25), band = t * (moonBands.length - 1), b0 = Math.floor(band), fr = band - b0;
      const bayer = [[0, 2], [3, 1]][j & 1][i & 1] / 4 + 0.125; // 2x2 ordered dither at the seam
      cv.px(Math.round(cx - r) + i, Math.round(cy - r) + j, moonBands[Math.min(moonBands.length - 1, b0 + (fr < 0.38 ? 0 : fr > 0.62 ? 1 : (fr > bayer ? 1 : 0)))]);
    }
    for (const [ax, ay, ar] of [[-0.35, -0.2, 0.17], [0.3, -0.35, 0.1], [0.12, 0.3, 0.2], [-0.4, 0.35, 0.09], [0.5, 0.15, 0.08]]) {
      const px = cx + ax * r, py = cy + ay * r, rad = ar * r; cv.disc(px, py, rad, rad * 0.9, withA(MP.moon6, 150)); cv.disc(px - 0.6, py - 0.6, rad * 0.62, rad * 0.55, withA(MP.moon4, 130));
    }
  },
  seal(cv, x, y, d, st) { // round rail button: ring + crescent
    const on = st === 'hover' || st === 'selected';
    cv.bevel(discMask(d), ramp(on ? MP.hot : MP.edge, MP.cardH, MP.card, MP.cardD), x, y);
    cv.ring(x + d / 2, y + d / 2, d / 2 - 3, d / 2 - 3, 1, on ? MP.red : MP.edge);
  },
  gem(cv, x, y, n, c) { cv.disc(x + n / 2, y + n / 2, n / 2 + 1.5, n / 2 + 1.5, withA(c, 70)); const m = discMask(n); cv.bevel(m, ramp(MP.redDD, mix(c, 0xFFFFFFFF, 0.5), c, mix(c, 0xFF000000, 0.4)), x, y); cv.px(x + 2, y + 2, 0xFFFFFFFF); },
  curl(cv, x, y, flipX, flipY, c) { const b = new Cv(9, 9); b.bmp(['K.....K..', '.K...K...', '..K.K....', '...K.....', '..K.K....', '.K...K...', 'K.....K..'], { K: c }); cv.blit(b, x, y, flipX, flipY); },
  plate(cv, x, y, w, h) { cv.bevel(pill(w, h), ramp(MP.edgeH, MP.cardH, MP.card, MP.cardD), x, y); cv.hl(x + (h >> 1), y, w - h, MP.red); },
  slot(cv, x, y, n, st) {
    const sel = st === 'active';
    if (sel) cv.disc(x + n / 2, y + n / 2, n / 2 + 3, n / 2 + 3, withA(MP.glow, 60));
    cv.bevel(discMask(n), ramp(sel ? MP.hot : MP.edge, MP.edgeH, MP.card, MP.cardD), x, y);
    cv.disc(x + n / 2, y + n / 2, n / 2 - 3, n / 2 - 3, MP.cardD);
  },
  keyseal(cv, x, y) { cv.bevel(discMask(9), ramp(MP.redDD, MP.hot, MP.red, MP.redD), x, y); },
};
