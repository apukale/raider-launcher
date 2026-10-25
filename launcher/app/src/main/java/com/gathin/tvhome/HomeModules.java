package com.gathin.tvhome;

/**
 * D-pad moves inside the Home 2×2 above Settings.
 * Reading order is Weather, World clocks, Panchang, Screensaver.
 * {@code contentOnRight} is the Hebrew header, where the clock sits to the right of the blocks.
 */
final class HomeModules {
    static final int WEATHER = 0;
    static final int CLOCKS = 1;
    static final int PANCHANG = 2;
    static final int SCREENSAVER = 3;
    static final int COUNT = 4;
    static final int COLUMNS = 2;

    static final int LEFT = 0;
    static final int RIGHT = 1;
    static final int UP = 2;
    static final int DOWN = 3;

    /** Stay on this block. */
    static final int STAY = -1;
    /** Leave toward the clock and Hindu calendar. */
    static final int EXIT_SIDE = -2;
    /** Leave downward onto the pill under this column. */
    static final int EXIT_DOWN = -3;

    static final int PILL_INPUTS = 0;
    static final int PILL_SETTINGS = 2;

    private HomeModules() {}

    static int neighbor(int index, int direction, boolean contentOnRight) {
        if (index < 0 || index >= COUNT) return STAY;
        int row = index / COLUMNS;
        int col = index % COLUMNS;
        if (direction == UP) return row == 0 ? STAY : index - COLUMNS;
        if (direction == DOWN) return row == 0 ? index + COLUMNS : EXIT_DOWN;
        boolean towardContent = contentOnRight ? direction == RIGHT : direction == LEFT;
        boolean awayFromContent = contentOnRight ? direction == LEFT : direction == RIGHT;
        if (towardContent) {
            if (!contentOnRight) return col == 0 ? EXIT_SIDE : index - 1;
            return col == COLUMNS - 1 ? EXIT_SIDE : index + 1;
        }
        if (awayFromContent) {
            if (!contentOnRight) return col == COLUMNS - 1 ? STAY : index + 1;
            return col == 0 ? STAY : index - 1;
        }
        return STAY;
    }

    /** Bottom-row block to focus when coming up from a pill. Wifi uses the inner column. */
    static int moduleAbovePill(int pill, boolean contentOnRight) {
        boolean settings = pill == PILL_SETTINGS;
        if (!contentOnRight) return settings ? SCREENSAVER : PANCHANG;
        return settings ? PANCHANG : SCREENSAVER;
    }

    /** Inputs or Settings, the pill under this block's column. */
    static int pillBelow(int index, boolean contentOnRight) {
        int col = index % COLUMNS;
        boolean settingsColumn = contentOnRight ? col == 0 : col == COLUMNS - 1;
        return settingsColumn ? PILL_SETTINGS : PILL_INPUTS;
    }
}
