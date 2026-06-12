package qsxhxxx.anvilfix;

import com.teamresourceful.resourcefulconfig.common.annotations.Comment;
import com.teamresourceful.resourcefulconfig.common.annotations.Config;
import com.teamresourceful.resourcefulconfig.common.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.common.annotations.IntRange;
import com.teamresourceful.resourcefulconfig.common.config.EntryType;
import com.teamresourceful.resourcefulconfig.web.annotations.Link;
import com.teamresourceful.resourcefulconfig.web.annotations.WebInfo;
import qsxhxxx.anvilfix.util.XpFormulaParser;

@WebInfo(
        title = "铁砧经验改革-CGJR",
        description = "为CGJR使用的铁砧经验逻辑与无昂贵"
)
@Config(AnvilFix.MODID)
public final class AnvilFixConfig {

    @IntRange(min = -1, max = 255)
    @ConfigEntry(id = "level_limit", type = EntryType.INTEGER, translation = "config.anvilfix_cgjr.level_limit")
    public static int levelLimit = -1;

    @ConfigEntry(id = "remove_incremental_repair_cost", type = EntryType.BOOLEAN, translation = "config.anvilfix_cgjr.remove_incremental_repair_cost")
    public static boolean removeIncrementalRepairCost = true;

    /** 经验公式（x=附魔总等级），默认 x*3/2 */
    @Comment("附魔等级部分加成：x = 2个物品附魔总等级\n运算符: (+:加法) (-:减法) (*:乘法) (/:除法) (^:指数，如x^2)\n函数: (sqrt:平方根) (ln:自然对数) (log:以10为底的对数) (exp:e的x次方)\n常量: (pi:圆周率) (e:自然常数)\n示例: x*3/2(线性增长) | x^1.5(幂函数) | sqrt(x)(开方) | ln(x+1)(对数增长) | x^2/4(二次函数)")
    @ConfigEntry(id = "xp_formula", type = EntryType.STRING, translation = "config.anvilfix_cgjr.xp_formula")
    public static String xpFormula = XpFormulaParser.DEFAULT_FORMULA;

    /**
     * 根据当前公式计算经验加成（每次实时解析，确保配置变更立即生效）
     * @param totalLevel 附魔总等级
     * @return 经验加成值
     */
    public static int evaluateXpBonus(int totalLevel) {
        try {
            return new XpFormulaParser(xpFormula).evaluate(totalLevel);
        } catch (IllegalArgumentException e) {
            // 公式非法时回退到默认值
            return new XpFormulaParser(XpFormulaParser.DEFAULT_FORMULA).evaluate(totalLevel);
        }
    }

    public static int getLevelLimit() {
        return AnvilFixConfig.levelLimit >= 0 ? (AnvilFixConfig.levelLimit + 1) : Integer.MAX_VALUE;
    }
}
