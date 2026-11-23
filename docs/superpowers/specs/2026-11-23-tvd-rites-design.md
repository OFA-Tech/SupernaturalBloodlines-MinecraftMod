# TVD rites (plan 21): design

Date 2026-11-23. Status: **decided by the owner on 2026-11-23** (the six questions of the first draft are answered in section 11; Q3 was changed by the owner: only a non-vampiric owner makes a ward). Plan: [plan 21](../plans/2026-11-23-plan-21-tvd-rites.md). Research: [bewitchment-tvd-rites.md](../research/bewitchment-tvd-rites.md).

**Clean-room note.** The three ideas come from Bewitchment, which is All Rights Reserved. Only mechanics are taken (the idea of an invitation rule, a locator from a person's blood, two curses). Names, numbers, art, sounds, lang text and book prose are ours. Nobody implementing this reads Bewitchment's code or Patchouli text. The show's rules (a vampire needs an invitation; a witch finds a person with their blood) are **from memory and not re-checked**; the design says where it follows the show and where it does not.

## 0. What ships

| Part of the plan | What | Where it lives (part) | Rules |
|---|---|---|---|
| 21a | **The invitation ward**: an *area ward* (a warded home) and a smaller *door ward*, with an invite list, spoken and hub invitations, expulsion on revoke, and the dead-owner rule | `artifacts` (next to mountain ash and the compass) | R300 to R314 |
| 21b | **The locator**: the Rite of the Locator (taglock) and its blood variants, read on the Vampire Compass in a third mode | `species` (blood seals, taglock owner), the compass side in `artifacts` through a `shared` bridge | R315 to R319 |
| 21c | **Curse of Insanity** and **Curse of Susceptibility** | `vampirismEnchanted` (needs Enchanted, not Tensura) | R320 to R324 |
| 21z | Shared: config, Admin rows, protocol, hub, book, lang, wiki | all of the above | R325 to R329 |

Rule ids: **R300 to R329**. The groups design reserved R240 to R299, so R300 is the next free block.

## 1. What was read, and what was checked

Read in the repo (main at `f6ac2f95`): `SPECIES-RULES.md` (R10b Revelation, R14, R81 prey, R95 mountain ash, R123, R126, R172 to R177 cloak and link, R204 to R214, R208 force-feed), the rites framework (`RiteKind`, `RiteUnlockTable`, `BindItemRiteFactory`, `RevelationRiteFactory`, `CloakingRiteFactory`, the rite JSON under `data/supernaturalbloodlines/enchanted/circle_magic/rite/`, the book generator `RitesBook` and its tests), `MountainAsh`/`MountainAshRules` (Ashwarden), `VampireCompassItem`, `CompassMode`, `CompassRules`, `CompassPrivacy`, the two Tensura curses (`Curses`, `MagiculeDrainCurse`, `SkillSealCurse`), `HunterCurse` and `HallucinationPayload`, `OpacBackend` (the Open Parties and Claims link), `BloodStamp`, `HubPage`, `ProfileActions`, `AdminCommands`, `HubRequests`, `LadderNet`.

Checked with `javap` against the jars (Gradle cache and the pack's `mods` folder; no Gradle was run):

| What | Result |
|---|---|
| Enchanted 4.2.7 `Curse` (`onInitialise`, `onRemove`, `tick`, `getMaxStrength`, `type`), `CurseManager` (`register`, `applyCurse`, `removeCurse`, `getCurses`) | as the Tensura curses use them |
| Enchanted 4.2.7 `Rite` (`onStart`, `onTick`, `onStop`, `cancel`, `findEntity`, `findTargetUUID`, protected `level`, `pos`), `RiteFactory` (`create`, `id`, `getOutputs`) | `pos` is the altar's position, so a ward needs no block of its own |
| NeoForge 21.1.265: `ServerChatEvent` (`getPlayer`, `getRawText`, cancellable), `LivingIncomingDamageEvent` (`getAmount`, `setAmount`, cancellable), `EntityTeleportEvent`, `PlayerInteractEvent.RightClickBlock` / `EntityInteract` (cancellable) | present |
| Open Parties and Claims 0.33.0: `OpenPACServerAPI.get(MinecraftServer)`, `.getServerClaimsManager()`, `IServerClaimsManagerAPI.get(ResourceLocation, BlockPos)` returning `IPlayerChunkClaimAPI` with `getPlayerId()`, `IPartyManagerAPI.getPartyByMember` / `getPartyByOwner` (already used by `OpacBackend`) | present, all read from the jar |
| Vampirism 1.10.13 and Werewolves 2.0.3.4 | no new hook is needed; every call below is one the repo already makes |

**Not verified** (the plan has a task to check each before relying on it; each is marked `UNVERIFIED` where it is used):

1. U1. Whether `ServerChatEvent` is posted on the server thread in 1.21.1 or on the chat executor. The design hands the work to `server.execute(...)` either way.
2. U2. That a mob never opens a warded door through a path the push-back does not cover (mob door goals do not post a NeoForge event). The design does not rely on blocking the open: it pushes the mob away from the door cell every few ticks.
3. U3. Which of our blood items carry a `BloodStamp` seal today (`original_blood`, `hybrid_blood`, `doppelganger_blood`, `sires_blood`, `klaus_blood`, and whether Vampirism's own bottle is ever stamped). The blood variants of the locator list only the items a test proves are stamped.
4. U4. Whether `vampirismEnchanted` already has a config file. `AdminCoverageTest` lists every config spec: if there is none, 21c creates one and adds it to the test.
5. U5. The `HallucinationPayload` test spy's setter name (the field `spy` exists; the plan reads the class first).
6. U6. Whether `AdminCommands` row ids starting with `W` are free (the table seen has B, P, D, X; the plan greps first).
7. U7. Show canon: that hybrids and Originals need an invitation too, that revoking an invitation expels a vampire who is already inside (the owner asked for it; the show may only stop re-entry), that a locator needs the person's blood, and that a vampire's own home has no barrier (the owner's rule for who may own a ward). The design treats them as owner decisions, not canon.
8. U8. The exact Vampirism event (or `FactionPlayerHandler` hook) that fires when a player joins the vampire faction, and whether it runs on the server thread; the turning and the hybrid transition both end in it. If there is none, the `BloodlineAccess` listener plus the login check (section 3.7) are the whole mechanism and the offline gap is the same.

## 2. Shared rules (R325 to R329)

- **R325, no oracle.** No message, particle, sound, refusal text, cost or timing of any rite in this plan depends on what somebody is. A ward turns away *whatever it blocks* the same way for every blocked entity. Every refusal of a ward rite, a locator rite or a curse rite is **one message per rite** ("The ward will not take.", "The blood will not answer.") and costs the same (the ingredients and the altar's power are spent; a circle that was cancelled for a wrong person on the centre is the one exception, section 2 *Consent*), so a refusal never says which condition failed. **The ward rite refuses a vampiric owner (R301) with that same one message**: it is the same text and the same cost as a full ward list, a claimed chunk or a caster who is not a witch type, so the refusal alone never tells the caster that the person on the centre is vampiric. It is not a perfect screen: a caster who already knows every other condition holds can infer it, so the message never says "vampire" and the ward rite is the only place in this plan where who somebody is changes an outcome (section 3.2 explains why the owner chose that). The locator's needle spins for **every** reason a target cannot be found (offline, dead, another dimension, out of range, cloaked, a spent attunement): one state. The ward never tells its owner who was turned away (`ward_alert_owner`, default off, says "your ward was tested" with no name and no kind).
- **R326, hidden Originals.** Nothing here ever names an Original the viewer has not discovered (R123). A locator on an Original's blood or taglock works as on anybody's and shows **"?"** where a name would be for every secret kind (the label `BloodStamp` already keeps); the compass shows a direction, never a name. A ward's invite list shows the names the *owner* chose, not species. A curse on an undiscovered Original works the same as on anyone.
- **R327, consent.** Where a rite binds a *person to something*, that person agrees by **sneaking on the circle's centre**, as in the other rites (R147: the caster is the subject unless somebody stands there; **a non-sneaking player standing on the centre cancels the rite and the ingredients come back**, so nobody is bound without a deliberate act). This governs the ward's owner (R301). The locator and the two curses act on **a taglock's owner or a bottle's source**, who is not at the circle; their "consent" is whatever the existing taglock rites ask (the owner's own taglock, taken by them), and the locator additionally tells the target (R317). Section 11, question 1, asks whether that is enough.
- **R328, config and Admin rows.** Every setting below is a server config key with an Admin row (`AdminEntries`, one `species(...)`-style line each, plus `AdminEntriesTest` count and `AdminCoverageTest`). 23 rows (13 ward, 5 locator, 5 curses). Nothing is hard-coded that the table in section 9 calls a setting.
- **R329, one hub place per thing, UI-first.** Every action a player can take has a hub button that sends the same typed command a player could type (`/ward ...`), with a row in `AdminCommands` so `CommandCoverageGameTests` sees it. The only chat-only act is **saying "come in"** (R305), and it has a hub button twin. The server validates every argument again; the client draws what the snapshot says and decides nothing.

## 3. The ward (21a, R300 to R314)

### 3.1 What a home is (R300)

A **ward** is a record in a saved index, not a block:

```
Ward { id, kind (AREA | DOOR), dimension, centre (BlockPos), halfSize, owner (UUID), invited (Set<UUID> + names), raisedAt, fallen (bool) }
```

- **AREA** (form a): an axis-aligned cube of half-size `ward_radius` (default 10, hard cap 24) centred on **the altar where the rite was performed** (`Rite.pos`, verified). Nothing is placed, so there is nothing to break, no new block to texture, and the home is exactly where the owner did the rite. Destroying the altar does not end the ward (the owner drops it from the hub, R309).
- **DOOR** (form b): a 3 by 3 by 3 cube centred on the **door cell** (a door, trapdoor or fence gate) found nearest to the altar within 4 blocks. The same record with `halfSize` 1. It is the small, cheap version: an uninvited vampire cannot stand in the doorway or use that door, but a wall or a window is not guarded. A base with an area ward does not need it; a one-room hut can afford it.

Why not the **Open Parties and Claims claim** as the definition: the claim is a set of chunks owned by whoever claimed them, it exists only with that mod, it is not where a house is (a house is part of a chunk), and tying a rule to a claim means a claim change silently changes who is safe. The claim is used for **one** thing, a build check (R301): you cannot ward ground that is claimed by somebody else. Why not a **placed block**: a block can be broken by the people the ward is for, moved, and needs art; the rite's altar is already the place of power, and a ward is a record kept on it.

### 3.2 The owner (R301)

The **owner** is the player who **sneaks on the circle's centre** when the rite starts (R327); if nobody stands there, the **caster**. A non-sneaking player on the centre cancels the rite, the ingredients come back and the caster is told (R147's rule). The caster must be a **witch type** (`SeveranceRiteFactory.isWitchType`, as Severance, Linking and Cloaking).

**Only a non-vampiric owner makes a ward (owner decision, show canon: a vampire's home has no barrier).** "Vampiric" is `Supernatural.isVampiric` on the owner: a vampire, an Original, a hybrid, an Original hybrid, a tribrid or a heretic (a Vampirism vampire faction player or one of those species). A vampiric owner is refused with the one generic message (R325). Consequences, all deliberate:

- A witch-type **caster who is herself a tribrid or a heretic** (both are witch types and both are vampiric) cannot make a ward for herself; she can only make one for a non-vampiric player who sneaks on the centre. A witch, a siphoner or a plain witch-type human can.
- The rule is checked **when the rite starts**, and again for the life of the ward: **a ward falls when its owner becomes vampiric** (owner decision, show canon: turning means dying first, so it is the same as the owner's death, R307). That covers a turning that completes, a hybrid transition, a species change to a vampiric type and joining the Vampirism vampire faction.
- `ward_owner_vampiric_ok` is **not** added: it is the owner's rule, not a setting.

- The caster and the owner are on the invite list from the start.
- A player owns at most `ward_max_per_owner` (3) wards, areas and doors together; the rite that would exceed it is the one generic refusal.
- With Open Parties and Claims and `ward_use_claims` on, the altar's chunk must be **unclaimed, claimed by the owner, or claimed by a member of the owner's party** (`getServerClaimsManager().get(dimension, pos).getPlayerId()`, then `getPartyByMember` for both). Otherwise the generic refusal. Without the mod, or with the link off, the check does not exist.

### 3.3 Who is blocked (R302)

An entity is **blocked** by a ward when all hold: it is **vampiric** (`Supernatural.isVampiric`: vampires, Originals, hybrids, Original hybrids, tribrids, heretics, and Vampirism's vampire mobs), it is **not on the invite list**, it is **not the owner**, the ward is **standing** (R304) and the part is on. Players in creative or spectator are never blocked. **Werewolves are not blocked** (the rule is the vampires'; `ward_blocks_werewolves`, default off, turns it on, using `Supernatural.isPrey`'s werewolf side). Mobs are never on a list, so a blocked mob is always blocked (`ward_affects_mobs`, default on).

A pure function `WardRules.blocks(...)` decides it from booleans; the runtime only gathers them.

### 3.4 What a blocked entity feels (R303)

Every 5 ticks (the Ashwarden cadence), for each loaded ward, the entities of the ward's box that are blocked are **pushed to the nearest face** of the box with the same impulse Ashwarden uses (horizontal 0.6, vertical 0.2, `hurtMarked` set), so the rule is "an uninvited vampire is never inside", whether they walked, teleported, were thrown or logged in there. No damage and no effect by default. A player who is pushed sees one line, **"You cannot enter."**, the same for every cause. The shared push becomes a function in `MountainAshRules` (`pushBack`) that Ashwarden and the ward both call; Ashwarden's behaviour and tests do not change.

A blocked entity that **cannot get out** (a roof over a 1-block-wide stair) is, after `ward_expel_seconds` (default 5) of being inside, teleported to the nearest standable position outside the box on the same level (`WardRules.exitPoint`, a search along the 4 horizontal directions). A teleport is only the fallback.

**Door wards also** cancel `PlayerInteractEvent.RightClickBlock` on their door cell for a blocked player (the door does not open), with the same one line. Mobs are covered by the push (U2).

### 3.5 Invitations (R304 to R306)

- **R304, a ward stands** while: it has not **fallen**, its owner is **alive** (the owner's `isAlive` if online; a dead owner marks every ward of theirs fallen even in unloaded chunks, through the index keyed by owner, R307), and `ward_enabled` is on.
- **R305, spoken invitation.** When the owner says **"come in"** (the whole message, trimmed, case-insensitive, trailing `. , ! ?` ignored; `ward_spoken_invites`, default on), the server invites **one player**: the one the owner is **looking at** within 8 blocks with line of sight, else the **nearest other player within 5 blocks**; the owner must be inside or within 8 blocks of the ward's box. The message is not cancelled (the others in chat read it). The invited player is told "X has invited you into their home." and the owner "You invited Y." Every player is invitable the same way, so the act reveals nothing. A phrase that finds nobody does nothing and says nothing. UNVERIFIED U1: the handler defers to `server.execute`.
- **R306, hub invitation.** The Path page shows the owner a **Your ward** card per ward (R310) with an **Invite** button next to every other player the snapshot already lists as online, a **Withdraw** button next to every invited name, and a **Drop the ward** button (confirm dialog). They send `/ward invite <player>`, `/ward uninvite <player>` and `/ward drop <ward>`. The list holds at most `ward_max_invites` (16) per ward; a full list is the generic "the list is full" reply (it reveals nothing about anyone).

An invite is **immediate** (the show's way: no accept step) and lasts until withdrawn, or until the ward falls. An invited vampire is not blocked by *that* ward only.

### 3.6 Revoking (R308)

`/ward uninvite` (or the hub's **Withdraw**, or saying **"get out"** at the same targeting as R305) removes the name. If `ward_expel_on_revoke` (default **true**, decided by the owner; the show may only stop re-entry, U7; it is a setting with an Admin row, and `false` gives the re-entry-only behaviour) and that player is **inside and now blocked**, the next push (R303) throws them out like any other uninvited vampire, and the timeout fallback applies. They are told "You are no longer welcome here." Nobody else is told.

### 3.7 The dead-owner rule (R307)

When the **owner dies** (`LivingDeathEvent` for the owner's UUID, any cause, any species), **every ward of theirs falls**: `fallen = true`, saved, effective at once even if the ward is in an unloaded chunk (the index is keyed by owner, not read from the chunk). `ward_falls_on_owner_death` (default on, it is the show's rule) can turn this off. A fallen ward is inert and shown as "fallen" on the owner's card until they drop it; it never comes back, a new rite raises a new one (the owner of a lost home is the one who needs the home again). An owner who is merely offline, or whose player data is gone, does **not** end the ward (they may come back).

**The second fall: the owner becomes vampiric (owner decision).** "Becomes vampiric" is `Supernatural.isVampiric` turning from false to true for the owner (a completed turning, a finished hybrid transition, a species change, joining the vampire faction). Every ward of theirs falls, exactly as at death (`fallen = true`, saved, never comes back). **The death listener alone does not cover it:** a turning's lethal blow is cancelled and the player lies down (R20c, R209), so no real death event reaches `LivingDeathEvent` for it; the ward must fall on the *becoming*, not on a death. Two triggers, both feeding `WardIndex.fallAllOf(owner)`: (1) a `BloodlineAccess` change listener on the owner (species or hybrid-curse change, the same hook `PackIndex` uses) and Vampirism's faction-change event (UNVERIFIED U8, Task 0 finds the exact event and its thread); (2) a check at the owner's **login** for an owner who became vampiric while offline or in a way no listener saw (it reads `Supernatural.isVampiric(owner)` against each ward the index holds for them). An owner who is offline when it happens leaves the ward standing until their next login (the index cannot read an offline player's species); that gap is accepted and listed in the risks. Setting: `ward_falls_on_owner_death` covers both falls (one switch: "the ward is tied to its owner's life as a human"); there is no separate key.

### 3.8 The hub (R309, R310)

- **R309, commands:** `/ward invite <player>`, `/ward uninvite <player>`, `/ward drop <ward>` (a ward is its short id, `[0-9a-f]{8}`), `/ward info` (page only). Rows `W1` to `W4` in `AdminCommands` (U6).
- **R310, the Path page:** a card **Your ward** for each ward the viewer owns (kind, rough place: dimension and the distance and bearing from the viewer, **not** coordinates for other people; the owner's own ward shows its coordinates, the owner built it), the invite list with **Withdraw**, and a line **"Homes that welcome you"** listing the names of owners whose ward invited the viewer (names only). Both come from one new S2C payload `WardViewPayload` on `LadderNet`'s optional registrar (the viewer's own wards and the owners who invited them; no one else's data). The protocol becomes the integer after main's at merge time. The client draws; it never decides.

### 3.9 Counters and limits (R311 to R314)

- **R311, mountain ash.** A mountain ash line still blocks every prey type; a ward is the soft, owner-controlled gate. They do not interact.
- **R312, cloak and Original rules.** A cloak (R172) does not help against a ward. An Original is blocked like any vampire; an Original who is invited is not.
- **R313, performance.** The check only runs for loaded wards, only for entities in the ward's box, every 5 ticks; with 3 wards per owner it is a handful of box queries per second per ward.
- **R314, wards in creative/claims.** Operators are not exempt (they can drop a ward by `/ward drop` for themselves only; an Admin row `D14` is **not** added, the Admin page has `ward_enabled`). A server that wants no wards sets `ward_enabled = false`, which stands every ward down at once without deleting them.

## 4. The locator (21b, R315 to R319)

### 4.1 The rite (R315)

**The Rite of the Locator**: small circle, witch-type caster (`isWitchType`), power 2,000 (the witch discount applies as everywhere), the **Vampire Compass** (consumed and given back, bound), and one of:

- a **filled taglock** of a player (as Revelation): rite file `rite_of_the_locator`;
- a **sealed blood bottle** of a player: `rite_of_the_locator_<item>` for each blood item U3 proves is stamped (expected `original_blood`, `hybrid_blood`, `doppelganger_blood`).

The factory (`LocatorRiteFactory`, id `locator`, modelled on `BindItemRiteFactory`, which takes an input item and returns an output item with components) reads the target **player** UUID from the taglock (`TaglockOwner.uuid`) or the bottle (`BloodStamp.read`) and writes two components on the returned compass: `locator_target` (UUID) and `locator_until` (game time). NPC bosses, villagers and mobs are refused with the generic message (R325), at the same cost. A taglock's owner who is **offline** is refused like Revelation (`OWNER_AWAY`), because a compass bound to nobody would only be a way to learn who is online later.

### 4.2 The compass (R316)

`CompassMode` gets a **third mode, `LOCATOR`** (sneak-use cycles `VAMPIRES -> DOPPELGANGERS -> LOCATOR -> VAMPIRES`; `toggled()` becomes a cycle and `CompassModeTest` follows). A compass with no attunement in `LOCATOR` mode behaves as an empty needle. With one, every `PERIOD` ticks `update` asks a `shared.species.LocatorTargets` bridge (the species part installs it; a server without the species part has no third mode, it is skipped in the cycle) for the target's position, and sets the lodestone tracker to a **blurred** point:

- the target must be online, alive, in the **holder's dimension**, within `locator_range` blocks horizontally (default 2000, 0 = the whole dimension), **not cloaked** (`CloakAccess.cloaked`, R172) and not creative/spectator, and the attunement must not be spent (`locator_minutes`, default 30 real minutes = 36,000 ticks). Otherwise the needle spins, **the same for every reason** (R325).
- the point is the centre of a `locator_blur` (default 32) block cell with a deterministic offset seeded by `(target, day)`, so the packet that reaches the client never carries the exact position and the needle cannot be triangulated by stepping.
- distance is shown in the tooltip as **"about N blocks"**, N rounded to the nearest 100 (and "very near" under 50), never as coordinates. Nothing goes to chat.

`CompassPrivacy` already empties the target when the compass is not held, dropped or put in a container; the new component is cleared the same way (`locator_target` is **not** removed by that, because the attunement is the item's purpose, but the *tracker target* is). A compass in a chest keeps its attunement and loses its needle.

### 4.3 The target is told (R317)

When the rite works, the **target** is told once, **"Somebody is looking for you."**, with no name, no place and no species (`locator_tell_target`, default on). If it is off, the target is not told. They are told **again** when the attunement ends only if `locator_tell_target` is on ("The search for you has ended."), so the pair of messages reveals nothing else.

### 4.4 What it never does (R318, R319)

- **R318.** It never names an Original or anyone: the compass has no label for a target; the hub's compass tooltip says "Tracking: ?" for every target (R326). The caster can see in the compass's name only that it is attuned, not to whom, unless the bottle's own label already named a common vampire (R210's label).
- **R319.** A ward does **not** hide its people from a locator: wards keep people out, they do not blind scrying. The counters are the cloak (R172), another dimension, range, time, and `locator_enabled = false`. A ward's owner can still be found.

## 5. The two curses (21c, R320 to R324)

Both are `Curse.Type`s registered with `CurseManager` exactly like `MagiculeDrainCurse` and `SkillSealCurse`, applied by a data-only rite using Enchanted's own `enchanted:apply_curse` factory, **ended by death** (our `LivingDeathEvent` LOWEST listener calls `removeCurse`, as `Curses` does), and lifted by Enchanted's remove-curse rite (`enchanted:remove_curse`) and by cleansing. They live in `parts/vampirismenchanted/curse` and need Vampirism + Enchanted, **not Tensura**. Strength levels 0 to 2 (`getMaxStrength` default).

### 5.1 Curse of Insanity (R320, R321)

- **R320.** While cursed, every `insanity_interval_seconds` (default 30; the interval shortens with strength: `interval / (strength + 1)`) plus or minus a third, the player gets **one hallucination**: a random `HallucinationPayload.Kind` (PHANTOM, WHISPER, DARKNESS). It reuses the existing payload and client, so **no wire change**: `HallucinationPayload` is untouched, and `ModNetwork.PROTOCOL` stays. A player without a channel (a fake player, a client without the mod) gets nothing; the server-side stand-in the Hunter's Curse uses for them is reused.
- **R321.** The timer counts its own calls (like Magicule Drain: Enchanted's `age` is not reliable), is not saved, and restarts on login. Nothing to undo in `onRemove`. A hallucination never changes anything on the server, so it **never** reveals or hides anything about a real creature (R325).

### 5.2 Curse of Susceptibility (R322 to R324)

- **R322.** While cursed, **damage the player takes is increased** by `susceptibility_extra_percent` (default 25) per strength level plus one (strength 0: +25%, 1: +50%, 2: +75%), capped at `susceptibility_max_percent` (default 200, a multiplier of x2). Implemented on `LivingIncomingDamageEvent` (verified: `getAmount`/`setAmount`), at priority LOW and not receiving cancelled events, so a blow that another mod cancelled stays cancelled.
- **R323.** It does **not** touch damage that bypasses invulnerability (`/kill`, the void, `failed_transition`), does not run while `DamageProbe.active()` (our probes deal nothing), and never raises a blow that has already been reduced to 0. It does not stack with itself (one curse instance per type).
- **R324.** The damage is raised for everything the player takes, not "magic only"; the owner asked for "extra damage taken". The multiplier is shown in the curse's tooltip line on the hub's curse list (the existing curse rows) as "takes +N% damage".

### 5.3 Rites

- `curse_insanity`: medium circle on **nether chalk**, power 2,000, items a filled **taglock**, a **moonstone** if one exists in the repo (plan Task 0 greps for it; otherwise `minecraft:fermented_spider_eye`) and `minecraft:soul_sand`; conditioned on `part_enabled vampirismEnchanted`.
- `curse_susceptibility`: the same shape, a taglock, `minecraft:glass_bottle` filled with a `vampirism:vampire_blood_bottle`-type item (the plan picks the exact id from the repo) and `minecraft:soul_sand`, power 2,500.

Both are taught by the **CURSES lore** with the two Tensura curses (`RiteUnlockTable.lore(m, id, CURSES)`), so a Torn Page of curses teaches four, and their entries are generated by `RitesBook` (the unique-label numerals follow: `(I)` to `(IV)`).

## 6. Rites framework changes (shared)

| Piece | Change |
|---|---|
| `RiteKind` | none (the ward and locator are `WITCH`-type rites; the curses use lore) |
| `RiteUnlockTable` | ward rites `RiteKind.WITCH`; locator rites `RiteKind.WITCH`; curses join `CURSES` lore |
| Rite JSON | `rite_of_the_ward`, `rite_of_the_door_ward`, `rite_of_the_locator`, `rite_of_the_locator_<item>` x N, `curse_insanity`, `curse_susceptibility`, each conditioned on its part |
| Advancements | the framework's `rites/known/<rite>` and `rites/sealed/<rite>` pairs are generated for each new rite id (`RiteUnlocksDataTest` already walks every rite file and fails for a rite with no pair) |
| `RitesBook` / lang | new entries, unique labels, the Tensura-only suffix only for the two Tensura curses |
| `FactionRiteHooks` / `RiteTargets` | reused (ward consent and the generic refusal) |

## 7. Hub (UI-first)

| Surface | Content | Server |
|---|---|---|
| Path page, card "Your ward" | owner: each ward, its invite list, Invite / Withdraw / Drop buttons | `/ward ...` rows `W1` to `W4`, validated again |
| Path page, line "Homes that welcome you" | names of owners who invited the viewer | `WardViewPayload` |
| Compass tooltip | "Tracking: ?" in `LOCATOR` mode, distance "about N blocks" | the stack's components |
| Curse list (existing) | two new curses with their one-line effect | the existing curse rows |
| Admin page | 23 rows (section 9) | `AdminEntries` |
| Rites (Enchanted's book) | the new pages | `RitesBook` |

No new hub page. Pages that do not show for a player do not show these (the Path card appears only for a player who owns a ward).

## 8. Protocol and wire

- One new S2C payload `WardViewPayload` (the viewer's own wards, invite names, and "welcomed by" names). `LadderNet.PROTOCOL` becomes **the integer after the one on main at merge time** (11 today, so 12; 13 if 20a lands first). `SnapshotIdsWireTest` pins the new payload's golden bytes and the protocol together, as it does for the hub snapshot.
- `HubSnapshotPayload`, `HallucinationPayload`, `ModNetwork` are **not** edited by this plan.
- `LocatorTargets` is a `shared.species` bridge, not a network type.
- SavedData: `supernaturalbloodlines_wards` (`WardIndex`: wards by id, wards by owner, fallen set, per-ward invites). New file, no migration; an old world has none.

## 9. Config (R328): 23 settings

| File (part) | Key | Default | Meaning |
|---|---|---|---|
| artifacts | `ward_enabled` | true | the ward rites and enforcement; off: every ward stands down (kept) |
| artifacts | `ward_radius` | 10 (1 to 24) | an area ward's half-size |
| artifacts | `ward_max_per_owner` | 3 | areas and doors together |
| artifacts | `ward_max_invites` | 16 | per ward |
| artifacts | `ward_falls_on_owner_death` | true | the show's rule |
| artifacts | `ward_affects_mobs` | true | vampire mobs are blocked too |
| artifacts | `ward_blocks_werewolves` | false | werewolves also need an invitation |
| artifacts | `ward_expel_on_revoke` | true | an uninvited one inside is thrown out |
| artifacts | `ward_expel_seconds` | 5 | the time after which a stuck blocked entity is teleported out |
| artifacts | `ward_spoken_invites` | true | "come in" / "get out" in chat |
| artifacts | `ward_door_enabled` | true | the door form |
| artifacts | `ward_alert_owner` | false | "your ward was tested" with no name |
| artifacts | `ward_use_claims` | true | the Open Parties and Claims build check |
| species | `locator_enabled` | true | the locator rites and the compass mode |
| species | `locator_minutes` | 30 | how long an attunement lasts |
| species | `locator_range` | 2000 | blocks, 0 = the whole dimension |
| species | `locator_blur` | 32 | blocks, the needle's cell |
| species | `locator_tell_target` | true | the target is told once, and when it ends |
| vampirismEnchanted (U4) | `insanity_enabled` | true | the rite and the curse |
| vampirismEnchanted | `insanity_interval_seconds` | 30 | base time between hallucinations |
| vampirismEnchanted | `susceptibility_enabled` | true | the rite and the curse |
| vampirismEnchanted | `susceptibility_extra_percent` | 25 | per strength level plus one |
| vampirismEnchanted | `susceptibility_max_percent` | 200 | the cap, as total damage |

`AdminEntries.ALL.size()` becomes **main's size at the time + 23** (178 + 23 = 201 on the branch at the time of writing; add the rows of any plan that landed first). `AdminSnapshotPayload.MAX_ROWS` must still hold it (the existing test says so): the plan checks it and raises it with the protocol bump if not.

## 10. Order, risks and what is left out

Order: 21a (the ward) first because it is the biggest and carries the payload; 21c second (smallest, independent); 21b last (it touches the compass and the species part). Each is a PR of its own, or one PR with three commits; the plan says commits.

Risks:

1. **The ward and a pack or group.** A pack mate is not on the list; this plan does not link wards to groups (the groups design, 20a to 20d, may offer "invite my group" later). Listed as a follow-up, not built.
2. **A vampire who is pushed out of a ward in a one-wide corridor** loops with the push; the timeout teleport is the answer, and a test covers a boxed-in vampire.
3. **Chat parsing.** "come in" said in another language does nothing. The phrase is a constant pair; localising it needs a lang-keyed table and is left out.
4. **Locator abuse** (a griefer tracking a base): range, blur, the cloak and a spent attunement limit it, the target is told, and `locator_enabled` removes it. No coordinates ever reach chat.
5. **Susceptibility as a kill tool.** Capped at x2 and ended by death; it makes a real fight lethal sooner, as a curse should. PvP gating is the same as the two Tensura curses (none beyond Enchanted's own); adding a PvP check to curse rites is a separate decision for all four.

Left out on purpose: Bewitchment's sigils as items, the Brazier, the curse poppet (low priority in the research), per-ward visual effects and sounds (a later art pass), group-wide invitations, wards that follow a claim, and anything from Bewitchment's art or text.

## 11. Decisions (owner, 2026-11-23)

The first draft asked six questions with a recommended answer each. The owner accepted the recommendations; **Q3 was changed** (the owner's change is marked). They are rules now, not options.

1. **The locator needs no consent.** The target cannot be at the circle, so nothing is asked of them. They are told **"Somebody is looking for you."** (R317, `locator_tell_target`, default on); the cloak, range, the blur and the attunement's time are the counters, and `locator_enabled` switches it off.
2. **A home is the altar spot plus a radius (the area ward), or a single door cell (the door ward). The owner is whoever sneaks on the circle centre** (the caster if nobody stands there). Open Parties and Claims is used **only to check build permission** (R301, `ward_use_claims`), never to define the home.
3. **Ward targets and owners (changed by the owner).**
   - The ward blocks **every vampiric type**: vampire, Original, hybrid, Original hybrid, tribrid, heretic. **Werewolves are not blocked** (`ward_blocks_werewolves`, default off, is the only switch for it).
   - **Only a non-vampiric owner makes a ward** (show canon: a vampire's home has no barrier). A vampiric owner gets the one generic refusal (R301, R325).
   - **The ward falls when its owner dies** (R307, `ward_falls_on_owner_death`, default on), **and when its owner becomes vampiric** (owner decision, 2026-11-23, section 3.7: turning means dying first; the same switch).
4. **Revoking an invitation expels the vampire** who is inside (R308). `ward_expel_on_revoke` is a setting, **default true**.
5. **Both ward forms ship in 21a.** The door ward is the same record at half-size 1 plus a door-interaction cancel (R300, R303).
6. **The ward and the locator are witch-type rites** (`isWitchType`). The two new curses join the existing **curse lore pages** (`CURSES`, with the two Tensura curses), so a Torn Page of curses teaches four.

Accepted by the owner: the refusal of a vampiric owner is the same message and cost as every other ward refusal and can still be inferred by a caster who knows the other conditions hold (R325). Still open: the show's reach for hybrids and Originals and whether revoking expels or only blocks re-entry are unverified canon (U7).