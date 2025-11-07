package com.milesight.beaveriot.integrations.camthinkaiinference.support.image;

import com.milesight.beaveriot.integrations.camthinkaiinference.support.ImageSupport;
import com.milesight.beaveriot.integrations.camthinkaiinference.support.image.config.ImageDrawConfig;
import com.milesight.beaveriot.integrations.camthinkaiinference.support.image.intefaces.ImageDrawAction;
import lombok.Data;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;

/**
 * author: Luxb
 * create: 2025/6/19 17:43
 **/
@Data
public class ImageDrawEngine {
    private static final String IMAGE_BASE64_HEADER_FORMAT = "data:image/{0};base64";
    private static final String IMAGE_SUFFIX_JPEG = "jpeg";
    private static final String IMAGE_SUFFIX_JPG = "jpg";
    private static final Set<String> IMAGE_JPEG_SET = Set.of(IMAGE_SUFFIX_JPEG, IMAGE_SUFFIX_JPG);
    private static final float JPEG_COMPRESSION_QUALITY = 1.0f;
    private static final String DEFAULT_IMAGE_SUFFIX = IMAGE_SUFFIX_JPEG;
    private static final String DEFAULT_IMAGE_BASE64_HEADER = MessageFormat.format(IMAGE_BASE64_HEADER_FORMAT, DEFAULT_IMAGE_SUFFIX);
    private ImageDrawConfig config;
    private BufferedImage image;
    private Graphics2D g2d;
    private String imageBase64Header;
    private String outputBase64Data;
    private List<ImageDrawAction> actions;
    private ColorManager colorManager;

    public ImageDrawEngine(ImageDrawConfig config) {
        this.config = config;
        this.actions = new ArrayList<>();
        this.colorManager = new ColorManager();
    }

    @SuppressWarnings("UnusedReturnValue")
    public ImageDrawEngine loadImageFromBase64(String imageBase64) throws IOException {
        String[] extractedData = ImageSupport.extractImageBase64(imageBase64);
        imageBase64Header = extractedData[0];
        String base64Data = extractedData[1];

        byte[] imageBytes = Base64.getDecoder().decode(base64Data);
        ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
        BufferedImage originImage = ImageIO.read(bis);
        image = new BufferedImage(
                originImage.getWidth(), originImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2d.drawImage(originImage, 0, 0, null);

        g2d.setColor(config.getLineColor());
        g2d.setStroke(new BasicStroke(config.getLineWidth()));

        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public ImageDrawEngine addAction(ImageDrawAction action) {
        if (!colorManager.isRegistered(action.getClass())) {
            colorManager.register(action.getClass(), action.getColorPickerMap());
        }
        actions.add(action);
        return this;
    }

    public ImageDrawEngine draw() throws IOException {
        for (ImageDrawAction action : actions) {
            action.draw(g2d, colorManager);
        }
        g2d.dispose();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        String imageSuffix = ImageSupport.getImageSuffixFromImageBase64Header(imageBase64Header);

        if (IMAGE_JPEG_SET.contains(imageSuffix.toLowerCase())) {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(IMAGE_SUFFIX_JPEG);
            if (writers.hasNext()) {
                ImageWriter writer = writers.next();
                try (ImageOutputStream ios = ImageIO.createImageOutputStream(bos)) {
                    writer.setOutput(ios);
                    ImageWriteParam param = writer.getDefaultWriteParam();
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(JPEG_COMPRESSION_QUALITY);
                    writer.write(null, new IIOImage(image, null, null), param);
                } finally {
                    writer.dispose();
                }
            }
        } else {
            ImageIO.write(image, imageSuffix, bos);
        }

        outputBase64Data = Base64.getEncoder().encodeToString(bos.toByteArray());
        return this;
    }

    @SuppressWarnings("unused")
    public String outputBase64Data() {
        return outputBase64Data;
    }

    public String outputImageBase64() {
        return imageBase64Header == null ? composeImageBase64(DEFAULT_IMAGE_BASE64_HEADER, outputBase64Data):
                composeImageBase64(imageBase64Header, outputBase64Data);
    }

    @SuppressWarnings("unused")
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
        String[] extractedData = ImageSupport.extractImageBase64(imageBase64);
        String base64Data = extractedData[1];

        byte[] imageData = Base64.getDecoder().decode(base64Data);

        try (FileOutputStream imageOutFile = new FileOutputStream(outputPath)) {
            imageOutFile.write(imageData);
        }
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

    private static String composeImageBase64(String prefix, String base64Data) {
        return prefix + "," + base64Data;
    }
}
