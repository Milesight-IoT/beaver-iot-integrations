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
    private final static float FONT_SIZE = 20.0f;
    private final static int FONT_BOX_PADDING_LEFT_AND_RIGHT = 6;
    private final static int FONT_BOX_PADDING_TOP_AND_BOTTOM = 4;
    private final static float FONT_BOX_TRANSPARENCY = 1.0f;
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
        Color color = colorPicker.nextColor();
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(DEFAULT_LINE_WIDTH));
        int rectX = x - (int) (DEFAULT_LINE_WIDTH / 2);
        int rectY = y - (int) (DEFAULT_LINE_WIDTH / 2);
        int rectWidth = width + (int) DEFAULT_LINE_WIDTH;
        int rectHeight = height + (int) (DEFAULT_LINE_WIDTH);
        g2d.drawRect(rectX, rectY, rectWidth, rectHeight);

        Font originalFont = g2d.getFont();
        Font newFont = originalFont.deriveFont(FONT_SIZE);
        g2d.setFont(newFont);

        FontMetrics fontMetrics = g2d.getFontMetrics();

        int textWidth = fontMetrics.stringWidth(text);
        int textHeight = fontMetrics.getHeight();
        int descent = fontMetrics.getDescent();

        Color textBoxColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (255 * FONT_BOX_TRANSPARENCY));
        g2d.setColor(textBoxColor);
        int outsideTextWidth = textWidth + 2 * FONT_BOX_PADDING_LEFT_AND_RIGHT;
        int outsideTextHeight = textHeight + 2 * FONT_BOX_PADDING_TOP_AND_BOTTOM;
        int textFillRectWidth = outsideTextWidth - (int) DEFAULT_LINE_WIDTH;
        int textFillRectHeight = outsideTextHeight - (int) DEFAULT_LINE_WIDTH;
        int textFillRectY = rectY - outsideTextHeight + 1;
        g2d.drawRect(rectX, textFillRectY, textFillRectWidth, textFillRectHeight);
        g2d.fillRect(rectX, textFillRectY, textFillRectWidth, textFillRectHeight);

        g2d.setColor(Color.BLACK);
        int textX = x - (int) DEFAULT_LINE_WIDTH + FONT_BOX_PADDING_LEFT_AND_RIGHT;
        int textY = y - (int) DEFAULT_LINE_WIDTH - descent - FONT_BOX_PADDING_TOP_AND_BOTTOM;
        g2d.drawString(text, textX, textY);
    }
}
