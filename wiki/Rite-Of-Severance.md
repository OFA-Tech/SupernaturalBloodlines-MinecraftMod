# Rite of Severance

The **Rite of Severance** (rule **R101b**) **breaks the whole sire bond** of a sired player. Siring needs no consent: it comes from blood or an Alpha's bite. This rite is how a sired player gets free. Part `species` (needs Vampirism and Enchanted; no Werewolves needed).

## How to do it

On a **medium circle** of ritual chalk, **6000** altar power (a witch pays x0.75, as for any rite):

| | |
|---|---|
| Items | **Sire's Blood**, **Spirit of Otherwhere** (Enchanted; the rare ingredient) |
| Performer | the **caster** must be a **witch, siphoner, heretic or tribrid** |
| Target | the **sired** player standing on the **centre block** of the circle. If the centre is empty, the **caster** is the target, so a sired witch-type can **sever herself**. |
| Time | **the night of a full moon**: the full-moon day **and** the time of day between **12786 and 23216** (Werewolves' own full-moon window). **Noon on the full-moon day is refused.** |

The Sire's Blood must be the blood **of the target's own sire**. Anything wrong cancels the rite, gives the ingredients back and tells the caster on the action bar:

| Message | Why |
|---|---|
| "This rite can only be performed under a full moon." | not the night of a full moon |
| "Only a witch, siphoner, heretic or tribrid can break a bond." | the caster is not a witch type |
| "The rite needs Sire's Blood." | no Sire's Blood in the circle |
| "Nobody sired them: there is no bond to break." | the target has no sire |
| "That is not the blood of their sire." | the blood belongs to someone else |
| "Their bond is already broken." | already severed |

On success the target sees: "The bond is broken. Your sire no longer holds you."

**There is no consent check.** A witch type can sever an unwilling player who stands in the circle.

## Getting Sire's Blood

Hit a player or creature in **melee** with a **silver blade** in your **main hand** while holding a **Glass Bottle in your off hand**, and the hit does damage: the bottle is used up (not in creative) and you get **Sire's Blood** bound to **the one you hit** (stack size 1; the tooltip says "Blood of <name>"). It cannot be drunk.

- Anyone can be hit for blood. Only the rite checks that it was the **right** sire.
- Silver blades are the item tag `#supernaturalbloodlines:silver_blades`:
  - the **Silver Dagger** (this mod)
  - Werewolves' **silver sword**
  - Tensura's silver swords and katanas: short sword, long sword, great sword, sword, katana, kodachi, odachi, tachi
- The **Silver Dagger** is crafted from a **silver ingot** over a **stick**. The ingot is any item in `#supernaturalbloodlines:silver_ingots` (`#c:ingots/silver`, Werewolves' silver ingot, Tensura's silver ingot). Werewolves is what fills `#c:ingots/silver`: with **neither Werewolves nor Tensura nor another silver mod** the dagger **cannot be crafted** (it is still in the creative tab). It is a sword of the iron tier (+1 attack, speed -1.8).

Textures for both items are placeholders.

## What it frees

For the sired who goes through it, the bond is gone:

- no **Tensura owner** link (and a Tensura owner set by a real **naming** is cleared too, but **only if it was the severed sire**; someone else's owner stays, and so does the name)
- the sire's **naming exception** no longer applies
- a hybrid can **hurt their sire** again (R43 ends)
- the sire can't use **`/bloodline call`** on them
- creatures the sire **compelled** no longer spare them

## What it keeps (blood, not bond)

- The **sire is still recorded**: the family tree shows it.
- **Sireline death still applies**: a severed child still dies with an Original (see [Sirelines](Sirelines#sireline-death)).

## Re-siring

A **new sire replaces the old one and ends the severance**: a severed player can be sired again (the "first sire wins" rule only holds while the bond is whole). From then on the **new** sire's tree and sireline death apply, not the old one's. The same sire coming back later is a fresh bond. A cure, death with no sire, or any other change of sire also ends the severance.

## Limits

- **Rolling back to an older build forgets severances**: the bonds come back there. See [For server owners](For-Server-Owners#removing-the-mod-or-tensura).
- The altar and chalk with the real ingredients, the tooltips and the item models have **not been checked in a real client** (the tests start the rite directly). See [Known limits](Known-Limits).
- Other rites with `requires_full_moon` (the Rite of the Moon, the Silver Moon, Immortality) still work on the whole full-moon **day**. Only this rite is night-only.
