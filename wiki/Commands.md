# Commands

The mod has three command roots: `/bloodline`, `/pack` and `/supernaturalbloodlines`. Vampirism, Werewolves, Tensura and Enchanted have their own commands too (for example `/vampirism level`), which this mod follows.

"Permission level 2" means an operator (the default `op-permission-level`) or a command block.

## `/bloodline` (part `species`)

| Command | Who | Does |
|---|---|---|
| `/bloodline tree [player]` | anyone for **their own** tree; **level 2** to read another player's (offline names work through the profile cache) | prints the sireline: "Sireline of X", "Sired by Y", then every descendant, indented (creatures show their type). Max depth 10. |
| `/bloodline call` | an **Original, Original hybrid, hybrid or tribrid** | calls your sired to you. 10 minute cooldown, only starts when someone was called. See [Sirelines](Sirelines#bloodline-call). |
| `/bloodline accept <token>` | the sired player | what the **[Accept]** chat button runs. Don't type it. |
| `/bloodline compel <token> discount\|follow` | the caster of a compulsion | what the **[Discount]** and **[Follow]** buttons run. Don't type it. |

Tokens last 60 seconds (calls) or 5 seconds (compulsion choices), are tied to one player, and are used once.

## `/pack` (needs Werewolves)

| Command | Who | Does |
|---|---|---|
| `/pack invite <player>` | an **Alpha** or True Alpha | invite an online werewolf with no pack, who isn't an Alpha. They get an **[Accept]** button (60 s). |
| `/pack accept <token>` | the invited player | what the button runs |
| `/pack leave` | a werewolf in a pack | leave the pack and become an Omega. Alphas can't. |
| `/pack info` | a werewolf | the pack: its Alpha, Betas, Emissary and anchor |
| `/pack heal <player>` | an **Alpha** | give the spark away to fully heal someone within 4 blocks, curing hybrid venom and wolfsbane. The Alpha becomes a Beta. |
| `/pack roar` | an **Alpha** or True Alpha | the Alpha Roar (60 s cooldown). Same as the **Alpha Roar** [key bind](Key-Binds). |
| `/pack emissary [player]` | an **Alpha** | name a witch, siphoner, heretic or tribrid as your Emissary. Without a name it clears it. |
| `/pack anchor [player]` | a werewolf | name a player as your **anchor**. Without a name it clears it. |

See [Werewolves and packs](Werewolves-And-Packs#packs-and-pack) for what each one means. A refused command says why in chat (for example "Only an Alpha can do that.", "You are not in a pack.").

## `/supernaturalbloodlines debug` (permission level 2)

For testing and for operators fixing a player. All of them affect only the player you name, and bad names give a normal command error and change nothing.

| Command | Does |
|---|---|
| `/supernaturalbloodlines debug get <player>` | prints the player's bloodline state |
| `/supernaturalbloodlines debug set <player> <species>` | sets the species. Names (any case): `NONE`, `ORIGINAL`, `ORIGINAL_HYBRID`, `HYBRID`, `WITCH`, `SIPHONER`, `HERETIC`, `TRIBRID`, `POTENTIAL`, `HUNTER_OF_THE_FIVE`. It sets the state only: it does **not** change the player's faction. |
| `/supernaturalbloodlines debug event <player> <event>` | runs a bloodline event through the rules. The **context is the defaults**: no faction, human-like, no Originals in the world, Werewolves loaded as installed. Events that need a faction (such as the Rite of the Five, which needs a hunter) will be refused. Faction actions in the result are listed, **not performed**. |
| `/supernaturalbloodlines debug eclipse start\|stop` | starts or stops a [lunar eclipse](Werewolves-And-Packs#lunar-eclipse). `start` outside the night makes tonight's eclipse and works in daytime until 23216; "An eclipse is already running." / "No eclipse is running." |

Events (any case): `RITE_OF_IMMORTALITY`, `DIED_WITH_ORIGINAL_BLOOD`, `DRANK_DOPPELGANGER_BLOOD`, `DRANK_OTHER_BLOOD`, `TRANSITION_TIMEOUT`, `UNSTABLE_DEATH`, `JOINED_VAMPIRE`, `RITE_OF_THE_MOON`, `RITE_OF_CLEANSING_BLOOD`, `RITE_OF_THE_SILVER_MOON`, `LEFT_BASE_FACTION`, `ANCESTRAL_CALLING_WITCH`, `ANCESTRAL_CALLING_SIPHONER`, `DIED`, `DRANK_THE_CURE`, `GENE_AWAKENED_AS_VAMPIRE`, `RITE_OF_THE_FIVE`, `KILLED_PREY`, `KILLED_BY_PREY`, `RITE_OF_LINEAGE_AWAKEN`, `CHOSEN_AS_POTENTIAL`, `KILLED_MARKED_HUNTER`, `HUNTER_CURSE_LIFTED`.

`debug eclipse` needs the `species` part and Werewolves. `get`, `set` and `event` are core: they work whatever parts are on.

## Other commands you will use

- `/vampirism level <faction> <level> [player]` and `/vampirism level none @s` (Vampirism): put a player in or out of a Vampirism faction. This mod's rules follow the change (the layer, the race, the sire).
- Tensura's race and owner commands: for example to **reset a player from an apex race** before removing the mod, or to clear a sire link by hand. See [For server owners](For-Server-Owners#removing-the-mod-or-tensura).
