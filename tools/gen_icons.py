#!/usr/bin/env python3
"""Draws the hub and HUD icon atlas: 16 x 16 sprites, 16 to a row, white on transparent (a theme tints them).

Writes src/main/resources/assets/supernaturalbloodlines/textures/gui/icons.png and icons.txt (the names in cell order, which
shared/hub/kit/Icons.java must match; a unit test compares them). Needs only the Python standard library:
    py tools/gen_icons.py
"""
import math
import os
import struct
import zlib

CELL = 16
COLUMNS = 16
ROWS = 4
NAMES = [
    "path", "skills", "quests", "rites", "bloodline", "pack", "guide", "loadout",
    "check", "lock", "crown", "dot", "close", "theme", "launch", "drop",
    "moon_new", "moon_waxing_crescent", "moon_first_quarter", "moon_waxing_gibbous", "moon_full", "moon_waning_gibbous",
    "moon_last_quarter", "moon_waning_crescent",
    "sun", "shield", "fang", "vial", "question", "info", "plus", "arrow_left", "arrow_right", "claw", "heart", "key",
    "boot", "eye", "voice", "spark", "bolt", "home", "settings", "admin", "bell",
]


class Canvas:
    def __init__(self):
        self.a = [[0] * CELL for _ in range(CELL)]

    def put(self, x, y, v=255):
        if 0 <= x < CELL and 0 <= y < CELL:
            self.a[y][x] = max(self.a[y][x], v)

    def clear(self, x, y):
        if 0 <= x < CELL and 0 <= y < CELL:
            self.a[y][x] = 0

    def disc(self, cx, cy, r, v=255):
        for y in range(CELL):
            for x in range(CELL):
                if (x + .5 - cx) ** 2 + (y + .5 - cy) ** 2 <= r * r:
                    self.put(x, y, v)

    def erase_disc(self, cx, cy, r):
        for y in range(CELL):
            for x in range(CELL):
                if (x + .5 - cx) ** 2 + (y + .5 - cy) ** 2 <= r * r:
                    self.clear(x, y)

    def ring(self, cx, cy, r, t=1.0, v=255):
        for y in range(CELL):
            for x in range(CELL):
                d = math.hypot(x + .5 - cx, y + .5 - cy)
                if r - t < d <= r:
                    self.put(x, y, v)

    def line(self, x0, y0, x1, y1, thick=1, v=255):
        n = int(max(abs(x1 - x0), abs(y1 - y0)) * 2) + 1
        for i in range(n + 1):
            x = x0 + (x1 - x0) * i / n
            y = y0 + (y1 - y0) * i / n
            for dx in range(thick):
                for dy in range(thick):
                    self.put(int(math.floor(x)) + dx, int(math.floor(y)) + dy, v)

    def rect(self, x0, y0, x1, y1, fill=True, v=255):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                if fill or x in (x0, x1) or y in (y0, y1):
                    self.put(x, y, v)

    def poly(self, pts, v=255):
        for y in range(CELL):
            for x in range(CELL):
                px, py = x + .5, y + .5
                inside = False
                j = len(pts) - 1
                for i in range(len(pts)):
                    xi, yi = pts[i]
                    xj, yj = pts[j]
                    if (yi > py) != (yj > py) and px < (xj - xi) * (py - yi) / (yj - yi) + xi:
                        inside = not inside
                    j = i
                if inside:
                    self.put(x, y, v)

    def bitmap(self, rows, ox, oy, v=255):
        for dy, row in enumerate(rows):
            for dx, ch in enumerate(row):
                if ch == "#":
                    self.put(ox + dx, oy + dy, v)


def moon(k):
    c = Canvas()
    sign = 1 if k <= 4 else -1
    cosphi = math.cos(2 * math.pi * k / 8)
    for y in range(CELL):
        for x in range(CELL):
            nx = (x + .5 - 8) / 6.5
            ny = (y + .5 - 8) / 6.5
            if nx * nx + ny * ny <= 1:
                hw = math.sqrt(max(0.0, 1 - ny * ny))
                lit = sign * nx >= cosphi * hw
                c.put(x, y, 255 if lit else 90)
    return c


def draw(name):
    c = Canvas()
    if name == "path":
        c.disc(7, 8, 6.5)
        c.erase_disc(10.2, 7, 5.4)
    elif name == "skills":
        c.poly([(8, 1), (10, 6), (15, 8), (10, 10), (8, 15), (6, 10), (1, 8), (6, 6)])
    elif name == "quests":
        for i in range(3):
            y = 3 + 4 * i
            c.rect(2, y, 3, y + 1)
            c.line(6, y, 14, y, 2)
    elif name == "rites":
        pts = [(8 + 7 * math.sin(2 * math.pi * i / 5), 8.5 - 7 * math.cos(2 * math.pi * i / 5)) for i in range(5)]
        for i in range(5):
            a, b = pts[i], pts[(i + 2) % 5]
            c.line(a[0], a[1], b[0], b[1])
    elif name == "bloodline":
        c.ring(5.5, 8, 4.5, 1.5)
        c.ring(10.5, 8, 4.5, 1.5)
    elif name == "pack":
        c.disc(8, 11.4, 3.5)
        for (x, y) in ((2.8, 8), (5.8, 4.2), (10.2, 4.2), (13.2, 8)):
            c.disc(x, y, 1.5)
    elif name == "guide":
        for (x, y) in ((8, 3), (3, 8), (13, 8), (8, 13)):
            c.poly([(x, y - 2.8), (x + 2.8, y), (x, y + 2.8), (x - 2.8, y)])
        c.disc(8, 8, 1.3)
    elif name == "loadout":
        c.rect(1, 4, 14, 12, fill=False)
        for x in (4, 7, 10):
            c.rect(x, 6, x + 1, 7)
        c.rect(4, 9, 11, 10)
    elif name == "check":
        c.line(2, 8, 6, 12, 2)
        c.line(6, 12, 13, 3, 2)
    elif name == "lock":
        c.rect(3, 7, 12, 14)
        c.line(4, 7, 4, 4)
        c.line(4, 4, 6, 2)
        c.line(6, 2, 9, 2)
        c.line(9, 2, 11, 4)
        c.line(11, 4, 11, 7)
        c.clear(7, 10)
        c.clear(8, 10)
        c.clear(7, 11)
        c.clear(8, 11)
    elif name == "crown":
        c.poly([(1, 13), (1, 4), (5, 8), (8, 2), (11, 8), (15, 4), (15, 13)])
    elif name == "dot":
        c.disc(8, 8, 2.6)
    elif name == "close":
        c.line(3, 3, 12, 12, 2)
        c.line(12, 3, 3, 12, 2)
    elif name == "theme":
        c.disc(6, 8, 5.5)
        c.erase_disc(8.8, 7, 4.5)
        c.poly([(10.5, 6), (15, 6), (12.75, 9.5)])
    elif name == "launch":
        c.line(3, 13, 12, 4, 2)
        c.line(5, 3, 13, 3, 2)
        c.line(12, 3, 12, 11, 2)
    elif name == "drop":
        c.disc(8, 10, 4.8)
        c.poly([(8, 1), (12.4, 8.5), (3.6, 8.5)])
    elif name.startswith("moon_"):
        order = ["new", "waxing_crescent", "first_quarter", "waxing_gibbous", "full", "waning_gibbous", "last_quarter", "waning_crescent"]
        c = moon(order.index(name[5:]))
    elif name == "sun":
        c.disc(8, 8, 3.4)
        for i in range(8):
            a = math.pi * i / 4
            c.line(8 + 5 * math.cos(a), 8 + 5 * math.sin(a), 8 + 7.3 * math.cos(a), 8 + 7.3 * math.sin(a))
    elif name == "shield":
        c.poly([(2, 2), (14, 2), (14, 8), (8, 15), (2, 8)])
    elif name == "fang":
        c.poly([(3, 2), (13, 2), (8, 15)])
    elif name == "vial":
        c.rect(6, 1, 9, 3)
        c.rect(4, 5, 11, 14, fill=False)
        c.rect(5, 9, 10, 13)
    elif name == "question":
        c.bitmap(["..####..", ".##..##.", ".....##.", "....##..", "...##...", "...##...", "........", "...##...", "...##..."], 4, 3)
    elif name == "info":
        c.disc(8, 3.6, 1.5)
        c.rect(6, 6, 8, 6)
        c.rect(7, 6, 8, 13)
        c.rect(5, 13, 10, 13)
    elif name == "plus":
        c.rect(7, 2, 8, 13)
        c.rect(2, 7, 13, 8)
    elif name == "arrow_left":
        c.poly([(2, 8), (8, 2), (8, 6), (14, 6), (14, 10), (8, 10), (8, 14)])
    elif name == "arrow_right":
        c.poly([(14, 8), (8, 2), (8, 6), (2, 6), (2, 10), (8, 10), (8, 14)])
    elif name == "claw":
        for x in (3, 7, 11):
            c.line(x, 2, x + 3, 13, 2)
    elif name == "heart":
        c.disc(5, 5.2, 3.3)
        c.disc(11, 5.2, 3.3)
        c.poly([(1.8, 6.2), (14.2, 6.2), (8, 14)])
    elif name == "key":
        c.ring(5, 5, 3.4, 1.6)
        c.line(7, 7, 14, 14, 2)
        c.rect(11, 12, 12, 13)
    elif name == "boot":
        c.rect(5, 2, 9, 10)
        c.rect(4, 10, 14, 13)
        c.rect(4, 13, 13, 14)
    elif name == "eye":
        c.ring(8, 8, 5.6, 1.6)
        c.disc(8, 8, 2.4)
    elif name == "voice":
        c.rect(2, 3, 13, 10)
        c.poly([(4, 10), (4, 14), (8, 10)])
        c.rect(5, 5, 10, 5, True, 0)
        c.rect(5, 7, 8, 7, True, 0)
    elif name == "spark":
        c.poly([(8, 1), (10, 6), (15, 8), (10, 10), (8, 15), (6, 10), (1, 8), (6, 6)])
    elif name == "bolt":
        c.poly([(9, 1), (3, 9), (7, 9), (6, 15), (13, 6), (8, 6)])
    elif name == "home":
        c.poly([(8, 1.5), (14.5, 8), (12.5, 8), (12.5, 14), (3.5, 14), (3.5, 8), (1.5, 8)])
        c.rect(6, 9, 9, 14, True, 0)
    elif name == "settings":
        c.ring(8, 8, 4.6, 2.2)
        for ang in range(0, 360, 45):
            x = 8 + 6.2 * math.cos(math.radians(ang))
            y = 8 + 6.2 * math.sin(math.radians(ang))
            c.disc(x, y, 1.3)
    elif name == "admin":
        c.poly([(8, 1), (14, 3.5), (14, 8), (8, 15), (2, 8), (2, 3.5)])
        c.rect(7, 4, 8, 9, True, 0)
        c.rect(7, 11, 8, 12, True, 0)
    elif name == "bell":
        c.disc(8, 7, 4.5)
        c.rect(3, 7, 12, 11)
        c.rect(2, 11, 13, 12)
        c.disc(8, 14, 1.6)
    else:
        raise SystemExit("no drawing for " + name)
    return c


def png(width, height, rgba):
    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    raw = b"".join(b"\x00" + bytes(rgba[y * width * 4:(y + 1) * width * 4]) for y in range(height))
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")


def main():
    assert len(NAMES) <= COLUMNS * ROWS
    w, h = COLUMNS * CELL, ROWS * CELL
    rgba = bytearray(w * h * 4)
    for i, name in enumerate(NAMES):
        c = draw(name)
        ox, oy = (i % COLUMNS) * CELL, (i // COLUMNS) * CELL
        for y in range(CELL):
            for x in range(CELL):
                a = c.a[y][x]
                o = ((oy + y) * w + ox + x) * 4
                rgba[o:o + 4] = bytes((255, 255, 255, a))
    out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "supernaturalbloodlines", "textures", "gui")
    os.makedirs(out, exist_ok=True)
    with open(os.path.join(out, "icons.png"), "wb") as f:
        f.write(png(w, h, rgba))
    with open(os.path.join(out, "icons.txt"), "w", newline="\n") as f:
        f.write("\n".join(NAMES) + "\n")
    print("wrote", len(NAMES), "icons to", os.path.normpath(out))


if __name__ == "__main__":
    main()
