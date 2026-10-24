package com.gathin.tvhome;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Compact settings popup. Stock {@code AlertDialog} list panels on a TV fill the screen and show
 * only a handful of rows; every settings chooser, list, confirm, and text prompt uses this card
 * instead so they share one width and one row size.
 */
final class SettingsPicker {
    interface Pick { void onPick(int index); }
    interface Choose { void onChoose(String value); }
    /** Live filter for a long list. {@code apply} receives the current field text. */
    interface Filter { String[] apply(String query); }
    /** Return false to leave the check unchanged (the last photo source cannot be turned off). */
    interface Allow { boolean allow(int index, boolean turnOn); }

    private static final float ROW_DP = 36f;
    private static final float CHROME_DP = 100f;
    private static final float WIDTH_DP = 400f;
    private static final float WIDTH_FLOOR_DP = 280f;
    private static final float WIDTH_FRACTION = 0.38f;
    private static final float HEIGHT_FRACTION = 0.72f;
    private static final int MAX_VISIBLE = 10;

    private SettingsPicker() {}

    /** Narrower than a stock TV dialog: at most 38% of the screen, and never wider than 400 dp. */
    static int panelWidthPx(int screenWidthPx, float density) {
        int preferred = Math.round(WIDTH_DP * density);
        int cap = Math.round(screenWidthPx * WIDTH_FRACTION);
        int floor = Math.round(WIDTH_FLOOR_DP * density);
        return Math.max(floor, Math.min(preferred, Math.max(cap, floor)));
    }

    /** How many rows fit in the card. Short lists stay short; long ones scroll after at most 10. */
    static int visibleRows(int itemCount, int screenHeightPx, float density) {
        if (itemCount <= 0) return 0;
        int row = Math.max(1, Math.round(ROW_DP * density));
        int chrome = Math.round(CHROME_DP * density);
        int scroll = Math.max(row, Math.round(screenHeightPx * HEIGHT_FRACTION) - chrome);
        int fit = Math.min(MAX_VISIBLE, Math.max(1, scroll / row));
        return Math.min(itemCount, fit);
    }

    static void single(Activity activity, CharSequence title, CharSequence[] items, int selected, Pick onPick) {
        single(activity, title, items, selected, null, onPick);
    }

    /** {@code faces} is optional; a non-null entry draws that row in its own typeface (the font list). */
    static void single(Activity activity, CharSequence title, CharSequence[] items, int selected, android.graphics.Typeface[] faces, Pick onPick) {
        rows(activity, title, items, selected, faces, false, null, onPick, null, null, false);
    }

    static void list(Activity activity, CharSequence title, CharSequence[] items, Pick onPick) {
        list(activity, title, items, onPick, false);
    }

    /**
     * @param swallowFirstCenterUp the Home tile menu opens on a long-press while OK is still down.
     *        The release must not activate the focused row.
     */
    static void list(Activity activity, CharSequence title, CharSequence[] items, Pick onPick, boolean swallowFirstCenterUp) {
        rows(activity, title, items, -1, null, false, null, onPick, null, null, swallowFirstCenterUp);
    }

    static void multi(Activity activity, CharSequence title, CharSequence[] items, boolean[] checked, Allow allow, Runnable onOk) {
        rows(activity, title, items, -1, null, true, checked, null, allow, onOk, false);
    }

    static void confirm(Activity activity, CharSequence title, CharSequence message, Runnable onPositive) {
        buttons(activity, title, message, null,
                activity.getString(android.R.string.cancel), null,
                null, null,
                activity.getString(android.R.string.ok), onPositive);
    }

    static void notice(Activity activity, CharSequence title, CharSequence message) {
        buttons(activity, title, message, null, null, null, null, null, activity.getString(android.R.string.ok), null);
    }

    static void buttons(Activity activity, CharSequence title, CharSequence message, View body,
            CharSequence negative, Runnable onNegative,
            CharSequence neutral, Runnable onNeutral,
            CharSequence positive, Runnable onPositive) {
        float density = activity.getResources().getDisplayMetrics().density;
        int width = panelWidthPx(activity.getResources().getDisplayMetrics().widthPixels, density);
        LinearLayout card = card(activity, width);
        addTitle(activity, card, title);
        if (message != null && message.length() > 0) {
            TextView bodyText = text(activity, message, 15f, 0xFFCAC4D0, false);
            bodyText.setPadding(dp(activity, 16), dp(activity, 4), dp(activity, 16), dp(activity, 8));
            int max = Math.round(activity.getResources().getDisplayMetrics().heightPixels * 0.42f);
            ScrollView scroll = new ScrollView(activity);
            scroll.setFillViewport(false);
            scroll.addView(bodyText);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            scroll.setLayoutParams(lp);
            card.addView(scroll);
            scroll.post(() -> {
                if (scroll.getChildAt(0).getHeight() > max) {
                    ViewGroup.LayoutParams slp = scroll.getLayoutParams();
                    slp.height = max;
                    scroll.setLayoutParams(slp);
                }
            });
        }
        if (body != null) {
            body.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            card.addView(body);
        }
        Dialog dialog = mount(activity, card, width);
        LinearLayout bar = buttonBar(activity, dialog, negative, onNegative, neutral, onNeutral, positive, onPositive);
        card.addView(bar);
        View focus = firstFocusable(body);
        if (focus == null && bar.getChildCount() > 0) focus = bar.getChildAt(bar.getChildCount() - 1);
        final View initial = focus;
        dialog.setOnShowListener(d -> {
            if (initial != null) initial.requestFocus();
            if (hasEditText(body)) {
                Window window = dialog.getWindow();
                if (window != null) window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
        });
        dialog.show();
    }

    /** Text prompt (DNS hostname, city search, input rename, night time). Already showing. */
    static Dialog form(Activity activity, CharSequence title, View body,
            CharSequence negative, Runnable onNegative,
            CharSequence neutral, Runnable onNeutral,
            CharSequence positive, Runnable onPositive) {
        float density = activity.getResources().getDisplayMetrics().density;
        int width = panelWidthPx(activity.getResources().getDisplayMetrics().widthPixels, density);
        LinearLayout card = card(activity, width);
        addTitle(activity, card, title);
        if (body != null) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            body.setLayoutParams(lp);
            card.addView(body);
        }
        Dialog dialog = mount(activity, card, width);
        LinearLayout bar = buttonBar(activity, dialog, negative, onNegative, neutral, onNeutral, positive, onPositive);
        card.addView(bar);
        View focus = firstFocusable(body);
        if (focus == null && bar.getChildCount() > 0) focus = bar.getChildAt(0);
        final View initial = focus;
        dialog.setOnShowListener(d -> {
            if (initial != null) initial.requestFocus();
            if (hasEditText(body)) {
                Window window = dialog.getWindow();
                if (window != null) window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
        });
        dialog.show();
        return dialog;
    }

    /**
     * Scrollable list with a type-to-filter field. Empty text shows every item.
     * Down from the field enters the list; Up from the first row returns to the field.
     */
    static void filter(Activity activity, CharSequence title, Filter filter, String selected, Choose onChoose) {
        float density = activity.getResources().getDisplayMetrics().density;
        int width = panelWidthPx(activity.getResources().getDisplayMetrics().widthPixels, density);
        LinearLayout card = card(activity, width);
        addTitle(activity, card, title);
        EditText field = new EditText(activity);
        field.setId(View.generateViewId());
        field.setSingleLine(true);
        field.setHint("Type to filter");
        field.setTextColor(0xFFF4F1EA);
        field.setHintTextColor(0xFFA9ABC7);
        field.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);
        field.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        field.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        LinearLayout.LayoutParams fieldLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fieldLp.setMargins(dp(activity, 16), dp(activity, 2), dp(activity, 16), dp(activity, 2));
        card.addView(field, fieldLp);
        TextView count = text(activity, "", 13f, 0xFFA9ABC7, false);
        count.setPadding(dp(activity, 16), 0, dp(activity, 16), dp(activity, 4));
        card.addView(count);
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        scroll.setFocusable(false);
        scroll.setVerticalScrollBarEnabled(true);
        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        int visible = Math.max(4, visibleRows(MAX_VISIBLE, activity.getResources().getDisplayMetrics().heightPixels, density) - 1);
        int scrollH = visible * Math.round(ROW_DP * density);
        card.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, scrollH));
        Dialog dialog = mount(activity, card, width);
        LinearLayout bar = buttonBar(activity, dialog, activity.getString(android.R.string.cancel), null, null, null, null, null);
        card.addView(bar);
        View cancel = bar.getChildCount() > 0 ? bar.getChildAt(0) : null;
        Runnable refill = () -> {
            String[] matches = filter == null ? new String[0] : filter.apply(field.getText().toString());
            if (matches == null) matches = new String[0];
            count.setText(matches.length == 0 ? "No matches" : matches.length == 1 ? "1 timezone" : matches.length + " timezones");
            list.removeAllViews();
            for (String zone : matches) {
                if (zone == null) continue;
                final String value = zone;
                LinearLayout row = new LinearLayout(activity);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setMinimumHeight(Math.round(ROW_DP * density));
                row.setPadding(dp(activity, 12), dp(activity, 2), dp(activity, 10), dp(activity, 2));
                row.setFocusable(true);
                row.setClickable(true);
                row.setId(View.generateViewId());
                row.setTag(value);
                TextView label = text(activity, value, 15f, 0xFFF4F1EA, true);
                label.setSingleLine(true);
                label.setEllipsize(TextUtils.TruncateAt.END);
                row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                if (value.equals(selected)) {
                    Mark mark = new Mark(activity, false);
                    mark.on = true;
                    int s = dp(activity, 16);
                    LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(s, s);
                    mp.setMarginStart(dp(activity, 10));
                    row.addView(mark, mp);
                }
                row.setOnFocusChangeListener((v, has) -> {
                    v.setBackground(SettingsStyle.focus(activity, SettingsStyle.accent(activity), has));
                    if (has) v.post(() -> scroll.requestChildRectangleOnScreen(v, new Rect(0, 0, v.getWidth(), v.getHeight()), false));
                });
                row.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (onChoose != null) onChoose.onChoose(value);
                });
                list.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            View first = list.getChildCount() > 0 ? list.getChildAt(0) : null;
            View last = list.getChildCount() > 0 ? list.getChildAt(list.getChildCount() - 1) : null;
            if (first != null) {
                field.setNextFocusDownId(first.getId());
                first.setNextFocusUpId(field.getId());
            } else if (cancel != null) field.setNextFocusDownId(cancel.getId());
            if (last != null && cancel != null) {
                last.setNextFocusDownId(cancel.getId());
                cancel.setNextFocusUpId(last.getId());
            } else if (cancel != null) cancel.setNextFocusUpId(field.getId());
            if (cancel != null) {
                field.setNextFocusUpId(cancel.getId());
                cancel.setNextFocusDownId(field.getId());
            }
        };
        field.setOnEditorActionListener((v, action, event) -> {
            if (action != android.view.inputmethod.EditorInfo.IME_ACTION_NEXT
                    && action != android.view.inputmethod.EditorInfo.IME_ACTION_DONE) return false;
            if (list.getChildCount() > 0) list.getChildAt(0).requestFocus();
            return true;
        });
        field.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int n, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int n) {}
            public void afterTextChanged(android.text.Editable s) { refill.run(); }
        });
        refill.run();
        View initial = field;
        if (selected != null) {
            for (int i = 0; i < list.getChildCount(); i++) {
                if (selected.equals(list.getChildAt(i).getTag())) { initial = list.getChildAt(i); break; }
            }
        }
        final View focus = initial;
        dialog.setOnShowListener(d -> {
            focus.requestFocus();
            if (focus == field) {
                Window window = dialog.getWindow();
                if (window != null) window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
        });
        dialog.show();
    }

    private static void rows(Activity activity, CharSequence title, CharSequence[] items, int selected,
            android.graphics.Typeface[] faces, boolean multi, boolean[] checked, Pick onPick, Allow allow, Runnable onOk,
            boolean swallowFirstCenterUp) {
        float density = activity.getResources().getDisplayMetrics().density;
        int width = panelWidthPx(activity.getResources().getDisplayMetrics().widthPixels, density);
        int count = items == null ? 0 : items.length;
        int visible = visibleRows(count, activity.getResources().getDisplayMetrics().heightPixels, density);
        LinearLayout card = card(activity, width);
        addTitle(activity, card, title);
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        scroll.setFocusable(false);
        scroll.setVerticalScrollBarEnabled(count > visible);
        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        int scrollH = Math.max(1, visible) * Math.round(ROW_DP * density);
        card.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, count == 0 ? 0 : scrollH));
        Dialog dialog = mount(activity, card, width);
        final boolean[] swallow = {swallowFirstCenterUp};
        View focus = null;
        View last = null;
        for (int i = 0; i < count; i++) {
            final int index = i;
            boolean on = multi && checked != null && index < checked.length && checked[index];
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setMinimumHeight(Math.round(ROW_DP * density));
            row.setPadding(dp(activity, 12), dp(activity, 2), dp(activity, 10), dp(activity, 2));
            row.setFocusable(true);
            row.setClickable(true);
            row.setId(View.generateViewId());
            TextView label = text(activity, items[i] == null ? "" : items[i], 16f, 0xFFF4F1EA, true);
            if (faces != null && index < faces.length && faces[index] != null) label.setTypeface(faces[index]);
            label.setMaxLines(2);
            label.setEllipsize(TextUtils.TruncateAt.END);
            row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            Mark mark = null;
            if (multi || selected >= 0) {
                mark = new Mark(activity, multi);
                mark.on = multi ? on : index == selected;
                int s = dp(activity, 16);
                LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(s, s);
                mp.setMarginStart(dp(activity, 10));
                row.addView(mark, mp);
            }
            final Mark markRef = mark;
            row.setOnFocusChangeListener((v, has) -> {
                v.setBackground(SettingsStyle.focus(activity, SettingsStyle.accent(activity), has));
                if (has) v.post(() -> scroll.requestChildRectangleOnScreen(v, new Rect(0, 0, v.getWidth(), v.getHeight()), false));
            });
            row.setOnKeyListener((v, code, event) -> {
                if (!swallow[0]) return false;
                if ((code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER) && event.getAction() == KeyEvent.ACTION_UP) {
                    swallow[0] = false;
                    return true;
                }
                return false;
            });
            row.setOnClickListener(v -> {
                if (multi) {
                    if (checked == null || index >= checked.length) return;
                    boolean turnOn = !checked[index];
                    if (allow != null && !allow.allow(index, turnOn)) return;
                    checked[index] = turnOn;
                    if (markRef != null) { markRef.on = turnOn; markRef.invalidate(); }
                    return;
                }
                dialog.dismiss();
                if (onPick != null) onPick.onPick(index);
            });
            list.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            if (selected >= 0 && index == selected) focus = row;
            if (focus == null) focus = row;
            last = row;
        }
        CharSequence negative = activity.getString(android.R.string.cancel);
        CharSequence positive = multi ? activity.getString(android.R.string.ok) : null;
        LinearLayout bar = buttonBar(activity, dialog, negative, null, null, null, positive, onOk);
        card.addView(bar);
        if (last != null && bar.getChildCount() > 0) {
            View button = bar.getChildAt(0);
            last.setNextFocusDownId(button.getId());
            for (int b = 0; b < bar.getChildCount(); b++) bar.getChildAt(b).setNextFocusUpId(last.getId());
        }
        final View initial = focus;
        dialog.setOnShowListener(d -> { if (initial != null) initial.requestFocus(); });
        dialog.show();
    }

    private static LinearLayout card(Activity activity, int width) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(0, dp(activity, 6), 0, dp(activity, 6));
        GradientDrawable bg = SettingsStyle.card(activity);
        bg.setStroke(Math.max(1, dp(activity, 1)), 0x33FFFFFF);
        card.setBackground(bg);
        card.setMinimumWidth(width);
        return card;
    }

    private static void addTitle(Activity activity, LinearLayout card, CharSequence title) {
        if (title == null || title.length() == 0) return;
        TextView view = text(activity, title, 18f, 0xFFF4F1EA, true);
        view.setPadding(dp(activity, 16), dp(activity, 8), dp(activity, 16), dp(activity, 6));
        card.addView(view);
    }

    private static TextView text(Activity activity, CharSequence value, float sp, int color, boolean medium) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setIncludeFontPadding(false);
        view.setTypeface(Fonts.of(activity, Fonts.SETTINGS, medium ? R.font.outfit_medium : R.font.outfit_regular));
        return view;
    }

    private static LinearLayout buttonBar(Activity activity, Dialog dialog,
            CharSequence negative, Runnable onNegative,
            CharSequence neutral, Runnable onNeutral,
            CharSequence positive, Runnable onPositive) {
        LinearLayout bar = new LinearLayout(activity);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(activity, 8), dp(activity, 2), dp(activity, 8), dp(activity, 4));
        addButton(activity, bar, dialog, negative, onNegative);
        addButton(activity, bar, dialog, neutral, onNeutral);
        addButton(activity, bar, dialog, positive, onPositive);
        return bar;
    }

    private static void addButton(Activity activity, LinearLayout bar, Dialog dialog, CharSequence label, Runnable action) {
        if (label == null || label.length() == 0) return;
        TextView button = text(activity, label, 15f, SettingsStyle.accent(activity), true);
        button.setId(View.generateViewId());
        button.setFocusable(true);
        button.setClickable(true);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(activity, 36));
        button.setPadding(dp(activity, 12), dp(activity, 6), dp(activity, 12), dp(activity, 6));
        button.setOnFocusChangeListener((v, has) -> v.setBackground(SettingsStyle.focus(activity, SettingsStyle.accent(activity), has)));
        button.setOnClickListener(v -> {
            dialog.dismiss();
            if (action != null) action.run();
        });
        bar.addView(button);
    }

    private static Dialog mount(Activity activity, LinearLayout card, int width) {
        Dialog dialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        FrameLayout root = new FrameLayout(activity);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        root.addView(card, lp);
        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.55f);
        }
        return dialog;
    }

    private static View firstFocusable(View view) {
        if (view == null) return null;
        if (view.isFocusable()) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = firstFocusable(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private static boolean hasEditText(View view) {
        if (view instanceof EditText) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) if (hasEditText(group.getChildAt(i))) return true;
        }
        return false;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    /** Radio for a single choice, box for a multi choice. Drawn, so it does not depend on the row's font. */
    private static final class Mark extends View {
        boolean on;
        private final boolean check;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Mark(Activity activity, boolean check) { super(activity); this.check = check; }
        @Override protected void onDraw(Canvas canvas) {
            float d = getResources().getDisplayMetrics().density;
            int accent = SettingsStyle.accent(getContext());
            float cx = getWidth() / 2f, cy = getHeight() / 2f, r = Math.min(getWidth(), getHeight()) / 2f - d;
            if (!check) {
                paint.setColor(on ? accent : SettingsStyle.SECONDARY);
                paint.setStyle(on ? Paint.Style.FILL : Paint.Style.STROKE);
                paint.setStrokeWidth(2f * d);
                canvas.drawCircle(cx, cy, r, paint);
                return;
            }
            paint.setStyle(on ? Paint.Style.FILL : Paint.Style.STROKE);
            paint.setStrokeWidth(2f * d);
            paint.setColor(on ? accent : SettingsStyle.SECONDARY);
            float inset = d;
            canvas.drawRoundRect(inset, inset, getWidth() - inset, getHeight() - inset, 3f * d, 3f * d, paint);
            if (on) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2f * d);
                paint.setColor(0xFF1C1B1F);
                canvas.drawLine(getWidth() * 0.24f, cy, cx - d, getHeight() * 0.72f, paint);
                canvas.drawLine(cx - d, getHeight() * 0.72f, getWidth() * 0.76f, getHeight() * 0.30f, paint);
            }
        }
    }
}
