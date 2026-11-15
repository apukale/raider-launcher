package com.gathin.tvhome;

/** D-pad moves for the Movies & Series screen. Edges stay put instead of wrapping or escaping. */
final class BrowseFocus {
    static final int FILTER = 0;
    static final int GRID = 1;

    static final class Step {
        final int zone;
        final int row;
        final int index;
        final boolean stay;

        Step(int zone, int row, int index, boolean stay) {
            this.zone = zone;
            this.row = row;
            this.index = index;
            this.stay = stay;
        }
    }

    private BrowseFocus() {}

    static Step filter(int row, int index, int rowSize, int filterRows, int key) {
        int size = Math.max(1, rowSize);
        int safe = Math.max(0, Math.min(index, size - 1));
        if (key == android.view.KeyEvent.KEYCODE_DPAD_LEFT) {
            return safe == 0 ? stay(FILTER, row, safe) : move(FILTER, row, safe - 1);
        }
        if (key == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) {
            return safe >= size - 1 ? stay(FILTER, row, safe) : move(FILTER, row, safe + 1);
        }
        if (key == android.view.KeyEvent.KEYCODE_DPAD_UP) {
            return row <= 0 ? stay(FILTER, row, safe) : move(FILTER, row - 1, safe);
        }
        if (key == android.view.KeyEvent.KEYCODE_DPAD_DOWN) {
            return row >= filterRows - 1 ? move(GRID, 0, 0) : move(FILTER, row + 1, safe);
        }
        return stay(FILTER, row, safe);
    }

    static Step grid(int position, int columns, int count, int filterRows, int key) {
        int cols = Math.max(1, columns);
        if (count <= 0) {
            if (key == android.view.KeyEvent.KEYCODE_DPAD_UP) return move(FILTER, Math.max(0, filterRows - 1), 0);
            return stay(GRID, 0, 0);
        }
        int pos = Math.max(0, Math.min(position, count - 1));
        int col = pos % cols;
        int row = pos / cols;
        if (key == android.view.KeyEvent.KEYCODE_DPAD_LEFT) {
            return col == 0 ? stay(GRID, row, pos) : move(GRID, row, pos - 1);
        }
        if (key == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (col == cols - 1 || pos + 1 >= count) return stay(GRID, row, pos);
            return move(GRID, row, pos + 1);
        }
        if (key == android.view.KeyEvent.KEYCODE_DPAD_UP) {
            if (row == 0) return move(FILTER, Math.max(0, filterRows - 1), 0);
            return move(GRID, row - 1, pos - cols);
        }
        if (key == android.view.KeyEvent.KEYCODE_DPAD_DOWN) {
            if (pos + cols >= count) return stay(GRID, row, pos);
            return move(GRID, row + 1, pos + cols);
        }
        return stay(GRID, row, pos);
    }

    private static Step move(int zone, int row, int index) {
        return new Step(zone, row, index, false);
    }

    private static Step stay(int zone, int row, int index) {
        return new Step(zone, row, index, true);
    }
}
