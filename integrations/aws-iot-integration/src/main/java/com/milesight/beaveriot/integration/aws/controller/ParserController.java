package com.milesight.beaveriot.integration.aws.controller;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.integration.aws.model.parser.ParserRequest;
import com.milesight.beaveriot.integration.aws.service.ParserService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.milesight.beaveriot.integration.aws.util.ParserUtils.isJsonValid;

/**
 * 解析器控制器
 */
@Slf4j
@RestController
@RequestMapping("/parser")
public class ParserController {

    @Autowired
    private ParserService parserService;

    @PostMapping
    public ResponseBody<Object> parser(@RequestBody ParserRequest parserRequest) {
        if (StrUtil.isEmpty(parserRequest.getInput())) {
            return ResponseBuilder.success("parameter cannot be empty");
        }
        if("encode".equals(parserRequest.getType())) {
            // 校验parserRequest.getInput()是否是json字符串
            boolean isValidJson = isJsonValid(parserRequest.getInput());
            if (Boolean.FALSE.equals(isValidJson)) {
                return ResponseBuilder.success("invalid json");
            }
        }
        String output = parserService.parser(parserRequest);
        if (CharSequenceUtil.isEmpty(output)) {
            return ResponseBuilder.success("parser not found");
        }
        return ResponseBuilder.success(output);
    }

}
