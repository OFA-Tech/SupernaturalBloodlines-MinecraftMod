// Home page mockup. Needs THEME.
const K = KF[THEME], C = HCOL[THEME];
const FX = { grimoire: 0, moon: 2000, tensura: 4000 }[THEME], FY = 6800;
const fname = 'Home · ' + K.name;
for (const n of PAGE.children.filter((c) => c.name === fname)) n.remove();
const fr = figma.createFrame(); fr.name = fname; fr.x = FX; fr.y = FY; fr.resize(1920, 1080); fr.clipsContent = true; fr.fills = []; PAGE.appendChild(fr);
const cv = new Cv(SW, SH), tx = [];
const say = (s, x, y, o = {}) => tx.push([s, x, y, o]);
const tw = (s) => Math.round(s.length * 5.3);
const PAGES = ['Home', 'Path', 'Skills', 'Quests', 'Bloodline', 'Pack', 'Guide', 'Loadout', 'Settings', 'Admin'];
const PICON = { Home: 'home', Path: 'moon', Skills: 'fang', Quests: 'scroll', Bloodline: 'drop', Pack: 'paw', Guide: 'book', Loadout: 'crown', Settings: 'gear', Admin: 'shield' };
const gold = { grimoire: GP.br, moon: MP.gold, tensura: TP.gold }[THEME];
const paperC = K.paper;
const panelBox = (x, y, w, h, kind) => { if (THEME === 'grimoire') { if (kind === 'card') K.inset(cv, x, y, w, h); } else if (kind === 'card') K.inset(cv, x, y, w, h); else K.panel(cv, x, y, w, h); };
const btn = (x, y, w, text, pri, st = 'normal') => { const o = K.button(cv, x, y, w, 14, st, pri); say(text, x, y + 3 + o.dy, { c: pri && st !== 'disabled' ? C.onPri : o.text, w, align: 'CENTER' }); return w; };
const chip = (x, y, text, kind) => { const w = tw(text) + 8, r = kind === 'good' ? ramp(0, withA(C.good, 90), withA(C.good, 60), withA(C.good, 40)) : kind === 'accent' ? { grimoire: ramp(GP.red, rgba('#F0C6C2'), rgba('#E8B4AE'), rgba('#D49A94')), moon: ramp(MP.red, rgba('#5A1E22'), rgba('#43161B'), rgba('#33100F')), tensura: ramp(TP.warn, rgba('#4A2230'), rgba('#3A1A26'), rgba('#2E1420')) }[THEME] : { grimoire: ramp(GP.paDD, GP.paH, rgba('#E2D6B0'), GP.paS), moon: ramp(MP.edge, MP.cardH, MP.card, MP.cardD), tensura: ramp(TP.edgeH, TP.panelH, TP.lit, TP.panel) }[THEME]; cv.bevel(rr(w, 11, 2), r, x, y); say(text, x + 4, y + 2, { c: kind === 'good' ? (THEME === 'grimoire' ? GP.good : C.good) : kind === 'accent' ? (THEME === 'grimoire' ? GP.red : THEME === 'moon' ? MP.hot : TP.warn) : C.text }); return w; };
const dot = (x, y, on) => { cv.disc(x, y, 2.5, 2.5, on ? C.good : { grimoire: GP.paDD, moon: MP.edge, tensura: TP.edgeH }[THEME]); };
const heads = [['fair', 'brown', 0], ['tan', 'black', 1], ['fair', 'red', 0], ['deep', 'black', 1], ['tan', 'brown', 0], ['fair', 'blond', 1]];

const youCard = (x, y, w, h) => {
  panelBox(x, y, w, h, 'flat'); const p = 8;
  faceBox(cv, K, x + p, y + p + 6, null, { skin: 'fair', hair: 'brown' }); say('Dev', x + p + 26, y + p + 5, { font: 'head', c: C.head });
  let cx = x + p + 26; cx += chip(cx, y + p + 17, 'Alpha', 'accent') + 3; chip(cx, y + p + 17, 'Werewolf · born', 'neutral');
  say('Level 38 · 6 SP · 1,028 EP', x + p, y + p + 36, { c: C.text }); say('Moon control: Full (you are an Alpha). Anchor: Tyler.', x + p, y + p + 48, { c: C.mut, w: w - 2 * p, lh: 10 });
  btn(x + p, y + h - 22, 90, 'Open my profile', false);
};
const worldCard = (x, y, w, h) => {
  panelBox(x, y, w, h, 'flat'); const p = 8;
  cv.bevel(discMask(16), ramp(rgba('#7A4A40'), rgba('#FFF1EA'), rgba('#F6C9BE'), rgba('#D98F86')), x + p, y + p + 5); say('The world now', x + p, y + 3, { c: C.mut, font: 'body' });
  say('Full moon in 2 days', x + p + 22, y + p + 5, { font: 'head', c: C.text }); say('Waxing gibbous tonight', x + p + 22, y + p + 16, { c: C.mut });
  say('No eclipse. Eclipses are 3 % of full moons.', x + p, y + p + 32, { c: C.mut, w: w - 2 * p, lh: 10 }); btn(x + p, y + h - 22, 90, 'Guide: the moon', false);
};
const waiting = (x, y, w, gap) => {
  const items = [['Hayley challenges you', 'Leadership duel · asked 12 s ago', '1:48', 0.88, ['Accept', 'Decline'], 0], ['Klaus calls you', 'Your sire · come to him', '0:22', 0.35, ['Accept'], 1], ['You compelled a villager', 'Choose what they do · 30 s left', '0:28', 0.93, ['Discount', 'Follow'], 1]];
  items.forEach(([t, s, tm, f, bs, hot], i) => {
    const h = 58, yy = y + i * (h + gap); panelBox(x, yy, w, h, 'card'); const p = 6;
    face(cv, x + p, yy + p, { skin: heads[i][0], hair: heads[i][1] }); say(t, x + p + 22, yy + p - 1, { font: 'head', c: C.text }); say(s, x + p + 22, yy + p + 10, { c: C.mut, w: w - 70, lh: 10 });
    say(tm, x + w - p - 40, yy + p - 1, { c: hot ? C.warn : C.text, w: 40, align: 'RIGHT', font: 'head' });
    K.bar(cv, x + p, yy + p + 24, w - 2 * p, 4, f);
    let bx = x + p; bs.forEach((b, j) => { bx += btn(bx, yy + h - 20, tw(b) + 18, b, j === 0) + 4; });
  });
};
const nextCard = (x, y, w, h) => {
  panelBox(x, y, w, h, 'flat'); const p = 8;
  say('Next step', x + p, y + 5, { c: C.mut }); say('Tracked: Feed 28 times', x + p, y + 16, { font: 'head', c: C.text }); K.bar(cv, x + p, y + 28, w - 2 * p, 5, 0.5); say('14 of 28 · rung 3, Hunter', x + p, y + 37, { c: C.mut });
  chip(x + p, y + 50, '3 tasks ready', 'good'); btn(x + p + 78, y + 48, 70, 'Claim all', true); btn(x + p + 152, y + 48, 54, 'Quests', false);
  say('+ 1 skill you can learn', x + p, y + 70, { c: C.text }); btn(x + w - p - 46, y + 66, 46, 'Skills', false);
};
const peopleCard = (x, y, w, h) => {
  panelBox(x, y, w, h, 'flat'); const p = 8;
  say('People', x + p, y + 5, { c: C.mut });
  heads.forEach(([s, hr, hl], i) => { const fx = x + p + i * 21; if (i === 0) { cv.rect(fx + 4, y + 14, 8, 2, gold); cv.rect(fx + 5, y + 12, 2, 2, gold); cv.rect(fx + 9, y + 12, 2, 2, gold); } cv.box(fx - 1, y + 17, 18, 18, i === 0 ? gold : { grimoire: GP.bord, moon: MP.edge, tensura: TP.edge }[THEME]); face(cv, fx, y + 18, { skin: s, hair: hr }); dot(fx + 8, y + 41, i !== 3 && i !== 5); });
  say("Hayley's pack: 6 members, 4 online", x + p, y + 48, { c: C.text }); say('Your sire: Klaus (Original). You sired 8.', x + p, y + 59, { c: C.mut, w: w - 2 * p, lh: 10 });
  btn(x + p, y + h - 22, 50, 'Pack', false); btn(x + p + 54, y + h - 22, 64, 'Bloodline', false);
};
const actions = (x, y, w, h) => {
  panelBox(x, y, w, h, 'flat'); const p = 8; say('What you can do now', x + p, y + 5, { c: C.mut });
  const items = [['Call your sired', 'Ready · calls your 8', 'normal', true], ['Alpha Roar', 'Cooldown · 41 s', 'disabled', false], ['Invite to pack', 'You lead no pack', 'disabled', false], ['Give spark (heal)', 'Nobody in 4 blocks', 'disabled', false]];
  const bw = Math.floor((w - 2 * p - 3 * 6) / 4);
  items.forEach(([t, s, st, pri], i) => { const bx = x + p + i * (bw + 6); btn(bx, y + 18, bw, t, pri, st); say(s, bx, y + 36, { c: C.mut, w: bw, align: 'CENTER' }); });
};
const notices = (x, y, w) => {
  say('NOTICES', x, y, { c: C.mut }); const wc = { grimoire: GP.red, moon: MP.gold, tensura: TP.gold }[THEME];
  cv.poly([[x + 3, y + 12], [x + 7, y + 19], [x - 1, y + 19]], wc); say('The Original of your line lies desiccated.', x + 12, y + 12, { c: C.text });
  cv.rect(x + 2, y + 24, 3, 3, wc); cv.rect(x + 1, y + 25, 5, 1, wc); cv.rect(x + 3, y + 23, 1, 5, wc); say('You took the spark. You hold 2 of 5.', x + 12, y + 24, { c: C.text });
};
const rail = (px) => {};

if (THEME === 'moon') {
  MK.sky(cv, 0, 0, SW, SH);
  say('Home', 94, 14, { font: 'title', size: 14, c: MP.text, k: 1.3 }); say('Who you are, what waits for you, and where to go next', 94, 36, { c: MP.mut });
  say('6 SP  ·  1,028 EP', 640, 24, { c: MP.gold, w: 230, align: 'RIGHT', font: 'head' }); MK.slot(cv, 884, 12, 24, 'normal'); cv.blit(ICON.moon(16), 888, 16); say('x', 926, 20, { c: MP.text, font: 'head' });
  PAGES.forEach((p, i) => { const y = 14 + i * 30, sel = i === 0; MK.slot(cv, 18, y, 26, sel ? 'active' : 'normal'); cv.blit(ICON[PICON[p]](16), 23, y + 5); if (sel) { MK.plate(cv, 48, y + 6, 36, 14); say('Home', 48, y + 9, { c: MP.text, w: 36, align: 'CENTER' }); cv.disc(40, y + 2, 5, 5, MP.red); say('3', 35, y - 3, { c: MP.text, w: 10, align: 'CENTER' }); } });
  ['V', 'W', 'E', 'T'].forEach((l, i) => { const y = 54 + i * 32; MK.slot(cv, 916, y, 26, 'normal'); say(l, 916, y + 8, { c: MP.gold, w: 26, align: 'CENTER', font: 'head' }); });
  youCard(94, 62, 250, 126); worldCard(94, 194, 250, 96);
  waiting(350, 62, 290, 6); say('Waiting for you', 358, 52, { c: MP.mut });
  nextCard(646, 62, 250, 94); peopleCard(646, 162, 250, 100);
  actions(94, 436, 802, 62);
  notices(358, 304, 280);
} else if (THEME === 'grimoire') {
  cv.rect(0, 0, SW, SH, rgba('#1E0C0C')); GK.cover(cv, 24, 16, 892, 500);
  const LP = [38, 30, 428, 472], RP = [476, 30, 428, 472];
  for (const [x, y, w, h] of [LP, RP]) { for (let k = 3; k >= 1; k--) cv.rect(x + (x < 400 ? -k : 0), y + k, w + (x < 400 ? 0 : k), h, [GP.paS, GP.paD, rgba('#AE9868')][k - 1]); cv.bevel(rr(w, h, 2), GR.page, x, y); }
  for (let i = 0; i < 10; i++) { cv.rect(LP[0] + LP[2] - 1 - i, LP[1] + 1, 1, LP[3] - 2, withA(rgba('#46280a'), 80 - i * 8)); cv.rect(RP[0] + i, RP[1] + 1, 1, RP[3] - 2, withA(rgba('#46280a'), 80 - i * 8)); }
  PAGES.forEach((p, i) => { const y = 36 + i * 22, sel = i === 0; GK.tab(cv, 902, y, 54, 18, sel ? 'selected' : 'normal'); say(p, sel ? 907 : 910, y + 5, { c: GP.paH }); });
  [['T', 0], ['E', 1], ['W', 2], ['V', 3]].forEach(([l, i]) => { const x = 74 + i * 38; cv.vl(x + 8, 516, 8, GP.brS); GK.seal(cv, x, 522, 16, 'normal'); say(l, x, 526, { c: GP.paH, w: 16, align: 'CENTER' }); });
  say('Home', 56, 40, { font: 'title', size: 14, c: GP.red, k: 1.3 }); say('Who you are, what waits for you, and where to go next', 56, 62, { c: GP.mut }); GK.divider(cv, 56, 76, 372);
  say('You', 56, 84, { font: 'head', c: GP.red }); const L = { x: 56, w: 372 };
  faceBox(cv, K, 56, 98, null, { skin: 'fair', hair: 'brown' }); say('Dev', 82, 98, { font: 'head', c: GP.ink }); chip(82, 110, 'Alpha', 'accent'); chip(116, 110, 'Werewolf · born', 'neutral');
  say('Level 38 · 6 SP · 1,028 EP', 56, 126, { c: GP.ink }); say('Moon control: Full (you are an Alpha). Anchor: Tyler.', 56, 138, { c: GP.mut }); btn(56, 152, 96, 'Open my profile', false);
  GK.divider(cv, 56, 180, 372); say('Next step', 56, 188, { font: 'head', c: GP.red });
  say('Tracked: Feed 28 times', 56, 202, { c: GP.ink, font: 'head' }); K.bar(cv, 56, 214, 372, 5, 0.5); say('14 of 28 · rung 3, Hunter', 56, 224, { c: GP.mut });
  chip(56, 240, '3 tasks ready', 'good'); btn(136, 238, 70, 'Claim all', true); btn(210, 238, 54, 'Quests', false); say('+ 1 skill you can learn', 56, 262, { c: GP.ink }); btn(382, 258, 46, 'Skills', false);
  GK.divider(cv, 56, 284, 372); say('The world now', 56, 292, { font: 'head', c: GP.red });
  cv.bevel(discMask(16), ramp(rgba('#4E0C10'), rgba('#D2494A'), rgba('#9A1C22'), rgba('#6E1217')), 56, 306); say('Full moon in 2 days', 80, 306, { font: 'head', c: GP.ink }); say('Waxing gibbous tonight', 80, 318, { c: GP.mut });
  say('No eclipse. Eclipses are 3 % of full moons.', 56, 336, { c: GP.mut }); btn(56, 350, 90, 'Guide: the moon', false);
  GK.divider(cv, 56, 378, 372); say('What you can do now', 56, 386, { font: 'head', c: GP.red });
  btn(56, 402, 110, 'Call your sired', true); btn(172, 402, 90, 'Alpha Roar', false, 'disabled'); btn(268, 402, 92, 'Invite to pack', false, 'disabled'); say('Ready: calls the 8 you sired  ·  Alpha Roar: 41 s left', 56, 420, { c: GP.mut });
  say('- 3 -', 230, 482, { c: GP.mut });
  const rx = 496; say('Waiting for you', rx, 40, { font: 'head', c: GP.red }); chip(840, 38, '3', 'accent'); GK.divider(cv, rx, 54, 372);
  waiting(rx, 62, 372, 4); notices(rx, 308, 372);
  say('People', rx, 352, { font: 'head', c: GP.red }); GK.divider(cv, rx, 365, 372);
  heads.forEach(([s, hr], i) => { const fx = rx + 4 + i * 24; cv.box(fx - 1, 373, 18, 18, i === 0 ? gold : GP.bord); face(cv, fx, 374, { skin: s, hair: hr }); dot(fx + 8, 398, i !== 3 && i !== 5); });
  say("Hayley's pack: 6 members, 4 online. Open a head to see a profile.", rx, 408, { c: GP.mut, w: 372, lh: 10 }); say('Your sire: Klaus (Original). You sired 8.', rx, 422, { c: GP.mut });
  btn(rx, 440, 50, 'Pack', false); btn(rx + 54, 440, 64, 'Bloodline', false);
  say('- 4 -', 680, 482, { c: GP.mut });
} else {
  cv.rect(0, 0, SW, SH, rgba('#0A1A2C')); for (let j = 0; j < SH; j += 2) cv.hl(0, j, SW, withA(0xFFFFFFFF, 3));
  TK.cover(cv, 8, 6, 944, 528); TK.title(cv, 14, 12, 932);
  say('《 Status · Home 》', 24, 14, { font: 'title', size: 10, c: TP.teal, k: 1.25 }); say('Werewolf · Alpha · 6 SP · 1,028 EP', 300, 15, { c: TP.mut, w: 360, align: 'CENTER' });
  TK.button(cv, 840, 14, 56, 12, 'normal', false); say('Theme ▾', 840, 16, { c: TP.text, w: 56, align: 'CENTER' }); say('✕', 926, 15, { c: TP.text });
  [['ME', ['Home', 'Path', 'Skills', 'Quests']], ['PEOPLE', ['Bloodline', 'Pack']], ['HELP', ['Guide', 'Loadout']], ['MORE', ['Settings', 'Admin']]].reduce((y, [g, ps]) => { say(g, 24, y, { c: TP.mut }); y += 11; ps.forEach((p) => { const sel = p === 'Home'; TK.tab(cv, 22, y, 134, 16, sel ? 'selected' : 'normal'); cv.blit(ICON[PICON[p]](16), 26, y); say(p, 46, y + 4, { c: sel ? TP.teal : TP.text, font: sel ? 'head' : 'body' }); if (sel) { cv.disc(142, y + 8, 5, 5, TP.warn); say('3', 137, y + 3, { c: TP.btnText, w: 10, align: 'CENTER' }); } y += 18; }); return y + 6; }, 34);
  say('OTHER MODS', 24, 396, { c: TP.mut }); ['Vampirism', 'Werewolves', 'Enchanted', 'Tensura'].forEach((p, i) => { const y = 408 + i * 20; TK.tab(cv, 22, y, 134, 17, 'normal'); say(p, 36, y + 5, { c: TP.gold }); });
  youCard(166, 36, 232, 118); worldCard(166, 160, 232, 98); actions(166, 264, 460, 62); notices(172, 340, 440);
  nextCard(404, 36, 222, 94); peopleCard(404, 136, 222, 100);
  say('Waiting for you', 640, 38, { font: 'head', c: TP.text }); chip(740 + 4, 36, '3', 'accent');
  waiting(634, 50, 306, 4);
}
cv.emit(fr, 0, 0, 2, 'screen');
for (const [s, x, y, o] of tx) T(fr, s, x, y, { S: 2, ...o });
return done({ id: fr.id, rects: cv.rects().length, texts: tx.length });
