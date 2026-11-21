// HUD mockup. Needs THEME and MODE ('super' | 'tensura').
const K = KF[THEME], isT = MODE === 'tensura';
const HC = { grimoire: { text: GP.ink, mut: GP.mut, head: GP.red, plateText: GP.paH, fill: GP.redH, mark: rgba('#D8B24A') }, moon: { text: MP.text, mut: MP.mut, head: MP.hot, plateText: MP.text, fill: MP.hot, mark: MP.gold }, tensura: { text: TP.text, mut: TP.mut, head: TP.teal, plateText: TP.text, fill: TP.teal, mark: TP.teal } }[THEME];
const FX = { grimoire: 0, moon: 2000, tensura: 4000 }[THEME], FY = isT ? 5600 : 4400;
const fname = 'HUD · ' + K.name + ' · ' + (isT ? 'Tensura mode' : 'Supernatural mode');
for (const n of PAGE.children.filter((c) => c.name === fname)) n.remove();
const fr = figma.createFrame(); fr.name = fname; fr.x = FX; fr.y = FY; fr.resize(1920, 1080); fr.clipsContent = true; fr.fills = []; PAGE.appendChild(fr);
const cv = new Cv(SW, SH), tx = [];
world(cv); vanillaBars(cv, 4);
const say = (s, x, y, o = {}) => tx.push([s, x, y, o]);

// status card, top left
{
  const x = 6, y = 6, w = 124, h = 54;
  K.panel(cv, x, y, w, h); faceBox(cv, K, x + 5, y + 5, null, { skin: 'fair', hair: 'brown' });
  K.bar(cv, x + 30, y + 17, 52, 6, 1); K.bar(cv, x + 30, y + 28, 52, 6, 0.8);
  say('Dev', x + 30, y + 4, { c: HC.head, font: 'head' }); say('38/38', x + 86, y + 15, { c: HC.text }); say('60/60', x + 86, y + 26, { c: HC.text });
  say(isT ? 'Slime · rank 3' : 'Alpha · rung 3', x + 6, y + 40, { c: HC.mut });
}
// moon clock, top right
{
  const x = 926, y = 6; K.slot(cv, x, y, 24, 'normal'); cv.blit(ICON.moon(16), x + 4, y + 4);
  const w = 118, px = 960 - 6 - w; K.plate(cv, px, 34, w, 14); say('Forced shift 1:40', px + 6, 37, { c: HC.plateText });
}
// loadout popups (Alt held)
const names = isT ? [['R', 'Predation', 'ready'], ['F', 'Sense', 'on'], ['V', 'Black Flame', '4 s'], ['G', 'Mimicry', 'ready'], ['X', 'Great Sage', 'locked'], ['C', 'Storm', 'ready']] : [['R', 'Bite', 'ready'], ['F', 'Thirst', 'on'], ['V', 'Moon Heart', '4 s'], ['G', 'Pack Howl', 'ready'], ['X', 'Alpha Roar', 'locked'], ['C', 'Silver Tongue', 'ready']];
[0, 1].forEach((g) => {
  const x = g ? 566 : 270, y = 452, w = 126, h = 44; K.panel(cv, x, y, w, h);
  for (let i = 0; i < 3; i++) { const [k, nm, st] = names[g * 3 + i]; say(k, x + 8, y + 5 + i * 12, { c: HC.head, font: 'head' }); say(nm, x + 20, y + 5 + i * 12, { c: HC.text }); say(st, x + w - 8 - st.length * 5, y + 5 + i * 12, { c: st === 'locked' ? HC.mut : st === 'ready' ? (THEME === 'grimoire' ? GP.good : MP.good) : HC.mut }); }
});
// mode pill
{
  const w = isT ? 148 : 160, x = 480 - w / 2; K.plate(cv, x, 478, w, 14);
  if (isT) TK.gem(cv, x + 6, 481, 8, TP.teal); else if (THEME === 'grimoire') GK.gem(cv, x + 6, 481, 9, GP.wax); else MK.gem(cv, x + 6, 481, 8, MP.red);
  say(isT ? 'TENSURA  ·  Caps again' : 'SUPERNATURAL  ·  Caps', x + 20, 481, { c: HC.plateText });
}
// slots
const sq = (x, y, n, active) => { // Tensura mode: square tile in the theme's material, teal mark on the corner
  const r = THEME === 'grimoire' ? ramp(active ? GP.br : GP.bord, active ? GP.brHH : GP.paH, GP.pa, GP.paS) : THEME === 'moon' ? ramp(active ? MP.hot : MP.edge, MP.edgeH, MP.card, MP.cardD) : ramp(active ? TP.tealH : TP.teal, TP.edgeH, TP.panel, TP.panelD);
  cv.bevel(rr(n, n, 2), r, x, y); cv.bevel(rr(n - 6, n - 6, 1), ramp(0, 0xFF1A1418, rgba('#150F14'), 0xFF1A1418), x + 3, y + 3);
  const d = new Cv(7, 7); d.poly([[3.5, 0], [7, 3.5], [3.5, 7], [0, 3.5]], 1); cv.bevel(d, ramp(TP.tealD, TP.tealH, TP.teal, rgba('#2AA0A8')), x - 3, y + n - 4);
};
const icons = isT ? ['gem', 'sigil', 'scroll', 'crown', 'moon', 'paw'] : ['fang', 'drop', 'moon', 'paw', 'crown', 'sigil'];
const tint = (i) => i;
const buildSlots = (cv, say, grp, bx, by) => {
  if (!isT && grp === 0) { // blood ring round Bite, drawn first so the slot sits on top of it
    const cx = bx + 12, cy = by + 12, r = 16, lit = Math.round(72 * 0.62);
    for (let i = 0; i < 72; i++) { const a = i / 72 * 2 * Math.PI, x = Math.round(cx + Math.sin(a) * r - 1), y = Math.round(cy - Math.cos(a) * r - 1); cv.rect(x, y, 2, 2, i < lit ? rgba('#D8182E') : withA(rgba('#D8182E'), 80)); }
  }
  for (let i = grp * 3; i < grp * 3 + 3; i++) {
  const j = i % 3, x = bx + j * 28, y = by, active = i === 1, locked = names[i][2] === 'locked';
  if (isT) sq(x, y, 24, active); else K.slot(cv, x, y, 24, active ? 'active' : 'normal');
  cv.blit(ICON[icons[i]](16), x + 4, y + 4 + (isT ? 0 : 0));
  if (names[i][2] === '4 s') { pie(cv, x + 12, y + 12, 11, 0.62, withA(0xFF000000, 175)); }
  if (locked) { cv.rect(x + 4, y + 4, 16, 16, withA(0xFF000000, 150)); cv.rect(x + 9, y + 11, 6, 5, rgba('#C8C8D0')); cv.box(x + 10, y + 7, 4, 5, rgba('#C8C8D0')); }
  // key seal
  if (THEME === 'grimoire') GK.seal(cv, x - 4, y - 4, 11, 'normal'); else if (THEME === 'moon') cv.bevel(discMask(11), ramp(MP.redDD, MP.hot, MP.red, MP.redD), x - 4, y - 4); else TK.keyseal(cv, x - 4, y - 4);
  say(names[i][0], x - 4, y - 4 + 1, { c: THEME === 'tensura' ? TP.btnText : GP.paH, w: 11, align: 'CENTER' });
  if (names[i][2] === '4 s') say('4s', x, y + 8, { c: 0xFFFFFFFF, w: 24, align: 'CENTER', shadow: 0xFF000000 });
  // mastery bar under the slot
  const mf = [0.8, 0.55, 0.3, 0.9, 0.1, 0.6][i]; cv.rect(x, y + 26 - 1, 24, 4, 0xFF0E0A0C); cv.rect(x + 1, y + 26, Math.round(22 * mf), 2, HC.fill);
}
};
buildSlots(cv, say, 0, 301, 508); buildSlots(cv, say, 1, 579, 508);
if (isT) { say('preset 2', 301, 496, { c: 0xFFFFFFFF, shadow: 0xFF000000 }); say('preset 3', 579, 496, { c: 0xFFFFFFFF, shadow: 0xFF000000 }); }
cv.emit(fr, 0, 0, 2, 'screen');
for (const [s, x, y, o] of tx) T(fr, s, x, y, { S: 2, ...o });
// zoomed detail of both slot groups (6 px per texel), so the craft can be judged
const ZS = 6, zt = [];
[0, 1].forEach((grp) => {
  const zc = new Cv(92, 44); zc.rect(0, 0, 92, 44, rgba('#5C9A3C')); zc.rect(0, 0, 92, 1, rgba('#82BE52'));
  buildSlots(zc, (s, x, y, o) => zt.push([grp, s, x, y, o]), grp, 8, 8);
  const ox = grp ? 1010 : 400, oy = 110; zc.emit(fr, ox / ZS, oy / ZS, ZS, 'detail ' + grp);
  for (const [g, s, x, y, o] of zt.filter((z) => z[0] === grp)) T(fr, s, ox / ZS + x, oy / ZS + y, { S: ZS, ...o, size: o.size || 8 });
  box(fr, ox, oy - 34, 92 * ZS, 26, 0x00000000);
  label(fr, 'Detail × 3: ' + (grp ? 'right group' : 'left group') + ' · keys, icons 16 px, mastery bar' + (grp === 0 && !isT ? ', blood ring' : ''), ox, oy - 28, 0xFF1A1A22, 15, true);
});
return done({ id: fr.id, rects: cv.rects().length });
