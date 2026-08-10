package com.qsxhxxx.anvilxpfix.forge.event;

import com.qsxhxxx.anvilxpfix.AnvilXpFix;
import com.qsxhxxx.anvilxpfix.forge.AnvilXpFixForgeConfig;
import com.qsxhxxx.anvilxpfix.forge.EnchantmentDiminishContext;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 超限附魔递减上下文激活处理器
 *
 * 性能优化：
 * 1. shouldActivate() 使用缓存配置（避免每tick读ForgeConfigSpec）
 * 2. PlayerTickEvent START: 仅在状态残留时clear()（正常deactivate后isActivated()=false，跳过clear）
 *    消除每tick无谓的ThreadLocal.remove() + 重新创建DiminishState
 */
@Mod.EventBusSubscriber(modid = AnvilXpFix.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EnchantmentDiminishContextHandler {

    /** 使用缓存配置（避免热路径每次读ForgeConfigSpec） */
    private static boolean shouldActivate() {
        return AnvilXpFixForgeConfig.getCachedKeepOverLevel();
    }

    // ==================== LivingEquipmentChangeEvent ====================

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEquipmentChangeHighest(LivingEquipmentChangeEvent event) {
        if (shouldActivate()) EnchantmentDiminishContext.activate();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEquipmentChangeLowest(LivingEquipmentChangeEvent event) {
        EnchantmentDiminishContext.deactivate();
    }

    // ==================== ItemAttributeModifierEvent ====================

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemAttributeModifierHighest(ItemAttributeModifierEvent event) {
        if (shouldActivate()) EnchantmentDiminishContext.activate();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemAttributeModifierLowest(ItemAttributeModifierEvent event) {
        EnchantmentDiminishContext.deactivate();
    }

    // ==================== LivingHurtEvent ====================

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurtHighest(LivingHurtEvent event) {
        if (shouldActivate()) EnchantmentDiminishContext.activate();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurtLowest(LivingHurtEvent event) {
        EnchantmentDiminishContext.deactivate();
    }

    // ==================== LivingAttackEvent ====================

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttackHighest(LivingAttackEvent event) {
        if (shouldActivate()) EnchantmentDiminishContext.activate();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingAttackLowest(LivingAttackEvent event) {
        EnchantmentDiminishContext.deactivate();
    }

    // ==================== TickEvent.PlayerTickEvent ====================
    // 覆盖：TonsOfEnchants Vitality（player.heal，每tick触发）

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerTickHighest(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START && shouldActivate()) {
            // 仅在状态残留时清理（正常END已deactivate，isActivated()=false时跳过clear）
            // 消除每tick无谓的ThreadLocal.remove() + DiminishState重建
            if (EnchantmentDiminishContext.isActivated()) {
                EnchantmentDiminishContext.clear();
            }
            EnchantmentDiminishContext.activate();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTickLowest(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            EnchantmentDiminishContext.deactivate();
        }
    }
}
