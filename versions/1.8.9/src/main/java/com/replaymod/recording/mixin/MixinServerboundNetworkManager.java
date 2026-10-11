package com.replaymod.recording.mixin;

import com.replaymod.recording.packet.ServerboundCapture;
import io.netty.channel.Channel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Records every packet the client sends, on the sending thread, when the game sends it. */
@Mixin(NetworkManager.class)
public abstract class MixinServerboundNetworkManager {
    @Shadow
    private Channel channel;

    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At("HEAD"))
    private void replayModServerbound$send(Packet<?> packet, CallbackInfo ci) {
        ServerboundCapture.onSend(channel, packet);
    }

    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;Lio/netty/util/concurrent/GenericFutureListener;[Lio/netty/util/concurrent/GenericFutureListener;)V", at = @At("HEAD"))
    private void replayModServerbound$sendWithListeners(Packet<?> packet,
                                                       GenericFutureListener<? extends Future<? super Void>> listener,
                                                       GenericFutureListener<? extends Future<? super Void>>[] listeners,
                                                       CallbackInfo ci) {
        ServerboundCapture.onSend(channel, packet);
    }
}
