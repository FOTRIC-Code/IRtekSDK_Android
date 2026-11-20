package com.fotric.sdkdemo.utils;

import java.util.Formatter;

public class TempUtils {

    /**
     * 温度转换
     *
     * @param kelvin_temperature
     * @return
     */
    public static String tempConvertKToC(float kelvin_temperature) {
        return new Formatter().format("%.2f", kelvin_temperature - 273.15F).toString();
    }
    /**
     * 温度转换
     *
     * @return
     */
    public static String tempConvertCToK(float temperature) {
        return new Formatter().format("%.2f", temperature + 273.15F).toString();
    }
}
