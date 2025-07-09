package com.milesight.beaveriot.integrations.camthinkaiinference.support.image.action;

import com.milesight.beaveriot.integrations.camthinkaiinference.support.image.ColorManager;
import com.milesight.beaveriot.integrations.camthinkaiinference.support.image.ColorPicker;
import com.milesight.beaveriot.integrations.camthinkaiinference.support.image.intefaces.ImageDrawAction;
import lombok.Data;

import java.awt.*;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/6/19 17:51
 **/
@Data
public class ImageDrawRectangleAction implements ImageDrawAction {
    private final static float DEFAULT_LINE_WIDTH = 2.0f;
    private final static String DEFAULT_COLOR_FIELD = "default";
    private int x;
    private int y;
    private int width;
    private int height;
    private String text;

    public ImageDrawRectangleAction(int x, int y, int width, int height, String text) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.text = text;
    }

    @Override
    public Map<String, ColorPicker> getColorPickerMap() {
        return Map.of(DEFAULT_COLOR_FIELD, new ColorPicker());
    }

    @Override
    public void draw(Graphics2D g2d, ColorManager colorManager) {
        ColorPicker colorPicker = colorManager.getColorPicker(this.getClass(), DEFAULT_COLOR_FIELD);
        g2d.setColor(colorPicker.nextColor());
        g2d.setStroke(new BasicStroke(DEFAULT_LINE_WIDTH));
        g2d.drawRect(x, y, width, height);
        g2d.drawString(text, x - DEFAULT_LINE_WIDTH, y - DEFAULT_LINE_WIDTH);
    }
}
