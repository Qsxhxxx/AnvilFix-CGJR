package com.qsxhxxx.anvilxpfix.mixin;

import com.qsxhxxx.anvilxpfix.AnvilXpFix;
import com.qsxhxxx.anvilxpfix.forge.AnvilXpFixForgeConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ItemStack 拦截
 * 移除累加修理成本：getBaseRepairCost 返回0
 */
@Mixin(ItemStack.class)
public class MixinItemStack {

    /** 移除累加修理成本 */
    @Inject(method = "getBaseRepairCost", at = @At("RETURN"), cancellable = true)
    private void anvilfix$getRepairCost(CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValue();
        if (original > 0
                && AnvilXpFixForgeConfig.removeIncrementalRepairCost()
                && !((ItemStack) (Object) this).is(AnvilXpFix.FORCE_REPAIR_COST_TAG)) {
            cir.setReturnValue(0);
        }
    }
}
