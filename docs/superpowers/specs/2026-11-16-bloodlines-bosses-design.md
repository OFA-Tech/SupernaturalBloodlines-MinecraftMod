# Bloodlines, NPC packs and show bosses — design

Owner decisions of 2026-11-16. To be committed as `docs/superpowers/specs/2026-11-16-bloodlines-bosses-design.md` once PR #32 (plan 14, packs) is merged.

## Why

The owner wants the world itself to carry the shows' lore. Wild werewolf packs and Alphas should be out there (a second source of sparks besides other players). The Originals should exist, so every vampire's bloodline actually leads somewhere. Show villains should be encounters with unique rewards. Killing an Original kills their whole line, as in The Vampire Diaries, and the lore's escape (breaking the line) should be playable.

## Goals

- **NPC werewolf packs on full moons.** They hunt with clear target priorities, and their Alphas carry a weaker spark that grows.
- **Eight once-per-world show bosses** under their real names: Mikael, Klaus, Elijah, Kol, Rebekah, Finn, Deucalion and Peter Hale. Each has its own encounter trigger and a unique reward.
- **Original bloodlines for every vampire.** Killing an Original with white oak kills their whole line.
- **Two rites to break a line,** shown clearly on the hub's Bloodline page in all three themes.
- **Rite of Severance** also clears Tensura's "named" status.

## Non-goals

- No GeckoLib models for now. Approach C (GeckoLib only for Klaus's hybrid form and Deucalion's Demon Wolf) is a later upgrade.
- No other show characters yet: Esther, Freya, Silas, Kai, the Nogitsune.
- Players never join NPC packs; they can only recruit from them.
- No compatibility with Howling Werewolf. Its ideas are reimplemented in our own code.

## Anti-goals (a failure even if it "works")

- One group making an Original safe to kill for the whole server. The imprisonment rite frees one branch, never the whole line.
- Bosses that spawn so often they become farmable. Each exists once per world, and an Original returns only after days.
- A player dying to a line death with no warning. The Bloodline page has to show whose line you are in before anyone can hunt an Original.

## Constraints

- **Mod rules:** parts never import each other except the documented `supernatural_tensura` exception. Every feature can be switched off per part, with config switches. Old saves load leniently, and every new codec field is optional.
- **Existing rules that stay:**
  - white oak is the only thing that kills an Original (R32); any other lethal damage puts them in the dying or desiccated state;
  - True Alpha sparks are never stolen;
  - stacked sparks (the Deucalion factor) have a cap of `max_stacked_sparks`;
  - the Rite of Severance cuts the direct sire bond.
- **Build target:** NeoForge 1.21.1. Optional linked mods: Vampirism, Werewolves, Enchanted, Tensura. Hub UI kit from plan 16, plus the 10-run test matrix.
- **Tooling:** Blockbench MCP is available for skins, textures, props and structures.

## Approaches considered (bosses)

| | Approach | Trade-off |
|---|---|---|
| **A (chosen)** | Human forms are humanoid NPCs on the player model with skins plus vanilla animations and a few poses. Wolf forms reuse the Werewolves werewolf model with new textures. Blockbench is used for skins, props and the Hale house. | Cheap, matches Minecraft and what players see when they transform, bosses can use real weapons, no new required mod. Fewer showy animations. |
| B | Fully custom GeckoLib models and animations for every boss | Most cinematic; 8× model and animation work, GeckoLib becomes required, and the humans look less like Minecraft. |
| C (later) | A, plus GeckoLib for only Klaus's hybrid form and Deucalion's Demon Wolf | About 80% of A's cost, with two showpiece transformations. |

## Decided design

### 1. Rite of Severance clears the Tensura name (small change)

- On severance, when Tensura is present and the severed sire is the player's Tensura owner (the existing `SubordinateLink.freeFrom` path), also clear Tensura's "named" status, so someone else can name them later.
- The EP and stats from the naming are **kept**.
- An owner who is not the severed sire is left alone, as today.
- **Re-siring after a severance (owner):** naming the player again pays Tensura's naming reward again, because the rite is already very costly. Make it a config parameter, `resire_naming_reward`, default `true`.

### 2. Original bloodlines

- **Every vampire line has a root Original:** Mikael, Finn, Elijah, Kol, Rebekah or Klaus. Players made Original by the rite are their own root.
- **NPC vampires:** they spawn assigned to one Original's line (weighted, configurable). A player turned by an NPC, or by a player in a line, inherits that line. A player with no traceable Original belongs to no line.
- **Line death:** killing an Original with white oak kills **every vampire in their line**, NPCs and players. The exceptions are anyone freed by a rite, and their branch if they used the imprisonment rite.
- **What a line death does:** the death applies even to offline players, at their next login, with a chat message naming the Original.
- **Config:** `original_line_death`, default on.
- **Replaces `sireline_death` (owner):** the old tree-based rule (default off) is removed and folded into this one. Line membership is **stored** per vampire, NPC or player, so the rites can change it. A player made Original by the rite is always the root of their own line, even if an Original sired them before.
- **Dead and missing roots:** once an Original is dead, new NPC vampires never join their line, but the existing members keep it. An Original boss who hasn't appeared yet still has a line. NPC Originals don't count toward `max_originals_per_world`.
- **Which mobs:** every Vampirism vampire mob gets a line except bosses (data attachment, lenient).
- **Discovering your Original (owner):** a non-Original vampire's root Original is **hidden** until they discover it. **The Rite of Revelation is extended** to reveal it, along with the genes it already reveals. Discovery is per player, so your sired don't learn it when you do. Config `original_visibility` = `discovered` (default) | `always`, in the same style as `gene_visibility`.
- **Warning:** when an Original of your line is desiccated (open to a white oak kill), every online member of that line gets a chat warning. If they haven't discovered their Original, it says "**the Original of your bloodline** lies desiccated", with no name. The login message names the Original only once it is discovered; before that it says it is unknown. The line-death message names the Original afterwards.
- **The Broken Line needs that Original's blood**, so in practice you first discover which one it is. The rite simply fails with the wrong blood.
- **Hybrids:** the existing rule that a hybrid does not die with their sire is about the **direct sire**. Line death from an Original applies to hybrids in that line too (Tyler would die with Klaus), unless freed.

### 3. Rite of the Broken Line

- **Performer:** a witch-type (witch, siphoner, heretic or tribrid).
- **Timing:** a lunar eclipse.
- **Ingredients:** blood of **that** Original (dropped by the boss, or traded from Elijah), white oak ash, and the target's own blood (Sire's Blood-style bound bottle).
- **Effect:** the target stops being in that Original's line and becomes the head of their own line. They do **not** become an Original. Their direct sire bond stays (that is what the Severance rite is for).
- **Descendants:** everyone the target sired or turned, players and NPCs, **stays** in the Original's line.

### 4. Rite of the Imprisoned Original (harder)

- **Requirement:** the Original must be trapped inside a closed mountain ash circle at the rite's centre for the whole rite. Mountain ash already blocks vampires. If the circle breaks, the rite fails.
- **Effect:** frees the target **and their whole branch** (everyone below them) from that Original's line.
- **Cost:** much higher than the Broken Line.
- **Limit:** it never frees the Original's whole line, by design.
- **Player Originals (owner):** both rites also work against a player Original. Getting their blood, or trapping them in mountain ash, is fair PvP.

### 5. Bloodline page, all three themes

**Content (same in every theme):**
- the line above the player up to the crowned Original root. Until the player has discovered it (Rite of Revelation), the root is a **"?"** node. NPC ancestors are also "?"; player ancestors are always shown;
- if the player is free, the link above them drawn **broken**, with a tooltip like "Line broken from Klaus (Rite of the Broken Line)", and an **open padlock** on their portrait;
- descendants below, whose links run **faintly past** the player up to the Original;
- the selected node's details: name, species, sire, line status, and the actions Call / Severance / Broken Line;
- the gene vials, kept as today;
- states: You are an Original (root), Not a vampire, Sire unknown, In a line, Free.

**Per theme:**
- **Grimoire:** an ink tree on the left page with the break as torn or burned ink and a red margin note. A wax padlock seal on the player. Descendants' lines drawn as faint dotted ink. Details on the right page.
- **Blood Moon:** the Original as a portrait inside the moon. The line hangs as a chain of portraits, with broken glowing chain links at the break. A thin red thread runs from the descendants behind the player to the moon. Details in the bottom bar.
- **Tensura:** hex nodes with teal connectors. A red dashed connector labelled "SEVERED". Details in the right info pane.

**Keyboard and narration:** arrow keys move along the tree. Narration names the break, for example "Tyler, your sire. Link above you broken: you are free of Klaus's line."

The page is mocked in the plan 16 Figma file (USFBf74c4I2BWDyfmRxFZO) before it is built.

### 6. NPC werewolf packs (folded into plan 15)

- **Spawning:** mainly on full-moon nights. Packs of NPC werewolves (the Werewolves mod's basic and human-form werewolves) are led by an NPC Alpha, the Werewolves `WerewolfAlphaEntity`. Lone wolves exist too.
- **Behaviour:** usually aggressive. Target priority: **vampire > hunter > other player > prey animals** (sheep, cows and so on). They hunt together.
- **NPC Alpha spark:** killing an NPC Alpha gives a **weaker spark** that **grows inside the holder over time** into a full spark. It works with stacking and the cap.
- **Recruiting:** players can **recruit** NPC pack wolves into their own pack, which ties into plan 15's NPC pack members. They cannot join an NPC pack.

### 7. Show bosses (plan 17), approach A

| Boss | Encounter | Unique reward |
|---|---|---|
| Mikael | Hunts a vampire player who reached a high rung, at night; feeds on vampires | **Mikael's Stake**: white oak stake that never breaks |
| Klaus | Full moon near a doppelgänger (villager or player); comes to sacrifice them | **Klaus's Blood**: counts as Original hybrid blood (makes hybrids), cures werewolf venom |
| Elijah | Not hostile at first; offers a deal; fights if attacked or betrayed | **The Elixir**: one-time return from death |
| Kol | Hunts Hunters of the Five whose mark is high | **Kol's Grimoire**: a witch rite plus Witch SP |
| Rebekah | Appears when someone makes The Cure or performs its rite | **The True Cure**: the only cure that works on an Original |
| Finn | Lunar eclipse witch ritual to link and kill the Originals | **Esther's Grimoire**: the Linking Spell rite |
| Deucalion | Full moon, comes for a player Alpha with 3+ stacked sparks, with his Alpha pack | **All his sparks** (up to the holder's cap) plus **Demon Wolf Roar** (makes other Alphas shift) |
| Peter Hale | Burned Hale house structure in a forest; feral on full moons | **His Alpha spark** plus **Memory Claws** (reveals a target's genes and sire; a new gene-discovery path) |

**Rules:**
- Each boss exists **once per world**.
- **Originals die only to white oak.** Any other defeat desiccates them, and they return after some in-game days (config). A white oak death is permanent and triggers line death.
- Deucalion and Peter can die normally. Their return rules are up to the builder, but must not be farmable.
- Original's blood for the Broken Line comes from defeating that Original (desiccation drop) or from Elijah's deal.
- **Deucalion and Peter** don't come back once killed (optional respawn config, off by default). Deucalion carries a fixed 4 sparks.

**Reward details (owner-approved):**
- **Esther's Grimoire / Linking Spell:** links **two creatures** for a while, so damage to one also hits the other. It never links the Originals together.
- **The True Cure:** the existing Cure keeps working on a desiccated Original. The True Cure is the one an Original can **drink willingly**, without being desiccated.
- **The Elixir:** a one-use item that returns you from any death **except a line death**.
- **Klaus's Blood:** always counts as Original-hybrid blood whatever `hybrid_transition_blood` says. The transition death still applies.
- **Memory Claws:** discover the target's genes, the same way as the Rite of Revelation, and respect `gene_visibility`.
- **Kol's Grimoire:** a **Cloaking rite** that hides you from the Vampire Compass and from hunter sense for a while, plus Witch SP.
- **Weaker NPC spark:** counts as nothing (not toward the cap, not toward Deucalion's 3+) until it has grown. It grows over a few in-game days, only while the holder is online. It can't be stolen while weak and is lost on a cure.

## Suggested build order (separate PRs)

1. Severance clears the Tensura name (small).
2. Original bloodlines, line death, both line-breaking rites, and the Bloodline page in three themes (after the Figma mock is approved).
3. Plan 15 (Howling ideas) including NPC werewolf packs, the weaker growing spark, and recruiting.
4. Plan 17: the eight bosses, approach A.
5. Later: approach C transformations.

## Research before PR 1

- Check whether Tensura's API can clear the "named" status without a mixin. Use reflection on a public field or method if there is one; use a mixin only as a last resort, within the part's mixin budget.

## Left to the builder

- Exact numbers: spawn weights, spark growth time, rite power costs, boss health and damage, desiccation return time. They follow the `docs/balance.md` rules and are added to it.
- How NPC vampires get a line (spawn event versus lazy assignment).
- The Hale house structure (a Blockbench model or a vanilla structure template).
- Boss AI details within the encounter descriptions.
