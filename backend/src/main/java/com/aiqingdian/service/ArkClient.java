package com.aiqingdian.service;

import com.aiqingdian.config.ArkProperties;
import com.aiqingdian.exception.ArkException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 火山方舟（豆包视觉模型）客户端，使用 OpenAI 兼容的 chat/completions 接口。
 */
@Service
public class ArkClient {

    private static final Logger log = LoggerFactory.getLogger(ArkClient.class);

    private static final String PLACEHOLDER_KEY = "YOUR_ARK_API_KEY";

    private final ArkProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ArkClient(ArkProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMs());
        factory.setReadTimeout(properties.getReadTimeoutMs());
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 发送一张图片（Base64）并返回模型回答文本。
     *
     * @param base64Image 图片 Base64 数据（不含 data: 前缀）
     * @param mimeType    图片 MIME 类型，如 image/jpeg
     * @param systemPrompt 系统提示词
     * @param userPrompt  用户提示词
     * @return 模型返回的文本内容
     */
    public String chat(String base64Image, String mimeType, String systemPrompt, String userPrompt) {
        String apiKey = properties.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty() || PLACEHOLDER_KEY.equals(apiKey.trim())) {
            throw new ArkException("尚未配置火山方舟 API Key，请在 application.yml 的 ark.api-key 或环境变量 ARK_API_KEY 中配置");
        }

        try {
            Map<String, Object> body = new LinkedHashMap<String, Object>();
            body.put("model", properties.getModel());

            Map<String, Object> systemMessage = new LinkedHashMap<String, Object>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);

            Map<String, Object> textPart = new LinkedHashMap<String, Object>();
            textPart.put("type", "text");
            textPart.put("text", userPrompt);

            Map<String, Object> imageUrl = new LinkedHashMap<String, Object>();
            imageUrl.put("url", "data:" + mimeType + ";base64," + base64Image);
            Map<String, Object> imagePart = new LinkedHashMap<String, Object>();
            imagePart.put("type", "image_url");
            imagePart.put("image_url", imageUrl);

            Map<String, Object> userMessage = new LinkedHashMap<String, Object>();
            userMessage.put("role", "user");
            userMessage.put("content", Arrays.asList(textPart, imagePart));

            body.put("messages", Arrays.asList(systemMessage, userMessage));
            body.put("temperature", 0.1);
            body.put("max_tokens", properties.getMaxTokens());

            // 深度思考模式：disabled=关闭思考、直接输出（最快），auto=自动，enabled=强制开启
            Map<String, Object> thinking = new LinkedHashMap<String, Object>();
            thinking.put("type", properties.getThinkingType());
            body.put("thinking", thinking);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String requestBody = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<String>(requestBody, headers);

            log.info("调用火山方舟模型：{}，图片 Base64 长度：{}", properties.getModel(), base64Image.length());
            ResponseEntity<String> response = restTemplate.postForEntity(properties.getBaseUrl(), entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()) {
                throw new ArkException("模型返回内容为空");
            }
            return content.asText();
        } catch (HttpStatusCodeException e) {
            String detail = e.getResponseBodyAsString();
            if (detail != null && detail.length() > 500) {
                detail = detail.substring(0, 500);
            }
            log.error("火山方舟返回错误状态码 {}：{}", e.getStatusCode(), detail);
            throw new ArkException("火山方舟接口调用失败（HTTP " + e.getStatusCode().value() + "）：" + detail);
        } catch (ArkException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用火山方舟接口异常", e);
            throw new ArkException("调用火山方舟接口异常：" + e.getMessage(), e);
        }
    }
}
