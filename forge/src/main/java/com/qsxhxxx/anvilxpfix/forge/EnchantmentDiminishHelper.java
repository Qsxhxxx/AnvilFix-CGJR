package com.qsxhxxx.anvilxpfix.forge;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.Map;

/**
 * 超限附魔效果递减计算工具类
 *
 * 公式B（双曲衰减）：d(n) = base × (1+factor) / (factor + n - maxLevel)
 * 其中 base = bonus(maxLevel) - bonus(maxLevel-1)，n > maxLevel
 * bonus(n) = bonus(maxLevel) + Σ d(k) for k=maxLevel+1 to n
 *
 * 性能优化：
 * 1. HELPER_RECURSION_GUARD 迁移到 EnchantmentDiminishContext.DiminishState（减少ThreadLocal数量）
 * 2. 所有 log() 调用前加 isEnabled() 短路检查（避免参数预构造开销）
 * 3. 热路径配置读取改用缓存（getCachedKeepOverLevel/getCachedDiminishFactor）
 */
public final class EnchantmentDiminishHelper {

    private EnchantmentDiminishHelper() {
    }

    /**
     * 计算递减后的伤害加成（getDamageBonus）
     */
    public static float getDiminishedDamageBonus(Enchantment enchantment, int level, MobType mobType, float originalBonus) {
        if (AnvilXpFixDebugLogger.isEnabled())
            AnvilXpFixDebugLogger.log("DamageBonus", "[Helper-DamageBonus] enter: enchantment={}, class={}, level={}, maxLevel={}, originalBonus={}",
                    enchantment, enchantment.getClass().getName(), level, enchantment.getMaxLevel(), originalBonus);

        // 递归保护：递推计算时直接返回原版结果
        if (EnchantmentDiminishContext.isHelperRecursionGuard()) {
            if (AnvilXpFixDebugLogger.isEnabled())
                AnvilXpFixDebugLogger.log("DamageBonus", "[Helper-DamageBonus] HELPER_RECURSION_GUARD=true, return original={}", originalBonus);
            return originalBonus;
        }

        // 铁砧合并期间使用原始值
        if (EnchantmentDiminishContext.isInAnvil()) {
            if (AnvilXpFixDebugLogger.isEnabled())
                AnvilXpFixDebugLogger.log("DamageBonus", "[Helper-DamageBonus] isInAnvil=true, return original={}", originalBonus);
            return originalBonus;
        }

        // 未开启保留超限附魔时不处理（使用缓存）
        if (!AnvilXpFixForgeConfig.getCachedKeepOverLevel()) {
            if (AnvilXpFixDebugLogger.isEnabled())
                AnvilXpFixDebugLogger.log("DamageBonus", "[Helper-DamageBonus] keepOverLevelEnchantment=false, return original={}", originalBonus);
            return originalBonus;
        }

        // 事件期间等级已被 MixinEnchantmentHelperTagLevel 递减，不再二次递减
        if (EnchantmentDiminishContext.isActivated()) {
            return originalBonus;
        }

        int maxLevel = enchantment.getMaxLevel();
        // 不超限直接返回原版结果
        if (level <= maxLevel) {
            if (AnvilXpFixDebugLogger.isEnabled())
                AnvilXpFixDebugLogger.log("DamageBonus", "[Helper-DamageBonus] level({}) <= maxLevel({}), return original={}", level, maxLevel, originalBonus);
            return originalBonus;
        }

        double factor = AnvilXpFixForgeConfig.getCachedDiminishFactor();
        // 防御性检查：factor < 0 时不处理（配置错误保护）
        if (factor < 0) {
            if (AnvilXpFixDebugLogger.isEnabled())
                AnvilXpFixDebugLogger.log("DamageBonus", "[Helper-DamageBonus] factor={} < 0, return original={}", factor, originalBonus);
            return originalBonus;
        }

        // 反比递减计算
        EnchantmentDiminishContext.setHelperRecursionGuard(true);
        try {
            float bonusMaxLevel = getBonusAt(enchantment, maxLevel, mobType);
            float bonusPrevLevel = getBonusAt(enchantment, maxLevel - 1, mobType);
            float base = bonusMaxLevel - bonusPrevLevel;

            if (AnvilXpFixDebugLogger.isEnabled())
                AnvilXpFixDebugLogger.log("DamageBonus", "[Helper-DamageBonus] base values: bonusMaxLevel={}, bonusPrevLevel={}, base={}, factor={}",
                        bonusMaxLevel, bonusPrevLevel, base, factor);

            if (base == 0) {
                if (AnvilXpFixDebugLogger.isEnabled())
                    AnvilXpFixDebugLogger.log("DamageBonus", "[Helper-DamageBonus] base=0, skip formula, return original={}", originalBonus);
                return originalBonus;
            }

            float factorF = (float) factor;
            float bonus = bonusMaxLevel;
            for (int k = maxLevel + 1; k <= level; k++) {
                float d = base * (1.0f + factorF) / (factorF + (k - maxLevel));
                bonus += d;
            }

            float result = Math.min(originalBonus, Math.max(0, bonus));
            if (AnvilXpFixDebugLogger.isEnabled())
                AnvilXpFixDebugLogger.log("DamageBonus", "[Helper-DamageBonus] FINAL: level={}, originalBonus={}, diminished={}, changed={}",
                        level, originalBonus, result, (result != originalBonus));
            return result;
        } finally {
            EnchantmentDiminishContext.setHelperRecursionGuard(false);
        }
    }

    /**
     * 计算递减后的保护加成（getDamageProtection）
     */
    public static int getDiminishedDamageProtection(Enchantment enchantment, int level, DamageSource source, int originalProtection) {
        if (EnchantmentDiminishContext.isHelperRecursionGuard()) return originalProtection;
        if (EnchantmentDiminishContext.isInAnvil()) return originalProtection;
        if (!AnvilXpFixForgeConfig.getCachedKeepOverLevel()) return originalProtection;
        if (EnchantmentDiminishContext.isActivated()) return originalProtection;

        int maxLevel = enchantment.getMaxLevel();
        if (level <= maxLevel) return originalProtection;

        double factor = AnvilXpFixForgeConfig.getCachedDiminishFactor();
        if (factor < 0) return originalProtection;

        EnchantmentDiminishContext.setHelperRecursionGuard(true);
        try {
            float bonusMaxLevel = getProtectionAt(enchantment, maxLevel, source);
            float bonusPrevLevel = getProtectionAt(enchantment, maxLevel - 1, source);
            float base = bonusMaxLevel - bonusPrevLevel;

            if (base == 0) return originalProtection;

            float factorF = (float) factor;
            float bonus = bonusMaxLevel;
            for (int k = maxLevel + 1; k <= level; k++) {
                float d = base * (1.0f + factorF) / (factorF + (k - maxLevel));
                bonus += d;
            }

            float result = Math.min(originalProtection, Math.max(0, bonus));
            return Math.round(result);
        } finally {
            EnchantmentDiminishContext.setHelperRecursionGuard(false);
        }
    }

    private static float getBonusAt(Enchantment enchantment, int level, MobType mobType) {
        if (level < 0) return 0;
        return enchantment.getDamageBonus(level, mobType);
    }

    private static float getProtectionAt(Enchantment enchantment, int level, DamageSource source) {
        if (level < 0) return 0;
        return enchantment.getDamageProtection(level, source);
    }

    /**
     * 计算物品上所有附魔的递减后伤害加成总和
     * 用于 EnchantmentHelper.getDamageBonus 静态方法的 Mixin
     */
    public static float calculateDiminishedDamageBonusSum(ItemStack stack, MobType mobType, float originalSum) {
        if (EnchantmentDiminishContext.isHelperRecursionGuard()) return originalSum;
        if (EnchantmentDiminishContext.isInAnvil()) return originalSum;
        if (!AnvilXpFixForgeConfig.getCachedKeepOverLevel()) return originalSum;

        if (stack == null || stack.isEmpty()) return originalSum;

        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);

        // 快速检查：没有超限附魔直接返回原版值
        boolean hasOverLevel = false;
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            if (entry.getValue() > entry.getKey().getMaxLevel()) {
                hasOverLevel = true;
                break;
            }
        }
        if (!hasOverLevel) return originalSum;

        EnchantmentDiminishContext.setHelperRecursionGuard(true);
        try {
            float diminishedSum = 0;
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                Enchantment enchantment = entry.getKey();
                int level = entry.getValue();
                int maxLevel = enchantment.getMaxLevel();

                float originalBonus = enchantment.getDamageBonus(level, mobType, stack);

                if (level > maxLevel && maxLevel > 0) {
                    float diminishedBonus = applyDiminishFormula(enchantment, level, maxLevel, mobType, stack, originalBonus);
                    diminishedSum += diminishedBonus;

                    if (AnvilXpFixDebugLogger.isEnabled())
                        AnvilXpFixDebugLogger.log("Sum", "[Helper-Sum] over-level: enchantment={}, level={}, maxLevel={}, originalBonus={}, diminished={}",
                                enchantment, level, maxLevel, originalBonus, diminishedBonus);
                } else {
                    diminishedSum += originalBonus;
                }
            }

            float result = Math.min(originalSum, Math.max(0, diminishedSum));
            if (AnvilXpFixDebugLogger.isEnabled())
                AnvilXpFixDebugLogger.log("Sum", "[Helper-Sum] FINAL: originalSum={}, diminishedSum={}, result={}",
                        originalSum, diminishedSum, result);
            return result;
        } finally {
            EnchantmentDiminishContext.setHelperRecursionGuard(false);
        }
    }

    /**
     * 应用公式B（双曲衰减）递减计算单个超限附魔的伤害加成
     */
    private static float applyDiminishFormula(Enchantment enchantment, int level, int maxLevel,
                                               MobType mobType, ItemStack stack, float originalBonus) {
        double factor = AnvilXpFixForgeConfig.getCachedDiminishFactor();
        if (factor < 0) return originalBonus;

        float bonusMaxLevel = enchantment.getDamageBonus(maxLevel, mobType, stack);
        float bonusPrevLevel = maxLevel > 1 ? enchantment.getDamageBonus(maxLevel - 1, mobType, stack) : 0;
        float base = bonusMaxLevel - bonusPrevLevel;

        if (base == 0) return originalBonus;

        float factorF = (float) factor;
        float bonus = bonusMaxLevel;
        for (int k = maxLevel + 1; k <= level; k++) {
            float d = base * (1.0f + factorF) / (factorF + (k - maxLevel));
            bonus += d;
        }

        return Math.min(originalBonus, Math.max(0, bonus));
    }

    // ==================== 等级递减核心方法（方案S） ====================

    /**
     * 计算递减后的等级小数值 bonus(n)
     */
    public static float calculateDiminishedLevelFloat(Enchantment enchantment, int level) {
        int maxLevel = enchantment.getMaxLevel();
        if (level <= maxLevel) return level;
        if (maxLevel <= 0) return level;

        double factor = AnvilXpFixForgeConfig.getCachedDiminishFactor();
        if (factor < 0) return level;

        float factorF = (float) factor;
        float bonus = maxLevel;
        for (int j = maxLevel + 1; j <= level; j++) {
            bonus += (1.0f + factorF) / (factorF + (j - maxLevel));
        }

        return Math.min(level, Math.max(maxLevel, bonus));
    }

    /**
     * 计算递减后的整数等级 = floor(bonus(n))
     */
    public static int calculateDiminishedLevel(Enchantment enchantment, int level) {
        int maxLevel = enchantment.getMaxLevel();
        if (level <= maxLevel) return level;
        if (maxLevel <= 0) return level;

        float bonusFloat = calculateDiminishedLevelFloat(enchantment, level);
        int diminishedLevel = (int) Math.floor(bonusFloat);

        return Math.max(maxLevel, diminishedLevel);
    }

    // ==================== 属性补偿（方案S） ====================

    /**
     * 对属性修改器应用补偿（方案S：等级递减 + 属性补偿恢复小数精度）
     */
    public static AttributeModifier diminishAttributeModifier(AttributeModifier original) {
        EnchantmentDiminishContext.LevelEntry entry = EnchantmentDiminishContext.getCurrentLevel();
        if (entry == null) {
            return original;
        }

        EnchantmentDiminishContext.consumeCurrentLevel();

        Enchantment enchantment = entry.enchantment;
        int originalLevel = entry.originalLevel;
        int diminishedLevel = entry.diminishedLevel;
        float bonusFloat = entry.bonusFloat;
        int maxLevel = enchantment.getMaxLevel();

        if (originalLevel <= maxLevel) {
            if (AnvilXpFixDebugLogger.isEnabled())
                AnvilXpFixDebugLogger.log("AttrModifier", "[Helper-AttrModifier] not over-level: enchantment={}, originalLevel={}, maxLevel={}",
                        enchantment, originalLevel, maxLevel);
            return original;
        }

        if (diminishedLevel <= 0) {
            if (AnvilXpFixDebugLogger.isEnabled())
                AnvilXpFixDebugLogger.log("AttrModifier", "[Helper-AttrModifier] diminishedLevel<=0, skip: enchantment={}, diminishedLevel={}",
                        enchantment, diminishedLevel);
            return original;
        }

        float compensationRatio = bonusFloat / diminishedLevel;
        if (compensationRatio > 1.5f || compensationRatio < 0.5f) {
            if (AnvilXpFixDebugLogger.isEnabled())
                AnvilXpFixDebugLogger.log("AttrModifier", "[Helper-AttrModifier] abnormal compensationRatio, skip: enchantment={}, bonusFloat={}, diminishedLevel={}, ratio={}",
                        enchantment, bonusFloat, diminishedLevel, compensationRatio);
            return original;
        }

        double newAmount = original.getAmount() * compensationRatio;
        AttributeModifier newModifier = new AttributeModifier(
                original.getId(), original.getName(), newAmount, original.getOperation());

        if (AnvilXpFixDebugLogger.isEnabled())
            AnvilXpFixDebugLogger.log("AttrModifier", "[Helper-AttrModifier] compensated: enchantment={}, originalLevel={}, maxLevel={}, diminishedLevel={}, bonusFloat={}, compensationRatio={}, originalAmount={}, newAmount={}",
                    enchantment, originalLevel, maxLevel, diminishedLevel, bonusFloat, compensationRatio, original.getAmount(), newAmount);

        return newModifier;
    }
}
