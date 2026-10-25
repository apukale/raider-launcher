package com.gathin.tvhome;

/**
 * D-pad moves inside the Home Layout 2×2 at the top of that settings panel.
 * Reading order is Weather, World clocks, Panchang, Screensaver.
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
    /** Leave toward the category rail. */
    static final int EXIT_LEFT = -2;
    /** Leave downward onto the Home Layout controls. */
    static final int EXIT_DOWN = -3;
    /** Leave upward onto the settings header. */
    static final int EXIT_UP = -4;

    private HomeModules() {}

    static int neighbor(int index, int direction) {
        if (index < 0 || index >= COUNT) return STAY;
        int row = index / COLUMNS;
        int col = index % COLUMNS;
        if (direction == UP) return row == 0 ? EXIT_UP : index - COLUMNS;
        if (direction == DOWN) return row == 0 ? index + COLUMNS : EXIT_DOWN;
        if (direction == LEFT) return col == 0 ? EXIT_LEFT : index - 1;
        if (direction == RIGHT) return col == COLUMNS - 1 ? STAY : index + 1;
        return STAY;
    }
}
