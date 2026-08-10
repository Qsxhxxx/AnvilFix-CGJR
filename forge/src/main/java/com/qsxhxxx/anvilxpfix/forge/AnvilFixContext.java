package com.qsxhxxx.anvilxpfix.forge;

/**
 * 铁砧修复上下文标记
 * 用于区分铁砧合并逻辑和游戏效果计算
 * 铁砧内使用原始附魔等级，游戏效果使用递减等级
 *
 * 性能优化：IN_ANVIL 已合并到 EnchantmentDiminishContext.DiminishState 中，
 * 此处仅做委托，保持API兼容。
 */
public final class AnvilFixContext {
    private AnvilFixContext() {}

    public static void enterAnvil() { EnchantmentDiminishContext.enterAnvil(); }
    public static void exitAnvil() { EnchantmentDiminishContext.exitAnvil(); }
    public static boolean isInAnvil() { return EnchantmentDiminishContext.isInAnvil(); }
}
