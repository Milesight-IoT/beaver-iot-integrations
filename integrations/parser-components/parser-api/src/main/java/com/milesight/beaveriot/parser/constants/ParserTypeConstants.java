package com.milesight.beaveriot.parser.constants;

public interface ParserTypeConstants {

    /**
     * IPSO
     */
    String IPSO = "IPSO";

    /**
     * UCP
     */
    String UCP = "UCP";

    String PRODUCT_TYPE = "PRODUCT_%s";

    String GLOBAL_TYPE = "GLOBAL_%s";

    public static String getProductParserType(String type) {
        return String.format(PRODUCT_TYPE, type);
    }

    public static String getGlobalParserType(String type) {
        return String.format(GLOBAL_TYPE, type);
    }
}
