// Shared mockup pieces: the world behind the HUD, vanilla bars, faces, wolves. 960 x 540 texels at GUI scale 2 = 1920 x 1080.
const SW = 960, SH = 540;
const KF = { grimoire: GK, moon: MK, tensura: TK };
const SKIN = { fair: [rgba('#E8B898'), rgba('#D0997A'), rgba('#B27C5E')], tan: [rgba('#D9A07A'), rgba('#BC8460'), rgba('#9A6848')], deep: [rgba('#A8704C'), rgba('#8E5A3C'), rgba('#744830')] };
const HAIR = { brown: [rgba('#6A4426'), rgba('#4C2E18')], black: [rgba('#2A2226'), rgba('#161216')], blond: [rgba('#D8B060'), rgba('#B08A40')], grey: [rgba('#B8B8BC'), rgba('#8A8A92')], red: [rgba('#B04A28'), rgba('#7E3018')] };
const face = (cv, x, y, o = {}) => { // 16 x 16 head, flat Minecraft-style, top-left light
  const [s1, s2, s3] = SKIN[o.skin || 'fair'], [h1, h2] = HAIR[o.hair || 'brown'];
  cv.rect(x, y, 16, 16, s1); cv.rect(x, y + 13, 16, 3, s2); cv.vl(x + 15, y, 16, s2); cv.hl(x, y, 16, h1);
  cv.rect(x, y, 16, 4, h1); cv.rect(x, y + 4, 3, 5, h1); cv.rect(x + 13, y + 4, 3, 5, h1); cv.rect(x + 3, y + 4, 10, 1, h2);
  if (o.long) { cv.rect(x, y + 9, 2, 6, h1); cv.rect(x + 14, y + 9, 2, 6, h1); }
  cv.hl(x, y + 3, 16, h2); cv.rect(x + 3, y + 7, 4, 1, h2); cv.rect(x + 9, y + 7, 4, 1, h2);
  cv.rect(x + 3, y + 8, 2, 2, 0xFFFFFFFF); cv.rect(x + 5, y + 8, 2, 2, o.eye || rgba('#3A4A9A')); cv.rect(x + 9, y + 8, 2, 2, o.eye || rgba('#3A4A9A')); cv.rect(x + 11, y + 8, 2, 2, 0xFFFFFFFF);
  cv.rect(x + 7, y + 10, 2, 2, s3); cv.rect(x + 5, y + 12, 6, 1, o.mouth || rgba('#9A5848'));
  if (o.beard) { cv.rect(x + 2, y + 11, 12, 4, withA(h2, 200)); cv.rect(x + 5, y + 12, 6, 1, rgba('#9A5848')); }
  if (o.glasses) { cv.rect(x + 2, y + 7, 12, 4, withA(0xFF101018, 235)); cv.hl(x + 2, y + 8, 12, withA(0xFFFFFFFF, 40)); }
  return cv;
};
const wolf = (cv, x, y, o = {}) => { // 24 x 24 front-facing wolf head
  const fur = o.fur || [rgba('#2C2A32'), rgba('#8A8890'), rgba('#5A5860'), rgba('#3E3C46'), rgba('#17161C')], eye = o.eye || rgba('#FFD36B');
  const m = new Cv(24, 24);
  m.poly([[2, 1], [10, 8], [4, 12]], 1); m.poly([[22, 1], [14, 8], [20, 12]], 1); m.disc(12, 13, 9.4, 8, 1); m.poly([[8, 17], [16, 17], [15, 23], [9, 23]], 1);
  cv.bevel(m, ramp(fur[4], fur[1], fur[2], fur[3]), x, y, true);
  const sn = new Cv(24, 24); sn.poly([[8.5, 15], [15.5, 15], [14.5, 22.5], [9.5, 22.5]], 1);
  for (let j = 0; j < 24; j++) for (let i = 0; i < 24; i++) if (sn.d[j * 24 + i]) cv.px(x + i, y + j, o.muzzle || mix(fur[2], 0xFFFFFFFF, 0.28));
  cv.poly([[x + 3, y + 3], [x + 8, y + 7.5], [x + 4.5, y + 10]], fur[4]); cv.poly([[x + 21, y + 3], [x + 16, y + 7.5], [x + 19.5, y + 10]], fur[4]);
  cv.rect(x + 10, y + 16, 4, 2, 0xFF0A0A0E); cv.px(x + 10, y + 16, 0xFF5A5A66);
  cv.rect(x + 6, y + 11, 4, 2, 0xFF0A0A0E); cv.rect(x + 14, y + 11, 4, 2, 0xFF0A0A0E); cv.rect(x + 7, y + 11, 2, 2, eye); cv.rect(x + 15, y + 11, 2, 2, eye); cv.px(x + 7, y + 11, 0xFFFFFFFF); cv.px(x + 15, y + 11, 0xFFFFFFFF);
  cv.px(x + 10, y + 21, 0xFFF4F0E0); cv.px(x + 10, y + 22, 0xFFF4F0E0); cv.px(x + 13, y + 21, 0xFFF4F0E0); cv.px(x + 13, y + 22, 0xFFF4F0E0);
  cv.hl(x + 11, y + 18, 2, 0xFF0A0A0E);
  return cv;
};
const figure = (cv, x, y, o = {}) => { // 14 x 30 blocky player seen from the front
  const [s1, s2] = SKIN.fair, [h1, h2] = HAIR[o.hair || 'brown'], sh = o.shirt || [rgba('#6E5238'), rgba('#523C28')], pa = o.pants || [rgba('#2E3A66'), rgba('#222B4C')];
  cv.rect(x + 3, y, 8, 8, s1); cv.rect(x + 3, y, 8, 3, h1); cv.hl(x + 3, y + 3, 8, h2); cv.rect(x + 4, y + 5, 2, 1, 0xFFFFFFFF); cv.rect(x + 8, y + 5, 2, 1, 0xFFFFFFFF); cv.px(x + 5, y + 5, o.eye || rgba('#3A4A9A')); cv.px(x + 8, y + 5, o.eye || rgba('#3A4A9A')); cv.rect(x + 3, y + 7, 8, 1, s2);
  cv.rect(x + 3, y + 8, 8, 12, sh[0]); cv.vl(x + 10, y + 8, 12, sh[1]); cv.rect(x, y + 8, 3, 12, sh[0]); cv.rect(x + 11, y + 8, 3, 12, sh[1]); cv.rect(x, y + 18, 3, 2, s1); cv.rect(x + 11, y + 18, 3, 2, s2);
  cv.rect(x + 3, y + 20, 4, 10, pa[0]); cv.rect(x + 7, y + 20, 4, 10, pa[1]); cv.rect(x + 3, y + 28, 8, 2, rgba('#3A3A44'));
  return cv;
};
const HEART = ['.........', '.KK...KK.', 'KRRK.KRRK', 'KRWRKRRRK', 'KRRRRRRRK', '.KRRRRRK.', '..KRRRK..', '...KRK...', '....K....'];
const SHANK = ['.....KKK.', '....KOOOK', '...KOOHOK', '..KOOOOK.', '.KOOOOK..', 'KBBOOK...', 'KBBK.....', '.KK......', '.........'];
const world = (cv) => {
  const sky = [rgba('#6FA0E4'), rgba('#7AAAE8'), rgba('#86B4EC'), rgba('#92BDEF'), rgba('#9EC6F2'), rgba('#AACFF5')];
  const bh = 78; sky.forEach((c, i) => { cv.rect(0, i * bh, SW, bh, c); if (i) { cv.dash(0, i * bh, SW, sky[i - 1], 4, 4); cv.dash(4, i * bh + 1, SW - 4, sky[i - 1], 4, 4); } });
  const r = rng(3); for (const [cx, cy, cw] of [[80, 60, 70], [300, 40, 90], [620, 70, 80], [820, 38, 70], [480, 110, 60]]) { cv.rect(cx, cy, cw, 10, withA(0xFFFFFFFF, 190)); cv.rect(cx + 10, cy - 8, cw - 26, 8, withA(0xFFFFFFFF, 190)); cv.rect(cx + 18, cy + 10, cw - 30, 6, withA(0xFFFFFFFF, 150)); }
  const hill = (base, col, amp, seed) => { const g = rng(seed); let h = amp; for (let x = 0; x < SW; x += 16) { h = Math.max(6, Math.min(amp * 2, h + Math.round((g() - 0.5) * 14))); cv.rect(x, base - h, 16, h + 2, col); } };
  hill(380, rgba('#7C9DB8'), 36, 9); hill(398, rgba('#5E8E6E'), 26, 21);
  cv.rect(0, 410, SW, SH - 410, rgba('#5C9A3C')); cv.rect(0, 410, SW, 3, rgba('#82BE52')); cv.rect(0, 413, SW, 2, rgba('#4C8230'));
  for (let i = 0; i < 160; i++) { const x = Math.floor(r() * SW), y = 418 + Math.floor(r() * (SH - 418)); cv.rect(x, y, 2, 1, r() < 0.5 ? rgba('#4C8230') : rgba('#6BAE46')); }
  cv.rect(740, 330, 8, 80, rgba('#6A4A2A')); cv.vl(745, 330, 80, rgba('#4C3418')); cv.rect(712, 270, 64, 36, rgba('#3F7A2E')); cv.rect(700, 290, 88, 36, rgba('#4A8A36')); cv.rect(718, 252, 52, 22, rgba('#4A8A36'));
  for (let i = 0; i < 70; i++) { const x = 700 + Math.floor(r() * 88), y = 252 + Math.floor(r() * 74); cv.rect(x, y, 3, 2, r() < 0.5 ? rgba('#5BA044') : rgba('#356A26')); }
};
const vanillaBars = (cv, sel = 0) => { // hearts, hunger, xp, hotbar (vanilla look)
  const x0 = 389, y0 = 518;
  cv.rect(x0, y0, 182, 22, rgba('#8B8B8B')); cv.rect(x0 + 1, y0 + 1, 180, 20, rgba('#2A2A2A')); cv.box(x0, y0, 182, 22, rgba('#1A1A1A'));
  for (let i = 1; i < 9; i++) cv.vl(x0 + i * 20, y0 + 1, 20, rgba('#5A5A5A'));
  cv.box(x0 - 1 + sel * 20, y0 - 1, 24, 24, 0xFFFFFFFF); cv.box(x0 + sel * 20, y0, 22, 22, rgba('#9A9A9A'));
  for (let i = 0; i < 10; i++) { cv.bmp(HEART, { K: rgba('#3A0808'), R: rgba('#E0302A'), W: rgba('#FF9A90') }, x0 + i * 8, 498); cv.bmp(SHANK, { K: rgba('#3A2208'), O: rgba('#C8782A'), H: rgba('#F0B060'), B: rgba('#E8E0D0') }, x0 + 182 - 9 - i * 8, 498); }
  cv.rect(x0, 510, 182, 5, rgba('#1E1E1E')); cv.rect(x0 + 1, 511, 120, 3, rgba('#7EFC20')); cv.hl(x0 + 1, 511, 120, rgba('#C8FFA0'));
};
const HCOL = {
  grimoire: { text: GP.ink, mut: GP.mut, head: GP.red, plateText: GP.paH, fill: GP.redH, good: GP.good, warn: GP.red, count: GP.red, onPri: GP.paH, onSec: GP.ink, link: GP.red },
  moon: { text: MP.text, mut: MP.mut, head: MP.hot, plateText: MP.text, fill: MP.hot, good: MP.good, warn: MP.hot, count: MP.gold, onPri: MP.text, onSec: MP.text, link: MP.hot },
  tensura: { text: TP.text, mut: TP.mut, head: TP.teal, plateText: TP.text, fill: TP.teal, good: TP.good, warn: TP.warn, count: TP.text, onPri: TP.btnText, onSec: TP.text, link: TP.teal },
};
const SKILLS = [['Keen Fangs', 'fang', 'on'], ['Thick Hide', 'paw', 'on'], ['Steady Thirst', 'drop', 'on'], ['Night Stride', 'moon', 'off'], ['Venom Fang', 'fang', 'sel'], ['Silver Tongue', 'scroll', 'off'], ["Predator's Thirst", 'drop', 'off'], ['Moon Heart', 'moon', 'off'], ['Packbound Howl', 'paw', 'off'], ['Blood Scent', 'sigil', 'on'], ['Iron Will', 'crown', 'on'], ['Ancestral Voice', 'gem', 'off']];
const dotted = (cv, x0, y0, x1, y1, c) => { const n = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)); for (let i = 0; i <= n; i += 3) cv.px(Math.round(x0 + (x1 - x0) * i / n), Math.round(y0 + (y1 - y0) * i / n), c); };
const hexRing = (cv, cx, cy, w, h, tip, c, t = 1) => { const m = hexMask(w, h, tip), x0 = Math.round(cx - w / 2), y0 = Math.round(cy - h / 2); for (let j = 0; j < h; j++) for (let i = 0; i < w; i++) { if (!m.d[j * w + i]) continue; let edge = false; for (let k = 1; k <= t; k++) if (!m.get(i - k, j) || !m.get(i + k, j) || !m.get(i, j - k) || !m.get(i, j + k)) edge = true; if (edge) cv.px(x0 + i, y0 + j, c); } };
// a skill node of the theme's shape: state on | off | sel
const skillNode = (cv, th, cx, cy, icon, st) => {
  const d = 26, x = Math.round(cx - d / 2), y = Math.round(cy - d / 2), on = st !== 'off';
  const ring = st === 'sel' ? { grimoire: GP.br, moon: MP.gold, tensura: TP.gold }[th] : st === 'on' ? { grimoire: GP.red, moon: MP.red, tensura: TP.teal }[th] : { grimoire: GP.paDD, moon: MP.edge, tensura: TP.edge }[th];
  const glow = { grimoire: GP.ready, moon: MP.gold, tensura: TP.glow }[th];
  if (st === 'sel') { if (th === 'tensura') cv.bevel(hexMask(d + 8, d - 1 + 8, 8), ramp(0, withA(glow, 70), withA(glow, 70), withA(glow, 70)), x - 4, y - 4); else cv.disc(cx, cy, d / 2 + 4, d / 2 + 4, withA(glow, 80)); }
  const base = { grimoire: ramp(ring, GP.paH, GP.pa, GP.paS), moon: ramp(ring, MP.cardH, MP.card, MP.cardD), tensura: ramp(ring, TP.panelH, TP.panel, TP.panelD) }[th];
  const m = th === 'tensura' ? hexMask(d, d - 1, 7) : discMask(d);
  cv.bevel(m, base, x, y); if (st === 'sel') cv.ring(cx, cy, d / 2 - 1, d / 2 - 1, 1, ring);
  const inner = th === 'tensura' ? hexMask(d - 6, d - 7, 5) : discMask(d - 6); cv.bevel(inner, ramp(0, 0, { grimoire: rgba('#2A1A18'), moon: MP.cardD, tensura: rgba('#0A1827') }[th], 0), x + 3, y + 3);
  cv.blit(ICON[icon](16), x + (d - 16) / 2, y + (d - 16) / 2 + (th === 'tensura' ? 0 : 0));
  if (!on) { const o = th === 'tensura' ? hexMask(d - 6, d - 7, 5) : discMask(d - 6); cv.stamp(x + 3, y + 3, o, withA({ grimoire: rgba('#2A1A18'), moon: MP.cardD, tensura: rgba('#0A1827') }[th], 170)); }
};
const faceBox = (cv, K, x, y, f, o) => { // face in a themed square plate: 20 x 20
  cv.bevel(rr(20, 20, 1), K === GK ? ramp(GP.bord, GP.brHH, GP.br, GP.brS) : K === MK ? ramp(MP.edgeH, MP.edgeH, MP.edge, MP.cardD) : ramp(TP.teal, TP.teal, TP.edgeH, TP.edge), x, y);
  face(cv, x + 2, y + 2, o);
};
