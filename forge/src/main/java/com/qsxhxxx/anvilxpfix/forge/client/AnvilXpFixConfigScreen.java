package com.qsxhxxx.anvilxpfix.forge.client;

import com.qsxhxxx.anvilxpfix.AnvilXpFix;
import com.qsxhxxx.anvilxpfix.forge.AnvilXpFixDebugLogger;
import com.qsxhxxx.anvilxpfix.forge.AnvilXpFixForgeConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 模组配置界面
 * 布局：左边纯文本配置名，右边修改项；悬停显示说明
 */
public class AnvilXpFixConfigScreen extends Screen {

    private static final Component TITLE = Component.translatable("anvil_xp_fix.config.title");
    private final Screen parent;

    private EditBox levelLimitInput;
    private Button repairCostBtn;
    private EditBox formulaInput;
    private Button keepOverLevelBtn;
    private EditBox diminishFactorInput;
    private Button debugLogBtn;

    // 等级上限帮助说明
    private static final Component LEVEL_LIMIT_HELP = Component.literal(
            "-1=无限制，0~255=自定义上限"
    );

    // 公式帮助说明
    private static final Component FORMULA_HELP = Component.literal(
            "经验消耗公式（-1=原版固定消耗，其他=按公式计算），x=附魔总等级\n" +
            "运算符: (+:加法) (-:减法) (*:乘法) (/:除法) (^:指数，如x^2)\n" +
            "函数: (sqrt:平方根) (ln:自然对数) (log:以10为底的对数) (exp:e的x次方)\n" +
            "常量: (pi:圆周率) (e:自然常数)\n" +
            "示例: x*3/2(线性增长) | x^1.5(幂函数) | sqrt(x)(开方) | ln(x+1)(对数增长) | x^2/4(二次函数)"
    );

    // 超限附魔帮助说明
    private static final Component KEEP_OVER_LEVEL_HELP = Component.literal(
            "开启：铁砧不截断超限附魔等级（如锋利10保留为锋利10）\n" +
            "关闭：原版行为（锋利10截断为锋利5）"
    );

    // 移除累加修理成本帮助说明
    private static final Component REPAIR_COST_HELP = Component.literal(
            "移除累加修理成本\n" +
            "开启：铁砧修复不增加修理成本（无累加惩罚）\n" +
            "关闭：原版行为（每次修复增加修理成本）"
    );

    // 递减系数帮助说明
    private static final Component DIMINISH_FACTOR_HELP = Component.literal(
            "超限附魔效果递减系数（需保留超限附魔等级=开启）\n" +
            "公式B（双曲衰减）：d(n) = base × (1+factor) / (factor + n - maxLevel)\n" +
            "-1=不降低（超限部分完整生效）\n" +
            "0=递减最快（超限部分趋近于0）\n" +
            "1=递减最慢（超限部分接近完整生效）\n" +
            "0.3=默认（推荐平衡值）"
    );

    // 调试日志帮助说明
    private static final Component DEBUG_LOG_HELP = Component.literal(
            "调试日志开关\n" +
            "开启：输出调试日志到 logs/qsxhxxx/anvilxpfix.log\n" +
            "关闭：不输出调试日志（默认）"
    );

    public AnvilXpFixConfigScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int rowHeight = 28;
        int startY = height / 2 - 70;

        // === 第1行：等级上限 ===
        this.levelLimitInput = new EditBox(this.font, cx + 10, startY + 1, 110, 18,
                Component.literal("levelLimit"));
        this.levelLimitInput.setValue(String.valueOf(AnvilXpFixForgeConfig.levelLimit.get()));
        this.levelLimitInput.setMaxLength(4);
        this.levelLimitInput.setResponder(value -> {});
        this.addRenderableWidget(levelLimitInput);

        // === 第2行：移除累加修理成本 ===
        boolean repairVal = AnvilXpFixForgeConfig.removeRepairCost.get();
        this.repairCostBtn = Button.builder(
                Component.literal(repairVal ? "开启" : "关闭"),
                b -> toggleRepairCost()
        ).bounds(cx + 10, startY + rowHeight, 60, 20).build();
        addRenderableWidget(repairCostBtn);

        // === 第3行：经验公式 ===
        this.formulaInput = new EditBox(this.font, cx + 10, startY + rowHeight * 2 + 1, 110, 18,
                Component.literal("xpFormula"));
        this.formulaInput.setValue(AnvilXpFixForgeConfig.xpFormula.get());
        this.formulaInput.setMaxLength(64);
        this.formulaInput.setResponder(value -> {});
        this.addRenderableWidget(formulaInput);

        // === 第4行：保留超限附魔等级 ===
        boolean keepOverLevel = AnvilXpFixForgeConfig.keepOverLevelEnchantment.get();
        this.keepOverLevelBtn = Button.builder(
                Component.literal(keepOverLevel ? "开启" : "关闭"),
                b -> toggleKeepOverLevel()
        ).bounds(cx + 10, startY + rowHeight * 3, 60, 20).build();
        addRenderableWidget(keepOverLevelBtn);

        // === 第5行：超限附魔效果递减系数 ===
        this.diminishFactorInput = new EditBox(this.font, cx + 10, startY + rowHeight * 4 + 1, 110, 18,
                Component.literal("overLevelDiminishFactor"));
        this.diminishFactorInput.setValue(String.valueOf(AnvilXpFixForgeConfig.overLevelDiminishFactor.get()));
        this.diminishFactorInput.setMaxLength(6);
        this.diminishFactorInput.setResponder(value -> {});
        this.addRenderableWidget(diminishFactorInput);

        // === 第6行：调试日志开关 ===
        boolean debugVal = AnvilXpFixForgeConfig.debugLogEnabled.get();
        this.debugLogBtn = Button.builder(
                Component.literal(debugVal ? "开启" : "关闭"),
                b -> toggleDebugLog()
        ).bounds(cx + 10, startY + rowHeight * 5, 60, 20).build();
        addRenderableWidget(debugLogBtn);

        // === 底部：保存按钮 ===
        addRenderableWidget(Button.builder(
                Component.literal("保存并退出"),
                b -> saveAndClose()
        ).bounds(cx - 50, startY + rowHeight * 6 + 15, 100, 20).build());

        this.setInitialFocus(levelLimitInput);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(this.font, TITLE, width / 2, height / 2 - 115, 0xFFFFFF);

        int cx = width / 2;
        int rowHeight = 28;
        int startY = height / 2 - 70;

        // 左边：纯文本配置名
        graphics.drawString(this.font, "等级上限", cx - 120, startY + 7, 0xFFFFFF, false);
        graphics.drawString(this.font, "移除累加修理成本", cx - 120, startY + rowHeight + 7, 0xFFFFFF, false);
        graphics.drawString(this.font, "经验消耗公式", cx - 120, startY + rowHeight * 2 + 7, 0xFFFFFF, false);
        graphics.drawString(this.font, "保留超限附魔等级", cx - 120, startY + rowHeight * 3 + 7, 0xFFFFFF, false);
        graphics.drawString(this.font, "超限效果递减系数", cx - 120, startY + rowHeight * 4 + 7, 0xFFFFFF, false);
        graphics.drawString(this.font, "调试日志", cx - 120, startY + rowHeight * 5 + 7, 0xFFFFFF, false);

        // 鼠标悬停显示tooltip
        int formulaY = startY + rowHeight * 2;
        int keepOverLevelY = startY + rowHeight * 3;
        int diminishY = startY + rowHeight * 4;
        int debugLogY = startY + rowHeight * 5;
        int repairCostY = startY + rowHeight;
        if (mouseX >= cx - 120 && mouseX <= cx + 120 && mouseY >= startY && mouseY <= startY + 19) {
            graphics.renderTooltip(this.font, LEVEL_LIMIT_HELP, mouseX, mouseY);
        }
        if (mouseX >= cx - 120 && mouseX <= cx + 120 && mouseY >= repairCostY && mouseY <= repairCostY + 19) {
            graphics.renderTooltip(this.font, REPAIR_COST_HELP, mouseX, mouseY);
        }
        if (mouseX >= cx - 120 && mouseX <= cx + 120 && mouseY >= formulaY && mouseY <= formulaY + 19) {
            graphics.renderTooltip(this.font, FORMULA_HELP, mouseX, mouseY);
        }
        if (mouseX >= cx - 120 && mouseX <= cx + 120 && mouseY >= keepOverLevelY && mouseY <= keepOverLevelY + 19) {
            graphics.renderTooltip(this.font, KEEP_OVER_LEVEL_HELP, mouseX, mouseY);
        }
        if (mouseX >= cx - 120 && mouseX <= cx + 120 && mouseY >= diminishY && mouseY <= diminishY + 19) {
            graphics.renderTooltip(this.font, DIMINISH_FACTOR_HELP, mouseX, mouseY);
        }
        if (mouseX >= cx - 120 && mouseX <= cx + 120 && mouseY >= debugLogY && mouseY <= debugLogY + 19) {
            graphics.renderTooltip(this.font, DEBUG_LOG_HELP, mouseX, mouseY);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** 切换修理成本开关 */
    private void toggleRepairCost() {
        boolean current = AnvilXpFixForgeConfig.removeRepairCost.get();
        AnvilXpFixForgeConfig.removeRepairCost.set(!current);
        repairCostBtn.setMessage(Component.literal(!current ? "开启" : "关闭"));
    }

    /** 切换保留超限附魔等级开关 */
    private void toggleKeepOverLevel() {
        boolean current = AnvilXpFixForgeConfig.keepOverLevelEnchantment.get();
        AnvilXpFixForgeConfig.keepOverLevelEnchantment.set(!current);
        keepOverLevelBtn.setMessage(Component.literal(!current ? "开启" : "关闭"));
    }

    /** 切换调试日志开关 */
    private void toggleDebugLog() {
        boolean current = AnvilXpFixForgeConfig.debugLogEnabled.get();
        AnvilXpFixForgeConfig.debugLogEnabled.set(!current);
        debugLogBtn.setMessage(Component.literal(!current ? "开启" : "关闭"));
        // 关闭调试日志时清空计数器，确保下次开启从0开始降频
        if (current) {
            AnvilXpFixDebugLogger.clearCounters();
        }
        // 刷新缓存开关（set()后立即生效，不依赖ModConfigEvent.Reloading触发时机）
        AnvilXpFixDebugLogger.updateEnabledCache();
    }

    /** 保存配置并关闭 */
    private void saveAndClose() {
        try {
            String levelStr = levelLimitInput.getValue().trim().isEmpty() ? "-1" : levelLimitInput.getValue().trim();
            int val = Integer.parseInt(levelStr);
            if (val > 255) val = 255;
            if (val < -1) val = -1;
            AnvilXpFixForgeConfig.levelLimit.set(val);
        } catch (NumberFormatException e) { /* 保持原值 */ }

        String newFormula = formulaInput.getValue().trim();
        if (!newFormula.isEmpty()) {
            try {
                new com.qsxhxxx.anvilxpfix.util.XpFormulaParser(newFormula).evaluate(10);
                AnvilXpFixForgeConfig.xpFormula.set(newFormula);
            } catch (IllegalArgumentException e) { /* 公式非法不保存 */ }
        }

        try {
            String factorStr = diminishFactorInput.getValue().trim();
            if (!factorStr.isEmpty()) {
                double factor = Double.parseDouble(factorStr);
                factor = Math.max(-1.0, Math.min(1.0, factor));
                AnvilXpFixForgeConfig.overLevelDiminishFactor.set(factor);
            }
        } catch (NumberFormatException e) { /* 保持原值 */ }

        // 刷新热路径缓存（配置值变更后立即同步，不依赖ModConfigEvent.Reloading触发时机）
        AnvilXpFixForgeConfig.refreshCache();
        AnvilXpFixDebugLogger.updateEnabledCache();

        onClose();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
