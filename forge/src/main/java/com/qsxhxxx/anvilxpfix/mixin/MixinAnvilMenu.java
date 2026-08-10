package com.qsxhxxx.anvilxpfix.mixin;

import com.qsxhxxx.anvilxpfix.forge.AnvilFixContext;
import com.qsxhxxx.anvilxpfix.forge.AnvilXpFixForgeConfig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 铁砧自定义经验消耗逻辑
 *
 * 配置项独立控制：
 * - levelLimit：限制cost上限，-1=不限制（始终生效）
 * - removeIncrementalRepairCost：true=无累加惩罚（MixinItemStack），false=原版累加
 * - xpFormula：-1=固定消耗(恒定值)，其他=按公式(x=附魔等级)计算
 */
@Mixin(net.minecraft.world.inventory.AnvilMenu.class)
public class MixinAnvilMenu {

    @Shadow @Final
    private net.minecraft.world.inventory.DataSlot cost;

    /** 缓存经验加成 */
    private int anvilfix$enchantBonus = 0;

    /** 在 createResult 开头：设置铁砧上下文标记 + 缓存经验加成 */
    @Inject(method = "createResult", at = @At("HEAD"))
    private void anvilfix$cacheEnchantBonus(CallbackInfo ci) {
        AnvilFixContext.enterAnvil(); // 铁砧内使用原始附魔等级

        java.util.List<?> slots = ((net.minecraft.world.inventory.AbstractContainerMenu) (Object) this).slots;
        ItemStack left = ((net.minecraft.world.inventory.Slot) slots.get(0)).getItem();
        ItemStack right = ((net.minecraft.world.inventory.Slot) slots.get(1)).getItem();

        if (!left.isEmpty()) {
            int totalLevel = getEnchantLevel(left) + getEnchantLevel(right);
            this.anvilfix$enchantBonus = AnvilXpFixForgeConfig.evaluateXpBonus(totalLevel);
        } else {
            this.anvilfix$enchantBonus = 0;
        }
    }

    /**
     * 等级上限检查（levelLimit始终生效）
     */
    @ModifyConstant(method = "createResult", constant = @org.spongepowered.asm.mixin.injection.Constant(intValue = 40, ordinal = 2), require = 0)
    private int anvilfix$modifyLevelLimit(int original) {
        return AnvilXpFixForgeConfig.getLevelLimit();
    }

    /**
     * 最终cost处理（levelLimit始终生效）
     * - 开启时：叠加经验公式 + levelLimit截断
     * - 关闭时：仅levelLimit截断（保留原版计算）
     * - 同时清除铁砧上下文（RETURN正常返回时清除，与TAIL双重保险）
     */
    @Inject(method = "createResult", at = @At("RETURN"))
    private void anvilfix$applyFinalCost(CallbackInfo ci) {
        int limit = AnvilXpFixForgeConfig.getLevelLimit();
        int currentCost = this.cost.get();
        if (currentCost > limit) {
            this.cost.set(limit);
            AnvilFixContext.exitAnvil();
            return;
        }
        // 开启时叠加经验公式
        if (this.anvilfix$enchantBonus > 0 && AnvilXpFixForgeConfig.removeIncrementalRepairCost()) {
            int newCost = currentCost + this.anvilfix$enchantBonus;
            this.cost.set(Math.min(newCost, limit));
        }
        // 清除铁砧上下文（exitAnvil多次调用安全，只是set(false)）
        AnvilFixContext.exitAnvil();
    }

    /**
     * 清除铁砧上下文标记（TAIL兜底，与RETURN双重保险）
     * 注意：@At("TAIL")不是finally，方法抛异常时TAIL不执行
     * 但createResult是原版方法不太可能抛异常，RETURN+TAIL双重清除已足够安全
     */
    @Inject(method = "createResult", at = @At("TAIL"))
    private void anvilfix$clearContext(CallbackInfo ci) {
        AnvilFixContext.exitAnvil();
    }

    /** 计算物品的总附魔等级 */
    private static int getEnchantLevel(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        int sum = 0;
        for (int level : EnchantmentHelper.getEnchantments(stack).values()) {
            sum += level;
        }
        return sum;
    }
}
