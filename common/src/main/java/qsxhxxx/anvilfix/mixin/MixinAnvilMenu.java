package qsxhxxx.anvilfix.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qsxhxxx.anvilfix.AnvilFixConfig;

/**
 * 铁砧自定义经验消耗逻辑：
 * 经验 = cost=0时的原版基础消耗 + 公式(附魔总等级)
 */
@Mixin(net.minecraft.world.inventory.AnvilMenu.class)
public class MixinAnvilMenu {

    @Shadow @Final private net.minecraft.world.inventory.DataSlot cost;

    /** 缓存当前操作的经验加成值 */
    private int anvilfix$enchantBonus = 0;

    /** 在 createResult 开头计算并缓存经验加成 */
    @Inject(method = "createResult", at = @At("HEAD"))
    private void anvilfix$cacheEnchantBonus(CallbackInfo ci) {
        java.util.List<?> slots = ((net.minecraft.world.inventory.AbstractContainerMenu)(Object)this).slots;
        ItemStack left = ((net.minecraft.world.inventory.Slot)slots.get(0)).getItem();
        ItemStack right = ((net.minecraft.world.inventory.Slot)slots.get(1)).getItem();

        if (!left.isEmpty()) {
            int totalLevel = getEnchantLevel(left) + getEnchantLevel(right);
            this.anvilfix$enchantBonus = AnvilFixConfig.evaluateXpBonus(totalLevel);
        } else {
            this.anvilfix$enchantBonus = 0;
        }
    }

    /** 等级上限检查 */
    @ModifyExpressionValue(
        method = "createResult",
        at = @At(value = "CONSTANT", args = "intValue=40", ordinal = 2)
    )
    private int anvilfix$modifyLevelLimit(int original) {
        return AnvilFixConfig.getLevelLimit();
    }

    /** 在 createResult 结束后，将附魔加成叠加到最终经验值上 */
    @Inject(method = "createResult", at = @At("RETURN"))
    private void anvilfix$applyXpBonus(CallbackInfo ci) {
        if (this.anvilfix$enchantBonus > 0) {
            this.cost.set(this.cost.get() + this.anvilfix$enchantBonus);
        }
    }

    /** 计算物品的总附魔等级 */
    private static int getEnchantLevel(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        return EnchantmentHelper.getEnchantments(stack).values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }
}
