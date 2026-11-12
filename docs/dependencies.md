# Linked-mod dependencies

No linked mod's jar, code or assets are in this repo. Everything is pulled from Maven or CurseMaven at build time.
All of them are `compileOnly` (never published) and, for dev runs, `localRuntime`. All are `transitive = false`:
the published POMs point at other repos and older versions (JEI, Guide-API, ...), so every runtime jar is listed by hand.
Version values live in `gradle.properties`.

| Mod | Coordinate | Repository | Compile | Runtime | Why |
|---|---|---|---|---|---|
| Vampirism | `de.teamlapen.vampirism:Vampirism:1.21-1.10.13` (`api` classifier for compile) | `maven.maxanier.de/releases` | api jar | full jar | vampire part. teamlapenlib is bundled inside the full jar, no separate artifact |
| Werewolves | `de.teamlapen.werewolves:Werewolves:1.21-2.0.3.4` | `maven.paube.de/releases` | full jar | full jar | werewolf part (no api classifier is published). Requires Vampirism at runtime |
| Enchanted | `net.favouriteless.enchanted:enchanted-neoforge-1.21.1:4.2.7` | `maven.favouriteless.dev/releases` | yes | yes | witch / curse part |
| stateobserver | `net.favouriteless.stateobserver:stateobserver-neoforge-1.21.1:1.6.3` | same | no | yes | Enchanted runtime dep |
| modopedia | `net.favouriteless.modopedia:modopedia-neoforge-1.21.1:1.1.10` | same | no | yes | Enchanted runtime dep |
| Tensura: Reincarnated | `curse.maven:tensura-reincarnated-643695:9476869` (tensura-neoforge-2.0.1.5) | `cursemaven.com` | yes | yes | slime / race part (`tensura_file_id`) |
| ManasCore | `curse.maven:manascore-619025:8022425` (manascore-neoforge-4.0.0.2) | `cursemaven.com` | outer + nested modules | outer jar | race / skill / storage API used by Tensura (`manascore_file_id`) |
| Architectury | `dev.architectury:architectury-neoforge:13.0.11` | `maven.architectury.dev` | yes | yes | required by Tensura and ManasCore (`>=13.0.8`) |
| GeckoLib | `software.bernie.geckolib:geckolib-neoforge-1.21.1:4.9.3` | `dl.cloudsmith.io/public/geckolib3/geckolib/maven/` | yes | yes | Tensura needs `>=4.7.5`, Enchanted `4.7`; 4.9.3 is what the pack runs |
| SmartBrainLib | `net.tslat.smartbrainlib:SmartBrainLib-neoforge-1.21.1:1.16.11` | `dl.cloudsmith.io/public/tslat/sbl/maven/` | yes | yes | Tensura needs `>=1.16.7` |
| TerraBlender | `com.github.glitchfiend:TerraBlender-neoforge:1.21.1-4.1.0.8` | `maven.minecraftforge.net` | no | yes | Tensura needs `>=4.1.0.0` |

Curios is not part of this plan step; a later plan adds it.

## Where the coordinates came from

- Vampirism / Werewolves: directory listings and POMs on the two Maven hosts (`maven-metadata.xml` for the version list); Vampirism's own `neoforge.mods.toml` for teamlapenlib.
- Enchanted and friends: Enchanted's POM (Gradle metadata) and `gradle/libs.versions.toml` on GitHub branch `1.21.1`.
- Tensura / ManasCore: CurseForge file pages (project 643695 file 9476869; project 619025 file 8022425); the page shows the CurseMaven line.
- Tensura's runtime deps: its `neoforge.mods.toml` (manascore_storage / skill / race, architectury, geckolib, smartbrainlib, terrablender).

## ManasCore nested modules

The real ManasCore modules (`race`, `skill`, `storage`, `network`, ...) are jar-in-jar under `META-INF/jars/` of the outer jar,
so `io.github.manasmods.manascore.race.api.RaceAPI` is not visible from the CurseMaven artifact alone.

- Compile: the `extractManascoreModules` Gradle task (a `Sync`) unpacks `META-INF/jars/*.jar` into `build/manascore-modules/`, and those jars are on `compileOnly` (and `testCompileOnly`).
- Runtime: only the outer jar is added; FML unpacks the nested modules itself (jar-in-jar).
- Nothing is committed: `build/` is ignored.

## Run switches

Pass on the Gradle command line, e.g. `.\gradlew.bat runClient -PnoLinkedMods`.

| Property | Effect |
|---|---|
| `-PnoLinkedMods` | no linked mod on the runtime classpath (compile classpath unchanged). Use to prove the game starts with every part "disabled (missing mods)" |
| `-PnoWerewolves` | drops only Werewolves from runtime |
| `-PnoCurios` | reserved, no-op until Curios is added |

## `neoforge.mods.toml`

All five are `type="optional"`, `ordering="AFTER"`:

| modId | range |
|---|---|
| `tensura` | `[2.0.1,2.0.2)` |
| `manascore_race` | `[4.0.0,4.1)` |
| `vampirism` | `[1.10.13,1.11)` |
| `werewolves` | `[2.0.3,2.1)` |
| `enchanted` | `[4.2.7,4.3)` |

The real versions (Tensura 2.0.1.5, ManasCore 4.0.0.2, Vampirism 1.10.13, Werewolves 2.0.3.4, Enchanted 4.2.7) all fall inside these ranges, so no change was needed.
