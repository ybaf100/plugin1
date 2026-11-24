package io.github.ybaf100.weapon.util;

import java.text.DecimalFormat;

/** 숫자 표시용 도우미. */
public final class Fmt {
    private static final ThreadLocal<DecimalFormat> NUMBER = ThreadLocal.withInitial(() -> new DecimalFormat("0.##"));
    private static final ThreadLocal<DecimalFormat> MONEY = ThreadLocal.withInitial(() -> new DecimalFormat("#,###"));

    private Fmt() {
    }

    /** 1.0 → "1", 1.25 → "1.25" */
    public static String num(double value) {
        return NUMBER.get().format(value);
    }

    /** 0.255 → "25.5%" */
    public static String pct(double fraction) {
        return num(fraction * 100.0) + "%";
    }

    /** 1250000 → "1,250,000" */
    public static String money(double value) {
        return MONEY.get().format(value);
    }
}
