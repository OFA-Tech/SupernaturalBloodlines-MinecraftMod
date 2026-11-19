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
| `/bloodline gene doppelganger <player> <true\|false>` | **level 2** | gives or takes the hidden doppelgänger gene. The player must be **online**. It counts as rolled (a later login does not roll over it) and does not reveal the gene to them. See [Werewolves and packs](Werewolves-And-Packs#genes). |

Tokens last 60 seconds (calls) or 5 seconds (compulsion choices), are tied to one player, and are used once.

## `/pack` (needs Werewolves; `/sb pack ...` is the same tree, for servers where another mod also has a `/pack`)

| Command | Who | Does |
|---|---|---|
| `/pack invite <player>` | the **leader** of a pack (an Alpha or True Alpha) | invite an online player who is in no pack: a werewolf joins as a Beta, another Alpha as a second Alpha, **anyone else as a Packmate**. They get an **[Accept]** button (60 s). |
| `/pack kick <player>` | the **leader** | remove a member, online or not. A werewolf Beta becomes an Omega. There is no limit on the size of a pack. |
| `/pack ally invite <player>` · `accept <token>` · `remove <player>` | the leader of a pack | make or end an alliance with another pack (its leader answers the **[Accept]**). Allies cannot hurt each other and the [Alpha Roar](Werewolves-And-Packs#alpha-roar) spares them. They share no pack bonus. |
| `/pack accept <token>` | the invited player | what the button runs |
| `/pack challenge <leader>` · `challenge accept` · `challenge decline` | a **second Alpha** of the pack; the leader | challenge the leader for the lead; the leader accepts or declines. A duel to the first defeat (any death is the defeat), where nobody dies and nobody loses the spark; the winner leads. Within 64 blocks and one dimension; 2 minutes to answer; the same two wait 10 minutes after a duel and a refused challenger 5. See [Werewolves and packs](Werewolves-And-Packs#mixed-packs-and-alpha-packs). |
| `/pack leave` | anyone in a pack | leave the pack (a werewolf Beta becomes an Omega). The leader can't; a second Alpha or a Packmate can. |
| `/pack info` | a pack member | the pack: its leader, members, Emissary, allies and your anchor |
| `/pack heal <player>` | an **Alpha** | give the spark up (it is destroyed, nobody inherits it) to fully heal someone within 4 blocks, curing hybrid venom and wolfsbane. The Alpha becomes a Beta. Not for a True Alpha. |
| `/pack protect on\|off` | anyone | **protect my True Alpha path** (on by default while it is open): a kill that would steal a spark destroys it instead (a pack inheritance can still give you one). In the hub: your own Profile and Settings. |
| `/pack surge` | a werewolf in a pack | Moonblood Surge: a buff for the pack within 24 blocks, paid for with Weakness and Slowness (240 s cooldown). Also a button on the Home page. |
| `/pack spirits` | an **Alpha** or True Alpha | Wolf Spirits: three spirit wolves for three minutes (90 s cooldown). Also a button on the Home page. |
| `/pack npc order <number> follow\|guard\|rest` | the wolf's owner or the pack's leader | give a Brother an order. The Pack page has the same buttons; `<number>` is the one `/pack npc list` shows. |
| `/pack npc hunt <number>`, `/pack npc sort <number>` | the same | turn a Brother's hunting trips, or its chest sorting, on or off. |
| `/pack npc release <number>` | the wolf's owner only | free a Brother (it goes back to the wild, even if it is out of reach: it is freed when it next loads). A Tensura subordinate is released in Tensura. |
| `/pack npc list` | anyone | the wolves of your pack with their numbers (the Pack page shows them as rows). |
| `/supernaturalbloodlines debug npcs list`, `... npcs release <player>` | operators | list every pack wolf, or release a player's (the player can be offline). Admin > Debug has both. |
| `/pack roar` | an **Alpha** or True Alpha | the Alpha Roar (60 s cooldown). Same as the **Alpha Roar** [key bind](Key-Binds). |
| `/pack emissary [player]` | the **leader** | name a witch, siphoner, heretic or tribrid who is **already a member** of the pack (invite them first) as your Emissary. Without a name it clears it. |
| `/pack anchor [player]` | a werewolf | name a player as your **anchor**. Without a name it clears it. |

See [Werewolves and packs](Werewolves-And-Packs#packs-and-pack) for what each one means. A refused command says why in chat (for example "Only an Alpha can do that.", "You are not in a pack.").

## `/supernaturalbloodlines debug` (permission level 2)

For testing and for operators fixing a player. All of them affect only the player you name, and bad names give a normal command error and change nothing.

| Command | Does |
|---|---|
| `/supernaturalbloodlines debug get <player>` | prints the player's bloodline state |
| `/supernaturalbloodlines debug set <player> <species>` | sets the species. Names (any case): `NONE`, `ORIGINAL`, `ORIGINAL_HYBRID`, `HYBRID`, `WITCH`, `SIPHONER`, `HERETIC`, `TRIBRID`, `POTENTIAL`, `HUNTER_OF_THE_FIVE`. It sets the state only: it does **not** change the player's faction. |
| `/supernaturalbloodlines debug event <player> <event>` | runs a bloodline event through the rules. The **context is the defaults**: no faction, human-like, no Originals in the world, Werewolves loaded as installed. Events that need a faction (such as the Rite of the Five, which needs a hunter) will be refused. Faction actions in the result are listed, **not performed**. |
| `/supernaturalbloodlines debug supernatural <player> show` | prints the player's ladder state: rungs, skill points, done and claimed tasks |
| `/supernaturalbloodlines debug supernatural <player> backfill` | lists the rungs the one-time migration gave this player |
| `/supernaturalbloodlines debug supernatural <player> reset` | forgets all ladder progress; the next login migrates again |
| `/supernaturalbloodlines debug supernatural <player> sp <n>` | adds (or takes) skill points |
| `/supernaturalbloodlines debug supernatural <player> rung <race> <n>` | sets a ladder's rung with no reward |
| `/supernaturalbloodlines debug eclipse start\|stop` | starts or stops a [lunar eclipse](Werewolves-And-Packs#lunar-eclipse). `start` outside the night makes tonight's eclipse and works in daytime until 23216; "An eclipse is already running." / "No eclipse is running." |

Events (any case): `RITE_OF_IMMORTALITY`, `DIED_WITH_ORIGINAL_BLOOD`, `DRANK_DOPPELGANGER_BLOOD`, `DRANK_OTHER_BLOOD`, `TRANSITION_TIMEOUT`, `UNSTABLE_DEATH`, `JOINED_VAMPIRE`, `RITE_OF_THE_MOON`, `RITE_OF_CLEANSING_BLOOD`, `RITE_OF_THE_SILVER_MOON`, `LEFT_BASE_FACTION`, `ANCESTRAL_CALLING_WITCH`, `ANCESTRAL_CALLING_SIPHONER`, `DIED`, `DRANK_THE_CURE`, `GENE_AWAKENED_AS_VAMPIRE`, `RITE_OF_THE_FIVE`, `KILLED_PREY`, `KILLED_BY_PREY`, `RITE_OF_LINEAGE_AWAKEN`, `CHOSEN_AS_POTENTIAL`, `KILLED_MARKED_HUNTER`, `HUNTER_CURSE_LIFTED`.

`debug eclipse` needs the `species` part and Werewolves. `get`, `set` and `event` are core: they work whatever parts are on.

## Other commands you will use

- `/vampirism level <faction> <level> [player]` and `/vampirism level none @s` (Vampirism): put a player in or out of a Vampirism faction. This mod's rules follow the change (the layer, the race, the sire).
- Tensura's race and owner commands: for example to **reset a player from an apex race** before removing the mod, or to clear a sire link by hand. See [For server owners](For-Server-Owners#removing-the-mod-or-tensura).
