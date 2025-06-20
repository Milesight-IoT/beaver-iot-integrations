package com.milesight.beaveriot.integrations.aiinference.support.image.action;

import com.milesight.beaveriot.integrations.aiinference.support.image.intefaces.ImageDrawAction;
import lombok.Data;

import java.awt.*;

/**
 * author: Luxb
 * create: 2025/6/19 17:51
 **/
@Data
public class ImageDrawRectangleAction implements ImageDrawAction {
    private int x;
    private int y;
    private int width;
    private int height;

    public ImageDrawRectangleAction(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public void draw(Graphics2D g2d) {
        g2d.drawRect(x, y, width, height);
    }
}
