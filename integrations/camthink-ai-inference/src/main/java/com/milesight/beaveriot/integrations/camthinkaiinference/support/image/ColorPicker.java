package com.milesight.beaveriot.integrations.camthinkaiinference.support.image;

import java.awt.*;
import java.util.Random;

/**
 * author: Luxb
 * create: 2025/6/20 18:24
 **/
public class ColorPicker {
    private static final Color[] COLORS = {
            Color.GREEN,
            Color.RED,
            Color.BLUE,
            Color.YELLOW,
            Color.ORANGE,
            Color.PINK,
            Color.CYAN,
            Color.MAGENTA
    };
    private int index;

    public ColorPicker() {
        this(0);
    }

    public ColorPicker(int startIndex) {
        index = startIndex % COLORS.length;
    }

    public Color nextColor() {
        Color color = COLORS[index];
        index = (index + 1) % COLORS.length;
        return color;
    }

    private static final Random RANDOM = new Random();

    @SuppressWarnings("unused")
    public static Color randomColor() {
        return COLORS[RANDOM.nextInt(COLORS.length)];
    }
}
