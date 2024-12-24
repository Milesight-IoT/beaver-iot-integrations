package com.milesight.beaveriot.parser.model;

import com.milesight.cloud.sdk.client.model.ThingSpec;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;


/**
 *
 *
 * @author linzy
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Multilingual implements Serializable {



    /**
     * "i18n": [
     *         {
     *             "code": "pd.am300.tsl.event.historical_data.name",
     *             "value": "Historical Data Return",
     *             "locale": "en"
     */

    /**
     *
     */
    private String code;

    private String value;

    private String locale;

}
