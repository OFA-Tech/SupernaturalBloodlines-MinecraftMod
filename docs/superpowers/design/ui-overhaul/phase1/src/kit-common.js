// Shared mask and ramp helpers for the three kits.
const ramp = (o, hi, b, sh, hi2, sh2) => ({ o, hi, b, sh, hi2, sh2 });
const solid = (w, h) => { const m = new Cv(w, h); m.rect(0, 0, w, h, 1); return m; };
// rounded/chamfered rectangle: corner pixels with dx+dy < r are removed
const rr = (w, h, r) => { const m = solid(w, h); for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) { const dx = Math.min(x, w - 1 - x), dy = Math.min(y, h - 1 - y); if (dx + dy < r) m.d[y * w + x] = 0; } return m; };
// diagonal notches on chosen corners (tl,tr,bl,br) of size r
const notch = (w, h, r, tl, tr, bl, br) => {
  const m = solid(w, h);
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
    const cut = (tl && x + y < r) || (tr && (w - 1 - x) + y < r) || (bl && x + (h - 1 - y) < r) || (br && (w - 1 - x) + (h - 1 - y) < r);
    if (cut) m.d[y * w + x] = 0;
  }
  return m;
};
const pill = (w, h) => { const m = new Cv(w, h), r = h / 2; for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) { const px = x + 0.5, py = y + 0.5, cx = Math.min(Math.max(px, r), w - r); if ((px - cx) * (px - cx) + (py - r) * (py - r) <= r * r) m.d[y * w + x] = 1; } return m; };
const discMask = (d) => { const m = new Cv(d, d); m.disc(d / 2, d / 2, d / 2, d / 2, 1); return m; };
// flat-top hexagon (points left and right), w x h, tip = how many columns the points are inset at the top row
const hexMask = (w, h, tip) => { const m = new Cv(w, h); for (let y = 0; y < h; y++) { const dy = Math.abs(y + 0.5 - h / 2) / (h / 2), ins = Math.round(tip * dy); for (let x = ins; x < w - ins; x++) m.d[y * w + x] = 1; } return m; };
// cooldown sweep: darkens the part of the disc still "left", clockwise from twelve o'clock
const pie = (cv, cx, cy, r, left, c) => { for (let j = Math.floor(cy - r); j <= Math.ceil(cy + r); j++) for (let i = Math.floor(cx - r); i <= Math.ceil(cx + r); i++) { const dx = i + 0.5 - cx, dy = j + 0.5 - cy; if (dx * dx + dy * dy > r * r) continue; let a = Math.atan2(dx, -dy); if (a < 0) a += 2 * Math.PI; if (a / (2 * Math.PI) < left) cv.px(i, j, c); } };
const bigBand = (cv, x, y, w, colors, bandH) => { colors.forEach((c, i) => cv.rect(x, y + i * bandH, w, bandH, c)); };
