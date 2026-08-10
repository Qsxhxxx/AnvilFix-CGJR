package com.qsxhxxx.anvilxpfix.forge;

import com.qsxhxxx.anvilxpfix.util.XpFormulaParser;
import net.minecraftforge.common.ForgeConfigSpec;

/** Forge配置 - 使用ForgeConfigSpec */
public class AnvilXpFixForgeConfig {

    public static final ForgeConfigSpec SPEC;
    public static ForgeConfigSpec.IntValue levelLimit;
    public static ForgeConfigSpec.BooleanValue removeRepairCost;
    public static ForgeConfigSpec.ConfigValue<String> xpFormula;
    public static ForgeConfigSpec.BooleanValue keepOverLevelEnchantment;
    public static ForgeConfigSpec.DoubleValue overLevelDiminishFactor;
    public static ForgeConfigSpec.BooleanValue debugLogEnabled;

    /** 缓存的公式解析器（避免每次铁砧操作都创建新对象） */
    private static XpFormulaParser cachedParser;
    private static String cachedFormula;

    // === 热路径配置缓存（volatile，配置变更时通过refreshCache更新） ===
    private static volatile boolean cachedKeepOverLevel = true;
    private static volatile double cachedDiminishFactor = 0.3;

    /** 刷新热路径配置缓存（配置加载/变更时调用） */
    public static void refreshCache() {
        try {
            cachedKeepOverLevel = keepOverLevelEnchantment.get();
            cachedDiminishFactor = overLevelDiminishFactor.get();
        } catch (Exception e) {
            // 配置未加载时保持默认值
        }
    }

    public static boolean getCachedKeepOverLevel() { return cachedKeepOverLevel; }
    public static double getCachedDiminishFactor() { return cachedDiminishFactor; }

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("general");

        levelLimit = builder.comment("铁砧等级上限（-1=无限制，0~255=自定义上限，始终生效）")
                .defineInRange("levelLimit", -1, -1, 255);
        removeRepairCost = builder.comment("移除累加修理成本（true=无累加惩罚，false=原版累加行为）")
                .define("removeIncrementalRepairCost", true);
        xpFormula = builder.comment(
                        "经验消耗公式（-1=原版固定消耗，其他=按公式计算cost），x=附魔总等级\n" +
                        "运算符: (+:加法) (-:减法) (*:乘法) (/:除法) (^:指数，如x^2)\n" +
                        "函数: (sqrt:平方根) (ln:自然对数) (log:以10为底的对数) (exp:e的x次方)\n" +
                        "常量: (pi:圆周率) (e:自然常数)\n" +
                        "示例: x*3/2(线性增长) | x^1.5(幂函数) | sqrt(x)(开方) | ln(x+1)(对数增长) | x^2/4(二次函数)"
                )
                .define("xpFormula", XpFormulaParser.DEFAULT_FORMULA);
        keepOverLevelEnchantment = builder.comment("保留超限附魔等级（true=铁砧不截断超限附魔等级，如锋利10保留为锋利10；false=原版截断到上限）")
                .define("keepOverLevelEnchantment", true);
        overLevelDiminishFactor = builder.comment(
                        "超限附魔递减系数（需开启保留超限）\n" +
                        "公式B（双曲衰减）：d(n) = base × (1+factor) / (factor + n - maxLevel)\n" +
                        "-1=不降低（超限部分完整生效），0=递减最快，1=递减最慢，0.3=默认\n" +
                        "factor越大，超限加成越高（递减越慢）"
                )
                .defineInRange("overLevelDiminishFactor", 0.3, -1.0, 1.0);
        debugLogEnabled = builder.comment("调试日志开关（true=输出调试日志到logs/qsxhxxx/anvilxpfix.log，false=关闭）")
                .define("debugLogEnabled", false);

        builder.pop();
        SPEC = builder.build();
    }

    public static int getLevelLimit() {
        int limit = levelLimit.get();
        return limit >= 0 ? limit : Integer.MAX_VALUE;
    }

    public static boolean removeIncrementalRepairCost() { return removeRepairCost.get(); }

    /**
     * 计算经验加成
     * @param totalLevel 附魔总等级
     * @return 公式计算结果；xpFormula为"-1"时返回0（不叠加，保留原版基础cost）
     */
    public static int evaluateXpBonus(int totalLevel) {
        String formula = xpFormula.get();
        if ("-1".equals(formula)) return 0;
        try {
            return getCachedParser(formula).evaluate(totalLevel);
        } catch (IllegalArgumentException e) {
            return new XpFormulaParser(XpFormulaParser.DEFAULT_FORMULA).evaluate(totalLevel);
        }
    }

    /** 获取缓存的公式解析器（线程安全，公式变化时更新缓存） */
    private static synchronized XpFormulaParser getCachedParser(String formula) {
        if (!formula.equals(cachedFormula)) {
            cachedParser = new XpFormulaParser(formula);
            cachedFormula = formula;
        }
        return cachedParser;
    }
}
