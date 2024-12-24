package com.milesight.beaveriot.integration.aws.model.parser;

import lombok.Data;

import java.util.List;

@Data
public class BatchDeleteProductRequest {
    private List<String> productIdList;
}
