# Groups

A **group** is a leader and their members. The kind of group follows the leader, and you never pick it:

| The leader is | The group is a |
|---|---|
| a werewolf, a hybrid, an Original hybrid or a tribrid | **pack** |
| a hunter (Vampirism hunter, Hunter of the Five, Potential) | **brotherhood** |
| a vampire (also a vampire siphoner) | **clan** |
| anyone else: a witch, a siphoner who is not a vampire, a heretic, a human | **coven** |

The first line that fits wins, so a werewolf who is also a vampire leads a pack.

## Founding
Invite someone. The invite works only when you fit the group you would lead: an active Alpha for a pack, a vampire for a clan, a witch, siphoner or heretic for a coven, a hunter for a brotherhood. The server setting `open_founding` lifts that (a human may then lead a coven). `clans_enabled`, `covens_enabled` and `groups_enabled` stop new groups of a kind; existing ones stay. `max_members` caps a group (0 = no limit).

## Everyday actions
`/group invite <player>`, `accept`, `leave`, `kick <player>`, `ally invite|accept|remove`, `info`. `/clan`, `/coven`, `/brotherhood` and `/sb group` are the same tree, and `/pack` keeps all its own commands. The hub's **Group** page (the old Pack page) has buttons for all of them, and says "pack", "clan", "coven" or "brotherhood" with that kind's own words. The wolf-only parts (wolves, Roar, duels, anchor, heal) show only for a pack.

## When the leader changes kind
The group changes type. Nobody is removed and nothing is deleted: what belonged to the old type is kept and hidden, and returns if the leader goes back. Every member is told once.

## When the leader stays away
After `absent_leader_days` (7) offline, the lead passes: a **pack** to its strongest second Alpha, a **clan** to the Day Walker who has been in longest, a **coven** to the longest-serving Elder, a **brotherhood** to the online member with the highest mark. With no heir nothing changes. The old leader stays as the next rank down.

## Allies and parties
Leaders of any two groups can ally, a coven and a pack included; allied members cannot hurt each other. With Open Parties and Claims, a group is its party. All of this works without Werewolves.

## Covens
A coven has a **Regent** (its leader), **Elders** and members. The Regent appoints Elders (after `elder_min_days` in the coven; one seat per four members, a vote of the Elders when there are some); the Regent and the Elders vote on shunning a member (who then cannot join any coven for a week) and on a new Regent. Votes need three electors and a majority; with fewer the Regent decides.

**The circle.** Stand within five blocks of a gold chalk and press **Open the circle** on the Group page: the rest of your coven near you gets a card on Home to **Join**. When the Regent, or any member who opened it, starts a rite on that chalk, the linked witches pay a share of it from their **channel meter** instead of the altar paying it all, and a bigger circle makes the rite stronger. **Strictly my meter** (the default) never asks more of you than your meter holds; the other two levels let a heavy rite hurt a little or a lot. If the one who holds the circle dies while the rite runs, the rest are hurt. A circle of 3 / 7 / 13 reaches tier I / II / III.

**What a coven can do:** the three grand rites (Fete des Benedictions, Night Watch, Great Harvest; see [Rites](Rites#coven-rites)), forge a Moon Ring or a Sun Ring, and the Rite of Mastery's Ultimate needs a circle of seven. The numbers are in [Configuration](Configuration#groups-supernaturalbloodlines-groupstoml).

## Clans
A clan is led by a vampire-like head (a vampire, an Original, an Augustine, a vampire-siphoner). A new member is a **Night Walker**; the head promotes a member to **Day Walker** (`/group promote <player>`, `/group demote <player>`), or the clock does after 14 real days in the clan; a demoted member is promoted again only by the head.

**Shared feeding.** When a clan member feeds on a creature, each clanmate within 16 blocks gets a little blood (a tenth of it, one or two points, at most four a minute for each mate). Bottles, blocks and bites between clanmates share nothing, and a share earns no task or skill.
**The sire-line bonus.** Each online clanmate of your own sireline within 32 blocks adds 3% attack damage, up to 15%.

**The Charter and the Game of Kings.** A clan of three or more can forge a **Clan Charter** (`/group charter forge`: a book and quill and a gold block). Only the clan can pick it up; it cannot be hurt and never despawns. A Day Walker with 7 days in the clan can **challenge** for it (`/group challenge`); the head cannot refuse (`/group challenge accept`, or it starts by itself after ten minutes while the head is online; a head who is away is waited for), and at the next in-game midnight the online Day Walker or head who holds the Charter leads. A chest, a hopper, a shulker box and a Curios slot hold it for nobody: **a head who puts the Charter away, or is offline at midnight with it, loses the clan to the challenger** (if the challenger is online, alive and a Day Walker), so the head should keep the Charter on them and stay online until midnight. No contest is settled in the first minute after a server starts. A clan has at most one contest every 7 days. A lost Charter can be reforged after a day. While a contest is on, its challenger and the holder of the Charter cannot be demoted, exiled or removed.

**Exile.** The head can exile a member (`/group exile <player>`): they cannot rejoin that clan for 7 days. A kick has no bar.

All of it works without Werewolves, and the numbers are in [Configuration](Configuration#groups-supernaturalbloodlines-groupstoml). Brotherhood perks come in a later update.

## Brotherhood

Hunters can found a Brotherhood. Plain humans can join and lead one; with open founding off, they cannot found one. The First Brother leads until an eligible online brother has a strictly higher mark. A tie keeps the leader. Nearby fitting brothers add attack damage and can share prey sight, but never kills. The Group page shows the Map and Code and only your mark and the First Brother's.
