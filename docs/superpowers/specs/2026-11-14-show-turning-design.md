# Plan 11: turning the show's way (short design, approved 2026-11-14)

**Why:** the owner wants vampires made the way *The Vampire Diaries* makes them: vampire blood in your system, then death, then human blood. Vampirism's quick routes (a bite or the fang item) should not hand out a full vampire. Instead they leave the player stuck as a **Ghoul** until they complete the real turning.

**Goals:**
- A real turning: you die with vampire blood in you, go through a timed transition, and finish by drinking human-like blood.
- A Ghoul state for anyone who turned through Vampirism's bite or fang item. A Ghoul is frozen in every progression until it finishes the real turning.
- Biting a vampire never feeds you, and costs a little of your own blood.

**Non-goals:** the vampire-feeding skill (Mikael). It moves to plan 12, which adds skill points and quests and will unlock feeding on vampires. Hybrid and tribrid venom on vampires is already rule R42, so no change there.

**Anti-goals:**
- Taking away anything a player already earned. EP, items and levels stay.
- Turning an existing vampire into a Ghoul on update. Only new turnings follow these rules.
- Breaking Vampirism's own UI or rituals. Refusals must explain themselves.

## Behaviour

1. **Vampire blood in your system.** Drinking from a vampire, or using a vampire-blood item, marks you as "has vampire blood" for a limited time. The builder sets the duration; the show's is about a day.
2. **Dying with vampire blood** puts you in **transition** for 24,000 ticks, the same timer as the hybrid transition in plan 04. Reuse that machinery.
   - If you drink human-like blood within that time, you become a full vampire: you join Vampirism's vampire faction normally.
   - If you don't, you die for real and come back as you were before. A Ghoul stays a Ghoul.
   - Vampirism's "dying / give up" screen is the death moment where it fits.
3. **Vampirism's bite or fang item** turns you into a **Ghoul**:
   - Vampirism level 1, with sun damage and blood thirst. You are weak to vampire weapons.
   - **No Vampirism levels or skill points.** Level-up rituals and altars refuse, with a message saying to complete your turning.
   - **No Tensura evolutions.** "Complete your turning" is added as a requirement to Tensura evolutions. EP keeps accumulating.
   - A player whose Tensura race is exactly `tensura:human` shows `tensura:ghoul`, then `tensura:vampire` on completing the turning (R20a follows). Other races keep their Tensura race.
   - A Ghoul leaves the state only through behaviour 1 and 2.
4. **Biting a vampire** (player or mob) never fills your blood, and costs a small amount of your own. Plan 12's skill will lift this.
5. **Existing vampires** keep their status.

## Approaches considered

- **Chosen:** reuse plan 04's transition and the rule tables (`BloodlineRules`). The Ghoul is a new state, and Vampirism's leveling and Tensura's evolutions get gates in front of them.
- **Rejected:** replacing Vampirism's infection entirely. It's bigger, and it breaks other mods that expect Sanguinare.
- **Rejected:** taking levels back from Ghouls after the fact. It's confusing, and it loses what the player earned.

## Left to the builder

- How long vampire blood stays in your system.
- How human-like blood is detected (reuse plan 05's human-like list).
- The exact hooks for Vampirism's level-up refusal and the Tensura evolution requirement (events first, mixins within budget).
- The blood cost when biting a vampire.
