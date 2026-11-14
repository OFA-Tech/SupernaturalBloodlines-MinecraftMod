# Hunters of the Five

Needs Vampirism and Enchanted (part `species`). Werewolves is optional (werewolf types are prey only with it).

The **Five** are the supernatural hunters of the world: marked humans from Vampirism's **hunter faction**, who grow stronger with every kill of *prey*.

## Who is prey

**Prey** means vampires, werewolves, hybrids, Originals, heretics and tribrids. **Witches and siphoners are not prey**: they are on nature's side (R81). A witch-werewolf is not prey either, because the species wins over the faction.

## How to become one: Rite of the Five

Medium circle, **10,000** power. Items: **5 candles** (plain, undyed), an **Iron Ingot**, a **Book**, a **filled Taglock**. The target must be a **human-like hunter** in Vampirism's hunter faction with no species. See [Rites](Rites#rite-of-the-five).

- It is refused if **the Five are full** ("The Five are already complete."): `max_five` (default 5). A live **potential** and an owed potential each hold a slot too, so the Five can never go over the limit.
- A player who isn't a hunter: "Only a hunter can do this."

The Five are kept in a **roster**. Leaving the hunter faction frees the slot. Logging out does not.

## The mark

- Every **prey kill** grows the mark, up to **20 kills**: **+1% attack damage** and **+0.5% movement speed** per kill (so up to +20% and +10%).
- **From 5 kills:** every 2 seconds the server tells the hunter which prey (no witches, siphoners or spectators) are within **24 blocks**, and the **hunter's client outlines them**. Only that player sees it. The outline is the entity's team colour (white if it has no team).
- **Immune to compulsion** (also potentials).
- Kill credit survives Vampirism's dying state: a hunter who hits a vampire into the dying state gets the kill when it gives up or dies (up to 10 minutes).

### Instinct

A Hunter of the Five with **no prey kill for 3 in-game days** (72,000 ticks; the clock starts when the mark is gained) gets **Weakness I**, checked every minute, until the next prey kill. It does not apply while the hunter is on a scoreboard team with an online prey player.

## Losing the mark

If a marked hunter is **killed by prey** (any entity, mobs too), they **lose the mark** and a random **online human-like hunter** (with no species) becomes a **potential**. With nobody online, the choice waits until the next hunter joins. A potential's **first prey kill completes the Five**: they become a Hunter of the Five. A potential who leaves the hunters owes a new one, like a lost mark.

## Hunter's curse

A **prey killer of a marked hunter** is cursed (R83): every 600 +- 200 ticks (30 s), a hallucination only they see:

- a phantom zombie for 5 s,
- a whisper (cave ambience),
- or darkness (40 ticks).

The curse lasts **until a potential completes the Five**. It does not stack. Humans and witches are exempt. Werewolf venom sends the same hallucinations to a poisoned vampire.

This covers a **vampire** who kills one of the Five (R25) and a werewolf type who does (R83). The Cure also clears it.

## Weaknesses and limits

- A hunter with a Tensura race tagged undead or daemon cannot be a hunter. See [Tensura integration](Tensura-Integration#hunters-and-werewolves-cannot-be-undead-races).
- Not every prey kill counts: a vampire that dies of the dying state's timer after more than 10 minutes, or whose killer logged out, gives no kill.
- Hunters can't take a werewolf's lineage by rite.

## Tensura (Tensura part)

No skills, **+10% max aura**. Apex race: **Eternal Hunter** (not undead). See [Tensura integration](Tensura-Integration).

## Hunters and the other species

- A hunter with the hidden gene: the gene stays latent forever.
- A Hunter of the Five who joins a non-hunter faction loses the mark.
