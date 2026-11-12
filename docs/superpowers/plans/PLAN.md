# SupernaturalBloodlines: roadmap

One NeoForge 1.21.1 mod (`supernaturalbloodlines`, package `net.ofatech.supernaturalbloodlines`) that links Tensura, Enchanted, Vampirism and Werewolves, and adds TVD-inspired species. Each **part** switches on only when its mods are installed.

- **Design (read first):** `docs/superpowers/specs/2026-11-11-supernaturalbloodlines-design.md`
- **Species rules (source of truth, R1–R103):** `docs/superpowers/specs/SPECIES-RULES.md`
- **Plans:** `docs/superpowers/plans/`

| # | Plan | Part | Needs | Can start after |
|---|---|---|---|---|
| 0 | `00-foundation.md` | repo, build, part loader, bloodline core | none | none |
| 1 | `01-vampirism-tensura.md` | `vampirism_tensura` (unified vampire, hunter/werewolf rules, holy damage, bloodless) | Vampirism, Tensura (+ Werewolves) | 0 |
| 2 | `02-enchanted-tensura.md` | `enchanted_tensura` (altar power, magicule curses, spirit poppet, rites, EP) | Enchanted, Tensura | 0 |
| 3 | `03-vampirism-enchanted.md` | `vampirism_enchanted` (plant parity, cure rites, blood rite, poppets) | Vampirism, Enchanted (+ Werewolves) | 0 |
| 4 | `04-species.md` | `species` (Original → hybrid → Original hybrid → witch/siphoner → heretic → tribrid) | Vampirism, Enchanted (+ Werewolves for hybrid types) | 0, and 3's rite factory helper |
| 5 | `05-species-tensura.md` | `species_tensura` (species as a layer on the Tensura race, apex evolutions, sireline → Tensura subordinates and naming, werewolf rank skills) | 4 + Tensura | 1, 4, 8, 9 |
| 6 | `06-artifacts.md` | `artifacts` (sun/moon/resurrection rings, vervain, ash dagger, the Cure, compass, hearing device) | Vampirism, Enchanted (+ Werewolves, Curios optional) | 0, 3, white oak from 4 |
| 7 | `07-hunters.md` | `species`: hunters of the Five (rite, mark, potentials, curse, instinct) | Vampirism, Enchanted | 4 (Task 1), 6 (`Supernatural`) |
| 8 | `08-sirelines.md` | `species`: sireline tree, `/bloodline`, compulsion, optional sireline death | Vampirism, Enchanted | 4 (Task 3) |
| 9 | `09-werewolf-packs.md` | `species`: Teen Wolf lineage (born/bitten, hidden gene, Rite of Lineage), ranks (Omega/Beta/Alpha/True Alpha), eyes, packs, roar, emissary, anchors, eclipse, electricity | Vampirism, Enchanted, Werewolves | 4 (Task 1), 8 (Task 1) |

## Working in parallel sessions

- Plans 1, 2 and 3 are independent once plan 0 is merged. Plans 4 and 6 can run in parallel after 3. Then 7, 8 and 9 run in parallel (9 after 8, Task 1), and 5 comes last. Run each in its own session, on its own branch and git worktree (`feature/<part>`). Each part touches only its own package `net.ofatech.supernaturalbloodlines.parts.<part>` and its own data folder `src/main/resources/data/supernaturalbloodlines/<part>/…`.
- **Shared files** (`core/…`, `SupernaturalBloodlines.java`, `Config.java`, `build.gradle`, lang files): change them only in small, separate commits, and rebase before merging.
- Start a session with: "Read `PLAN.md`, the design, and `docs/superpowers/plans/0N-….md`, then execute it task by task."

## Definition of done (per part)

- `./gradlew build` and `./gradlew test` are green.
- The part's GameTests pass: `./gradlew runGameTestServer`.
- The game starts with the part's mods **missing**, with no errors from this mod in `logs/latest.log`.
- Every feature has been checked in game. The part's checklist in `docs/parts/<part>.md` is ticked.
- `CHANGELOG.md` is updated.
