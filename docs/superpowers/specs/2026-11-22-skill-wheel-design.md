# Skill wheel — design

Owner-approved direction, 2026-11-22. Items marked *(call)* were decided by the designer and can be overruled.

## Intent

Players of the pack carry powers from many mods (ours, Vampirism, Werewolves, Tensura and its addons, Infinity Stones). Today each mod brings its own keys and its own wheel. The pack runs out of keys and the same power can appear twice. We want **one place** that lists every power the player has, grouped by where it comes from, drawn in our three themes, with every other mod's skill key removed.

## The two ways to use a power

- **The 6 HUD slots stay** (R F V Z X C, Caps Lock modes). They are for quick access and do not change.
- **The wheel is the full list.** It holds every power the player has. From it the player can use a power, or put it in a slot.

## Goals

- One wheel holding every usable power from every installed mod we support, in categories (mods): **Bloodline** (ours), **Vampirism**, **Werewolves**, **Tensura** (Tensura plus its addons' skills, **spells and battlewill arts**), **Infinity Stones** (one entry per carried stone; its powers are its modes, see the mode rule below), **Great Sage**. An empty category is hidden (Enchanted has no abilities today, so it never shows).
- **Onion wheel, option A (owner, 2026-11-22):** the wheel is three rings.
  - **Inner ring: the mods** (the categories above).
  - **Middle ring: the groups of the hovered mod**, and it appears **only when that mod has more than one group** (after empty groups are hidden). Tensura has **Skills · Magic (spells) · Battlewill (arts)**; every other mod has one group and skips this ring.
  - **Outer ring: the powers of the hovered group.**
  - **Paging (owner, 2026-11-22):** a group with more than about 12 entries pages on the outer ring: scroll over the ring, or the arrows at the ring's ends (and Left/Right at the first and last cell).
  - **Which scroll wins where (call):** (1) Alt held and the hovered power has modes: Alt + scroll cycles its mode (Alt stays mode-cycling only). (2) No Alt and the hovered power has variable levels (Infinity Stones): scroll changes its level. (3) No Alt, over the outer ring on empty space or a power without levels, and the group has more than one page: scroll turns the page. (4) Anywhere else (inner or middle ring, a one-page group): nothing.
  - Alt keeps one job: **mode cycling**. It never opens or changes rings.
- **Spells and battlewill arts are in scope (owner, 2026-11-22).** Both are Tensura skills (`Magic` and `Battlewill` extend `TensuraSkill`), listed from the player's skills and used through the same path as any Tensura skill. Spells are cast by holding (their cast time), so they are *held* powers unless the spell casts instantly. Tensura's own preset rules apply to them as to every Tensura skill.
- **Passive switches stay on the wheel as on/off switches (owner, 2026-11-23, decided).** Tensura has 56 skills that it does not let a player put in a slot but does let them switch on and off (the three regeneration tiers, danger sense, strengthen body, majesty, dragon skin, water breathing and the like). They are listed as TOGGLE entries and flipped with Tensura's own toggle (the one its skill screen sends), so they never need a slot. The always-on protections (resistances and nullifications, `ResistSkill`) and the skills with no switch at all (eye of truth, unpredictability) are not powers and are not listed.
- **Left click uses** the power. **Right click puts it in a slot**: a row of the 6 slots pops up and the player clicks one; pressing a slot key (R/F/V…) while hovering also works.
- **Three kinds of power:**
  - *instant*: fires, the wheel closes;
  - *toggle / passive switch*: flips, the wheel stays open, the segment glows while on;
  - *aimed / held*: the wheel closes, an aim hint shows ("Click to release · Right-click to cancel"), the power fires on confirm. Held Tensura skills hold while the mouse button is held and release when it is let go. The same aim flow works when an aimed power is used from a slot.
- **Locked powers** (known but not usable now, or not unlocked) show greyed with the reason on hover. Powers the player does not know do not show.
- Cooldown shows as a sweep; toggled powers glow; mastery shows as on the HUD.
- **No duplicates.**
  - Where our power replaces another mod's (our Bite for Vampirism's suck and Werewolves' bite, our sprint-leap for Werewolves' leap, our Alpha Roar slot), only ours shows.
  - Where two mods give the same power (Vampirism and Tensura invisibility…), the wheel and the slots show **one entry** (rule C below).
- **Every other mod's skill key is covered** (unbound while our HUD is active, restored when it isn't, hidden in Controls), using the existing `KeyCover`.
- **Themes:** Grimoire, Blood Moon and Tensura each get their own wheel drawn from the art kit (32 px skill icons, frames, glows). The look is mocked in Figma and approved by the owner before it is built.

## Deduplication — rule C

- A data file lists pairs (or groups) of the *same* power across mods, each with a **main side**. The owner approves the list before it ships (**owner accepted all picks of the research table, 2026-11-22**: `docs/superpowers/research/skill-overlaps.md`, main sides as proposed there). Default main side: the side of the player's main kind (a vampire uses Vampirism's version; a Tensura race skill uses Tensura's).
- The player sees **one entry**. Using it runs the main side.
- **Shared cooldown:** using it puts every side on cooldown. **For a lasting action (Vampirism and Werewolves actions whose cooldown starts when they end), when the active side ends, the main side's cooldown applies to every side (owner, 2026-11-22).**
- **No stacking:** while one side is on, the other cannot be turned on (from any key, ours or theirs).
- Nothing changes in either mod's unlocks, levels, costs or mastery.
- Only clear matches are deduplicated; "loose" matches stay separate (**all 13 loose matches stay separate, owner, 2026-11-22**).
- **Owner answers (2026-11-22):**
  1. **Vampirism's vision key and Tensura's race-ability key move onto the wheel**, and both keys are covered. That enables rows M02 (night vision), M03 (bat form) and M12 (bite and drain).
  2. **Spells and arts are in**: Tensura's Invisibility spell joins M01 as a close match; Vampirism stays the main side for vampires.
  3. **No dedup for a mode inside a bigger skill.** Traveler's and Suppressor's teleport and the haki fear skills stay with their skill and are not group members.
  4. **Tensura tiers** (self, ultraspeed, infinite regeneration): the wheel shows only the highest tier the player has.
  5. **M09 (blood projectile): keep both**, because Ascension's version costs health.
  6. The main sides are as proposed in the research table.
  - **Infinity stone powers stay separate from the dedup groups (owner, 2026-11-22).** A stone is one entry with modes, and its powers exist only because the player holds the stone. So the Infinity members of M01, M02, M05 and M08 are deliberately **not** deduplicated; those groups run between the other sides only.

## Keys

- **Open the wheel: G** *(call: G is Great Sage's menu key in the pack; Great Sage's menu moves into the wheel)*. Hold or press G to open. While open: left click uses, right click slots, releasing G over a hovered power uses it, G again or Esc closes. A **quick tap** (under ~200 ms) repeats the last power used from the wheel.
- **Covered keys (skill keys only):** Vampirism's action wheel and action 1–9 and suck; **Vampirism's vision key (owner, 2026-11-22)**; Werewolves bite and leap; Tensura skill slots 1–3; **Tensura's race-ability key (owner, 2026-11-22)**; Infinity Stones' wheel (H) and its use/favourite keys, and **Infinity's soul control key (`key.infinity.soul_control`, default G; owner, 2026-11-22)**; Great Sage's assess, scan, thought acceleration and magic sense. (Vampirism's default R wheel key currently clashes with our slot 1; covering it fixes that.)
- **Left alone (not skill keys):** Tensura status screen, naming, dodge, mode switch; Vampirism skill screen; Ascension codex; Horizons journal; Enchanted broomstick.

## Infinity Stones compat

- Each stone is a Tensura UNIQUE skill (`StoneSkill`) whose modes are the stone's unlocked powers. The Tensura category would already see them; with the compat they get their own category.
- *(Superseded by the owner's mode rule below: a stone is **one entry**, and its powers are picked with Alt + click. So Infinity Stones has one group and no middle ring.)*
- The compat reads three small public parts of the friend's mod, by reflection, optional, no build dependency: which powers need aiming (`AbilityMeta.needsAim`), the second option of a dual power, and scroll-to-change levels (`VariableAbility`).
- **Soul control (owner, 2026-11-22):** `key.infinity.soul_control` is covered (it also frees G for the wheel). Soul control becomes a wheel entry in the Soul Stone's place in the Infinity Stones ring, shown only while a possession is running (it is "take over / give back", a second step). It sends his own action, not a key press. His release key (`soul_release`) is left alone.
- **Second option:** Shift + left click *(call: right click is already "put in a slot"; his mod already passes the second option as "sneaking")*.
- **Variable levels:** scroll over the hovered power.
- If those parts change name or shape, the compat logs once and switches itself off; stones then show as plain Tensura skills. His mod is not changed.

## Constraints

- **Server trust:** the wheel only asks. Every use goes through the existing server paths (`ActionService` for our and Vampirism/Werewolves actions, Tensura's own skill packets for Tensura skills, his mod's own path for stones). The server re-checks that the player has and may use the power. New or changed payloads bump the network version, with a CHANGELOG note ("update server and clients together").
- **Existing rules hold:** downed, snapped and dying players cannot use anything; targeted powers use the 18d target pipeline; no refusal may reveal what a target is.
- **Held and cast-time powers that are in no Tensura slot (builder finding, 2026-11-22):** Tensura's `getMaxHeldTime` is 20 ticks for a skill that is not in the active preset (72000 when slotted, 7200 when staff-casting). A held art, or a spell with a cast time over one second, used from the wheel with no slot would be cut after one second. The plan keeps the skill counted as "in the preset" for the whole of its hold through the existing next-preset hook (no new mixin), tested first. **If that does not hold**, those powers are listed locked with "put it in a slot first" (the slot picker still works) and never run from a hidden slot; instant skills, instant-cast spells and instant arts are unaffected.
- **Performance:** the entry list is built when the wheel opens and when the player's powers change, never per frame; text is measured once and cached.
- **Accessibility:** keyboard (arrows/Tab move, Enter uses, a slot key assigns), screen-reader narration of category, power and state, 4.5:1 text contrast, state never shown by colour alone, "Reduce motion" turns off wheel animation.
- **UI-first rule:** every new setting is reachable in the hub (Settings for client options, Admin for server options).
- **Parts:** the wheel lives in the `hud` part; each mod's category is gated on that mod (and our part for it) being loaded. With the `hud` part off, nothing is covered and other mods keep their own keys.
- The game is not paused while the wheel is open *(call)*.

## Non-goals

- No change to any other mod's skill trees, unlocks, costs or balance.
- No gamepad support.
- Non-skill keys stay with their mods.
- No new powers.

## Anti-goals (failure even if it works)

- The same power appearing twice, or two versions running at once.
- A wheel that looks like one theme recoloured, or art that looks AI-made.
- A power usable from the wheel that the server would refuse from its own key.
- Losing a player's other-mod keys for good (they must come back when our HUD is off).

## Approaches considered

- **A. Our own wheel screen wired into what we already have** (slot actions, Tensura activation, aim-and-confirm), drawn with the art kit. **Picked:** reuses tested code paths and matches the rest of the mod.
- B. Reskin Vampirism's radial screen and add the other mods to it. Rejected: fights Vampirism's code, and it only knows Vampirism actions.
- C. Copy the Infinity Stones wheel and adapt it. Rejected: a different style and codebase to maintain.

## Order of work

1. **Catalogue and server:** one server-side list of every power the player has (with kind, state, cooldown, lock reason), the use/assign payloads, key covering for the new keys. Tested headless.
2. **Overlap table:** the research table goes to the owner; approved pairs go in the data file with rule C (shared cooldown, no stacking).
3. **Figma mockups** of the wheel in all three themes (closed, open, aiming, locked, slot picker) → owner approval.
4. **Wheel screen** on the art kit, all three themes, keyboard and narration.
5. **Infinity Stones compat** and the aim flow for slots.

## Settled after the builder check

- **Which slots a right click fills (owner, option A):** the slot picker shows the slots of the power's own mode, and the HUD switches to that mode.
  - A Tensura skill goes into the player's Tensura preset, saved through Tensura's own server path, so Tensura's menus see it too.
  - Our powers and Vampirism or Werewolves powers go into our Loadout.

- **Opening G** *(call)*: holding G longer than ~200 ms opens the wheel while held. Releasing over a power uses it. Releasing over nothing leaves it open until a click, G or Esc. A tap under ~200 ms opens nothing and repeats the last power used from the wheel: an instant power fires, and an aimed or held power goes straight into aim mode. The last power is remembered client-side across sessions.
- **Powers with several modes (owner, 2026-11-22):** a power with several ways to run it shows as **one entry**. This covers Tensura skills with modes, each Infinity Stone (its powers are its modes) and anything else with modes.
  - **Changing the mode:** hold the same mode key the HUD already uses (Alt, Tensura's "next mode" key, which we keep). While it is held, a left click on an entry moves it to the next mode instead of using it. Alt + right click moves to the previous mode *(call)*, and Alt + scroll moves either way *(call)*.
  - **What the entry shows:** the current mode's name under the power. While Alt is held, each entry with modes shows a small mode list *(call)*.
  - **One mode setting:** the chosen mode is the skill's own mode, the one Tensura already stores. So the wheel, the slots and Tensura's own screens always agree. A slot holds the power, not a fixed mode, so changing the mode on the wheel also changes what the slot fires.
  - **Locked modes** are skipped when cycling, as Infinity Stones already does.
  - **Plain scroll** (no Alt) over a power with variable levels still changes its level.
  - **Dedup never looks inside a skill's modes (owner, 2026-11-22; replaces "keyed by power plus mode").** A group matches whole powers only. A skill with modes is never hidden, merged or flagged because one of its modes resembles another mod's power.
- **Unslotted Tensura skills** *(call)*: first test against the Tensura jar whether `skillActivationPacket` accepts a skill that is in no preset slot. If it refuses, use the narrowest fix that changes nothing a player sees in Tensura: a small, documented mixin that lets our server path activate a skill the player owns, staying within the part's mixin budget. Never park the skill in a hidden preset slot.
- **Great Sage menu key** *(call)*: covered too, since G is ours now. The build checks what else that menu does. Anything that isn't a power (pages, settings) stays reachable through an "Open Great Sage" button in its category, and through the hub's Other mods links.
- **Main side for hybrids and everyone else** *(call)*: the main side is fixed per pair in the overlap data file that the owner approves. It isn't worked out per player, and there's no per-player switch.

## Known limits (owner, 2026-11-22)

- **Infinity Stones `soul.regen` clears every effect.** Turning that power off runs `effect clear @s`, which also removes Vampirism's and Werewolves' rage and ours. **We do not fix it and the wheel shows no warning for it.** It goes into `wiki/Known-Limits.md` and is written up as a bug report for the friend's mod (the report text is in the W5 plan task). Rule C's "no stacking" does not protect against it.
- Great Sage's menu pages (analyse, archive, battle mode, allocation, settings) are not powers; they stay behind its "Open Great Sage" entry.

## Left to the builder

Exact segment geometry and animation timings (within the approved mockups), the payload shape, the cache invalidation points, the data-file format for overlaps, how the Loadout page reuses the catalogue.
