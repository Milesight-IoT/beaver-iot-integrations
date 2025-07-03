package com.milesight.beaveriot.integrations.camthinkaiinference.support.image.config;

import lombok.Data;

import java.awt.*;

/**
 * author: Luxb
 * create: 2025/6/19 17:43
 **/
@Data
public class ImageDrawConfig {
    private final static Color DEFAULT_LINE_COLOR = Color.RED;
    private final static float DEFAULT_LINE_WIDTH = 2.0f;
    private Color lineColor;
    private float lineWidth;

    public static ImageDrawConfig getDefault() {
        ImageDrawConfig config = new ImageDrawConfig();
        config.setLineColor(DEFAULT_LINE_COLOR);
        config.setLineWidth(DEFAULT_LINE_WIDTH);
        return config;
    }
}
