# Items and blocks

Every item and block this mod adds, with its in-game name, what it does and how to get it. Most things only exist when their [part](Parts-And-Compatibility) is on and its mods are installed. **Textures are placeholders** (generated 16x16 images) for most of them.

"Rite" means a circle-magic rite: see [Rites](Rites) for the exact ingredients and power.

## Species items (part `species`)

| In-game name | What it does | How to get it |
|---|---|---|
| **Original's Blood** | Drink it (32 ticks, stacks to 16): gives the `Original's Blood` effect for 5 minutes. A werewolf who dies with it in them starts the [hybrid transition](Hybrids-And-Original-Hybrids#the-transition). Remembers whose blood it is. You get a glass bottle back. | Rite of Original's Blood (an Original on the circle) |
| **Doppelgänger Blood** | Drink it during a stable hybrid transition to complete it. Only usable then. | Rite of Doppelgänger Blood (needs Werewolves) |
| **Hybrid's Blood** | Drink it only while poisoned with werewolf venom: cures the venom. | Rite of Hybrid's Blood (a hybrid type on the circle; needs Werewolves) |
| **Sire's Blood** | Bound to the one you hit (tooltip "Blood of <name>"). Ingredient of the [Rite of Severance](Rite-Of-Severance); only the blood of the target's own sire works. Can't be drunk. Stack size 1. | Hit a player or creature in melee with a **silver blade** while holding a **Glass Bottle in your off hand** (the bottle is used up) |
| **Silver Dagger** | A small silver blade (iron-tier sword, +1 attack, speed -1.8) for taking Sire's Blood. Any item in `#supernaturalbloodlines:silver_blades` works: also Werewolves' silver sword and Tensura's silver swords and katanas. | Craft: a silver ingot (`#supernaturalbloodlines:silver_ingots`: Werewolves', Tensura's or any `#c:ingots/silver`) over a Stick. Not craftable with no silver mod at all. |
| **Moonstone** | Ingredient of the Rite of the Moon and of Rite of Lineage (Awaken). | Craft: Amethyst Shard + Silver Ingot (Werewolves), shapeless |
| **Taser** | Hit a supernatural to give them **Shocked** for 30 s. 16 charges; every hit spends one; repair with redstone in an anvil. | Craft: Lightning Rod / Redstone / Iron Ingot, top to bottom |
| **White Oak Stake** | The weapon that kills an Original at once. Wooden-sword stats. | Craft: 2 White Oak Planks over a Stick |
| **White Oak Log** | | Grows from a White Oak Sapling |
| **White Oak Planks** | | 1 White Oak Log makes 4 |
| **White Oak Sapling** | Grows a white oak tree (looks like an oak). Nothing drops it. | Craft: Oak Sapling + Amethyst Shard, shapeless |

Status effects of this part: **Shocked** (Slowness I, no natural regeneration, no form actions, 30 s), **Original's Blood**, **Unstable** (1 minute, ends in death), **Werewolf Venom** (10 minutes).

## Rings and wards

Part `artifacts`. Rings sit in a **Curios `ring` slot** if Curios is installed, or **anywhere in your inventory** (hotbar, armor, offhand) if not. Curios is optional. All the rings are bound to **one player** by their rite; a ring bound to someone else, or unbound, does nothing. A ring's tooltip says "Bound to X" or "Not bound to anyone". Rings cannot be rebound.

| In-game name | What it does | How to get it |
|---|---|---|
| **Sun Ring** | While the bound **vampire** wears it, Vampirism's sun damage is cancelled (and the sun kill and nausea). Weakness in the sun is **not** stopped. | Craft: 4 Gold Ingots around a Glowstone block, then bind it with the **Ward of Daylight** |
| **Moon Ring** | Moon control for a **werewolf**: skips the forced full-moon shift. Half forms at will, full forms from Werewolves level 8. Needs Werewolves. | Craft: 4 Silver Ingots around an Amethyst Shard, then bind with the **Ward of the Moon** |
| **Resurrection Ring** | For a **human-side** player (not a vampire or werewolf): a death caused by a **supernatural attacker** is cancelled, **3 charges**: full health and Regeneration II for 10 s. Tooltip shows "Charges: N". An empty ring does nothing. Deaths that Vampirism, the Original rules or a Totem already saved cost no charge. | **Ward of Return** (rite, no crafting) |

> **Release check pending:** with the ring in an **Accessories** slot (the OFA pack), the charge may be taken from a copy, so the ring could keep reviving. See [Known limits](Known-Limits).

## Other artifacts (part `artifacts`)

| In-game name | What it does | How to get it |
|---|---|---|
| **Vampire Compass** | The needle points to the nearest vampire, hybrid or Original within **64 blocks** (not yourself), refreshed once a second. With nobody in range it spins. | Rite of the Vampire Compass |
| **Hearing Device** | Right-click: **stuns** every supernatural within **5 blocks** for 5 s (Slowness V and Nausea). **60 s** cooldown. Works on mobs and on players you could hit with PvP on (not creative or spectators). You, humans and other mobs aren't affected. | Rite of the Hearing Device |
| **White Oak Ash** | Ingredient. | Smelt a White Oak Log |
| **White Oak Ash Dagger** | A hit on an **Original or Original hybrid** leaves it in them as **Desiccated** (30 minutes: no moving, attacking or using items), until another player pulls it out (right-click), it times out or they die. **Anyone supernatural who wields it takes 10 damage per hit.** | Craft: White Oak Ash / Iron Ingot / Stick, top to bottom |
| **The Cure** | Drinking it removes every bloodline layer and the faction. A desiccated Original can't drink it by themselves: someone feeds it to them. | Rite of the Cure |

## Vervain

| In-game name | What it does | How to get it |
|---|---|---|
| **Vervain** (block, a crop with 8 stages, and the **herb** item) | Grows like wheat. The herb is what the tea, the Compass and the Cure use. | Harvest a **fully grown** crop (it also gives seeds; a young crop only gives its seeds back) |
| **Vervain Seeds** | Plants the crop. | **25%** in village house chests (plains, taiga, savanna, snowy, desert) |
| **Vervain Tea** | **10 minutes** of the `Vervain` effect. | Kettle: 2 Vervain + 1 Sugar with a bottle |

**While you have the Vervain effect, or hold vervain in your offhand:** a vampire who **bites** you or hits you **barehanded** takes **4 magic damage** and Weakness (a vervain bite gives no blood). A vampire who **eats** it takes **6**. Vervain also makes you **immune to compulsion**. A werewolf's bite doesn't burn (Werewolves gives no hook).

## Mountain ash

| In-game name | What it does | How to get it |
|---|---|---|
| **Mountain Ash Sapling** | Grows a mountain ash tree. | Drops from Mountain Ash Leaves, like an oak sapling (leaves drop themselves with shears or Silk Touch) |
| **Mountain Ash Log**, **Planks**, **Leaves** | Wood blocks. | The tree generates **rarely in forest biomes** |
| **Mountain Ash Dust** | Placed like redstone dust: lays a **Mountain Ash Line** (gives no power). | Stonecutter: 1 Log makes 4 Dust |
| **Mountain Ash Line** (block) | **A 1.5-block wall for supernaturals only.** Vampires, werewolves, hybrids, Originals, heretics and tribrids **cannot walk across it**. Witches, siphoners and humans pass. | placed from Dust |

How it works:

- They are stopped **by collision only** (no path-finding): mobs may stand pushing against it.
- Arrows and tridents they fire **stick** into the wall; other projectiles are discarded.
- They **cannot break it by hand**. Nothing can be placed or spawn on the line (it's not a floor).
- A line laid by a pack's [Emissary](Werewolves-And-Packs#emissary) also stops the mobs in `#supernaturalbloodlines:supernatural_mobs` (only the Vampirism ghost for now).
- **Explosions and pistons destroy it.** It has no flammability, leaf colour or potted sapling.

## Wolfsbane and wolf lichen

**Need Werewolves.** Nothing here is registered without it. They only affect werewolf types (werewolf faction, hybrids, Original hybrids, tribrids). For anyone else the plant is just food.

| In-game name | How to get it |
|---|---|
| **Yellow Wolfsbane** (block and plant), **Seeds**, **Ash** | seeds: **15%** in village house chests (plains, taiga, snowy) and pillager outposts; crop; ash: smelt the plant |
| **Northern Blue Wolfsbane** (block and plant), **Seeds**, **Ash** | seeds: **4%** in igloos, dungeons and mineshafts; crop; ash: smelt the plant |
| **Wolf Lichen** (a carpet), **Ash** | generates on stone in cold biomes (snowy slopes, grove, frozen and jagged peaks, taiga types, windswept hills and forest); ash: smelt the lichen |
| **Potions** of Yellow Wolfsbane, Northern Blue Wolfsbane and Wolf Lichen | brew a plant into an awkward potion; splash, lingering and tipped arrows follow |

Effects (see [Werewolves and packs](Werewolves-And-Packs#wolfsbane)):

- **Common** (purple): Weakness I and Nausea, 30 s.
- **Yellow:** 1 damage every 2 s for 60 s. Never kills.
- **Northern blue:** 1 damage every 5 s, **death after 5 minutes** (damage type "died of wolfsbane poison").
- **Wolf lichen:** no natural regeneration and no form changes (except back to human), 60 s.

Ways to be hit: **eating** the plant, an **anvil coating** (a sword or axe plus the plant: 8 hits on werewolf types), a **potion**. **Cure:** eat the **ash of the same strain**, or use it while sneaking. Other ashes do nothing.

Other recipes with wolfsbane: Enchanted and Werewolves wolfsbane convert into each other (see [Enchanted integration](Enchanted-Integration#plants-and-blood)).

## Poppets and altar blocks

| In-game name | What it does | How to get it |
|---|---|---|
| **Sun Poppet** | Absorbs a **lethal** hit of Vampirism sun damage. 1 durability. | Spinning wheel (see [Enchanted integration](Enchanted-Integration#poppets)) |
| **Silver Poppet** (needs Werewolves) | Absorbs a **lethal** hit by a silver weapon or silver ammunition, or a werewolf bite. | Spinning wheel |
| **Spirit Poppet** (needs Tensura) | Absorbs a **lethal** hit of Tensura soul damage. | Spinning wheel |
| **Blood Stone** | Altar power **25** (up to 16). | Rite of Blood Offering |
| **Condensed Magicule** | Altar power **30** (up to 16). | Rite of Magicule Offering |

## Where do I find it?

| Part | Items and blocks |
|---|---|
| `enchanted_tensura` | Condensed Magicule, Spirit Poppet |
| `vampirism_enchanted` | Blood Stone, Sun Poppet, Silver Poppet |
| `species` | Original's Blood, Doppelgänger Blood, Hybrid's Blood, Sire's Blood, Silver Dagger, Moonstone, Taser, White Oak items |
| `artifacts` | rings, Vampire Compass, Hearing Device, White Oak Ash and Ash Dagger, The Cure, vervain, mountain ash, wolfsbane strains and wolf lichen |
| `vampirism_tensura`, `species_tensura` | none (data and Tensura races only) |

Items and blocks of a part that is switched off **vanish from existing worlds** when they load. See [For server owners](For-Server-Owners#switching-parts-off).
