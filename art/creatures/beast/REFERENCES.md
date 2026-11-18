# La Bête du Gévaudan: references

All art in this folder is original. The references below were looked at for silhouette, proportion, colour, face and movement; no pixels or meshes were copied. The still frames were downloaded only to a temp folder to be looked at while modelling and are not in the repo.

## What was checked, and what it showed

| # | Source | What was taken from it |
|---|---|---|
| 1 | Teen Wolf Wiki, "The Beast of Gevaudan" (teen-wolf-pack.fandom.com/wiki/The_Beast_of_Gevaudan). The page itself returns 403, so the wikitext was read through the MediaWiki API: `api.php?action=parse&page=The_Beast_of_Gevaudan&prop=wikitext`. | The "Physical Appearance" section: a bigger version of an Alpha's beast form, **almost twice the size**, **more fur**, **large red visible veins**, a **feline face** with an **oversized mouth and no lips**, a body that looks like **black smoke or shadow**, and eyes that are **entirely glowing white-blue**, "electric blue", with no iris. Hair colour: black. Trivia: a black, smoky aura. Gerard's legend: "oil-black, solid and shapeless, a shadow pretending to be real". |
| 2 | Show still, 5x15 "A Credible Threat" (teen-wolf-pack.fandom.com/wiki/File:5x15_La_Bete.png) | The strongest silhouette reference. It is **hunched, on all fours, with a head carried low between huge shoulders and a heavy hump**. The head is **small for the body**, with **two pointed cat ears**, **two narrow, angled white-blue eyes** and a **wide mouth full of irregular teeth**. The coat is near-black, with **lighter silver-grey fur on the chest and belly**. The arms are long and thick, ending in **long claws**. There is **no skull, no exposed ribs and no bone armour**. |
| 3 | Show still, 5x19 "The Beast of Beacon Hills" (File:5x19_The_Beast.png) | The face close up: a **broad, flat cat-like head**, short muzzle, **pointed ears set high**, eyes slanted down toward the nose (angry), and a **ragged toothy mouth**. Dark, matted, ridged fur. |
| 4 | Show still, episode 5x17, the Beast carrying Liam past the school buses at night (File:5x17_Liam_vs_beast.png) | Mid-leap pose: **long arms**, **digitigrade hind legs**, a **lean, powerful torso under a bulky shoulder mass**. Movement: it crouches and then springs. |
| 5 | Show still, 5x20 "Apotheosis" (File:5x20_Cerberus_briefly_overpowers_the_Beast.png) | The glowing eye shape and the grin of dense, uneven teeth at the closest range; the dark, textured, almost leathery fur. |
| 6 | The Dread Doctors' fresco, 5x10 (File:5x10_dread_doctors_painting.jpg) | How the show draws the Beast as an **upright bipedal brute with long clawed arms**: the basis for the `rear_up` animation. |
| 7 | DeviantArt, "Teen Wolf Reboot - Beast of Gevaudan" by Asanbonsam (deviantart.com/asanbonsam/art/Teen-Wolf-Reboot---Beast-of-Gevaudan-662834324), fan critique and redesign | Fan reading of the on-screen design: a **"hulking black brute"** with **no visible tail** (the critic's complaint), compared with the historical beast's long tail. Used as a second opinion that the show's Beast is bulky and heavy, not sleek. The artwork itself was not copied. |
| 8 | Teen Wolf Wiki, "Werewolf" (teen-wolf-pack.fandom.com, MediaWiki API, section 3) and the existing round-2 boss notes in `art/bosses/REFERENCES.md` | Context for the Alpha beast form the Beast is "twice" of, and the palette already used by the other Teen Wolf wolf textures in this mod, so the Beast sits next to them. |
| 9 | Wikipedia, "Beast of Gévaudan" (historical beast) | Real-life descriptions: tawny or russet fur, a dark stripe down the back, a tail longer than a wolf's ending in a tuft, great teeth and claws, bounding "as if flying" across fields. Used for the **dorsal mane/stripe**, the **tail tuft** and the leaping movement. |
| 10 | `docs/superpowers/specs/2026-11-17-beast-of-gevaudan.md` (main checkout) | Gameplay rules: about **2x scale**, a boss with a **Roar**, a **leap** at range, **regeneration**, a **collapse at 25% health** (phase 3), a **smoky shadow body**, and the owner's answer that it is **season 5 with no mask**. |
| 11 | Reddit r/TeenWolf thread on the painting having red eyes while the Beast has white-blue ones (reddit.com/r/TeenWolf/comments/491zv2) | Search snippet only (the page itself loaded empty). Confirms the **white-blue glowing eyes** as the Beast's identifying feature. |
| 12 | Teen Wolf Wiki, "Supernatural Eyes" (teen-wolf-pack.fandom.com/wiki/Supernatural_Eyes) | Search snippet: the Beast's eye gallery, white-blue, unlike red, blue or gold Werewolf eyes. |

## Corrections to the brief, from the references

- **No bone or skull features.** The brief said the Beast was "partly skeletal" with exposed ribs and a skull-like muzzle. None of the show stills, the wiki or the spec show that. (The spec also says there is no skull mask: those belong to the Dread Doctors.) The model therefore uses **fur, a feline face and teeth**, and keeps bone-white only for **teeth and claws**.
- **A tail is kept but made short and heavy.** The brief lists a segmented tail; the show's Beast barely shows one (fan critique, #7). The tail is three thick segments with a tuft, carried low.
- **"Mane or bone spikes" became mane tufts.** Instead of spikes, the Beast gets ragged fur tufts along the neck, shoulder hump and back (the historical dorsal stripe, #9, and the show's matted ridged fur).

## What each reference decided

| Feature | Decided by |
|---|---|
| Hunched all-fours stance, low head, huge shoulder hump | #2, #4 |
| Small head with pointed cat ears, wide toothy mouth | #2, #3, #5, #1 |
| Narrow, angled, glowing white-blue eyes | #1, #3, #5, #11 |
| Near-black matted fur with silver-grey belly and chest | #1, #2 |
| Red veins | #1 |
| Long clawed arms, digitigrade hind legs, crouch-and-spring motion | #4, #9 |
| Rears up like the fresco | #6, #10 |
| Roar, collapse, 2x size | #10 |
