package com.qsxhxxx.anvilxpfix.mixin;

import com.qsxhxxx.anvilxpfix.forge.AnvilXpFixDebugLogger;
import com.qsxhxxx.anvilxpfix.forge.EnchantmentDiminishContext;
import com.qsxhxxx.anvilxpfix.forge.EnchantmentDiminishHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 超限附魔递减 - 读等级入口点（方案S核心）
 *
 * 拦截 EnchantmentHelper.getTagEnchantmentLevel(Enchantment, ItemStack)
 *
 * 方案S行为：
 * - 事件期间（DIMINISH_ACTIVE=true）：
 *   1. 计算递减后整数等级 diminishedLevel = floor(bonus(n))
 *   2. 计算小数 bonusFloat = bonus(n)
 *   3. setReturnValue(diminishedLevel) 修改返回值（事件效果用整数等级）
 *   4. setCurrentLevel(ench, originalLevel, diminishedLevel, bonusFloat) 记录到ThreadLocal（属性补偿用）
 * - 非事件期间：不修改返回值，不记录ThreadLocal（tooltip/铁砧等场景不受影响）
 *
 * 调用链汇聚点（基于实际字节码调研确认）：
 * - 所有读附魔等级的入口最终都汇聚到 getTagEnchantmentLevel
 *   - EnchantmentHelper.getTagEnchantmentLevel（本 Mixin 拦截）
 *   - IForgeItemStack.getEnchantmentLevel → IForgeItem.getEnchantmentLevel → getTagEnchantmentLevel
 *   - 原版 EnchantmentHelper.m_44843_（已@Deprecated）→ stack.getEnchantmentLevel → getTagEnchantmentLevel
 *
 * 覆盖的模组：
 * - cgjrextra 暴击率/暴击伤害/斩杀概率（触发概率被递减）
 * - TonsOfEnchants JuggerNog、Vitality、LifestealArrow、InstaKill（触发概率和基于等级的数值被递减）
 * - MajoSpellEnchantment MaxMana、CDReduction（通过 ItemStack.getEnchantmentLevel）
 *
 * 双重递减避免：
 * - 事件期间 getDiminishedDamageBonus/getDiminishedDamageProtection/calculateDiminishedDamageBonusSum
 *   检查 DIMINISH_ACTIVE=true，直接返回原版值，不再二次递减
 *
 * 注意：getTagEnchantmentLevel 是 Forge patches 添加的方法，不在原版 mappings 中，
 *      必须 remap = false，否则 Mixin 注解处理器找不到 obfuscation mapping 会编译失败
 */
@Mixin(EnchantmentHelper.class)
public class MixinEnchantmentHelperTagLevel {

    /**
     * 在 getTagEnchantmentLevel 返回后：
     * - 事件期间：计算递减后等级，修改返回值，记录到ThreadLocal
     * - 非事件期间：不处理（避免tooltip/铁砧等场景污染ThreadLocal）
     *
     * remap = false：Forge patches 添加的方法，不在原版 mappings 中
     * cancellable = true：需要修改返回值
     */
    @Inject(
            method = "getTagEnchantmentLevel",
            at = @At("RETURN"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private static void anvilfix$recordTagEnchantmentLevel(Enchantment enchantment, ItemStack stack,
                                                            CallbackInfoReturnable<Integer> cir) {
        // 非事件期间不处理（避免tooltip/铁砧等场景污染ThreadLocal）
        if (!EnchantmentDiminishContext.isActivated()) return;

        int originalLevel = cir.getReturnValue();
        int maxLevel = enchantment.getMaxLevel();

        // 非超限：记录原始等级（属性补偿时判断是否超限）
        if (originalLevel <= maxLevel) {
            EnchantmentDiminishContext.setCurrentLevel(enchantment, originalLevel, originalLevel, originalLevel);
            return;
        }

        // 超限：计算递减后小数bonus，再floor得整数等级（只遍历1次公式，避免双重计算）
        // 原实现分别调 calculateDiminishedLevel(内部遍历1次) 和 calculateDiminishedLevelFloat(又遍历1次)，
        // 现只调1次float版本再floor，结果完全等价（maxLevel<=0/level<=maxLevel边界已验证）
        float bonusFloat = EnchantmentDiminishHelper.calculateDiminishedLevelFloat(enchantment, originalLevel);
        int diminishedLevel = Math.max(maxLevel, (int) Math.floor(bonusFloat));

        // 调试日志：超限递减结果（降频输出，每100次输出1次，输出到logs/qsxhxxx/anvilxpfix.log）
        if (AnvilXpFixDebugLogger.isEnabled())
            AnvilXpFixDebugLogger.log("TagLevel","[TagLevel] over-level: enchantment={}, original={}, diminished={}, bonusFloat={}",
                    enchantment, originalLevel, diminishedLevel, bonusFloat);

        // 记录到ThreadLocal（属性补偿用）
        EnchantmentDiminishContext.setCurrentLevel(enchantment, originalLevel, diminishedLevel, bonusFloat);

        // 修改返回值为递减后等级（事件效果用整数等级计算）
        if (diminishedLevel != originalLevel) {
            cir.setReturnValue(diminishedLevel);
        }
    }
}
