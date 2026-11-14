# Supernatural hub

> **Status: in progress.** The foundation is in: every player has a supernatural race and a ladder for it, held as data. There is no screen yet; the hub, the tasks, skill points and skills arrive in the next phases of the same system.

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

Skills that tune a rule: Silver Tongue (compulsion reaches 6 blocks, a bigger villager discount), Bloodmaker's Yield (2 bottles of Original's Blood), Sire's Call (call every 5 minutes), Steady Thirst and Venom Fang (hybrids), Moon Calm, Pack Heart, Roar's Echo and Wolfsbane Tolerance (werewolves), Steady Circle, Wide Circle, Emissary's Bond and Ancestral Voice (witches), the five siphoner skills, Steady Aim, Hunter's Eye, Patient Hunter and Mark Mastery (hunters), Vervain Blood and Elder's Skin. Ashwarden has no effect yet, because a mountain ash line is already unbreakable for vampires and werewolves.

Keen Fangs adds 1 blood to every feed on human-like blood (villagers, traders and other humans).
