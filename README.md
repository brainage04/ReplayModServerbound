# ReplayModServerbound
A **modified version of [ReplayMod](https://github.com/ReplayMod/ReplayMod) 2.6.20** for Minecraft 1.8.9 (Forge 11.15.1.2318).
ReplayMod records game sessions and replays them from any perspective; stock ReplayMod records only what the server
sends to the client. This fork also records what the player's own client sends to the server and the player's input
in every tick, so a recording shows what the player actually pressed.

- Based on: ReplayMod 2.6.20 (upstream tag `Release 2.6.20`, commit `50cfcbab`), unmodified apart from the changes below.
- Changed: one commit on top of upstream (`Record serverbound packets and per-tick input on 1.8.9`): a new recording stream
  (`ServerboundStream`, `ServerboundCapture`), four 1.8.9 mixins that feed it, small hooks in `PacketListener` and
  `RecordingEventHandler`, the mixin config, the version (`2.6.20-serverbound.1`) and this README. Nothing else is changed;
  only the 1.8.9 version records the extra data.
- Licence: GNU GPL v3, as upstream (`LICENSE.md`); see "Licence" below. This repository is the corresponding source.
- Not affiliated with the ReplayMod project. Please do not ask the ReplayMod developers for help with this fork.

## What this fork records (Forge 1.8.9)
On Minecraft 1.8.9 its recordings also hold
everything the client sends to the server, and the client's input in every tick, in an extra zip entry
`serverbound.tmcpr`. Everything else in a recording is what ReplayMod 2.6.20 writes, so stock ReplayMod opens,
plays and renders these recordings and ignores the extra entry. Other Minecraft versions are not changed.

`serverbound.tmcpr` is framed like `recording.tmcpr`: `[int32 ms][int32 length][record]`, big-endian, `ms` on the
same clock as `recording.tmcpr`. A record is `[u8 kind][body]`
(`src/main/java/com/replaymod/recording/packet/ServerboundStream.java`):

| Kind | When | Body |
| --- | --- | --- |
| 0 header | first record | `[u8 layout version = 1][VarInt protocol][VarInt length + UTF-8 generator]` |
| 1 packet | every packet the client sends, when the game sends it (keep-alive answers come from the network thread) | `[u8 state: 0 play, 2 login][VarInt packet id][packet body as encoded for the wire, before compression]` |
| 2 input | end of every client tick that had a tick start | `[u32 flags][u8 hotbar slot][u8 attack presses][u8 use presses][f32 forward][f32 strafe][f32 yaw][f32 pitch][f32 turn yaw][f32 turn pitch]` |
| 3 tick start | start of every client tick with a player | empty; written right after ReplayMod's per-tick record of the recording player, with the same `ms` |

Input flags: bits 0-9 the key bindings held at the end of the tick (forward, back, left, right, jump, sneak, sprint,
attack, use, pick block), 10 a screen is open, 11 no player, 12 and 13 the jump and sneak the tick's movement used,
14 sprinting, 15 using an item, 16 on the ground (all at the end of the tick). `forward` and `strafe` are the movement
input the tick's physics used (after the sneak and item-use slowdowns), so toggle-sprint and toggle-sneak mods are
covered. Attack and use presses count the key presses that reached the game in the tick (presses while using an item
included). `yaw` and `pitch` are the player's rotation at the end of the tick; the turn fields sum the mouse turns
applied since the previous input record (server-set rotations are not turns). Readers skip records of unknown kinds
and bytes after the fields they know.

Packets between the tick start of tick k and its input were sent during tick k; packets between that input and the
next tick start were sent while the client handled server packets between ticks, or from the network thread.

Chat messages that are login or password commands (`/login`, `/l`, `/log`, `/register`, `/reg`, `/changepassword`,
`/changepass`, `/unregister`, `/unreg`, `/email`, also with a `plugin:` prefix) are recorded as the command name only.

When ReplayMod's recording controls cut or split a recording (pause, split markers), the processed output files do
not get the entry; the unprocessed original that ReplayMod keeps in `replay_recordings/raw` has it. Edits made in the
replay editor (trim, connect) also drop it.

### Install (for a friend, or yourself)
1. You need Minecraft 1.8.9 with Forge 1.8.9 (11.15.1.2318), for example a Forge 1.8.9 instance in Prism Launcher or MultiMC.
2. Download `replaymod-1.8.9-2.6.20-serverbound.1.jar` from the [v1.0.0 release](https://github.com/brainage04/ReplayModServerbound/releases/tag/v1.0.0).
3. Put it in the instance's `.minecraft/mods` folder. If `replaymod-1.8.9-2.6.20.jar` (the normal ReplayMod) is in there, remove it:
   only one ReplayMod jar may be installed. Settings and existing recordings stay where they are.
4. Start the game and record as usual (ReplayMod records automatically when you join a world or server).

Recordings are saved as `.mcpr` files in `.minecraft/replay_recordings/` (the unprocessed originals ReplayMod keeps after
cutting or splitting are in `.minecraft/replay_recordings/raw/`). They work with or without this fork: stock ReplayMod
and the replay tools open any of them. Recordings made without this fork simply lack the player's own inputs (the packets
the client sent and the keys held each tick).

### Licence
ReplayMod is free software under the GNU General Public License v3 (LICENSE.md); this fork is too. ReplayStudio
(`libs/ReplayStudio`) is under the GNU LGPL v3, jGui under the MIT licence. This public repository is the complete
corresponding source of the released jar (clone it with `git clone --recurse-submodules`; the submodules are the
unmodified upstream ReplayStudio, jGui and Translations at the commits recorded here); the release also carries a
source archive of the same tree. Whoever passes the jar on to someone else must also give them that source (or this
link), keep the licence notices, and pass it on under the GPL v3 with no further restrictions.

Build the 1.8.9 jar with `./gradlew --no-daemon :1.8.9:bundleJar` (Gradle 8.7 runs on Java 17 or 21; the 1.8.9
sources compile with a Java 8 toolchain, found through `-Porg.gradle.java.installations.paths=<JDK 8 home>`). The jar
is `versions/1.8.9/build/libs/replaymod-1.8.9-<version>.jar`. Building every Minecraft version's sources down the
preprocessor chain takes about 20 minutes the first time.

## Building
Make sure your sub-projects are up-to-date: `git submodule update --init --recursive`

For compiling 1.7.10, you must run `./gradlew :jGui:1.7.10:setupDecompWorkspace :1.7.10:setupDecompWorkspace` once after the initial clone. This may take quite some time.

### No IDE
You can build the mod by running `./gradlew build` (or just `./gradlew bundleJar`). You can then find the final jar files in `versions/$MCVERSION/build/libs/`.
You can also build single versions by running `./gradlew :1.8:build` (or just `./gradlew :1.8:bundleJar`) (builds the MC 1.8 version).

### IntelliJ
Ensure you have at least IDEA 2020.1.
Build the mod via Gradle as explained above at least once (`./gradlew compileJava` should be sufficient). This will ensure that the sources for all MC versions are generated.
Then import the Gradle project from within IDEA: File -> Open -> build.gradle -> Open as Project
Finally configure IDEA to build everything by itself instead of delegating it to Gradle (cause that is slow): File -> Settings -> Build, Execution, Deployment -> Build Tools -> Gradle -> Build and run using: IntelliJ IDEA

### Eclipse

## Development
### Branches
Loosely based on [this branching model](http://nvie.com/posts/a-successful-git-branching-model/) with `stable` instead of `master`.

TL;DR:
Main development happens on the `develop` branch, snapshots are built from this branch.
The `stable` branch contains the most recent release.

The `master` branch is solely to be used for the `version.json` file that contains a list of all versions
used by the clients to check for updates of this mod.

### The Preprocessor
To support multiple Minecraft versions with the ReplayMod, a [JCP](https://github.com/raydac/java-comment-preprocessor)-inspired preprocessor is used.
It has by now acquired a lot more sophisticated features to make it as noninvasive as possible.
Please read the [preprocessor's README](https://github.com/ReplayMod/preprocessor/blob/master/README.md) to understand how it works.

### Versioning
The ReplayMod uses the versioning scheme outlined [here](https://docs.minecraftforge.net/en/1.12.x/conventions/versioning/)
with three changes:
- No `MAJORAPI`, the ReplayMod does not provide any external API
- "Updating to a new Minecraft version" should not increment `MAJORMOD`, we maintain one version of the ReplayMod
for each version of Minecraft and all these versions share the same feature set (and most bugs). We therefore try to
keep the version name the same for all of them (with the exception of `MCVERSION` of course). This also means that the
"Multiple Minecraft Version" section does not apply.
- For pre-releases the shorter `-bX` is used instead of `-betaX`

When a new version is (pre-)release, a new commit modifying the `version.txt` file should be added and the
`versions.json` file in the `master` branch should be updated. To simplify this process the gradle task `doRelease` can
be used: `./gradlew -PreleaseVersion=2.0.0-rc1 doRelease`. It will create the commit and update the version.json
accordingly.

Care should be taken that the updated `version.json` is not pushed before a jar file is available on the
download page (or Jenkins) as it will inform the users of the update.

### Bugs
GitHub should generally be used to report bugs.

In the past, bugs were tracked via [Bugzilla](https://bugs.replaymod.com/), so bug numbers in commits prior to 2020 such as `(fixes #42)` generally referred to Bugzilla unless noted otherwise.

## License
The ReplayMod is provided under the terms of the GNU General Public License Version 3 or (at your option) any later version.
See `LICENSE.md` for the full license text.
