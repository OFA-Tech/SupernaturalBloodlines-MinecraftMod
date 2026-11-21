// Skills page mockup. Needs THEME.
const K = KF[THEME], C = HCOL[THEME];
const FX = { grimoire: 0, moon: 2000, tensura: 4000 }[THEME], FY = 8000;
const fname = 'Skills · ' + K.name;
for (const n of PAGE.children.filter((c) => c.name === fname)) n.remove();
const fr = figma.createFrame(); fr.name = fname; fr.x = FX; fr.y = FY; fr.resize(1920, 1080); fr.clipsContent = true; fr.fills = []; PAGE.appendChild(fr);
const cv = new Cv(SW, SH), tx = [];
const say = (s, x, y, o = {}) => tx.push([s, x, y, o]);
const PAGES = ['Home', 'Path', 'Skills', 'Quests', 'Rites', 'Bloodline', 'Pack', 'Guide', 'Loadout'];
const PICON = { Home: 'home', Path: 'moon', Skills: 'fang', Quests: 'scroll', Rites: 'sigil', Bloodline: 'drop', Pack: 'paw', Guide: 'book', Loadout: 'crown' };
const pips = (x, y, n, on, c) => { for (let i = 0; i < n; i++) { cv.disc(x + 3 + i * 9, y + 3, 3.5, 3.5, i < on ? c : withA(c, 70)); cv.ring(x + 3 + i * 9, y + 3, 3.5, 3.5, 1, i < on ? 0xFF000000 & 0x00000000 | withA(c, 255) : c); } };
const nodeAt = (cx, cy, rx, ry, i) => { const a = -Math.PI / 2 + i * Math.PI / 6; return [cx + Math.cos(a) * rx, cy + Math.sin(a) * ry]; };
const lab = (s, nx, ny, c, font, back) => { const w = Math.round(s.length * 5.3) + 4; cv.rect(Math.round(nx - w / 2), ny + 13, w, 10, back); say(s, nx - 48, ny + 15, { c, w: 96, align: 'CENTER', font }); };
const desc = 'Your bite carries poison into any vampire it touches. Each rank adds a quarter more venom damage, and the poison lingers until the victim drinks a hybrid\'s blood.';

if (THEME === 'grimoire') {
  cv.rect(0, 0, SW, SH, rgba('#1E0C0C')); for (let i = 0; i < 90; i++) cv.px(Math.floor(rng(i + 7)() * SW), Math.floor(rng(i + 99)() * SH), withA(GP.leM, 120));
  GK.cover(cv, 24, 16, 892, 500);
  const LP = [38, 30, 428, 472], RP = [476, 30, 428, 472];
  for (const [x, y, w, h] of [LP, RP]) { for (let k = 3; k >= 1; k--) cv.rect(x + (x < 400 ? -k : 0), y + k, w + (x < 400 ? 0 : k), h, [GP.paS, GP.paD, rgba('#AE9868')][k - 1]); cv.bevel(rr(w, h, 2), GR.page, x, y); }
  for (let i = 0; i < 10; i++) { cv.rect(LP[0] + LP[2] - 1 - i, LP[1] + 1, 1, LP[3] - 2, withA(rgba('#46280a'), 80 - i * 8)); cv.rect(RP[0] + i, RP[1] + 1, 1, RP[3] - 2, withA(rgba('#46280a'), 80 - i * 8)); }
  cv.grain(LP[0] + 4, LP[1] + 4, LP[2] - 8, LP[3] - 8, withA(GP.paD, 90), 0.0015, 5, 1); cv.grain(RP[0] + 4, RP[1] + 4, RP[2] - 8, RP[3] - 8, withA(GP.paD, 90), 0.0015, 8, 1);
  // index tabs on the right edge
  PAGES.forEach((p, i) => { const y = 36 + i * 22, sel = p === 'Skills'; GK.tab(cv, 902, y, 54, 18, sel ? 'selected' : 'normal'); say(p, sel ? 907 : 910, y + 5, { c: GP.paH }); });
  [['T', 0], ['E', 1], ['W', 2], ['V', 3]].forEach(([l, i]) => { const x = 74 + i * 38; cv.vl(x + 8, 516, 8, GP.brS); GK.seal(cv, x, 522, 16, 'normal'); say(l, x, 526, { c: GP.paH, w: 16, align: 'CENTER' }); });
  // left page
  const x0 = 56;
  say('Skills', x0, 40, { font: 'title', size: 14, c: GP.red, k: 1.3 }); say('6 SP to spend', 330, 42, { font: 'head', c: GP.red, w: 90, align: 'RIGHT' }); say('1,028 EP', 330, 54, { c: GP.mut, w: 90, align: 'RIGHT' });
  say('Chapter: Bloodline · Werewolves · Vampirism', x0, 62, { c: GP.mut }); GK.divider(cv, x0, 76, 372);
  say('Venom Fang', x0, 96, { font: 'title', size: 12, c: GP.red, k: 1.3 }); pips(x0, 114, 3, 2, GP.red); say('rank 2 of 3', x0 + 34, 112, { c: GP.mut });
  GK.slot(cv, x0, 130, 36, 'normal'); cv.blit(ICON.fang(32), x0 + 2, 132);
  say(desc, x0 + 46, 128, { c: GP.ink, w: 322, lh: 11 });
  [['Next rank', '3 skill points'], ['Requires', 'Hybrid rung 3 ✓'], ['Source', 'Supernatural Bloodlines']].forEach(([a, b], i) => { const y = 188 + i * 16; say(a, x0, y, { c: GP.ink }); say(b, x0 + 372 - 160, y, { c: GP.ink, w: 160, align: 'RIGHT' }); for (let k = x0 + 46; k < x0 + 372 - 100; k += 3) cv.px(k, y + 8, GP.paD); });
  GK.seal(cv, x0, 246, 36, 'normal'); say('LEARN', x0, 260, { c: GP.paH, w: 36, align: 'CENTER', size: 8 }); GK.seal(cv, x0, 246, 36, 'normal');
  say('Put in a HUD slot', x0 + 48, 258, { c: GP.red }); cv.hl(x0 + 48, 267, 80, GP.red);
  say('Ranks', x0, 290, { font: 'head', c: GP.red });
  [['Rank 1', '+25 % venom damage', 1], ['Rank 2', '+50 % venom damage', 1], ['Rank 3', '+75 % and the poison spreads · 3 SP', 0]].forEach(([a, b, d], i) => { const y = 304 + i * 14; cv.disc(x0 + 3, y + 4, 3.5, 3.5, d ? GP.red : withA(GP.red, 70)); cv.ring(x0 + 3, y + 4, 3.5, 3.5, 1, GP.red); say(a, x0 + 12, y, { c: GP.ink }); say(b, x0 + 56, y, { c: d ? GP.mut : GP.ink }); });
  GK.divider(cv, x0, 352, 372);
  say('Learned', x0, 364, { font: 'head', c: GP.red }); say('Keen Fangs · Thick Hide · Steady Thirst · Blood Scent · Iron Will', x0, 378, { c: GP.ink, w: 372, lh: 11 });
  say('- 14 -', 230, 482, { c: GP.mut });
  // right page: chapter tabs and the graph
  const rx = 496;
  [['Bloodline', 1], ['Werewolves', 0], ['Vampirism', 0]].forEach(([t, sel], i) => { const x = rx + i * 100; say(t, x, 44, { font: sel ? 'head' : 'body', c: sel ? GP.red : GP.mut }); if (sel) cv.hl(x, 56, 50, GP.red); });
  const cx = 690, cy = 276;
  cv.ring(cx, cy, 52, 52, 1, GP.bord); cv.ring(cx, cy, 46, 46, 1, GP.paDD); for (let i = 0; i < 60; i++) { const a = i / 60 * 2 * Math.PI; cv.px(Math.round(cx + Math.cos(a) * 49), Math.round(cy + Math.sin(a) * 49), GP.paDD); }
  const pts = []; for (let i = 0; i < 5; i++) { const a = -Math.PI / 2 + i * 2 * Math.PI / 5; pts.push([Math.round(cx + Math.cos(a) * 40), Math.round(cy + Math.sin(a) * 40)]); } for (let i = 0; i < 5; i++) cv.line(...pts[i], ...pts[(i + 2) % 5], GP.paD);
  SKILLS.forEach((s, i) => { const [nx, ny] = nodeAt(cx, cy, 138, 128, i), a = -Math.PI / 2 + i * Math.PI / 6; const sx = cx + Math.cos(a) * 52, sy = cy + Math.sin(a) * 52, ex = nx - Math.cos(a) * 14, ey = ny - Math.sin(a) * 14; if (s[2] === 'off') dotted(cv, sx, sy, ex, ey, GP.paDD); else cv.line(Math.round(sx), Math.round(sy), Math.round(ex), Math.round(ey), s[2] === 'sel' ? GP.red : GP.bord); });
  figure(cv, cx - 7, cy - 15);
  SKILLS.forEach((s, i) => { const [nx, ny] = nodeAt(cx, cy, 138, 128, i); skillNode(cv, 'grimoire', nx, ny, s[1], s[2]); lab(s[0], nx, ny, s[2] === 'sel' ? GP.red : s[2] === 'off' ? GP.mut : GP.ink, s[2] === 'sel' ? 'head' : 'body', GP.pa); });
  say('- 15 -', 680, 482, { c: GP.mut });
} else if (THEME === 'moon') {
  MK.sky(cv, 0, 0, SW, SH);
  const cx = 480, cy = 250; MK.halo(cv, cx, cy, 58, MP.glow, 5); MK.moon(cv, cx, cy, 116);
  SKILLS.forEach((s, i) => { const [nx, ny] = nodeAt(cx, cy, 190, 138, i), a = -Math.PI / 2 + i * Math.PI / 6; const ex = nx - Math.cos(a) * 15, ey = ny - Math.sin(a) * 15, sx = cx + Math.cos(a) * 62, sy = cy + Math.sin(a) * 62; if (s[2] === 'off') dotted(cv, sx, sy, ex, ey, MP.edge); else cv.line(Math.round(sx), Math.round(sy), Math.round(ex), Math.round(ey), s[2] === 'sel' ? MP.hot : withA(MP.moon3, 160)); });
  figure(cv, cx - 7, cy - 15, { shirt: [rgba('#5A1A1A'), rgba('#3E1010')], hair: 'black', eye: rgba('#E8402A') });
  SKILLS.forEach((s, i) => { const [nx, ny] = nodeAt(cx, cy, 190, 138, i); skillNode(cv, 'moon', nx, ny, s[1], s[2]); lab(s[0], nx, ny, s[2] === 'sel' ? MP.hot : s[2] === 'off' ? MP.mut : MP.text, s[2] === 'sel' ? 'head' : 'body', withA(MP.void, 190)); });
  say('Skills', 38, 22, { font: 'title', size: 14, c: MP.text, k: 1.3 }); say('Bloodline · Werewolves · Vampirism', 38, 42, { c: MP.mut });
  say('6 SP  ·  1,028 EP', 640, 28, { c: MP.gold, w: 230, align: 'RIGHT', font: 'head' });
  MK.slot(cv, 884, 14, 24, 'normal'); cv.blit(ICON.moon(16), 888, 18); say('x', 926, 20, { c: MP.text, font: 'head' });
  [0, 1, 2, 3, 4, 5, 6, 7, 8].forEach((i) => { const y = 54 + i * 32, sel = i === 2; MK.slot(cv, 20, y, 26, sel ? 'active' : 'normal'); cv.blit(ICON[PICON[PAGES[i]]](16), 25, y + 5); if (sel) { MK.plate(cv, 52, y + 6, 40, 14); say('Skills', 52, y + 9, { c: MP.text, w: 40, align: 'CENTER' }); } });
  ['V', 'W', 'E', 'T'].forEach((l, i) => { const y = 54 + i * 32; MK.slot(cv, 912, y + 20, 26, 'normal'); say(l, 912, y + 28, { c: MP.gold, w: 26, align: 'CENTER', font: 'head' }); });
  [['Bloodline · 6 SP', 'selected', 112], ['Werewolves · 8 pts', 'normal', 120], ['Vampirism · locked', 'disabled', 120]].forEach(([t, st, w], i, a) => { const x = 480 - 182 + a.slice(0, i).reduce((s, v) => s + v[2] + 6, 0); MK.tab(cv, x, 62, w, 14, st); say(t, x, 65, { c: st === 'disabled' ? rgba('#8A7071') : MP.text, w, align: 'CENTER' }); });
  MK.panel(cv, 120, 462, 720, 56);
  say('Venom Fang', 136, 470, { font: 'head', c: MP.hot }); pips(cv.w ? 214 : 214, 472, 3, 2, MP.hot); say('Your bite poisons vampires: +25 % venom damage per rank.', 136, 486, { c: MP.mut });
  MK.button(cv, 552, 476, 120, 16, 'normal', false); say('Put in HUD slot', 552, 480, { c: MP.text, w: 120, align: 'CENTER' });
  MK.button(cv, 678, 476, 150, 16, 'normal', true); say('Learn rank 3 · 3 SP', 678, 480, { c: MP.text, w: 150, align: 'CENTER' });
} else {
  cv.rect(0, 0, SW, SH, rgba('#0A1A2C')); for (let j = 0; j < SH; j += 2) cv.hl(0, j, SW, withA(0xFFFFFFFF, 3));
  TK.cover(cv, 8, 6, 944, 528); TK.title(cv, 14, 12, 932);
  say('《 Status · Skills 》', 24, 14, { font: 'title', size: 10, c: TP.teal, k: 1.25 }); say('Hybrid · Beastfolk · 6 SP · 1,028 EP', 300, 15, { c: TP.mut, w: 360, align: 'CENTER' });
  TK.button(cv, 840, 14, 56, 12, 'normal', false); say('Theme ▾', 840, 16, { c: TP.text, w: 56, align: 'CENTER' }); say('✕', 926, 15, { c: TP.text });
  // left list
  say('PAGES', 24, 34, { c: TP.mut });
  PAGES.forEach((p, i) => { const y = 46 + i * 20, sel = p === 'Skills'; TK.tab(cv, 22, y, 134, 17, sel ? 'selected' : 'normal'); cv.blit(ICON[PICON[p]](16), 26, y + 0); say(p, 46, y + 5, { c: sel ? TP.teal : TP.text, font: sel ? 'head' : 'body' }); });
  say('OTHER MODS', 24, 234, { c: TP.mut });
  ['Vampirism', 'Werewolves', 'Enchanted', 'Tensura'].forEach((p, i) => { const y = 246 + i * 20; TK.tab(cv, 22, y, 134, 17, 'normal'); say(p, 36, y + 5, { c: TP.gold }); });
  // centre graph
  [['Bloodline · 6 SP', 'selected', 110], ['Werewolves · 8 pts', 'normal', 120], ['Vampirism · locked', 'disabled', 120]].forEach(([t, st, w], i, a) => { const x = 170 + 20 + a.slice(0, i).reduce((s, v) => s + v[2] + 6, 0); TK.button(cv, x, 36, w, 14, st === 'selected' ? 'normal' : st, st === 'selected'); say(t, x, 39, { c: st === 'selected' ? TP.btnText : st === 'disabled' ? rgba('#5F7C8E') : TP.text, w, align: 'CENTER' }); });
  const cx = 424, cy = 292;
  hexRing(cv, cx, cy, 120, 104, 22, TP.teal, 1); hexRing(cv, cx, cy, 104, 90, 19, TP.tealD, 1); hexRing(cv, cx, cy, 74, 64, 14, withA(TP.gold, 200), 1);
  for (let i = 0; i < 48; i++) { const a = i / 48 * 2 * Math.PI; if (i % 4 === 0) cv.rect(Math.round(cx + Math.cos(a) * 60 - 1), Math.round(cy + Math.sin(a) * 52 - 1), 2, 2, TP.tealD); }
  SKILLS.forEach((s, i) => { const [nx, ny] = nodeAt(cx, cy, 168, 142, i), a = -Math.PI / 2 + i * Math.PI / 6; const ex = nx - Math.cos(a) * 15, ey = ny - Math.sin(a) * 15, sx = cx + Math.cos(a) * 58, sy = cy + Math.sin(a) * 52; if (s[2] === 'off') dotted(cv, sx, sy, ex, ey, TP.edge); else cv.line(Math.round(sx), Math.round(sy), Math.round(ex), Math.round(ey), s[2] === 'sel' ? TP.teal : TP.edgeH); });
  figure(cv, cx - 7, cy - 15, { shirt: [rgba('#2A5A8A'), rgba('#1E4268')], eye: rgba('#3FD0D6') });
  SKILLS.forEach((s, i) => { const [nx, ny] = nodeAt(cx, cy, 168, 142, i); skillNode(cv, 'tensura', nx, ny, s[1], s[2]); lab(s[0], nx, ny, s[2] === 'sel' ? TP.teal : s[2] === 'off' ? TP.mut : TP.text, s[2] === 'sel' ? 'head' : 'body', withA(TP.bg, 225)); });
  // right info pane
  TK.panel(cv, 706, 36, 232, 484); say('INFO', 720, 46, { c: TP.mut });
  say('Venom Fang', 720, 60, { font: 'title', size: 12, c: TP.teal, k: 1.3 }); pips(720, 80, 3, 2, TP.teal);
  TK.slot(cv, 720, 94, 30, 'normal'); cv.blit(ICON.fang(16), 727, 100);
  say('Your bite poisons vampires: +25 % venom damage per rank.', 758, 94, { c: TP.text, w: 170, lh: 11 });
  [['NEXT RANK', '3 SP'], ['NEEDS', 'Hybrid rung 3 ✓'], ['SOURCE', 'Supernatural Bloodlines']].forEach(([a, b], i) => { say(a, 720, 140 + i * 28, { c: TP.mut }); say(b, 720, 151 + i * 28, { c: TP.text }); });
  TK.divider(cv, 720, 228, 204);
  say('RANKS', 720, 240, { c: TP.mut });
  [['Rank 1', '+25 % venom damage', 1], ['Rank 2', '+50 % venom damage', 1], ['Rank 3', '+75 %, poison spreads', 0]].forEach(([a, b, d], i) => { const y = 254 + i * 14; cv.rect(720, y + 1, 6, 6, d ? TP.teal : 0); cv.box(720, y + 1, 6, 6, TP.teal); say(a, 732, y, { c: TP.text }); say(b, 772, y, { c: d ? TP.mut : TP.text }); });
  TK.divider(cv, 720, 306, 204); say('LEARNED IN BLOODLINE', 720, 318, { c: TP.mut });
  ['Keen Fangs', 'Thick Hide', 'Steady Thirst', 'Blood Scent', 'Iron Will'].forEach((s, i) => { say(s, 732, 332 + i * 13, { c: TP.text }); cv.rect(720, 334 + i * 13, 5, 5, TP.good); });
  TK.button(cv, 720, 456, 204, 16, 'normal', false); say('Put in HUD slot', 720, 460, { c: TP.text, w: 204, align: 'CENTER' });
  TK.button(cv, 720, 478, 204, 16, 'normal', true); say('Learn rank 3 · 3 SP', 720, 482, { c: TP.btnText, w: 204, align: 'CENTER' });
}
cv.emit(fr, 0, 0, 2, 'screen');
for (const [s, x, y, o] of tx) T(fr, s, x, y, { S: 2, ...o });
return done({ id: fr.id, rects: cv.rects().length, texts: tx.length });
