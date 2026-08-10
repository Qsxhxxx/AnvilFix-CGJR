package com.qsxhxxx.anvilxpfix.mixin;

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
 * 弹射物附魔递减 - 三叉戟投掷
 *
 * 在三叉戟投掷时激活递减标志，覆盖以下附魔的等级读取：
 *
 * - RIPTIDE（激流）：在releaseUsing偏移32-36通过getRiptide读取
 *   影响激流冲刺速度：2.5 + riptide * 0.5
 *
 * - LOYALTY（忠诚）：在ThrownTrident构造函数中读取（releaseUsing中创建ThrownTrident）
 *   读取后存到entityData，之后tick中从entityData读取，返回速度 = 0.05 * loyalty
 *   因为ThrownTrident在releaseUsing执行期间创建，DIMINISH_ACTIVE仍为true，所以LOYALTY也被覆盖
 *
 * 注意：CHANNELING（引雷）在ThrownTrident.onHitEntity中读取（命中时），
 * 那时触发LivingHurtEvent，DIMINISH_ACTIVE已被EnchantmentDiminishContextHandler激活，
 * 所以引雷附魔也被覆盖，不需要在此Mixin中处理。
 */
@Mixin(net.minecraft.world.item.TridentItem.class)
public class MixinTridentItem {

    /** 三叉戟投掷时激活递减（在激流/忠诚读取之前） */
    @Inject(method = "releaseUsing", at = @At("HEAD"), require = 0)
    private void anvilfix$activateDiminish(ItemStack stack, Level level, LivingEntity entity, int count, CallbackInfo ci) {
        if (AnvilXpFixForgeConfig.getCachedKeepOverLevel()) {
            EnchantmentDiminishContext.activate();
        }
    }

    /** 三叉戟投掷后清除递减 */
    @Inject(method = "releaseUsing", at = @At("TAIL"), require = 0)
    private void anvilfix$deactivateDiminish(ItemStack stack, Level level, LivingEntity entity, int count, CallbackInfo ci) {
        if (AnvilXpFixForgeConfig.getCachedKeepOverLevel()) {
            EnchantmentDiminishContext.deactivate();
        }
    }
}
