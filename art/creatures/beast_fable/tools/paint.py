"""python paint.py faces.json out.png  -> paints the Beast's texture from the cube face UVs Blockbench reported.

Every colour below was sampled from a show still (see ../REFERENCES.md, "Palette"). Nothing is invented:
the two vein reds are the only derived values, and the derivation is written there.
"""
import json
import random
import sys
from PIL import Image

FUR = ["#070809", "#121212", "#161616", "#191b20", "#1f272d", "#262e36", "#2f3a42", "#36414b"]  # stills 2, 6
CHEST = ["#191b20", "#1d2226", "#263036", "#323e44", "#3c474f", "#4d5b60"]  # still 2, chest and muzzle
RIM = "#667987"  # still 4, rim light on the back
PAW = ["#110e05", "#131105", "#181406", "#1d1a07"]  # still 5, fingers
CLAW = ["#5e5433", "#7b6d45", "#aa9767", "#c6b381"]  # still 5, claws
GUM = ["#070809", "#1b1b1b", "#1e1e1e", "#24272c"]  # stills 1, 2, inside the mouth
FLESH = ["#2f2321", "#302523", "#3d3230", "#4a3f3d", "#5a4f4d"]  # still 1, tongue and palate
TEETH = ["#62635d", "#787470", "#cbbcc7", "#e4d3dc"]  # stills 1, 3
EYE_CORE, EYE_GLOW, EYE_HALO = "#edfdfd", "#9caecb", "#434f65"  # still 1
VEIN, VEIN_DARK = "#6a2c34", "#461a1f"  # still 3, see REFERENCES.md


def rgb(h, a=255):
    return (int(h[1:3], 16), int(h[3:5], 16), int(h[5:7], 16), a)


class Face:
    """One cube face as a w x h grid. col 0 is the canonical left edge, row 0 the canonical top edge:
    north: left = +x, top = +y.  east: left = back.  west: left = front.  south: left = -x.
    up: left = +x, top = back.  down: left = +x."""

    def __init__(self, img, uv):
        self.img = img
        u1, v1, u2, v2 = uv
        self.w, self.h = round(abs(u2 - u1)), round(abs(v2 - v1))
        self.u1, self.v1 = u1, v1
        self.su = 1 if u2 >= u1 else -1
        self.sv = 1 if v2 >= v1 else -1

    def put(self, c, r, colour):
        if 0 <= c < self.w and 0 <= r < self.h:
            x = int(self.u1 + c) if self.su > 0 else int(self.u1 - 1 - c)
            y = int(self.v1 + r) if self.sv > 0 else int(self.v1 - 1 - r)
            self.img.putpixel((x, y), rgb(colour) if isinstance(colour, str) else colour)

    def cells(self):
        return ((c, r) for r in range(self.h) for c in range(self.w))


def streaks(rnd, w, h):
    """Matted fur: per-column vertical runs, neighbouring columns loosely linked into clumps. Values in -1..1."""
    grid = [[0] * w for _ in range(h)]
    for c in range(w):
        r = -rnd.randint(0, 4)
        while r < h:
            run = rnd.randint(3, 7)
            val = rnd.choice([-1, 0, 0, 0, 1])
            if c and rnd.random() < 0.6 and 0 <= r < h:
                val = grid[r][c - 1]
            for k in range(run):
                if 0 <= r + k < h:
                    grid[r + k][c] = val
            r += run
    return grid


def fur(face, rnd, ramp, base, lit=0):
    """lit: +1 for faces that catch light (tops), -2 for undersides."""
    g = streaks(rnd, face.w, face.h)
    for c, r in face.cells():
        i = base + lit + g[r][c]
        if face.h >= 6:
            if r < max(1, face.h // 6):
                i += 1
            elif r >= face.h - max(1, face.h // 5):
                i -= 1
        roll = rnd.random()
        if roll < 0.02:
            i += 2
        elif roll < 0.045:
            i -= 2
        face.put(c, r, ramp[max(0, min(len(ramp) - 1, i))])


def vein(face, rnd, start, steps, branch=0.16):
    """A red vein wandering down the face from start=(col,row)."""
    c, r = start
    todo = [(c, r, steps, 0.4)]
    while todo:
        c, r, n, bright = todo.pop()
        lean = rnd.choice([-1, 1])
        for k in range(n):
            face.put(c, r, VEIN if rnd.random() < bright and k < n - 2 else VEIN_DARK)
            if rnd.random() < branch and n - k > 4:
                todo.append((c - lean, r + 1, (n - k) // 2, 0))
            if k % 4 == 3 and rnd.random() < 0.7:  # a sidestep every few pixels, joined so the line stays unbroken
                c += lean if rnd.random() < 0.8 else -lean
                face.put(c, r, VEIN_DARK)
            r += 1


def tufts(face, rnd, root):
    """Ragged fur silhouette on a plane, the rest stays transparent. root: 'bottom', 'left' or 'right'."""
    along, across = (face.w, face.h) if root == "bottom" else (face.h, face.w)
    length = across * 0.6
    for a in range(along):
        length = max(across * 0.25, min(across, length + rnd.uniform(-across * 0.45, across * 0.45)))
        taper = 1 - 0.55 * abs(2 * (a + 0.5) / along - 1) if root == "bottom" else 1  # crest: tallest in the middle
        n = max(1, round(length * taper))
        for k in range(n):
            tone = 3 if k < n * 0.4 else 2 if k < n - 1 else 1
            if rnd.random() < 0.15:
                tone -= 1
            cell = (a, face.h - 1 - k) if root == "bottom" else (k, a) if root == "left" else (face.w - 1 - k, a)
            face.put(*cell, FUR[tone])
        if rnd.random() < 0.3 and n + 1 < across:  # a loose fleck past the tip: the smoky edge
            k = n + 1
            cell = (a, face.h - 1 - k) if root == "bottom" else (k, a) if root == "left" else (face.w - 1 - k, a)
            face.put(*cell, FUR[1])


def teeth(face, lengths, rooted_top):
    """Needle fangs, one pixel wide, with gaps: the lower row is drawn to land in the gaps of the upper row,
    and the dark gum behind shows between them. Packed any tighter, the mouth reads as a pale band."""
    for c, n in enumerate(lengths[: face.w]):
        for k in range(n):
            r = k if rooted_top else face.h - 1 - k
            face.put(c, r, TEETH[3] if (k == n - 1 and n == 4) else TEETH[2] if k >= n - 2 else TEETH[1])


def paint(cube, img):
    name, mat, tone = cube["name"], cube["mat"], cube["tone"]
    rnd = random.Random(name)
    F = {d: Face(img, uv) for d, uv in cube["faces"].items()}
    F = {d: f for d, f in F.items() if f.w and f.h}
    lit = {"up": 1, "down": -2}
    left = name.endswith("_left")
    outer = "east" if left else "west"

    if mat in ("fur", "chest", "skull", "snout", "brow", "jaw", "ear", "nose"):
        ramp, base = (CHEST, 2) if mat == "chest" else (FUR, 3 + tone)
        if mat in ("brow", "nose"):
            base = 1
        for d, f in F.items():
            fur(f, rnd, ramp, base, lit.get(d, 0))

    if name == "ribcage":
        f = F["north"]  # pale matted chest fur running up from the pecs
        for c, r in f.cells():
            edge = 4 + abs(c - f.w / 2 + 0.5) * 0.9 + rnd.uniform(-1.5, 1.5)
            if r > edge:
                f.put(c, r, CHEST[rnd.choice([1, 2, 2, 3])])
        for d, c0 in (("east", 0.75), ("west", 0.25)):  # veins over the ribs, towards the front
            f = F[d]
            vein(f, rnd, (int(f.w * c0), 2), 12)
            vein(f, rnd, (int(f.w * (c0 + (0.12 if d == "west" else -0.12))), 6), 9)
        for c in range(1, F["up"].w - 1):
            if rnd.random() < 0.25:
                F["up"].put(c, rnd.randrange(F["up"].h), RIM)
    elif name in ("hump", "hump_ridge"):
        f = F["up"]
        for c, r in f.cells():
            if rnd.random() < 0.07:
                f.put(c, r, RIM)
    elif name == "belly":
        f = F["down"]
        for c, r in f.cells():
            if 2 <= c < f.w - 2 and rnd.random() < 0.7:
                f.put(c, r, CHEST[rnd.choice([0, 1, 1, 2])])
    elif name == "neck":
        f = F["down"]
        for c, r in f.cells():
            f.put(c, r, CHEST[rnd.choice([0, 1, 1, 2])])
        for d in ("east", "west"):
            vein(F[d], rnd, (F[d].w // 2, 1), 8)
    elif name.startswith(("shoulder_l", "shoulder_r")):
        f = F[outer]
        vein(f, rnd, (3, 1), 9)
        vein(f, rnd, (7, 0), 10)
        vein(F["up"], rnd, (4, 1), 8)
        vein(F["north"], rnd, (3 if left else 4, 2), 7)
    elif name.startswith("upper_arm"):
        vein(F[outer], rnd, (4, 0), 15)
        vein(F["north"], rnd, (3, 3), 11)
    elif name.startswith("forearm_cuff"):
        vein(F[outer], rnd, (4, 0), 6, branch=0.3)
    elif name.startswith("thigh"):
        vein(F[outer], rnd, (5, 1), 10)

    elif name == "skull":
        f = F["north"]
        for c in (6, 7):  # the dark crease between the eyes
            for r in range(2, 5):
                f.put(c, r, FUR[1])
        # slanted eyes: outer corner high, inner corner low; rows 3-4 sit between the brow and the snout
        eye = {(1, 3): EYE_GLOW, (2, 3): EYE_CORE, (3, 3): EYE_CORE, (4, 3): EYE_GLOW,
               (2, 4): EYE_GLOW, (3, 4): EYE_CORE, (4, 4): EYE_CORE, (5, 4): EYE_GLOW,
               (0, 3): EYE_HALO, (1, 4): EYE_HALO, (1, 2): EYE_HALO, (2, 2): EYE_HALO, (3, 2): EYE_HALO, (5, 3): EYE_HALO}
        for (c, r), colour in eye.items():
            f.put(c, r, colour)
            f.put(f.w - 1 - c, r, colour)
        # the grin runs past the snout and turns up along the cheeks
        for c in (0, f.w - 1):
            f.put(c, 8, TEETH[2])
            f.put(c, 7, GUM[1])
        for d in ("east", "west"):
            f = F[d]
            front = (lambda k: f.w - 1 - k) if d == "east" else (lambda k: k)
            line = [(0, 8), (1, 8), (2, 8), (3, 8), (4, 8), (5, 7), (6, 7), (7, 6), (8, 5)]
            for k, r in line:
                f.put(front(k), r, TEETH[2] if k % 3 == 1 else GUM[1])
                f.put(front(k), r - 1, GUM[1])
            f.put(front(8), 4, GUM[1])
    elif name == "snout":
        f = F["north"]
        for c, r in f.cells():
            if r in (1, 2) and rnd.random() < 0.6:
                f.put(c, r, CHEST[rnd.choice([1, 2, 3])])
        for r in (2, 3):  # split upper lip under the nose
            f.put(5, r, FUR[1])
            f.put(6, r, FUR[1])
        for d in ("north", "east", "west"):  # lipless: bare gum along the bottom edge, tooth roots showing
            f = F[d]
            for c in range(f.w):
                f.put(c, f.h - 1, GUM[3] if c % 3 == 1 else GUM[1])
        f = F["down"]
        for c, r in f.cells():
            f.put(c, r, GUM[rnd.choice([1, 2])])
    elif name == "nose":
        f = F["north"]
        for c, r in f.cells():
            f.put(c, r, FUR[1])
        f.put(0, 1, FUR[0])
        f.put(3, 1, FUR[0])
        for c in range(F["up"].w):
            F["up"].put(c, 0, FUR[5])
    elif name == "jaw":
        f = F["north"]
        for c, r in f.cells():
            if r >= 1:
                f.put(c, r, CHEST[rnd.choice([0, 1, 1, 2])])
        for d in ("north", "east", "west"):
            f = F[d]
            for c in range(f.w):
                f.put(c, 0, GUM[3] if c % 3 == 2 else GUM[1])
        f = F["up"]
        for c, r in f.cells():
            f.put(c, r, GUM[rnd.choice([1, 2])])
    elif name.startswith("ear_"):
        f = F["north"]  # bare inner ear
        for c, r in f.cells():
            if 0 < c < f.w - 1 or f.w <= 2:
                f.put(c, r, CHEST[3] if (c + r) % 2 else CHEST[2])

    elif mat == "gum":
        for d, f in F.items():
            for c, r in f.cells():
                f.put(c, r, GUM[rnd.choice([0, 1, 1, 2])])
        palate = F.get("down" if name == "gum_upper" else "up")
        if name != "throat":
            for c, r in palate.cells():
                palate.put(c, r, FLESH[rnd.choice([0, 1, 1, 2])])
        else:
            for c, r in F["north"].cells():
                F["north"].put(c, r, FLESH[0] if rnd.random() < 0.3 else GUM[0])
    elif mat == "tongue":
        for d, f in F.items():
            for c, r in f.cells():
                f.put(c, r, FLESH[rnd.choice([2, 3, 3, 4])])
        f = F["up"]
        for r in range(f.h - 2):
            f.put(1, r, FLESH[2])
            f.put(2, r, FLESH[2])
    elif mat == "teeth_upper":
        front = [0, 4, 0, 0, 3, 0, 0, 3, 0, 0, 4, 0]
        sides = [0, 3, 0, 0, 3, 0, 0, 2, 0]
        for d, f in F.items():
            lens = front if d in ("north", "south") else (sides[::-1] if d == "east" else sides)
            teeth(f, lens, True)
    elif mat == "teeth_lower":
        front = [0, 0, 4, 0, 0, 3, 0, 0, 4, 0, 0]
        sides = [0, 0, 0, 3, 0, 0, 2, 0, 0]
        for d, f in F.items():
            lens = front if d in ("north", "south") else (sides[::-1] if d == "east" else sides)
            teeth(f, lens, False)
    elif mat == "mane":
        for d, f in F.items():
            if name.startswith(("wisp_tail", "elbow_tuft")):
                root = "right" if d == "east" else "left"  # rooted at the front edge, streaming back
            else:
                root = "bottom"
            tufts(f, random.Random(name), root)
    elif mat == "paw":
        for d, f in F.items():
            fur(f, rnd, FUR, 2, lit.get(d, 0))
        f = F["north"]  # fingers: warm bare hide, split every two pixels in front of each claw
        for c, r in f.cells():
            f.put(c, r, PAW[0] if c % 2 == 0 and f.w > 5 and c else PAW[rnd.choice([2, 3])])
        f = F["up"]
        for c, r in f.cells():
            if r >= f.h - 3:
                f.put(c, r, PAW[0] if c % 2 == 0 and c else PAW[rnd.choice([1, 2, 3])])
        f = F["down"]
        for c, r in f.cells():
            f.put(c, r, PAW[rnd.choice([0, 1])])
    elif mat == "claw":
        for d, f in F.items():
            for c, r in f.cells():
                if d == "north":
                    i = 3
                elif d == "south":
                    i = 0
                elif d in ("up", "down"):
                    i = round(3 * r / max(1, f.h - 1))  # row 0 is the back edge
                else:
                    k = c if d == "east" else f.w - 1 - c
                    i = round(3 * k / max(1, f.w - 1))
                if d == "down":
                    i = max(0, i - 1)
                f.put(c, r, CLAW[i])


def main():
    data = json.load(open(sys.argv[1]))
    img = Image.new("RGBA", tuple(data["texture"]), (0, 0, 0, 0))
    for cube in data["cubes"]:
        paint(cube, img)
    img.save(sys.argv[2])
    print("painted", len(data["cubes"]), "cubes ->", sys.argv[2])


if __name__ == "__main__":
    main()
