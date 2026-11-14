# Witches and siphoners

Needs Vampirism and Enchanted (parts `species`, `vampirism_enchanted`). **Not** Werewolves. Witches and siphoners are on **nature's side**: they are never *prey* for hunters, and they walk through [mountain ash](Items-And-Blocks#mountain-ash) lines.

## How to become one: Rite of Ancestral Calling

A small circle, **4000** power. Items: a **Bone**, an **Amethyst Shard**, **Lapis Lazuli**. See [Rites](Rites#rite-of-ancestral-calling).

The player standing on the circle (else the caster) becomes:

- a **witch** (75% by default), or
- a **siphoner** (25%; the chance is `siphoner_chance`, see [Configuration](Configuration#species-supernaturalbloodlines-speciestoml)).

The rite is refused if the target:

| Refusal message | Why |
|---|---|
| "Your race cannot take a bloodline." | not human-like (Tensura) |
| "A vampire cannot take up magic." | in the vampire faction |
| "You already belong to a faction." | a hunter |
| "You already carry a bloodline." | already has a species |

A player with no faction, or in the **werewolf** faction, can take it. A witch who is also a werewolf can later become a [tribrid](Heretics-And-Tribrids).

## Witch

- **Altar range x1.5.** When a witch stands within the altar's normal range as the altar loads, the altar's range is 1.5 times larger. The range is decided once, when the altar loads: a witch who arrives later does not widen it, one who leaves does not narrow it.
- **Rite cost x0.75** (rounded up). The caster, or the player on the circle's centre, must be a witch type near a linked altar.
- Both bonuses can be switched off with `witch_bonus_enabled`.
- **Joining the vampires removes their magic**: the witch becomes a plain vampire.
- Tensura (needs the Tensura part): no skills, **+30% max magicule and +10% max aura**. Apex race: **Ancestral Witch**.

## Siphoner

A siphoner drains power from the world and keeps it.

- **How:** **sneak + empty main hand + right-click** on:
  - a **block with altar power** (Enchanted's own entries, or this mod's, such as garlic, magic ore, Blood Stone), or
  - a **living creature**.
- **Gain:** **+50 siphoned power** per use, up to a **cap of 1000**. At the cap: "You cannot hold any more siphoned power."
- **Blocks:** each block gives power once per **30 seconds per player**. Too soon: "This block has nothing left to give yet."
- **Creatures:** a creature takes **2 magic damage** and gives power only if the hit lands (so its damage immunity frames limit the rate). Once per 30 seconds per creature. The damage follows PvP rules and claims: a refused hit gives no power.
- **Spend it on rites:** the **start cost** of a rite is paid from siphoned power first, and the altar pays the rest. **If the siphoned power covers the whole cost, no altar is needed.** Per-tick costs never use siphoned power. If the rite refuses its target (wrong faction and so on), the siphoned power is not taken.
- **It resets on death** (R65). Species stays.
- A siphoner **cannot start the hybrid transition**.
- A siphoner who **joins the vampires** becomes a **heretic**. See [Heretics and Tribrids](Heretics-And-Tribrids).
- Tensura: skill `drain`, +10% max magicule. Apex race: **Void Siphon** (not undead).

Witch bonuses (range and cost) apply to witch, heretic and tribrid. Siphoning applies to siphoner, heretic and tribrid. A plain **witch does not siphon** and a plain **siphoner does not get the witch bonus**.

## Emissary of a werewolf pack

An Alpha may name a **witch, siphoner, heretic or tribrid** as their **Emissary** (`/pack emissary <player>`). The Emissary pays **10% less** for rites while the Alpha or a Beta of the pack is within 16 blocks. Their mountain ash lines also block more creatures. A witch can be the Emissary of **only one** Alpha. See [Werewolves and packs](Werewolves-And-Packs#emissary).

## Rite of Severance

A witch, siphoner, heretic or tribrid can perform the [Rite of Severance](Rite-Of-Severance), which breaks the sire bond of a sired player on the circle (or their own, if the circle is empty).

## Weaknesses

Witches and siphoners have no special weakness of their own. They are not prey, and pass mountain ash. They lose their magic if they join the vampires (witch) or become heretics (siphoner).

## Ending it

- The Cure removes the bloodline.
- Joining the vampires: witch becomes nothing; siphoner becomes a heretic.
- A witch who is also a werewolf: the Rite of the Silver Moon cures only the lycanthropy.

## Mixins

The witch bonus uses three mixins into Enchanted (`AltarRangeMixin`, `RiteCostMixin`, `RiteCancelMixin`; all `require = 0`). If an Enchanted update breaks one, that bonus is missing and the log says so; nothing crashes. See [Parts and compatibility](Parts-And-Compatibility#mixins).
