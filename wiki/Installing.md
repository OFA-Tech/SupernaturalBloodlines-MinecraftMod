# Installing

**Supernatural Bloodlines** is a **NeoForge 1.21.1** mod that links Tensura, Enchanted, Vampirism and Werewolves and adds supernatural species and bloodlines. It does **nothing on its own**: each feature (a *part*) switches on only when the mods it needs are installed. See [Parts and compatibility](Parts-And-Compatibility).

## Requirements

| What | Version | Notes |
|---|---|---|
| **Minecraft** | **1.21.1** | exactly 1.21.1 |
| **NeoForge** | **21.1.265** or newer | built and tested on 21.1.265 |
| **Java** | 21 | what NeoForge 1.21.1 needs |

The mod declares every linked mod as **optional**, with a version range. **A linked mod outside its range stops the game from starting** with a version error, so stick to these:

| Mod | Mod id | Supported range | Built and tested with |
|---|---|---|---|
| **Vampirism** | `vampirism` | `[1.10.13, 1.11)` | 1.21-1.10.13 |
| **Werewolves** | `werewolves` | `[2.0.3, 2.1)` | 1.21-2.0.3.4 (needs Vampirism) |
| **Enchanted** | `enchanted` | `[4.2.7, 4.3)` | 4.2.7 |
| **Tensura: Reincarnated** | `tensura` | `[2.0.1, 2.0.2)` | 2.0.1.5 |
| **ManasCore** (race) | `manascore_race` | `[4.0.0, 4.1)` | 4.0.0.2 |
| **Curios** | `curios` | `[9.5, 10)` | 9.5.1+1.21.1 |

Mods those mods need (install them as their own pages say):

| Needed by | Mod | Tested with |
|---|---|---|
| Tensura, ManasCore | **Architectury** | 13.0.11 (Tensura needs 13.0.8 or newer) |
| Tensura, Enchanted | **GeckoLib** | 4.9.3 (Tensura needs 4.7.5 or newer, Enchanted 4.7) |
| Tensura | **SmartBrainLib** | 1.16.11 (needs 1.16.7 or newer) |
| Tensura | **TerraBlender** | 1.21.1-4.1.0.8 (needs 4.1.0.0 or newer) |
| Enchanted | **stateobserver** | 1.6.3 |
| Enchanted | **modopedia** | 1.1.10 |
| Werewolves | Vampirism | |

Vampirism ships its own library (teamlapenlib) inside its jar.

Nothing else is needed. **Curios is optional**: without it the rings work from your inventory. **Werewolves is optional too**: without it there are no hybrids, packs or wolfsbane.

## Client and server

**Install the same mods and the same version of this mod on the server and on every client.**

- The mod registers items, blocks, effects and races, so a client without the same parts cannot join (registry mismatch).
- It also reads **COMMON configs** on both sides (not synced): keep the part switches and the species_tensura options equal. See [For server owners](For-Server-Owners#common-configs-must-match).
- A **dedicated server** runs fine: the client-only classes (the downed screen, the eye particles, the Hunter outline, the eclipse fog, the Alpha Roar key) are only loaded on a client.

## Steps

1. Install NeoForge 1.21.1.
2. Put the linked mods you want (from the table above) and **this mod's jar** in the `mods/` folder, on the server and on each client.
3. Start the game. The log shows one line per part:

   ```
   Supernatural Bloodlines loading
   Part SPECIES: enabled
   Part ARTIFACTS: disabled (missing: enchanted)
   ```

   A part says `enabled`, `disabled (missing: ...)` (a mod is not installed), `disabled (config)` (you switched it off), or `disabled (requires ...)` (a part it builds on is off).
4. Read [Parts and compatibility](Parts-And-Compatibility) to see which parts you got.

## Which mods switch on which parts

| Part | Needs | Optional extras |
|---|---|---|
| **vampirism_tensura** | Vampirism, Tensura | Werewolves |
| **enchanted_tensura** | Enchanted, Tensura | Vampirism (one rite) |
| **vampirism_enchanted** | Vampirism, Enchanted | Werewolves |
| **species** | Vampirism, Enchanted, and the vampirism_enchanted part | Werewolves (hybrids, packs) |
| **species_tensura** | Vampirism, Enchanted, Tensura, and the species part | Werewolves (rank skills) |
| **artifacts** | Vampirism, Enchanted, and the vampirism_enchanted part | Werewolves (Moon Ring, wolfsbane), Curios (ring slot) |

With **no linked mod at all** the game starts and every part reads `disabled (missing: ...)`.

## Updating

1. Stop the server.
2. Back up the world and the `config/supernaturalbloodlines-*.toml` files.
3. Replace the jar (and keep the linked mods inside the supported ranges).
4. Read the [changelog](https://github.com/OFA-Tech/SupernaturalBloodlines-MinecraftMod/blob/main/CHANGELOG.md).

## Removing

Read [For server owners](For-Server-Owners#removing-the-mod-or-tensura) **first**. The short version: **reset players on an apex race with Tensura's race command before you delete the jar**, or those players cannot load.
