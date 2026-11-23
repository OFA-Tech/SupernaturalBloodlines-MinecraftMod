# Bewitchment: the parts plan 21 uses

> Copied from the full survey kept at `C:\Users\caiom\AppData\Local\Temp\opencode\bewitchment\bewitchment.md` (2026-11-23). Only what plan 21 ("TVD rites") reads is kept: the licence facts, the curse table, sigils and the crystal ball, and section 6 (what is worth adding). **Bewitchment is All Rights Reserved: ideas only, no code, art, lang text or book prose.** Show-canon claims in it are from memory and not verified.

## 1. Licence and what we may take
| | |
|---|---|
| **Licence** | **All Rights Reserved** (`fabric.mod.json` `"license"`). CurseForge says "All Rights Reserved", Modrinth says "Licensed ARR". Every source file has the header `All Rights Reserved (c) MoriyaShiine`. The repo has no LICENSE file | [fabric.mod.json](https://raw.githubusercontent.com/MoriyaShiine/bewitchment/main/src/main/resources/fabric.mod.json), [CurseForge](https://www.curseforge.com/minecraft/mc-mods/bewitchment), [Modrinth](https://modrinth.com/mod/bewitchment), [sample header](https://raw.githubusercontent.com/MoriyaShiine/bewitchment/main/src/main/java/moriyashiine/bewitchment/common/item/ContractItem.java) |
| What the licence allows | The page says showcases, videos and modpacks hosted on CurseForge or Modrinth are fine. **Nothing is said about reusing code, art, models, sounds, lang text or the Patchouli book.** Under ARR we cannot copy, port or fork it | [CurseForge description](https://www.curseforge.com/minecraft/mc-mods/bewitchment) |


### 2.2 Curses (13), cast on a brazier with a taglock

Registry: [BWCurses.java](https://github.com/MoriyaShiine/bewitchment/blob/main/src/main/java/moriyashiine/bewitchment/common/registry/BWCurses.java). Cast with four ingredients in a **Brazier** lit by flint and steel. They last 7 days. Lesser curses need 400 ME, greater ones 800. They are lifted by the Cleansing ritual. Source: [conducting_curses](https://github.com/MoriyaShiine/bewitchment/blob/main/src/main/resources/assets/bewitchment/patchouli_books/book_of_shadows/en_us/entries/curses/conducting_curses.json).

| Curse | Effect | Source |
|---|---|---|
| Solar Hatred | sunlight burns the target | [solar_hatred](https://github.com/MoriyaShiine/bewitchment/blob/main/src/main/resources/assets/bewitchment/patchouli_books/book_of_shadows/en_us/entries/curses/solar_hatred.json) |
| Misfortune | random Poison, Weakness, Blindness, Nausea | [misfortune](https://github.com/MoriyaShiine/bewitchment/blob/main/src/main/resources/assets/bewitchment/patchouli_books/book_of_shadows/en_us/entries/curses/misfortune.json) |
| Weak Lungs | drowns at once underwater | [weak_lungs](https://github.com/MoriyaShiine/bewitchment/blob/main/src/main/resources/assets/bewitchment/patchouli_books/book_of_shadows/en_us/entries/curses/weak_lungs.json) |
| Outrage | neutral mobs turn hostile | [outrage](https://github.com/MoriyaShiine/bewitchment/blob/main/src/main/resources/assets/bewitchment/patchouli_books/book_of_shadows/en_us/entries/curses/outrage.json) |
| Forest's Wrath | double damage from fire and axes | [forests_wrath](https://github.com/MoriyaShiine/bewitchment/blob/main/src/main/resources/assets/bewitchment/patchouli_books/book_of_shadows/en_us/entries/curses/forests_wrath.json) |
| Lightning Rod | lightning redirects to the target | [lightning_rod](https://github.com/MoriyaShiine/bewitchment/blob/main/src/main/resources/assets/bewitchment/patchouli_books/book_of_shadows/en_us/entries/curses/lightning_rod.json) |
| Unlucky | bad luck, and beneficial effects may fail | [unlucky](https://github.com/MoriyaShiine/bewitchment/blob/main/src/main/resources/assets/bewitchment/patchouli_books/book_of_shadows/en_us/entries/curses/unlucky.json) |
| Compromised | negative effects hit harder | [compromised](https://github.com/MoriyaShiine/bewitchment/blob/main/src/main/resources/assets/bewitchment/patchouli_books/book_of_shadows/en_us/entries/curses/compromised.json) |
| Army of Worms | crops near the target stop growing | [army_of_worms](https://github.com/MoriyaShiine/bewitchment/blob/main/src/main/resources/assets/bewitchment/patchouli_books/book_of_shadows/en_us/entries/curses/army_of_worms.json) |
| Arachnophobia | fear of spiders | [arachnophobia](https://github.com/MoriyaShiine/bewitchment/blob/main/src/main/resources/assets/bewitchment/patchouli_books/book_of_shadows/en_us/entries/curses/arachnophobia.json) |
| **Insanity** (greater) | the target sees monsters that are not there | [insanity](https://github.com/MoriyaShiine/bewitchment/blob/main/src/main/resources/assets/bewitchment/patchouli_books/book_of_shadows/en_us/entries/curses/insanity.json) |
| **Susceptibility** (greater) | double magic damage and slower movement. Also the first step to catching Vampirism or Lycanthropy | [susceptibility](https://github.com/MoriyaShiine/bewitchment/blob/main/src/main/resources/assets/bewitchment/patchouli_books/book_of_shadows/en_us/entries/curses/susceptibility.json) |
| Apathy (greater) | no ME, villagers and demons refuse to trade, animals flee | [apathy](https://github.com/MoriyaShiine/bewitchment/blob/main/src/main/resources/assets/bewitchment/patchouli_books/book_of_shadows/en_us/entries/curses/apathy.json) |

Recipes: [curses recipe folder](https://github.com/MoriyaShiine/bewitchment/tree/main/src/main/resources/data/bewitchment/recipes/curses).

## 2. Sigils and the taglock list
### 2.7 Sigils (12) and the Taglock list

Registry: [BWSigils.java](https://github.com/MoriyaShiine/bewitchment/blob/main/src/main/java/moriyashiine/bewitchment/common/registry/BWSigils.java). How they work: [using_sigils](https://github.com/MoriyaShiine/bewitchment/blob/main/src/main/resources/assets/bewitchment/patchouli_books/book_of_shadows/en_us/entries/sigils/using_sigils.json).

- **Active sigils** go on usable blocks, or on brooms made from Dragon's Blood wood. Mending, Cleansing, Judgment, Decay, Shrieking, Sentinel.
- **Passive sigils** go anywhere and affect everything around. Slippery (nobody can take taglocks), Shadows (blocks scrying), Extending (doubles incense time), Smelly (draws hostile mobs), Ruin (blocks positive potion effects), Heavy (more fall damage).
- Taglocks can be added to a sigil to make it a **whitelist or blacklist** (Athame + sneak toggles). All sigils expire after a time.

## 3. Fortunes and the Crystal Ball
### 2.8 Fortunes and the Crystal Ball

- 12 fortunes: six good (Power, Wisdom, Courage, Treasure, Hawkeye, Merchant) and six bad (Illness, Infested, Thunderbolt, Clumsiness, Encounter, Exhausted). Registry: [BWFortunes.java](https://github.com/MoriyaShiine/bewitchment/blob/main/src/main/java/moriyashiine/bewitchment/common/registry/BWFortunes.java); text: [lang en_us.json](https://github.com/MoriyaShiine/bewitchment/blob/main/src/main/resources/assets/bewitchment/lang/en_us.json) (keys `fortune.bewitchment.*`).
- The **Crystal Ball** gives one fortune for a little altar ME. It can also **scry a taglock**: "precise information on the target". Shadows sigils block it ("Something obstructs your vision"). Source: [crystal_ball](https://github.com/MoriyaShiine/bewitchment/blob/main/src/main/resources/assets/bewitchment/patchouli_books/book_of_shadows/en_us/entries/tools/crystal_ball.json), [CrystalBallBlock.java](https://github.com/MoriyaShiine/bewitchment/blob/main/src/main/java/moriyashiine/bewitchment/common/block/CrystalBallBlock.java).

## 6. Worth adding: details

All are ideas only. Nothing here is copied from BW. Clean-room note: I read BW's public Book of Shadows text and parts of its source to describe behaviour. The implementer should work from this document's descriptions, with our own numbers, names, art and text.

### 6.1 Locator rite (scrying with a taglock) — size M

- **Idea (from BW):** the Crystal Ball gives "precise information" about a taglock's owner, and its Shadows sigil blocks that. Source: [crystal_ball](https://github.com/MoriyaShiine/bewitchment/blob/main/src/main/resources/assets/bewitchment/patchouli_books/book_of_shadows/en_us/entries/tools/crystal_ball.json).
- **Fit:** the TVD witch "locator spell" is a signature scene. I did not re-check show canon in this session.
- **Engine:** a new `RiteFactory` in the species or artifacts part, shaped like our *Rite of Revelation* (small circle, one filled taglock, witch type performer, power about 2,000 before the witch discount).
  - Output: a one-use *Locator* item or a mode on the existing Vampire Compass. The Compass already has a sneak-use second mode (doppelgänger mode, `wiki/Items-And-Blocks.md`), so a "taglock" mode reuses its needle code.
  - Reads the target's current dimension and position. It works only while the owner is online. A Rite of Cloaking cloak already hides players from the Compass, so it would hide from this too.
- **Hub:** it appears automatically in the hub's Rites page ("every rite of this mod the server has"). A ladder task such as "locate three people" can use the existing task system (`supernatural/ladder/witch.json`).
- **Guards:** server config (`locator_enabled`, default on), no coordinates in chat (direction and a rough distance only), an option for the target to be told, and the PvP/claims check we already do for rites.
- **Size reasoning:** one new factory, one item or compass mode, lang strings, tests, a hub line.

### 6.2 Threshold / invitation ward — size M (a variant of it is S)

- **Idea (from BW):** taglock white/blacklists on passive sigils, and Elder blocks locked with a silver ingot and a taglock list. Source: [using_sigils](https://github.com/MoriyaShiine/bewitchment/blob/main/src/main/resources/assets/bewitchment/patchouli_books/book_of_shadows/en_us/entries/sigils/using_sigils.json), [elder_trees](https://github.com/MoriyaShiine/bewitchment/blob/main/src/main/resources/assets/bewitchment/patchouli_books/book_of_shadows/en_us/entries/introduction/elder_trees.json).
- **Fit:** the TVD invitation rule (a vampire cannot enter a home unless invited). I did not re-verify it against the show in this session. It complements mountain ash, which blocks every supernatural, with a softer, owner-controlled gate.
- **Engine:** a medium-circle rite performed by a witch type (like *Rite of Severance*) that binds a small region around a door or a placed block. Invited names come from the same consent gesture our blood rites use (the invited player sneaks on the circle centre). An uninvited vampire, hybrid or heretic is pushed back by the code that already pushes at mountain ash lines (Ashwarden).
- **Hub:** a line on the Rites page. The Bloodline page could list "homes where you are invited".
- **Size reasoning:** new saved region data, enforcement tick, invite/uninvite commands, tests. Smaller variant (S): only a block-lock for doors that opens for a list of invited players, no pushing.

### 6.3 Two small curses — size S each

- **Insanity (greater):** the target sees monsters that are not there. We already have hallucination logic (Hunter's curse R83 and werewolf venom R42v). Show fit is loose ("visions/hallucinations"). Source: [insanity](https://github.com/MoriyaShiine/bewitchment/blob/main/src/main/resources/assets/bewitchment/patchouli_books/book_of_shadows/en_us/entries/curses/insanity.json).
- **Susceptibility (greater):** takes twice the magic damage and moves slower. Reuses Shocked-style effects and Tensura's magic damage. Source: [susceptibility](https://github.com/MoriyaShiine/bewitchment/blob/main/src/main/resources/assets/bewitchment/patchouli_books/book_of_shadows/en_us/entries/curses/susceptibility.json).
- **Engine:** the same plumbing as our two Tensura curses (a `Curse` class registered with `CurseManager`, a rite JSON on nether chalk, around 2,000 power; see `docs/superpowers/plans/02-enchanted-tensura.md`). No Tensura needed for these two, so they would sit in the `species` part.
- **Ingredients to pick ourselves**, from show-themed items (vervain, moonstone, white oak ash).

### 6.4 Curse poppet — size S–M, low priority

- **Idea (from BW):** the poppet absorbs one curse like a sponge. Source: [curse.json](https://github.com/MoriyaShiine/bewitchment/blob/main/src/main/resources/assets/bewitchment/patchouli_books/book_of_shadows/en_us/entries/poppets/curse.json).
- **Fit:** our Skill Seal and Magicule Drain are harsh, and today they can only be lifted by Enchanted's remove-curse rite. A poppet gives a pre-emptive counter.
- **Engine:** an item in the spinning-wheel recipe folder like our Sun/Silver/Spirit poppets, plus a hook in the curse-apply path. The BW "rebind it to curse someone else" half is not needed.
- **Why low:** it needs a hook into Enchanted's curse application, and we already have `AltarRangeMixin`-style mixins that the wiki calls fragile (`wiki/Witches-And-Siphoners.md`).

---

## 7. Licence summary and what to do

- ARR in four places (CurseForge, Modrinth, `fabric.mod.json`, every source file). Copy no code, textures, models, sounds, lang text, recipe sets or Patchouli prose.
- Fine: mechanics and ideas, since game mechanics are not protected by copyright. Re-implement with our own names, numbers, art and text. Do not paste BW's Patchouli wording into our wiki or lang files.
- If any of the 6.x items go ahead, write the design from section 6, not from BW's code.

## 8. Not verified

- Show canon claims (the invitation rule, the locator spell, "vampires spawn on new moons") are from memory, not checked this session.
- Whether Werewolves already drops armour and tools in beast form.
- Exact current Enchanted curse list and whether its `CommandRite` can be fully data-driven. I saw the file names in the repo tree, not their contents.
- I did not open BW's brewing recipes, Demon trade lists or Fortune implementations in detail, and I did not run BW.
- Nycto's content was read only from its Modrinth page.
