package com.fotric.sdkdemo.bean;

public class InfoItem {
    public String key;
    public String value;
    //数据类型
    public ThermalParamType type;

    public InfoItem(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public InfoItem(ThermalParamType type, String value) {
        this.value = value;
        this.type = type;
    }
}
