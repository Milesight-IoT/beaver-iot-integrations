package com.milesight.beaveriot.parser.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParserDefinition implements Serializable {

    /**
     * 解析器
     */
    private List<ParserDataSpec> definitions;
}
