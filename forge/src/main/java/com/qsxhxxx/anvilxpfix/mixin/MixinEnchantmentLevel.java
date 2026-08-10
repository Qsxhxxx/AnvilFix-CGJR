package com.qsxhxxx.anvilxpfix.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.qsxhxxx.anvilxpfix.forge.AnvilXpFixForgeConfig;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 保留超限附魔等级
 *
 * 原版逻辑：AnvilMenu.createResult() 中
 * k = Mth.clamp(k, enchantment.getMinLevel(), enchantment.getMaxLevel())
 * 导致锋利10被截断为锋利5
 *
 * 修复：使用@ModifyExpressionValue修改getMaxLevel()的返回值
 * 兼容性：@ModifyExpressionValue允许多模组链式修改，不会与@Redirect冲突
 */
@Mixin(net.minecraft.world.inventory.AnvilMenu.class)
public class MixinEnchantmentLevel {

    @ModifyExpressionValue(
            method = "createResult",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/Enchantment;getMaxLevel()I")
    )
    private int anvilfix$keepOverLevelEnchantment(int maxLevel) {
        if (AnvilXpFixForgeConfig.getCachedKeepOverLevel()) {
            return Integer.MAX_VALUE;
        }
        return maxLevel;
    }
}
