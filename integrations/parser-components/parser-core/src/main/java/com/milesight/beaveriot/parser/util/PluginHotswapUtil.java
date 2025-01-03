package com.milesight.beaveriot.parser.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author linzy
 * 插件热插拔实用程序
 */
@Slf4j
public class PluginHotswapUtil {

    public static List<Class<?>> reloadClass(String targetUrl, Class<?> interfaceClazz) throws Exception {
        // 加载本地jar
        File file = new File(targetUrl);
        URL url = file.toURI().toURL();
        URLClassLoaderProxy classLoader = new URLClassLoaderProxy(new URL[]{}, interfaceClazz.getClassLoader());
        classLoader.addURL(url); // 直接调用代理类中的 addURL 方法

        List<String> classList = new ArrayList<>();
        JarFile jarFile = null;

        // 打开 jar 文件
        try {
            jarFile = new JarFile(file);
            Enumeration<JarEntry> enumeration = jarFile.entries();
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = enumeration.nextElement();
                String name = jarEntry.getName();
                if (name.endsWith(".class")) {
                    String className = name.replace(".class", "").replaceAll("/", ".");
                    classList.add(className);
                }
            }
        } catch (Exception e) {
            log.error("Error opening jar file: " + e.getMessage());
        } finally {
            if (jarFile != null) {
                jarFile.close();
            }
        }

        List<Class<?>> result = new ArrayList<>();
        for (String className : classList) {
            Class<?> clazz = classLoader.loadClass(className);
            if (interfaceClazz.isAssignableFrom(clazz) && !clazz.isInterface() && !clazz.isAnonymousClass()) {
                result.add(clazz);
            }
        }
        // 确保 URLClassLoader 被关闭
        try {
            classLoader.close();
        } catch (Exception e) {
            log.error("Error closing class loader: " + e.getMessage(), e);
        }
        return result;
    }


}
