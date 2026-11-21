# Rites live in Enchanted's book, and unlock through species and lore

Status: research and plan only (2026-11-21). Nothing built, no Gradle run. Branch `feature/rites-unlock`, worktree `rites-unlock`. Decisions in section 8 were accepted; building (the plan was written from `origin/main` 3bc1d59, merged up to 685e5da).

Owner decisions (2026-11-21):

1. The hub Rites page goes. All rite content lives in Enchanted's Art of Witchcraft. The hub keeps the rite quests and links to the book.
2. Rites for your own kind unlock when you become that kind. Rarer or dangerous rites unlock only from lore: grimoires, pages in loot, boss drops.
3. The hub's E seal and every rite link need The Art of Witchcraft in the inventory. Without it: "You need The Art of Witchcraft".
4. A locked entry is visible but sealed: title "???" with a wax seal, a one-line hint ("Found in an old grimoire"), no circle, no ingredients.

## 1. How locking works in Modopedia (read from 1.1.10 and the pack's 1.1.11 jars; the 1.1.11 classes are byte-identical to 1.1.10, only `zh_cn.json` differs)

- **One lock exists: an optional `"advancement": "<id>"` on a category or an entry** (`EntryImpl`, `CategoryImpl`). Nothing else: no `locked`, `hidden` or condition field, no extension point for a custom condition type.
- **It only filters the list.** `ButtonListScreen.createEntryButton` / `createCategoryButton` check `AdvancementHelper.hasAdvancement(id)` (the client's `ClientAdvancements`, progress done). If not done: with the book type's `locked_view_type` `hidden` (the default, and what `enchanted:classic` uses: Enchanted's book JSON does not set it) the button is not made at all; with `translucent` it is drawn at 20% and cannot be clicked. **It is not an access control**: `BookOpenHandler.tryOpenEntry` (our own links), the `$(el:...)` links in pages, and the "study" feature (hold the study key over an item that an entry lists in `assigned_items`) open an entry whatever its advancement says. Enchanted's own entries use no advancements at all.
- **The title cannot be changed per player.** `Entry.title` is a literal string, so one entry cannot say "???" while locked and the real name after. `translucent` would show the real title (a leak) and is a book-level setting we do not own. So "???" needs a second entry (the decoy, below).
- **Per player, saved, synced: yes**, because the gate is a vanilla advancement. It is in the player's advancement file, survives death, is sent by `ClientboundUpdateAdvancementsPacket` at login and on change, and works in multiplayer. Verified in the 1.21.1 sources (`PlayerAdvancements.flushDirty`, `AdvancementVisibilityEvaluator`): **a root advancement with no `display` is sent to the client exactly when it is done**, and is removed again (`visible.remove` -> the removed-ids set) when it is revoked. A displayless root has no tab, no toast and no chat line, and the Advancements screen ignores it. So an advancement `{"criteria": {"unlock": {"trigger": "minecraft:impossible"}}}` is a clean per-player flag, set with `PlayerAdvancements.award/revoke`.
- **Refresh.** `ClientAdvancementsMixin` re-inits an open book screen on every update packet, but injects at `HEAD` of `update`, i.e. before the new state is applied: a book that is open while a rite is learned shows the old list until it is reopened or a later packet comes. Acceptable (learning happens while reading a page, in the hub, or in the world); noted as a known limit, not worked around.
- **No negation.** There is no "show while NOT done". A decoy that must vanish on unlock therefore needs its own advancement that we **revoke** on unlock (below).

### The design that gives "???" and a seal

Per gated rite there are two entries and two advancements, in the same category, next to each other:

| | real entry | decoy entry |
|---|---|---|
| id | `supernaturalbloodlines/rites/<rite>` (as today) | `supernaturalbloodlines/sealed/<rite>` |
| `advancement` | `supernaturalbloodlines:rites/known/<rite>` | `supernaturalbloodlines:rites/sealed/<rite>` |
| title / icon | the rite's name / its icon | "???" / a wax item (honeycomb) |
| pages | the circle, text, ingredients, as today | one page: header "???", the hint, a sealed line. No circle, no ingredients |
| visible | when `known` is done | while `sealed` is done |

The server keeps one invariant per player and gated rite: **exactly one of `known`, `sealed` is done** (`known` = learned; `sealed` = not yet). Unlock = award `known`, revoke `sealed`. Rites that are open to everyone (below) have neither advancement and no decoy.

Other locking designs considered and rejected: one dynamic "sealed rites" page (not in the list, and not what the owner described); overriding Enchanted's book JSON for `locked_view_type` (a same-path override is fragile and `translucent` leaks the titles); a mixin into the static `ButtonListScreen.createEntryButton` for a custom condition (breaks on any Modopedia update).

### Leaks we close ourselves

- `assigned_items` (study) opens an entry from an item: **drop `assigned_items` from every lore-locked real entry** (keep it on kind-locked ones: studying a Sun Ring while you are a vampire is natural, and you know the rite by then).
- Links from other pages and from the hub use the real entry only when `known`, else the decoy (`RiteLinks.open`, phase 4).
- The decoy's text carries the hint and nothing of the rite. The book test checks that no decoy page mentions a circle, an item, or the rite's name.

## 2. The unlock map (all 31 rites)

Three buckets. **Open** (8): no gate, visible from the start. **Kind**: unlocks when you become that kind. **Lore**: only from the source listed. A gated rite may have both (kind OR lore).

Kinds (a pure `RiteKinds.of(species, wolf lineage, Vampirism faction)`): `VAMPIRE` (vampire faction, or any vampire-type species), `WEREWOLF` (werewolf faction or wolf lineage, or any hybrid type), `HUNTER` (hunter faction), `ORIGINAL` (Original, Original hybrid), `HYBRID` (hybrid, Original hybrid, tribrid), `WITCH`, `SIPHONER`, `TRIBRID`. They reuse the sides already in `HubRites` (`VAMPIRE_SIDE`, `WEREWOLF_SIDE`, `WITCH_SIDE`).

**Rule that the ladder forces on the map:** every rite a ladder task asks a species to **perform** (`role: performer` in `data/.../supernatural/ladder/*.json`) must be known by that species, or the quest cannot be done. They are:
witch: `rite_of_lineage_reveal`, `ward_of_daylight`, `ward_of_the_moon`, `ward_of_return`, `rite_of_severance`, `rite_of_the_cure`, `ancestral_calling`; siphoner: `rite_of_severance`, `purification`; tribrid: `rite_of_lineage_awaken`, `rite_of_severance`. (Target-role tasks need nobody to know the rite.) This is the old `HubRites.fromLadders` idea moved into the unlock rule, and a data test pins it (section 6).

| rite | bucket | kinds that unlock it | lore source | source exists? |
|---|---|---|---|---|
| ancestral_calling | open | | | |
| blood_offering | open | | | |
| hearing_device | open | | | |
| magicule_offering | open | | | |
| purification | open | | | |
| purification_holy_water | open | | | |
| rite_of_revelation | open | | | |
| vampire_compass | open | | | |
| cleansing_blood | kind | VAMPIRE | | |
| ward_of_daylight | kind | VAMPIRE, WITCH | | |
| rite_of_the_five | kind | HUNTER | | |
| alpha_trial | kind | WEREWOLF | | |
| silver_moon | kind | WEREWOLF | | |
| werewolf_brother | kind | WEREWOLF | | |
| doppelganger_blood | kind | WEREWOLF, HYBRID | | |
| rite_of_lineage_reveal | kind | WEREWOLF, WITCH | | |
| rite_of_lineage_awaken | kind | WEREWOLF, TRIBRID | | |
| ward_of_the_moon | kind | WEREWOLF, WITCH | | |
| hybrid_blood | kind | HYBRID | | |
| original_blood | kind | ORIGINAL | | |
| rite_of_the_moon | kind | ORIGINAL | | |
| rite_of_cloaking | lore | | **Kol's Grimoire**, read | yes: item and Kol's drop exist; its right-click read exists (5 SP), it gets one more effect |
| rite_of_linking | lore | | **Esther's Grimoire**, read | item and Finn's drop exist; it has **no right-click yet**: add the read |
| rite_of_severance | lore + kind | WITCH, SIPHONER, TRIBRID | Esther's Grimoire | as above |
| rite_of_the_broken_line | lore | | **Finn's Notes** (a page, from Finn) | **new**: item and a Finn drop |
| rite_of_the_imprisoned_original | lore | | Finn's Notes | new |
| rite_of_the_cure | lore + kind | WITCH | **Elijah's Letter** (a page, from Elijah) | **new**: item and an Elijah drop |
| immortality | lore | | **Old Rite Page** (loot) | **new**: item and chest loot |
| ward_of_return | lore + kind | WITCH | Old Rite Page | new |
| curse_magicule_drain | lore | | **Cursebinder's Page** (loot, Tensura installed) | **new** |
| curse_skill_seal | lore | | Cursebinder's Page | new |

Totals: 8 open, 13 kind, 10 lore (3 of them also kind). 23 rites carry the two advancements.

Lore items. One new item, **Torn Page** (`supernaturalbloodlines:rite_page`, stacks to 1, data component `rite_lore` = a lore id). Right-click reads it, consumes it, and teaches the lore's rites: "You learn the Rite of ...". The lore ids are `finn`, `elijah`, `old_rites`, `curses`. **Kol's and Esther's Grimoires teach through the same `RiteUnlocks.learnLore` call** (`kol`, `esther`) on the read they already have (Kol's) or get (Esther's), and are not consumed. Loot: a global loot modifier (the mechanism `vervain_seeds_in_villages` already uses) adds `old_rites` to ancient city, woodland mansion, stronghold library and the Hale house chests, and `curses` to the same tables under `mod_loaded tensura`. Boss pages are dropped by the same code that drops the grimoires today (`FinnEntity`, `KolEntity` give their reward stack; Elijah's reward is looked up the same way).

**Performing teaches.** When a rite works (`RiteSignal.completed`, already called for every rite) the performer and the target learn it. This is the fallback for existing worlds (someone who already did a rite is not sealed out of it) and it is why refusing to perform (question 1) is not needed.

## 3. How a rite is unlocked: our own advancements, granted by the server

Chosen over a custom Modopedia condition, because Modopedia has no such hook (section 1).

- `data/supernaturalbloodlines/advancement/rites/known/<rite>.json` and `.../sealed/<rite>.json` for the 23 gated rites: displayless roots with one `minecraft:impossible` criterion. They are **generated** from one source of truth, `data/supernaturalbloodlines/supernatural/rite_unlocks.json` (rite -> kinds, lore, hint key), by the same test-side generator that already makes the book, with the same "stale files fail the build" test.
- `RiteUnlocks` (server, species part): `reconcile(player)` computes the kinds from the state and awards `known` for every rite whose kinds match, then enforces the invariant (revoke `sealed` where `known`, award `sealed` where not). It runs at login, on a bloodline change (`BloodlineAccess` listener), on Vampirism's faction-level event, on a rite completing, on a lore read, and on `AdvancementEvent` (so `/advancement grant everything` or `revoke` cannot break the invariant). Knowledge is never lost (a cured vampire still knows the vampire rites).
- Admin: `/bloodline rites list|learn|forget <player> <rite|lore|all>` (op level 2), for testing and for servers.
- Per player and synced: the advancement file and vanilla's packet, as in section 1. A `/reload` keeps progress by id.

## 4. Performing a locked rite

**Recommendation: do not refuse.** Knowledge of a rite leaks online anyway, a refusal needs a server-side check in every factory (or a `RiteBeginEvent` hook) plus a message, and "performing teaches" gives the world a coherent story: a player who finds the circle from a wiki still earns the entry by doing it. The lock is about the book and the hub, not about the act. See question 1.

## 5. What to delete or adapt in the hub

Delete: the Rites page and everything only it uses.

- `shared/hub/HubPage.RITES` (and its `Group.MAGIC`, which then has no page: the nav group goes), the Magic nav heading, `NavGroups`/layout code that counts it, `HubPages.SHARED.put(HubPage.RITES, ...)`.
- `page/RitesPage`, `page/BookRitesPage`, `page/StageRitesPage`, `page/RiteDiagram`; the rite views in `GrimoireTheme`, `BloodMoonTheme`, `TensuraHubTheme`/`HubThemeViews`.
- `HubRites` (the list, `forYou`, `Cost`, `descKeys`; `trialNotes` moves into the Alpha Trial text), `shared/supernatural/RiteLayout` (keep only `bookEntry`), `HubSocial.Rite`/`Circle`, the rite list in `HubSnapshotPayload` and `SpeciesHub.rites()`. **Wire change: `LadderNet.PROTOCOL` 8 -> 9**, golden hex updated, CHANGELOG line.
- `DevShots`/`HubScreen`/`DevDemo` `rites_*` variants and `devSelect`; `docs/superpowers/design/rites-book` hub shots (the book shots stay).
- Lang: `hub.rites.*`, `hub.page.rites`, `hub.book.rites.*`, the Guide lines that send the player to "the Rites page".
- Tests: `HubRitesTest` (rewritten around `RiteKinds`), `RiteLayoutTest` (trimmed), `HubReturnTest` and `GuideChaptersTest` (RITES references), `SnapshotIdsWireTest` (new golden).

Adapt:

- **E launcher** (`HubClient`): `Launcher` gets a `blocked` supplier. The seal stays, but without the item it shows "You need The Art of Witchcraft" (tooltip and action bar) and does nothing. The check is Modopedia's own `InventoryUtils.hasBook(player, enchanted:art_of_witchcraft)` (main inventory, the same rule as its study feature), so the hub and the book agree.
- **Rite links** that pointed at the page become `RiteLinks.open(riteId)`: no item -> the same message; known -> `EnchantedBook.openRite` on the real entry; sealed -> the decoy entry. Sites: `HomePage` (Alpha Trial notice button), `LinePageSupport` (3 actions), `ProfilePage.OPEN_RITES`, `PackWolves` (recruit), `HubClient.onTrialWarning` (the True Alpha warning dialog is a screen-level dialog: open Home and show it there; the dialog keeps working).
- **Quests**: `RiteSignal`, `HubModel.ritesCountWhen` and the ladder rite tasks stay as they are. The Quests page's rite tasks gain a "Open in the book" link through `RiteLinks`.
- `EnchantedBook.openRite` returns the real or decoy entry id by the client's advancement state (the check Modopedia itself makes, `AdvancementHelper.hasAdvancement`).

## 6. Phases

1. **Rules and data (pure, no game)**: `RiteKinds`, `RiteUnlockRules` (core), `rite_unlocks.json`, the advancement and decoy-entry generator, hints in lang. Tests: below.
2. **Server**: `RiteUnlocks`, its triggers, `/bloodline rites`, the Torn Page item and its read, Kol/Esther read hooks, Finn/Elijah drops, the loot modifiers. GameTests.
3. **Book**: generator emits the decoys and the `advancement` fields, drops `assigned_items` on lore rites, `rite_page` component behaviour in the placeholder. Regenerate.
4. **Hub**: the deletions and adaptations of section 5, protocol 9.
5. **Docs and shots**: wiki Rites page ("how rites are learned"), `docs/parts/*`, CHANGELOG, Guide, hidden shots of the book as (a) a human (sealed list) and (b) a werewolf with a grimoire read, and of the hub Home with the E seal with and without the book.

## 7. Tests (each rule and the generator; mutation-checked)

- `RiteKindsTest`: each species/faction -> kinds, including hybrid types, an Original hybrid, a human who is also a witch.
- `RiteUnlockRulesTest`: which rites a given set of kinds and lore knows; open rites are always known.
- `RiteUnlocksDataTest` (file walk): every rite JSON is open or in `rite_unlocks.json` exactly once; every gated rite has a source (kind or lore); **every rite a ladder task asks a species to perform is known by that species** (reads the ladder JSON); every lore id has a source in the table above; every gated rite has both advancement files and both entries; the generated files equal the committed ones (stale test); no decoy page mentions a circle, an item or the rite's name; no lore-locked real entry has `assigned_items`.
- `RitesBookTest` (extended): real entries carry `advancement`, decoys carry the other one, both in the same category and adjacent.
- GameTests: the invariant (exactly one of known/sealed) after login, after becoming a kind, after a lore read, after `advancement grant everything`, and after revoke; performing teaches; Kol's and Esther's Grimoires teach once and stay; a Torn Page is consumed; a locked rite can still be performed (question 1); the admin command.
- Unit: `RiteLinks` decision (no item / known / sealed), `LadderNet` golden for protocol 9.
- Mutants: swap known/sealed, drop the ladder rule, teach on read without consuming, skip the invariant on login, let a lore rite keep `assigned_items`, skip the book check on the E seal.
- Hidden shots as in phase 5; the open-book-while-learning staleness (section 1) is documented, not tested.

## 8. Open questions: all decided by the owner on 2026-11-21 (my pick was accepted for each; the answer is the first sentence)

1. **Refuse to perform a locked rite?** No (section 4). Performing teaches it.
2. **Does losing a kind re-lock?** No: knowledge stays (a cured vampire still knows the vampire rites).
3. **Are the 8 open rites visible from the start?** Yes. They are the harmless or entry-level ones; the ones that make a player a witch (`ancestral_calling`) and the Blood Stone must be reachable by a human.
4. **Lore sources: is the list above right?** Esther's and Kol's Grimoires teach on read; Finn's Notes (Broken Line, Imprisoned Original: Finn works an eclipse ritual) and Elijah's Letter (the Cure) are new boss pages; `old_rites` (Immortality, Ward of Return) and `curses` are new loot pages. Pick: yes. The alternative for the Cure is Rebekah, but she only comes after a Cure exists.
5. **Tensura curses' source.** Dungeon loot under `mod_loaded tensura` for now. A Tensura mob or structure drop would be better, but needs a look at Tensura's tables; pick: ship the loot version, revisit.
6. **Hint wording for kind-locked entries.** "Known to werewolves", "Known to vampires", "A witch's rite"; for lore: "Found in an old grimoire", "Carried by one who works the eclipse", "Written in a stranger's hand". Pick: this, in the generated lang.
7. **Wax seal icon.** The decoy entry's icon is `minecraft:honeycomb` (wax). Pick: yes; a custom item texture later if wanted.
8. **Admin command and creative.** `/bloodline rites ...` as above, **and a row on the hub Admin page** (owner addition: grant and revoke from the hub too); creative players are not exempt. Pick: yes.
9. **Existing worlds.** Login reconcile gives kind unlocks at once and "performing teaches" covers past rites; nobody is sealed out of what they already do. Pick: yes.
10. **The staleness of an open book** (section 1). Pick: accept.

## 9. Not checked

Nothing here ran: the advancement visibility behaviour is read from the 1.21.1 sources and Modopedia's code, not seen in game. The first phase-2 GameTest and a hidden shot of the book list settle it. Tensura's loot tables and Elijah's reward code were not read.
