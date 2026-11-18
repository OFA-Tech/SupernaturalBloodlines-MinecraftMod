# Hub information architecture (2026-11-17)

Status: **proposal for the owner. Nothing here is built.** No code was changed; the Figma mockups are paused (see Appendix A).

Owner brief (2026-11-17): "I want the full overview of the mod, and all the commands and features well placed inside the UI, not just dropped anywhere. The players' and NPCs' profiles in the pack should also be in the bloodline family tree, and attributes should be unknown until you know them (or are in the pack, which shares them all at once)."

How this was made: impeccable `shape` playbook (discovery, then a brief, no code). There is no `PRODUCT.md` and no human to interview in a subagent run, so the discovery answers below are **inferred from the owner's words, the audit and the code**, and every inference is marked "assumed". The open questions in section 6 are the interview. Surface mode for every screen in this document is **Operate** (the player is in a task; familiar patterns beat novelty; brand lives in details).

Sources read: `wiki/*.md` (all pages listed in `Home`), `docs/parts.md`, `docs/parts/*.md` (headings and hook sections), `docs/hub-ui-kit.md`, `docs/superpowers/specs/2026-11-17-ui-first-audit.md`, `2026-11-16-bloodlines-bosses-design.md`, `2026-11-17-beast-of-gevaudan.md`, `SPECIES-RULES.md` (pack and gene rules), `docs/superpowers/plans/2026-11-17-ui-first.md`, `lang/en_us.json`, and `parts/supernatural/client/hub/**`, `shared/hub/**`, `shared/supernatural/HubSocial.java`, `parts/supernatural/net/HubSnapshotPayload.java`.

## 0. The brief (shape phase 3)

- **Job and audience.** A player of a modded survival server (Vampirism + Werewolves + Enchanted + Tensura + this mod) who has just become something, or is about to, and has to understand what they are, what they can do now, who they are tied to, and what is waiting for an answer. Second audience: an OP who runs the server and must change settings and fix players without typing commands. Mode: Operate. Success: a player finds any feature of the mod in two clicks or one key, never types a command, and never has to guess whether a thing is hidden because they are not allowed or because they have not found it yet. *(assumed)*
- **Proof.** Every one of the 32 commands and 67 config keys in the audit has exactly one named home (section 3.4). Every person shown anywhere in the hub opens the same Profile page (3.6). A fact about another person is shown only when the server sent it (3.7).
- **Direction.** One hub, three skins (Grimoire, Blood Moon, Tensura), **one structure**. Pages are grouped by what the player is trying to do. The existing theme work (plan 16, the Bloodline v3 mockups) is kept: this document changes where things live, not how a theme looks.
- **Scope and boundaries.** In: hub pages, requests, profiles, settings, admin, navigation. Out: new gameplay rules, new datapack formats, the HUD overlay's look, any visual redesign of a theme. Untouched: the three themes' identity, `FocusOrder` zones, `ConfirmModel`, `Detail`, the server-checks-everything rule.
- **Anti-goals.** A second place to do the same thing. A page that is a dump of everything. A locked, greyed Admin page shown to players. Any client-side hiding of a secret (the server must not send it). A new modal where an inline state works.
- **Constraints.** NeoForge 1.21.1, parts never import each other, text at least 8 GUI px, contrast tests per theme, 480 x 270 GUI must fit, every action keeps sending the typed command or a server-checked payload.

---

## 1. Full feature overview

Grouped by what the player is trying to do, not by code part. "Where today" says how a player reaches it now: **hub** (a page), **chat** (a clickable message), **cmd** (typed), **world** (an action with the normal keys), **item**, **toml** (a config file), **none**.

Status: **built** = in `main`, **partial** = some of it, **planned** = designed, not built.

### 1.1 Become something, and stop being it

| Feature | What it is | Status | Where today |
|---|---|---|---|
| Species | Vampire (Vampirism), Ghoul (the turning, a clock), Original (Rite of Immortality), hybrid and Original hybrid (transition with Original's Blood then Doppelgänger Blood, or the vampire gene), witch and siphoner (Rite of Ancestral Calling, 75% / 25%), heretic (siphoner joins vampires), tribrid, werewolf (born or bitten), Alpha, True Alpha, Hunter of the Five (Rite of the Five), potential hunter | built | world, rites; result shown on **hub** Path |
| Tensura layer | A species is a layer on a human-like Tensura race (skills, magicule bonus, undead treatment); nine apex races; sires become Tensura owners and name the sired | built | hub Path and Bloodline (race, EP); Tensura's own screens |
| Cures | Cleansing Blood, Silver Moon, The Cure (the only one for Originals), Cure for a desiccated Original | built | rites, items |
| The True Cure | An Original can drink it willingly (Rebekah's reward) | planned (plan 17) | none |

### 1.2 Grow

| Feature | What it is | Status | Where today |
|---|---|---|---|
| Supernatural race and ladders | One ladder per race (12 files), rung 0 is the race; a witch or siphoner climbs a second, magic ladder at the same time; a rung once reached is never lost | built | hub Path |
| Tasks and claims | Tasks come from SB, Vampirism, Werewolves, Enchanted, Tensura; a done task shows a wax stamp, pressed to claim; the server checks | built | hub Path, Quests and Rites (three places, see 2.3) |
| Skill points (SP) and EP | SP from rungs, one pool across races; EP with Tensura | built | hub counters |
| Skills | 41 skills, 11 trees, one per race; bought once, dormant when the race changes; Vampirism's and Werewolves' own trees unlocked through the hub with their own points | built | hub Skills (tabs) |
| Skill quests | A skill can need a quest (Predator's Thirst needs Hunter of Hunters) | built | hub Quests |
| Werewolf rank skills, apex evolutions, evolution boost | Tensura skills by rank; nine apex races | built | Tensura's evolution screen; no hub view |
| Loadout | Six HUD slots (R F V / Z X C) filled with Bite and unlocked actions | built | hub Skills tab and Loadout page (duplicate) |
| Track on HUD | One task shown as a line on the HUD | built | hub Path (needs HUD part) |

### 1.3 Magic and crafting

| Feature | What it is | Status | Where today |
|---|---|---|---|
| Rites | About 26 Enchanted circle-magic rites (immortality, species, cures, lineage, revelation, severance, broken line, imprisoned original, curses, wards, artifacts); chalk circle, items, altar power, full moon or eclipse conditions | built | hub Rites lists them; performing is **world** |
| Altar power sources | Blood Stone, Condensed Magicule, siphoning, Emissary discount (x0.9), witch bonus (x0.75) | built | items, world |
| Witch and siphoner powers | Mountain ash lines (Ashwarden), siphon (sneak + right-click), Emissary of a pack | built | world; Emissary via **cmd** |
| New rites planned | Valet Pike (Beast of Gévaudan), forcing rite (optional), Linking Spell (Esther's Grimoire), Cloaking rite (Kol's Grimoire) | planned | none |

### 1.4 People: sire, line, pack, hunters

| Feature | What it is | Status | Where today |
|---|---|---|---|
| Sireline | Who made whom; `bloodline tree`, `bloodline call` (10 min), the sire bond (sired cannot hurt the sire), Tensura naming | built | hub Bloodline; call via hub; tree of others **cmd** (OP) |
| Compulsion | Sneak + right-click a villager or lesser vampire: discount for a day, or follow | built | **world** + a chat choice (5 s) |
| Original lines | Every vampire has a stored line to an Original; killing an Original with white oak kills the whole line; the root is hidden until the Rite of Revelation; Rite of the Broken Line and Rite of the Imprisoned Original free you | built (PR #34), NPC Originals arrive with plan 17 | hub Bloodline (three themes), Rites |
| Severance | Rite of Severance cuts the direct sire bond and clears the Tensura name | built | hub Bloodline link, Rites |
| Packs | Invite, kick, ally, challenge, leave, heal, roar, Emissary, anchor, info; anyone can be a Packmate; second Alphas; successor; absent leader; Open Parties and Claims link; stacked sparks (Deucalion factor); pack bonus (+5% attack per Beta near, Regeneration for Betas) | built | hub Pack (partial), chat, **cmd** |
| Genes | Hidden werewolf gene (15%) and doppelgänger gene (5%), unknown until discovered (Rite of Lineage Reveal, awakening, Rite of Revelation; the doppelgänger gene also by a taglock) | built | hub Bloodline vials (own genes only) |
| Hunters of the Five | The mark grows with prey kills; instinct; hunter's curse; Predator's Thirst | built | hub Path, Skills; world |
| Requests | A sire's call (60 s), a pack invitation (60 s), an ally invitation, a leadership challenge (2 min), a compulsion choice (5 s) | built as chat buttons only | **chat** |

### 1.5 Survive and read the world

| Feature | What it is | Status | Where today |
|---|---|---|---|
| Moon | Full moon forces a shift unless you have moon control (Moon Ring, rank, hybrid types, anchor); lunar eclipse (3%) removes werewolf forms and the pack bonus; Moon almanac | built | hub Path (Grimoire almanac); HUD moon warning |
| Weaknesses | White oak, ash dagger, vervain, mountain ash, wolfsbane (three strains), wolf lichen, silver, werewolf venom, electricity and the Taser, holy damage, hunters | built | **item**, **world**; wiki only |
| Artifacts | Sun Ring, Moon Ring, Resurrection Ring, Vampire Compass (two modes), Hearing Device, Taser, White Oak Ash Dagger, The Cure, poppets (Sun, Silver, Spirit), wards | built | **item** |
| Downed and turning screens | Original hybrid downed screen (Give up or Wait), the Ghoul's turning | built | separate screens, hub Path for a Ghoul |

### 1.6 Moment to moment

| Feature | What it is | Status | Where today |
|---|---|---|---|
| Ability HUD | Six slots, Bite, blood in Tensura's food spot, mode pill, tracker, moon warning | built | HUD, hub Loadout |
| Key binds | Hub key (Home), Alpha Roar (unbound), slots R F V Z X C, HUD mode (Caps Lock) | built | Minecraft Controls |
| World actions | Compel, siphon, drink blood, pull the dagger, Hearing Device | built | **world** (stay there) |

### 1.7 Customise (per player, client only)

Theme, HUD on or off, hide covered keys, hide Tensura's skill HUD, blood in food spot, HUD mode, tracker anchor. 7 keys. Built; only the theme and (by key) the mode have a UI today.

### 1.8 Planned world content (the hub must have a place for it)

| Feature | Source | Hub hooks it needs |
|---|---|---|
| **NPC werewolf packs** on full moons, led by an NPC Alpha; target priority vampire, hunter, other player, prey | bosses design 6, plan 15 | Home "world now" (full moon warning exists), Guide chapter, Admin switch |
| **Weaker NPC spark** that grows while the holder is online | same | Profile of self (spark: weak, grows in N days), Pack page |
| **Recruiting** NPC wolves into your pack; the Alpha Trial hook (`AlphaTrial.win`) | same, plan 14 | Pack page (recruit), **NPC profiles** (3.6), Requests (none: recruiting is in-world) |
| **Howling ideas** (plan 15), reimplemented in our own code, no Howling Werewolf compat | bosses design non-goals | unknown scope: Guide and Pack until plan 15 is written (open question 10) |
| **Eight show bosses**, once per world: Mikael (Mikael's Stake), Klaus (Klaus's Blood), Elijah (the Elixir), Kol (Kol's Grimoire: Cloaking rite), Rebekah (True Cure), Finn (Esther's Grimoire: Linking Spell), Deucalion (all his sparks, Demon Wolf Roar), Peter Hale (his spark, **Memory Claws**) | bosses design 7, plan 17 | Rites (new rites), Notices (boss warnings), Profile knowledge (Memory Claws, 3.7), Admin (`bosses` part switch) |
| **The Beast of Gévaudan**, ninth boss, season 5: a Host NPC by day, the Beast at night; Valet Pike made by a witch's rite with a hunter's or an advanced vampire or werewolf's blood; reward Mantle of the Beast | `2026-11-17-beast-of-gevaudan.md`, owner answers | Rites (Valet Pike), Guide (a "Hunting the Beast" chapter), Notices, Admin (`beast_of_gevaudan_enabled`) |
| **NPC Originals and NPC vampires with lines** | bosses design 2 | Bloodline tree and Profile (NPC nodes show "?" until discovered) |
| Approach C models (Klaus hybrid, Demon Wolf, Beast) | later | none |

### 1.9 For an OP

| Feature | What it is | Status | Where today |
|---|---|---|---|
| Part switches | Nine parts (`enchantedTensura`, `vampirismEnchanted`, `vampirismTensura`, `species`, `speciesTensura`, `artifacts`, `supernatural`, `supernaturalTensura`, `hud`), later `bosses`; need a restart; server and clients must match | built | **toml** |
| Common config | 60 keys in seven toml files (11 core, 5 vampirism_tensura, 2 supernatural, 5 hud, 1 artifacts, 27 species, 9 species_tensura); 59 usable, 1 reserved | built | **toml** |
| Debug commands | `get`, `set`, `event`, ladder `show` `backfill` `reset` `sp` `rung`, eclipse `start` `stop` | built | **cmd** |
| OP-only player commands | `bloodline tree <other>`, `bloodline gene doppelganger` | built | **cmd** |
| Datapack knobs | Ladders, skills, trees, quests, tags, data maps; reload with `/reload` | built | datapack (stays out of the hub; Admin shows "N ladders, M skills loaded") |
| Gamerules | None registered today | built (path), 0 entries | none |
| Config warnings | `ConfigWarning` warns ops on reload | built | chat |

---

## 2. Current hub map

### 2.1 Chrome and entry

- **Open and close:** hub key (default **Home**, category Supernatural Bloodlines). Also a fifth tab in Tensura's own menu. After a launcher's screen closes, `HubReturn` brings the hub back to the page it left.
- **Chrome in every theme:** theme button (a menu of the three looks, saved to `hub-client.toml`), close (x), counters (SP always, EP with Tensura), the emblem (live model with a ring), the Detail area (selection's text and actions).
- **Launchers (seals, rails, list):** **T** Tensura status, **E** Enchanted book, **V** or **W** Vampirism or Werewolves skill screen. Grimoire: wax seals under the book. Blood Moon: right rail. Tensura: "Other mods" list.
- **Keys:** Tab walks nav, stage, panel, actions (`FocusOrder`). Number keys 1 to 8 jump to the 1st to 8th **visible** page (positional). Left and right arrows turn the page, Page Up and Down scroll, Enter or Space presses, Esc closes or cancels a question.
- **Data:** `HubSnapshotPayload` is asked for **on every open** and never kept in sync. It carries the sire, ancestors, sired, line, `Origin`, the pack as names, ranks and online flags, the Five count, your genes, and the rites.

### 2.2 Pages

| Page | Visible when | What it holds | Notes by theme |
|---|---|---|---|
| **Path** | always | Your story (name, races, sire and whether severed, your Tensura race and EP), each ladder rung by rung, the next rung's tasks with claim stamps, Track on HUD, a Ghoul's turning clock, two-ladder bookmarks | Grimoire: left story, right tasks, moon almanac, sigil model. Blood Moon: moon filled by progress, rungs as gems, tasks as cards, bar. Tensura: magic circle, hex ladder, info pane |
| **Skills** | always | Tab 1 our trees (SP): learn or see why locked; tab 2 your Vampirism or Werewolves tree (that mod's points); tab 3 Loadout (HUD part) | Grimoire radial tree plus a left page. Blood Moon tree round the moon. Tensura hexes |
| **Quests** | always | Every active task of the next rungs (both ladders) and open skill quests, filter, detail with claim stamp | |
| **Rites** | with Enchanted | Your rite tasks with claim stamps, then every rite the server loaded (yours first), detail: power, circle, items | |
| **Bloodline** | always | The tree (crowned root or "?", up to two ancestors, sire, you, up to three sired), five states (Original, not a vampire, sire unknown, in a line, free), actions Call, Severance, Revelation, Broken Line (the last three are links to the Rites page), gene vials, Tensura race, EP, magicules, the Five | Grimoire ink tree. Blood Moon chain under the moon. Tensura hexes |
| **Pack** | Werewolves **and** a werewolf-type creature race (`HubPage.visible`) | Ring of portraits by role colour plus a ledger; the chosen member's Kick, Challenge, End alliance; Accept or Decline of a challenge; Leave; an Accept that sends `pack accept` with no token; a one-line hint "type /pack invite" | `BookPackPage` has three styles (BOOK, MOON, TENSURA). The shared `PackPage` is only a fallback and asks before leaving by pressing twice |
| **Guide** | always | Chapters: your creature, your magic, then hub, path, skills, quests, rites, bloodline, pack, loadout, moon (static text) | |
| **Loadout** | HUD part | The same list and six slots as Skills tab 3 (`LoadoutPage` draws `LoadoutTab`) | Duplicate of a Skills tab |
| (empty states) | | "You run alone", "No one turned you yet", "All caught up", each with one action | |
| (Ghoul) | | Only Path and Guide open; the rest are dimmed with a lock | |

### 2.3 Problems (misplaced, duplicated, buried, missing)

Ranked. "Evidence" is where to look.

| # | Problem | Kind | Evidence |
|---|---|---|---|
| 1 | **A Packmate who is not a werewolf type has a pack and no Pack page.** `PACK` is visible only for `werewolves && wolfRace(creature)`. The wiki says "anyone can join a pack" (human, vampire, witch). An Emissary (a witch) is a pack member and cannot open the page that lists the pack. | misplaced / missing | `HubPage.java` 31, `Werewolves-And-Packs` "Mixed packs" |
| 2 | **Requests have no home.** A sire call, a pack invitation, an ally invitation, a challenge and a compulsion choice are chat messages with buttons. The hub is not told when one arrives (snapshot is pulled on open). The compulsion window is 5 s. | missing | audit B4-B6, P2, P6; `HubSnapshotPayload` |
| 3 | **The hub's Accept button is broken.** `pack:accept` sends `pack accept` with no token; the command needs `accept <token>`. | bug | audit P2, `BookPackPage` 267, `PackPage` 62 |
| 4 | **The same task can be claimed in three places:** Path (right page), Quests, Rites (rite tasks with stamps). | duplicated | page javadocs |
| 5 | **Loadout is both a Skills tab and a top-level page,** drawn by the same class. It takes ribbon 8 and a number key. | duplicated | `LoadoutPage`, `SkillsPage` |
| 6 | **Skills mixes three jobs:** spend our SP, spend another mod's points, arrange the HUD. The V and W seals open the same Vampirism or Werewolves tree a second way. | muddled / duplicated | `SkillsPage`, `HubClient` launchers |
| 7 | **A person is a name with no page.** Pack members and tree nodes are names, ranks and an online dot. There is no species, level, last seen, anchor or pack bonus, and no way to act on a person except Kick, Challenge, End alliance. The Bloodline tree and the Pack ledger do not know about each other. | missing | `HubSocial.Member`, `LinePageSupport.detail` (actions only on the YOU node) |
| 8 | **Own attributes are spread over three pages:** species and sire on Path, genes, Tensura race, EP and magicules on Bloodline, rank on Pack. There is no "me" page. | scattered | `PathPage`, `LinePageSupport`, `BookPackPage` |
| 9 | **Bloodline's Severance, Revelation and Broken Line buttons only jump to the Rites page,** without selecting the rite. | buried | `LinePageSupport` 133, 136, 142 (`setPage(RITES)`) |
| 10 | **Pack actions are mostly missing:** invite (a text hint), ally invite and accept, heal, Emissary, anchor (not even shown), Roar (key only). | missing | audit P1, P5, P6, P11-P15 |
| 11 | **No Settings page.** One theme button in the chrome; the other six client settings are toml only. | missing | audit U1-U7 |
| 12 | **No Admin page.** 60 config keys, 12 OP commands and the OP-only form of `tree` are typed or edited by hand. | missing | audit |
| 13 | **Number keys are positional,** so key 6 is Pack for one player and Guide for another. Adding pages shifts them again. | inconsistent | `FocusOrder.pageForKey` |
| 14 | **Ribbon order mixes jobs:** Guide sits between the doing pages and Loadout. Nothing is grouped. | muddled | `HubPage` order |
| 15 | **There is no landing screen.** The hub opens on the last page, so "what changed since I left" (a request, a ready claim, tonight's moon) is on no page. The moon almanac is on Path only in the Grimoire. | missing | `HubClient` |
| 16 | **Leave pack is confirmed two ways:** a dialog in the Grimoire, "press twice" in the shared page. Plan 16 says every irreversible action asks first, with Esc or Stay safe. | inconsistent | `PackPage` 64-74 vs `BookPackPage` 270 |
| 17 | **The Guide explains the hub itself in static chapters** away from the page they describe, and is the only place the wiki's weaknesses, items and rules could go; none of that is in it. | buried | `GuideChapters.GENERAL` |
| 18 | **Launchers have no heading** in the Grimoire and the Blood Moon ("Other mods" exists only in Tensura). | inconsistent | `GrimoireTheme`, `BloodMoonTheme` |
| 19 | **Genes and the root Original are "unknown until discovered" for yourself only.** There is no rule for anyone else, so nothing can be shown about other people without inventing one. | missing rule | `Genes.java` ("Other players' genes are never sent") |
| 20 | **Moon control, anchor and the weak or stacked spark** (what makes your form tonight) are shown nowhere in the hub. | missing | wiki "Moon control", "Anchors" |

---

## 3. Proposed hub map

### 3.1 Principles

1. **One home per thing.** A command, a setting, a request kind and a task each have one page that owns it. Other pages may *link* to it or show a read-only summary; they never carry a second copy of the control.
2. **Pages are jobs.** Me, Magic, People, Help, System. A player names the job, then finds the page.
3. **A person is one object.** Wherever a person appears (pack ring, ledger, family tree, request, home, admin picker, chat) the same Profile opens.
4. **Unknown is a state, not a hole.** A fact the player does not know is drawn "?" with a tooltip that says how to find out. The server never sends it (3.7).
5. **Hidden is not greyed.** A page the player may not use (Admin, Pack for someone with no pack) is absent. A thing that is temporarily unavailable (cooldown, too far) is shown disabled with its reason.
6. **Same structure in all themes.** Section 4.

### 3.2 Pages, ordered by player job

New order and groups. Group labels are drawn by the theme (4.3).

| Group | Page | Key | Visible when | Why it is a page |
|---|---|---|---|---|
| (landing) | **Home** *(new)* | `0` | always | The overview and the inbox: who you are, what is waiting, what is next, what the world is doing. A landing screen is the only way to answer "what changed" and to give every feature a front door |
| **Me** | **Path** | 1 | always | Your ladder and rungs: the story of growing. Loses the task list and the claim stamps (they live in Quests); keeps a "N tasks ready, Claim" shortcut |
| | **Skills** | 2 | always | Spend SP and arrange the HUD. Tabs: Bloodline skills, Faction skills (Vampirism or Werewolves, spent with that mod's points), **Loadout** (the HUD slots). The Loadout page goes away |
| | **Quests** | 3 | always | The one place for tasks and claims, including rite tasks (a filter chip "Rites" replaces the Rites page's stamps) |
| **Magic** | **Rites** | 4 | with Enchanted | The rite book: every rite the server has, what it does, power, circle, items, conditions, "needed for" links. Performing is in the world |
| **People** | **Bloodline** | 5 | always | The family: who made you, whom you made, the Original's line. A node opens a Profile |
| | **Pack** | 6 | **in a pack, or a werewolf-type race with Werewolves** (fixes problem 1) | The pack: ring, ledger, allies, invite, roar, Emissary, anchor. A portrait or row opens a Profile |
| **Help** | **Guide** | 7 | always | What you are, how the mod works, weaknesses, items, keys, the hub. A "?" on a page opens its chapter |
| **System** | **Settings** | 8 | always | Your own client settings (3.4) |
| | **Admin** | 9 | OP (permission level 2 or more) | Server settings, debug actions, part status (3.8) |
| (not in the nav) | **Profile** | , | reached from a person | One page for one person (3.6) |

Result: nine pages for an OP, eight for a player, seven for a Ghoul (Home, Path, Guide, and the locked ones dimmed as today). **Keys 1 to 7 keep today's meaning** (Path, Skills, Quests, Rites, Bloodline, Pack, Guide); key 8 changes from Loadout to Settings, which is the only visible change. Numbers stay positional (a hidden page leaves no gap), as today.

Order inside People is Bloodline then Pack to keep today's keys 5 and 6.

### 3.3 Home

Home answers four questions in one screen. It is not a dashboard of numbers: every block is a door.

| Block | Shows | Door to |
|---|---|---|
| **You** | Name, live model, race (and magic race), pack rank, level, SP, EP, moon control state (full, half, none; anchor), a Ghoul's turning clock | Profile (self) |
| **Waiting for you** | Requests (3.5) and the last notices; each request has its own buttons and a countdown | Requests act in place; names open a Profile |
| **Next step** | The tracked task, or the next rung's nearest task; "N ready to claim" with a Claim all button (sends the same claim packets); skill points you can spend ("1 skill you can learn") | Quests, Skills |
| **The world now** | Moon phase and days to the full moon, an eclipse running or coming, a full-moon night warning (NPC packs later) | Guide chapter "moon" |
| **People** | Your pack as a strip of heads (online dots, crown on the leader), your sire and how many you sired, "no pack" empty state with the next step | Profile, Pack, Bloodline |
| **What you can do now** | Actions that are ready for you: Call your sired (cooldown), Roar (cooldown), Invite (leader), Heal (Alpha); disabled with the reason when not | the owning page's control (same command) |

States: Ghoul (turning clock replaces the rest, as today), no species ("Human": a short "become something" list that points to the Guide), no Tensura (no EP), HUD off (no Track), nothing waiting (the block collapses to one line).

Opening rule: the hub opens on **Home the first time in a session and whenever a request is waiting**, otherwise on the last page (a Settings option decides, 3.4; open question 2).

### 3.4 A home for every command and config key

Legend for "control": **toggle**, **stepper** (typed entry, range shown), **slider** (range 0 to 1 or a short range, with stepper), **choice** (segmented for 4 or fewer, drop-down for more), **text**, **list** (one entry per line, validated by the server's parser), **button**, **picker** (online players, plus a typed name where the command takes an offline name), **seal** (Grimoire's wax seal, the primary action).

#### Player commands

| Id | Command | Page | Control |
|---|---|---|---|
| B1 | `bloodline tree` (own) | Bloodline | the tree itself |
| B2 | `bloodline tree <player>` (OP) | Admin > Debug; also Profile in OP view | picker + "Show family tree" |
| B3 | `bloodline call` | Bloodline (Detail action); Home "What you can do now"; Profile of a sired person | seal "Call your sired" (cooldown text) |
| B4 | `bloodline accept <token>` | Home > Requests (type: sire's call) | Accept; expires 60 s |
| B5, B6 | `bloodline compel <token> discount\|follow` | Home > Requests (type: compulsion) **and** a toast | two buttons Discount, Follow; 5 s countdown |
| B7 | `bloodline gene doppelganger <player> <bool>` (OP) | Admin > Debug | picker + toggle |
| P1 | `pack invite <player>` | Pack > Invite; Profile of a person in no pack (leader) | picker + button "Invite" |
| P2 | `pack accept <token>` | Home > Requests (type: pack invitation) | Accept; replaces the broken tokenless button |
| P3 | `pack leave` | Pack footer; Profile (self) | button + confirm dialog |
| P4 | `pack kick <player>` | Profile (leader viewing a member); Pack ledger selection | danger button + confirm |
| P5 | `pack ally invite <player>` | Pack > Allies; Profile of another pack's leader (leader viewing) | picker + button |
| P6 | `pack ally accept <token>` | Home > Requests (type: alliance) | Accept |
| P7 | `pack ally remove <player>` | Pack > Allies row; Profile of an allied leader | danger button + confirm |
| P8 | `pack challenge <leader>` | Profile of the leader (second Alpha viewing) | seal "Duel" + confirm; rules and disabled reasons inline |
| P9, P10 | `pack challenge accept`, `decline` | Home > Requests (type: challenge); Profile of the challenger | Accept, Decline |
| P11 | `pack heal <player>` | Profile of a member within 4 blocks (any **Alpha**, not only the leader); Pack > Actions | button + confirm (gives up the spark) |
| P12 | `pack info` | Pack page itself (leader, members, Emissary, allies, **anchor**) | , |
| P13 | `pack emissary [player]` | Profile of an eligible member (leader viewing): Name, Clear; Pack > Emissary row | button; disabled "An emissary has to be a witch" |
| P14 | `pack roar` | Pack > Actions; Home "What you can do now"; the key bind | button with cooldown; key bind listed in Settings |
| P15 | `pack anchor [player]` | Profile (any player, werewolf viewing): "Set as my anchor"; Profile (self): change, clear; Pack row | button |

#### OP commands (`supernaturalbloodlines debug ...`)

All in **Admin > Debug**, each sending the typed command (Brigadier's `requires(2)` is the check).

| Id | Command | Control |
|---|---|---|
| D1 | `debug get <player>` | picker + button, reply in the reply box |
| D2 | `debug set <player> <species>` | picker + species drop-down + button |
| D3 | `debug event <player> <event>` | picker + event drop-down (23 values) + button |
| D4 | `debug supernatural <player> show` | picker + button |
| D5 | `... backfill` | picker + button |
| D6 | `... reset` | picker + danger button + confirm |
| D7 | `... sp <n>` | picker + stepper (-1,000,000 to 1,000,000) + button |
| D8 | `... rung <race> <n>` | picker + race drop-down + stepper 0 to 20 + button |
| D9, D10 | `debug eclipse start\|stop` | two buttons; disabled "Needs Werewolves" |

#### Client keys (Settings)

| Id | Key | Control |
|---|---|---|
| U1 | `hub_theme` | choice (Grimoire, Blood Moon, Tensura; Tensura greyed "needs Tensura"). The chrome button stays as a shortcut to the same setting |
| U2 | `hud_enabled` | toggle |
| U3 | `hide_covered_keys` | toggle, with the covered-keys note |
| U4 | `hide_tensura_skill_hud` | toggle (only with Tensura) |
| U5 | `blood_in_food_spot` | toggle (only with Tensura and Vampirism) |
| U6 | `mode` | choice (Auto, Tensura, Supernatural) |
| U7 | `tracker_anchor` | choice (Top centre, Top right, Top left, Above the mode pill) |
| new | `open_on` | choice (Home, Last page): the answer to open question 2 |

Settings also lists the **key binds** read-only (Home, Alpha Roar, slots R F V Z X C, HUD mode) with the current key and one button "Open Controls". The hub cannot rebind keys.

#### COMMON keys (Admin > Settings, by part)

Control and badge. **Restart** = "Needs restart" (C1-C9). Every other key is **Live**. Clients also read T1, T3, T4: the server pushes their values to clients (decided, question 7), so they are **Live** like the rest.

| Id | Key | Admin section | Control |
|---|---|---|---|
| C1-C9 | `parts.<each of the nine parts>` | Core > Parts | toggle each, **Restart**, confirm on change |
| C10 | `werewolves.full_form_level` | Core | stepper 1-100 |
| C11 | `werewolves.moon_bound_werewolves` | Core | toggle |
| V1 | `cure_race` | Vampirism x Tensura | text (a resource id, validated) |
| V2 | `race_level_floor` | Vampirism x Tensura | list (`race=level`) |
| V3 | `werewolf_skill_grants` | Vampirism x Tensura | list (`level=skillId`) |
| V4 | `werewolf_beastfolk_enabled` | Vampirism x Tensura | toggle |
| V5 | `tensura_holy_vs_vampire` | Vampirism x Tensura | slider + stepper 0-100 |
| S1 | `rite_complete_signal` | Supernatural | toggle |
| S2 | `vampire_feed_blood` | Supernatural | stepper 1-20 |
| H1 | `rate_limit_per_second` | HUD | stepper 1-100 |
| H2 | `bite_reach` | HUD | stepper 1-16 (decimal) |
| H3 | `mixins.next_preset_active` | HUD | toggle |
| H4 | `mixins.tensura_feed_fills_blood` | HUD | toggle |
| H5 | `mixins.hide_in_controls_mixin` | , | **not shown** (reserved; on the allow-list as hidden with a reason) |
| A1 | `artifacts.compass_doppelganger_range` | Artifacts | stepper 1-30,000,000 |
| X1 | `max_originals_per_world` | Species | stepper 0-10,000 (0 = none) |
| X2 | `transition_ticks` | Species | stepper 20-24,000,000 (shown as a duration) |
| X3 | `vampire_blood_ticks` | Species | stepper 20-24,000,000 |
| X4 | `bite_vampire_cost` | Species | stepper 0-20 |
| X5 | `max_five` | Species | stepper 1-10,000 |
| X6 | `only_alphas_turn` | Species | toggle |
| X7 | `max_stacked_sparks` | Species | stepper 1-20 |
| X8 | `duel_reconnect_seconds` | Species | stepper 0-600 |
| X9 | `left_pack_grace_minutes` | Species | stepper 0-1440 |
| X10 | `absent_leader_days` | Species | stepper 0-3650 (0 = off) |
| X11 | `opac_link_enabled` | Species | toggle |
| X12 | `witch_bonus_enabled` | Species | toggle |
| X13 | `siphoner_chance` | Species | slider 0-1 |
| X14 | `original_line_death` | Species | toggle |
| X15 | `original_visibility` | Species | choice (discovered, always) |
| X16 | `line_rites_enabled` | Species | toggle |
| X17 | `npc_line_weights` | Species | list (`name=weight`) |
| X18 | `imprisonment_ring_max_radius` | Species | stepper 2-16 |
| X19 | `imprisonment_rite_seconds` | Species | stepper 5-300 |
| X20 | `latent_gene_chance` | Species | slider 0-1 |
| X21 | `alpha_bite_death_chance` | Species | slider 0-1 |
| X22 | `lunar_eclipse_chance` | Species | slider 0-1 |
| X23 | `doppelganger_gene_chance` | Species | slider 0-1 |
| X24 | `doppelganger_villager_chance` | Species | slider 0-1 |
| X25 | `gene_visibility` | Species | choice (discovered, always) |
| X26 | `doppelganger_rite_taglock` | Species | choice (doppelganger, human) |
| X27 | `hybrid_transition_blood` | Species | choice (original_hybrid, original) |
| T1 | `layers` | Species x Tensura | list (`SPECIES=skill,...\|mag\|aura\|undead`), pushed to clients |
| T2 | `rank_skills` | Species x Tensura | list (`RANK=skill,...`) |
| T3 | `undead_treatment_enabled` | Species x Tensura | toggle, pushed to clients |
| T4 | `apex_evolutions_enabled` | Species x Tensura | toggle, pushed to clients |
| T5 | `evolution_progress_multiplier` | Species x Tensura | slider + stepper 1-10 |
| T6 | `sire_subordinate_link_enabled` | Species x Tensura | toggle |
| T7 | `sire_naming_enabled` | Species x Tensura | toggle |
| T8 | `severance_clears_name` | Species x Tensura | toggle |
| T9 | `resire_naming_reward` | Species x Tensura | toggle |

Counts check: 11 + 5 + 2 + 5 + 1 + 27 + 9 = 60. Gamerules: none; the path is built (plan task 7); the section appears only when an entry exists (open question 11).

#### Things that stay in the world on purpose

Compel, siphon, drink blood, pull the dagger, Hearing Device, the downed screen, performing a rite. The hub **explains** each in the Guide and shows the result; it never replaces the in-world action.

### 3.5 Requests and notices

Two kinds of message. **Requests** need an answer and expire. **Notices** are information. Each has one home: **Home > Waiting for you**. Everything else is a pointer to it.

| Kind | Arrives when | Answer | Expires | Also shown as |
|---|---|---|---|---|
| Sire's call | your sire used `call` | Accept | 60 s | nav badge on Home, toast, chat (unchanged) |
| Pack invitation | a leader invited you | Accept (Decline = wait) | 60 s | same |
| Alliance invitation | another leader asked yours | Accept (leader only) | token life | same |
| Leadership challenge | a second Alpha challenged you (you lead) | Accept, Decline | 2 min | same; also on the challenger's Profile |
| Compulsion choice | you compelled someone (you cast) | Discount, Follow | 5 s (30 s while the hub is open) | toast first (too short for a menu); Home |
| **Notices** | line death warning, an Original of your line is desiccated, a duel result, a spark gained, lost or capped, absent-leader result, a cure removed your sparks, an eclipse starts or ends, a login catch-up | none (dismiss) | kept 20 most recent for the session | Home, chat (unchanged) |

Behaviour:
- **Push, not pull.** The server sends the new request to the target when it opens (the plan already does this for the 5 s compulsion window); the hub stays correct while open.
- **Toast.** A small overlay in the corner of the game screen (drawn by the supernatural part, so it works with the HUD off), 8 GUI px text, the sender, the kind, the countdown, and the hub key's name. Pressing the hub key opens Home with the first request focused. It never takes focus from the game.
- **Badge.** A dot with a number on the Home tab (Grimoire), rail icon (Blood Moon), nav row (Tensura).
- **Chat buttons stay** as the fallback (a client without the hub, or the hub closed). A request answered in chat disappears from Home on the next push.
- **Tokens** stay the player's own and are never shown.
- **No decline command exists** for invitations and calls: declining is waiting or dismissing (a local "Dismiss" hides the card). Only the challenge has a real Decline (`pack challenge decline`).

### 3.6 The unified Profile page

One page, one layout, for one person: a player (online or not), an NPC in your line, a recruited NPC wolf (plan 15), or yourself. It is a **route**, not a nav page: the origin page stays lit in the nav and a back control returns to it ("Back to Pack", "Back to Bloodline", "Back to Home"). Esc goes back one step (4.2).

**Opened from:** Pack ring and ledger, Bloodline tree nodes, Home (people strip, requests), a Requests card (the sender), Admin pickers (OP view), and a chat name (a click on a name in a request message runs a client-only `open profile` that opens the hub on it). The same Profile opens in all of them.

**Fields.** The schema is fixed; a field is **known** (a value, or an explicit "none") or **unknown** ("?"). Grouped, in this order:

| Group | Fields |
|---|---|
| Identity | Name, portrait or live model (only when loaded nearby; otherwise a head icon), **species and race** (and magic race), **rank** (Alpha with sparks, True Alpha, Beta, Omega, Packmate, Emissary, Ally, a Tensura subordinate), **level** |
| Bonds | **Pack** (and its leader), days in the pack, **sire**, **line** (the Original's root or "free"), who they sired (count), **anchor** |
| Secrets | **Genes** (werewolf, doppelgänger): known, not carried, or "?" |
| Presence | Online or **last seen**, where (dimension and distance, only when online and near) |
| Contribution | The **pack bonus** this person gives (3.6.1) |

3.6.1 Pack bonus copy, from the rules (`Werewolves-And-Packs`): an Alpha gains +5% attack per Beta within 64 blocks (up to +25%, bitten Betas count twice) with three or more near; those Betas get Regeneration I; the Alpha rank is +20% attack and +4 max health, each spark past the first adds +10% to both; a Packmate adds nothing to the bonus (their perks are receiving it). This text needs a check by the owner (open question 12).

**What "?" does.** Every unknown field shows "?" and a tooltip with the way to find out, so unknown is a teaching moment: genes ("Rite of Revelation, Rite of Lineage (Reveal), Memory Claws, or being in their pack"), line ("Rite of Revelation, for your own line"), anchor and species ("Join their pack, or see them shift").

**Actions depend on your relationship to the person.** One list, drawn in the theme's Detail area, at most one primary. Disabled actions stay visible with their reason; hidden actions are the ones that can never apply to you.

| Action | You are | They are | Command | Confirm | Disabled reasons shown |
|---|---|---|---|---|---|
| Challenge to a leadership duel | a second Alpha of the pack | its leader | `pack challenge <leader>` | yes, with the rules inline: loser left at 1 HP, 10 min cooldown for the pair, 64 blocks, same dimension, answer within 2 min | cooldown "4 min left", too far, other dimension, already in a duel |
| Accept, Decline | the leader | a challenger | `pack challenge accept\|decline` | no (Decline is the safe one) | none |
| Remove from the pack | the leader | a member (not an Emissary or an ally's leader) | `pack kick` | yes | "You can't remove someone you are dueling" |
| Name Emissary, Clear Emissary | the leader | a member who is a witch, siphoner, heretic or tribrid | `pack emissary` | no | "An emissary has to be a witch", "already serves another Alpha" |
| Give spark (heal) | an Alpha (not a True Alpha) | anyone within 4 blocks | `pack heal` | yes (gives up the spark) | too far, "A True Alpha cannot give up the spark" |
| End the alliance | the leader | an allied leader | `pack ally remove` | yes | none |
| Invite to the pack | the leader | someone in no pack | `pack invite` | no | already in a pack |
| Invite to ally | the leader | another pack's leader | `pack ally invite` | no | none |
| Set as my anchor, Clear | a werewolf | any player | `pack anchor` | no | none |
| Leave the pack | yourself | , | `pack leave` | yes | "The leader can't leave" |
| Call (all your sired) | a caller (Original, hybrid, tribrid) | one of your sired | `bloodline call` | no | cooldown, no sired online |
| Severance | the sired | your sire | opens the Rites page on the Rite of Severance | no | "No sire", "Already severed" |
| Open in Admin | an OP | anyone | opens Admin > Debug with the person picked | no | , |

Duel in progress (a state of the profile, not a page): the opponent, the time left, and "paused, waiting for X (45 s)" while a duelist is away (`duel_reconnect_seconds`), with a back button to the Pack page.

**Profiles of yourself** show the same fields with **your own** knowledge rules (genes unknown until discovered, root unknown until the Rite of Revelation), plus: Tensura race, EP, magicules, moon control (and why), spark (stacked, weak and growing), and the actions Leave pack, anchor, and links to Path and Skills. This replaces the scattered attributes of problem 8.

**NPC profiles:** a recruited NPC wolf is a pack member and gets the same page. An NPC in your family line (a vampire creature you made, an NPC ancestor) shows name or type, species and "?" for the rest; an NPC Original's root shows "?" until discovered. Bosses have no profile in v1 (a bestiary chapter in the Guide later).

### 3.7 Knowledge rules (what you know about whom)

**Rule zero.** The server computes what an observer may know about a subject and sends **only those fields**. A field not sent is unknown; the client draws "?" for every schema field that is absent. It never sends "none" for a field the observer does not know (that would leak, for example, that a stranger has no sire). A known field may be an explicit "none".

Fields: `NAME`, `SPECIES`, `RANK`, `LEVEL`, `PACK`, `PACK_DAYS`, `PRESENCE` (online or last seen), `SIRE`, `LINE`, `GENES`, `ANCHOR`, `BONUS`.

Key: **K** always known to this observer. **D** known only once discovered (by a rite or ability below, or pooled by the pack) and then stays known. **?** not known to this relation: not sent, unless the observer has discovered the fact anyway (their own record, their pack's pool, an ability), and then it is shown whatever its letter says. A stranger's own letters are all **?**: the client already has their name and online dot from its own player list.

| Observer's relation to the subject | NAME | SPECIES | RANK, LEVEL | PACK, PACK_DAYS | PRESENCE | SIRE | LINE | GENES | ANCHOR | BONUS |
|---|---|---|---|---|---|---|---|---|---|---|
| **Yourself** | K | K | K | K | K | K (or "unknown", as today) | **D** (Rite of Revelation, or `original_visibility = always`) | **D** (Rite of Lineage Reveal, awakening, Revelation, doppelgänger taglock; or `gene_visibility = always`) | K | K |
| **Same pack** (Alphas, Betas, Packmates, an Emissary, recruited NPCs) | K | K | K | K | K (online, last seen, distance) | K | **D**, pooled by the pack | **D**, pooled by the pack | K | K |
| **Allied pack's leader** | K | K | rank label "Ally" only | K (their pack) | online only | ? | ? | ? | ? | ? |
| **Your direct sire** (a player) | K | K | level K, rank K if you share a pack | D | online only | K (their sire's name, if a player) | same line as yours (no root revealed) | ? | ? | ? |
| **Someone you sired, directly** | K | K | level K | pack if shared | online only | K (you) and whether the bond is severed | same line (no root revealed) | ? | ? | ? |
| **Deeper descendants, player ancestors above your sire** | K | K | ? | ? | ? | ? | ? | ? | ? | ? |
| **NPC in your line** (creature, NPC ancestor) | type K, a person's name only if known, else "?" | K for a creature, "?" for an NPC ancestor | ? | ? | ? | ? | ? | ? | ? | ? |
| **Discovered by an ability** | | | | | | | | | | |
| Rite of Revelation (on yourself) | | | | | | | root Original | your genes | | |
| Rite of Lineage (Reveal) (on yourself) | | | | | | | | werewolf gene | | |
| Rite of Doppelgänger Blood, with their taglock | | | | | | | | their doppelgänger gene | | |
| Memory Claws (Peter Hale's reward, plan 17) | | | | | | their sire | | their genes (respecting `gene_visibility`) | | |
| **A stranger** you can see by name (online list) | ? | ? | ? | ? | ? | ? | ? | ? | ? | ? |
| **A stranger offline** you never met | not listed | | | | | | | | | |
| **An OP in Admin's OP view** | K | K | K | K | K | K | K | K | K | K (banner "OP view", never in the player-facing Profile) |

Rules behind the table:

1. **Pack shares everything the pack knows, at once.** On joining you receive the pack's pool; when anyone in the pack discovers a fact about someone, every current member knows it; leaving the pack keeps what you already know about people but stops live updates (`PRESENCE`, `PACK`, `ANCHOR`, `BONUS`).
2. **Pooling does not invent knowledge.** A gene nobody has discovered stays unknown to the carrier's own pack too. That is the reading recommended in open question 1; the literal reading ("a pack member's genes are simply shown to the whole pack") is option B there.
3. **Discoveries are per observer and permanent,** like the Rite of Revelation today ("Discovery is per player, so your sired don't learn it when you do"). Stored on the observer as `known[subject] = set of facts`, capped (for example 256 subjects), plus a pool in the pack's saved data.
4. **Values are live.** A known fact is re-read when the Profile opens; the client never shows a stored copy. (Option: a "known as of" stamp for facts about people outside your pack; open question 13.)
5. **The client may be asked, never trusted.** The client sends only a subject id (a player UUID, or an opaque per-session token for an NPC). The server derives the relationship from its own pack and line indexes.
6. **Never send an unknown field,** and never send a relationship the observer does not have. A stranger's payload has no fields.
7. **Existing rules are the base case.** `Genes.forClient`, `original_visibility` and `gene_visibility` keep governing *your own* facts. `HubSocial` stays "names, ranks, online flags only"; the new Profile payload is the only carrier of anything more, and it is built from the table above.
8. **Tests that prove it.** A pure `Knowledge.visible(relation, fact)` table test (this table, row by row), a pure payload test "a stranger's payload has no fields", a GameTest "a non-member cannot request a member's profile and learn the species", and mutation checks (flip one cell, expect a red test).

### 3.8 Admin: how it fits without cluttering the hub for players

- **Absent, not locked.** For a player (permission level below 2) the Admin page does not exist: no tab, no rail icon, no nav row, no key, no tooltip. A level change while the hub is open removes the page (`HubPage.orFirst` already does that). The client's `hasPermissions(2)` only decides what to draw; the server checks every payload and command again.
- **One group, one entry.** System > Admin, drawn apart from the other pages (a gap and a different tint in the Grimoire, a divider in the Blood Moon, an "Operator" heading in Tensura) so an OP never confuses it with the player pages.
- **Inside Admin:** a left list of sections and a right pane, the layout the plan already has. Sections: **Settings** (by part, with search, the 59 keys above), **Debug** (player picker, the D and B commands, a reply area), **Parts** (read-only: each part on or off and why, "disabled (requires VAMPIRISM_ENCHANTED)", loaded ladders and skills counts, the mods it found; this is the Troubleshooting page as a screen), **Gamerules** (hidden while empty, see question 11).
- **Badges and states.** "Needs restart" on the nine part switches, "Live" on everything else, "also set on clients" on T1, T3, T4; a field is default, unsaved, saved or invalid; changes apply on Apply or Enter, never per keystroke; a part switch always asks first.
- **Nowhere else.** The only OP-only things outside Admin are the "Open in Admin" action on a Profile and the OP view of a Profile. Home, Pack and Bloodline never show OP controls, so an OP who plays sees the player hub.

---

## 4. Navigation and keyboard model

### 4.1 One structure, three drawings

Every theme has the same five parts. How each draws them:

| Part | Grimoire | Blood Moon | Tensura |
|---|---|---|---|
| **Nav** (page list) | Index tabs on the right edge; a gap between groups; Home is the brass clasp at the top (with the theme switch beside it); Admin sits after a wider gap in a darker tab | Left rail of round icons with a divider between groups; the active icon shows a label chip; Home is the top icon; Admin below a divider | Left list with group headings **ME**, **MAGIC**, **PEOPLE**, **HELP**, **SYSTEM** (and **OPERATOR** for Admin); Home on top; "Other mods" stays under it |
| **Group label** | Tab colour family, plus the label in the tooltip | Divider plus tooltip | Heading text (already the Tensura idiom: PAGES, OTHER MODS) |
| **Badge** (requests waiting) | Red wax dot with a number on the Home tab | Dot with a number on the Home icon | Number chip on the Home row |
| **Stage** | the book's two pages | the moon stage | the centre of the window |
| **Detail** | the opposite page | the floating bar | the info pane |
| **Profile route** | the two pages become the profile; a "Back to X" red link at the top of the left page | the stage becomes the profile; a back pill at the top left | the info pane becomes full width; a "‹ Pack" button in the title bar |

Launchers (T E V W) stay in their current places and get the heading "Other mods" in all three themes (problem 18).

### 4.2 Keyboard (the same in every theme)

| Key | Does |
|---|---|
| Hub key (Home by default) | opens, and closes from anywhere (always wins, as today) |
| `0` | Home |
| `1` to `9` | page by position (positional, hidden pages leave no gap, as today) |
| Left, Right | previous or next page in the nav order (as today; a page that needs them, like a ring, answers first) |
| Tab, Shift+Tab | nav, then stage, then panel, then actions (`FocusOrder`, unchanged); up and down arrows walk the same order |
| Enter, Space | press the focused thing |
| Esc | **one step back**: closes a menu or dialog first, then cancels an edited field (reverts), then goes back from a Profile to its origin, then closes the hub. It is always the safe answer in a dialog |
| Backspace or Alt+Left | back from a Profile (same as Esc on a Profile) |
| `/` | focus the search field (Admin, Guide) |
| `?` | open the Guide chapter for the current page |
| Page Up, Page Down | scroll |
| Enter in a field | commit; Esc reverts |

Confirm dialogs keep the existing rule: the safe button is focused first, Enter alone never confirms, only a left click or Tab then Enter does, Esc cancels.

### 4.3 Shared behaviour

- **Focus order inside a Profile:** back control, identity, fields (each focusable so a "?" tooltip is reachable), actions (primary first).
- **Tooltips and narration:** every control and every "?" has words; the narrator reads "Hayley, leader of your pack, Alpha times 3. Species unknown." style sentences from the same lang keys.
- **Disabled keeps its place** and says why (a tooltip and a visible line), as `Detail.Action.disabledReason` already does.
- **Sizes:** nothing under 8 GUI px, 480 x 270 GUI must fit, text wraps by words, a panel never covers a neighbour (the existing kit tests).
- **Hover and focus on a person:** a portrait or row that opens a Profile shows a hover ring, a focus ring, and a one-line hint "Click a portrait to open a profile" under the legend, so the affordance is discoverable.

---

## 5. Migration

Principle: **no save data changes; existing players keep keys 1 to 7; nothing is removed until its replacement ships.** Every action keeps sending the same typed command or a server-checked payload.

| Item | Change | Risk | Mitigation |
|---|---|---|---|
| Home | new page, key `0`, new landing rule | low: nothing removed | a Settings option "Open on: Last page" restores today's behaviour |
| Settings | new page (the ui-first plan's Settings page, now key 8) | low | the chrome theme button stays |
| Admin | new page, OP only (ui-first plan Task 5, now key 9) | low: hidden for players | server re-check, allow-list, coverage tests (already planned) |
| Requests | new Home block, toast, badge; chat buttons unchanged; hub accept fixed | medium: new payload and a protocol bump (`LadderNet` "1" to "2", as the plan has it) | keep chat buttons; the payload is optional on the client |
| Profile | new route; `HubSocial.Member` is not widened; a new `ProfileRequest` and `ProfilePayload` carry the fields | medium: the privacy rule is the risk | section 3.7 tests; a stranger's payload is empty by construction |
| **Loadout page removed** | `HubPage.LOADOUT` stays in the enum as an alias that opens Skills > Loadout; ribbon 8 becomes Settings | low: players who used key 8 get Settings | Guide chapter "loadout" keeps its text; a link in Settings "Loadout lives in Skills" |
| **Path loses its task list** | Path shows the next rung and "N ready, Claim"; tasks and stamps live in Quests; Rites loses its rite-task stamps | medium: claim muscle memory | **Claim all** on Path and Home sends the same claim packets; Quests gets a chip "Rites" |
| **Genes and Tensura facts move** | from Bloodline to Profile (self); Bloodline keeps a one-line "Genes: 1 of 2 known" link | low | the data and the server rule do not change |
| Rites deep links | Severance, Revelation, Broken Line buttons open the Rites page **on that rite** | low | pure improvement |
| **Pack page visibility** | visible to anyone in a pack, not only wolf races (fixes problem 1) | low | a pure rule in `HubPage.visible`, `HubPageVisibilityTest` |
| Pack actions | invite, ally, heal, Emissary, anchor, Roar added; "press twice" replaced by `ConfirmModel`; the invite hint text removed | low | each sends the same typed command |
| Number keys | `FocusOrder.pageForKey` accepts `0` and `1` to `9` | low | keys 1 to 7 unchanged |
| Rename | none. Page names stay Path, Skills, Quests, Rites, Bloodline, Pack, Guide. New names: Home, Settings, Admin, Profile | , | , |
| Remove | the tokenless Accept button, the shared `PackPage` fallback's two-press leave, the `invite_hint` string, the Loadout ribbon | low | after the replacements ship |
| Docs | `wiki/Supernatural-Hub.md`, `Commands.md` (a "Hub" column), `Configuration.md`, `docs/hub-ui-kit.md` | none | with the build |

**Order of work (each step ships alone):** (1) pure knowledge model and `Knowledge` tests; (2) Requests, pack buttons and the accept fix (ui-first Task 2); (3) Settings (Task 3); (4) Profile route and payload; (5) Home; (6) the page reshuffle (Loadout fold, Path and Quests split, Pack visibility); (7) Admin (Tasks 4 to 8). The ui-first plan's tasks stay valid; the changes to them are: Task 2's `RequestCard` is drawn on Home as well as the Pack page, Task 3's Settings is key 8, Task 5's Admin is key 9 once Loadout folds, and P4, P8, P11, P13 live on the Profile.

---

## 6. Open questions for the owner

**Owner answers (2026-11-17): every recommendation below is approved ("your picks"), with three overrides that were decided earlier and win over the recommendation:**

- **Q7 (override):** the three client-read values (T1 `layers`, T3 `undead_treatment_enabled`, T4 `apex_evolutions_enabled`) are **pushed from the server to clients** (option c), on join and on change. Admin drops the "also set it on clients" note for them; it shows the "Live" badge.
- **Q9 (override):** in **single player with cheats off there is no Admin page**. The integrated-server host gets no exception: Admin shows only when `hasPermissions(2)` is true, the same test the commands use.
- **Q8 (override):** the compulsion choice waits **30 s while the hub is open** (the command's own 5 s token stays when it is closed), plus the corner toast and the optional unbound "answer request" key.

The questions are kept as asked, with the recommendation that was approved.

1. **What does "the pack shares them all at once" mean for secrets?** A. (**recommended**) The pack shares everything about members except what nobody has discovered; discoveries are pooled, so one member's Rite of Revelation on someone teaches the whole pack. B. The pack literally sees every member's genes and line from the moment they join, even undiscovered ones. A keeps discovery meaningful (and the Original's identity safe from a pack-mate who turns on you); B is simpler and matches the sentence more literally.
2. **Which page opens the hub?** Recommend Home on the first open of a session and whenever a request waits, else the last page, with a Settings option.
3. **Quests and Path: split or merge?** Recommend keep both; Quests owns tasks and claims, Path owns the ladder (problem 4). Alternative: merge Quests into Path as a tab (one fewer page).
4. **Loadout:** fold into Skills as a tab (recommended) or keep a top-level page.
5. **Profile of a stranger:** show name and "?" (recommended, it teaches how to find out) or show nothing.
6. **NPC profiles in v1:** recruits and your own line's NPCs only (recommended); bosses and wild NPCs wait for a bestiary.
7. **Client copies T1, T3, T4:** the plan's options (a) note "also set on clients", (b) move to a SERVER config, (c) send the values on join. ~~Recommend (a) now, (c) later.~~ **Decided: (c), the server pushes them to clients on join and on change.**
8. **Requests outside the hub:** a toast in the corner and an optional unbound "answer request" key (recommended); ~~raise the 5 s compulsion window to 10 s when a toast is up~~. **Decided: 30 s while the hub is open.** Or chat only.
9. **Admin without the `supernatural` part, and the single-player host with cheats off:** recommend accept the plan's answer (no hub means no Admin; commands still work). ~~Show Admin to the integrated-server host anyway.~~ **Decided: no exception; cheats off in single player means no Admin page.**
10. **Plan 15 scope ("Howling ideas"):** there is no written plan. Recommend the hub reserves only a Guide chapter and the Pack's recruit control until plan 15 exists.
11. **Gamerules:** hide the section while there are none (recommended, the plan's choice) or show an empty state. The empty state is mocked in Appendix A anyway.
12. **Pack bonus copy for a non-Beta, and "Packmate adds nothing":** this text is mine, derived from the rules. Please confirm or rewrite.
13. **Facts about people outside your pack:** live value (recommended) or "known as of day N".
14. **Heal is an Alpha's, not only the leader's** (`pack heal` is for any Alpha; the brief for the mockups said leader only). Recommend follow the command: any Alpha, not a True Alpha.
15. **OP view of a Profile:** recommend a separate OP view reached only from Admin, with a banner, so an OP who plays never sees secrets by accident.
16. **Chat-name clicks** opening a Profile need a client-only command or event. Recommend yes, small.

---

## Appendix A: mockup plan v2 (paused; follows the map above)

Nothing more is drawn until the owner answers section 6. What exists in the Figma file `USFBf74c4I2BWDyfmRxFZO`, page **UI-first v1**: one frame, `Pack profile / Grimoire - challenge the leader` (`45:2`), a first Grimoire draft of the profile. It is **not polished** and becomes the Grimoire Profile frame of this plan. The helper library for the file is stored in the file's shared plugin data (namespace `sbui`), so later frames reuse it. No PNG export has been made yet.

All frames 1920 x 1080, text at least 16 Figma px, contrast 4.5:1 for text and 3:1 for UI, checked by the helper.

| # | Frame | Themes | Purpose |
|---|---|---|---|
| 1 | **Nav system** | 3 | The groups, the Home tab with a badge, Admin set apart, the launchers under "Other mods" |
| 2 | **Home** | 3 | The five blocks; variants: requests waiting, nothing waiting, no species, a Ghoul |
| 3 | **Profile, knowledge states** | 3 | A pack member (everything known), a stranger (only "?" with how-to tooltips), a sire, a sired, yourself, an NPC recruit |
| 4 | **Profile, actions by relationship** | Grimoire full, 2 strips | Co-Alpha to leader (challenge with rules and disabled reasons), leader to a Beta (remove, Emissary, spark), leader to a stranger (invite), a werewolf to anyone (anchor), your own (leave), OP view |
| 5 | **Confirm dialogs** | 3 | Challenge, remove, heal, leave, part switch; Esc safe, Enter never confirms |
| 6 | **Duel states** | Blood Moon, Tensura | In progress with time left, paused "waiting for X (45 s)", back to Pack |
| 7 | **Requests** | 3 | The Home card for each of the five kinds, the toast, the countdown, expired |
| 8 | **Pack page** | Grimoire full, 2 strips | Portrait hover and focus, the "click a portrait" hint, Allies, Invite picker, anchor row, Roar |
| 9 | **Bloodline page** | Grimoire full, 2 strips | A node that opens a Profile, "?" ancestors, the genes link to Profile |
| 10 | **Settings** | Grimoire full, 2 strips | The 7 keys plus `open_on`, key binds read-only |
| 11 | **Admin** | Tensura full, 2 strips | Settings by part with search, controls (toggle, stepper, slider, choice, list), badges, unsaved, saved and invalid; Core with the restart badges |
| 12 | **Admin, Debug, Parts, Gamerules** | Tensura | Picker and reply area; the Parts status table; the Gamerules empty state |

Process for the rebuild: Operate mode, the existing three looks, no new decoration; one frame per row, each opened as a PNG and checked for clipping before the next. Export to `C:\Users\caiom\AppData\Local\Temp\opencode\ui-first-v1\`.

### Drawn (2026-11-17)

Figma file `USFBf74c4I2BWDyfmRxFZO`, page "UI-first v1", 32 frames at 1920 x 1080. PNGs (1920 x 1080) are in `C:\Users\caiom\AppData\Local\Temp\opencode\ui-first-v1\`, numbered 01 to 32 in the order below. Frame ids: Nav `46:2` `46:335` `46:582`; Home `48:2` `48:445` `47:839`; Profile `49:2` (Grimoire, challenge the leader) `50:2` (Blood Moon, leader views a Packmate, pack-bonus wording for owner review) `51:2` (Tensura, own profile); knowledge states `51:267` `51:565` `51:863`; challenge blocked states `52:285`; duel `53:2` (in progress) `53:196` (paused, waiting 45 s); requests and toast `53:449`; confirm dialogs `55:2`; Pack `56:2` `58:2`; Bloodline `59:2` `59:342`; Skills with Loadout `58:912` `58:1237`; Settings `67:351` `60:354` `66:2`; Admin (Tensura) `63:213` species, `63:586` core and search, `63:996` debug, `64:2` OP view of a profile, `64:334` parts and gamerules empty state, plus Grimoire and Blood Moon variants `65:2`.

Added later the same day (PNGs 33 to 51, plus re-exports of 02, 05, 08, 10 to 12 and 14): Home for a Ghoul `70:2` `70:332` `70:551`; Home with no species `71:2` `71:326`; no pack, Pack and own Profile `71:495` `71:809`; True Alpha Profile `72:2` `72:359`; True Alpha Pack `72:612` `72:1003` `72:1302`; full Blood Moon and Tensura shells for Bloodline `74:2` `74:255`, Skills `74:539` `74:775`, Settings `74:1040` `74:1299`; the "taking a spark ends your path to True Alpha" dialogs `74:1588`.

Owner rulings on those frames: a Ghoul keeps Home, Path, Guide and Settings open (the rest locked); the True Alpha warning shows for every Beta or Omega whose path is still open, before an Alpha Trial or a steal that would give them a spark; heal has no warning (the Alpha gives up the spark, the healed player does not become an Alpha) and is a plain confirm, "Give up your spark to heal X? You become a Beta." The Admin Grimoire and Blood Moon frames stay strips.

## Appendix B: how the planned features slot in

| Planned feature | Slots into |
|---|---|
| NPC packs, recruiting | Pack (recruit), Profile (NPC recruit), Home "world now" (full moon warning), Guide, Admin switch |
| Weak growing spark | Profile (self): "spark: weak, grows in N days"; Notices when it matures |
| Bosses (each) | Rites (their rites), Notices (an Original desiccated, a boss near), Guide (a chapter per encounter), Admin (`bosses` part switch and per-boss enable) |
| Beast of Gévaudan | Rites (Valet Pike), Guide chapter, Admin (`beast_of_gevaudan_enabled`) |
| Memory Claws | the knowledge table (3.7), a new "Discovered by an ability" row |
| True Cure, Elixir, Mantle | items; Guide "Items" chapter; Profile (self) shows the Mantle's state |
| New config keys | automatically a row in Admin by the coverage test; a new part adds a section |
