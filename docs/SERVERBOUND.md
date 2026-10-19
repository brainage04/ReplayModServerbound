# Serverbound recording (Forge 1.8.9)

`serverbound.tmcpr` is framed like `recording.tmcpr`: `[int32 ms][int32 length][record]`, big-endian, `ms` on the
same clock as `recording.tmcpr`. A record is `[u8 kind][body]`
(`src/main/java/com/replaymod/recording/packet/ServerboundStream.java`):

- `0 header` - first record: `[u8 layout version = 1][VarInt protocol][VarInt length + UTF-8 generator]`.
- `1 packet` - outgoing play/login packet: `[u8 state: 0 play, 2 login][VarInt packet id][packet body as encoded for the wire, before compression]`.
- `2 input` - end of every client tick that had a tick start: `[u32 flags][u8 hotbar slot][u8 attack presses][u8 use presses][f32 forward][f32 strafe][f32 yaw][f32 pitch][f32 turn yaw][f32 turn pitch]`.
- `3 tick start` - start of every client tick with a player: empty; written right after ReplayMod's per-tick record of the recording player, with the same `ms`.

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
