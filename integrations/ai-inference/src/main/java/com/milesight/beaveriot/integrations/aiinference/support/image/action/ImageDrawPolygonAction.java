package com.milesight.beaveriot.integrations.aiinference.support.image.action;

import com.milesight.beaveriot.integrations.aiinference.support.image.intefaces.ImageDrawAction;
import lombok.Data;

import java.awt.*;

/**
 * author: Luxb
 * create: 2025/6/20 08:45
 **/
@Data
public class ImageDrawPolygonAction implements ImageDrawAction {
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
        g2d.draw(polygon);
    }
}
