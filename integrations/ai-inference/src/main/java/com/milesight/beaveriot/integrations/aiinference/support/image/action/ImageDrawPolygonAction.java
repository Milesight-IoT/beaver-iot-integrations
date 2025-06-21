package com.milesight.beaveriot.integrations.aiinference.support.image.action;

import com.milesight.beaveriot.integrations.aiinference.support.image.ColorPicker;
import com.milesight.beaveriot.integrations.aiinference.support.image.intefaces.ImageDrawAction;
import lombok.Data;

import java.awt.*;

/**
 * author: Luxb
 * create: 2025/6/20 08:45
 **/
@Data
public class ImageDrawPolygonAction implements ImageDrawAction {
    private final static float DEFAULT_LINE_WIDTH = 2.0f;
    private final static ColorPicker colorPicker = new ColorPicker(1);
    private Polygon polygon;

    public ImageDrawPolygonAction() {
        polygon = new Polygon();
    }

    public ImageDrawPolygonAction addPoint(int x, int y) {
        polygon.addPoint(x, y);
        return this;
    }

    @Override
    public void draw(Graphics2D g2d) {
        g2d.setColor(colorPicker.nextColor());
        g2d.setStroke(new BasicStroke(DEFAULT_LINE_WIDTH));
        g2d.draw(polygon);
    }
}
