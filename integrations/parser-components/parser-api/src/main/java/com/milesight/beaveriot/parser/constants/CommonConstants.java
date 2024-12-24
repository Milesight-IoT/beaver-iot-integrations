package com.milesight.beaveriot.parser.constants;


/**
 * @Description: 解析器通用常量
 */
public class CommonConstants {



    public static final String PRODUCT = "products";

    public static final String MODEL = "model";

    public static final String SN_IDENTIFICATION = "snIdentification";
    public static final String SN_ADDITIONAL_BITS = "snAdditionalBits";

    public static final String PLUGINS_DIR = "plugins";
    public static final String PLUGIN_KEY = "plugin";

    /**
     * 只读 read
     */
    public static final String R = "r";

    /**
     * 只写 write
     */
    public static final String W = "w";

    public static final String DECODE = "decode";

    public static final String ENCODE = "encode";

    public static final String NODE = "_item";


    public static final String HISTORICAL_DATA = "historical_data";


    public static final String TIMESTAMP = "timestamp";



    /**
     * 连接节点
     */
    public static final String CONNECT_NODE = "%s.%s";

    /**
     * 连接数组结构节点
     */
    public static final String CONNECT_ARRAY_STRUCT_NODE = "%s[%S]";

    /**
     * jar包URL
     */
    public static final String JAR_URL = "jar:%s!/";


    /**
     * 获取Path
     */
    public static String getPath(String parentId, String nodeId) {
        return String.format(CONNECT_NODE, parentId, nodeId);
    }

    /**
     * 获取数组Path
     */
    public static String getArrayStructPath(String parentId, int index) {
        return String.format(CONNECT_ARRAY_STRUCT_NODE, parentId, index);
    }

    /**
     * 获取URL
     */
    public static String getPlugInUrlByUrl(String url) {
        return String.format(JAR_URL, url);
    }


}