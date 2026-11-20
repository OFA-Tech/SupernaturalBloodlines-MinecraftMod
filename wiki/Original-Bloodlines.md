# Original Bloodlines

Every vampire belongs to the **line of one Original**. A player Original is the root of their own line; six more roots (Mikael, Finn, Elijah, Kol, Rebekah and Klaus) belong to the NPC Originals. A vampire you turn joins your line, and a player an NPC vampire turns joins the NPC's.

## Finding your Original

The Bloodline page shows the Original of your line as **?** until you discover it. The **Rite of Revelation** reveals it (it also reveals your genes). Discovery is yours alone: the people you sired do not learn it from you. Player ancestors are always shown; NPC ancestors stay **?**. The server can show every Original from the start with `original_visibility = always`.

## Line death

On by default (`original_line_death`), for new and existing worlds alike: set it to `false` to keep the old behaviour (the old `sireline_death` key is ignored). Kill an Original with white oak and every vampire and player in their line dies; converted animals and villagers are never touched. **Only a melee white-oak blow by someone other than the Original counts:** an Original's own white oak (their own hand, or a kill that names them) does nothing, and neither does a death with no killer (the environment, a bare `/kill`). Offline players die once at their next login. When an Original is desiccated, in the dying state or downed, their line is warned once ("lies desiccated", or "lies open to white oak" when downed), without the name if you have not discovered them. A rite wrong in any way you could not know about (the wrong blood, the wrong Original in the circle, an open ring) gives the same answer: "The rite fails."

## Breaking out

The NPC Originals are the bosses (part `bosses`); Mikael, Klaus, Elijah and Rebekah are in. Line death and the warnings happen for them as for a player Original. Both rites need a witch, siphoner, heretic or tribrid performer, a **lunar eclipse** (which comes from Werewolves), and bottles of Sire's Blood. Someone freed from a line is not touched by line death, and the Bloodline page shows a broken link and an open padlock.

| Rite | Frees | Needs |
|---|---|---|
| **Rite of the Broken Line** | only the target (their sire bond stays, and so do the people they sired) | two bottles: the Original's and the target's, white oak ash, a spirit of otherwhere; medium circle |
| **Rite of the Imprisoned Original** | the target and their whole branch | the target's blood, 3 white oak ash, 2 spirits of otherwhere; large circle; the Original **brought down** (downed, in Vampirism's dying state, or desiccated) and held on the centre inside a closed mountain ash ring (radius up to 8) for 30 seconds. Standing there is not enough: "The Original must be brought down first." |

The target must be online and not an Original. **There is no consent step:** a witch can free an unwilling player, as with the Rite of Severance. Both rites work on player Originals. The power (12000 and 36000), the 30 seconds and the radius are tunable (`imprisonment_rite_seconds`, `imprisonment_ring_max_radius`, `line_rites_enabled`).

## NPC Originals

Mikael, Klaus, Elijah and Rebekah are four of the six Originals who are not players (part `bosses`). **Elijah** is calm: he makes a deal (one vial of an NPC Original's blood, any of the five, the same list for everybody, for an oath of 7 in-game days by default: not to harm or desiccate any NPC Original but Mikael, and not to strike him; breaking it makes him hostile for good, with no second deal. Your tamed animals and minions count as you), and pays the Elixir. **Rebekah** comes for whoever makes The Cure at the rite, and pays the True Cure. They are not Vampirism creatures: no sun, no bite. A blow that would kill one of them only **desiccates** them: they fall, with 1 health, drop a bottle of their blood (at most once every few days) and their line is warned. They get up after original_return_days days (5). **White oak kills one only while they are down**; on a standing one it only hurts (`npc_original_stake_needs_open`). A white oak death is for good, and the line dies with them. The Ash Dagger brings a standing one down. **Only a player's white oak kills an Original**: Mikael's own stake does not, so he cannot cause line death, and he does not hunt Originals or Original hybrids (`mikael_stakes_originals` turns that around, with a warning to the player he hunts). When an Original gets up again, their line is told. Each pays one reward, the first time they are brought down. Operators have `/supernaturalbloodlines boss <boss> ...` and a section in the Admin page. **Klaus** comes on a full moon night for a doppelgänger (a villager, or a player who carries the gene: a player is warned 90 seconds before he arrives) and needs Werewolves. He shifts into a wolf at half health or against a werewolf, and the wolf's bite is werewolf venom; his Blood cures it and starts the hybrid transition in his line. Neither Klaus nor Mikael's mob hand can start line death: only a player's white oak counts. **Being hunted by Klaus tells a player they carry the doppelgänger gene.** A plain mountain ash line holds neither Klaus nor Mikael: only one laid by a pack Emissary does.
