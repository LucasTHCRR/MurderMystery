<div align="center">

[![Discord](https://img.shields.io/badge/Discord-dc.gg%2Fpaperstream-5865F2?style=flat-square&logo=discord&logoColor=white)](https://dc.gg/paperstream)
[![Release](https://img.shields.io/modrinth/v/murdermystery-minigame?style=flat-square&label=release&color=00AF5C)](https://modrinth.com/plugin/murdermystery-minigame/versions)
[![Downloads](https://img.shields.io/modrinth/dt/murdermystery-minigame?style=flat-square&label=downloads&color=00AF5C)](https://modrinth.com/plugin/murdermystery-minigame)
[![License](https://img.shields.io/badge/license-GPL--3.0-blue?style=flat-square)](https://www.gnu.org/licenses/gpl-3.0.txt)

[![Built with Java 21](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/built-with/java21_vector.svg)](https://adoptium.net/)
[![Available for Paper](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/supported/paper_vector.svg)](https://papermc.io/)

[![Watch us on Youtube](https://raw.githubusercontent.com/intergrav/devins-badges/refs/heads/v3/assets/cozy/social/youtube-plural_vector.svg)](https://www.youtube.com/@PaperrStream)
[![Chat with us on Discord](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/social/discord-plural_vector.svg)](https://dc.gg/paperstream)
[![Available on Codeberg](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/available/codeberg_vector.svg)](https://codeberg.org/LucasTHCR/MurderMystery)

</div>

# MurderMystery

Murder Mystery Classic for your own Paper server. One murderer, one detective, everyone else is innocent, and nobody knows who is who.

---

Plays like the classic minigame on Hypixel: roles are handed out in secret, the murderer hunts, the detective protects, the innocents collect gold and try to stay alive. Rounds are short, the rules are simple, and every line of text, every number and every sound can be changed in a config file.

Drop the jar in, run `/murder setup start <arena>` and walk through your map. A guided wizard tells you what is missing at every step.

---

## Requirements

- **Paper 1.21.x** or a fork
- **Java 21**
- **Citizens**, **FancyNpcs** or **ZNPCsPlus** - optional, for lobby NPCs
- **PlaceholderAPI** - optional, for placeholders in scoreboard and holograms
- **Vault** - optional, to pay rewards into the server economy
- **AdvancedReplay** - optional, for round replays
- **CorpseReborn** - optional, for corpses at the place of death

Folia is not supported.

---

## The roles

| Role | Count | Gear | Goal |
|---|---|---|---|
| Innocent | the rest | nothing, a bow and one arrow per 10 gold | survive, collect gold, expose the murderer |
| Detective | 1 per 16 players | bow, a new arrow every 5 seconds | shoot the murderer, protect the innocents |
| Murderer | 1 per 16 players | knife, one hit kills, throwable | kill everyone without getting caught |

Roles are announced at the start. The knife and the detective bow arrive after 15 seconds. A round lasts 5 minutes, if time runs out the innocents win.

---

## How a round works

**The knife** can be thrown with right click. One second wind-up, then it flies in a straight line. It never leaves the murderer's hand, and there is a 5 second cooldown.

**Gold** lies at fixed spawn points and respawns on a timer. Every 10 gold buy one shot: the first time a bow with one arrow, after that one more arrow. This goes for the murderer too. Only the detective's bow reloads on its own.

**The detective** can die or quit. Then the bow drops, and whoever picks it up becomes the new detective. If the detective died in water, lava or the void, the bow lands in the middle of the map instead.

**The hero** is whoever kills the murderer without being the detective.

**Friendly fire** is punished: hitting an innocent with an arrow kills the shooter too.

**The final phase** starts when two survivors are left and everyone glows. With one survivor left the murderer gets Speed I.

---

## Setting up an arena

```
/murder setup start classic
```

The wizard walks the whole setup and always knows what is next. You walk through the map and press `[SET HERE]` in chat. It knows by itself whether it is collecting spawns, gold spawns or the altar, and says how many are still missing.

Two steps need more than standing still:

- **Bounds**: `/murder wand`, left click one corner, right click the other, then `[SET HERE]`
- **Spawns and gold**: these collect several points and advance on their own once the target is reached

The steps for zones, Kali altar, brewing stations, settings and the join NPC are optional. Finish with `/murder setup finish`, which validates and enables the arena.

Everything the wizard does can also be typed by hand. It reads the real state out of `arenas.yml` instead of counting along, so the two never get out of sync. `/murder info <arena>` shows a checklist of what is still missing.

---

## Commands

Main command `/murder`, aliases `/mm` and `/murdermystery`.

### Players

| Command | Permission |
|---|---|
| `/murder join [arena]` | `murder.play` |
| `/murder leave` | `murder.play` |
| `/murder role` | `murder.play` |
| `/murder list` | `murder.play` |
| `/murder vote <map>` | `murder.play` |
| `/murder shop` | `murder.shop` |
| `/murder stats [player]` | `murder.stats` |
| `/murder spectate <arena>` | `murder.spectate` |
| `/murder scoreboard` | `murder.play` |
| `/murder replay <last\|list\|play\|stop>` | `murder.replay` |

### Admin

| Command | What it does |
|---|---|
| `/murder setup ...` | guided setup wizard |
| `/murder create\|delete\|clone\|rename <arena>` | manage arenas |
| `/murder enable\|disable <arena>` | toggle an arena |
| `/murder info <arena>` | checklist of what is missing |
| `/murder set <arena> <option> <value>` | per arena settings |
| `/murder setlobby\|setexit` | global points |
| `/murder setspawn\|setgold\|setspectator <arena>` | arena points |
| `/murder setaltar\|setpotion <arena>` | Kali altar, brewing stations |
| `/murder wand\|pos1\|pos2\|setbounds\|zone ...` | regions and death zones |
| `/murder npc ...` | lobby NPCs |
| `/murder kali ...` | test blessings, check chances, set favor |
| `/murder start\|stop\|kick\|forcerole\|forcemap` | control a round |
| `/murder cleanup\|debug\|hooks\|save` | maintenance |
| `/murder config get\|set\|list <path>` | change config in game |
| `/murder reload [file]` | reload everything or one file |

`/murder help <topic>` has a short guide for each area: `setup`, `arena`, `spawns`, `zones`, `npc`, `kali`, `potions`, `gold`, `blocked`, `scoreboard`, `replay`, `nametags`, `config`.

---

## Permissions

| Permission | Default | What it does |
|---|---|---|
| `murder.play` | everyone | play and use the player commands |
| `murder.shop` | everyone | open `/murder shop` |
| `murder.stats` | everyone | use `/murder stats` |
| `murder.spectate` | everyone | spectate running rounds |
| `murder.replay` | everyone | watch own replays |
| `murder.replay.others` | op | watch replays of rounds you were not part of |
| `murder.replay.admin` | op | record, delete and clean up replays |
| `murder.join.full` | op | join full arenas |
| `murder.admin` | op | all admin and setup commands |
| `murder.admin.npc` | op | NPC commands, falls back to `murder.admin` |
| `murder.bypass.commands` | op | ignore the blocked commands while in a game |

---

## Configuration

Nothing is hard coded. No text, no number, no item, no sound, no particle.

| File | Contains |
|---|---|
| `config.yml` | times, roles, gold, death rules, blocked commands, hooks |
| `messages.yml` | every line of text, including the in-game help |
| `arenas.yml` | arenas, spawns, zones, per arena overrides (written at runtime) |
| `npcs.yml` | every NPC (written at runtime) |
| `scoreboard.yml` | sidebar, bossbar, actionbar, tablist, per state and per role |
| `items.yml` | every item |
| `sounds.yml` | every event with a sound, each one switchable |
| `gui.yml` | menu layouts and click actions |
| `kali.yml` | altar, favor curve, blessings, curses |
| `potions.yml` | mystery potion colours, effects, ratio |
| `cosmetics.yml` | skins, effects, prices |
| `stats.yml` | statistics per uuid |

Every global option can be overridden per arena. What is in `arenas.yml` wins, what is missing comes from `config.yml`:

```yaml
arenas:
  classic:
    overrides:
      round-time: 300
      gold.respawn-seconds: 15
      death-rules.water: false
```

`/murder reload` reloads without a restart. Running rounds are left alone.

---

## Death zones

Select with the wand, then `/murder zone add <arena> <name> <type>`.

| Type | Effect |
|---|---|
| `INSTANT` | instant death on entering |
| `DAMAGE` | damage per second while inside |
| `PUSHBACK` | pushes back to the last valid spot |
| `BORDER` | warning plus push back, death after 5 seconds |
| `NO_BUILD` | no placing or breaking |
| `SAFE` | lifts the global rules, e.g. for a decorative pond |

On top of that there are global rules for water, lava, fire, void, fall damage, suffocation, cactus and a height limit. All of them are configurable and can be lifted with a `SAFE` zone.

A death in a zone has no killer. If the murderer drowns, the innocents win normally, and the bow of a detective who died that way still drops.

---

## Blocked commands

While a player is inside an arena or its queue they cannot use commands that break the round or leak roles. Outside a game nothing is blocked.

Namespaces (`/minecraft:tp`) and plugin aliases (`/gm`) are resolved, so nobody slips through sideways. `BLACKLIST` is the default, `WHITELIST` exists for tournaments, and `always-allowed` applies in both modes so you can never lock yourself out of your own game.

---

## Extras

- **Kali**: sacrifice items at an altar to raise your favor, then pray. High favor means a blessing, low favor a curse. Investing gold lowers the risk. 9 blessings, 14 curses, all chances configurable.
- **Mystery potion**: brewing stations sell an unknown potion for 1 gold. Same colour means same effect, but only inside that round. The mapping is rolled fresh every game and lives server side only.
- **Cosmetics**: knife skins, bow skins and death effects, paid with coins. Never gameplay relevant.
- **Stats and levels**: wins, kills, K/D, gold, playtime, coins and leaderboards for NPC holograms.
- **Map voting**, **join signs** and **spectator mode**. Anti camping glow and hidden name tags can be switched on in `config.yml`.

---

## Integrations

All optional and detected at runtime. `/murder hooks` shows what was found and which provider is active.

| Plugin | Used for |
|---|---|
| Citizens, FancyNpcs, ZNPCsPlus | lobby NPCs |
| PlaceholderAPI | `%papi_<placeholder>%` in scoreboard and holograms |
| Vault | paying rewards into the server economy |
| AdvancedReplay | round replays |
| CorpseReborn | corpses at the place of death |

Without an NPC plugin the built-in fallback takes over. It is an armour stand with a skinned head rather than a full player model, install Citizens if you want the real thing.

---

## Support

- **Discord**: [dc.gg/paperstream](https://dc.gg/paperstream)
- **Issues**: [Codeberg](https://codeberg.org/LucasTHCR/MurderMystery/issues)
- **Mail**: support@paperstream.dev
