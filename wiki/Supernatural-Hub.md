# Supernatural hub

> **Status: in progress.** Races, ladders, tasks, skill points and skills are in. The hub screen has all its pages, launchers and a tab in Tensura's menu; the Loadout waits for the ability HUD.

## The supernatural race

Your **supernatural race** is worked out from what the mod already stores about you. It is never saved on its own, so it cannot drift from your real state.

| Race | Who |
|---|---|
| Ghoul | a Sanguinare that finished, until the turning is done |
| Original hybrid, Original, Tribrid, Hybrid, Heretic | the species of the same name |
| Hunter | a hunter, a potential, or one of the Five |
| Vampire | a Vampirism vampire with no species |
| Werewolf | a Werewolves werewolf with no species |
| Human | everyone else |

A **witch** or a **siphoner** also has a second, **magic** race. It is climbed at the same time as the creature race: a witch-werewolf climbs the Witch and the Werewolf ladders. A tribrid or a heretic is one race, with no second ladder.

## Ladders

Each race has a ladder of rungs. Rung 0 is the race itself and pays nothing. Every other rung has tasks; claiming the last one reaches the rung and pays skill points (SP) and, with Tensura, EP. A rung once reached is never lost, even if you change race and come back.

The ladders are datapack files, `data/supernaturalbloodlines/supernatural/ladder/<race>.json`, so a server owner or a pack maker can change tasks and rewards without code. See `docs/parts/supernatural.md` in the repository for the format.

Ghouls are frozen: no task counts and nothing can be claimed until the turning is done.

## Skills

Skill points (SP) from the ladders buy **skills**. There are 40 to buy, in 11 trees (one per race, none for a Ghoul). Each tree lists a skill with the rung of your race it needs, sometimes a skill to learn first, and for Predator's Thirst a quest.

- A skill is bought once, with the one SP pool you share across your races. A witch-werewolf sees both trees.
- A skill works only while the tree of your current race lists it. If you change race it goes dormant, and it comes back when you are that race again. You never lose a skill you bought.
- A skill can add health, speed, attack or armor, change a number in one of the mod's rules (compulsion range, rite cost, siphon cap and so on), give you a Tensura skill, or change how much blood you get from feeding.
- The skills are datapack files (`supernatural/skill` and `supernatural/tree`), so a pack can change costs and trees.
- Primeval Roar (S-WOL-8) is never bought: a True Alpha who is a Primeval Alpha gets it (phase 12d).

### What the skills change

Skills that tune a rule: Silver Tongue (compulsion lasts 1.5 times as long and the villager discount is a bit bigger: +25 reputation, the villager's own limit), Bloodmaker's Yield (2 bottles of Original's Blood), Sire's Call (call every 5 minutes), Steady Thirst and Venom Fang (hybrids), Moon Calm, Pack Heart, Roar's Echo and Wolfsbane Tolerance (werewolves), Steady Circle, Wide Circle, Emissary's Bond and Ancestral Voice (witches), the five siphoner skills, Steady Aim, Hunter's Eye, Patient Hunter and Mark Mastery (hunters), and Vervain Blood. Ashwarden: vampires and werewolves that touch a mountain ash line you laid (while you are online) are pushed back and weakened for 5 seconds.

**Elder's Skin (S-VAM-7)** gives you **Vampirism's own less_sundamage skill** (it then also shows in Vampirism's skill screen and costs no Vampirism skill points; the sun hurts less by Vampirism's own rule). **Night Stride (S-VAM-2, +5% speed at night) and Thick Hide (S-WOL-1, +1 armor)** overlap with what Vampirism and Werewolves already give; they stack with those on purpose.

Keen Fangs adds 1 blood to every feed on human-like blood (villagers, traders and other humans).

### Quests and your Vampirism and Werewolves skills

Some skills need a quest first. Predator's Thirst (S-VAM-6) needs Hunter of Hunters: kill 10 vampires, have 3 bites on a vampire refused and feed 25 times. It opens when you reach the rung of your tree that lists the skill. With Predator's Thirst, biting a vampire feeds you (3 blood, or what Vampirism gives for that victim) and drains them; a vampire mob is also hurt (never killed) and weakened for 10 seconds, and an Original cannot be killed by the drain.

The skill points of Vampirism and Werewolves are spent through the hub on those mods' own skill trees: the server asks the mod whether the skill can be unlocked and the mod takes its own points. A Ghoul cannot unlock skills.

## The screen (phase 12e)

Press **Home** (Supernatural Bloodlines key category, rebindable; it also closes the hub) to open the hub. It is one screen with pages; how it looks is a **theme**, a client setting (`hub_theme` in `supernaturalbloodlines-hub-client.toml`). There are three, with the same pages, actions and keys:

- **Grimoire** (default): an open book. Ribbons on the right edge are the pages, wax seals under them the other mods' screens.
- **Blood Moon**: a dark screen with a big red moon that fills as you claim the tasks of your next rung; the rungs are gems around it (a witch's or siphoner's second ladder is the gold ring and the gems above). Pages on the left rail, other mods on the right rail, your tasks and points in the cards under the moon.
- **Tensura**: Tensura's own menu style (grey frame, navy panels), made wider, with the pages and the other mods as two lists on the left. Only with Tensura; otherwise the Grimoire.

Change it in [Configuration](Configuration#supernatural-client-supernaturalbloodlines-hub-clienttoml), even while the hub is open: it keeps the page.

- **Ribbons** on the right edge of the book are the pages: Path, Skills, Quests, Rites (with Enchanted), Bloodline and Pack (with Werewolves, for werewolf-type races or anyone in a pack). 
- **Path.** The left page is your story: your name, your races, who sired you (and whether the bond is severed), then each ladder you climb, rung by rung. A green seal is a rung reached, the red one is your next rung (with how many of its tasks you have claimed), tan ones are sealed. The right page lists the next rung's tasks, each with a tag for the mod it comes from (SB ours, VMP Vampirism, WW Werewolves, ENC Enchanted, TEN Tensura) and its count. A done task shows a **wax stamp**: press it to claim. The server checks every claim.
- **Quests.** Every task of your next rungs on the left (both ladders for a witch or siphoner werewolf), and any open skill quest such as Hunter of Hunters. Pick one to see it in full on the right: its source, its progress, how that mod does it (for a level step, the altar to use), and the stamp when it can be claimed.
- **A Ghoul** sees Path, with the turning and the time left, and Bloodline; the other pages say to complete the turning first.
- **Keyboard:** Tab or the up and down arrows move through everything on the page (scrolling to it), Page Up and Page Down scroll, Enter or Space presses it, the left and right arrows turn the page, Esc or the hub key closes. Every tooltip says in words what the picture shows.

### Pages and launchers (phase 12e, Tasks 3 to 5)

- **Skills.** Three tabs. *Bloodline skills*: your race trees (both for a witch or siphoner werewolf, a shared skill once), each skill known, ready to learn, locked with the reason, or dormant; press the stamp to learn it with your skill points. *Your Vampirism or Werewolves tree*: that mod's own skills, unlocked with that mod's own points (the mod checks parents and exclusions). A Loadout tab comes with the ability HUD.
- **Rites.** The rite tasks of your next rungs, with their claim stamps, then every rite of this mod the server has, yours first: its power, circle and items.
- **Bloodline.** Your family tree: the sire's line above you (and whether your bond is severed), and everyone you sired below; a **Call your sired** stamp for Originals and hybrid types (the same as `/bloodline call`), the Five for a hunter, your genes (Unknown ? until you discover them, unless the server shows them always), and Tensura's race, EP and magicules.
- **Pack.** Only for a werewolf or a hybrid type. Your rank, the members and who is online, and the `/pack accept` and `/pack leave` actions (leaving asks you to press twice) (the server checks them as if typed).
- **Path** also shows your Tensura race and EP.
- **Seals.** Wax seals under the ribbons open other mods' screens as they are: **T** Tensura's status menu, **E** Enchanted's book, **V** or **W** your Vampirism or Werewolves skill tree. Press Esc there and the hub comes back on the page you left.
- **Tensura's menu** has a fifth tab on the right, under its three "coming soon" tabs, that opens the hub.
