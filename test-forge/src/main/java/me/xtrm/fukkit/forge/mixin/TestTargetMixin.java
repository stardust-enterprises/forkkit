package me.xtrm.fukkit.forge.mixin;

import me.xtrm.fukkit.bukkit.TestTarget;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author xtrm
 */
@Mixin(TestTarget.class)
@Pseudo
public class TestTargetMixin {
    @Inject(
            method = "isMixinPatched",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    @Dynamic
    private static void isMixinPatched(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
}
