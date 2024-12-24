package com.milesight.beaveriot.integration.aws.model.parser;

import com.milesight.beaveriot.base.page.GenericPageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class SearchPluginRequest extends GenericPageRequest {
    private String name;
}
