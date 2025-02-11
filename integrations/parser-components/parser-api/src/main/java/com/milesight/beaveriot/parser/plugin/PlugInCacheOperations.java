package com.milesight.beaveriot.parser.plugin;

/**
 * 解析器缓存
 */
public interface PlugInCacheOperations {

    /**
     * 删除属性
     *
     * @param sn 属性sn
     */
    void delete(String sn);

    /**
     * 获取属性值
     *
     * @param sn 属性sn
     * @return 属性值
     */
    String get(String sn);

    /**
     * 更新属性值
     *
     * @param sn  属性sn
     * @param value 属性值
     * @param expirationTime 过期时间, 秒
     */
    void put(String sn, String value, Long expirationTime);

}
