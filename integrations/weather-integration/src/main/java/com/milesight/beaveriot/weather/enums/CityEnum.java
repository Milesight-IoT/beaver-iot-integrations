package com.milesight.beaveriot.weather.enums;

import com.milesight.beaveriot.base.enums.EnumCode;

/**
 * @Author yuanh
 * @Description
 * @Package com.milesight.beaveriot.weather.enums
 * @Date 2024/11/28 10:02
 */
public enum CityEnum implements EnumCode {
    BEI_JING("101010100","北京"),
    HAI_DIAN("101010200","海淀"),
    CHAO_YANG("101010300","朝阳"),
    SHUN_YI("101010400","顺义"),
    HUAI_ROU("101010500","怀柔"),
    TONG_ZHOU("101010600","通州"),
    CHANG_PING("101010700","昌平"),
    HARBIN("101050101","哈尔滨"),
    SHA_HAI("101020100","上海"),
    MIN_HANG("101020200","闵行"),
    BAO_SHAN("101020300","宝山"),
    HUANG_PU("101020400","黄浦"),
    FU_ZHOU("101230101","福州"),
    MIN_QING("101230102","闽清"),
    MIN_HOU("101230103","闽侯"),
    LUO_YUAN("101230104","罗源"),
    LIAN_JIANG("101230105","连江"),
    GU_LOU("101230106","鼓楼"),
    YONG_TAI("101230107","永泰"),
    PING_TAN("101230108","平潭"),
    TAI_JIANG("101230109","台江"),
    CHANG_LE("101230110","长乐"),
    FU_QING("101230111","福清"),
    CANG_SHAN("101230112","仓山"),
    MA_WEI("101230113","马尾"),
    JIN_AN("101230114","晋安"),
    XIA_MEN("101230201","厦门"),
    TONG_AN("101230202","同安"),
    SI_MING("101230203","思明"),
    HAI_CANG("101230204","海沧"),
    HU_LI("101230205","湖里"),
    JI_MEI("101230206","集美"),
    XIANG_AN("101230207","翔安"),
    NING_DE("101230301","宁德"),
    GU_TIAN("101230302","古田"),
    XIA_PU("101230303","霞浦"),
    SHOU_NING("101230304","寿宁"),
    ZHOU_NING("101230305","周宁"),
    FU_AN("101230306","福安"),
    ZHE_RONG("101230307","柘荣"),
    FU_DING("101230308","福鼎"),
    PING_NAN("101230309","屏南"),
    JIAO_CHENG("101230310","蕉城"),
    PU_TIAN("101230401","莆田"),
    XIAN_YOU("101230402","仙游"),
    HAN_JIANG("101230404","涵江"),
    XIU_YU("101230405","秀屿"),
    LI_CHENG("101230406","荔城"),
    CHENG_XIANG("101230407","城厢"),
    QUAN_ZHOU("101230501","泉州"),
    AN_XI("101230502","安溪"),
    JIN_MEN("101230503","金门"),
    YONG_CHUN("101230504","永春"),
    DE_HUA("101230505","德化"),
    NAN_AN("101230506","南安"),
    HUI_AN("101230508","惠安"),
    JIN_JIANG("101230509","晋江"),
    SHI_SHI("101230510","石狮"),
    LI_CHENG2("101230511","鲤城"),
    FENG_ZE("101230512","丰泽"),
    LUO_JIANG("101230513","洛江"),
    QUAN_GANG("101230514","泉港"),
    ZHANG_ZHOU("101230601","漳州"),
    CHANG_TAI("101230602","长泰"),
    NAN_JING("101230603","南靖"),
    PING_HE("101230604","平和"),
    LONG_HAI("101230605","龙海"),
    ZHANG_PU("101230606","漳浦"),
    ZHAO_AN("101230607","诏安"),
    DONG_SHAN("101230608","东山"),
    YUN_XIAO("101230609","云霄"),
    HUA_AN("101230610","华安"),
    XIANG_CHENG("101230611","芗城"),
    LONG_WEN("101230612","龙文"),
    LONG_YAN("101230701","龙岩"),
    CHANG_TING("101230702","长汀"),
    LIAN_CHENG("101230703","连城"),
    WU_PING("101230704","武平"),
    SHANG_HANG("101230705","上杭"),
    YONG_DING("101230706","永定"),
    ZHANG_PING("101230707","漳平"),
    XIN_LUO("101230708","新罗"),
    SAN_MING("101230801","三明"),
    NING_HUA("101230802","宁化"),
    QING_LIU("101230803","清流"),
    TAI_NING("101230804","泰宁"),
    JIANG_LE("101230805","将乐"),
    JIAN_NING("101230806","建宁"),
    MING_XI("101230807","明溪"),
    SHA_XIAN("101230808","沙县"),
    YOU_XI("101230809","尤溪"),
    YONG_AN("101230810","永安"),
    DA_TIAN("101230811","大田"),
    MEI_LIE("101230812","梅列"),
    SAN_YUAN("101230813","三元"),
    NAN_PING("101230901","南平"),
    SHUN_CHANG("101230902","顺昌"),
    GUANG_ZE("101230903","光泽"),
    SHAO_WU("101230904","邵武"),
    WU_YI_SHAN("101230905","武夷山"),
    PU_CHENG("101230906","浦城"),
    JIAN_YANG("101230907","建阳"),
    SONG_XI("101230908","松溪"),
    ZHENG_HE("101230909","政和"),
    JIAN_OU("101230910","建瓯"),
    YAN_PING("101230911","延平"),

    ;

    private String code;
    private String value;

    CityEnum(String code, String value) {
        this.code = code;
        this.value = value;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getValue() {
        return value;
    }
}
