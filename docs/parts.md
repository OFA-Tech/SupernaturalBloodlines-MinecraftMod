# Parts

A part is an optional feature set. It is enabled when every mod it requires is installed and its config switch is on. The `@Mod` class logs one line per part at startup: `Part <NAME>: enabled`, `disabled (missing: …)`, `disabled (config)` or `disabled (requires …)`. Disabled parts' classes are never loaded.

| Part | Config key | Required mods | Needs | Checklist |
|---|---|---|---|---|
| `ENCHANTED_TENSURA` | `enchantedTensura` | enchanted, tensura | | [enchanted_tensura](parts/enchanted_tensura.md) |
| `VAMPIRISM_ENCHANTED` | `vampirismEnchanted` | vampirism, enchanted | | [vampirism_enchanted](parts/vampirism_enchanted.md) |
| `VAMPIRISM_TENSURA` | `vampirismTensura` | vampirism, tensura | | [vampirism_tensura](parts/vampirism_tensura.md) |
| `SPECIES` | `species` | vampirism, enchanted | | [species](parts/species.md) |
| `SPECIES_TENSURA` | `speciesTensura` | vampirism, enchanted, tensura | `SPECIES` | [species_tensura](parts/species_tensura.md) |
| `ARTIFACTS` | `artifacts` | vampirism, enchanted | | [artifacts](parts/artifacts.md) |

The server and clients need the same part switches and the same linked mods.

Werewolves and Curios are optional extras inside parts; the pure rules in `core` never import them.

## Debug commands

Permission level 2. For quick state changes in a test world:

- `/supernaturalbloodlines debug get <player>` prints the player's bloodline state.
- `/supernaturalbloodlines debug set <player> <species>` sets the species (name, any case).
- `/supernaturalbloodlines debug event <player> <event>` runs a `BloodlineEvent` through `BloodlineRules`. **The context is `BloodlineContext.defaults()`: faction NONE, human-like true, no Originals in the world.** Core can't see factions, so events that depend on a faction (for example the Rite of the Five, which needs a hunter) will be refused here. Faction actions in the result are listed but not performed.

Bad species or event names give a normal command error and change nothing.

## Run checks

- `./gradlew runGameTestServer -PnoLinkedMods` runs the GameTests without any linked mod.
- `./gradlew runClient -PnoLinkedMods` starts the game with no linked mod: the log must show every part as `disabled (missing: …)` and no ERROR from `supernaturalbloodlines`.
- `-PnoWerewolves` drops only Werewolves from the run classpath.
