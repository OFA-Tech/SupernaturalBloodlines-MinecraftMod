# Sirelines

Needs Vampirism and Enchanted (part `species`). Werewolves is optional.

A **sireline** is a family tree of who made whom. A **sire** is the one who turned you, and their **sired** are the ones they turned. The server keeps the tree for players and for creatures.

## How a sire is recorded

| How you were made | Sire |
|---|---|
| a vampire **player** uses Vampirism's **Infect** action on you, and you turn (sanguinare) | that vampire |
| a vampire player converts a **creature** (it becomes a vampire creature) | that vampire. A vampire *mob* that infects someone is not recorded. |
| you finish the **hybrid transition** | the Original whose blood you drank before dying |
| an **Alpha's bite** turns you | that Alpha |
| you are **compelled** (a lesser vampire mob) | the compeller |

- A child has **one sire**. The **first sire wins**: nothing overwrites it, except that finishing the hybrid transition replaces it with the Original's, and that a player whose bond was **severed** can be sired again (see below).
- A pending infection is kept for **24,000 ticks** (20 minutes), and is dropped if the infection is cured.
- A **cure** removes you from the tree as a child. Your own children stay in it.
- A dead sired creature is removed from the tree. Players never are.
- The tree remembers the last known name of everyone, so it can show an offline or deleted sire.

## `/bloodline tree`

`/bloodline tree [player]` prints "Sireline of X", "Sired by Y" (if any) and then every descendant, depth first, one indented line each (creatures show their entity type), to a maximum depth of 10.

- Anyone can read **their own** tree.
- Reading someone else's needs permission level 2 ("You can only read your own sireline.").

## `/bloodline call`

An **Original, Original hybrid, hybrid or tribrid** can **call their sired**. Everyone else: "Only an Original or a hybrid can call their sired."

- **12,000 ticks (10 minutes)** cooldown. It only starts if someone was actually called ("None of your sired can hear you now.").
- Only **direct** children are called. A sired player whose bond was severed is skipped.
- A sired **player** (online) gets a chat message with a clickable **[Accept]** (valid for **60 seconds**, only for them). On accepting they are **teleported** to a free spot with a floor, two blocks from where the caller **is now**, in any dimension. A dead sired player cannot accept. If the caller is in the air or in water, the sired land on the ground below (up to 8 blocks); if there is none, the call is refused ("There is no safe ground near the one who called you.").
- Sired **creatures** loaded in the caller's dimension are told every second, for **30 seconds**, to walk to the caller. No chunk is loaded to reach them.
- `/bloodline accept <token>` is what the button runs. It's not meant to be typed.

## Sire bond

- **R43:** a **hybrid** type cannot hurt their sire: directly, by projectile, by area cloud or by pet. The sire can still hurt them. A cured player with a stale sire is not bound, and neither is one whose bond was severed.
- Gaps: an area cloud with no owner (a dispenser), and an arrow whose owner is offline, can still hurt.
- With **Tensura**, **every** sired player becomes the sire's Tensura subordinate, and cannot hurt the sire either. See [Tensura integration](Tensura-Integration#sires-as-tensura-owners-and-naming).

## Compulsion

The Original, Original hybrid, hybrid, tribrid, or any **vampire of Vampirism level 10 or higher** can compel:

- **How:** **sneak + right-click with an empty main hand** on a target within **4 blocks**, **eye to eye**: the target's head must look within 30 degrees of you, and you within 15 degrees of them.
- **Immune:** **players** (always), anyone with the **vervain** effect, and **hunters** (potentials and the Five).
- A click on a villager or lesser vampire by an eligible caster is always the compulsion's: no trade screen opens, even when refused.
- One attempt per caster per second.

### A compelled villager

A 5-second choice appears in chat, with clickable buttons:

- **[Discount]**: a cure-sized discount (major positive gossip 20, reputation 100) for **a day** (24,000 ticks). What it added is taken back exactly when it ends. It never stacks, and a villager holds **one** discount at a time: a second caster's compulsion takes the first back.
- **[Follow]**: the villager walks to you for **5 minutes**.

### A compelled lesser vampire

A vampire mob (not an advanced vampire, boss, baron, task master or a converted creature) becomes **your sired**:

- it does **not despawn**
- it **never targets you, or anyone below you** in your sireline (everyone else is still prey). A descendant whose bond was severed is not spared.
- a vampire already sired by another stays theirs ("That creature already answers to another.")

## Sireline death

Optional, **off by default**: `sireline_death` in `supernaturalbloodlines-species.toml`.

When an **Original or Original hybrid** is **killed with white oak**:

- everyone they sired **and everyone those sired** dies too, with the damage type `supernaturalbloodlines:sireline` ("... died with the line they were born into"). It gets past armor, invulnerability and effects, **and a Totem of Undying**.
- A sired vampire in Vampirism's dying state is killed too.
- **Offline players** die once on their next login (only if they still have a sire).
- **Loaded creatures** are removed; unloaded ones when their chunk loads. Nothing is force-loaded.
- **A sired Original, and their whole line, survive.**
- The dead leave the tree. A sireline death gives a hunter no mark kill.

## Naming and breaking the bond

- A sire can **name** their own sired through Tensura's naming menu, even with the server's player naming off, if the sired turned their own naming on. A named sired player gets Tensura's stats but **never** its race evolution. See [Tensura integration](Tensura-Integration#sire-naming-r101).
- **Siring needs no consent** (it comes from blood or a bite). The **[Rite of Severance](Rite-Of-Severance)** is how a sired player frees themselves of the bond: a witch type performs it on the night of a full moon, with the sire's blood. It frees the Tensura link, the naming exception, the hybrid's "can't hurt the sire" (R43), `/bloodline call` and the sparing by the sire's compelled creatures. **The tree still shows the sire and sireline death still applies** (blood, not bond).
- **Re-siring:** a **new sire replaces the old one** and ends the severance, so a freed player can be sired again. The first-sire-wins rule only holds while the bond is whole. From then on the **new** sire's tree and sireline death apply.

## Hybrids and the tree

A sired werewolf (bitten by an Alpha) has the Alpha as their sire. That is why the Tensura link stops a bitten Beta from hurting the Alpha.
