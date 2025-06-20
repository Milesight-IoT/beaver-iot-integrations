package com.milesight.beaveriot.integrations.aiinference.support.image;

import com.milesight.beaveriot.integrations.aiinference.support.image.config.ImageDrawConfig;
import com.milesight.beaveriot.integrations.aiinference.support.image.intefaces.ImageDrawAction;
import lombok.Data;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * author: Luxb
 * create: 2025/6/19 17:43
 **/
@Data
public class ImageDrawEngine {
    public static final String IMAGE_BASE64_HEADER_FORMAT = "data:image/{0};base64";
    public static final String DEFAULT_IMAGE_SUFFIX = "jpeg";
    public static final String DEFAULT_IMAGE_BASE64_HEADER = MessageFormat.format(IMAGE_BASE64_HEADER_FORMAT, DEFAULT_IMAGE_SUFFIX);
    private ImageDrawConfig config;
    private BufferedImage image;
    private Graphics2D g2d;
    private String imageBase64Header;
    private String outputBase64Data;
    private List<ImageDrawAction> actions;

    public ImageDrawEngine(ImageDrawConfig config) {
        this.config = config;
        this.actions = new ArrayList<>();
    }

    public ImageDrawEngine loadImageFromBase64(String imageBase64) throws IOException {
        String[] extractedData = extractImageBase64(imageBase64);
        imageBase64Header = extractedData[0];
        String base64Data = extractedData[1];

        byte[] imageBytes = Base64.getDecoder().decode(base64Data);
        ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
        image = ImageIO.read(bis);

        g2d = image.createGraphics();
        g2d.setColor(config.getLineColor());
        g2d.setStroke(new BasicStroke(config.getLineWidth()));

        return this;
    }

    public ImageDrawEngine addAction(ImageDrawAction action) {
        actions.add(action);
        return this;
    }

    public ImageDrawEngine draw() throws IOException {
        for (ImageDrawAction action : actions) {
            action.draw(g2d);
        }
        g2d.dispose();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        String imageSuffix;
        if (imageBase64Header == null) {
            imageSuffix = DEFAULT_IMAGE_SUFFIX;
        } else {
            imageSuffix = getImageSuffixFromImageBase64Header(imageBase64Header);
        }
        ImageIO.write(image, imageSuffix, bos);

        outputBase64Data = Base64.getEncoder().encodeToString(bos.toByteArray());
        return this;
    }

    public String outputBase64Data() {
        return outputBase64Data;
    }

    public String outputImageBase64() {
        return imageBase64Header == null ? composeImageBase64(DEFAULT_IMAGE_BASE64_HEADER, outputBase64Data):
                composeImageBase64(imageBase64Header, outputBase64Data);
    }

    public static String convertImageToBase64(String filePath) throws IOException {
        File file = new File(filePath);
        String base64Data;
        String imageSuffix = getImageSuffix(filePath);

        try (FileInputStream imageInFile = new FileInputStream(file)) {
            byte[] imageData = new byte[(int) file.length()];
            int bytes = imageInFile.read(imageData);
            if (bytes < 0) {
                throw new IOException("No bytes read from image file");
            }
            base64Data = Base64.getEncoder().encodeToString(imageData);
        }

        return composeImageBase64(getImageBase64Header(imageSuffix), base64Data);
    }

    public static void convertBase64ToImage(String imageBase64, String outputPath) throws IOException {
        String[] extractedData = extractImageBase64(imageBase64);
        String base64Data = extractedData[1];

        byte[] imageData = Base64.getDecoder().decode(base64Data);

        try (FileOutputStream imageOutFile = new FileOutputStream(outputPath)) {
            imageOutFile.write(imageData);
        }
    }

    private static String getImageSuffixFromImageBase64Header(String imageBase64Header) {
        return imageBase64Header.substring(imageBase64Header.indexOf('/') + 1, imageBase64Header.indexOf(';'));
    }

    private static String getImageBase64Header(String imageSuffix) {
        return MessageFormat.format(IMAGE_BASE64_HEADER_FORMAT, imageSuffix);
    }

    private static String getImageSuffix(String filePath) {
        if (filePath.contains(".") && filePath.lastIndexOf(".") < filePath.length() - 1) {
            return filePath.substring(filePath.lastIndexOf(".") + 1);
        } else {
            return DEFAULT_IMAGE_SUFFIX;
        }
    }

    private static String[] extractImageBase64(String imageBase64) {
        if (imageBase64 == null) {
            throw new IllegalArgumentException("Invalid Data URI format");
        }

        if (!imageBase64.contains(",")) {
            return new String[]{null, imageBase64};
        }

        String imageBase64Header = imageBase64.substring(0, imageBase64.indexOf(','));
        String base64Data = imageBase64.substring(imageBase64.indexOf(',') + 1);

        return new String[]{imageBase64Header, base64Data};
    }

    private static String composeImageBase64(String prefix, String base64Data) {
        return prefix + "," + base64Data;
    }
}
