# Infinity Stones: turning off `soul.regen` clears every effect

For the author of Infinity Stones (Tensura). We found this while putting his stones on our skill wheel; we did not change his mod.

- **Mod version:** `8.0.0-alpha.6`
- **Power:** Soul stone, `soul.regen` (Reality/Soul regeneration)

## Steps

1. Carry the Soul stone and switch `soul.regen` on.
2. Have another effect on the player that the power did not add: Vampirism's or Werewolves' rage, a potion, a beacon effect.
3. Switch `soul.regen` off.

## Expected

Only the effects the power added are removed (`regeneration` and `instant_health`).

## Actual

Every effect on the player is removed. The power's last command is `effect clear @s`, which has no effect id, so it clears everything: the other mods' buffs, potions and beacon effects too.

## Suggested fix

Clear only what the power applied: `effect clear @s minecraft:regeneration` and `effect clear @s minecraft:instant_health` (or whatever effects `soul.regen` gives, one `effect clear @s <id>` each).

## Why it matters to a mod pack

With other mods that give status effects (Vampirism, Werewolves, Tensura), switching the power off silently removes their effects. No other mod can prevent it, because the effects are cleared by a command on his side.
