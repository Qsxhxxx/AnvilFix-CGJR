package com.qsxhxxx.anvilxpfix.mixin;

import com.qsxhxxx.anvilxpfix.forge.AnvilXpFixForgeConfig;
import com.qsxhxxx.anvilxpfix.forge.EnchantmentDiminishContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 弹射物附魔递减 - 弩射箭/装弹/装填
 *
 * 拦截三个static方法，覆盖弩的所有附魔等级读取：
 *
 * 1. tryLoadProjectiles（private static）：
 *    读取MULTISHOT（多重射击），决定装弹1支或3支
 *
 * 2. getChargeDuration（public static）：
 *    读取QUICK_CHARGE（快速装填），装填时间 = 25 - 5*等级
 *
 * 3. performShooting（public static）：
 *    PIERCING（穿透）在getArrow中读取（performShooting调用getArrow）
 *
 * 注意：MULTISHOT和QUICK_CHARGE在装弹/装填时读取，不在射弹时读取，
 * 所以需要分别拦截。所有方法都是static，Mixin注入方法也必须是static。
 */
@Mixin(net.minecraft.world.item.CrossbowItem.class)
public class MixinCrossbowItem {

    // ===== tryLoadProjectiles（MULTISHOT - 装弹时读取）=====

    @Inject(method = "tryLoadProjectiles", at = @At("HEAD"), require = 0)
    private static void anvilfix$activateTryLoad(LivingEntity entity, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (AnvilXpFixForgeConfig.getCachedKeepOverLevel()) {
            EnchantmentDiminishContext.activate();
        }
    }

    @Inject(method = "tryLoadProjectiles", at = @At("TAIL"), require = 0)
    private static void anvilfix$deactivateTryLoad(LivingEntity entity, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (AnvilXpFixForgeConfig.getCachedKeepOverLevel()) {
            EnchantmentDiminishContext.deactivate();
        }
    }

    // ===== getChargeDuration（QUICK_CHARGE - 装填速度）=====

    @Inject(method = "getChargeDuration", at = @At("HEAD"), require = 0)
    private static void anvilfix$activateChargeDuration(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (AnvilXpFixForgeConfig.getCachedKeepOverLevel()) {
            EnchantmentDiminishContext.activate();
        }
    }

    @Inject(method = "getChargeDuration", at = @At("TAIL"), require = 0)
    private static void anvilfix$deactivateChargeDuration(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (AnvilXpFixForgeConfig.getCachedKeepOverLevel()) {
            EnchantmentDiminishContext.deactivate();
        }
    }

    // ===== performShooting（PIERCING - 射弹时在getArrow中读取）=====

    @Inject(method = "performShooting", at = @At("HEAD"), require = 0)
    private static void anvilfix$activatePerformShooting(Level level, LivingEntity entity, InteractionHand hand,
                                                          ItemStack stack, float velocity, float inaccuracy,
                                                          CallbackInfo ci) {
        if (AnvilXpFixForgeConfig.getCachedKeepOverLevel()) {
            EnchantmentDiminishContext.activate();
        }
    }

    @Inject(method = "performShooting", at = @At("TAIL"), require = 0)
    private static void anvilfix$deactivatePerformShooting(Level level, LivingEntity entity, InteractionHand hand,
                                                            ItemStack stack, float velocity, float inaccuracy,
                                                            CallbackInfo ci) {
        if (AnvilXpFixForgeConfig.getCachedKeepOverLevel()) {
            EnchantmentDiminishContext.deactivate();
        }
    }
}
