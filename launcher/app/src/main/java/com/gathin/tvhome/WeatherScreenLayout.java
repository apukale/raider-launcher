package com.gathin.tvhome;

/**
 * Vertical budget for the Weather screen. The today column, including the hourly
 * temperatures and the line under them, fits inside the safe area. A 1080p TV keeps
 * comfortable bottom padding. A shorter screen gives up gap before it gives up type.
 */
final class WeatherScreenLayout {
    static final int SIDE_DP = 48;
    static final int MAP_EDGE_DP = 48;
    static final int MAP_GAP_DP = 16;
    static final int COLUMN_DP = 560;

    static final class Spec {
        int side = SIDE_DP;
        int top = 28;
        int bottom = 36;
        int header = 48;
        int headerGap = 12;
        int attrGap = 8;
        int attrSp = 13;
        int column = COLUMN_DP;
        int mapEdge = MAP_EDGE_DP;
        int mapGap = MAP_GAP_DP;
        int sectionGap = 10;
        int nowPadV = 12;
        int nowPadH = 20;
        int nowIcon = 52;
        int nowTempSp = 44;
        int nowConditionSp = 18;
        int nowFeelsSp = 15;
        int statPadV = 8;
        int statPadH = 14;
        int statLabelSp = 14;
        int statValueSp = 18;
        int statInnerGap = 4;
        int stripPadV = 10;
        int stripPadH = 8;
        int hourSp = 14;
        int hourIcon = 24;
        int hourTempSp = 20;
        int hourRainSp = 13;
        int hourGap = 4;
        int captionGap = 6;
        int captionSp = 13;
        int dayTextSp = 16;
        int dayTempSp = 16;
        int listPadV = 8;
        int listPadH = 16;
        int dayRow = 50;
        int highLowWidth = 140;

        int line(int sp) { return sp + 6; }

        int nowHeight() { return Math.max(nowIcon, line(nowTempSp)) + nowPadV * 2; }

        int statRow() {
            return statPadV * 2 + line(statLabelSp) + statInnerGap + line(statValueSp);
        }

        int stripHeight() {
            return stripPadV * 2
                    + line(hourSp) + hourGap
                    + hourIcon + hourGap
                    + line(hourTempSp) + hourGap
                    + line(hourRainSp);
        }

        int todayColumn() {
            return nowHeight() + sectionGap + statRow() + sectionGap + statRow() + sectionGap
                    + stripHeight() + captionGap + line(captionSp);
        }

        /** Height left for the cards, under the header and above the attribution. */
        int body(int screenHeightDp) {
            return screenHeightDp - top - bottom - header - headerGap - attrGap - line(attrSp);
        }

        boolean todayFits(int screenHeightDp) {
            return todayColumn() <= body(screenHeightDp);
        }
    }

    private WeatherScreenLayout() {}

    static Spec fit(int screenHeightDp) {
        int height = screenHeightDp <= 0 ? 540 : screenHeightDp;
        Spec spec = new Spec();
        int guard = 0;
        while (!spec.todayFits(height) && guard++ < 24) {
            if (spec.bottom > 28) spec.bottom -= 2;
            else if (spec.sectionGap > 6) spec.sectionGap -= 2;
            else if (spec.headerGap > 8) spec.headerGap -= 2;
            else if (spec.stripPadV > 8) spec.stripPadV -= 2;
            else if (spec.nowPadV > 8) spec.nowPadV -= 2;
            else if (spec.statPadV > 4) spec.statPadV -= 1;
            else if (spec.top > 27) spec.top -= 1;
            else if (spec.captionGap > 4) spec.captionGap -= 2;
            else break;
        }
        int inner = Math.max(0, spec.body(height) - spec.listPadV * 2);
        spec.dayRow = Math.max(44, inner / 7);
        spec.highLowWidth = (int) Math.ceil(spec.dayTempSp * 0.7 * 12);
        return spec;
    }
}
