package com.replaymod.recording.mixin;

import com.replaymod.recording.packet.ServerboundCapture;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sums the player's mouse turns (applied every frame) between the ends of two ticks. */
@Mixin(Entity.class)
public abstract class MixinServerboundEntity {
    @Inject(method = "setAngles", at = @At("HEAD"))
    private void replayModServerbound$beforeTurn(float yaw, float pitch, CallbackInfo ci) {
        ServerboundCapture.beforeTurn((Entity) (Object) this);
    }

    @Inject(method = "setAngles", at = @At("RETURN"))
    private void replayModServerbound$afterTurn(float yaw, float pitch, CallbackInfo ci) {
        ServerboundCapture.afterTurn((Entity) (Object) this);
    }
}
