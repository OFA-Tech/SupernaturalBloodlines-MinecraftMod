# Enhanced Celestials 2 (optional)

[Enhanced Celestials 2](https://www.curseforge.com/minecraft/mc-mods/enhanced-celestials-2) adds lunar events: a Blood Moon, a Blue Moon, a Harvest Moon and their "super" versions. This mod can read them. It is **optional**: install it from CurseForge or Modrinth if you want it (Supernatural Bloodlines does not include it). Tested with **Enhanced Celestials 2 Core 2.0.3.x** and **Default Lunar Events 2.0.1.x** on 1.21.1.

You need four jars for the events: Core, **Default Lunar Events** (Core alone has no events), **CorgiLib** and **Data Anchor**. Server and clients need the same ones.

## The rule

An Enhanced Celestials event is **a second layer on top of the real moon**. A Blood Moon can fall on a new moon, but it will not make a werewolf shift: the full-moon rites, the forced shift and moon control all follow the real full moon, as before.

## What each event does

| Event | What changes |
|---|---|
| **Blood Moon** | A vampire's bite gives **x1.25 blood** (`blood_moon_bite_bonus`). Wild werewolf packs may spawn as on a full moon, at **half the chance** (`blood_moon_pack_chance_factor`). Enhanced Celestials' own spawn changes apply to our mobs too. |
| **Blue Moon** | Every rite costs **x0.8 altar power** (`blue_moon_rite_cost`). It never lets you perform a full-moon rite on a night that is not a full moon. |
| **Harvest Moon** | Our crops (vervain and the wolfsbane strains) are put in Enhanced Celestials' list of Harvest Moon crops, so they should drop **double**, like the vanilla ones (not yet checked in a game). |
| **Super moons** (always a full moon) | The Rite of the Moon, the Silver Moon and Immortality cost **x0.75** (`super_moon_rite_cost`), on top of the Blue Moon discount. |
| **Super Blue Moon** | The Werewolves **forced full-moon shift is skipped for everybody** (`super_blue_skips_forced_shift`, on by default): one calm full moon. Shifting at will follows the usual rule. |
| **Our lunar eclipse** | It becomes an Enhanced Celestials event: the moon turns dark orange and the light turns red. Our chat notice is the only one. If another event is already running, our eclipse still happens (the werewolf weakness is ours) and our own chat line and sky are used. |

Every number is in [Configuration](Configuration#celestials-supernaturalbloodlines-celestialstoml) and on the Admin page of the [hub](Supernatural-Hub). `1` turns an effect off. The part switch is `celestials` (restart required); without Enhanced Celestials, or with the part off, nothing changes.

## The hub

On the **Path** page, in all three looks, the almanac (the next six moons) gets a **coloured ring** on a day that has an event, with its name in the tooltip, and a line such as **"Blood Moon in 3 days."** under the full-moon line.

## Not checked, not built

See [Known limits](Known-Limits#enhanced-celestials-2-optional).
