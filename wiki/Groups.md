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

Clan, coven and brotherhood **perks** (circles, the Charter, brothers in arms) come in later updates.