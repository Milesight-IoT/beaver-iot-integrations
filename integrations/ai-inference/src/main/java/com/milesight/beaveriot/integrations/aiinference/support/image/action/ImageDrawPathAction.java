package com.milesight.beaveriot.integrations.aiinference.support.image.action;

import com.milesight.beaveriot.integrations.aiinference.support.image.intefaces.ImageDrawAction;
import lombok.Data;

import java.awt.*;
import java.awt.geom.GeneralPath;

/**
 * author: Luxb
 * create: 2025/6/20 09:06
 **/
@Data
public class ImageDrawPathAction implements ImageDrawAction {
    private GeneralPath path;

    public ImageDrawPathAction() {
        path = new GeneralPath();
    }

    public ImageDrawPathAction lineTo(int x, int y) {
        path.lineTo(x, y);
        return this;
    }

    public ImageDrawPathAction moveTo(int x, int y) {
        path.moveTo(x, y);
        return this;
    }

    @Override
    public void draw(Graphics2D g2d) {
        g2d.draw(path);
    }
}
