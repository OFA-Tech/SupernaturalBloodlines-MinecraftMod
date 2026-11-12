# Supernatural Bloodlines

A NeoForge 1.21.1 mod that links **Tensura: Reincarnated**, **Enchanted**, **Vampirism** and **Werewolves**, and adds supernatural species and bloodlines inspired by vampire and werewolf TV shows.

Each feature set is an optional **part** that turns on only when the mods it needs are installed. With none of the linked mods installed, the mod loads, reports every part as disabled, and does nothing else.

> Status: early development (foundation). See `docs/superpowers/plans/PLAN.md` for the roadmap.

## Parts

| Part | Config key | Required mods | Checklist |
|---|---|---|---|
| `ENCHANTED_TENSURA` | `enchantedTensura` | enchanted, tensura | [docs/parts/enchanted_tensura.md](docs/parts/enchanted_tensura.md) |
| `VAMPIRISM_ENCHANTED` | `vampirismEnchanted` | vampirism, enchanted | [docs/parts/vampirism_enchanted.md](docs/parts/vampirism_enchanted.md) |
| `VAMPIRISM_TENSURA` | `vampirismTensura` | vampirism, tensura | [docs/parts/vampirism_tensura.md](docs/parts/vampirism_tensura.md) |
| `SPECIES` | `species` | vampirism, enchanted | [docs/parts/species.md](docs/parts/species.md) |
| `SPECIES_TENSURA` | `speciesTensura` | vampirism, enchanted, tensura (and the `species` part) | [docs/parts/species_tensura.md](docs/parts/species_tensura.md) |
| `ARTIFACTS` | `artifacts` | vampirism, enchanted | [docs/parts/artifacts.md](docs/parts/artifacts.md) |

Werewolves and Curios are optional extras inside parts. Each part also has a switch in the common config. Details: [docs/parts.md](docs/parts.md).

## Install

1. Install NeoForge 21.1.x for Minecraft 1.21.1 (Java 21).
2. Put the mod jar in `mods/` on the server and on every client (both sides need it). The server and the clients must have the same part switches and the same linked mods.
3. Add the mods for the parts you want, at the tested versions:
   - Tensura: Reincarnated `2.0.1` and ManasCore `4.0.x`
   - Vampirism `1.10.13`
   - Werewolves `2.0.3`
   - Enchanted `4.2.7`

The startup log has one line per part (`Part SPECIES: enabled` or `disabled (missing: …)`).

## Docs

| What | Where |
|---|---|
| Design | `docs/superpowers/specs/2026-11-11-supernaturalbloodlines-design.md` |
| Species rules (R1–R103) | `docs/superpowers/specs/SPECIES-RULES.md` |
| Roadmap and plans | `docs/superpowers/plans/` |
| Parts, debug commands, run checks | `docs/parts.md` |
| Mixins | `docs/mixins.md` |
| Linked mod versions | `docs/dependencies.md` |

## Building

Java 21 toolchain (Gradle downloads it if missing):

```
./gradlew build
```

Run checks without any linked mod: `./gradlew runGameTestServer -PnoLinkedMods`, `./gradlew runClient -PnoLinkedMods`.

## License

MIT for this project's own code. The linked mods are separate works under their own licenses and are not included here. Fan-made, not affiliated with any TV show.
