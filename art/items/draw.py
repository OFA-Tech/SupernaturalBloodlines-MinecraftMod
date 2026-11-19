"""Tiny 16x16 pixel canvas used by icons.py and models3d.py."""
from PIL import Image


def hexc(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


class Canvas:
    def __init__(self, n=16):
        self.n = n
        self.p = {}

    def d(self, x, y, c):
        if 0 <= x < self.n and 0 <= y < self.n:
            self.p[(x, y)] = c

    def r(self, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.d(x, y, c)

    def pts(self, c, *xy):
        for x, y in xy:
            self.d(x, y, c)

    def line(self, x0, y0, x1, y1, c):
        dx, dy = abs(x1 - x0), -abs(y1 - y0)
        sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
        e = dx + dy
        while True:
            self.d(x0, y0, c)
            if x0 == x1 and y0 == y1:
                break
            e2 = 2 * e
            if e2 >= dy:
                e += dy
                x0 += sx
            if e2 <= dx:
                e += dx
                y0 += sy

    def ell(self, cx, cy, rx, ry, c, hole=None):
        for y in range(self.n):
            for x in range(self.n):
                v = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2
                if v <= 1 and (hole is None or ((x - cx) / hole[0]) ** 2 + ((y - cy) / hole[1]) ** 2 > 1):
                    self.d(x, y, c)

    def outline(self, c, diag=False):
        add = []
        for (x, y) in self.p:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) + (((1, 1), (-1, 1), (1, -1), (-1, -1)) if diag else ()):
                q = (x + dx, y + dy)
                if q not in self.p and 0 <= q[0] < self.n and 0 <= q[1] < self.n:
                    add.append(q)
        for q in add:
            self.p[q] = c

    def img(self):
        im = Image.new("RGBA", (self.n, self.n), (0, 0, 0, 0))
        for (x, y), c in self.p.items():
            im.putpixel((x, y), hexc(c))
        return im
