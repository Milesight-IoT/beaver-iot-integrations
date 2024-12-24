package com.milesight.beaveriot.integration.aws.model;

import com.milesight.beaveriot.base.page.GenericPageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class SearchProductRequest extends GenericPageRequest {
    private String name;
}
