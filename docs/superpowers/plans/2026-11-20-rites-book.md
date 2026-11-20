# Rites in Enchanted's book + a real circle on the hub Rites page

Status: research done, nothing built. Branch to be created by the owner. Read-only research; no Gradle runs.

## 1. How Enchanted 4.2.7 shows rites (verified in the jar and the -sources jars)

- The book is **Modopedia** (`modopedia` is a required dependency of Enchanted). Book `enchanted:art_of_witchcraft` is `data/enchanted/modopedia/books/art_of_witchcraft.json`.
- Content is **client resource-pack data**, not datapack: `assets/<any namespace>/modopedia/books/art_of_witchcraft/<lang>/{categories,entries}/**.json`.
  `BookContentLoader.getBookResources` uses `FileToIdConverter.json("modopedia/books/art_of_witchcraft").listMatchingResources(...)`, which scans **every namespace**. So we **can** add chapters and entries from `assets/supernaturalbloodlines/modopedia/books/art_of_witchcraft/en_us/...` with no code at all.
- Sharp edges (from `BookContentLoader.parseBookResources` / `BookContentImpl.getContent`):
  1. Ids are the **path only** (namespace is dropped): `en_us/entries/<id>` and `en_us/categories/<id>`. Our ids must be unique against Enchanted's: prefix everything with `supernaturalbloodlines/`. A same-path file would silently replace Enchanted's.
  2. Content is per language and `getContent(lang)` returns that language's map **only**, falling back to `en_us` only when the language has no content at all. If we ship `pt_br`, a pt_br player loses the whole of Enchanted's book (it only ships `en_us`). **Ship `en_us` only.**
  3. Entry/category text is literal strings, not lang keys. No `neoforge:conditions` on entries.
  4. We cannot add children to Enchanted's `circle_magic` category without overriding its file (same path = fragile). Use our own top-level category.
- Category JSON: `title, landing_text, icon, sort_num, children[], entries[], display_on_front_page`. Entry JSON: `title, icon, assigned_items[], advancement?, pages[]`. Link syntax in text: `$(b)$(el:category/entry)text$(/el)$(/b)`.
- Rite page = template `enchanted:page/rite` with `"rite": "<rite id>"` -> page component `enchanted:rite` (`RitePageComponent`). It reads the rite from the **synced datapack registry** `EData.RITE_TYPES_REGISTRY` (throws `"<id> is not a valid rite"` if missing; `loadPages` catches and **drops that page**, logging an error). It draws:
  - the gold heart glyph (`textures/gui/gold_glyph.png`), then per `rite.getShapes()` the PNG `textures/gui/circle_magic_shapes/<small|medium|large>_circle_<ritual|nether|otherwhere>_chalk.png` (100x100) via `EnchantedClient.getShapeGuiTexture`;
  - the items in rings (`RingsItemDisplay`) on the circle, hover tooltip lists shapes and chalk;
  - widgets under it with tooltips: power, tick power, time range, weather, sacrifices (`rite.getEntities()`).
  Anything that is only in our factory code (full moon, who may perform, target) is **not** shown by the widget. The text page must say it.
- Enchanted shapes (data/enchanted/enchanted/circle_magic/shape): small 7x7 (chalk radius 3), medium 11x11 (radius 5), large 15x15 (radius 7). Chalks: ritual, nether (infernal), otherwhere. Heart glyph = golden chalk in the centre. Our rites only use valid combos.
- Opening at an entry: `net.favouriteless.modopedia.client.BookOpenHandler.tryOpenEntry(ResourceLocation bookId, String entry)` (we already reflect `tryOpenBook` in `HubClient`).
- Our Modopedia is `compileOnly`-absent (only `localRuntime`). The pack has modopedia 1.1.11, the build 1.1.10: re-check the API when adding `compileOnly`.

## 2. Our rites (31, `data/supernaturalbloodlines/enchanted/circle_magic/rite/`)

Circle = chalk. All ritual chalk unless said. P = altar power. Needs = extra condition seen in JSON or factory.

| Rite (file) | Circle | Items | P | Needs / notes |
|---|---|---|---|---|
| alpha_trial | medium | moonstone, silver ingot, 8 bone | 12000 | werewolves; full moon; werewolf Beta/Omega in beast form |
| ancestral_calling | small | bone, amethyst shard, lapis | 4000 | |
| blood_offering | small | 4 blood bottle, stone -> blood stone | 1000 | vampirism |
| cleansing_blood | medium | garlic, holy water, golden apple | 4000 | vampirism |
| curse_magicule_drain | medium **nether** | taglock, low magic crystal, soul sand | 2000 | tensura |
| curse_skill_seal | medium **nether** | taglock, medium magic crystal, soul sand | 2000 | tensura |
| doppelganger_blood | small | taglock, 2 diamond, amethyst | 6000 | werewolves |
| hearing_device | medium | note block, 2 iron, amethyst | 3000 | |
| hybrid_blood | small | glass bottle | 1000 | werewolves |
| immortality | medium | white oak sapling, golden apple, human heart | 12000 | villager sacrifice (JSON `entities`); full moon (factory) |
| magicule_offering | small | low magic crystal, amethyst | **0** | tensura; power 0 hides the power widget |
| original_blood | small | glass bottle | 1000 | |
| purification | medium | 2 gold, glowstone dust | 3000 | acts in radius **8** (circle is 5) |
| purification_holy_water | medium | gold, holy water | 3000 | same |
| rite_of_cloaking | medium | Kol grimoire, 2 phantom membrane, ender pearl | 4000 | witch types only |
| rite_of_lineage_awaken | medium | moonstone, 3 bone | 9000 | werewolves |
| rite_of_lineage_reveal | small | bone, glowstone dust, paper | 3000 | werewolves |
| rite_of_linking | medium | Esther grimoire, 2 lead, 2 amethyst | 5000 | witch types; effect radius 5.5 (ok for medium); 2nd creature must consent |
| rite_of_revelation | small | taglock | 1000 | |
| rite_of_severance | medium | sire's blood, spirit of otherwhere | 6000 | **full moon night (code only)**, witch types, target stands at centre |
| rite_of_the_broken_line | medium | 2 sire's blood, white oak ash, spirit of otherwhere | 12000 | |
| rite_of_the_cure | medium | 2 white oak ash, 4 vervain, enchanted golden apple | 20000 | desiccated Original within radius **6** (circle is 5) |
| rite_of_the_five | medium | 5 candle, iron ingot, book, taglock | 10000 | hunter |
| rite_of_the_imprisoned_original | **large** | sire's blood, 3 white oak ash, 2 spirit of otherwhere | 36000 | **closed mountain-ash ring** (`imprisonment_ring_max_radius`, default 8), Original at centre, lunar eclipse, runs `imprisonment_rite_seconds`; none of it is in the JSON |
| rite_of_the_moon | medium | moonstone, original blood | 15000 | villager+wolf sacrifice (JSON); full moon; werewolves |
| silver_moon | medium | 2 silver ingot, wolfsbane | 4000 | werewolves; full moon |
| vampire_compass | medium | compass, vervain | 2000 | |
| ward_of_daylight | medium | sun ring, 2 sunflower, 4 glowstone dust | 6000 | vampire |
| ward_of_return | medium | totem, 4 gold, diamond | 8000 | |
| ward_of_the_moon | medium | moon ring, 2 amethyst, 2 silver ingot | 6000 | werewolves |
| werewolf_brother | small | moonstone, beef, silver nugget | 6000 | werewolves |

### Circle and data problems found

1. **The hub never draws the rite's circle.** `BookRitesPage.circle` (also used by `StageRitesPage`) draws the same two tick rings and 8 squares for every rite; `lit = items.size()` makes the squares look like item slots. `RitesPage` (flat theme) prints only "Circle: medium circle".
2. **Chalk type is lost.** `SpeciesHub.rites()` sends `rite.getShapes().keySet()` only; the chalk block (the map value) is dropped. `curse_magicule_drain` and `curse_skill_seal` need **infernal (nether) chalk** but the hub says "medium circle". A player who draws ritual chalk gets a rite that never starts. The heart glyph (golden chalk) is not mentioned either.
3. **Imprisoned Original**: large circle (chalk radius 7) plus a mountain-ash ring up to radius 8 (default), i.e. possibly outside the chalk, and the Original stands in the middle. No text in hub or book says so. Gametest ring is radius 6 (inside the chalk). Decide a documented ring (see open question 3).
4. **Cure** checks a desiccated Original within 6 blocks, medium chalk is radius 5. **Purification** hits creatures within 8. Say so ("within 6 blocks of the centre", "everything within 8 blocks").
5. **Full moon is code-only for Severance** (and Alpha Trial); JSON has `requires_full_moon` only for immortality, moon, silver moon. Neither the hub nor Enchanted's widgets show it.
6. **Sacrifices are not on the hub**: `HubSocial.Rite` has no `entities`; immortality and moon need villager (and wolf).
7. **Descriptions**: only `rite_of_cloaking`, `rite_of_linking` (+ alpha trial notes) have `hub.rites.desc.*`. The other 28 have nothing but a name.
8. **magicule_offering power 0**: intended (it takes magicules) or a mistake? Hub prints "Power: 0".
9. A rite whose part or mod is off is not in the registry, so a book page for it is dropped with an error on every reload.

## 3. Plan

### Phase 1 - Rite facts on the wire (small, unblocks everything)
- `HubSocial.Rite`: `shapes` becomes `"<shape>=<chalk>"` pairs (e.g. `enchanted:medium_circle=enchanted:nether_chalk`); add `sacrifices` (entity ids), `fullMoon` (factory `requires_full_moon`, plus a new `requires_full_moon: true` in Severance and Alpha Trial JSON so data and code agree), `tickPower`. Update `HubSnapshotPayload.RITE` codec and `SpeciesHub.rites()`.
- Pure helper `RiteFacts` (core, testable) parses the pair strings.

### Phase 2 - Book chapters (data only, no Java)
- `assets/supernaturalbloodlines/modopedia/books/art_of_witchcraft/en_us/`
  - `categories/supernaturalbloodlines.json` (top level, `sort_num` 6, front page) with children:
    `supernaturalbloodlines/vampire`, `/werewolf`, `/witch`, `/lines` (Originals), `/artifacts` (wards, compass, device), `/tensura`. Plus a short "Supernatural rites" intro entry that links to Enchanted's `circle_magic/tutorial/performing_rites`.
  - `entries/supernaturalbloodlines/rites/<path>.json` x 31. Page 1 `modopedia:page/headered_text`; page 2 `enchanted:page/rite` with the rite id. Long texts get a second text page.
  - `assigned_items`: the produced items (sun ring, moon ring, compass, hearing device, resurrection ring, blood stone) so the item lookup opens the entry.
- Entry text order (always the same): what it does; who can perform it; who/what stands where (centre, in the circle); cost (power, items spent, what is given back on a refusal); conditions (full moon, eclipse, sacrifice, mod); warnings (cannot be undone, hits radius N).
- Source of truth: the existing lang keys `supernaturalbloodlines.hub.rites.desc.<path>.1..3` (finish the missing 28). A script `tools/gen_rites_book.py` (Python is `C:\Users\caiom\AppData\Local\Python\pythoncore-3.14-64\python.exe`) writes the 31 entries and the category files from the rite JSONs + `en_us.json`; the circle line ("Medium circle, infernal chalk, heart glyph in golden chalk") comes from the rite JSON so it cannot drift. Committed output; a test fails if it is stale.
- `en_us` only (see section 1). Other languages keep the hub lang files.

### Phase 3 - Placeholder for rites that are off
- Register `supernaturalbloodlines:rite` page component (client, `PageComponentRegistry`): wraps `RitePageComponent`; if the rite is not in the registry it draws "Not available on this server (part/mod off)" instead of the page being dropped. Entry template points to it. Needs `compileOnly` Modopedia (1.1.10) in `build.gradle`; check against pack 1.1.11.
- Hub "Open Enchanted's book" button deep-links to the chosen rite's entry: add reflection for `BookOpenHandler.tryOpenEntry(book id, "supernaturalbloodlines/rites/<path>")` next to `tryOpenBook` in `HubClient`.

### Phase 4 - Hub Rites page (all 3 themes + flat)
- One shared `RiteDiagram` in `shared/hub/kit/client`: blits Enchanted's own `gold_glyph.png` and `circle_magic_shapes/<shape>_<chalk>.png` at 100 px (scaled down to the region width, min 64), on a dark plate drawn in the theme's ink (white ritual chalk is unreadable on Grimoire parchment); items laid out the way Enchanted does (rings around the centre, 6 per ring) as real item icons with count and hover tooltip; sacrifice and moon/eclipse/power chips under it with tooltips; a small "heart glyph = golden chalk" label.
- Replace `BookRitesPage.circle` (Grimoire, also called by `StageRitesPage` for Blood Moon and Tensura) and add the diagram to the flat `RitesPage`; keep "have / missing" from the player's inventory per item.
- Layout maths in a pure `RiteDiagram.layout(...)` (positions of ring items) so it can be unit tested.
- Show the Phase 2 description lines on all pages, and "Open in Enchanted's book" opens that entry.
- Re-shoot the dev screenshots (`DevDemo`/`HubDev`) for the 3 themes with: a small, a medium nether, the large imprisoned rite.

### Phase 5 - Fix the data problems from section 2
- Imprisoned: text + book say ring and Original at centre; decide ring radius (open question 3).
- Cure and Purification: say the real radius.
- Add the 28 missing `desc` lines; magicule offering power decision.

## 4. Testing

- `RiteBookDataTest` (file walk, like `ItemModelCoverageTest`): for every rite JSON a) entry exists at `entries/supernaturalbloodlines/rites/<path>.json`, b) its rite page id equals the rite id, c) exactly one of our categories lists it and every listed entry exists, d) every shape/chalk pair is one of the 3x3 Enchanted has, e) only an `en_us` folder exists, f) no id lacks the `supernaturalbloodlines/` prefix, g) the generated files equal the committed ones, h) lang `desc.<path>.1` exists for every rite, i) every `requires_full_moon` in JSON is mentioned in the text.
- `RiteFactsTest`: the pair parser; `RiteDiagramTest`: layout puts each item once, no overlap, <= 3 rings.
- Existing `HubRitesTest` stays green (ordering, faction).
- GameTest: with all parts on, every rite of ours is in the `RITE_TYPES_REGISTRY`, and `SpeciesHub.rites()` gives the chalk for the two nether curses.
- Manual: open the book in the pack (modopedia 1.1.11) with all parts on and with `species` off (placeholder, no error spam in `latest.log`); switch language to pt_br and check Enchanted's chapters are still there; screenshot the 3 hub themes.

## 5. Decisions (owner accepted every recommended answer)

1. Own top-level chapter, not editing Enchanted's "Circle Magic". DECIDED.
2. `en_us` only in the book. DECIDED.
3. Imprisoned ring: inside the chalk, 6 blocks from the centre; config max stays 8. DECIDED.
4. No advancement gating. DECIDED.
5. Entries generated by a script, with a stale test. DECIDED.
6. Modopedia `compileOnly` for the placeholder component; reflection for the open calls. DECIDED.
7. `magicule_offering` costs magicules and no altar power; say so. DECIDED.
8. `requires_full_moon` goes into the Severance and Alpha Trial JSON. DECIDED.
9. A rite that is off shows placeholder text in the book. DECIDED.
