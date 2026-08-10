package com.qsxhxxx.anvilxpfix.forge;

import net.minecraft.world.item.enchantment.Enchantment;

/**
 * 超限附魔递减上下文（方案S：统一等级递减 + 属性补偿）
 *
 * 性能优化：合并6个ThreadLocal为1个ThreadLocal<DiminishState>，
 * 减少ThreadLocalMap查找次数（从6次降为1次）。
 *
 * 合并的ThreadLocal：
 * - CURRENT_LEVEL（LevelEntry）→ state.currentLevel
 * - DIMINISH_ACTIVE（Boolean）→ state.diminishActive
 * - ACTIVE_COUNT（Integer）→ state.activeCount
 * - RECURSION_GUARD（Boolean）→ state.itemAttrRecursionGuard
 * - HELPER_RECURSION_GUARD（Boolean）→ state.helperRecursionGuard（从EnchantmentDiminishHelper迁入）
 * - IN_ANVIL（Boolean）→ state.inAnvil（从AnvilFixContext迁入）
 */
public final class EnchantmentDiminishContext {

    private EnchantmentDiminishContext() {
    }

    /**
     * 附魔等级上下文
     * 记录最近一次读等级操作的附魔实例、原始等级、递减后整数等级、递减后小数bonus
     */
    public static final class LevelEntry {
        public final Enchantment enchantment;
        public final int originalLevel;
        public final int diminishedLevel;
        public final float bonusFloat;

        public LevelEntry(Enchantment enchantment, int originalLevel, int diminishedLevel, float bonusFloat) {
            this.enchantment = enchantment;
            this.originalLevel = originalLevel;
            this.diminishedLevel = diminishedLevel;
            this.bonusFloat = bonusFloat;
        }
    }

    /**
     * 统一上下文状态（合并所有ThreadLocal字段）
     * 所有字段通过单个ThreadLocal访问，减少ThreadLocalMap查找
     */
    private static final class DiminishState {
        LevelEntry currentLevel = null;
        boolean diminishActive = false;
        int activeCount = 0;
        boolean itemAttrRecursionGuard = false;
        boolean helperRecursionGuard = false;
        boolean inAnvil = false;
    }

    /** 唯一的ThreadLocal（替代原来的6个） */
    private static final ThreadLocal<DiminishState> STATE =
            ThreadLocal.withInitial(DiminishState::new);

    // ==================== 递减激活/停用 ====================

    /** 激活递减（事件/方法开始时调用，支持嵌套） */
    public static void activate() {
        DiminishState s = STATE.get();
        s.activeCount++;
        s.diminishActive = true;
    }

    /** 停用递减（事件/方法结束时调用，支持嵌套） */
    public static void deactivate() {
        DiminishState s = STATE.get();
        s.activeCount--;
        if (s.activeCount <= 0) {
            s.activeCount = 0;
            s.diminishActive = false;
            s.currentLevel = null;
        }
    }

    /** 检查递减是否激活 */
    public static boolean isActivated() {
        return STATE.get().diminishActive;
    }

    // ==================== 附魔等级上下文 ====================

    /** 设置当前读到的附魔等级上下文（只在DIMINISH_ACTIVE=true时记录） */
    public static void setCurrentLevel(Enchantment enchantment, int originalLevel, int diminishedLevel, float bonusFloat) {
        DiminishState s = STATE.get();
        if (!s.diminishActive) return;
        s.currentLevel = new LevelEntry(enchantment, originalLevel, diminishedLevel, bonusFloat);
    }

    /** 获取当前附魔等级上下文（不清除） */
    public static LevelEntry getCurrentLevel() {
        return STATE.get().currentLevel;
    }

    /** 清除当前附魔等级上下文 */
    public static void consumeCurrentLevel() {
        STATE.get().currentLevel = null;
    }

    // ==================== 递归保护（ItemAttributeModifierEvent） ====================

    public static boolean isRecurseGuard() {
        return STATE.get().itemAttrRecursionGuard;
    }

    public static void setRecurseGuard(boolean value) {
        STATE.get().itemAttrRecursionGuard = value;
    }

    // ==================== 递归保护（Helper递推计算） ====================

    public static boolean isHelperRecursionGuard() {
        return STATE.get().helperRecursionGuard;
    }

    public static void setHelperRecursionGuard(boolean value) {
        STATE.get().helperRecursionGuard = value;
    }

    // ==================== 铁砧上下文（从AnvilFixContext合并） ====================

    public static void enterAnvil() {
        STATE.get().inAnvil = true;
    }

    public static void exitAnvil() {
        STATE.get().inAnvil = false;
    }

    public static boolean isInAnvil() {
        return STATE.get().inAnvil;
    }

    // ==================== 兜底清理 ====================

    /** 强制清空所有状态（兜底清理，防止ThreadLocal残留） */
    public static void clear() {
        STATE.remove();
    }
}
