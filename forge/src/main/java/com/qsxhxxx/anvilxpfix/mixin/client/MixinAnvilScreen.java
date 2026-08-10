package com.qsxhxxx.anvilxpfix.mixin.client;

import com.qsxhxxx.anvilxpfix.forge.AnvilXpFixForgeConfig;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** 客户端：修改铁砧界面"过高"警告的显示阈值（始终生效） */
@Mixin(AnvilScreen.class)
public class MixinAnvilScreen {

    @ModifyConstant(method = "renderLabels",
            constant = @Constant(intValue = 40, ordinal = 0),
            require = 0)
    private int anvilfix$modifyDisplayLimit(int original) {
        return AnvilXpFixForgeConfig.getLevelLimit();
    }
}
