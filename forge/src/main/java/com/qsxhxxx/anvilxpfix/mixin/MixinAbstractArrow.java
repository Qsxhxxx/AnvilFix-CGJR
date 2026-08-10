package com.qsxhxxx.anvilxpfix.mixin;

import com.qsxhxxx.anvilxpfix.forge.AnvilXpFixDebugLogger;
import com.qsxhxxx.anvilxpfix.forge.AnvilXpFixForgeConfig;
import com.qsxhxxx.anvilxpfix.forge.EnchantmentDiminishContext;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 弹射物附魔递减 - 骷髅/Mob射箭
 *
 * 在骷髅等Mob射箭时激活递减标志，覆盖力量/冲击/火矢附魔的等级读取。
 *
 * 这些附魔在AbstractArrow.setEnchantmentEffectsFromEntity中通过
 * getEnchantmentLevel(Enchantment, LivingEntity)读取（基于实体装备），
 * 最终经过getTagEnchantmentLevel，被MixinEnchantmentHelperTagLevel递减。
 *
 * 覆盖的附魔：
 * - POWER（力量）：偏移0-7，baseDamage += level * 0.5 + 0.5
 * - PUNCH（冲击）：偏移8-15，setKnockback(level)
 * - FLAMING（火矢）：偏移87-91，setSecondsOnFire(100)
 *
 * 注意：玩家用弓走BowItem.releaseUsing路径，不走此方法。
 * 此方法仅用于Mob射箭（如骷髅弓箭手）。
 */
@Mixin(net.minecraft.world.entity.projectile.AbstractArrow.class)
public class MixinAbstractArrow {

    /** Mob射箭时激活递减（在附魔读取之前） */
    @Inject(method = "setEnchantmentEffectsFromEntity", at = @At("HEAD"), require = 0)
    private void anvilfix$activateDiminish(LivingEntity entity, float baseDamage, CallbackInfo ci) {
        boolean keepOverLevel = AnvilXpFixForgeConfig.getCachedKeepOverLevel();
        if (AnvilXpFixDebugLogger.isEnabled())
            AnvilXpFixDebugLogger.log("AbstractArrow", "[MixinAbstractArrow] setEnchantmentEffectsFromEntity HEAD: keepOverLevel={}, DIMINISH_ACTIVE before={}",
                    keepOverLevel, EnchantmentDiminishContext.isActivated());
        if (keepOverLevel) {
            EnchantmentDiminishContext.activate();
            if (AnvilXpFixDebugLogger.isEnabled())
                AnvilXpFixDebugLogger.log("AbstractArrow", "[MixinAbstractArrow] setEnchantmentEffectsFromEntity HEAD: activated, DIMINISH_ACTIVE after={}",
                        EnchantmentDiminishContext.isActivated());
        }
    }

    /** Mob射箭后清除递减 */
    @Inject(method = "setEnchantmentEffectsFromEntity", at = @At("TAIL"), require = 0)
    private void anvilfix$deactivateDiminish(LivingEntity entity, float baseDamage, CallbackInfo ci) {
        boolean keepOverLevel = AnvilXpFixForgeConfig.getCachedKeepOverLevel();
        if (AnvilXpFixDebugLogger.isEnabled())
            AnvilXpFixDebugLogger.log("AbstractArrow", "[MixinAbstractArrow] setEnchantmentEffectsFromEntity TAIL: keepOverLevel={}, DIMINISH_ACTIVE before={}",
                    keepOverLevel, EnchantmentDiminishContext.isActivated());
        if (keepOverLevel) {
            EnchantmentDiminishContext.deactivate();
            if (AnvilXpFixDebugLogger.isEnabled())
                AnvilXpFixDebugLogger.log("AbstractArrow", "[MixinAbstractArrow] setEnchantmentEffectsFromEntity TAIL: deactivated, DIMINISH_ACTIVE after={}",
                        EnchantmentDiminishContext.isActivated());
        }
    }
}
