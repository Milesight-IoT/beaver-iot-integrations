package com.milesight.beaveriot.gateway.model;

import cn.hutool.json.JSONUtil;
import lombok.Data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * @description: 机器人API数据结构
 * {"msgtype":"text",
 * "text":{
 * "content":"测试机器人报警！",
 * "mentioned_list":["@all"]
 * }
 * }
 * 这里只用了最简参数，实际接口有更多参数，具体可参考官网：
 * https://work.weixin.qq.com/api/doc/90000/90136/91770
 * @author: hubg
 * @create: 2021-11-23 15:36
 **/
@Data
public class RobotData {
    String msgtype = "text";  //消息类型
    Map<String, Object> text; //消息内容

    public RobotData(String content, Integer isAtAll) {
        String atAll = "";
        if (isAtAll == 1) {
            atAll = "@all";
        }
        this.text = new HashMap<>();
        text.put("content", content);
        text.put("mentioned_list", Collections.singletonList(atAll));
    }

    public static String toJsonStr(RobotData rd) {
        return JSONUtil.toJsonStr(rd);
    }

}
