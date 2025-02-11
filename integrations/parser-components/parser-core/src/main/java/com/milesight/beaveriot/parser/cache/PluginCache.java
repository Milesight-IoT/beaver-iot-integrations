package com.milesight.beaveriot.parser.cache;

import com.milesight.beaveriot.parser.plugin.PlugIn;
import com.milesight.beaveriot.parser.util.PluginHotswapUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.milesight.beaveriot.parser.constants.CommonConstants.PLUGINS_DIR;
import static com.milesight.beaveriot.parser.constants.CommonConstants.PLUGIN_KEY;


/**
 * @author lzy
 */
@Slf4j
@Component
public class PluginCache {
    private PluginCache() {
    }


    /**
     * 插件缓存
     */
    private static final Map<String, List<PlugIn>> CACHE_PLUG_IN_MAP = new HashMap<>();

    private static final Map<String, String> CACHE_URL_MAP = new HashMap<>();

    public static void addToCache(String key, List<PlugIn> plugIns) {
        CACHE_PLUG_IN_MAP.put(key, plugIns);
    }
    public static void addToCacheUrl(String key, String url) {
        CACHE_URL_MAP.put(key, url);
        List<PlugIn> plugIns = loadParserPlugIn(url);
        CACHE_PLUG_IN_MAP.put(key, plugIns);
    }

    public static void removeToCache(String key) {
        CACHE_PLUG_IN_MAP.remove(key);
    }

    public static List<PlugIn> getFromCache(String key) {
        return CACHE_PLUG_IN_MAP.get(key);
    }

    public static void pluginLoader() {
        // 获取当前工作目录
        String currentDir = System.getProperty("user.dir");
        File resourceDir = Paths.get(currentDir).toFile();
        // 创建 plugin 文件夹
        File pluginDir = new File(resourceDir, PLUGINS_DIR);
        if (!pluginDir.exists()) {
            boolean created = pluginDir.mkdirs();
            if (!created) {
                log.error("Failed to create directory: " + pluginDir.getAbsolutePath());
            }
            return;
        }
        // 获取 plugin 文件夹下的所有文件
        File[] files = pluginDir.listFiles();
        for (File file : files) {
            if (file.isFile()) {
                String fileUrl = file.toURI().toString();
                CACHE_URL_MAP.put(PLUGIN_KEY, fileUrl);
                List<PlugIn> plugIns = loadParserPlugIn(fileUrl);
                CACHE_PLUG_IN_MAP.put(PLUGIN_KEY, plugIns);
            }
        }
    }

    /**
     * 加载解析器插件
     *
     * @param targetUrl 解析器插件地址
     * @return List<PlugIn>
     */
    @SneakyThrows
    public static List<PlugIn> loadParserPlugIn(String targetUrl) {
        List<PlugIn> plugIns = new ArrayList<>();
        try {
            List<Class<?>> beans = PluginHotswapUtil.reloadClass(targetUrl, PlugIn.class);
            if (beans.isEmpty()) {
                log.warn("上行 下行 upLink or downLink Parser plugin is empty!");
                return plugIns;
            }
            for (Class<?> bean : beans) {
                val interfaceClass = (Class<? extends PlugIn>) bean;
                plugIns.add(interfaceClass.getDeclaredConstructor().newInstance());
            }
        } catch (Exception e) {
            log.error("上行 下行 upLink or downLink Parser plugin failed to load:{}", e.getMessage(), e);
            throw e;
        }
        return plugIns;
    }
}
