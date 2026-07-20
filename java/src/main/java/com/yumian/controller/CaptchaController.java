package com.yumian.controller;

import com.yumian.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final StringRedisTemplate redisTemplate;

    @GetMapping("/generate")
    public Result<Map<String, String>> generate() {
        // 生成4位随机数
        String code = String.format("%04d", (int) (Math.random() * 10000));

        // 生成图片
        int w = 100, h = 38;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        // 背景
        g.setColor(new Color(240, 244, 248));
        g.fillRect(0, 0, w, h);

        // 干扰线
        g.setColor(new Color(148, 163, 184, 80));
        g.setStroke(new BasicStroke(1));
        for (int i = 0; i < 4; i++) {
            int x1 = (int) (Math.random() * w);
            int y1 = (int) (Math.random() * h);
            int x2 = (int) (Math.random() * w);
            int y2 = (int) (Math.random() * h);
            g.drawLine(x1, y1, x2, y2);
        }

        // 数字
        Color[] colors = {
            new Color(11, 29, 58), new Color(14, 165, 233),
            new Color(245, 158, 11), new Color(16, 185, 129), new Color(139, 92, 246)
        };
        g.setFont(new Font("Arial", Font.BOLD, 22));
        for (int i = 0; i < 4; i++) {
            g.setColor(colors[i % colors.length]);
            double angle = (Math.random() - 0.5) * 0.4;
            g.rotate(angle, 12 + i * 22, 26);
            g.drawString(String.valueOf(code.charAt(i)), 10 + i * 22, 28);
            g.rotate(-angle, 12 + i * 22, 26);
        }
        g.dispose();

        // 转 base64
        String base64;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            return Result.error(500, "生成验证码失败");
        }

        // 存入 Redis（固定 key，每次覆盖，TTL 30秒）
        redisTemplate.opsForValue().set("captcha_code", code, 30, TimeUnit.SECONDS);

        Map<String, String> result = new HashMap<>();
        result.put("captchaImage", "data:image/png;base64," + base64);
        return Result.success(result);
    }
}
