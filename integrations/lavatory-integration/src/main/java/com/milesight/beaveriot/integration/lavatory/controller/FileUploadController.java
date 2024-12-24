package com.milesight.beaveriot.integration.lavatory.controller;

import com.milesight.beaveriot.base.response.ResponseBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@RestController
@RequestMapping("/public/integration/lavatory/file")
public class FileUploadController {

    @Value("${file.upload-dir}")
    private String uploadDir;
    @PostMapping("/upload")
    public com.milesight.beaveriot.base.response.ResponseBody<Object> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            // 检查上传目录是否存在，不存在则创建
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 检查文件名是否包含无效字符
            if (fileName.contains("..")) {
                return ResponseBuilder.fail("500", "Filename contains invalid path sequence " + fileName);
            }

            // 保存文件
            Path targetLocation = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // 构建文件访问URL
            String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/public/integration/lavatory/file/images/")
                    .path(fileName)
                    .toUriString();
            return ResponseBuilder.success(fileDownloadUri);
        } catch (IOException ex) {
            ex.printStackTrace(); // 打印堆栈跟踪以便调试
            return ResponseBuilder.fail("500", "Could not store file " + fileName + ". Please try again!");
        }
    }

    @GetMapping("/images/{fileName:.+}")
    @ResponseBody
    public ResponseEntity<Resource> getImage(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } catch (MalformedURLException ex) {
            ex.printStackTrace(); // 打印堆栈跟踪以便调试
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
}