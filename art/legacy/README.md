# Superseded art

The four Werewolves-model boss textures (`deucalion_demon_wolf`, `peter_hale_wolf`, `klaus_hybrid`, `beast_of_gevaudan`) were replaced by the GeckoLib ones (`*_geo.png`, plan 17, open question 11). They were moved out of `src/main/resources` so they are not in the jar. `ArtLegacyTest` keeps them out.

`blockbench/` holds the four old Blockbench projects of those textures (Modded Entity format, the Werewolves beast model, 128x64): `klaus_hybrid`, `beast_of_gevaudan`, `deucalion_demon_wolf`, `peter_hale_wolf`. They were in `art/blockbench/bosses/` and are not the real ones: **the GeckoLib models are in `art/blockbench/creatures/`** (`klaus_hybrid`, `beast_of_gevaudan`, `deucalion_demon_wolf`, `peter_hale_alpha`). `klaus_hybrid.bbmodel` here is the old Werewolves-model project, not the one with the current coat.
