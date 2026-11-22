// Logo directions: pixel wordmark + emblem, 128 px mod icon, silhouette test, lockup, mod list row.
const FX = 0, FY = 10400;
const fname = 'Logo · three directions';
for (const n of PAGE.children.filter((c) => c.name === fname)) n.remove();
const fr = sheet(fname, FX, FY, 2400, 940, rgba('#101014'));
const G57 = {
  A: ['.###.', '#...#', '#...#', '#####', '#...#', '#...#', '#...#'], B: ['####.', '#...#', '#...#', '####.', '#...#', '#...#', '####.'], D: ['###..', '#..#.', '#...#', '#...#', '#...#', '#..#.', '###..'],
  E: ['#####', '#....', '#....', '####.', '#....', '#....', '#####'], I: ['.###.', '..#..', '..#..', '..#..', '..#..', '..#..', '.###.'], L: ['#....', '#....', '#....', '#....', '#....', '#....', '#####'],
  N: ['#...#', '##..#', '##..#', '#.#.#', '#..##', '#..##', '#...#'], O: ['.###.', '#...#', '#...#', '#...#', '#...#', '#...#', '.###.'], P: ['####.', '#...#', '#...#', '####.', '#....', '#....', '#....'],
  R: ['####.', '#...#', '#...#', '####.', '#.#..', '#..#.', '#...#'], S: ['.####', '#....', '#....', '.###.', '....#', '....#', '####.'], T: ['#####', '..#..', '..#..', '..#..', '..#..', '..#..', '..#..'],
  U: ['#...#', '#...#', '#...#', '#...#', '#...#', '#...#', '.###.'],
};
const bits = (ch) => G57[ch].map((r) => [...r].map((c) => (c === '#' ? 1 : 0)));
const scale2x = (b) => {
  const h = b.length, w = b[0].length, o = []; for (let y = 0; y < h * 2; y++) o.push(new Array(w * 2).fill(0));
  const g = (x, y) => (x < 0 || y < 0 || x >= w || y >= h ? 0 : b[y][x]);
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
    const P = g(x, y), A = g(x, y - 1), Bn = g(x + 1, y), C = g(x - 1, y), D = g(x, y + 1); let p1 = P, p2 = P, p3 = P, p4 = P;
    if (C === A && C !== D && A !== Bn) p1 = A; if (A === Bn && A !== C && Bn !== D) p2 = Bn; if (D === C && D !== Bn && C !== A) p3 = C; if (Bn === D && Bn !== A && D !== C) p4 = D;
    o[2 * y][2 * x] = p1; o[2 * y][2 * x + 1] = p2; o[2 * y + 1][2 * x] = p3; o[2 * y + 1][2 * x + 1] = p4;
  }
  return o;
};
// mask of a word. big: scale2x + one px horizontal thickening (11 x 14 letters), small: plain 5 x 7
const wordMask = (text, big, gap) => {
  const letters = [...text].map((ch) => (ch === ' ' ? null : big ? scale2x(bits(ch)).map((r) => r.map((v, i, a) => (v || a[i - 1] ? 1 : 0))) : bits(ch)));
  const lw = big ? 11 : 5, lh = big ? 14 : 7, chars = [...text], xs = []; let cx = 0;
  chars.forEach((ch, i) => { if (i && chars[i - 1] === 'L' && ch === 'I') cx -= big ? 3 : 1; xs.push(cx); cx += lw + gap; });
  const W = cx - gap, m = new Cv(W, lh);
  letters.forEach((b, i) => { if (!b) return; b.forEach((r, y) => r.forEach((v, x) => { if (v) m.d[y * W + xs[i] + x] = 1; })); });
  return m;
};
const styleWord = (m, bands, outlineC, shadowC) => {
  const f = new Cv(m.w + 2, m.h + 2); f.stamp(1, 1, m, 1);
  const body = new Cv(m.w + 2, m.h + 2);
  for (let y = 0; y < m.h; y++) for (let x = 0; x < m.w; x++) if (m.d[y * m.w + x]) { const b = bands[Math.min(bands.length - 1, Math.floor(y * bands.length / m.h))]; body.px(x + 1, y + 1, b); }
  body.outline(outlineC);
  const out = new Cv(m.w + 6, m.h + 6);
  const sh = new Cv(m.w + 2, m.h + 2); for (let i = 0; i < body.d.length; i++) if (body.d[i]) sh.d[i] = shadowC;
  out.blit(sh, 1, 3); out.blit(body, 0, 0);
  return out;
};
const wordmark = (bands, outlineC, shadowC) => {
  const top = styleWord(wordMask('SUPERNATURAL', false, 3), [bands[1], bands[2]], outlineC, shadowC), big = styleWord(wordMask('BLOODLINES', true, 2), bands, outlineC, shadowC);
  const w = Math.max(top.w, big.w), c = new Cv(w, top.h + big.h + 1);
  c.blit(top, Math.round((w - top.w) / 2), 0); c.blit(big, Math.round((w - big.w) / 2), top.h + 1); return c;
};
// ---- emblems, 56 x 56 ----
const emblemA = () => {
  const c = new Cv(56, 56);
  c.bevel(discMask(56), ramp(GP.brD, GP.brHH, GP.br, GP.brS, GP.brH, GP.brS), 0, 0, true);
  for (let i = 0; i < 16; i++) { const a = i / 16 * 2 * Math.PI; c.rect(Math.round(28 + Math.cos(a) * 25.5 - 1), Math.round(28 + Math.sin(a) * 25.5 - 1), 2, 2, GP.brD); }
  c.bevel(discMask(46), ramp(GP.waxD, GP.waxH, GP.wax, GP.waxM, rgba('#B53539'), rgba('#7C151A')), 5, 5, true);
  c.ring(28, 28, 19, 19, 1, GP.waxM);
  c.blit(ICON.moon(34), 11, 8); c.blit(ICON.fang(22), 17, 28);
  return c;
};
const emblemB = () => {
  const c = new Cv(56, 56);
  MK.halo(c, 28, 28, 22, MP.glow, 3);
  c.bevel(discMask(50), ramp(MP.red, MP.edgeH, rgba('#12060A'), MP.void), 3, 3, false); c.ring(28, 28, 24, 24, 1, MP.red);
  c.blit(ICON.drop(46), 5, 6); c.blit(ICON.moon(22), 17, 21);
  return c;
};
const emblemC = () => {
  const c = new Cv(56, 56);
  c.bevel(hexMask(56, 50, 13), ramp(TP.tealD, TP.tealH, TP.teal, rgba('#2AA0A8'), rgba('#7CE4E8'), rgba('#2E8E96')), 0, 3, true);
  c.bevel(hexMask(48, 42, 11), ramp(0, 0, rgba('#0A1827'), 0), 4, 7);
  const mono = styleWord(wordMask('SB', true, 2), [rgba('#E4FEFF'), rgba('#A8F0F3'), TP.teal, rgba('#2AA0A8')], rgba('#06121F'), rgba('#06222A'));
  c.blit(mono, (56 - mono.w) / 2 | 0, 12); c.blit(ICON.fang(16), 20, 33);
  return c;
};
const DIRS = [
  { key: 'A', name: 'A · Seal', desc: 'A wax-and-brass seal: the crescent over the fang. Reads as the Grimoire first, works on any theme. Warm, formal, old-world.', emb: emblemA, tile: GP.leD, tileHi: GP.leM, bands: [rgba('#FFF0B0'), rgba('#F0CF7A'), rgba('#D8A83E'), rgba('#9A6E1E')], outline: rgba('#2A1606'), shadow: rgba('#12080A'), pal: [['Brass', GP.br], ['Wax', GP.wax], ['Leather', GP.le], ['Cream', GP.paH]] },
  { key: 'B', name: 'B · Drop', desc: 'A blood drop with the moon cut into it, inside a red corona. The simplest silhouette of the three; the strongest at 32 px. Cinematic, cold.', emb: emblemB, tile: rgba('#12060A'), tileHi: MP.edge, bands: [rgba('#FFF1EA'), rgba('#FFD9CF'), rgba('#F6B1A5'), rgba('#E8786A')], outline: rgba('#2A0B14'), shadow: rgba('#8A211D'), pal: [['Blood', MP.red], ['Moon', MP.moon], ['Corona', MP.glow], ['Void', MP.void]] },
  { key: 'C', name: 'C · Glyph', desc: 'A hex badge with an SB monogram and a fang: the status-window look. The most game-like and the most modern; the least "gothic".', emb: emblemC, tile: rgba('#0A1827'), tileHi: TP.edge, bands: [rgba('#E4FEFF'), rgba('#A8F0F3'), TP.teal, rgba('#2AA0A8')], outline: rgba('#06121F'), shadow: rgba('#0F3A44'), pal: [['Teal', TP.teal], ['Teal light', TP.tealH], ['Panel', TP.panel], ['Gold', TP.gold]] },
];
await label(fr, 'Logo directions for "Supernatural Bloodlines"', 60, 28, 0xFFFFFFFF, 36, true);
await label(fr, 'Pixel-art, 1 texel = 1 GUI px. The mod icon is a 64 × 64 texel tile shown at 128 × 128 (an exact 2×); every part is hand-placed pixels, no gradients, no scaling by fractions. Pick one in the owner review; the other themes recolour it.', 60, 76, 0xFF9A9AA6, 15);
DIRS.forEach((d, i) => {
  const x0 = 60 + i * 780, y0 = 130; box(fr, x0 - 20, y0 - 10, 760, 800, rgba('#17171C'));
  label(fr, d.name, x0, y0, 0xFFFFFFFF, 28, true); label(fr, d.desc, x0, y0 + 40, 0xFF9A9AA6, 14, false, 720);
  const emb = d.emb();
  // mod icon tile 64 x 64
  const tile = new Cv(64, 64); tile.bevel(rr(64, 64, 4), ramp(d.tileHi, d.tileHi, d.tile, d.tile), 0, 0); tile.blit(emb, 4, 4);
  tile.emit(fr, x0 / 2, (y0 + 110) / 2, 2, 'mod icon 128 ' + d.key);
  tile.emit(fr, (x0 + 160), (y0 + 174), 1, 'mod icon 64 ' + d.key);
  label(fr, 'mod icon 128 × 128 (2×)', x0, y0 + 244, 0xFF8A8A98, 13); label(fr, '64 × 64 (1×)', x0 + 160, y0 + 244, 0xFF8A8A98, 13);
  // silhouette tests
  const gray = (c) => { if (!c) return 0; const g = Math.round(lum(c) ** 0.45 * 255); return pack(g, g, g); };
  const emSil = new Cv(56, 56); for (let k = 0; k < emSil.d.length; k++) emSil.d[k] = gray(emb.d[k]);
  emSil.emit(fr, (x0 + 280) / 2, (y0 + 110) / 2, 2, 'value check ' + d.key);
  const half = new Cv(28, 28); for (let y = 0; y < 28; y++) for (let x = 0; x < 28; x++) half.d[y * 28 + x] = emSil.d[(2 * y) * 56 + 2 * x];
  half.emit(fr, x0 + 420, y0 + 174, 2, 'value check half ' + d.key);
  label(fr, 'value check: greyscale at 2× and at half size', x0 + 280, y0 + 244, 0xFF8A8A98, 13);
  // lockup
  box(fr, x0, y0 + 280, 720, 250, d.tile === GP.leD ? rgba('#2A0E0E') : d.key === 'B' ? rgba('#12060A') : rgba('#0A1827'));
  const wm = wordmark(d.bands, d.outline, d.shadow), lk = new Cv(56 + 6 + wm.w, 56); lk.blit(emb, 0, 0); lk.blit(wm, 62, Math.round((56 - wm.h) / 2));
  lk.emit(fr, (x0 + 20) / 3, (y0 + 304) / 3, 3, 'lockup ' + d.key);
  label(fr, 'lockup (3×): emblem + wordmark. 5 × 7 pixel letters, Scale2x-smoothed, banded, 1 px outline, 2 px drop.', x0, y0 + 536, 0xFF8A8A98, 13, false, 720);
  // mod list row
  box(fr, x0, y0 + 566, 720, 90, rgba('#26262E'));
  tile.emit(fr, x0 + 12, y0 + 578, 1, 'row icon ' + d.key);
  label(fr, 'Supernatural Bloodlines', x0 + 92, y0 + 582, 0xFFFFFFFF, 20, true); label(fr, 'Version 1.0 · Vampires, werewolves and the bloodlines between', x0 + 92, y0 + 612, 0xFF9A9AA6, 14);
  label(fr, 'mod list row at real size', x0, y0 + 662, 0xFF8A8A98, 13);
  d.pal.forEach(([nm, c], k) => { box(fr, x0 + k * 180, y0 + 706, 168, 40, c); label(fr, nm + ' ' + hex6(c), x0 + k * 180, y0 + 752, 0xFF9A9AA6, 12); });
});
return done({ id: fr.id });
