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
