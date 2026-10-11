package com.replaymod.recording.mixin;

import com.replaymod.recording.packet.ServerboundCapture;
import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Counts the attack and use key presses that reach the game (each one is consumed in the tick it arrives). */
@Mixin(KeyBinding.class)
public abstract class MixinServerboundKeyBinding {
    @Inject(method = "onTick", at = @At("HEAD"))
    private static void replayModServerbound$press(int keyCode, CallbackInfo ci) {
        ServerboundCapture.onKeyPress(keyCode);
    }
}
