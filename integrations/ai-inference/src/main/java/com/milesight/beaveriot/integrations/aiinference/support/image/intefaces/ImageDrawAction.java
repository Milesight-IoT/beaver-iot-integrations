package com.milesight.beaveriot.integrations.aiinference.support.image.intefaces;

import com.milesight.beaveriot.integrations.aiinference.support.image.ColorManager;
import com.milesight.beaveriot.integrations.aiinference.support.image.ColorPicker;

import java.awt.*;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/6/19 17:49
 **/
public interface ImageDrawAction {
    Map<String, ColorPicker> getColorPickerMap();
    void draw(Graphics2D g2d, ColorManager colorManager);
}
