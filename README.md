# ReplayModServerbound

ReplayModServerbound is a client-only fork of [ReplayMod](https://github.com/ReplayMod/ReplayMod) 2.6.20 for Minecraft 1.8.9 Forge that adds outgoing packet and per-tick input recording.

Based on: ReplayMod 2.6.20 (upstream tag `Release 2.6.20`, commit `50cfcbab`).
Not affiliated with the ReplayMod project. Please do not ask the ReplayMod developers for help with this fork.

## Features

On Minecraft 1.8.9, recordings also hold outgoing play/login packets and the client's input in an extra zip entry, `serverbound.tmcpr`. Everything else in a recording is what ReplayMod 2.6.20 writes, so stock ReplayMod opens, plays and renders these recordings and ignores the extra entry. Other Minecraft versions are not changed.

Chat messages that are login or password commands (`/login`, `/l`, `/log`, `/register`, `/reg`, `/changepassword`,
`/changepass`, `/unregister`, `/unreg`, `/email`, also with a `plugin:` prefix) are recorded as the command name only.

## Getting started

1. Install Minecraft 1.8.9 with Forge 1.8.9.
2. Download `replaymod-1.8.9-2.6.20-serverbound.1.jar` from the [v1.0.0 release](https://github.com/brainage04/ReplayModServerbound/releases/tag/v1.0.0).
3. Put it in the instance's `.minecraft/mods` folder. If `replaymod-1.8.9-2.6.20.jar` (the normal ReplayMod) is in there, remove it:
   only one ReplayMod jar may be installed. Settings and existing recordings stay where they are.
4. Start the game and record as usual (ReplayMod records automatically when you join a world or server).

Recordings are saved as `.mcpr` files in `.minecraft/replay_recordings/` (the unprocessed originals ReplayMod keeps after
cutting or splitting are in `.minecraft/replay_recordings/raw/`). They work with or without this fork: stock ReplayMod
and the replay tools open any of them. Recordings made without this fork simply lack the player's own inputs (the packets
the client sent and the keys held each tick).

## Commands

No additional commands or keybinds; use ReplayMod's existing controls.

## Configuration

No additional configuration.

## Compatibility

When ReplayMod's recording controls cut or split a recording (pause, split markers), the processed output files do
not get the entry; the unprocessed original that ReplayMod keeps in `replay_recordings/raw` has it. Edits made in the
replay editor (trim, connect) also drop it.

## Documentation

- [docs/SERVERBOUND.md](docs/SERVERBOUND.md) - the fork's record layout.
- [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) - building the Forge 1.8.9 fork.

### Licence

The ReplayMod is provided under the terms of the GNU General Public License Version 3 or (at your option) any later version.
See [LICENSE.md](LICENSE.md) for the full license text.
This fork uses the same licence. ReplayStudio (`libs/ReplayStudio`) is under the GNU LGPL v3, jGui under the MIT licence.
This repository is the corresponding source; clone it with `git clone --recurse-submodules` to include upstream ReplayStudio, jGui and Translations at the recorded commits.
If you redistribute the JAR, follow the GPL v3 source-distribution requirements, retain the licence notices, and add no further restrictions.
