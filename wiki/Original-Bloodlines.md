# Original Bloodlines

Every vampire belongs to the **line of one Original**. A player Original is the root of their own line; six more roots (Mikael, Finn, Elijah, Kol, Rebekah and Klaus) belong to the NPC Originals. A vampire you turn joins your line, and a player an NPC vampire turns joins the NPC's.

## Finding your Original

The Bloodline page shows the Original of your line as **?** until you discover it. The **Rite of Revelation** reveals it (it also reveals your genes). Discovery is yours alone: the people you sired do not learn it from you. Player ancestors are always shown; NPC ancestors stay **?**. The server can show every Original from the start with `original_visibility = always`.

## Line death

On by default (`original_line_death`). Kill an Original with white oak and everyone in their line dies. Offline players die once at their next login. When an Original is desiccated, in the dying state or downed, their line is warned (without the name if you have not discovered them).

## Breaking out

Both rites need a witch, siphoner, heretic or tribrid performer, a **lunar eclipse**, and bottles of Sire's Blood. Someone freed from a line is not touched by line death, and the Bloodline page shows a broken link and an open padlock.

| Rite | Frees | Needs |
|---|---|---|
| **Rite of the Broken Line** | only the target (their sire bond stays, and so do the people they sired) | two bottles: the Original's and the target's, white oak ash, a spirit of otherwhere; medium circle |
| **Rite of the Imprisoned Original** | the target and their whole branch | the target's blood, 3 white oak ash, 2 spirits of otherwhere; large circle; the Original standing on the centre inside a closed mountain ash ring (radius up to 8) for 30 seconds |

The target must be online and not an Original. Both rites work on player Originals. The power (12000 and 36000), the 30 seconds and the radius are tunable (`imprisonment_rite_seconds`, `imprisonment_ring_max_radius`, `line_rites_enabled`).
