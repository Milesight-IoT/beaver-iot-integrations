package com.milesight.beaveriot.integrations.camthinkaiinference.support.image.action;

import com.milesight.beaveriot.integrations.camthinkaiinference.support.image.ColorManager;
import com.milesight.beaveriot.integrations.camthinkaiinference.support.image.ColorPicker;
import com.milesight.beaveriot.integrations.camthinkaiinference.support.image.intefaces.ImageDrawAction;
import lombok.Data;

import java.awt.*;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/6/20 08:45
 **/
@Data
public class ImageDrawPolygonAction implements ImageDrawAction {
    private final static float DEFAULT_LINE_WIDTH = 1.0f;
    private final static String DEFAULT_COLOR_FIELD = "default";
    private final static float FILL_TRANSPARENCY = 0.26f;
    private Polygon polygon;

    public ImageDrawPolygonAction() {
        polygon = new Polygon();
    }

    public ImageDrawPolygonAction addPoint(int x, int y) {
        polygon.addPoint(x, y);
        return this;
    }

    @Override
    public Map<String, ColorPicker> getColorPickerMap() {
        return Map.of(DEFAULT_COLOR_FIELD, new ColorPicker(1));
    }

    @Override
    public void draw(Graphics2D g2d, ColorManager colorManager) {
        ColorPicker colorPicker = colorManager.getColorPicker(this.getClass(), DEFAULT_COLOR_FIELD);
        Color color = colorPicker.nextColor();

        Color fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (255 * FILL_TRANSPARENCY));
        g2d.setColor(fillColor);
        g2d.fill(polygon);

        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(DEFAULT_LINE_WIDTH));
        g2d.draw(polygon);
    }
}