package com.qsxhxxx.anvilxpfix.mixin;

import com.qsxhxxx.anvilxpfix.forge.EnchantmentDiminishHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 超限附魔效果递减 - 保护类附魔子类
 *
 * 处理重写了 getDamageProtection 且不调用 super 的子类：
 * - ProtectionEnchantment：保护、火焰保护、摔落缓冲、爆炸保护、弹射物保护
 *
 * Java 方法重写机制：实例方法调用基于实际类型，子类重写的方法不经过父类
 * 因此必须在这些子类中也注入 Mixin
 *
 * 使用 @Inject 在 RETURN 处拦截返回值，调用递推公式计算
 * 递推公式：bonus(n) = bonus(n-1) + (bonus(n-1) - bonus(n-2)) * factor
 */
@Mixin(ProtectionEnchantment.class)
public class MixinProtectionEnchantment {

    /**
     * 拦截 getDamageProtection 返回值，应用递推公式
     */
    @Inject(
            method = "getDamageProtection",
            at = @At("RETURN"),
            cancellable = true
    )
    private void anvilfix$diminishDamageProtection(int level, DamageSource source, CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValue();
        Enchantment enchantment = (Enchantment) (Object) this;
        int diminished = EnchantmentDiminishHelper.getDiminishedDamageProtection(enchantment, level, source, original);
        if (diminished != original) {
            cir.setReturnValue(diminished);
        }
    }
}
