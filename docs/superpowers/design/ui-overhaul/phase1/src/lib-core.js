// Style-guide generator, core: pixel canvas -> merged Figma rectangles, text, contrast.
// Run inside use_figma (plugin sandbox). 1 texel = 1 GUI px; S = Figma px per texel.
const rgba = (h) => { // '#rrggbb' | '#rrggbbaa' | int -> packed 0xAARRGGBB (>>>0)
  if (typeof h === 'number') return h >>> 0;
  h = h.replace('#', ''); const a = h.length === 8 ? parseInt(h.slice(6, 8), 16) : 255;
  return (((a << 24) | parseInt(h.slice(0, 6), 16)) >>> 0);
};
const A = (c) => c >>> 24, R = (c) => (c >>> 16) & 255, G = (c) => (c >>> 8) & 255, B = (c) => c & 255;
const pack = (r, g, b, a = 255) => (((a << 24) | (r << 16) | (g << 8) | b) >>> 0);
const withA = (c, a) => ((((a << 24) | (c & 0xffffff))) >>> 0);
const mix = (c1, c2, t) => pack(Math.round(R(c1) + (R(c2) - R(c1)) * t), Math.round(G(c1) + (G(c2) - G(c1)) * t), Math.round(B(c1) + (B(c2) - B(c1)) * t));
const lin = (v) => { v /= 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); };
const lum = (c) => 0.2126 * lin(R(c)) + 0.7152 * lin(G(c)) + 0.0722 * lin(B(c));
const contrast = (a, b) => { const x = lum(a), y = lum(b); return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05); };
const hex6 = (c) => '#' + [R(c), G(c), B(c)].map((v) => v.toString(16).padStart(2, '0')).join('').toUpperCase();
const fig = (c) => ({ r: R(c) / 255, g: G(c) / 255, b: B(c) / 255 });
const rng = (seed) => () => { seed = (seed * 1664525 + 1013904223) >>> 0; return seed / 4294967296; };

class Cv {
  constructor(w, h) { this.w = w; this.h = h; this.d = new Uint32Array(w * h); }
  get(x, y) { return (x < 0 || y < 0 || x >= this.w || y >= this.h) ? 0 : this.d[y * this.w + x]; }
  px(x, y, c) {
    if (x < 0 || y < 0 || x >= this.w || y >= this.h) return this;
    if (c === 0) return this;
    const i = y * this.w + x, sa = A(c);
    if (sa === 0) { this.d[i] = c; return this; } // mask value (never emitted)
    const dst = this.d[i];
    if (sa === 255 || dst === 0) { this.d[i] = c; return this; }
    const da = A(dst), oa = sa + da * (255 - sa) / 255, f = sa / oa, g = (da * (255 - sa) / 255) / oa;
    this.d[i] = pack(Math.round(R(c) * f + R(dst) * g), Math.round(G(c) * f + G(dst) * g), Math.round(B(c) * f + B(dst) * g), Math.round(oa));
    return this;
  }
  clr(x, y) { if (x >= 0 && y >= 0 && x < this.w && y < this.h) this.d[y * this.w + x] = 0; return this; }
  rect(x, y, w, h, c) { for (let j = 0; j < h; j++) for (let i = 0; i < w; i++) this.px(x + i, y + j, c); return this; }
  box(x, y, w, h, c) { this.rect(x, y, w, 1, c); this.rect(x, y + h - 1, w, 1, c); this.rect(x, y + 1, 1, h - 2, c); this.rect(x + w - 1, y + 1, 1, h - 2, c); return this; }
  hl(x, y, w, c) { return this.rect(x, y, w, 1, c); }
  vl(x, y, h, c) { return this.rect(x, y, 1, h, c); }
  line(x0, y0, x1, y1, c) {
    let dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0), sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, e = dx + dy;
    for (;;) { this.px(x0, y0, c); if (x0 === x1 && y0 === y1) break; const e2 = 2 * e; if (e2 >= dy) { e += dy; x0 += sx; } if (e2 <= dx) { e += dx; y0 += sy; } }
    return this;
  }
  // filled disc / ellipse by pixel centre; (cx,cy) = centre in texel coords (may be .5)
  disc(cx, cy, rx, ry, c) {
    ry = ry === undefined ? rx : ry;
    for (let j = Math.floor(cy - ry - 1); j <= Math.ceil(cy + ry + 1); j++) for (let i = Math.floor(cx - rx - 1); i <= Math.ceil(cx + rx + 1); i++) {
      const dx = (i + 0.5 - cx) / rx, dy = (j + 0.5 - cy) / ry; if (dx * dx + dy * dy <= 1.0) this.px(i, j, c);
    }
    return this;
  }
  ring(cx, cy, rx, ry, t, c) { // ellipse ring thickness t
    for (let j = Math.floor(cy - ry - 1); j <= Math.ceil(cy + ry + 1); j++) for (let i = Math.floor(cx - rx - 1); i <= Math.ceil(cx + rx + 1); i++) {
      const dx = (i + 0.5 - cx) / rx, dy = (j + 0.5 - cy) / ry, d = dx * dx + dy * dy;
      const ix = (rx - t) / rx, iy = (ry - t) / ry; if (d <= 1.0 && (dx * dx) / (ix * ix) + (dy * dy) / (iy * iy) >= 1.0) this.px(i, j, c);
    }
    return this;
  }
  poly(pts, c) { // even-odd fill of polygon in texel coords
    let y0 = 1e9, y1 = -1e9, x0 = 1e9, x1 = -1e9;
    for (const p of pts) { y0 = Math.min(y0, p[1]); y1 = Math.max(y1, p[1]); x0 = Math.min(x0, p[0]); x1 = Math.max(x1, p[0]); }
    for (let j = Math.floor(y0); j <= Math.ceil(y1); j++) for (let i = Math.floor(x0); i <= Math.ceil(x1); i++) {
      const X = i + 0.5, Y = j + 0.5; let inside = false;
      for (let a = 0, b = pts.length - 1; a < pts.length; b = a++) {
        const [xa, ya] = pts[a], [xb, yb] = pts[b];
        if (((ya > Y) !== (yb > Y)) && (X < (xb - xa) * (Y - ya) / (yb - ya) + xa)) inside = !inside;
      }
      if (inside) this.px(i, j, c);
    }
    return this;
  }
  blit(o, x, y, flipX = false, flipY = false) {
    for (let j = 0; j < o.h; j++) for (let i = 0; i < o.w; i++) { const c = o.d[j * o.w + i]; if (c) this.px(x + (flipX ? o.w - 1 - i : i), y + (flipY ? o.h - 1 - j : j), c); }
    return this;
  }
  // bitmap from strings; pal maps char -> color ('.' = empty)
  bmp(rows, pal, x = 0, y = 0, flipX = false) {
    rows.forEach((r, j) => { for (let i = 0; i < r.length; i++) { const ch = r[i]; if (ch !== '.' && ch !== ' ' && pal[ch] !== undefined) this.px(x + (flipX ? r.length - 1 - i : i), y + j, pal[ch]); } });
    return this;
  }
  grain(x, y, w, h, c, density, seed, len = 2) { // sparse short horizontal fibres
    const r = rng(seed);
    for (let j = y; j < y + h; j++) for (let i = x; i < x + w; i++) if (r() < density) { for (let k = 0; k < len; k++) this.px(i + k, j, c); }
    return this;
  }
  dash(x, y, w, c, on = 3, off = 3) { for (let i = 0; i < w; i += on + off) this.rect(x + i, y, Math.min(on, w - i), 1, c); return this; } // cheap dither seam: a row of dashes
  dither(x, y, w, h, c, kind = 0) { // ordered 2x2 dither: kind 0 checker, 1 quarter, 2 three quarter
    for (let j = 0; j < h; j++) for (let i = 0; i < w; i++) {
      const a = (i + j) & 1, b = (i & 1) === 0 && (j & 1) === 0, d3 = !((i & 1) === 1 && (j & 1) === 1);
      if ((kind === 0 && a === 0) || (kind === 1 && b) || (kind === 2 && d3)) this.px(x + i, y + j, c);
    }
    return this;
  }
  mask() { const m = new Cv(this.w, this.h); for (let i = 0; i < this.d.length; i++) m.d[i] = this.d[i] ? 1 : 0; return m; }
  copy() { const o = new Cv(this.w, this.h); o.d.set(this.d); return o; }
  // paint every empty pixel that touches (4-neighbour) a filled one
  outline(c) {
    const o = this.copy();
    for (let j = 0; j < this.h; j++) for (let i = 0; i < this.w; i++) if (!this.get(i, j) && (this.get(i - 1, j) || this.get(i + 1, j) || this.get(i, j - 1) || this.get(i, j + 1))) o.d[j * this.w + i] = c;
    this.d.set(o.d); return this;
  }
  // light from top-left: colour a 0/1 mask with a ramp {o,hi,b,sh,(hi2),(sh2)}, 1 px outline outside
  bevel(mask, ramp, ox = 0, oy = 0, band2 = false) {
    const m = (x, y) => (x < 0 || y < 0 || x >= mask.w || y >= mask.h) ? 0 : mask.d[y * mask.w + x] ? 1 : 0;
    for (let y = -1; y <= mask.h; y++) for (let x = -1; x <= mask.w; x++) {
      if (m(x, y)) {
        const up = m(x, y - 1), dn = m(x, y + 1), lf = m(x - 1, y), rt = m(x + 1, y);
        let c = ramp.b;
        if (!up) c = ramp.hi; else if (!dn) c = ramp.sh; else if (!lf) c = ramp.hi; else if (!rt) c = ramp.sh;
        else if (band2) { if (!m(x, y - 2) || !m(x - 2, y)) c = ramp.hi2 || ramp.hi; else if (!m(x, y + 2) || !m(x + 2, y)) c = ramp.sh2 || ramp.sh; }
        this.px(ox + x, oy + y, c);
      } else if (ramp.o && (m(x - 1, y) || m(x + 1, y) || m(x, y - 1) || m(x, y + 1))) this.px(ox + x, oy + y, ramp.o);
    }
    return this;
  }
  stamp(x, y, mask, c) { for (let j = 0; j < mask.h; j++) for (let i = 0; i < mask.w; i++) if (mask.d[j * mask.w + i]) this.px(x + i, y + j, c); return this; }
  // merge into rectangles and emit as Figma nodes inside a new frame
  rects() {
    const out = [], open = new Map();
    for (let y = 0; y < this.h; y++) {
      const seen = new Set();
      let x = 0;
      while (x < this.w) {
        const c = this.d[y * this.w + x];
        if (!c) { x++; continue; }
        let x1 = x + 1; while (x1 < this.w && this.d[y * this.w + x1] === c) x1++;
        const key = x + '|' + (x1 - x) + '|' + c, o = open.get(key);
        if (o && o.last === y - 1) { o.h++; o.last = y; } else { const r = { x, y, w: x1 - x, h: 1, c, last: y }; out.push(r); open.set(key, r); }
        x = x1;
      }
    }
    return out;
  }
  emit(parent, x, y, S, name) {
    const f = figma.createFrame(); f.name = name || 'pixels'; f.x = x * S; f.y = y * S; f.resize(this.w * S, this.h * S); f.fills = []; f.clipsContent = false;
    parent.appendChild(f);
    for (const r of this.rects()) {
      const n = figma.createRectangle(); n.x = r.x * S; n.y = r.y * S; n.resize(r.w * S, r.h * S);
      n.fills = [{ type: 'SOLID', color: fig(r.c), opacity: A(r.c) / 255 }]; f.appendChild(n);
    }
    return f;
  }
}

// ---------- Figma helpers ----------
const FONTS = { body: { family: 'Pixelify Sans', style: 'Medium' }, head: { family: 'Pixelify Sans', style: 'SemiBold' }, title: { family: 'Pixelify Sans', style: 'Bold' }, mono: { family: 'Silkscreen', style: 'Regular' } };
const loadFonts = async () => { for (const k in FONTS) await figma.loadFontAsync(FONTS[k]); };
// text: x,y in texels, size in texels (8 = Minecraft body). returns node (width in px: n.width)
const T = (parent, str, x, y, o = {}) => {
  const S = o.S || 2, sz = o.size || 8, t = figma.createText();
  t.fontName = FONTS[o.font || 'body']; t.fontSize = Math.round(sz * S * (o.k || 1.22) * 10) / 10; t.characters = String(str);
  t.fills = [{ type: 'SOLID', color: fig(o.c === undefined ? 0xFFFFFFFF : o.c) }];
  t.lineHeight = { unit: 'PIXELS', value: (o.lh || 9) * S }; t.letterSpacing = { unit: 'PIXELS', value: o.ls || 0 };
  if (o.w) { t.textAutoResize = 'HEIGHT'; t.resize(o.w * S, t.height); }
  if (o.align) t.textAlignHorizontal = o.align;
  if (o.shadow !== undefined && o.shadow !== null) t.effects = [{ type: 'DROP_SHADOW', color: { ...fig(o.shadow), a: 1 }, offset: { x: S, y: S }, radius: 0, spread: 0, visible: true, blendMode: 'NORMAL' }];
  t.x = x * S; t.y = y * S - (o.dy === undefined ? 1 : o.dy) * S; parent.appendChild(t); if (o.name) t.name = o.name;
  return t;
};
const label = async (parent, str, x, y, c = 0xFFD8D8D8, size = 14, bold = false, w = 0) => { // annotation text in real Figma px (not texels)
  const t = figma.createText(); t.fontName = bold ? { family: 'Inter', style: 'Semi Bold' } : { family: 'Inter', style: 'Regular' }; t.fontSize = size; t.characters = String(str);
  t.fills = [{ type: 'SOLID', color: fig(c) }]; if (w) { t.textAutoResize = 'HEIGHT'; t.resize(w, t.height); } t.x = x; t.y = y; parent.appendChild(t); return t;
};
const initInter = async () => { await figma.loadFontAsync({ family: 'Inter', style: 'Regular' }); await figma.loadFontAsync({ family: 'Inter', style: 'Semi Bold' }); };
const sheet = (name, x, y, w, h, bg) => { const f = figma.createFrame(); f.name = name; f.x = x; f.y = y; f.resize(w, h); f.fills = [{ type: 'SOLID', color: fig(bg) }]; f.clipsContent = true; figma.currentPage.appendChild(f); return f; };
const box = (parent, x, y, w, h, c, name) => { const r = figma.createRectangle(); r.x = x; r.y = y; r.resize(w, h); r.fills = [{ type: 'SOLID', color: fig(c), opacity: A(c) / 255 }]; if (name) r.name = name; parent.appendChild(r); return r; };
