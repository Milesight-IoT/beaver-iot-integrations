package com.milesight.beaveriot.parser.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 解析器解码输出数据
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ParserEncodeResponse {

    /**
     * sn
     */
    private String sn;

    /**
     * 失败任务ids
     */
    private List<String> failedTaskIds;

}