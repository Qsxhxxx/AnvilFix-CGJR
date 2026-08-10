package com.qsxhxxx.anvilxpfix.forge;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.util.HashMap;
import java.util.Map;

/**
 * 调试日志工具类
 *
 * 性能优化：
 * 1. isEnabled() 为 public + volatile 缓存，供热路径短路检查（避免参数预构造开销）
 * 2. 计数器使用 ThreadLocal<int[]>（消除 CAS 竞争）
 * 3. 类别索引用数组预初始化（消除 computeIfAbsent 同步开销）
 */
public final class AnvilXpFixDebugLogger {

    private AnvilXpFixDebugLogger() {
    }

    /** 报错日志Logger（输出到主日志文件，前缀[anvilxpfix]） */
    private static final Logger ERROR_LOGGER = LogManager.getLogger("anvilxpfix");

    /** 调试日志Logger（输出到单独文件） */
    private static final Logger DEBUG_LOGGER = LogManager.getLogger("anvil_xp_fix_debug");

    // === 类别索引（用数组替代ConcurrentHashMap，预初始化） ===
    private static final String[] CATEGORY_NAMES = {
            "TagLevel", "DamageBonus", "Sum", "StaticMixin", "DamageEnchantment",
            "Protection", "AttrModifier", "BowItem", "AbstractArrow"
    };
    private static final int[] CATEGORY_INTERVALS = {100, 100, 100, 100, 100, 100, 50, 10, 10};
    private static final Map<String, Integer> CATEGORY_INDEX;
    static {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < CATEGORY_NAMES.length; i++) {
            map.put(CATEGORY_NAMES[i], i);
        }
        CATEGORY_INDEX = map;
    }

    // === ThreadLocal计数器（消除CAS竞争，每线程独立） ===
    private static final ThreadLocal<int[]> THREAD_COUNTERS =
            ThreadLocal.withInitial(() -> new int[CATEGORY_NAMES.length]);

    // === 缓存的开关状态（volatile，配置变更时通过updateEnabledCache更新） ===
    private static volatile boolean cachedEnabled = false;

    private static volatile boolean initialized = false;

    /**
     * 初始化调试日志文件
     * 在模组启动时调用，配置Log4j2把调试日志输出到单独文件
     */
    public static void init() {
        if (initialized) return;
        synchronized (AnvilXpFixDebugLogger.class) {
            if (initialized) return;
            try {
                LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
                Configuration config = ctx.getConfiguration();

                PatternLayout layout = PatternLayout.newBuilder()
                        .withConfiguration(config)
                        .withPattern("[%d{HH:mm:ss}] [%t/%level]: %msg%n")
                        .build();

                FileAppender fileAppender = FileAppender.newBuilder()
                        .withFileName("logs/qsxhxxx/anvilxpfix.log")
                        .withName("AnvilXpFixDebugFile")
                        .withLayout(layout)
                        .setConfiguration(config)
                        .build();

                fileAppender.start();
                config.addAppender(fileAppender);

                LoggerConfig loggerConfig = new LoggerConfig("anvil_xp_fix_debug", Level.INFO, false);
                loggerConfig.addAppender(fileAppender, null, null);
                config.addLogger("anvil_xp_fix_debug", loggerConfig);

                ctx.updateLoggers();
                initialized = true;
                // 初始化后更新缓存
                updateEnabledCache();
            } catch (Exception e) {
                ERROR_LOGGER.error("[anvilxpfix] 调试日志初始化失败", e);
            }
        }
    }

    /** 更新缓存的开关状态（配置加载/变更时调用） */
    public static void updateEnabledCache() {
        try {
            cachedEnabled = AnvilXpFixForgeConfig.debugLogEnabled != null
                    && AnvilXpFixForgeConfig.debugLogEnabled.get();
        } catch (Exception e) {
            cachedEnabled = false;
        }
    }

    /**
     * 检查调试日志是否启用
     * public 供热路径短路检查：调用方先 if(!isEnabled()) return 避免参数预构造
     */
    public static boolean isEnabled() {
        return cachedEnabled;
    }

    /**
     * 按类别降频输出调试日志
     * 性能：ThreadLocal计数器（无CAS）+ 数组索引（无HashMap同步）
     */
    public static void log(String category, String message, Object... params) {
        if (!cachedEnabled) return;
        Integer idx = CATEGORY_INDEX.get(category);
        int interval = (idx != null) ? CATEGORY_INTERVALS[idx] : 100;
        int[] counters = THREAD_COUNTERS.get();
        int counterIdx = (idx != null) ? idx : 0;
        if (counters[counterIdx]++ % interval != 0) return;
        DEBUG_LOGGER.info(message, params);
    }

    /** 清除所有降频计数器（配置关闭调试日志时调用） */
    public static void clearCounters() {
        THREAD_COUNTERS.remove();
    }

    /** 报错日志（输出到主日志，前缀[anvilxpfix]，不受开关控制） */
    public static void error(String message, Object... params) {
        ERROR_LOGGER.error("[anvilxpfix] " + message, params);
    }
}
