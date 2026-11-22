// One theme's kit sheet. Needs THEME ('grimoire' | 'moon' | 'tensura') defined before this file.
const K = { grimoire: GK, moon: MK, tensura: TK }[THEME];
const GX = { grimoire: 0, moon: 2500, tensura: 5000 }[THEME];
const W = 2400, H = 2300, S = 4, M = 60, Y6 = 1860;
for (const n of PAGE.children.filter((c) => c.name === 'Kit · ' + K.name)) n.remove();
const stage = { grimoire: GP.le, moon: MP.sky, tensura: TP.bg }[THEME];
const sheetBg = { grimoire: rgba('#1B0B0B'), moon: rgba('#0B0509'), tensura: rgba('#08141F') }[THEME];
const sh = sheet('Kit · ' + K.name, GX, 1900, W, H, sheetBg);
const WHITE = 0xFFF2F2F2, GREY = 0xFF9A9AA6;
const D = {
  grimoire: {
    sub: 'The book. A leather cover with brass fittings, two parchment pages, wax seals on cords. Warm, tactile, quiet; ink on paper.',
    tags: 'leather  ·  brass  ·  parchment  ·  wax',
    sw: [['Leather', GP.le, 'cover, tabs'], ['Leather deep', GP.leD, 'shadow, outline'], ['Brass', GP.br, 'metal, lines'], ['Brass light', GP.brH, 'metal highlight'], ['Parchment', GP.pa, 'paper (text sits here)'], ['Parchment shade', GP.paS, 'bevel, rules'], ['Ink', GP.ink, 'body text'], ['Ink muted', GP.mut, 'captions'], ['Red ink', GP.red, 'titles, accent'], ['Wax', GP.wax, 'primary action'], ['Moss', GP.good, 'ready, good'], ['Candle', GP.glow, 'light, glow']],
    pairs: [['Ink on parchment', GP.ink, GP.pa], ['Ink muted on parchment', GP.mut, GP.pa], ['Red ink on parchment', GP.red, GP.pa], ['Moss on parchment', GP.good, GP.pa], ['Parchment on wax button', GP.paH, rgba('#8E1C22')], ['Ink on button', GP.ink, rgba('#D8C896')], ['Parchment on leather tab', GP.paH, GP.le], ['Disabled on button (3:1 rule)', rgba('#7C6F52'), rgba('#D5CBB0')]],
    glowName: 'Candle', glowSteps: GP.glow, light: 'Warm candle light from the top-left. Highlights are cream, shadows are brown, never grey.',
  },
  moon: {
    sub: 'The night. A banded red sky, an obsidian card with a blood-red hairline, pill buttons, one big moon. Cinematic, round, dark.',
    tags: 'sky  ·  obsidian  ·  blood  ·  moon',
    sw: [['Void', MP.void, 'screen floor'], ['Sky', MP.sky, 'top of the sky band'], ['Card', MP.card, 'panels (text sits here)'], ['Card light', MP.cardH, 'bevel, hover'], ['Edge', MP.edge, 'hairlines'], ['Blood', MP.red, 'primary action'], ['Blood hot', MP.hot, 'hover, titles on dark'], ['Moon', MP.moon, 'body text, moon core'], ['Moon rose', MP.moon3, 'moon mid'], ['Moon muted', MP.mut, 'captions'], ['Gold', MP.gold, 'SP, EP, ready'], ['Moss', MP.good, 'good']],
    pairs: [['Moon on card', MP.text, MP.card], ['Muted on card', MP.mut, MP.card], ['Blood hot on card', MP.hot, MP.card], ['Gold on card', MP.gold, MP.card], ['Moon on blood button', MP.text, rgba('#BF332B')], ['Moss on card', MP.good, MP.card], ['Moon on sky', MP.text, MP.sky], ['Disabled on card (3:1 rule)', rgba('#8A7071'), rgba('#22121A')]],
    glowName: 'Blood glow', glowSteps: MP.glow, light: 'Cold red moon light from the top-left. Glow is stepped in alpha bands, never a smooth blur.',
  },
  tensura: {
    sub: 'The status window. Deep blue panels with cut corners, teal brackets, hexagonal gems and a title bar. Precise, readable, a little electric.',
    tags: 'window  ·  teal  ·  hexagon  ·  bracket',
    sw: [['Window', TP.bg, 'screen floor'], ['Panel', TP.panel, 'panels (text sits here)'], ['Panel lit', TP.lit, 'title bar, hover'], ['Edge', TP.edge, 'borders'], ['Teal', TP.teal, 'primary action'], ['Teal light', TP.tealH, 'bevel highlight'], ['Teal deep', TP.tealD, 'pressed, ring'], ['Text', TP.text, 'body text'], ['Muted', TP.mut, 'captions'], ['Gold', TP.gold, 'operator, other mods'], ['Green', TP.good, 'ready, good'], ['Alert', TP.warn, 'timers, warnings']],
    pairs: [['Text on panel', TP.text, TP.panel], ['Muted on panel', TP.mut, TP.panel], ['Teal on panel', TP.teal, TP.panel], ['Gold on panel', TP.gold, TP.panel], ['Dark on teal button', TP.btnText, TP.teal], ['Green on panel', TP.good, TP.panel], ['Alert on panel', TP.warn, TP.panel], ['Disabled on panel (3:1 rule)', rgba('#5F7C8E'), rgba('#10243A')]],
    glowName: 'Teal glow', glowSteps: TP.glow, light: 'Screen light from inside the window. Glow is stepped in alpha bands, scanlines are 5 % white on every second row.',
  },
}[THEME];

const sec = async (n, title, y, note) => { await label(sh, n + ' · ' + title, M, y, WHITE, 24, true); if (note) await label(sh, note, M + 460, y + 5, GREY, 15); box(sh, M, y + 38, W - 2 * M, 1, 0x30FFFFFF); };
const stageBox = (x, y, w, h, c = stage) => box(sh, x, y, w, h, c);

// header
await label(sh, K.name, M, 36, WHITE, 56, true);
await label(sh, D.sub, M, 112, GREY, 19);
await label(sh, D.tags, M, 146, 0xFFD8B070 & 0xFFFFFFFF, 18, true);
await label(sh, 'Style guide · phase 1 · 1 texel = 1 GUI pixel · light from the top-left · shown here at 4×', W - M - 520, 50, GREY, 14);

// 1 palette
await sec('1', 'Palette', 196, 'text colours are checked against the colour they sit on (WCAG 4.5:1 for text)');
D.sw.forEach(([nm, c, role], i) => {
  const x = M + i * 190, y = 250;
  box(sh, x, y, 180, 96, c, nm); box(sh, x, y, 180, 96, 0x00000000); const ln = box(sh, x, y + 95, 180, 1, 0x28FFFFFF);
  label(sh, nm, x, y + 104, WHITE, 14, true); label(sh, hex6(c), x, y + 124, GREY, 13); label(sh, role, x, y + 142, GREY, 12);
});
const ratios = [];
for (let i = 0; i < D.pairs.length; i++) {
  const [nm, fg, bg] = D.pairs[i], x = M + (i % 4) * 570, y = 440 + Math.floor(i / 4) * 78, r = contrast(fg, bg);
  box(sh, x, y, 552, 66, bg, nm);
  T(sh, 'Venom Fang', x / 4 + 4, y / 4 + 6, { S: 4, c: fg, size: 8, w: 70 });
  const dis = nm.indexOf('Disabled') >= 0, ok = dis ? r >= 3 : r >= 4.5, light = lum(bg) > 0.4;
  label(sh, nm, x + 8, y + 3, light ? 0xFF2A2A2A : 0xFFD0D0D8, 12);
  label(sh, r.toFixed(1) + ':1  ' + (ok ? 'pass' : 'FAIL'), x + 380, y + 28, ok ? (light ? 0xFF14602A : 0xFF7CE0A0) : (light ? 0xFFA01010 : 0xFFFF6A6A), 17, true);
  ratios.push([nm, +r.toFixed(2), ok]);
}

// 2 light and glow
await sec('2', 'Light and glow', 640, D.light);
const gx = M;
[1, 0.6, 0.32, 0.16, 0.08].forEach((a, i) => { box(sh, gx + i * 140, 700, 128, 96, stage); box(sh, gx + i * 140, 700, 128, 96, withA(D.glowSteps, Math.round(a * 255))); label(sh, Math.round(a * 100) + ' %', gx + i * 140, 802, GREY, 13); });
label(sh, D.glowName + ' — one colour, five alpha steps (100 / 60 / 32 / 16 / 8).', gx, 826, WHITE, 14, true);
{
  const cv = new Cv(136, 36); cv.rect(0, 0, 136, 36, stage);
  const n = 24, st = ['normal', 'active'];
  K.slot(cv, 6, 6, n, 'normal'); cv.blit(ICON.fang(16), 10, 10);
  K.slot(cv, 38, 6, n, 'active'); cv.blit(ICON.fang(16), 42, 10); // hover/ready lit
  K.slot(cv, 70, 6, n, 'active'); cv.blit(ICON.fang(16), 74, 10); pie(cv, 82, 18, 11, 0.6, withA(K.dark, 190)); // cooldown sweep
  K.slot(cv, 102, 6, n, 'normal'); cv.blit(ICON.fang(16), 106, 10); cv.rect(106, 10, 16, 16, withA(K.dark, 150));
  cv.emit(sh, 760 / 4, 696 / 4, 4, 'slot states');
  ['idle', 'hover · ready glow', 'cooldown sweep', 'locked'].forEach((t, i) => label(sh, t, 760 + 24 + i * 128 - (i === 1 ? 12 : 0) - 4, 846, GREY, 12));
}
label(sh, 'Ready glow pulses between the 32 % and 60 % steps (see Motion). Cooldown sweeps clockwise from twelve.', 1360, 700, GREY, 14);
label(sh, 'Focus ring (keyboard) is always 2 px: dark outside, light inside, so it reads on any surface.', 1360, 724, GREY, 14);
{
  const cv = new Cv(56, 22); cv.rect(0, 0, 56, 22, K.paper); K.focus(cv, 11, 4, 34, 14); K.button(cv, 11, 4, 34, 14, 'normal', false); cv.emit(sh, 1360 / 4, 756 / 4, 4, 'focus demo');
}

// 3 frame family
await sec('3', 'Frame family', 870, 'panel · inset · divider · scroll bar · tab · nine-slice');
stageBox(M, 920, 2280, 300);
{
  const cv = new Cv(560, 74); cv.rect(0, 0, 560, 74, 0);
  K.panel(cv, 4, 4, 56, 40); K.inset(cv, 68, 4, 56, 40); K.divider(cv, 134, 22, 70);
  K.scroll(cv, 214, 4, 62, 0.15, 14); K.scroll(cv, 224, 4, 62, 0.7, 20);
  const tabs = ['normal', 'selected', 'hover', 'disabled'];
  tabs.forEach((t, i) => K.tab(cv, 248 + (i % 2) * 36, 6 + Math.floor(i / 2) * 20, 30, 14, t));
  cv.emit(sh, M / 4 + 4, 944 / 4, 4, 'frame family');
  ['panel', 'inset', 'divider', 'scroll bar ×2', 'tab: normal · selected · hover · disabled'].forEach((t, i) => label(sh, t, [76, 348, 612, 932, 1084][i], 1150, GREY, 13));
}
// nine-slice at 1x, 2x, 4x
{
  const pw = 36, ph = 28, c0 = new Cv(pw, ph); K.panel(c0, 0, 0, pw, ph);
  const ox = 1500, oy = 946; let x = ox;
  [1, 2, 4].forEach((s) => {
    const cv = new Cv(pw, ph); cv.d.set(c0.d); cv.emit(sh, x / s, oy / s, s, 'nine-slice ' + s + 'x');
    const k = 6; for (const gx2 of [k, pw - k]) box(sh, x + gx2 * s, oy, 1, ph * s, 0xAA66FFFF); for (const gy of [k, ph - k]) box(sh, x, oy + gy * s, pw * s, 1, 0xAA66FFFF);
    label(sh, s + '×', x, oy + ph * s + 8, WHITE, 14, true); x += pw * s + 40;
  });
  await label(sh, 'Nine-slice: corners 6 px stay fixed, edges tile, the centre fills.', ox, 1150 + 40, GREY, 13);
}

// 4 buttons
await sec('4', 'Buttons', 1280, 'secondary and primary, five states each');
stageBox(M, 1330, 2280, 250, K.paper);
{
  const states = ['normal', 'hover', 'pressed', 'disabled', 'focus'];
  const cv = new Cv(556, 62); cv.rect(0, 0, 556, 62, K.paper);
  const texts = [];
  [false, true].forEach((pri, row) => states.forEach((st, i) => {
    const x = 6 + i * 112, y = 8 + row * 24 + (row ? 4 : 0), w = 100, h = 14;
    if (st === 'focus') K.focus(cv, x, y, w, h);
    const o = K.button(cv, x, y, w, h, st === 'focus' ? 'normal' : st, pri); texts.push([st, x, y + o.dy, w, h, o.text, pri]);
  }));
  cv.emit(sh, M / 4 + 6, 1350 / 4, 4, 'buttons');
  texts.forEach(([st, x, y, w, h, c, pri]) => T(sh, pri ? 'Learn rank 3' : 'Put in slot', (M / 4 + 6) + x + 4, (1350 / 4) + y + 3, { S: 4, c, size: 8, w: w - 8, align: 'CENTER' }));
  const lc = lum(K.paper) > 0.4 ? 0xFF4A3A2A : 0xFFB8B8C4;
  states.forEach((st, i) => label(sh, st, (M / 4 + 6 + 6 + i * 112) * 4, 1556, lc, 14, true));
}

// 5 ornaments
await sec('5', 'Ornament set', 1610, 'seals, gems, corners, flourishes: the only places the theme may show off');
stageBox(M, 1660, 2280, 160);
{
  const cv = new Cv(560, 66);
  if (THEME === 'grimoire') {
    GK.cap(cv, 4, 4, 0, 0); GK.cap(cv, 20, 4, 1, 0);
    ['normal', 'hover', 'pressed'].forEach((s, i) => GK.seal(cv, 40 + i * 22, 4, 16, s));
    [GP.wax, rgba('#3F6FA8'), GP.br, GP.good].forEach((c, i) => GK.gem(cv, 114 + i * 12, 8, 9, c));
    cv.rect(112, 24, 60, 36, GP.pa); GK.curl(cv, 114, 26, false, false, GP.ink); GK.curl(cv, 159, 26, true, false, GP.ink); GK.curl(cv, 114, 49, false, true, GP.ink); GK.curl(cv, 159, 49, true, true, GP.ink);
    cv.bevel(rr(8, 22, 0), ramp(GP.waxD, GP.waxH, GP.wax, GP.waxM), 190, 4); cv.rect(190, 26, 3, 3, GP.leD); cv.rect(195, 26, 3, 3, GP.leD); cv.px(193, 25, GP.leD); cv.px(194, 25, GP.leD);
    cv.bevel(rr(52, 20, 2), GR.leather, 210, 6, true); cv.box(212, 8, 48, 16, withA(GP.br, 200));
    cv.bevel(rr(36, 12, 2), ramp(GP.bord, GP.paH, GP.pa, GP.paS), 276, 8);
  } else if (THEME === 'moon') {
    K.slot(cv, 4, 4, 24, 'normal'); K.slot(cv, 34, 4, 24, 'active');
    [null, 4, 7, 11, 14].forEach((off, i) => {
      const x = 70 + i * 18, dm = discMask(14), lit = new Cv(14, 14);
      cv.disc(x + 7, 11, 7, 7, MP.cardD); cv.ring(x + 7, 11, 7, 7, 1, MP.edge);
      if (off !== null) { lit.d.set(dm.d); const sh2 = new Cv(14, 14); sh2.stamp(off, 0, dm, 1); for (let k = 0; k < lit.d.length; k++) if (sh2.d[k]) lit.d[k] = 0; cv.bevel(lit, ramp(0, MP.moon, MP.moon2, MP.moon3), x, 4); }
    });
    K.divider(cv, 166, 12, 70); K.gem(cv, 250, 6, 10, MP.red); K.gem(cv, 266, 6, 10, MP.gold);
    cv.blit(ICON.drop(16), 288, 2); cv.blit(ICON.moon(16), 308, 2);
    MK.halo(cv, 346, 18, 8, MP.glow); cv.bevel(discMask(16), ramp(MP.redDD, MP.hot, MP.red, MP.redD), 338, 10);
    K.plate(cv, 376, 6, 90, 14); cv.rect(480, 8, 1, 10, MP.moon); cv.rect(477, 12, 7, 1, MP.moon); cv.rect(494, 6, 1, 6, MP.moon3); cv.rect(492, 9, 5, 1, MP.moon3);
  } else {
    K.slot(cv, 4, 4, 26, 'normal'); K.slot(cv, 38, 4, 26, 'active');
    K.gem(cv, 76, 8, 12, TP.teal); K.gem(cv, 94, 8, 12, TP.gold); K.gem(cv, 112, 8, 12, TP.good);
    cv.rect(130, 4, 70, 40, TP.panel); TK.brackets(cv, 132, 6, 66, 36, TP.teal);
    TK.title(cv, 210, 4, 90); cv.rect(210, 18, 90, 26, TP.panel);
    for (let i = 0; i < 4; i++) { const x = 318 + i * 10; cv.poly([[x, 10], [x + 4, 14], [x, 18]], withA(TP.teal, 255 - i * 55)); }
    for (let i = 0; i < 5; i++) cv.rect(318 + i * 16, 30, 3, 3, i === 2 ? TP.teal : TP.edge); cv.hl(321, 31, 64, TP.edge);
    K.plate(cv, 410, 6, 90, 14); K.keyseal(cv, 510, 8);
  }
  cv.emit(sh, M / 4 + 6, 1690 / 4, 4, 'ornaments');
}

// 6 type scale
await sec('6', 'Type scale on the Minecraft font', Y6, 'sizes in GUI px, shown at GUI scale 2 (real size); the whole screen scales together, so a texel never changes size');
const paper = K.paper;
box(sh, M, Y6 + 50, 2280, 300, paper);
const tc = { grimoire: [GP.red, GP.ink, GP.ink, GP.mut], moon: [MP.text, MP.hot, MP.text, MP.mut], tensura: [TP.teal, TP.text, TP.text, TP.mut] }[THEME];
const rows = [
  ['Title', '16 px line · display bitmap font, cap height 11', 'Skills', 'title', 12, tc[0]],
  ['Heading', '8 px · Minecraft font, bold, theme colour', 'Venom Fang', 'head', 8, tc[1]],
  ['Body', '8 px · Minecraft font, shadow off on paper', 'Your bite carries poison into any vampire it touches.', 'body', 8, tc[2]],
  ['Caption', '8 px · same size, muted colour, never smaller', 'Requires Hybrid rung 3 · Supernatural Bloodlines', 'body', 8, tc[3]],
];
rows.forEach(([nm, spec, sample, font, size, c], i) => {
  const y = Y6 + 70 + i * 72;
  label(sh, nm, M + 24, y + 10, K.id === 'grimoire' ? 0xFF5A4630 : 0xFFB8B8C4, 18, true); label(sh, spec, M + 24, y + 36, K.id === 'grimoire' ? 0xFF6A5236 : 0xFF8A8A98, 13);
  T(sh, sample, (M + 560) / 2, y / 2 + 2, { S: 2, c, size, font, w: 400, lh: 9 });
});
await label(sh, 'Only one real text size exists: Minecraft\'s 8 px font. Title is a custom 16 px bitmap font (a resource-pack font provider), same texel density. Nothing is scaled 1.5×.', M, Y6 + 366, GREY, 13);
return done({ id: sh.id, ratios });
