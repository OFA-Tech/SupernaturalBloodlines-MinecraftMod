# Plan 13d: compatibility hardening

Branch `feature/compat-hardening` (from main 208d93f). Source: a read-only audit against Vampirism 1.10.13, Werewolves 2.0.3.4, Tensura 2.0.1.5 and Enchanted 4.2.7. **Every item is verified (javap or a test) before it is fixed**; each fix has a GameTest or unit test that fails without it. One commit per group, the 10-run matrix (`--no-daemon`, output to files) after each, report at `.superpowers/sdd/13d-compat/report.md`. No client run and no OFA pack edit while the owner plays.

## Groups

| Group | Items |
|---|---|
| A | **H1** Werewolves `free_will` skill counts as FULL moon control (`moon_bound_werewolves` stays on by default, R96); `wiki/Werewolves-And-Packs.md` |
| B | **H2** `supernaturalbloodlines:skill/<id>` modifiers are owned by `Part.SUPERNATURAL` in `StalePartModifiers`; "Before removing the mod" in `wiki/For-Server-Owners.md` with every modifier id |
| C | **H3** `KeyCover` covers keys only while `hud_enabled` and the server has the hud channel; restores on leaving the server and when `hud_enabled` is false; removal docs |
| D | **M4** veto vampire-line `SET_RACE` for non-vampire-faction players (like `GhoulRace`); **M5** werewolf cure restores the race we replaced (recorded by `WerewolfRaceSync`) |
| E | **M7** `undead_treatment_enabled=false` undoes the `tensura:undead` tag merge; **M8** `only_alphas_turn` switch; **M9** startup warning when `RiteCostMixin` / `RiteCompleteMixin` did not apply; **M10** optional dependency ranges; **M6** next-preset mixin docs (no code) |
| F | Low: siphoner keeps villager and merchant menus; `/sb pack`, `/sb bloodline` aliases; vervain doc note; `bypasses_invulnerability` trim; **owner:** S-VAM-7 grants Vampirism's `less_sundamage`; S-VAM-2 / S-WOL-1 overlap documented |
| G | Doc drift: `Configuration.md` "do nothing yet", `Known-Limits.md` (version, mangled text), `docs/mixins.md`, `docs/parts.md` (9 parts), `docs/parts/hud.md:7`, `Tensura-Integration.md` mixin count, `neoforge.mods.toml` comment |

## Rules

- Parts never import each other; shared code goes in `shared/`.
- A known limit gets a `shortcut:` comment or a doc line, not silence.
- If an audit claim does not hold, the report says so and nothing is changed for it.
