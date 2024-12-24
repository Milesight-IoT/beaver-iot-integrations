package com.milesight.beaveriot.parser.plugin;

/**
 * @Description: 插件缓存接口
 */
public interface PlugInCacheAware {

    void setParserDataCache(PlugInCacheOperations redisTemplate);

}