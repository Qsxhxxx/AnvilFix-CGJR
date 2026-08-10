package com.qsxhxxx.anvilxpfix.mixin;

import com.qsxhxxx.anvilxpfix.forge.EnchantmentDiminishHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 超限附魔效果递减 - Enchantment 父类
 *
 * 处理未被子类重写的附魔（如水下速掘、精准采集等）
 *
 * 使用 @Inject 在 RETURN 处拦截返回值，调用递推公式计算
 * 递推公式：bonus(n) = bonus(n-1) + (bonus(n-1) - bonus(n-2)) * factor
 *
 * 注意：DamageEnchantment（锋利/亡灵杀手/节肢杀手）、TridentImpalerEnchantment（穿刺）、
 * ProtectionEnchantment（保护类）都重写了对应方法且不调用 super，
 * 所以必须在这些子类中也注入 Mixin，参见 MixinDamageEnchantment 和 MixinProtectionEnchantment
 */
@Mixin(Enchantment.class)
public class MixinEnchantmentHelper {

    /**
     * 拦截 getDamageBonus 返回值，应用递推公式
     */
    @Inject(
            method = "getDamageBonus",
            at = @At("RETURN"),
            cancellable = true
    )
    private void anvilfix$diminishDamageBonus(int level, MobType mobType, CallbackInfoReturnable<Float> cir) {
        float original = cir.getReturnValue();
        Enchantment enchantment = (Enchantment) (Object) this;
        float diminished = EnchantmentDiminishHelper.getDiminishedDamageBonus(enchantment, level, mobType, original);
        if (diminished != original) {
            cir.setReturnValue(diminished);
        }
    }

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
