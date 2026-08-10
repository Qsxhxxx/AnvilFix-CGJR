package com.qsxhxxx.anvilxpfix.forge;

import com.qsxhxxx.anvilxpfix.AnvilXpFix;
import com.qsxhxxx.anvilxpfix.forge.client.AnvilXpFixClientSetup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Forge模组入口
 *
 * 注意：主类禁止直接 import 客户端专用类（如 ConfigScreenHandler、FMLClientSetupEvent、
 * AnvilXpFixConfigScreen 等 Screen 子类），否则 Forge 的 RuntimeDistCleaner 会在
 * 服务端类加载阶段扫描常量池并抛出
 * "Attempted to load class net/minecraft/client/gui/screens/Screen for invalid dist DEDICATED_SERVER" 异常。
 *
 * 客户端初始化通过 DistExecutor.unsafeRunWhenOn 间接调用 AnvilXpFixClientSetup.init()：
 * 双重 lambda 模式（外层 Supplier<Runnable>，内层 Runnable），服务端不会执行 supplier.get()，
 * 因此 AnvilXpFixClientSetup 及 AnvilXpFixConfigScreen 都不会被服务端 JVM 加载。
 */
@Mod(AnvilXpFix.MODID)
public class AnvilXpFixForge {

    public AnvilXpFixForge() {
        // 注册配置
        ModLoadingContext.get().registerConfig(
                ModConfig.Type.COMMON,
                AnvilXpFixForgeConfig.SPEC
        );

        // 初始化调试日志（配置Log4j2输出到单独文件）
        AnvilXpFixDebugLogger.init();

        // 注册配置加载/重载监听器：刷新热路径缓存
        // cachedKeepOverLevel/cachedDiminishFactor/cachedEnabled 必须在配置变更后同步更新，
        // 否则热路径读到过期值（用户改配置不生效、调试日志开关不生效）
        FMLJavaModLoadingContext.get().getModEventBus()
                .addListener(AnvilXpFixForge::onModConfigLoading);
        FMLJavaModLoadingContext.get().getModEventBus()
                .addListener(AnvilXpFixForge::onModConfigReloading);

        // 客户端初始化：通过 DistExecutor 间接调用，服务端不会加载 AnvilXpFixClientSetup
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> AnvilXpFixClientSetup.init());
    }

    /** 配置加载时刷新热路径缓存（cachedKeepOverLevel/cachedDiminishFactor/cachedEnabled） */
    private static void onModConfigLoading(ModConfigEvent.Loading event) {
        AnvilXpFixForgeConfig.refreshCache();
        AnvilXpFixDebugLogger.updateEnabledCache();
    }

    /** 配置重载时刷新热路径缓存 */
    private static void onModConfigReloading(ModConfigEvent.Reloading event) {
        AnvilXpFixForgeConfig.refreshCache();
        AnvilXpFixDebugLogger.updateEnabledCache();
    }
}
