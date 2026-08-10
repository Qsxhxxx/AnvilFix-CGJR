package com.qsxhxxx.anvilxpfix.forge.client;

import com.qsxhxxx.anvilxpfix.AnvilXpFix;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * 客户端初始化（仅客户端加载，服务端不加载此类）
 *
 * 职责：注册模组配置界面（ConfigScreenHandler.ConfigScreenFactory）
 *
 * 隔离原理：@Mod 主类 AnvilXpFixForge 通过 DistExecutor.unsafeRunWhenOn 间接调用 init()，
 * 服务端不会执行外层 lambda，因此本类及 AnvilXpFixConfigScreen（extends Screen）都不会被
 * 服务端 JVM 加载，避免 RuntimeDistCleaner 抛出 "Attempted to load class Screen for
 * invalid dist DEDICATED_SERVER" 异常。
 */
public class AnvilXpFixClientSetup {

    /**
     * 客户端初始化入口
     * 注册 FMLClientSetupEvent 监听器，在事件触发时注册配置界面
     */
    public static void init() {
        FMLJavaModLoadingContext.get().getModEventBus()
                .addListener(AnvilXpFixClientSetup::onClientSetup);
    }

    /**
     * 客户端启动事件回调
     * 通过 enqueueWork 在主线程注册配置界面工厂
     */
    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ModList.get()
                .getModContainerById(AnvilXpFix.MODID)
                .orElseThrow()
                .registerExtensionPoint(
                        ConfigScreenHandler.ConfigScreenFactory.class,
                        () -> new ConfigScreenHandler.ConfigScreenFactory(
                                (mc, parent) -> new AnvilXpFixConfigScreen(parent)
                        )
                ));
    }
}
