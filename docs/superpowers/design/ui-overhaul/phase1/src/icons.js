// Shared icon set recipes: 16 (HUD), 32 (hub), 64 (large). Geometry is authored on a 32 grid and scaled by n/32;
// bevel stays 1 texel at every size, so a bigger icon gets finer detail, never fatter outlines. Light from the top-left.
const IR = {
  bone: ramp(rgba('#4A3A2A'), rgba('#FFFFF2'), rgba('#E9E0C8'), rgba('#B5A98A'), rgba('#FFFFFF'), rgba('#9A8E70')),
  blood: ramp(rgba('#3A0A10'), rgba('#FF6A60'), rgba('#C01830'), rgba('#7A0E1E'), rgba('#FF8F85'), rgba('#5E0A16')),
  gum: ramp(rgba('#2A0608'), rgba('#B02A3A'), rgba('#7A1224'), rgba('#4A0A16'), rgba('#9A2234'), rgba('#36070F')),
  gold: ramp(rgba('#4A3208'), rgba('#FFF0B0'), rgba('#E8B33A'), rgba('#A8761A'), rgba('#FFD86A'), rgba('#8A5E10')),
  moon: ramp(rgba('#4E3A44'), rgba('#FFFFFF'), rgba('#F2E6D0'), rgba('#C9B6A0'), rgba('#FFF8EA'), rgba('#AE9A86')),
  fur: ramp(rgba('#1E1A22'), rgba('#9A8E94'), rgba('#6A5A62'), rgba('#443840'), rgba('#7E7076'), rgba('#2E2630')),
  violet: ramp(rgba('#1E0F33'), rgba('#C9A6FF'), rgba('#8A5AD6'), rgba('#5A3A9A'), rgba('#A47EEA'), rgba('#3E2670')),
  night: ramp(rgba('#0E0820'), rgba('#3A2A5E'), rgba('#22163E'), rgba('#140C28'), rgba('#2E2250'), rgba('#0E0820')),
  teal: ramp(rgba('#0F3A44'), rgba('#D8FEFF'), rgba('#3FD0D6'), rgba('#1F8A94'), rgba('#8EEAEE'), rgba('#17707A')),
  paper: ramp(rgba('#5A3A1A'), rgba('#FFF6DC'), rgba('#E9DDB8'), rgba('#BBA874'), rgba('#FFFFFF'), rgba('#A08C5A')),
  paperD: ramp(rgba('#5A3A1A'), rgba('#E2D4A8'), rgba('#CDBB8A'), rgba('#9E8A58')),
};
const iconMask = (n, fn) => { const m = new Cv(n, n); fn(m, n / 32); return m; };
const paintIn = (cv, mask, n, fn, c) => { const t = new Cv(n, n); fn(t, n / 32); for (let i = 0; i < t.d.length; i++) if (t.d[i] && mask.d[i]) { cv.d[i] = c; } };
const thick = (cv, x0, y0, x1, y1, t, c) => { for (let k = 0; k < t; k++) cv.line(Math.round(x0 + k), Math.round(y0), Math.round(x1 + k), Math.round(y1), c); };
const ICON = {
  fang(n) {
    const cv = new Cv(n, n), s = n / 32, P = (a) => a.map(([x, y]) => [x * s, y * s]);
    const lo = n < 32, gh = lo ? 2 : 6 * s, gy = lo ? 2 : 4 * s;
    const gum = iconMask(n, (m) => { m.rect(Math.round(5 * s), Math.round(gy), Math.round(22 * s), Math.round(gh), 1); if (!lo) { m.disc(5 * s, 7 * s, 3 * s, 3 * s, 1); m.disc(27 * s, 7 * s, 3 * s, 3 * s, 1); } });
    const top = lo ? 4.5 : 9;
    const f1 = iconMask(n, (m) => { m.poly(P([[6.5, top * 2 - top], [16, top], [15.4, 17], [11.6, 28], [8, 17.5]]), 1); });
    const f2 = iconMask(n, (m) => { m.poly(P([[17.5, top], [26, top], [24.8, 16], [21.8, 25], [19, 17]]), 1); });
    cv.bevel(f1, IR.bone, 0, 0, n >= 32); cv.bevel(f2, IR.bone, 0, 0, n >= 32); cv.bevel(gum, IR.gum, 0, 0, n >= 32);
    if (n >= 32) { const d = iconMask(n, (m) => { m.disc(11.8 * s, 30 * s, 1.4 * s, 1.4 * s, 1); m.poly(P([[11.8, 28.4], [13, 29.8], [10.6, 29.8]]), 1); }); cv.bevel(d, IR.blood, 0, 0, false); }
    return cv;
  },
  drop(n) {
    const cv = new Cv(n, n), s = n / 32;
    const m = iconMask(n, (mm) => { mm.disc(16 * s, 20 * s, 9.5 * s, 9.5 * s, 1); mm.poly([[16 * s, 2.5 * s], [24.6 * s, 17 * s], [7.4 * s, 17 * s]], 1); mm.poly([[16 * s, 2.5 * s], [22.5 * s, 14 * s], [9.5 * s, 14 * s]], 1); });
    cv.bevel(m, IR.blood, 0, 0, n >= 32);
    if (n >= 32) { cv.disc(12.2 * s, 21 * s, 1.5 * s, 2.8 * s, withA(0xFFFFFFFF, 190)); cv.disc(13 * s, 15.5 * s, 0.8 * s, 1.2 * s, withA(0xFFFFFFFF, 140)); }
    else cv.px(Math.round(12 * s), Math.round(20 * s), withA(0xFFFFFFFF, 220));
    return cv;
  },
  moon(n) {
    const cv = new Cv(n, n), s = n / 32;
    const m = iconMask(n, (mm) => { mm.disc(14.5 * s, 16.5 * s, 11.5 * s, 11.5 * s, 1); const c = new Cv(n, n); c.disc(20.5 * s, 13.5 * s, 9.8 * s, 9.8 * s, 1); for (let i = 0; i < c.d.length; i++) if (c.d[i]) mm.d[i] = 0; });
    cv.bevel(m, IR.moon, 0, 0, n >= 32);
    const st = iconMask(n, (mm) => { const x = 24 * s, y = 9 * s, a = 3.4 * s, b = 0.9 * s; mm.poly([[x, y - a], [x + b, y - b], [x + a, y], [x + b, y + b], [x, y + a], [x - b, y + b], [x - a, y], [x - b, y - b]], 1); });
    cv.bevel(st, IR.gold, 0, 0, false);
    return cv;
  },
  paw(n) {
    const cv = new Cv(n, n), s = n / 32, D = (m, x, y, rx, ry) => m.disc(x * s, y * s, rx * s, ry * s, 1);
    const pad = iconMask(n, (m) => { D(m, 16, 23.2, 7.8, 5.6); D(m, 11, 19.6, 3.8, 3.4); D(m, 21, 19.6, 3.8, 3.4); D(m, 16, 18.6, 4.6, 3.2); });
    const toes = [[7, 13.6, 2.7, 3.5], [12.4, 8.6, 2.9, 3.7], [19.6, 8.6, 2.9, 3.7], [25, 13.6, 2.7, 3.5]];
    for (const t of toes) cv.bevel(iconMask(n, (m) => D(m, ...t)), IR.fur, 0, 0, n >= 32);
    cv.bevel(pad, IR.fur, 0, 0, n >= 32);
    if (n >= 32) for (const [x, y, rx, ry] of [[12.4, 8, 1, 1.3], [19.6, 8, 1, 1.3], [7, 13, 1, 1.2], [25, 13, 1, 1.2]]) cv.disc(x * s, y * s, rx * s, ry * s, withA(0xFFFFFFFF, 60));
    return cv;
  },
  sigil(n) {
    const cv = new Cv(n, n), s = n / 32, cx = 16 * s, cy = 16 * s;
    cv.bevel(iconMask(n, (m) => m.disc(cx, cy, 14 * s, 14 * s, 1)), IR.night, 0, 0, n >= 32);
    cv.ring(cx, cy, 11 * s, 11 * s, Math.max(1, Math.round(1.2 * s)), rgba('#8A5AD6'));
    if (n >= 32) cv.ring(cx, cy, 12.7 * s, 12.7 * s, 1, rgba('#3E2670'));
    const pts = []; for (let i = 0; i < 5; i++) { const a = -Math.PI / 2 + i * 2 * Math.PI / 5; pts.push([Math.round(cx + Math.cos(a) * 10 * s - 0.5), Math.round(cy + Math.sin(a) * 10 * s - 0.5)]); }
    for (let i = 0; i < 5; i++) { const [x0, y0] = pts[i], [x1, y1] = pts[(i + 2) % 5]; cv.line(x0, y0, x1, y1, rgba('#C9A6FF')); }
    for (const [x, y] of pts) { cv.px(x, y, 0xFFFFFFFF); if (n >= 32) { cv.px(x + 1, y, rgba('#8A5AD6')); cv.px(x, y + 1, rgba('#8A5AD6')); } }
    cv.px(Math.round(cx), Math.round(cy), rgba('#FFD36B')); if (n >= 32) { cv.px(Math.round(cx) - 1, Math.round(cy), rgba('#FFD36B')); cv.px(Math.round(cx), Math.round(cy) - 1, rgba('#FFD36B')); cv.px(Math.round(cx) - 1, Math.round(cy) - 1, rgba('#FFD36B')); }
    return cv;
  },
  gem(n) {
    const cv = new Cv(n, n), s = n / 32, w = Math.round(28 * s), h = Math.round(25 * s), ox = Math.round(2 * s), oy = Math.round(3.5 * s);
    const m = hexMask(w, h, Math.round(7 * s)); cv.bevel(m, IR.teal, ox, oy, n >= 32);
    if (n >= 32) {
      const mm = new Cv(n, n); mm.blit(m, ox, oy); const cx = 16 * s, cy = 16 * s;
      paintIn(cv, mm, n, (t) => t.poly([[ox + 3 * s, oy + 12 * s], [cx, cy], [cx - 4 * s, oy + 1 * s], [ox + 9 * s, oy + 1 * s]], 1), rgba('#8EEAEE'));
      paintIn(cv, mm, n, (t) => t.poly([[cx, cy], [ox + w - 2, oy + 12 * s], [ox + w - 8 * s, oy + h - 1], [cx, oy + h - 1]], 1), rgba('#2AA0A8'));
      paintIn(cv, mm, n, (t) => t.poly([[cx, cy], [cx + 5 * s, oy + 1 * s], [cx + 11 * s, oy + 1 * s], [ox + w - 3 * s, oy + 12 * s]], 1), rgba('#5ADCE2'));
      cv.line(Math.round(cx), Math.round(cy), Math.round(ox + 3 * s), Math.round(oy + 12 * s), rgba('#17707A')); cv.line(Math.round(cx), Math.round(cy), Math.round(cx), Math.round(oy + h - 2), rgba('#17707A')); cv.line(Math.round(cx), Math.round(cy), Math.round(ox + w - 3 * s), Math.round(oy + 12 * s), rgba('#17707A'));
      cv.px(Math.round(cx - 6 * s), Math.round(oy + 3 * s), 0xFFFFFFFF); cv.px(Math.round(cx - 6 * s) + 1, Math.round(oy + 3 * s), 0xFFFFFFFF);
    } else cv.px(Math.round(8 * s), Math.round(10 * s), 0xFFFFFFFF);
    return cv;
  },
  scroll(n) {
    const cv = new Cv(n, n), s = n / 32;
    cv.bevel(iconMask(n, (m) => m.rect(Math.round(8 * s), Math.round(6 * s), Math.round(16 * s), Math.round(20 * s), 1)), IR.paper, 0, 0, n >= 32);
    cv.bevel(iconMask(n, (m) => m.disc(16 * s, 6.4 * s, 10.5 * s, 3.2 * s, 1)), IR.paperD, 0, 0, n >= 32);
    cv.bevel(iconMask(n, (m) => m.disc(16 * s, 25.6 * s, 10.5 * s, 3.2 * s, 1)), IR.paperD, 0, 0, n >= 32);
    if (n >= 32) { for (const y of [11, 14.5, 18]) cv.hl(Math.round(11 * s), Math.round(y * s), Math.round((y === 18 ? 7 : 11) * s), rgba('#6A4A28')); cv.bevel(iconMask(n, (m) => m.disc(21.4 * s, 21.4 * s, 3.4 * s, 3.4 * s, 1)), IR.blood, 0, 0, false); }
    else { cv.hl(5, 7, 6, rgba('#6A4A28')); cv.hl(5, 9, 6, rgba('#6A4A28')); cv.px(10, 11, rgba('#C01830')); }
    return cv;
  },
  home(n) {
    const cv = new Cv(n, n), s = n / 32, P = (a) => a.map(([x, y]) => [x * s, y * s]);
    cv.bevel(iconMask(n, (m) => m.rect(Math.round(8 * s), Math.round(14 * s), Math.round(16 * s), Math.round(13 * s), 1)), IR.paper, 0, 0, n >= 32);
    cv.bevel(iconMask(n, (m) => m.poly(P([[3.5, 15], [16, 4.5], [28.5, 15], [26, 17], [16, 8.6], [6, 17]]), 1)), IR.blood, 0, 0, n >= 32);
    cv.bevel(iconMask(n, (m) => m.rect(Math.round(13.5 * s), Math.round(19 * s), Math.round(5 * s), Math.round(8 * s), 1)), IR.gum, 0, 0, false);
    return cv;
  },
  book(n) {
    const cv = new Cv(n, n), s = n / 32;
    cv.bevel(iconMask(n, (m) => m.rect(Math.round(6 * s), Math.round(5 * s), Math.round(20 * s), Math.round(21 * s), 1)), IR.gum, 0, 0, n >= 32);
    cv.bevel(iconMask(n, (m) => m.rect(Math.round(8 * s), Math.round(24 * s), Math.round(18 * s), Math.round(2.5 * s) + 1, 1)), IR.paper, 0, 0, false);
    cv.bevel(iconMask(n, (m) => m.poly([[16 * s, 9 * s], [20 * s, 13 * s], [16 * s, 17 * s], [12 * s, 13 * s]], 1)), IR.gold, 0, 0, false);
    if (n >= 32) cv.vl(Math.round(9 * s), Math.round(6 * s), Math.round(18 * s), rgba('#B02A3A'));
    return cv;
  },
  crown(n) {
    const cv = new Cv(n, n), s = n / 32, P = (a) => a.map(([x, y]) => [x * s, y * s]);
    cv.bevel(iconMask(n, (m) => m.poly(P([[6, 21], [5, 9], [11.5, 15], [16, 6], [20.5, 15], [27, 9], [26, 21]]), 1)), IR.gold, 0, 0, n >= 32);
    cv.bevel(iconMask(n, (m) => m.rect(Math.round(6 * s), Math.round(21 * s), Math.round(20 * s), Math.round(5 * s), 1)), IR.gold, 0, 0, n >= 32);
    for (const [x, y] of [[5, 8.4], [16, 5.2], [27, 8.4]]) cv.bevel(iconMask(n, (m) => m.disc(x * s, y * s, 1.9 * s, 1.9 * s, 1)), IR.gold, 0, 0, false);
    cv.bevel(iconMask(n, (m) => m.disc(16 * s, 23.4 * s, 1.9 * s, 1.9 * s, 1)), IR.blood, 0, 0, false);
    if (n >= 32) { for (const x of [10, 22]) cv.px(Math.round(x * s), Math.round(23.4 * s), rgba('#FFF0B0')); }
    return cv;
  },
};
