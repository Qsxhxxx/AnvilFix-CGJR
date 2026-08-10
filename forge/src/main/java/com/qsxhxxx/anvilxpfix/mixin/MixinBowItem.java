package com.qsxhxxx.anvilxpfix.mixin;

import com.qsxhxxx.anvilxpfix.forge.AnvilXpFixDebugLogger;
import com.qsxhxxx.anvilxpfix.forge.AnvilXpFixForgeConfig;
import com.qsxhxxx.anvilxpfix.forge.EnchantmentDiminishContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 弹射物附魔递减 - 弓释放
 *
 * 在弓释放箭矢时激活递减标志，覆盖力量/冲击/火矢/无限附魔的等级读取。
 * 这些附魔在BowItem.releaseUsing中通过getItemEnchantmentLevel读取，
 * 最终经过getTagEnchantmentLevel，被MixinEnchantmentHelperTagLevel递减。
 *
 * 覆盖的附魔：
 * - INFINITY（无限）：偏移24-31，事件前读取，决定是否消耗箭矢
 * - POWER（力量）：偏移283，决定箭矢伤害加成
 * - PUNCH（冲击）：偏移319，决定击退力度
 * - FLAMING（火矢）：偏移340，点燃目标
 *
 * 注意：HEAD激活/TAIL清除配对，EnchantmentDiminishContext用计数器支持嵌套
 */
@Mixin(net.minecraft.world.item.BowItem.class)
public class MixinBowItem {

    /** 弓释放时激活递减（在附魔读取之前） */
    @Inject(method = "releaseUsing", at = @At("HEAD"), require = 0)
    private void anvilfix$activateDiminish(ItemStack stack, Level level, LivingEntity entity, int count, CallbackInfo ci) {
        boolean keepOverLevel = AnvilXpFixForgeConfig.getCachedKeepOverLevel();
        if (AnvilXpFixDebugLogger.isEnabled())
            AnvilXpFixDebugLogger.log("BowItem","[MixinBowItem] releaseUsing HEAD: keepOverLevel={}, DIMINISH_ACTIVE before={}",
                    keepOverLevel, EnchantmentDiminishContext.isActivated());
        if (keepOverLevel) {
            EnchantmentDiminishContext.activate();
            if (AnvilXpFixDebugLogger.isEnabled())
                AnvilXpFixDebugLogger.log("BowItem","[MixinBowItem] releaseUsing HEAD: activated, DIMINISH_ACTIVE after={}",
                        EnchantmentDiminishContext.isActivated());
        }
    }

    /** 弓释放后清除递减 */
    @Inject(method = "releaseUsing", at = @At("TAIL"), require = 0)
    private void anvilfix$deactivateDiminish(ItemStack stack, Level level, LivingEntity entity, int count, CallbackInfo ci) {
        boolean keepOverLevel = AnvilXpFixForgeConfig.getCachedKeepOverLevel();
        if (AnvilXpFixDebugLogger.isEnabled())
            AnvilXpFixDebugLogger.log("BowItem","[MixinBowItem] releaseUsing TAIL: keepOverLevel={}, DIMINISH_ACTIVE before={}",
                    keepOverLevel, EnchantmentDiminishContext.isActivated());
        if (keepOverLevel) {
            EnchantmentDiminishContext.deactivate();
            if (AnvilXpFixDebugLogger.isEnabled())
                AnvilXpFixDebugLogger.log("BowItem","[MixinBowItem] releaseUsing TAIL: deactivated, DIMINISH_ACTIVE after={}",
                        EnchantmentDiminishContext.isActivated());
        }
    }
}
