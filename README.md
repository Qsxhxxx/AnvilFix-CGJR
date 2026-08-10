# Anvil XP Fix

Minecraft 1.20.1 Forge 模组，修复和扩展铁砧机制。

## 功能

### 铁砧经验修复

- **等级上限**：可配置铁砧操作等级上限（默认40，可设-1取消限制）
- **移除累加修理成本**：消除原版每次修复递增的惩罚成本
- **经验消耗公式**：自定义公式计算经验消耗（支持+-*/^运算符、sqrt/ln/log/exp函数、pi/e常量、x变量=附魔总等级）

### 超限附魔保留与递减

- **保留超限附魔等级**：铁砧合并不再截断超出上限的附魔（如锋利10保留为锋利10而非截断为锋利5）
- **双曲衰减递减**：超限附魔在游戏效果上按公式B递减 `d(n) = base × (1+factor) / (factor + n - maxLevel)`，平衡超限附魔的数值
- **属性补偿**：等级递减+属性补偿恢复小数精度，确保递减后效果平滑过渡

### 覆盖范围

递减覆盖所有武器和防具附魔效果：

- 伤害类：锋利/亡灵杀手/节肢杀手/穿刺
- 保护类：保护/火焰保护/摔落缓冲/爆炸保护/弹射物保护
- 弹射物：弓/弩/三叉戟/骷髅射箭的力量/冲击/火矢/无限/多重射击/快速装填/穿透/激流/忠诚/引雷
- 属性修改器：通过ThreadLocal+Mixin拦截addTransientModifier和ItemAttributeModifierEvent
- 第三方模组兼容：cgjrextra/TonsOfEnchants/MajoSpellEnchantment等

## 配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| levelLimit | -1 | 等级上限（-1=无限） |
| removeIncrementalRepairCost | true | 移除累加修理成本 |
| xpFormula | x*3/2 | 经验消耗公式（-1=原版） |
| keepOverLevelEnchantment | true | 保留超限附魔等级 |
| overLevelDiminishFactor | 0.3 | 递减系数（-1=不递减） |
| debugLogEnabled | false | 调试日志 |

## 技术架构

- **方案S**：统一等级递减 + 属性补偿，通过Mixin拦截getTagEnchantmentLevel实现
- **ThreadLocal合并**：6个ThreadLocal合并为1个`DiminishState`，减少ThreadLocalMap查找
- **热路径缓存**：volatile缓存配置值，避免每次铁砧操作读ForgeConfigSpec
- **递归保护**：Helper递推和ItemAttributeModifierEvent双重递归保护
- **铁砧上下文**：区分铁砧合并（用原始等级）和游戏效果（用递减等级）
