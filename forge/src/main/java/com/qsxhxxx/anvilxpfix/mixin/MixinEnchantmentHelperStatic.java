package com.qsxhxxx.anvilxpfix.mixin;

import com.qsxhxxx.anvilxpfix.forge.AnvilXpFixDebugLogger;
import com.qsxhxxx.anvilxpfix.forge.EnchantmentDiminishHelper;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 超限附魔效果递减 - EnchantmentHelper.getDamageBonus 静态方法
 *
 * 原理：1.20.1 Forge 中 EnchantmentHelper.getDamageBonus(ItemStack, MobType) 静态方法
 * 内部调用 enchantment.getDamageBonus(level, mobType, stack) [3参数版本]，
 * 该方法来自 IForgeEnchantment 接口的 default 实现。
 *
 * 其他模组的附魔可能重写 3 参数版本不调用 super，
 * 导致注入 2 参数版本的 MixinEnchantmentHelper/MixinDamageEnchantment 无效。
 *
 * 解决方案：在静态方法上注入 Mixin，重新遍历附魔应用递减，覆盖所有情况。
 * 这是最通用的方案，不管其他模组怎么实现，都会被递减。
 *
 * 与实例方法 Mixin 的协作：
 * - EnchantmentDiminishHelper.calculateDiminishedDamageBonusSum 设置 RECURSION_GUARD
 * - 调用 enchantment.getDamageBonus 时，实例方法 Mixin 检查 RECURSION_GUARD
 * - RECURSION_GUARD 为 true 时直接返回原版值，避免双重递减
 */
@Mixin(EnchantmentHelper.class)
public class MixinEnchantmentHelperStatic {

    /**
     * 拦截 EnchantmentHelper.getDamageBonus 静态方法返回值
     * 重新遍历附魔，对超限附魔应用递减公式
     */
    @Inject(
            method = "getDamageBonus",
            at = @At("RETURN"),
            cancellable = true,
            require = 0
    )
    private static void anvilfix$diminishDamageBonusSum(ItemStack stack, MobType mobType, CallbackInfoReturnable<Float> cir) {
        float original = cir.getReturnValue();
        float diminished = EnchantmentDiminishHelper.calculateDiminishedDamageBonusSum(stack, mobType, original);

        // 调试日志（降频输出，每100次输出1次，输出到logs/qsxhxxx/anvilxpfix.log）
        if (AnvilXpFixDebugLogger.isEnabled())
            AnvilXpFixDebugLogger.log("StaticMixin", "[StaticMixin] stack={}, mobType={}, original={}, diminished={}, changed={}",
                    stack, mobType, original, diminished, (diminished != original));

        if (diminished != original) {
            cir.setReturnValue(diminished);
        }
    }
}
