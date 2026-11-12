# Mixins

Every mixin must be listed here. Aim for 0 to 2; use `require = 0` and a config switch.

All mixins below live in `supernaturalbloodlines.tensura.mixins.json`. `TensuraMixinPlugin` applies the whole config only when Tensura and Vampirism are installed and `parts.vampirismTensura` is not `false` in `supernaturalbloodlines-common.toml` (read from the file, because COMMON configs load after mixins). Otherwise nothing is touched.

| Mixin | Target | Why | Config switch |
|---|---|---|---|
| `VampireRaceSunMixin` | `VampireRace.shouldBurn(LivingEntity)` (HEAD, returns false) | Tensura has no config option for its vampire burn (checked in `VampireConfig`). A player in the Vampirism vampire faction already takes Vampirism's sun damage, so Tensura's fire would be a second burn. Also called from `GhoulRace`/`WightRace`, so it is gated on "player is in the Vampirism vampire faction" (`VampireSunRule`). Skipping `shouldBurn` also skips the helmet damage. | `parts.vampirismTensura` |
| `RaceUtilsSunMixin` | `RaceUtils.isUnderSun(LivingEntity)` (HEAD, returns false) | `VampireRace.onTick` adds Weakness, Slowness and Mining Fatigue (amplifier 3) whenever `isUnderSun` is true, before it ever calls `shouldBurn`. Faction vampires skip them too: Vampirism's sun handling wins (its own sun damage, nausea and weakness by level). The same method gates Tensura's bat form under sun, so a faction vampire may use it by day. Only faction-vampire players are affected; the night/moon-phase branch of `onTick` is unchanged. | `parts.vampirismTensura` |
