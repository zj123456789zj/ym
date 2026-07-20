package com.yumian.utils;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;

public class VectorUtils {

    /** float[] → JSON 字符串（存入数据库） */
    public static String toJson(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    /** JSON 字符串 → float[]（从数据库读取） */
    public static float[] fromJson(String json) {
        JSONArray arr = JSONUtil.parseArray(json);
        float[] result = new float[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            result[i] = arr.getDouble(i).floatValue();
        }
        return result;
    }

    /** 余弦相似度 */
    public static double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("向量维度不匹配: " + a.length + " vs " + b.length);
        }
        double dotProduct = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0 ? 0 : dotProduct / denom;
    }
}
