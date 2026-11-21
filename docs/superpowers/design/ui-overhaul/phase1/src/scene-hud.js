// HUD mockup, matching PR #56 (HudLayout, SlotBadge, MasteryBar, SlotLabel). Needs THEME and MODE ('super' | 'tensura').
const K = KF[THEME], isT = MODE === 'tensura';
const HC = { grimoire: { text: GP.ink, mut: GP.mut, head: GP.red, plateText: GP.paH, fill: GP.redH, mark: rgba('#D8B24A') }, moon: { text: MP.text, mut: MP.mut, head: MP.hot, plateText: MP.text, fill: MP.hot, mark: MP.gold }, tensura: { text: TP.text, mut: TP.mut, head: TP.teal, plateText: TP.text, fill: TP.teal, mark: TP.teal } }[THEME];
const FX = { grimoire: 0, moon: 2000, tensura: 4000 }[THEME], FY = isT ? 5600 : 4400;
const fname = 'HUD · ' + K.name + ' · ' + (isT ? 'Tensura mode' : 'Supernatural mode');
for (const n of PAGE.children.filter((c) => c.name === fname)) n.remove();
const fr = figma.createFrame(); fr.name = fname; fr.x = FX; fr.y = FY; fr.resize(1920, 1080); fr.clipsContent = true; fr.fills = []; PAGE.appendChild(fr);
const cv = new Cv(SW, SH), tx = [];
world(cv); vanillaBars(cv, 4);
const say = (s, x, y, o = {}) => tx.push([s, x, y, o]);
const meas = (s, font) => { const t = T(fr, s, 0, 0, { S: 2, font: font || 'body' }); const w = t.width / 2; t.remove(); return Math.ceil(w); };

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
// vanilla offhand slot (left of the hotbar) and attack indicator (right of it): the columns stand outside both
cv.rect(360, 518, 29, 22, rgba('#8B8B8B')); cv.rect(361, 519, 27, 20, rgba('#2A2A2A')); cv.box(360, 518, 29, 22, rgba('#1A1A1A')); cv.box(366, 520, 22, 18, rgba('#5A5A5A'));
cv.line(581, 523, 589, 531, rgba('#C8C8D0')); cv.line(581, 524, 588, 531, rgba('#9A9AA4')); cv.rect(578, 534, 16, 2, rgba('#1E1E1E')); cv.rect(578, 534, 10, 2, rgba('#E0E0E0'));

// ---- the six slots: HudLayout.slots at 960 x 540: left column x = 480 - 91 - 29 - 4 - 22, right column x = 480 + 91 + 6 + 18 + 4, rows 24 apart from y = 540 - 3 - 70
const SLOT = 22, LABEL_H = 20, LABEL_AIR = 3, PAD = 4;
const COLX = [334, 599], ROW0 = 467;
const KEYS = ['R', 'F', 'V', 'Z', 'X', 'C'];
// [name, state (cooldown or blocked reason), mastery 0..1 (Tensura mode), description, kind]
const D = isT ? [
  ['Predation', null, 0.8, 'Devour and learn'], ['Sense', null, 0.55, 'Feel life nearby'], ['Black Flame', '4 s', 0.3, 'Burn what you hit'],
  ['Mimicry', null, 1, 'Copy a shape'], ['Great Sage', 'Needs 200 magicules', 0.1, 'Think and analyse'], ['Storm', null, 0.6, 'Call a lightning storm'],
] : [
  ['Bite', null, null, 'Feed on who you face'], ['Thirst', null, null, 'Smell blood nearby'], ['Moon Heart', '4 s', null, 'Calm the moon pull'],
  ['Pack Howl', null, null, 'Call your pack to you'], ['Alpha Roar', 'Needs the Alpha rank', null, 'Frighten foes'], ['Silver Tongue', null, null, 'Charm a villager\'s price'],
];
const ICONS = isT ? ['gem', 'sigil', 'scroll', 'crown', 'moon', 'paw'] : ['fang', 'drop', 'moon', 'paw', 'crown', 'sigil'];
const ACTIVE = 1;
const line2 = ([nm, state, m, desc]) => [state, m === null ? null : (m >= 1 ? 'Mastered' : Math.round(m * 100) + '%'), desc].filter(Boolean).join(' · ');
const barColor = (i) => (D[i][2] >= 1 ? 0xFFFFD36B : i === 4 ? 0xFF8E8AA8 : { grimoire: 0xFFE0554A, moon: 0xFFE2493F, tensura: 0xFF3FD0D6 }[THEME]); // MasteryBar.color: gold / learning / progress

const squareTile = (c, x, y, active) => { // Tensura mode: a square in the theme's material, a diamond mark on the top right corner
  const r = THEME === 'grimoire' ? ramp(active ? GP.br : GP.bord, active ? GP.brHH : GP.paH, GP.pa, GP.paS) : THEME === 'moon' ? ramp(active ? MP.hot : MP.edge, MP.edgeH, MP.card, MP.cardD) : ramp(active ? TP.tealH : TP.teal, TP.edgeH, TP.panel, TP.panelD);
  c.bevel(rr(SLOT, SLOT, 2), r, x, y); c.bevel(rr(SLOT - 6, SLOT - 6, 1), ramp(0, 0xFF1A1418, rgba('#150F14'), 0xFF1A1418), x + 3, y + 3);
};
const labelBox = (c, x, y, w, h) => { if (THEME === 'grimoire') c.bevel(rr(w, h, 1), GR.page, x, y); else K.panel(c, x, y, w, h); };

const drawGroup = (c, say2, grp, bx, by, S) => {
  if (!isT && grp === 0) { // blood ring round Bite, drawn first so its slot sits on top of it
    const cx = bx + 11, cy = by + 11, r = 15, lit = Math.round(72 * 0.62);
    for (let i = 0; i < 72; i++) { const a = i / 72 * 2 * Math.PI, x = Math.round(cx + Math.sin(a) * r - 1), y = Math.round(cy - Math.cos(a) * r - 1); c.rect(x, y, 2, 2, i < lit ? rgba('#D8182E') : withA(rgba('#D8182E'), 80)); }
  }
  for (let j = 0; j < 3; j++) {
    const i = grp * 3 + j, x = bx, y = by + j * (SLOT + 2), d = D[i], active = i === ACTIVE, cool = d[1] === '4 s', blocked = d[1] && !cool, lift = isT ? 5 : 0;
    if (isT) squareTile(c, x, y, active); else K.slot(c, x, y, SLOT, active ? 'active' : 'normal');
    c.blit(ICON[ICONS[i]](16), x + 3, y + 3);
    if (cool) pie(c, x + 11, y + 11, 10, 0.62, withA(0xFF000000, 175));
    if (blocked) { c.rect(x + 3, y + 3, 16, 16, withA(0xFF000000, 150)); c.rect(x + 8, y + 10, 6, 5, rgba('#C8C8D0')); c.box(x + 9, y + 6, 4, 5, rgba('#C8C8D0')); }
    // key badge (SlotBadge.key): Grimoire seal bottom right, Blood Moon plate bottom centre, Tensura tab top left; lifted over the mastery bar
    const kw = 6;
    if (THEME === 'grimoire') { const bw = 11, ky = y + SLOT - (lift === 0 ? 12 : lift + 12); GK.seal(c, x + SLOT - bw - 1, ky, bw, 'normal'); say2(KEYS[i], x + SLOT - bw - 1, ky + 1, { c: GP.paH, w: bw, align: 'CENTER' }, 1); }
    else if (THEME === 'moon') { const ky = y + SLOT - 10 - lift; c.bevel(rr(kw + 4, 10, 1), ramp(MP.edge, MP.cardH, MP.cardD, MP.void), x + (SLOT - kw) / 2 - 2, ky); say2(KEYS[i], x + (SLOT - kw) / 2 - 2, ky + 1, { c: MP.text, w: kw + 4, align: 'CENTER' }, 1); }
    else { c.bevel(notch(kw + 4, 11, 2, true, false, false, true), ramp(TP.tealD, TP.tealH, TP.teal, rgba('#2AA0A8')), x - 2, y - 3); say2(KEYS[i], x - 2, y - 2, { c: TP.btnText, w: kw + 4, align: 'CENTER' }, 1); }
    if (cool) say2('4s', x, THEME === 'tensura' ? y + SLOT - 10 - lift : y + 2, { c: 0xFFFFFFFF, w: SLOT, align: 'CENTER', shadow: 0xFF000000 }, 1);
    if (isT) { // MasteryBar: a 4 px backing 1 px above the bottom frame line, a 2 px track inside, the fill on it; and the mark of Tensura mode
      const m = { fraction: d[2] }, iw = SLOT - 4 - 2, fw = d[2] >= 1 ? iw : Math.max(1, Math.min(iw - 1, Math.round(d[2] * iw)));
      c.rect(x + 2, y + SLOT - 1 - 4, SLOT - 4, 4, 0xFF05080C); c.rect(x + 3, y + SLOT - 1 - 3, iw, 2, 0xFF2A2F36); c.rect(x + 3, y + SLOT - 1 - 3, fw, 2, barColor(i));
      const mk = new Cv(5, 5); mk.poly([[2.5, 0], [5, 2.5], [2.5, 5], [0, 2.5]], 1); c.bevel(mk, ramp(TP.tealD, TP.tealH, TP.teal, rgba('#2AA0A8')), x + SLOT - 2, y - 3);
    }
    // the Alt label: two lines, on the OUTER side of the column, level with the slot's row
    const l1 = d[0], l2 = line2(d), tw = Math.max(meas(l1, 'head'), meas(l2)), pw = tw + 2 * PAD, py = y + (SLOT - LABEL_H) / 2;
    const px = grp === 0 ? x - LABEL_AIR - pw : x + SLOT + LABEL_AIR;
    labelBox(c, px, py, pw, LABEL_H);
    say2(l1, px + PAD, py + 2, { c: HC.head, font: 'head', w: pw - 2 * PAD, align: grp === 0 ? 'RIGHT' : 'LEFT' }, 1);
    say2(l2, px + PAD, py + 10, { c: HC.mut, w: pw - 2 * PAD, align: grp === 0 ? 'RIGHT' : 'LEFT' }, 1);
  }
};
// mode pill (HudLayout.pill: PILL_ABOVE 74 over the bottom edge, centred between the columns)
{
  const w = isT ? 148 : 160, x = 480 - w / 2; K.plate(cv, x, 466, w, 14);
  if (isT) TK.gem(cv, x + 6, 469, 8, TP.teal); else if (THEME === 'grimoire') GK.gem(cv, x + 6, 469, 9, GP.wax); else MK.gem(cv, x + 6, 469, 8, MP.red);
  say(isT ? 'TENSURA  ·  Caps again' : 'SUPERNATURAL  ·  Caps', x + 20, 469, { c: HC.plateText });
}
const sayT = (s, x, y, o) => say(s, x, y, o);
drawGroup(cv, (s, x, y, o) => say(s, x, y, o), 0, COLX[0], ROW0, 2);
drawGroup(cv, (s, x, y, o) => say(s, x, y, o), 1, COLX[1], ROW0, 2);
if (isT) { say('preset 2', COLX[0], 454, { c: 0xFFFFFFFF, shadow: 0xFF000000 }); say('preset 3', COLX[1], 454, { c: 0xFFFFFFFF, shadow: 0xFF000000 }); }
cv.emit(fr, 0, 0, 2, 'screen');
for (const [s, x, y, o] of tx) T(fr, s, x, y, { S: 2, ...o });

// zoomed detail, 4 px per texel: each column with its labels, to judge the craft
const ZS = 4;
[0, 1].forEach((grp) => {
  const zt = [], W = grp ? 260 : 200, H = 76, zc = new Cv(W, H); zc.rect(0, 0, W, H, rgba('#5C9A3C')); zc.rect(0, 0, W, 1, rgba('#82BE52'));
  drawGroup(zc, (s, x, y, o) => zt.push([s, x, y, o]), grp, grp === 0 ? 168 : 10, 4, ZS);
  const ox = grp ? 880 : 40, oy = 170; zc.emit(fr, ox / ZS, oy / ZS, ZS, 'detail ' + grp);
  for (const [s, x, y, o] of zt) T(fr, s, ox / ZS + x, oy / ZS + y, { S: ZS, ...o });
  label(fr, 'Detail × 4: ' + (grp ? 'right column (Z X C), labels on the right' : 'left column (R F V), labels on the left') + (isT ? ' · mastery bar on each slot, key badge lifted over it' : grp === 0 ? ' · blood ring on Bite' : ''), ox, oy - 34, 0xFF1A1A22, 16, true);
});
return done({ id: fr.id, rects: cv.rects().length });
