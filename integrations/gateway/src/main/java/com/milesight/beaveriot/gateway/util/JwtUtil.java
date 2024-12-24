package com.milesight.beaveriot.gateway.util;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;

import java.util.Map;

public class JwtUtil {

    /**
     * 解码JWT（不验证签名）
     *
     * @param token JWT字符串
     * @return 解析后的JWT内容
     */
    public static Map<String, Object> decodeJwt(String token) {
        // 使用Hutool JWT工具类解析JWT
        JWT jwt = JWTUtil.parseToken(token);

        // 获取JWT的头部
        Map<String, Object> header = jwt.getHeaders();
        // 获取JWT的有效载荷
        Map<String, Object> payload = jwt.getPayloads();

        // 打印头部和有效载荷
        System.out.println("JWT头部: " + header);
        System.out.println("JWT有效载荷: " + payload);

        return payload;
    }

    public static void main(String[] args) {
        // 示例JWT
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJsb3JhLWFwcC1zZXJ2ZXIiLCJleHAiOjE3MzIzNDQ2MjQsImlzcyI6ImxvcmEtYXBwLXNlcnZlciIsIm5iZiI6MTczMjI1ODIyNCwic3ViIjoidXNlciIsInVzZXJuYW1lIjoiYWRtaW4ifQ.Th35E-VYWfsYuvej9rgWf-P6rqxcafU6gGPNIHvb8OI";

        try {
            decodeJwt(token);
        } catch (Exception e) {
            System.err.println("JWT解码失败: " + e.getMessage());
        }
    }

}
