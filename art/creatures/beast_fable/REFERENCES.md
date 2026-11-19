# La Bête du Gévaudan (Fable version): references

This is the second, independent take on the Beast, made to be compared with the first one in `art/creatures/beast/`. I read that version's `REFERENCES.md` and the spec, then gathered my own stills. I did not open its geometry, texture, build tools or previews until this version was committed.

All art here is original. The stills were only looked at and sampled for colour, in a temp folder; none is in the repo. The wiki's image host answers 403, so each still was fetched from its Wayback Machine copy (`web.archive.org/web/2024if_/<image url>`).

## Read first

- `art/creatures/beast/REFERENCES.md` (the first version's sources and its three corrections to the brief: no bone or skull parts, a short tail, mane tufts instead of spikes).
- `docs/superpowers/specs/2026-11-17-beast-of-gevaudan.md` (main checkout): season 5, no mask, about twice an Alpha, smoky shadow body, white-blue eyes, red veins, Roar, leap, collapse at 25% health.

## My own references

| # | Source | What I took from it |
|---|---|---|
| 1 | Still, 5x14 "The Sword and the Spirit", the Beast roaring through a hole in a wall (teenwolf.fandom.com, `File:Teen-Wolf-Season-5-Episode-14-Sword-and-Spirit-Beast-Roar.jpg`) | The open mouth: long thin fangs, top and bottom, spaced apart on a dark mouth, and a dull tongue. Eye colour. The `roar` pose. |
| 2 | Still, 5x15 "Amplification", close-up from the front (teenwolf.fandom.com, `File:Teen-Wolf-Season-5-Episode-15-Amplification-Beast-closeup.jpg`) | The main shape reference. A broad, flat, domed head carried below the shoulders; small pointed ears at the top corners, leaning outwards; slanted eyes that drop towards the nose; a lipless grin as wide as the face; huge shoulders and long arms set wide, like a gorilla. Fur colours. |
| 3 | Still, 5x18 "Maid of Gévaudan", the Beast at the mountain ash barrier (teenwolf.fandom.com, `File:Teen Wolf Season 5 Episode 18 Maid of Gevaudan The beast at the barrier.jpg`) | The only still that shows the **red veins** clearly: thin lines over the shoulder, upper arm and neck. Smoke curling off the body. |
| 4 | Still, 5x18, the Beast coming down the steps (teenwolf.fandom.com, `File:Teen Wolf Season 5 Episode 18 Maid of Gevaudan The beast on the steps.jpg`) | The stance on all fours: weight on the knuckles, back sloping down to small hindquarters, a fuzzy outline. Rim-light colour. No tail visible. |
| 5 | Still, 5x12 "Damnatio Memoriae", the claws (teen-wolf-pack.fandom.com, `File:5x12 Beast of Gevaudan claws.jpg`) | Long, curved, pale horn-coloured claws on dark, bare fingers. Claw and finger colours. |
| 6 | Still, 5x17 "A Credible Threat" (teen-wolf-pack.fandom.com, `File:5x17 the beast of gevaudan.png`) | Same view as #2 from another episode: confirms the head, ears and grin are the design and not one frame's lighting. |
| 7 | Teen Wolf Wiki text, "La Bête du Gévaudan" (teenwolf.fandom.com/wiki/La_Bête_du_Gévaudan) and "The Beast of Gevaudan" (teen-wolf-pack.fandom.com), read through web search | "Almost twice the size" of an Alpha, more fur, large red visible veins, feline face, oversized lipless mouth, black smoke or shadow, eyes entirely glowing white-blue. |
| 8 | CBR, "10 Scariest Creatures in Teen Wolf, Ranked" (cbr.com/teen-wolf-scariest-creatures-villains) | Second opinion on what viewers remember of it: sheer size and strength, a shapeshifter stronger than an Alpha. Backs the heavy build. |
| 9 | TVLine recap of 5x18 (tvline.com/recaps/teen-wolf-recap-season-5-episode-18-beast-revealed-interview-686735) | The reviewer calls it an "overgrown Stitch": the wide grin and big head are what people see first. I made the head a little larger than the first version describes. |

## What the stills changed, against the first version's notes

- **No tail.** None of my stills shows one. I put a short smoke wisp at the rump instead (`wisp_tail`), which the collapse animation fades out.
- **Smooth domed head, no cheek ruff.** The fur reads as a ragged outline on the hump and elbows, not on the face.
- **The grin is the face.** Teeth are separate needle fangs on thin planes with gaps, upper and lower rows interlocking, over a dark gum. A solid row read as a pale band at this size.

## Palette

Every colour in `tools/paint.py` is a median taken from a region of one of the stills with `PIL` (the sampling script ran in the temp folder).

| Use | Colours | From |
|---|---|---|
| Fur, dark to light | `#070809 #121212 #161616 #191b20 #1f272d #262e36 #2f3a42 #36414b` | #2 and #6, body, by brightness band |
| Chest, muzzle and inner ear (paler, matted) | `#191b20 #1d2226 #263036 #323e44 #3c474f #4d5b60` | #2, chest and teeth-in-shadow bands |
| Rim light flecks | `#667987` | #4, brightest 3% of the body |
| Bare fingers and pads | `#110e05 #131105 #181406 #1d1a07` | #5, fingers |
| Claws, base to tip | `#5e5433 #7b6d45 #aa9767 #c6b381` | #5, claws |
| Gums and mouth | `#070809 #1b1b1b #1e1e1e #24272c` | #1 mouth, #2 between the teeth |
| Tongue and palate | `#2f2321 #302523 #3d3230 #4a3f3d #5a4f4d` | #1, reddest pixels in the mouth |
| Teeth | `#62635d #787470 #cbbcc7 #e4d3dc` | #1 (unlit), #3 (lit by its own eyes) |
| Eyes: core, glow, halo | `#edfdfd #9caecb #434f65` | #1 |
| Veins | `#6a2c34 #461a1f` | **derived**, see below |

**The veins are the only derived colours.** Still #3 is graded teal, so the veins there measure `#272938` on hide of `#0e2b3a`: about 2.8 times the hide's red, with the same green and blue. Applying that ratio to the ungraded hide from #2 (`#262e36` and `#191b20`) gives `#6a2c34` and `#461a1f`. The wiki text (#7) says the veins are red, which is what the ratio produces.
