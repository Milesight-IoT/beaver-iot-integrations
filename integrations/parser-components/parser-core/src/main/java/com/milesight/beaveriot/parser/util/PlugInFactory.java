package com.milesight.beaveriot.parser.util;

import com.milesight.beaveriot.parser.annotaion.PlugInType;
import com.milesight.beaveriot.parser.constants.CommonConstants;
import com.milesight.beaveriot.parser.enums.MatchType;
import com.milesight.beaveriot.parser.plugin.PlugIn;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @Description: 插件工厂
 */

@Slf4j
public class PlugInFactory {

    private PlugInFactory() {
    }



    /**
     * 设置插件
     *
     * @param plugInMap           插件集合
     * @param plugIns                插件对象
     */
    public static void setupPlugin(String sn, Map<String, PlugIn> plugInMap, List<PlugIn> plugIns) {
        if (plugIns == null) {
            return;
        }
        plugIns.forEach(bean -> {
            Class<?> cls = bean.getClass();
            PlugInType plugInTypeAnnotation = cls.getAnnotation(PlugInType.class);
            List<String> accessModes = Arrays.asList(plugInTypeAnnotation.accessModes());
            // 匹配型号
            if (matchPlugInPermissions(plugInTypeAnnotation, sn)) {
                // 判断读写权限
                if (accessModes.stream().anyMatch(model -> model.equalsIgnoreCase(CommonConstants.R))) {
                    // 缓存插件
                    plugInMap.put(plugInTypeAnnotation.channel() + "|" + plugInTypeAnnotation.type(), bean);
                } else if (accessModes.contains(CommonConstants.W)) {
                    // 缓存插件
                    plugInMap.put(plugInTypeAnnotation.id(), bean);
                }
            }
        });
    }

    /**
     * 获取解析器查询条件
     *
     * @param sn sn
     * @return ParserInfoQuery
     */
    public static String getSnMaskBySn(String sn) {
        StringBuilder parserInfoQuery = new StringBuilder();
        if (StringUtils.isNotBlank(sn)) {
            // 产品SN标识
            String snIdentification = sn.substring(0, 4);
            parserInfoQuery.append(snIdentification);
            if (sn.length() > 12) {
                // 产品SN附加位
                String snAdditionalBits = sn.substring(sn.length() - 4, sn.length() - 1);
                parserInfoQuery.append(":");
                parserInfoQuery.append(snAdditionalBits);
            }
        }
        return parserInfoQuery.toString();
    }

    /**
     * 匹配产品型号和读写权限
     *
     * @param plugInTypeAnnotation 插件类型注解
     * @return 是否匹配
     */
    private static boolean matchPlugInPermissions(PlugInType plugInTypeAnnotation, String sn) {
        String snMark = getSnMaskBySn(sn);
        List<String> snMarks = Arrays.asList(plugInTypeAnnotation.snMark());
        if (snMarks.isEmpty()){
            return true;
        }
        // 匹配方式
        MatchType matchType = plugInTypeAnnotation.matchType();
        switch (matchType) {
            case EXACT:
                return snMarks.stream().anyMatch(model -> model.equalsIgnoreCase(snMark));
            case PREFIX:
                return snMarks.stream().anyMatch(snMark::startsWith);
            case SUFFIX:
                return snMarks.stream().anyMatch(snMark::endsWith);
            case CONTAINS:
                return snMarks.stream().anyMatch(snMark::contains);
            default:
                return false;
        }
    }

}