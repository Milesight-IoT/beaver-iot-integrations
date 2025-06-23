package com.milesight.beaveriot.integrations.aiinference.support.image.action;

import com.milesight.beaveriot.integrations.aiinference.support.image.ColorManager;
import com.milesight.beaveriot.integrations.aiinference.support.image.ColorPicker;
import com.milesight.beaveriot.integrations.aiinference.support.image.intefaces.ImageDrawAction;
import lombok.Data;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * author: Luxb
 * create: 2025/6/20 09:06
 **/
@Data
public class ImageDrawPathAction implements ImageDrawAction {
    private final static float DEFAULT_LINE_WIDTH = 2.0f;
    private static final int DEFAULT_POINT_DIAMETER = 10;
    private final static String LINE_COLOR_FIELD = "line";
    private final static String POINT_COLOR_FIELD = "point";
    private Map<Integer, Point> pointMap;
    private List<Line> lineList;
    private GeneralPath path;

    public ImageDrawPathAction() {
        path = new GeneralPath();
        pointMap = new HashMap<>();
        lineList = new ArrayList<>();
    }

    public ImageDrawPathAction addPoint(int x, int y, int pointId) {
        pointMap.put(pointId, new Point(x, y));
        return this;
    }

    public ImageDrawPathAction addLine(int startPointId, int endPointId) {
        lineList.add(new Line(startPointId, endPointId));
        return this;
    }

    @Override
    public Map<String, ColorPicker> getColorPickerMap() {
        return Map.of(
                LINE_COLOR_FIELD, new ColorPicker(4),
                POINT_COLOR_FIELD, new ColorPicker()
        );
    }

    @Override
    public void draw(Graphics2D g2d, ColorManager colorManager) {
        g2d.setStroke(new BasicStroke(DEFAULT_LINE_WIDTH));

        ColorPicker lineColorPicker = colorManager.getColorPicker(this.getClass(), LINE_COLOR_FIELD);
        g2d.setColor(lineColorPicker.nextColor());
        for (Line line : lineList) {
            Point startPoint = pointMap.get(line.getStartPointId());
            Point endPoint = pointMap.get(line.getEndPointId());
            if (startPoint != null && endPoint != null) {
                path.moveTo(startPoint.getX(), startPoint.getY());
                path.lineTo(endPoint.getX(), endPoint.getY());
            }
        }
        g2d.draw(path);

        ColorPicker pointColorPicker = colorManager.getColorPicker(this.getClass(), POINT_COLOR_FIELD);
        for (Point point : pointMap.values()) {
            g2d.setColor(pointColorPicker.nextColor());
            g2d.fillOval((int)point.getX() - DEFAULT_POINT_DIAMETER / 2, (int)point.getY() - DEFAULT_POINT_DIAMETER / 2, DEFAULT_POINT_DIAMETER, DEFAULT_POINT_DIAMETER);
        }
    }

    @Data
    public static class Line {
        private int startPointId;
        private int endPointId;

        public Line(int startPointId, int endPointId) {
            this.startPointId = startPointId;
            this.endPointId = endPointId;
        }
    }
}
