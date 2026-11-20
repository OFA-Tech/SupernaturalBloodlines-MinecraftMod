# Boss art (round 2, redone against the owner's references)

Textures: `src/main/resources/assets/supernaturalbloodlines/textures/entity/boss/`
Blockbench projects: `art/blockbench/bosses/<asset>.bbmodel` (Modded Entity format, texture embedded)
Previews (front | back | ¾, wolves also a head close-up): `art/previews/bosses/<asset>.png`
Generators (re-run to rebuild every texture): `art/bosses/tools/` (`paint_skins.py`, `paint_wolves.py`, `paint_tricorn.py`; Blockbench scripts `bb_*.js`). `paint_skins.py klaus` ends with `klaus_skin_fix.py` (Klaus's hands, cuff and no paint squares), so a re-run gives the shipped `klaus.png` byte for byte in its pixels.
Item model: `models/item/sebastien_tricorn.json` with `textures/item/sebastien_tricorn.png` (see "Sebastien's tricorn" below).
Owner's reference notes: `art/bosses/REFERENCES.md`.

All art is original. Wikis, show descriptions and fan skins were used as inspiration and colour reference only; no pixels were copied. Planet Minecraft blocks image downloads (HTTP 403), so for the Originals the fan skins could only be checked by page title and tags, and the looks come from the show wikis below.

## Player skins (64×64, classic arms unless noted, both layers used)

| File | Model | Palette | Notes |
|---|---|---|---|
| `mikael.png` | player | all black (jacket, shirt, trousers, boots), black belt with silver buckle; short slicked grey-blond hair, pale blue eyes | light stubble. The white oak stake is silvery-white and carved (grooves): tucked in the belt on the left hip, and in the `.bbmodel` also as a prop held upright in the right fist (its two texture nets sit in free skin space at (56,16) and (56,30), not used by the game model) |
| `klaus.png` | player | dark charcoal henley under an open black leather moto jacket, dark denim, brown boots; curled dirty-blond hair, dark blue-green eyes, pearl-white skin | layered silver/gold/cord necklaces on the open front, paint smudges on forearms and thigh (he paints) |
| `elijah.png` | player | black-charcoal pinstripe suit, white shirt, dark steel-blue tie; short brown hair, hazel eyes | lapels, pocket square and cuffs on the second layer, clean-shaven |
| `kol.png` | player | navy jacket over a grey henley, dark grey trousers, brown boots; messy brown hair, dark brown eyes | light stubble; modern rogue (the show's grey/navy/black/brown palette) |
| `rebekah.png` | **slim arms** | cream blouse, grey wool coat, dark jeans, brown boots; long icy-blonde waves, light blue eyes, pale skin with a few freckles | long gold pendant necklace; hair spills over the coat on the jacket layer |
| `finn.png` | player | grey dress coat over a white shirt and dark tie, dark trousers, black shoes; brown hair slicked back, hazel-green eyes | coat tails on the pants layers |
| `freya.png` | **slim arms** | brown leather jacket over a charcoal top, dark jeans, black boots; long dirty-blonde hair, green eyes, smoky liner | pendant on a cord. Possible future NPC/boss |
| `deucalion_human.png` | player | dark charcoal knit sweater with clean vertical ribs, lighter grey textured (checker-knit) shawl collar and shoulder panels, dark trousers; short swept dark-brown hair in three-tone strands, dark stubble along jaw and chin, blind eyes (clouded pale iris, reddened white) | dark glasses with thin frames and arms are the **only** thing on the hat layer: hide that layer (the `hat` model part) to take them off, e.g. when he shifts. No coat or cane. No random noise: every tone is placed by a regular pattern |
| `peter_hale.png` | player | camel wool overcoat (lapels, open) over a dark grey herringbone waistcoat and a white open-collar shirt, dark trousers; slicked-back dark hair, short trimmed beard and stubble (mouth visible), blue-green eyes | coat to knee level on the pants layers, charcoal trousers and dark shoes below |
| `peter_hale_burned.png` | player | Season 1 burned look in burn tones, not blood: intact skin, dull pink-brown scar (two tones, mottled, with a pale waxy sheen along the ridges), dark crust, grey-black char with grey flakes and ash; raw red only in a few small splits; grey button shirt, black leather jacket, charcoal trousers | one burn field in 3D runs from the right side of his face (viewer's left, as in the 1x04 and 1x09 stills) down the right arm, across the chest and back, to the right hip; a smaller one takes the left hand. Skin grades intact, scar, crust, char towards each centre. The jacket survives only on his left, the shirt is burned through along the swath with a black and scorched-brown fringe, the right trouser leg is burned open. Right half of the face stays recognisably Peter (clouded eye on the burned side) |
| `sebastien_valet.png` | player | French line-infantry soldier of the 1760s: undyed greyish-white wool coat with blue cuffs and turn-backs and brass buttons, buff waistcoat, black cravat, white buff cross-belt, buff breeches, black gaiters | bare-headed on the skin: natural dark hair pulled back into a queue with a black ribbon at the nape, tail and bow hanging on the coat back; clean face with light stubble; healthy tan skin. His tricorn is a 3D item model, not paint (the preview shows it worn) |
| `beast_host.png` | player | linen button-up shirt, brown vest, grey trousers; medium-brown skin, dark brown eyes, short dark brown hair | the Beast's host, Mason Hewitt in the show; village clothes for the mod |

## Werewolf-model textures (128×64)

All four use the Werewolves mod beast model (`WerewolfBeastModel`) UV layout exactly. Eyes are 2 px wide and painted into the face; the mod's separate eye overlay layer, if used, stacks on top.

| File | Palette | Notes |
|---|---|---|
| `deucalion_demon_wolf.png` | blue-grey **skin** (not fur), dark swept-back hair on the head and neck, red eyes | heavy brow ridge, wrinkled snout, pointed ears with dark-red inner, jagged row of white fangs over a dark mouth line, dark claws. Only the hair parts are cut out; the body is smooth skin with muscle shading and dark veins |
| `peter_hale_wolf.png` | dark blue-grey body, shaggy black mane over neck, shoulders and back, red eyes, white claws and fangs | **Needs a quadruped pose or model later** (the Werewolves beast model is a hunched biped; the show's Alpha moves on all fours). GeckoLib upgrade candidate. |
| `klaus_hybrid.png` | warm tan-brown fur, cream chest, dark saddle, black branching hybrid veins, yellow eyes | the show never shows a full wolf form clearly, so the coat follows his dirty-blond/brown colouring |
| `beast_of_gevaudan.png` | black-brown fur with russet back streaks and a pale underbelly mark, dull red veins, bone-white face plates, white-blue eyes | brow band, skull cap, cheek guards (the flat sideburn planes), muzzle ridge, spine ridge, shoulder and forearm plates. Meant to render at about 2×, so details are 2–4 px blocks |

## Sebastien's tricorn (3D item model)

`models/item/sebastien_tricorn.json` (10 cuboids, plain 1.21.1 Java item model) and `textures/item/sebastien_tricorn.png` (16x16, 1 texel per model unit, the same density as a skin). Blockbench project: `art/blockbench/bosses/sebastien_tricorn.bbmodel`. Rebuild with `paint_tricorn.py` and `bb_tricorn.js`.

- Black felt, low crown, the brim cocked up on three sides into a point at the front, a gold lace edge, and a black ribbon cockade with a gold loop and button on the wearer's left.
- A square head cannot sit inside a true triangle of a sensible size, so the two long sides are each a 45-degree front panel plus a straight side panel. It is 14 wide and 18 long on an 8-wide head.
- `display.head` (scale 1.6, up 17.6) makes one model unit one skin pixel and sits the brim one pixel below the top of the head. It was checked in Blockbench on the player model (last two views of `art/previews/bosses/sebastien_tricorn.png`), not in the game.
- **Not wired yet.** There is no `sebastien_tricorn` item in the code. Register one and put it in Sebastien's head slot: `CustomHeadLayer` then draws it with the head transform. Until then he is bare-headed, as he is in the show's stills.

## Sources

- **Mikael** — vampirediaries.fandom.com/wiki/Mikael ("short dark blond hair, blue eyes", dark suits over dress shirts), vampirediaries.fandom.com/wiki/White_Oak_Stake (carved with care). Owner's reference: all black, grey-blond, silver-white carved stake.
- **Klaus** — vampirediaries.fandom.com/wiki/Niklaus_Mikaelson/Appearance (curled dirty-blond hair, dark blue-green eyes, pearl-white skin); planetminecraft.com/skin/klaus-mikaelson-the-originals (page only, image blocked).
- **Elijah** — vampirediaries.fandom.com/wiki/Elijah_Mikaelson (short brown hair, hazel-brown eyes, always in suits); planetminecraft.com/skin/elijah-mikaelson-the-originals (page only).
- **Kol** — vampirediaries.fandom.com/wiki/Kol_Mikaelson (brown hair, dark brown eyes; greys, navy, black and brown; henleys and stylish jackets); planetminecraft.com/skin/kol-mikaelson-the-originals (page only).
- **Rebekah** — vampirediaries.fandom.com/wiki/Rebekah_Mikaelson (long icy light-blonde wavy hair, light blue eyes, pale skin with freckles, white/black/brown/grey, coats, long necklaces); planetminecraft.com/skin/rebekah-mikaelson-the-originals (page only).
- **Finn** — vampirediaries.fandom.com/wiki/Finn_Mikaelson (brown hair slicked off the face, hazel-green eyes, dress shirts under gray dress coats); planetminecraft.com/skin/finn-mikaelson-the-originals (page only).
- **Freya** — vampirediaries.fandom.com/wiki/Freya_Mikaelson (long dirty-blonde hair, green eyes, darker modern clothes, make-up, pendant); planetminecraft.com/skin/freya-mikaelson-the-originals (page only).
- **Deucalion (human and Demon Wolf)** — teen-wolf-pack.fandom.com/wiki/Deucalion (blue eyes, sandy-blond hair; Alpha form dark blue-black/blue-grey skin with a ridged face and brow), teenwolfwiki.com/werewolf ("dark blue-gray skin, more demonic than wolf-like"); owner's reference: dark knit sweater with grey shawl collar and shoulder panels, red eyes, pointed ears, wrinkled snout, fangs.
- **Peter Hale (human, risen, wolf)** — teen-wolf-pack.fandom.com/wiki/Peter_Hale (brown hair smoothed back, blue eyes, goatee or scruff, tailored coats), teenwolf.fandom.com/wiki/Peter_Hale (resurrection from the Hale house grave); owner's reference: camel overcoat, herringbone waistcoat, mud-covered risen look, black and blue-grey Alpha with a black mane and white claws.
- **Beast of Gévaudan** — teen-wolf-pack.fandom.com/wiki/The_Beast_of_Gevaudan (about twice an Alpha's beast form, more fur), teenwolf-next-generation.fandom.com (glowing white-blue eyes, black smoky aura), en.wikipedia.org/wiki/Beast_of_Gévaudan (historical beast: tawny/russet fur, black-streaked back, pale underbelly mark → the russet streaks and underbelly mark).
- **Klaus's hybrid form** — vampirediaries.fandom.com/wiki/Hybrid (werewolf eyes and fangs, never a clearly shown full wolf), reddit.com/r/TheOriginals (yellow eyes, black veins).

### Round 5 sources (extra checks)

- **Burned Peter clothing choice** — the sources say he was left in a long-term care facility burned over 75% of his body (hospital/coma look) and was burned head to toe again in Season 6; so he is bare except burned scraps and gauze bandages, as in a burn ward.

- **Sebastien Valet** — tvline.com/casting-news/teen-wolf-spoilers-gilles-marini-lachlan-buchanan-cast-flashback-episode-674061 and greekreporter.com/2016/01/18/gilles-marini-to-guest-star-in-teen-wolf (a French soldier returning home to Gévaudan; played by Gilles Marini), yahoo.com/entertainment/teen-wolf-recap-maid-g-032500374.html and thesupernaturalfoxsisters.com/2016/02/24/teen-wolf-s05e18-recap-maid-of-gevaudan (wounded soldiers Sebastien and Marcel in 1760 French North America, then the Gévaudan tavern), teenwolf.fandom.com/wiki/La_Bête_du_Gévaudan ("an 18th century soldier"), fortticonderoga.org/news/understanding-french-army-uniforms and militaryheritage.com/charts/7warchtf.htm (French line infantry: greyish-white collarless coat, coloured cuffs, tricorn, white cross-belts). Takeaway: a soldier, not a vampire or a powdered aristocrat; no stills could be opened, so the cut follows the historical uniform.
- **Peter Hale, burned** — teen-wolf-pack.fandom.com/wiki/Peter_Hale (third-degree burns over 75% of his body; "burned from head to toe" leaving the train station), haleyeaianbohen.tumblr.com (his face was burned on the left side), tvline.com Season 6 Episode 5 recap (charred, burning body in the woods), imdb.com Radio Silence trivia (third time he is burned). Takeaway: charred and red-raw patches, mottled scar tissue, left face worst.
- **Klaus** — also wornontv.net/the-originals (black leather moto jacket), Teen Wolf x TVD wiki and Lorelei Gilbert wiki (curled dirty-blond hair, dark blue eyes, pearl-white skin), Pinterest "Klaus Mikaelson henley / necklace" boards (henleys, necklaces).
- **Elijah** — also fanlore.org/wiki/Elijah_Mikaelson (impeccable suits), lorelei-gilbert-series.fandom.com (short brown hair, hazel brown eyes).
- **Kol** — also the-originals-diaries.fandom.com, vampirediaries.fandom.com/wiki/Kol_Mikaelson/Appearance (dark brown hair and eyes).
- **Rebekah** — also the-escape-series.fandom.com/wiki/Rebekah_Mikaelson and reddit.com/r/TheOriginals (natural light blonde, blue eyes; straight or wavy in The Originals).
- **Finn** — also the-escape-series.fandom.com/wiki/Finn_Mikaelson and the-mikaelsons-diaries.fandom.com (brown hair, hazel-green eyes; refined dress coats). Only these two plus the wiki were reachable.
- **Freya** — also shopyourtv.com and wornontv.net/the-originals (brown leather jacket, black lace dress, crop top), true-love.fandom.com/wiki/Freya_Mikaelson (long dirty-blonde hair, pendant).
- **Mikael** — also teen-wolf-x-tvd.fandom.com/wiki/Mikael, reddit.com/r/TheOriginals (the stake "he spent a lot of time making it look nice").
- **Deucalion (human)** — also teen-wolf-pack.fandom.com/wiki/Gideon_Emery and yourprops.com (screen-worn wardrobe listing, page body not readable); sandy-blond hair and blue eyes confirmed by two wikis.
- **Peter Hale (human)** — also hleatherjackets.com and usajacket.com Teen Wolf: The Movie replica listings (brown wool-blend lapel coat).
- **Beast host (Mason Hewitt)** — teen-wolf-pack.fandom.com/wiki/Mason_Hewitt (medium-brown skin, dark brown eyes, short dark brown hair, button-up shirts), heroes-and-villain.fandom.com/wiki/Mason_Hewitt, en.wikipedia.org/wiki/Khylin_Rhambo.

### Round 7 sources (owner's corrections)

- **Deucalion, human** (replaces the blond, blue-eyed face) — stills `File:6x19 Deucalion.png` (short dark-brown hair swept up and across, dark stubble, black knit zip-neck), `File:3x10 Alpha Pack Deucalion Kali.jpg`, `File:3x01 Deucalion and Scott 2.jpg` and `File:3x07 Deucalion cane blade.jpg` (dark-lensed glasses with thin dark frames in human form), `File:5x14 Deucalion 5.png` (pale clouded eyes with a red rim); teen-wolf-pack.fandom.com/wiki/Deucalion ("When he was blind, his eyes were cloudy with a red ring around his iris"; his roar "shattered the lenses of his sunglasses"). The hair colour is the 6x19 hair scaled by that still's skin exposure; the lens glint is sampled from 3x10. The wiki text says "sandy blond", the stills and the owner say dark brown: the stills win.
- **Peter Hale, burned** (tones) — the same stills as round 6: `File:1x04 peter hale burnt.jpg` and `File:1x09 peter burned face.png` show healed, ridged, dull pink-brown scar tissue with a waxy shine and no open blood; teenwolfwiki.com/Peter_Hale+Season_1 ("burned to a crisp") for the char.

### Round 6 sources (polish pass)

- **Deucalion, human** — teen-wolf-pack.fandom.com/wiki/Deucalion ("sandy blond hair", blue eyes, "button-up shirts and sweaters in neutral colors"); stills `File:3x01 Deucalion and Scott 2.jpg` (dark shawl-collar sweater), `File:3x04 Deucalion.png` and `File:5x14 Deucalion 5.png` (short hair swept up and across, light stubble); owner's reference for the grey collar and shoulder panels.
- **Peter Hale, burned** — stills `File:1x04 peter hale burnt.jpg` (ridged ruddy scar tissue from forehead to neck on the right side of his face, hairline burned back at that temple) and `File:1x09 peter burned face.png` (same side; grey button shirt, black leather jacket); teenwolfwiki.com/Peter_Hale+Season_1 ("burned and broken for six years", "burned to a crisp" in Code Breaker); heroes-and-villain.fandom.com/wiki/Peter_Hale (burned three times, burn marks on the side of his face until Wolf's Bane). The stills put the burn on his right; round 5 had it on his left from a blog post.
- **Tricorn** — Metropolitan Museum 26.56.88 (commons.wikimedia.org `File:Tricorne MET 26.56.88.jpg`: black wool, metallic gold lace edge, gold loop and button, black ribbon cockade; the felt and ribbon colours are sampled from it); commons.wikimedia.org `File:Hatt m1763.jpg` (army hat of 1763: felt cocked hat with a tape-bound edge); fashionhistory.fitnyc.edu/tricorne-hat ("generally dark", "often edged with a gold braided trim", worn point forward); en.wikipedia.org/wiki/Tricorne (low crown, brim cocked on three sides, gold or silver lace, a cockade on military hats); Kronoskaf's French regulars in North America, quoted on theminiaturespage.com/boards/msg.mv?id=307469 ("black tricorne laced gold with a black cockade"). The lace reuses the brass of his coat buttons, since French hats were laced in the button colour.
- **Sebastien Valet** — teen-wolf-pack.fandom.com/wiki/Sebastien_Valet (olive skin, black hair "long enough to pull back with a ribbon", his Seven Years' War uniform); stills `File:5x18 Sebastien.gif`, `File:5x18 Sebastien dies.jpg` (bare-headed, long dark hair, white shirt).
- Fandom pages still return 403, but the wiki's `api.php` (page text and image lists) and `static.wikia.nocookie.net` images can be fetched.

## Source of the werewolf UV layout

`de.teamlapen.werewolves:Werewolves:1.21-2.0.3.4` (Gradle cache). `WerewolfBeastModel.createBodyLayer()` was disassembled with `javap -c` and parsed by `tools/parse_model.py` into `tools/beast_model.json` (46 parts: pivots, rotations, boxes, texOffs, mirror). The same data builds the wolf `.bbmodel`s and was checked against the mod's own `beast_0.png`. Only layout numbers are used; none of the mod's textures are copied.

The `.bbmodel` wolves show the model's base pose with feet above the grid (the mod's renderer shifts it in game).
