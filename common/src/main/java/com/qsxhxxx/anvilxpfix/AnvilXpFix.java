package com.qsxhxxx.anvilxpfix;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 铁砧经验修复模组 - 常量定义
 */
public class AnvilXpFix {

    public static final String MODID = "anvil_xp_fix";
    /** 强制保留累加修理成本的物品标签 */
    public static final TagKey<Item> FORCE_REPAIR_COST_TAG =
            TagKey.create(Registries.ITEM, new ResourceLocation(MODID, "force_incremental_repair_cost"));
}
