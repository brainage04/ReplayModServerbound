package com.replaymod.recording.packet;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static com.replaymod.replaystudio.util.Utils.writeInt;

/**
 * The client's side of a recording: every packet the client sends and its input in each client tick, kept in the
 * zip entry {@value #ENTRY} next to {@code recording.tmcpr}. Replay viewers that do not know the entry ignore it.
 *
 * <p>The entry is framed like {@code recording.tmcpr}: {@code [int32 ms][int32 length][record]}, big-endian, with
 * {@code ms} on the same clock as {@code recording.tmcpr}. A record is {@code [u8 kind][body]}:
 * <ul>
 *     <li>{@link #HEADER} (first record): {@code [u8 layout version][VarInt protocol][String generator]}</li>
 *     <li>{@link #PACKET}: {@code [u8 state][VarInt packet id][packet body]}, the packet as the game encodes it for
 *     the wire (before compression and encryption), at the moment the game sends it, from whichever thread sends
 *     it (keep-alive answers come from the network thread)</li>
 *     <li>{@link #INPUT} (end of every client tick that had a {@link #TICK_START}):
 *     {@code [u32 flags][u8 hotbar slot][u8 attack presses][u8 use presses][f32 forward][f32 strafe][f32 yaw]
 *     [f32 pitch][f32 turn yaw][f32 turn pitch]}</li>
 *     <li>{@link #TICK_START} (start of every client tick with a player, empty body): written right after the
 *     recording player's per-tick record in {@code recording.tmcpr}, with the same {@code ms}</li>
 * </ul>
 * Readers skip records of unknown kinds and bytes after the fields they know.
 */
public final class ServerboundStream {
    public static final String ENTRY = "serverbound.tmcpr";
    public static final int LAYOUT_VERSION = 1;

    public static final int HEADER = 0;
    public static final int PACKET = 1;
    public static final int INPUT = 2;
    public static final int TICK_START = 3;

    public static final int STATE_PLAY = 0;
    public static final int STATE_CONFIGURATION = 1;
    public static final int STATE_LOGIN = 2;

    // INPUT flags: key bindings held at the end of the tick ...
    public static final int F_FORWARD = 1;
    public static final int F_BACK = 1 << 1;
    public static final int F_LEFT = 1 << 2;
    public static final int F_RIGHT = 1 << 3;
    public static final int F_JUMP = 1 << 4;
    public static final int F_SNEAK = 1 << 5;
    public static final int F_SPRINT = 1 << 6;
    public static final int F_ATTACK = 1 << 7;
    public static final int F_USE = 1 << 8;
    public static final int F_PICK = 1 << 9;
    // ... and the game's state
    public static final int F_SCREEN = 1 << 10;
    public static final int F_NO_PLAYER = 1 << 11;
    public static final int F_INPUT_JUMP = 1 << 12;
    public static final int F_INPUT_SNEAK = 1 << 13;
    public static final int F_SPRINTING = 1 << 14;
    public static final int F_USING_ITEM = 1 << 15;
    public static final int F_ON_GROUND = 1 << 16;

    private final OutputStream out;

    public ServerboundStream(OutputStream out) {
        this.out = out;
    }

    public void write(int time, byte[] record) throws IOException {
        writeInt(out, time);
        writeInt(out, record.length);
        out.write(record);
    }

    public static byte[] header(int protocol, String generator) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bytes.write(HEADER);
        bytes.write(LAYOUT_VERSION);
        writeVarInt(bytes, protocol);
        byte[] text = generator.getBytes(StandardCharsets.UTF_8);
        writeVarInt(bytes, text.length);
        bytes.write(text, 0, text.length);
        return bytes.toByteArray();
    }

    public static byte[] packet(int state, int packetId, byte[] body, int length) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(length + 7);
        bytes.write(PACKET);
        bytes.write(state);
        writeVarInt(bytes, packetId);
        bytes.write(body, 0, length);
        return bytes.toByteArray();
    }

    public static byte[] tickStart() {
        return new byte[]{TICK_START};
    }

    public static byte[] input(int flags, int slot, int attackPresses, int usePresses, float forward, float strafe,
                               float yaw, float pitch, float turnYaw, float turnPitch) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(36);
        try (DataOutputStream data = new DataOutputStream(bytes)) {
            data.writeByte(INPUT);
            data.writeInt(flags);
            data.writeByte(slot);
            data.writeByte(Math.min(attackPresses, 255));
            data.writeByte(Math.min(usePresses, 255));
            data.writeFloat(forward);
            data.writeFloat(strafe);
            data.writeFloat(yaw);
            data.writeFloat(pitch);
            data.writeFloat(turnYaw);
            data.writeFloat(turnPitch);
        } catch (IOException e) {
            throw new AssertionError(e); // in-memory
        }
        return bytes.toByteArray();
    }

    private static void writeVarInt(ByteArrayOutputStream out, int value) {
        while ((value & ~0x7F) != 0) {
            out.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.write(value);
    }
}
