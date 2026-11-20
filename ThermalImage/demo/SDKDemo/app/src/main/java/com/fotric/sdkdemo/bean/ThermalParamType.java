package com.fotric.sdkdemo.bean;

/**
 * 报警信息
 */
public enum ThermalParamType {

    atmTemp("大气温度", "atmTemp"),
    distance("目标距离", "distance"),
    emissivity("发射率", "emissivity"),
    extOptTemp("外部光学温度", "extOptTemp"),
    extOptTrans("外部光学透过率", "extOptTrans"),
    reflTemp("反射温度", "reflTemp"),
    relHumidity("相对湿度", "relHumidity");

    public String type;
    public String name;

    ThermalParamType(String name, String type) {
        this.type = type;
        this.name = name;
    }

}
