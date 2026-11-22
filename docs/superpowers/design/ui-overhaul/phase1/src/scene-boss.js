// Boss bar mockup: real-size stack on the world, and a zoomed shift sequence below. Needs THEME.
const K = KF[THEME], C = HCOL[THEME];
const FX = { grimoire: 0, moon: 2000, tensura: 4000 }[THEME], FY = 9200;
const fname = 'Boss bars · ' + K.name;
for (const n of PAGE.children.filter((c) => c.name === fname)) n.remove();
const fr = figma.createFrame(); fr.name = fname; fr.x = FX; fr.y = FY; fr.resize(1920, 1080); fr.clipsContent = true; fr.fills = []; PAGE.appendChild(fr);
const SPEC = { grimoire: rgba('#1B0B0B'), moon: rgba('#0B0509'), tensura: rgba('#08141F') }[THEME];
const tw = (s) => Math.round(s.length * 5.3);

// ---- portraits, 24 x 24 -------------------------------------------------
const bust = (kind) => {
  const p = new Cv(24, 24);
  const human = (o) => { p.rect(0, 0, 24, 24, o.bg); p.rect(3, 19, 18, 5, o.coat); p.rect(9, 17, 6, 3, SKIN.fair[1]); p.rect(10, 19, 4, 1, o.collar || SKIN.fair[0]); face(p, 4, 3, o); };
  if (kind === 'klaus') human({ bg: rgba('#3A2A2C'), coat: rgba('#262B38'), skin: 'fair', hair: 'blond', beard: true, eye: rgba('#7A5A22'), collar: rgba('#E4DDCB') });
  if (kind === 'deuc') human({ bg: rgba('#22262E'), coat: rgba('#3A3C44'), skin: 'tan', hair: 'grey', beard: true, glasses: true });
  if (kind === 'deuc-wolf') { p.rect(0, 0, 24, 24, rgba('#22262E')); wolf(p, 0, 0, { fur: [rgba('#1A191F'), rgba('#6A6870'), rgba('#3C3A44'), rgba('#26252C'), rgba('#0C0C10')], eye: rgba('#FFD36B'), muzzle: rgba('#5A5864') }); }
  if (kind === 'beast') { p.rect(0, 0, 24, 24, rgba('#2A1A18')); wolf(p, 0, 0, { fur: [rgba('#3A2A1E'), rgba('#9A7A5A'), rgba('#6E5238'), rgba('#4A3626'), rgba('#1A100A')], eye: rgba('#FF4A3A'), muzzle: rgba('#B09270') }); p.line(5, 6, 9, 12, rgba('#C8B8A0')); }
  if (kind === 'morph') {
    const h = bust('deuc'), w = bust('deuc-wolf'); const jag = [0, 1, -1, 1, 0, -1];
    for (let y = 0; y < 24; y++) { const s = 12 + jag[y % 6]; for (let x = 0; x < 24; x++) p.d[y * 24 + x] = (x < s ? h : w).d[y * 24 + x]; }
    for (let y = 0; y < 24; y++) { const s = 12 + jag[y % 6]; p.px(s, y, withA(0xFFFFE8A0, 200)); }
  }
  return p;
};
const clip = (cv, src, x, y, m, mx, my) => { for (let j = 0; j < 24; j++) for (let i = 0; i < 24; i++) { const c = src.d[j * 24 + i]; if (c && m.get(x + i - mx, y + j - my)) cv.px(x + i, y + j, c); } };

// ---- one bar. returns text ops in bar-local texels -----------------------
const FORMS = { human: ['HUMAN', 'SHIFTING', 'BEAST'], host: ['HOST', 'BEAST', 'SPIRIT'] };
const bossBar = (cv, x, y, o) => {
  const tx = [], say = (s, px, py, op = {}) => tx.push([s, px, py, op]);
  K.panel(cv, x - 5, y - 5, 250, 46);
  const shifting = o.active === 1 && o.set === 'human', fillR = { grimoire: shifting ? ramp(0, GP.brHH, GP.br, GP.brS, GP.brH, GP.brS) : ramp(0, rgba('#C2373B'), rgba('#8E1C22'), rgba('#5E0F14'), rgba('#A8282C'), rgba('#741519')), moon: shifting ? ramp(0, rgba('#FFF0B0'), MP.gold, MP.goldD) : MR.pri, tensura: shifting ? TR.gold : TR.pri }[THEME];
  // portrait plate 34 x 34 in the theme's shape
  const px0 = x, py0 = y + 1, art = bust(o.portrait);
  if (THEME === 'grimoire') { cv.bevel(rr(34, 34, 2), ramp(GP.brD, GP.brHH, GP.br, GP.brS, GP.brH), px0, py0, true); cv.bevel(rr(28, 28, 1), ramp(0, 0, GP.leD, 0), px0 + 3, py0 + 3); clip(cv, art, px0 + 5, py0 + 5, solid(28, 28), px0 + 3, py0 + 3); for (const [a, b] of [[1, 1], [32, 1], [1, 32], [32, 32]]) cv.px(px0 + a, py0 + b, GP.brD); }
  else if (THEME === 'moon') { if (shifting) cv.disc(px0 + 17, py0 + 17, 21, 21, withA(MP.gold, 70)); cv.bevel(discMask(34), ramp(shifting ? MP.gold : MP.red, MP.edgeH, MP.card, MP.cardD), px0, py0); const m = discMask(28); cv.bevel(m, ramp(0, 0, MP.cardD, 0), px0 + 3, py0 + 3); clip(cv, art, px0 + 5, py0 + 5, m, px0 + 3, py0 + 3); cv.ring(px0 + 17, py0 + 17, 15.5, 15.5, 1, shifting ? MP.gold : MP.red); }
  else { if (shifting) cv.bevel(hexMask(42, 38, 11), ramp(0, withA(TP.glow, 60), withA(TP.glow, 60), withA(TP.glow, 60)), px0 - 4, py0 - 2); cv.bevel(hexMask(34, 31, 8), ramp(shifting ? TP.gold : TP.teal, TP.edgeH, TP.panel, TP.panelD), px0, py0 + 1); const m = hexMask(28, 25, 6); cv.bevel(m, ramp(0, 0, rgba('#0A1827'), 0), px0 + 3, py0 + 4); clip(cv, art, px0 + 5, py0 + 4, m, px0 + 3, py0 + 4); }
  // name plate
  const nx = x + 40, nw = 200;
  if (THEME === 'grimoire') GK.plate(cv, nx, y, nw, 13); else if (THEME === 'moon') MK.plate(cv, nx, y, nw, 13); else TK.plate(cv, nx, y, nw, 13);
  say(o.name, nx + 8, y + 2, { font: 'head', c: THEME === 'grimoire' ? GP.paH : C.text });
  // form chips, right aligned in the name plate row
  const set = FORMS[o.set]; let cx = nx + nw - 4; const chips = [];
  for (let i = set.length - 1; i >= 0; i--) { const w = tw(set[i]) + 6; cx -= w; chips.unshift([set[i], cx, w, i]); cx -= 2; }
  // bar
  const by = y + 16, bw = nw;
  if (THEME === 'grimoire') { cv.bevel(rr(bw, 9, 1), ramp(GP.bord, GP.paDD, rgba('#CBB985'), GP.paH), nx, by); cv.bevel(solid(Math.round((bw - 2) * o.hp), 7), fillR, nx + 1, by + 1, true); cv.px(nx - 1, by + 4, GP.br); cv.px(nx + bw, by + 4, GP.br); }
  else if (THEME === 'moon') { cv.bevel(pill(bw, 9), ramp(MP.void, MP.cardD, MP.cardD, MP.cardH), nx, by); cv.bevel(pill(Math.max(9, Math.round((bw - 2) * o.hp)), 7), fillR, nx + 1, by + 1, true); }
  else { cv.bevel(solid(bw, 9), ramp(TP.edge, TP.void, TP.void, TP.panelD), nx, by); cv.bevel(solid(Math.round((bw - 2) * o.hp), 7), fillR, nx + 1, by + 1, true); }
  for (const f of [0.25, 0.5, 0.75]) cv.vl(nx + Math.round(bw * f), by + 1, 7, withA(0xFF000000, 90));
  if (shifting) for (let i = 4; i < bw - 4; i += 8) cv.rect(nx + i, by + 3, 3, 1, withA(0xFFFFFFFF, 150));
  // hp caption + chips
  say(o.hpText, nx + 2, y + 28, { c: THEME === 'grimoire' ? GP.mut : C.mut });
  for (const [t, chx, w, i] of chips) {
    const on = i === o.active, cy = y + 27;
    const r = THEME === 'grimoire' ? (on ? (shifting ? ramp(GP.brD, GP.brHH, GP.br, GP.brS) : ramp(GP.waxD, GP.waxH, GP.wax, GP.waxM)) : ramp(GP.bord, GP.paH, rgba('#E2D6B0'), GP.paS)) : THEME === 'moon' ? (on ? (shifting ? MR.gold : MR.pri) : MR.chip) : (on ? (shifting ? TR.gold : TR.pri) : ramp(TP.edgeH, TP.panelH, TP.lit, TP.panel));
    cv.bevel(THEME === 'moon' ? pill(w, 10) : THEME === 'tensura' ? notch(w, 10, 2, true, false, false, true) : rr(w, 10, 2), r, chx, cy);
    say(t, chx, cy + 2, { w, align: 'CENTER', c: on ? (THEME === 'grimoire' ? (shifting ? GP.ink : GP.paH) : shifting ? rgba('#1A1204') : THEME === 'tensura' ? TP.btnText : MP.text) : (THEME === 'grimoire' ? GP.ink : C.mut) });
  }
  return tx;
};
const barH = 46;
const cv = new Cv(SW, SH), tx = [];
world(cv);
// top-centre stack at real size
const stack = [['Klaus Mikaelson', 'klaus', 'human', 0, 0.82, '1,640 / 2,000'], ['Deucalion', 'morph', 'human', 1, 0.55, '1,100 / 2,000'], ['Beast of Gévaudan', 'beast', 'host', 1, 0.31, '620 / 2,000']];
const SX = 355;
stack.forEach(([name, portrait, set, active, hp, hpText], i) => { const y = 14 + i * (barH + 6); for (const [s, px, py, op] of bossBar(cv, SX, y, { name, portrait, set, active, hp, hpText })) tx.push([s, px, py, op, 2, 0, 0]); });
// spec area
cv.rect(0, 176, SW, SH - 176, SPEC);
cv.emit(fr, 0, 0, 2, 'screen');
for (const [s, x, y, o, S] of tx) T(fr, s, x, y, { S, ...o });
// zoomed sequence: Deucalion human -> shifting -> beast, 3 px per texel
const ZS = 3, seq = [['1 · Human', 'deuc', 0, 0.55, 'Chip HUMAN is lit. The bar is the theme\'s health colour.'], ['2 · Shifting (the boss can be hit)', 'morph', 1, 0.55, 'Chip SHIFTING lights gold, the bar turns gold and striped, the portrait morphs through a jagged seam. About 2 s, swap at the midpoint.'], ['3 · Beast', 'deuc-wolf', 2, 0.55, 'Chip BEAST is lit, the portrait is the wolf, the bar goes back to the health colour.']];
for (let i = 0; i < 3; i++) {
  const zc = new Cv(250, 46); const ztx = bossBar(zc, 5, 5, { name: 'Deucalion', portrait: seq[i][1], set: 'human', active: seq[i][2], hp: seq[i][3], hpText: '1,100 / 2,000' });
  const ox = 30, oy = 446 + i * 205; zc.emit(fr, ox / ZS, oy / ZS, ZS, 'zoom ' + seq[i][0]);
  for (const [s, x, y, o] of ztx) T(fr, s, ox / ZS + x, oy / ZS + y, { S: ZS, ...o });
  label(fr, seq[i][0], 830, oy + 8, 0xFFFFFFFF, 22, true);
  label(fr, seq[i][4], 830, oy + 44, 0xFF9A9AA6, 16, false, 1040);
}
label(fr, K.name + ' · boss bar', 30, 352, 0xFFFFFFFF, 34, true);
label(fr, 'Top: real size at GUI scale 2, three bosses stacked with a 6 px gap (Klaus, Deucalion mid-shift, the Beast of Gévaudan with Host / Beast / Spirit). Below: one bar zoomed × 3 through a shift.', 30, 396, 0xFF9A9AA6, 15, false, 1800);
label(fr, 'Spec: card 250 × 46 · portrait plate 34 × 34 (art 24 × 24) · name plate 200 × 13 · bar 200 × 9 with ticks at 25 / 50 / 75 % · chips 10 high, text 8 px · gap between bars 6.', 830, 760, 0xFF9A9AA6, 15, false, 1040);
label(fr, 'Reduce motion: no seam flash and no gold stripes moving; the chip and the bar colour still change, instantly.', 830, 812, 0xFF9A9AA6, 15, false, 1040);
return done({ id: fr.id, rects: cv.rects().length });
