package com.qsxhxxx.anvilxpfix.mixin;

import com.qsxhxxx.anvilxpfix.forge.AnvilXpFixDebugLogger;
import com.qsxhxxx.anvilxpfix.forge.EnchantmentDiminishHelper;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.enchantment.DamageEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.TridentImpalerEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 超限附魔效果递减 - 伤害类附魔子类
 *
 * 处理重写了 getDamageBonus 且不调用 super 的子类：
 * - DamageEnchantment：锋利、亡灵杀手、节肢杀手
 * - TridentImpalerEnchantment：穿刺（三叉戟）
 *
 * Java 方法重写机制：实例方法调用基于实际类型，子类重写的方法不经过父类
 * 因此必须在这些子类中也注入 Mixin
 *
 * 使用 @Inject 在 RETURN 处拦截返回值，调用递推公式计算
 * 递推公式：bonus(n) = bonus(n-1) + (bonus(n-1) - bonus(n-2)) * factor
 */
@Mixin({
        DamageEnchantment.class,
        TridentImpalerEnchantment.class
})
public class MixinDamageEnchantment {

    /**
     * 拦截 getDamageBonus 返回值，应用递推公式
     */
    @Inject(
            method = "getDamageBonus",
            at = @At("RETURN"),
            cancellable = true,
            require = 0
    )
    private void anvilfix$diminishDamageBonus(int level, MobType mobType, CallbackInfoReturnable<Float> cir) {
        float original = cir.getReturnValue();
        Enchantment enchantment = (Enchantment) (Object) this;
        // 调试日志：输出调用信息和附魔实际类型（降频输出，每100次输出1次）
        if (AnvilXpFixDebugLogger.isEnabled())
            AnvilXpFixDebugLogger.log("DamageEnchantment", "[MixinDamageEnchantment] class={}, enchantment={}, level={}, mobType={}, originalBonus={}",
                    enchantment.getClass().getName(),
                    enchantment,
                    level,
                    mobType,
                    original);
        float diminished = EnchantmentDiminishHelper.getDiminishedDamageBonus(enchantment, level, mobType, original);
        if (AnvilXpFixDebugLogger.isEnabled())
            AnvilXpFixDebugLogger.log("DamageEnchantment", "[MixinDamageEnchantment] result: original={}, diminished={}, changed={}",
                    original, diminished, (diminished != original));
        if (diminished != original) {
            cir.setReturnValue(diminished);
        }
    }
}
