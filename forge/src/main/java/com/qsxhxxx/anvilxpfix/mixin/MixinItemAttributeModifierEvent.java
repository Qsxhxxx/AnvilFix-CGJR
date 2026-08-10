package com.qsxhxxx.anvilxpfix.mixin;

import com.qsxhxxx.anvilxpfix.forge.EnchantmentDiminishContext;
import com.qsxhxxx.anvilxpfix.forge.EnchantmentDiminishHelper;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 超限附魔递减 - 拦截 ItemAttributeModifierEvent.addModifier
 *
 * 原理：模组在事件中读取附魔等级后，紧邻调用 event.addModifier 添加属性修改器。
 * MixinEnchantmentHelperTagLevel/GetLevel 已将读到的等级记录到ThreadLocal，
 * 这里取出等级，若超限则用公式B计算ratio，乘到modifier的amount上。
 *
 * 实现方式：@Inject HEAD cancellable
 * - 检查ThreadLocal，若超限创建新AttributeModifier（amount已递减）
 * - 设置递归保护后调用 addModifier(attribute, newModifier)
 * - 取消原调用
 *
 * 覆盖的模组（基于实际字节码调研）：
 * - MajoSpellEnchantment MaxMana、CDReduction（通过 ItemStack.getEnchantmentLevel）
 *
 * 注意：tooltip 显示也会触发 ItemAttributeModifierEvent，
 * 此时应用递减是合理的（玩家看到的属性值就是实际效果）
 */
@Mixin(value = ItemAttributeModifierEvent.class, remap = false)
public class MixinItemAttributeModifierEvent {

    /**
     * 拦截 addModifier(Attribute, AttributeModifier) 重载
     * 若 ThreadLocal 中有超限附魔等级，应用公式B递减 modifier 的 amount
     */
    @Inject(
            method = "addModifier(Lnet/minecraft/world/entity/ai/attributes/Attribute;Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void anvilfix$diminishAddModifier(Attribute attribute, AttributeModifier modifier,
                                              CallbackInfoReturnable<Boolean> cir) {
        // 递归保护：避免调用 addModifier(newModifier) 时再次进入 Mixin
        if (EnchantmentDiminishContext.isRecurseGuard()) return;

        // 应用递减（内部会检查ThreadLocal、超限、计算ratio）
        AttributeModifier newModifier = EnchantmentDiminishHelper.diminishAttributeModifier(modifier);
        if (newModifier == modifier) return; // 未修改，放行原调用

        // 设置递归保护，调用原方法添加新 modifier
        EnchantmentDiminishContext.setRecurseGuard(true);
        try {
            ((ItemAttributeModifierEvent) (Object) this).addModifier(attribute, newModifier);
        } finally {
            EnchantmentDiminishContext.setRecurseGuard(false);
        }
        // 取消原调用，返回 true（modifier 已添加）
        cir.setReturnValue(Boolean.TRUE);
    }
}
