package com.replaymod.recording.mixin;

import com.replaymod.recording.packet.ServerboundCapture;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Writes the input of each client tick when the tick ends. */
@Mixin(Minecraft.class)
public abstract class MixinServerboundMinecraft {
    @Inject(method = "runTick", at = @At("RETURN"))
    private void replayModServerbound$tickEnd(CallbackInfo ci) {
        ServerboundCapture.onTickEnd();
    }
}
