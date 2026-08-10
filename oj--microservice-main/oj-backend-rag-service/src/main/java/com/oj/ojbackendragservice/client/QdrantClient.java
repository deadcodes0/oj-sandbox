package com.oj.ojbackendragservice.client;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.oj.ojbackendcommon.common.ErrorCode;
import com.oj.ojbackendcommon.exception.BusinessException;
import com.oj.ojbackendragservice.config.RagConfig;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Qdrant 向量库 REST 客户端（hutool HttpRequest，不引入官方 SDK，保持 Java 8 兼容）
 */
@Component
public class QdrantClient {

    @Resource
    private RagConfig ragConfig;

    private final Gson gson = new Gson();

    private String baseUrl() {
        RagConfig.QdrantConfig c = ragConfig.getQdrant();
        return "http://" + c.getHost() + ":" + c.getPort();
    }

    private String collectionUrl() {
        return baseUrl() + "/collections/" + ragConfig.getQdrant().getCollectionName();
    }

    /**
     * 确保集合存在，不存在则创建（Cosine 距离）
     */
    public void ensureCollection() {
        HttpResponse getResp = HttpRequest.get(collectionUrl()).timeout(5000).execute();
        if (getResp.getStatus() == 200) {
            return;
        }
        Map<String, Object> vectors = new HashMap<>();
        vectors.put("size", ragConfig.getQdrant().getDimension());
        vectors.put("distance", "Cosine");
        Map<String, Object> body = new HashMap<>();
        body.put("vectors", vectors);
        HttpResponse resp = HttpRequest.put(collectionUrl())
                .body(gson.toJson(body))
                .timeout(5000)
                .execute();
        if (resp.getStatus() != 200) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Qdrant 创建集合失败：" + resp.body());
        }
    }

    /**
     * 写入/更新单个向量点
     */
    public void upsertPoint(long id, List<Float> vector, Map<String, Object> payload) {
        ensureCollection();
        Map<String, Object> point = new HashMap<>();
        point.put("id", id);
        point.put("vector", vector);
        point.put("payload", payload);
        List<Map<String, Object>> points = new ArrayList<>();
        points.add(point);
        Map<String, Object> body = new HashMap<>();
        body.put("points", points);
        HttpResponse resp = HttpRequest.put(collectionUrl() + "/points?wait=true")
                .body(gson.toJson(body))
                .timeout(15000)
                .execute();
        if (resp.getStatus() != 200) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Qdrant 写入向量失败：" + resp.body());
        }
    }

    /**
     * 删除单个向量点（集合不存在视为已删除）
     */
    public void deletePoint(long id) {
        Map<String, Object> body = new HashMap<>();
        List<Long> ids = new ArrayList<>();
        ids.add(id);
        body.put("points", ids);
        HttpResponse resp = HttpRequest.post(collectionUrl() + "/points/delete?wait=true")
                .body(gson.toJson(body))
                .timeout(5000)
                .execute();
        if (resp.getStatus() != 200 && resp.getStatus() != 404) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Qdrant 删除向量失败：" + resp.body());
        }
    }

    /**
     * 相似向量检索，返回命中的向量点
     */
    public List<SearchResult> search(List<Float> vector, int limit) {
        Map<String, Object> body = new HashMap<>();
        body.put("vector", vector);
        body.put("limit", limit);
        body.put("with_payload", true);
        HttpResponse resp = HttpRequest.post(collectionUrl() + "/points/search")
                .body(gson.toJson(body))
                .timeout(10000)
                .execute();
        if (resp.getStatus() != 200) {
            if (resp.getStatus() == 404) {
                // 集合尚未创建，视为无结果
                return new ArrayList<>();
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Qdrant 检索失败：" + resp.body());
        }
        return parseSearchResult(resp.body());
    }

    /**
     * 解析 /points/search 响应
     */
    public static List<SearchResult> parseSearchResult(String json) {
        List<SearchResult> results = new ArrayList<>();
        JsonObject root = new Gson().fromJson(json, JsonObject.class);
        JsonArray resultArr = root.getAsJsonArray("result");
        if (resultArr == null) {
            return results;
        }
        for (JsonElement element : resultArr) {
            JsonObject obj = element.getAsJsonObject();
            long id = obj.get("id").getAsLong();
            double score = obj.get("score").getAsDouble();
            JsonObject payload = obj.has("payload") ? obj.getAsJsonObject("payload") : new JsonObject();
            results.add(new SearchResult(id, score, payload));
        }
        return results;
    }

    /**
     * 删除整个集合（rebuild 前清空，不存在则忽略）
     */
    public void deleteCollection() {
        HttpResponse resp = HttpRequest.delete(collectionUrl()).timeout(5000).execute();
        int status = resp.getStatus();
        if (status != 200 && status != 404) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Qdrant 删除集合失败：" + resp.body());
        }
    }

    /**
     * 检索结果
     */
    public static class SearchResult {
        private final long id;
        private final double score;
        private final JsonObject payload;

        public SearchResult(long id, double score, JsonObject payload) {
            this.id = id;
            this.score = score;
            this.payload = payload;
        }

        public long getId() {
            return id;
        }

        public double getScore() {
            return score;
        }

        public JsonObject getPayload() {
            return payload;
        }
    }
}
