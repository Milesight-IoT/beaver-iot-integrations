package com.milesight.beaveriot.integration.aws.sdk.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author lzy
 */
@RequiredArgsConstructor
@Getter
public enum RfRegion {

    /**
     * rf region
     **/

    CN470("CN470", 7, 15, 5083000, 512, 2, 508300000, 15, 1, 0, 0, 5053000, new Integer[]{4709000, 4725000, 4741000, 4757000, 5041000, 5053000, 5057000, 5073000, 5083000, 5089000}, 0, 505300000),
    EU868("EU868", 5, 15, 8695250, 512, 3, 869525000, 15, 1, 0, 0, 8695250, new Integer[]{8681000, 8683000, 8685000, 8695250}, 1, 869525000),
    IN865("IN865", 13, 15, 8665500, 512, 4, 866550000, 15, 1, 2, 0, 8665500, new Integer[]{8650625, 8654025, 8659850, 8665500}, 0, 866550000),
    RU864("RU864", 5, 15, 8689000, 512, 3, 868900000, 15, 1, 0, 0, 8691000, new Integer[]{8689000, 8691000}, 1, 869100000),
    US915("US915", 13, 15, 9233000, 512, 8, 923300000, 15, 1, 8, 0, 9233000, new Integer[]{9039000, 9041000, 9043000, 9045000, 9047000, 9049000, 9051000, 9053000, 9046000, 9233000}, 0, 923300000),
    AU915("AU915", 13, 15, 9233000, 512, 8, 923300000, 15, 1, 8, 0, 9233000, new Integer[]{9168000, 9170000, 9172000, 9174000, 9176000, 9178000, 9180000, 9182000, 9175000, 9233000}, 0, 923300000),
    AS923_1("AS923-1", 5, 15, 9234000, 512, 3, 923400000, 15, 1, 2, 0, 9232000, new Integer[]{9232000, 9234000}, 1, 923200000),
    AS923_2("AS923-2", 5, 15, 9216000, 512, 3, 921600000, 15, 1, 2, 0, 9214000, new Integer[]{9214000, 9216000}, 1, 921400000),
    AS923_3("AS923-3", 5, 15, 9168000, 512, 3, 916800000, 15, 1, 2, 0, 9166000, new Integer[]{9166000, 9168000}, 1, 916600000),
    AS923_4("AS923-4", 5, 15, 9175000, 512, 3, 917500000, 15, 1, 2, 0, 9173000, new Integer[]{9173000, 9175000}, 1, 917300000),
    KR920("KR920", 9, 15, 9231000, 512, 3, 923100000, 15, 1, 0, 0, 9219000, new Integer[]{9219000, 9221000, 9223000, 9225000}, 0, 921900000),
    ;

    private final String value;
    private final Integer maxEirp;

    private final Integer classBTimeout;
    private final Integer pingSlotFreq;
    private final Integer pingSlotPeriod;
    private final Integer pingSlotDr;
    private final Integer frequencies;

    private final Integer classCTimeout;

    private final Integer rxDelay1;
    private final Integer rxDataRate2;
    private final Integer rxDrOffset1;
    private final Integer rxFreq2;
    private final Integer[] factoryPresetFreqs;
    private final Integer maxDutyCycle;

    private final Integer downlinkFrequency;

    public static RfRegion fromValue(String value) {
        for (RfRegion rfRegion : RfRegion.values()) {
            if (rfRegion.getValue().equals(value) || rfRegion.name().equals(value)) {
                return rfRegion;
            }
        }
        return null;
    }

}
