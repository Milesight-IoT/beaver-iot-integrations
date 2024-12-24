package com.milesight.beaveriot.parser.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;


/**
 * ProductInformation
 *
 * @author linzy
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductInformation implements Serializable {

    /**
     * "productInformation": {
     *                 "name": "${pd.am308lkelly8795.info.name}",
     *                 "model": "AM308LKELLY8795",
     *                 "photoUrl": "https://imgs.iotku.com/2022/1/20/4e11d8dc6881a960.png",
     *                 "deviceType": "SUB_DEVICE",
     *                 "gatewayType": "LORA",
     *                 "snLength": 16,
     *                 "snIdentification": "8795",
     *                 "snAdditionalBits": "999",
     *                 "defaultLoraClassType": "A",
     *                 "supportLoraClassTypes": [
     *         "A",
     *                 "C"
     *         ]
     *     }
     */
    private String name;
    private String model;
    private String photoUrl;
    private String deviceType;
    private String gatewayType;
    private Integer snLength;
    private String snIdentification;
    private String snAdditionalBits;
    private String defaultLoraClassType;
    private List<String> supportLoraClassTypes;

}
