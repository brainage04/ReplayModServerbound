package com.replaymod.recording.packet;

import com.replaymod.recording.ReplayModRecording;
import com.replaymod.recording.handler.ConnectionEventHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.util.MovementInput;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static com.replaymod.recording.packet.ServerboundStream.*;

/**
 * Feeds the recording's {@link ServerboundStream}: the packets the client sends (from the network manager's send
 * methods) and the input of each client tick (key bindings, the movement input the tick used, key presses and mouse
 * turns), written at the end of the tick.
 */
public final class ServerboundCapture {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Minecraft mc = Minecraft.getMinecraft();

    // Main thread only: what happened since the last tick ended.
    private static int attackPresses;
    private static int usePresses;
    private static float turnYaw;
    private static float turnPitch;
    private static float yawBeforeTurn;
    private static float pitchBeforeTurn;

    private ServerboundCapture() {
    }

    private static PacketListener recording() {
        ReplayModRecording module = ReplayModRecording.instance;
        if (module == null) return null;
        ConnectionEventHandler handler = module.getConnectionEventHandler();
        return handler == null ? null : handler.getPacketListener();
    }

    /** A packet the game sends on the connection that owns {@code channel}; any thread. */
    public static void onSend(Channel channel, Packet<?> packet) {
        PacketListener listener = recording();
        if (listener == null || channel == null || listener.getChannel() != channel) return;
        if (packet instanceof FMLProxyPacket) {
            packet = ((FMLProxyPacket) packet).toC17Packet();
        } else if (packet instanceof C01PacketChatMessage) {
            packet = withoutSecrets((C01PacketChatMessage) packet);
        }
        EnumConnectionState state = EnumConnectionState.getFromPacket(packet);
        int stateId;
        if (state == EnumConnectionState.PLAY) {
            stateId = STATE_PLAY;
        } else if (state == EnumConnectionState.LOGIN) {
            stateId = STATE_LOGIN;
        } else {
            return;
        }
        Integer packetId = state.getPacketId(EnumPacketDirection.SERVERBOUND, packet);
        if (packetId == null) return;
        ByteBuf buf = Unpooled.buffer();
        try {
            packet.writePacketData(new PacketBuffer(buf));
            int length = buf.readableBytes();
            byte[] body = new byte[length];
            buf.readBytes(body);
            listener.saveServerboundPacket(stateId, packetId, body, length);
        } catch (Exception e) {
            LOGGER.error("Recording serverbound packet " + packet.getClass().getName() + ":", e);
        } finally {
            buf.release();
        }
    }

    private static final Set<String> SECRET_COMMANDS = new HashSet<>(Arrays.asList(
            "login", "l", "log", "register", "reg", "changepassword", "changepass", "unregister", "unreg", "email"));

    /** A login or password command keeps only its name. */
    private static C01PacketChatMessage withoutSecrets(C01PacketChatMessage packet) {
        String message = packet.getMessage();
        if (!message.startsWith("/")) return packet;
        int end = message.indexOf(' ');
        String command = end < 0 ? message.substring(1) : message.substring(1, end);
        int namespace = command.indexOf(':');
        String name = (namespace < 0 ? command : command.substring(namespace + 1)).toLowerCase(Locale.ROOT);
        return SECRET_COMMANDS.contains(name) ? new C01PacketChatMessage("/" + command) : packet;
    }

    /** A key or mouse button went down and reached the game's key bindings (not a screen). */
    public static void onKeyPress(int keyCode) {
        if (keyCode == 0) return;
        GameSettings settings = mc.gameSettings;
        if (keyCode == settings.keyBindAttack.getKeyCode()) attackPresses++;
        if (keyCode == settings.keyBindUseItem.getKeyCode()) usePresses++;
    }

    public static void beforeTurn(Entity entity) {
        if (entity != mc.thePlayer) return;
        yawBeforeTurn = entity.rotationYaw;
        pitchBeforeTurn = entity.rotationPitch;
    }

    /** A mouse turn of the player, as applied (pitch clamped). */
    public static void afterTurn(Entity entity) {
        if (entity != mc.thePlayer) return;
        turnYaw += entity.rotationYaw - yawBeforeTurn;
        turnPitch += entity.rotationPitch - pitchBeforeTurn;
    }

    public static void onTickEnd() {
        int attack = attackPresses;
        int use = usePresses;
        float dYaw = turnYaw;
        float dPitch = turnPitch;
        attackPresses = 0;
        usePresses = 0;
        turnYaw = 0;
        turnPitch = 0;

        PacketListener listener = recording();
        if (listener == null) return;

        GameSettings settings = mc.gameSettings;
        int flags = 0;
        flags |= held(settings.keyBindForward, F_FORWARD);
        flags |= held(settings.keyBindBack, F_BACK);
        flags |= held(settings.keyBindLeft, F_LEFT);
        flags |= held(settings.keyBindRight, F_RIGHT);
        flags |= held(settings.keyBindJump, F_JUMP);
        flags |= held(settings.keyBindSneak, F_SNEAK);
        flags |= held(settings.keyBindSprint, F_SPRINT);
        flags |= held(settings.keyBindAttack, F_ATTACK);
        flags |= held(settings.keyBindUseItem, F_USE);
        flags |= held(settings.keyBindPickBlock, F_PICK);
        if (mc.currentScreen != null) flags |= F_SCREEN;

        int slot = 0;
        float forward = 0, strafe = 0, yaw = 0, pitch = 0;
        EntityPlayerSP player = mc.thePlayer;
        if (player == null) {
            flags |= F_NO_PLAYER;
        } else {
            MovementInput input = player.movementInput;
            if (input != null) {
                forward = input.moveForward;
                strafe = input.moveStrafe;
                if (input.jump) flags |= F_INPUT_JUMP;
                if (input.sneak) flags |= F_INPUT_SNEAK;
            }
            if (player.isSprinting()) flags |= F_SPRINTING;
            if (player.isUsingItem()) flags |= F_USING_ITEM;
            if (player.onGround) flags |= F_ON_GROUND;
            slot = player.inventory.currentItem;
            yaw = player.rotationYaw;
            pitch = player.rotationPitch;
        }
        listener.saveServerboundInput(ServerboundStream.input(flags, slot, attack, use, forward, strafe, yaw, pitch, dYaw, dPitch));
    }

    private static int held(KeyBinding binding, int flag) {
        return binding.isKeyDown() ? flag : 0;
    }
}
