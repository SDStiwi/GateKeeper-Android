package com.sdstiwi.gatekeeper;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

import androidx.core.content.ContextCompat;

/**
 * Assigns a consistent accent color to each course name so the weekly
 * timetable is easy to scan at a glance (same course = same color everywhere),
 * and builds the rounded, tinted cell backgrounds used by the timetable views.
 */
public final class TimetableColorUtil {

    private TimetableColorUtil() {
    }

    private static final int[] PALETTE = {
            R.color.math_color,
            R.color.physics_color,
            R.color.chemistry_color,
            R.color.cs_color,
            R.color.english_color,
            R.color.biology_color,
            R.color.history_color,
            R.color.art_color,
            R.color.geography_color,
            R.color.music_color,
            R.color.pe_color,
            R.color.drama_color
    };

    /** Deterministic color for a given course/class name (same name -> same color every time). */
    public static int colorFor(Context context, String className) {
        if (className == null || className.trim().isEmpty()) {
            return ContextCompat.getColor(context, R.color.study_color);
        }
        int index = Math.abs(className.trim().toLowerCase().hashCode()) % PALETTE.length;
        return ContextCompat.getColor(context, PALETTE[index]);
    }

    /** Rounded, tinted card background for a populated session cell, accented with the course color. */
    public static GradientDrawable sessionBackground(Context context, int accentColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dp(context, 12));
        drawable.setColor(withAlpha(accentColor, 28));
        drawable.setStroke(Math.round(dp(context, 1.5f)), withAlpha(accentColor, 150));
        return drawable;
    }

    /** Neutral "Free" slot background. */
    public static GradientDrawable freeBackground(Context context) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dp(context, 12));
        drawable.setColor(Color.parseColor("#F8FAFC"));
        drawable.setStroke(Math.round(dp(context, 1f)), Color.parseColor("#E2E8F0"));
        return drawable;
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private static float dp(Context context, float value) {
        return value * context.getResources().getDisplayMetrics().density;
    }
}
