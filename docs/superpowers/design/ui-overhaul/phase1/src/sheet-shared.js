// Shared sheets: 00 read me + pixel grid, 10 icons, 11 motion.
for (const n of PAGE.children.filter((c) => /^(00 ·|10 ·|11 ·)/.test(c.name))) n.remove();
const WHITE = 0xFFF2F2F2, GREY = 0xFFA0A0AC, DIM = 0xFF70707C, LINE = 0x30FFFFFF, BG = rgba('#141418'), CARD = rgba('#1C1C22');
const hd = (f, t, x, y) => label(f, t, x, y, WHITE, 24, true);
const rule = (f, x, y, w) => box(f, x, y, w, 1, LINE);
const para = (f, t, x, y, w, c = GREY, s = 15) => label(f, t, x, y, c, s, false, w);

// ================= 00 READ ME ===================
{
  const f = sheet('00 · Read me and the pixel grid', 0, 0, 2400, 1640, BG);
  label(f, 'UI overhaul · style guide · phase 1', 60, 40, WHITE, 52, true);
  para(f, 'Owner approval gate. Nothing is built until this page is approved. Every pixel on this page is a rectangle on a grid: one texel is one GUI pixel, and the same art is shown at GUI scale 2, 3 and 4.', 60, 112, 1500, GREY, 19);
  hd(f, '1 · The pixel grid', 60, 190); rule(f, 60, 228, 2280);
  // the same 12 x 12 sprite at GUI scale 2, 3, 4
  const sp = new Cv(34, 34); sp.rect(0, 0, 34, 34, rgba('#2A1A18')); sp.blit(ICON.drop(32), 1, 1);
  [2, 3, 4].forEach((s, i) => { const x = 60 + [0, 150, 330][i]; sp.emit(f, x / s, 262 / s, s, 'grid ' + s + 'x'); label(f, 'GUI scale ' + s, x, 262 + 34 * s + 8, GREY, 13); });
  para(f, 'Rules that hold on every screen', 860, 250, 700, WHITE, 19);
  para(f, '• One texel size per screen. Frames, icons, ornaments and the type all sit on the same grid. Nothing is scaled by a fraction, and nothing is smoothed.\n• Light comes from the top-left: highlight on the top and left edge, shade on the bottom and right. Outlines are a darker shade of the fill, never pure black.\n• No gradients. A fade is a stepped band, or a 2 × 2 ordered dither on the seam, as in the sky and the moon.\n• Text is Minecraft\'s 8 px font only. Titles use a custom bitmap font at the same density. Contrast is at least 4.5:1 (3:1 for disabled), checked against the real colours on every theme sheet.\n• Focus ring: 2 px, dark outside and light inside, so it reads on any surface. Keyboard focus never depends on colour alone.\n• The theme draws the frame, the plate and the glow around an icon. The icon itself is shared by all three themes.', 860, 284, 1480, GREY, 16);
  hd(f, '2 · The three themes keep their identity', 60, 560); rule(f, 60, 598, 2280);
  [[GK, 'Grimoire', 'A real open book: a leather cover with brass corners, parchment pages, index tabs on the right edge, wax seals on cords. Ink on paper, warm and quiet.', 'Kept from the earlier mockups: the two-page layout, the tabs, the seals, the red ink.'], [MK, 'Blood Moon', 'A night screen: a banded red sky, one big dithered moon, round rails, pill buttons. Cinematic and dark.', 'Kept: the moon as the centre, the circle rails left and right, the bottom detail bar.'], [TK, 'Tensura', 'A status window: cut corners, teal brackets, hexagonal gems, a title bar and a list. Precise and a little electric.', 'Kept: the title bar, the page list with groups, the info pane on the right.']].forEach(([K, nm, a, b], i) => {
    const x = 60 + i * 760, y = 630;
    box(f, x, y, 720, 470, K === GK ? GP.le : K === MK ? MP.sky : TP.bg);
    const cv = new Cv(176, 72); cv.rect(0, 0, 176, 72, K === GK ? GP.le : K === MK ? MP.sky : TP.bg);
    K.panel(cv, 6, 6, 56, 40); K.inset(cv, 66, 6, 40, 40); K.button(cv, 112, 8, 56, 14, 'normal', true); K.button(cv, 112, 26, 56, 14, 'normal', false); K.slot(cv, 8, 48, 22, 'normal'); cv.blit(ICON.fang(16), 11, 51); K.slot(cv, 34, 48, 22, 'active'); cv.blit(ICON.drop(16), 37, 51); K.tab(cv, 62, 52, 36, 14, 'selected'); K.tab(cv, 102, 52, 36, 14, 'normal'); K.bar(cv, 142, 54, 28, 6, 0.6);
    cv.emit(f, (x + 16) / 4, (y + 16) / 4, 4, 'preview ' + nm);
    label(f, nm, x + 16, y + 312, WHITE, 22, true); para(f, a, x + 16, y + 346, 688, 0xFFD0D0D8, 14); para(f, b, x + 16, y + 410, 688, 0xFF9A9AA6, 13);
  });
  hd(f, '3 · What is on this page', 60, 1130); rule(f, 60, 1168, 2280);
  para(f, 'Row 1: this page · 10 Icons · 11 Motion.   Row 2: one kit sheet per theme (palette with contrast, light and glow, frame family with the nine-slice at 1×, 2× and 4×, buttons in five states, ornaments, type scale).   Then, each in the three themes at 1920 × 1080 and GUI scale 2: the HUD in Supernatural and in Tensura mode with Alt held, the Home page, the Skills page, and the boss bars (alone and three stacked, with a zoomed shift).   Last: three logo directions.', 60, 1186, 2280, GREY, 15);
  hd(f, '4 · Where the references went', 60, 1290); rule(f, 60, 1328, 2280);
  [['Cobblemon', 'Clean panels and smooth motion. We took: calm panels, one accent colour per screen, a settled rhythm of 6 and 8 px gaps, and a restrained ease.'], ['Create · Ponder', 'Brass, wood and a readable build. We took: the brass fittings and rivets on the Grimoire, labels that explain themselves, and diagrams that read at a glance.'], ['Genshin · Hollow Knight', 'Ornament, glow and weight. We took: the stepped glow on what is ready, the moon as a hero object, and motion that arrives with weight and leaves quickly.']].forEach(([t, d], i) => { const x = 60 + i * 760; label(f, t, x, 1350, WHITE, 18, true); para(f, d, x, 1382, 700, GREY, 14); });
  hd(f, '5 · For the owner to decide', 60, 1470); rule(f, 60, 1508, 2280);
  para(f, '1  The logo direction (A seal, B drop, C glyph).     2  Final palettes of the three themes (every colour is on its kit sheet).     3  A 16 px companion icon set for the HUD slots (a 24 px slot cannot hold a 32 px icon at one texel size).     4  A custom 16 px bitmap font for titles, so titles never use a scaled font.     5  Whether any page layout changes beyond reskinning (these mockups keep the approved layouts).', 60, 1526, 2280, GREY, 15);
}

// ================= 10 ICONS ===================
{
  const f = sheet('10 · Icons: grid and silhouette rules', 2500, 0, 2400, 1760, BG);
  label(f, 'Shared icon set', 60, 40, WHITE, 52, true);
  para(f, 'One set of icons for all three themes. 16 px for HUD slots and tight lists, 32 px for the hub, 64 px where something is shown large. The theme sets the frame, the plate and the glow around an icon, never the icon.', 60, 112, 1500, GREY, 19);
  hd(f, '1 · The grid', 60, 190); rule(f, 60, 228, 2280);
  [[16, 24, 'ICON.fang'], [32, 12, ''], [64, 6, '']].forEach(([n, S, _], i) => {
    const x = 60 + i * 520, y = 250; box(f, x, y, n * S, n * S, rgba('#22222A'));
    for (let k = 0; k <= n; k++) { const a = (k % 8 === 0) ? 0x40FFFFFF : 0x14FFFFFF; box(f, x + k * S, y, 1, n * S, a); box(f, x, y + k * S, n * S, 1, a); }
    box(f, x + 2 * S * n / 32, y + 2 * S * n / 32, n * S - 4 * S * n / 32, n * S - 4 * S * n / 32, 0x00000000);
    const live = Math.round(2 * n / 32); const lv = new Cv(n, n); lv.box(live, live, n - 2 * live, n - 2 * live, withA(rgba('#6AE0FF'), 140)); lv.emit(f, x / S, y / S, S, 'live area ' + n);
    const ic = ICON[n === 16 ? 'fang' : n === 32 ? 'drop' : 'moon'](n); ic.emit(f, x / S, y / S, S, 'sample ' + n);
    label(f, n + ' × ' + n + ' px' + (n === 16 ? ' · HUD slot, lists' : n === 32 ? ' · hub pages, tooltips' : ' · large, details, emblem'), x, y + n * S + 10, WHITE, 15, true);
    label(f, n === 16 ? 'Live area 14 × 14 (1 px margin). One outline, two tones, no detail smaller than 2 px.' : n === 32 ? 'Live area 28 × 28 (2 px margin). Outline, three tones, a 1 px highlight band.' : 'Live area 56 × 56 (4 px margin). Outline, four tones, a second highlight band, interior detail.', x, y + n * S + 36, GREY, 13, false, 480);
  });
  hd(f, '2 · Silhouette and shading rules', 60, 790); rule(f, 60, 828, 2280);
  para(f, '• The silhouette alone must say what the icon is, on a light and on a dark ground (see row 4). One idea per icon, one dominant colour family plus at most one accent.\n• Outline is 1 texel, a darker shade of the icon\'s own colour, never black and never the theme colour. The outline stays at 1 texel at 16, 32 and 64 px, so a bigger icon gets finer detail, not fatter lines.\n• Light from the top-left. Each material has a four-tone ramp: outline, highlight, base, shade (+ two extra bands at 32 and 64 px). Ramps are shared (bone, blood, gold, moon, fur, violet, teal, paper).\n• No anti-aliasing, no dithering inside an icon, no glow inside the canvas. Glow belongs to the frame.\n• Corners of the canvas stay empty; a shape never touches the live-area edge without a 1 texel outline inside the canvas.\n• Locked: the same icon at 40 % ink under the frame colour with a padlock; cooldown: a clockwise dark sweep over the icon. Neither redraws the icon.', 60, 850, 1500, GREY, 15);
  hd(f, '3 · Eight sample icons: 32 px and the 16 px companion', 60, 1130); rule(f, 60, 1168, 2280);
  const names = [['fang', 'Bite · fangs'], ['drop', 'Blood'], ['moon', 'Moon · path'], ['paw', 'Werewolf · pack'], ['sigil', 'Blood Scent · sigil'], ['gem', 'Tensura skill'], ['scroll', 'Quests'], ['crown', 'Alpha']];
  names.forEach(([nm, lb], i) => {
    const x = 60 + (i % 8) * 285, y = 1190; box(f, x, y, 270, 250, rgba('#22222A'));
    ICON[nm](32).emit(f, (x + 14) / 5, (y + 14) / 5, 5, nm + ' 32'); ICON[nm](16).emit(f, (x + 190) / 5, (y + 14) / 5, 5, nm + ' 16');
    label(f, lb, x + 14, y + 190, WHITE, 14, true); label(f, '32 px · 16 px companion', x + 14, y + 212, DIM, 12);
  });
  hd(f, '4 · Silhouette check at real size, light and dark', 60, 1470); rule(f, 60, 1508, 2280);
  [rgba('#E8E8EE'), rgba('#101018')].forEach((bg, row) => {
    const y = 1530 + row * 100; box(f, 60, y, 2280, 90, bg); const cv = new Cv(1100, 40);
    names.forEach(([nm], i) => { const ic = ICON[nm](32), sil = new Cv(32, 32); for (let k = 0; k < sil.d.length; k++) sil.d[k] = ic.d[k] ? (row ? rgba('#E8E8EE') : rgba('#101018')) : 0; cv.blit(sil, 8 + i * 44, 4); const s16 = ICON[nm](16), sl = new Cv(16, 16); for (let k = 0; k < sl.d.length; k++) sl.d[k] = s16.d[k] ? (row ? rgba('#E8E8EE') : rgba('#101018')) : 0; cv.blit(sl, 380 + i * 24, 12); });
    cv.emit(f, 80 / 2, (y + 5) / 2, 2, 'silhouettes ' + row);
  });
}

// ================= 11 MOTION ===================
{
  const f = sheet('11 · Motion rules', 5000, 0, 2400, 1700, BG);
  label(f, 'Motion', 60, 40, WHITE, 52, true);
  para(f, 'Purposeful only. Motion explains a change (a page turned, a skill learned, a form shifted) or says that something is ready. Everything runs on the client tick (20 per second), snaps to whole texels, and turns off with the Settings option "Reduce motion".', 60, 112, 1600, GREY, 19);
  hd(f, '1 · The rules', 60, 190); rule(f, 60, 228, 2280);
  para(f, '• Only whole texels. Position moves in whole texel steps and sprites swap in 2 to 4 frames. Nothing scales by a fraction, so pixel art never goes soft.\n• Short and with weight: arrive fast, settle once, leave faster. Most things take 2 to 8 ticks (100 to 400 ms). Nothing blocks input; a press is accepted on the first tick.\n• One thing at a time. A screen may glow one ready target, not five; flourishes queue and never overlap.\n• Reduce motion (client setting reduce_motion, default off) turns off tweens, glow pulses, flourishes and our screen shake in the hub, the HUD and the boss bars. State changes still happen, instantly. It does not touch world animation (boss transformations, particles).', 60, 250, 1500, GREY, 15);
  hd(f, '2 · Events', 60, 470); rule(f, 60, 508, 2280);
  const rows = [['Event', 'What moves', 'Duration', 'Easing', 'With reduce motion'],
    ['Page turn (Grimoire)', 'The page edge moves across in 4 steps, a shade band follows it', '6 ticks · 300 ms', 'ease-out cubic', 'Instant swap'],
    ['Page change (Moon, Tensura)', 'Old content fades out, new content slides 4 texels and fades in', '4 ticks · 200 ms', 'ease-out quad', 'Instant swap'],
    ['Button press', 'Button drops 1 texel, highlight and shade swap; on release it rises 1 texel and a 1 px glint crosses', '2 ticks down · 3 ticks up', 'step', 'Colour swap only'],
    ['Hover', 'Border brightens, 1 px outer glow in 2 alpha steps', '2 ticks', 'step', 'Same (it is state, not motion)'],
    ['Slot ready glow', 'Halo alternates between the 32 % and 60 % alpha bands', '30-tick loop · 1.5 s', 'sine, 4 steps', 'Held at 60 %'],
    ['Cooldown sweep', 'Dark wedge shrinks clockwise from twelve; seconds in the middle', 'the cooldown', 'linear', 'Same (it is information)'],
    ['Skill unlocked / quest claimed', 'A ring expands in 4 steps while 4 sparks rise 3 texels and fade', '12 ticks · 600 ms', 'ease-out cubic', 'The ring is drawn once, no sparks'],
    ['Boss bar health', 'The bar shortens to the new value; a pale trail follows 6 ticks later', '10 ticks · 500 ms', 'ease-out cubic', 'Instant'],
    ['Boss form chip', 'The lit chip changes, the bar turns gold and striped while the boss can be hit, the portrait morphs through a jagged seam', 'the 40-tick shift', 'step', 'Chip and colour change, no seam flash'],
    ['Tooltip', 'Appears after a short delay, fades in', '10 ticks delay · 3 ticks fade', 'linear', 'No fade']];
  const cw = [380, 820, 320, 260, 500];
  rows.forEach((r, i) => { const y = 530 + i * 54; box(f, 60, y, 2280, 50, i === 0 ? rgba('#26262E') : (i % 2 ? CARD : BG)); let x = 70; r.forEach((c, k) => { label(f, c, x, y + 8, i === 0 ? WHITE : k === 0 ? WHITE : GREY, i === 0 ? 14 : 13, i === 0 || k === 0, cw[k] - 16); x += cw[k]; }); });
  hd(f, '3 · The easings (shown on a 64 × 64 grid, 3 px per texel)', 60, 1170); rule(f, 60, 1208, 2280);
  const ease = { 'linear': (t) => t, 'ease-out quad': (t) => 1 - (1 - t) * (1 - t), 'ease-out cubic': (t) => 1 - Math.pow(1 - t, 3), 'sine (loop)': (t) => 0.5 - 0.5 * Math.cos(t * 2 * Math.PI), 'step (4)': (t) => Math.floor(t * 4) / 3 > 1 ? 1 : Math.floor(t * 4) / 3 };
  Object.entries(ease).forEach(([nm, fn], i) => {
    const cv = new Cv(66, 66); cv.rect(0, 0, 66, 66, rgba('#22222A')); cv.box(1, 1, 64, 64, rgba('#3A3A46'));
    let py = null; for (let x = 0; x <= 63; x++) { const y = 64 - Math.round(fn(x / 63) * 62); if (nm.startsWith('step')) cv.px(1 + x, y, rgba('#FFD36B')); else { cv.px(1 + x, y, rgba('#FFD36B')); if (py !== null) cv.line(x, py, 1 + x, y, rgba('#FFD36B')); } py = y; }
    cv.emit(f, (60 + i * 260) / 3, 1236 / 3, 3, 'easing ' + nm); label(f, nm, 60 + i * 260, 1236 + 200, WHITE, 14, true);
  });
  hd(f, '4 · Four frames of three motions (4 px per texel)', 60, 1470); rule(f, 60, 1508, 2280);
  // press
  { const cv = new Cv(150, 26); cv.rect(0, 0, 150, 26, rgba('#2A0E0E')); for (let i = 0; i < 4; i++) { const st = ['normal', 'hover', 'pressed', 'normal'][i], dy = st === 'pressed' ? 1 : 0; GK.button(cv, 4 + i * 36, 6 + (i === 3 ? 0 : 0), 32, 14, st, true); if (i === 3) { cv.px(4 + i * 36 + 4, 7, 0xFFFFFFFF); cv.px(4 + i * 36 + 5, 7, 0xFFFFFFFF); } }
    cv.emit(f, 60 / 4, 1530 / 4, 4, 'press strip'); label(f, 'Button press: rest · hover · down 1 texel · up with glint', 60, 1640, GREY, 13); }
  // ready glow
  { const cv = new Cv(150, 36); cv.rect(0, 0, 150, 36, rgba('#2A0E0E')); [0.32, 0.5, 0.6, 0.5].forEach((a, i) => { const x = 8 + i * 36; cv.disc(x + 12, 18, 16, 16, withA(GP.ready, Math.round(a * 140))); GK.slot(cv, x, 6, 24, 'normal'); cv.blit(ICON.fang(16), x + 4, 10); });
    cv.emit(f, 800 / 4, 1520 / 4, 4, 'glow strip'); label(f, 'Ready glow: 32 % · 50 % · 60 % · 50 %', 800, 1668 - 14, GREY, 13); }
  // ring flourish
  { const cv = new Cv(150, 36); cv.rect(0, 0, 150, 36, rgba('#2A0E0E')); [6, 10, 14, 17].forEach((r, i) => { const x = 18 + i * 36; cv.ring(x, 18, r, r, 1, withA(GP.ready, 255 - i * 60)); GK.gem(cv, x - 4, 14, 9, GP.br); if (i > 0) for (let k = 0; k < 4; k++) cv.px(x - 6 + k * 4, 18 - i * 2 - k % 2 * 2, withA(GP.brHH, 255 - i * 50)); });
    cv.emit(f, 1500 / 4, 1520 / 4, 4, 'ring strip'); label(f, 'Unlock flourish: ring grows in 4 steps, sparks rise and fade', 1500, 1668 - 14, GREY, 13); }
}
return done({});
