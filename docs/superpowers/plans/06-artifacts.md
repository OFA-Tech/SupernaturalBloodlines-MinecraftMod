# Part `artifacts` Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The show-inspired items: Sun Ring, Moon Ring, Resurrection Ring, Vervain, White Oak Ash Dagger, The Cure, Vampire Compass and Hearing Device. Rings go in Curios ring slots when Curios is present.

**Architecture:** `parts.artifacts` registers the items and effects. One helper, `ArtifactSlots`, hides the Curios check:
- `parts.artifacts.curios.CuriosSlots` is loaded only if `curios` is present.
- `InventorySlots` is the fallback.

Enchanted rites (data JSON, plus plan 3's `FactionRiteFactory` / `RiteTargets` where a target is bound) make the enchanted items. Effects read `BloodlineAccess` and the Vampirism API.

**Tech Stack:** As foundation, plus the Vampirism API, Enchanted API, Curios API (`top.theillusivec4.curios:curios-neoforge` compileOnly, `localRuntime` in dev), and Werewolves (optional, Moon Ring).

**Spec:** design section "6. `artifacts`".

## Global Constraints

- Everything in foundation's Global Constraints.
- Curios is optional. Ring items get the tag `curios:ring` (data file `data/curios/tags/item/ring.json`, conditioned on `curios`), plus `CuriosApi.registerCurio` only in `CuriosSlots`.
- Generic names only: Sun Ring, Moon Ring, Resurrection Ring, Vervain, White Oak Ash Dagger, The Cure, Vampire Compass, Hearing Device.
- The white oak wood and tag come from plan 4, Task 2. If plan 4 isn't merged yet, that task's white oak block, item and tag must be moved here in a shared commit first.

## Review Focus

- **A Sun Ring worn by someone other than its bound vampire** must give no protection. Pinned by `sunRingOnlyProtectsBoundVampire`.
- **A Resurrection Ring with 0 charges** must not stop death, and must not crash when the item is removed mid-death. Pinned by `emptyResurrectionRingDoesNothing`.
- **A desiccated Original who logs out** must still be desiccated on login, until the timeout. Pinned by `desiccationSurvivesRelog`.
- **The Hearing Device near other players and non-supernatural mobs** must not affect them. Pinned by `hearingDeviceIgnoresHumans`.
- **Curios absent:** a ring in the main inventory still works, and no Curios class is loaded. Pinned by `ringWorksWithoutCurios` plus the `-PnoCurios` run.

---

### Task 1: Discovery and slot helper

**Files:**
- Create: `docs/parts/artifacts.md` (hook points: Vampirism sun damage type and sun event, the Werewolves full-moon forced transform location, how Vampirism compulsion or "glowing" works if anything exists, the Curios 9.5.1 API package names), `parts/artifacts/ArtifactSlots.java`, `parts/artifacts/curios/CuriosSlots.java`, `parts/artifacts/InventorySlots.java`
- Modify: `build.gradle` (Curios compileOnly + localRuntime, and a `-PnoCurios` flag)

**Interfaces:**
- **`static boolean ArtifactSlots.isWorn(Player p, Item item)`:** if Curios is loaded, `CuriosSlots.isEquipped(p, item)` (`CuriosApi.getCuriosInventory(p).map(h -> h.isEquipped(item)).orElse(false)`) OR it's in the inventory; otherwise it checks the inventory.
- **`static Optional<ItemStack> ArtifactSlots.findWorn(Player p, Item item)`:** the same lookup, returning the stack (needed for data components such as the bound UUID and charges).

- [ ] **Step 1:** Write the GameTests `ringWorksWithoutCurios` (inventory path) and `ringFoundInCuriosSlot` (only when Curios is loaded).
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS). Commit "feat(artifacts): slot helper".

### Task 2: Sun Ring and Moon Ring

**Files:**
- Create: `parts/artifacts/item/SunRingItem.java`, `parts/artifacts/werewolves/MoonRingItem.java`, the data component `supernaturalbloodlines:bound_to` (UUID), the rites `ward_of_daylight.json` and `ward_of_the_moon.json` (the latter conditioned on `werewolves`), using a new factory `supernaturalbloodlines:bind_item` (codec fields: `input: Item`, `output: Item`; it binds the centre player's UUID to the output), and textures and lang.

**Interfaces:**
- **Sun protection:** a `LivingIncomingDamageEvent` listener. If the damage is Vampirism's sun damage type and `findWorn(p, SUN_RING)` has `bound_to == p.getUUID()`, cancel it. If Vampirism has a sun-damage API hook (Task 1), use that instead and record which.
- **Moon Ring (R96):** while worn by its bound werewolf, it gives **moon control** (`MoonControl.of(player)` returns HALF, or FULL from Werewolves level ≥ `full_form_level`, default 8). `MoonControl` lives in `parts.species.pack` (plan 9). If plan 9 isn't merged, create it here in a shared commit.
  - With control, the forced full-moon shift is skipped. If Werewolves has no hook for that, accept a `require = 0` mixin and list it in `docs/mixins.md`.
  - Plan 9, Task 4 enforces the rest: without control, form actions are blocked except during the forced shift.

- [ ] **Step 1:** Write the GameTests:
  - `sunRingBlocksSunForBoundVampire`
  - `sunRingOnlyProtectsBoundVampire`
  - `wardOfDaylightBindsTarget`
  - `moonRingPreventsForcedShift` (werewolves only)
  - `moonRingAllowsHalfFormsAtWill`
  - `moonRingAllowsFullFormsAtLevel8`
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS). Commit "feat(artifacts): sun and moon rings".

### Task 3: Resurrection Ring and Vervain

**Files:**
- Create: `parts/artifacts/item/ResurrectionRingItem.java` (data component `charges`, default 3), the rite `ward_of_return.json`, the vervain crop block, seeds and item, the `vervain_tea` item (kettle recipe JSON `enchanted:kettle`), the effect `vervain` (10 min), loot entries for the seeds (village and Enchanted-style loot), and `parts/artifacts/VervainRules.java`.

**Interfaces:**
- **Resurrection:** a `LivingDeathEvent` listener at HIGH priority. Conditions:
  - the victim is a player with no vampire or werewolf faction and a species of NONE, POTENTIAL or HUNTER_OF_THE_FIVE
  - the attacker is supernatural: `core/Supernatural.java` is a predicate registry (`static void register(Predicate<Entity> isSupernatural, Predicate<Entity> isVampiric, Predicate<Entity> isPrey)`, `static boolean is(Entity)`, `static boolean isVampiric(Entity)`, `static boolean isPrey(Entity)`; prey = vampire and werewolf types, never witches or siphoners, R81), because core can't reference Vampirism. This part registers a Vampirism vampire or werewolf faction player or mob, or a non-NONE species other than the hunter ones. Plan 5 registers Tensura races tagged `tensura:daemon` or `tensura:undead`.
  - the ring is worn with charges > 0
  - Then: cancel, set health to max, apply Regeneration II for 10 s, and use 1 charge.
- **Vervain:**
  - Players with the `vervain` effect, or vervain in the offhand, are immune to compulsion (`static boolean VervainRules.protects(Player)`, used by plan 8).
  - A Vampirism vampire or a species with vampire traits that bites or barehand-hits a vervain player takes 4 damage and Weakness I for 10 s.
  - A vampire eating vervain or tea takes 6 damage.

- [ ] **Step 1:** Write the GameTests:
  - `resurrectionRingRevivesHumanFromVampire`
  - `resurrectionRingIgnoresZombieKill` (not supernatural)
  - `emptyResurrectionRingDoesNothing`
  - `vervainBurnsBitingVampire`
  - `vervainProtectsFromCompulsion` (asserts `VervainRules.protects`)
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS). Commit "feat(artifacts): resurrection ring and vervain".

### Task 4: White Oak Ash Dagger and The Cure

**Files:**
- Create: `parts/artifacts/item/AshDaggerItem.java`, the `white_oak_ash` item (smelting white oak logs), the effect `desiccated` (movement speed −100%, no attack, no item use, grey tint client-side; duration 36000 ticks), `parts/artifacts/item/TheCureItem.java`, and the rite `rite_of_the_cure.json` (power 20000, and the `entities` field needs a player with the `desiccated` effect present: new factory `supernaturalbloodlines:requires_desiccated`, which fails otherwise).

**Interfaces:**
- **Dagger hit on ORIGINAL / ORIGINAL_HYBRID:** apply `desiccated` and store the dagger stack in the victim's attachment `supernaturalbloodlines:lodged_dagger`.
- **Pulling the dagger:** another player right-clicks the desiccated player. The effect is removed and the dagger dropped. The timeout also removes it.
- **Wielder rule:** if the attacker is a vampire, werewolf or species (any non-human per `Supernatural.is`), the attacker takes 10 damage.
- **The Cure:** drinking it runs `BloodlineAccess.applyEvent(p, DRANK_THE_CURE, ctx)`, then `OutcomeExecutor.run` (plan 4) and Vampirism `leaveFaction(false)`.

- [ ] **Step 1:** Write the GameTests:
  - `daggerDesiccatesOriginal`
  - `pullingDaggerFrees`
  - `desiccationSurvivesRelog`
  - `vampireWieldingDaggerIsHurt`
  - `cureRiteNeedsDesiccatedOriginal`
  - `theCureCuresOriginal`
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS). Commit "feat(artifacts): ash dagger and the cure".

### Task 5: Vampire Compass and Hearing Device

**Files:**
- Create: `parts/artifacts/item/VampireCompassItem.java` (it uses the vanilla `LodestoneTracker` component, updated every 20 ticks server-side to the nearest `Supernatural.isVampiric(e)` within 64 blocks, else spinning), `parts/artifacts/item/HearingDeviceItem.java` (use = stun as in the design, 1200-tick cooldown via `ItemCooldowns`), the rites/recipes, textures and lang.

- [ ] **Step 1:** Write the GameTests:
  - `compassPointsToNearestVampire`
  - `hearingDeviceStunsSupernatural`
  - `hearingDeviceIgnoresHumans`
  - `hearingDeviceHasCooldown`
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS).
### Task 6: Mountain ash barrier

**Files:**
- Create: the mountain ash sapling, log, leaves and planks set (worldgen: a rare tree in forest biomes via a NeoForge biome modifier JSON), the `mountain_ash_dust` item (from bark: logs in a stonecutter or mortar recipe), `parts/artifacts/block/MountainAshLineBlock.java` (placed like redstone dust, no power), and `parts/artifacts/block/MountainAshLineBlock#getCollisionShape`

**Interfaces:**
- **Collision (R95):** `getCollisionShape(state, level, pos, CollisionContext ctx)` returns a full-height (1.5 blocks) box when `ctx instanceof EntityCollisionContext e && e.getEntity() != null && Supernatural.isPrey(e.getEntity())` (vampire and werewolf types; **witches pass**), else empty. Projectiles whose owner is prey are discarded on contact.
- **Breaking:** any non-supernatural player can break the line. Supernaturals can't break it by hand (`PlayerEvent.BreakSpeed` → 0).
- **Emissary-strengthened line:** if the placer was a pack Emissary (plan 9), store a flag in a block entity. Hostile mobs tagged `supernaturalbloodlines:supernatural_mobs` are also blocked.

- [ ] **Step 1:** Write the GameTests:
  - `werewolfCannotCrossAshLine`
  - `vampireCannotCrossAshLine`
  - `witchWalksThrough`
  - `humanWalksThrough`
  - `supernaturalCannotBreakLine`
  - `supernaturalProjectileStopped`
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS). Commit "feat(artifacts): mountain ash barrier".

### Task 7: Wolfsbane strains (R95)

**Files:**
- Create: crops, seeds and items for `yellow_wolfsbane`, `northern_blue_wolfsbane` and `wolf_lichen` (lichen is a block that grows on stone in cold biomes); `<strain>_ash` items (smelting the plant); the effects `wolfsbane_yellow`, `wolfsbane_northern_blue` and `wolf_lichen`; `parts/artifacts/werewolves/WolfsbaneStrains.java`; and loot and worldgen entries (yellow is uncommon, northern blue rare, lichen uncommon in cold biomes). The common purple strain is the existing Werewolves and Enchanted wolfsbane; add both to `supernaturalbloodlines:wolfsbane/common`.

**Interfaces:**
- **Strain effects on werewolf types** (from eating, splash, tipped arrows or weapon coating; weapon coating is an anvil recipe that adds a data component `wolfsbane_coating`, 8 hits):
  - common: Weakness I + Nausea, 30 s
  - yellow: 1 damage every 40 ticks for 60 s
  - northern blue: lethal after 6,000 ticks, 1 damage every 100 ticks meanwhile
  - lichen: no natural regeneration and no form actions for 1,200 ticks
- **Cure:** eating or burning (use on yourself) the **ash of the same strain** removes that strain's effect. Other ashes do nothing.

- [ ] **Step 1:** Write the GameTests:
  - `commonWolfsbaneWeakens`
  - `northernBlueKillsWithoutAsh`
  - `sameStrainAshCures`
  - `otherAshDoesNotCure`
  - `lichenBlocksForms`
  - `coatedWeaponAppliesStrain`
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS). Commit "feat(artifacts): wolfsbane strains".

### Task 8: Checks

- [ ] **Step 3:** Check in game with the OFA pack instance, including the Accessories ring slots. Tick `docs/parts/artifacts.md`. Run `-PnoCurios` and `-PnoWerewolves`. Update `CHANGELOG.md`. Commit "feat(artifacts): compass and hearing device".
