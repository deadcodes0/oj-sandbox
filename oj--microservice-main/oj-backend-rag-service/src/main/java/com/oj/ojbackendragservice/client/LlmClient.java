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
 * 大模型服务 REST 客户端（OpenAI 兼容接口，base-url + /chat/completions、/embeddings）
 */
@Component
public class LlmClient {

    @Resource
    private RagConfig ragConfig;

    private final Gson gson = new Gson();

    /**
     * 文本向量化
     *
     * @param text 文本
     * @return 向量
     */
    public List<Float> embed(String text) {
        RagConfig.LlmConfig c = ragConfig.getLlm();
        Map<String, Object> body = new HashMap<>();
        body.put("model", c.getEmbeddingModel());
        body.put("input", text);
        HttpResponse resp = HttpRequest.post(c.getBaseUrl() + "/embeddings")
                .header("Authorization", "Bearer " + c.getApiKey())
                .body(gson.toJson(body))
                .timeout(c.getTimeout())
                .execute();
        if (resp.getStatus() != 200) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "模型服务调用失败（embeddings）：" + resp.body());
        }
        return parseEmbedding(resp.body());
    }

    /**
     * 解析 /embeddings 响应
     */
    public static List<Float> parseEmbedding(String json) {
        JsonObject root = new Gson().fromJson(json, JsonObject.class);
        JsonArray dataArr = root.getAsJsonArray("data");
        if (dataArr == null || dataArr.size() == 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "模型服务返回异常（embeddings）");
        }
        JsonArray embeddingArr = dataArr.get(0).getAsJsonObject().getAsJsonArray("embedding");
        List<Float> vector = new ArrayList<>();
        for (JsonElement element : embeddingArr) {
            vector.add(element.getAsFloat());
        }
        return vector;
    }

    /**
     * 对话生成
     *
     * @param messages OpenAI 消息列表
     * @return 模型回复内容
     */
    public String chat(List<Map<String, String>> messages) {
        RagConfig.LlmConfig c = ragConfig.getLlm();
        Map<String, Object> body = new HashMap<>();
        body.put("model", c.getChatModel());
        body.put("messages", messages);
        body.put("stream", false);
        HttpResponse resp = HttpRequest.post(c.getBaseUrl() + "/chat/completions")
                .header("Authorization", "Bearer " + c.getApiKey())
                .body(gson.toJson(body))
                .timeout(c.getTimeout())
                .execute();
        if (resp.getStatus() != 200) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "模型服务调用失败（chat）：" + resp.body());
        }
        return parseChatContent(resp.body());
    }

    /**
     * 解析 /chat/completions 响应
     */
    public static String parseChatContent(String json) {
        JsonObject root = new Gson().fromJson(json, JsonObject.class);
        JsonArray choices = root.getAsJsonArray("choices");
        if (choices == null || choices.size() == 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "模型服务返回异常（chat）");
        }
        JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
        return message.get("content").getAsString();
    }
}
