package com.ccloud.framework.common.util.number;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额工具。项目内金额一律用 int 存储，单位「分」——用浮点数存钱是事故的开始。
 */
public class MoneyUtils {

    private static final int PRICE_SCALE = 2;

    /**
     * 按百分比计算金额，四舍五入到分
     *
     * @param price   原始金额（分）
     * @param percent 百分比，如 80 表示八折
     */
    public static Integer calculateRatePrice(Integer price, Double percent) {
        return calculateRatePrice(price, percent, 0, RoundingMode.HALF_UP).intValue();
    }

    public static BigDecimal calculateRatePrice(Number price, Number rate, int scale, RoundingMode roundingMode) {
        return BigDecimal.valueOf(price.doubleValue())
                .multiply(BigDecimal.valueOf(rate.doubleValue()))
                .divide(BigDecimal.valueOf(100), scale, roundingMode);
    }

    /**
     * 分转元，用于展示
     */
    public static BigDecimal fenToYuan(Integer fen) {
        return fen == null ? null
                : BigDecimal.valueOf(fen).divide(BigDecimal.valueOf(100), PRICE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 元转分，用于入库
     */
    public static Integer yuanToFen(BigDecimal yuan) {
        return yuan == null ? null : yuan.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    public static String fenToYuanStr(Integer fen) {
        BigDecimal yuan = fenToYuan(fen);
        return yuan == null ? null : yuan.toPlainString();
    }
}
