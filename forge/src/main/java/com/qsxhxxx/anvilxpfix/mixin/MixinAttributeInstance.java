package com.qsxhxxx.anvilxpfix.mixin;

import com.qsxhxxx.anvilxpfix.forge.EnchantmentDiminishContext;
import com.qsxhxxx.anvilxpfix.forge.EnchantmentDiminishHelper;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 超限附魔递减 - 拦截 AttributeInstance.addTransientModifier
 *
 * 原理：模组在事件中读取附魔等级后，紧邻调用 addTransientModifier 添加属性修改器。
 * MixinEnchantmentHelperTagLevel/GetLevel 已将读到的等级记录到ThreadLocal，
 * 这里取出等级，若超限则用公式B计算ratio，乘到modifier的amount上。
 *
 * 实现方式：@ModifyArg 修改 addTransientModifier 内部 addModifier 调用的参数
 * - addTransientModifier 内部只调用一次 addModifier（private方法）
 * - 修改 addModifier 的参数为新的 AttributeModifier（amount已递减）
 * - 不需要递归保护，因为 addModifier 是 private 方法，不会再触发本 Mixin
 *
 * 覆盖的模组（基于实际字节码调研）：
 * - cgjrextra 暴击率/暴击伤害（CritAttributeSync）
 * - TonsOfEnchants JuggerNog
 */
@Mixin(AttributeInstance.class)
public class MixinAttributeInstance {

    /**
     * 修改 addTransientModifier 内部 addModifier 调用的参数
     * 若 ThreadLocal 中有超限附魔等级，应用公式B递减 modifier 的 amount
     */
    @ModifyArg(
            method = "addTransientModifier",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;addModifier(Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;)V"),
            index = 0,
            require = 0
    )
    private AttributeModifier anvilfix$diminishTransientModifier(AttributeModifier original) {
        return EnchantmentDiminishHelper.diminishAttributeModifier(original);
    }
}
