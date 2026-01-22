package com.approval.opsagent.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Service
public class AiServiceClient {

    private final RestClient rest;
    private final ObjectMapper om = new ObjectMapper();

    public AiServiceClient(AiServiceProperties props) {
        // ✅ HTTP/2 업그레이드(h2c) 같은 거 안 하게 HTTP/1.1로 고정
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(60));

        this.rest = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(props.getBaseUrl())
                .build();
    }

    public String createPlan(Object payload) {
        try {
            // ✅ “객체 직렬화가 안 되어서 바디가 비는” 상황을 막기 위해 JSON 문자열로 강제 전송
            String json = om.writeValueAsString(payload);

            return rest.post()
                    .uri("/plan")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(json)
                    .retrieve()
                    .body(String.class);

        } catch (Exception e) {
            throw new IllegalStateException("AI request failed: " + e.getMessage(), e);
        }
    }

    public String ragIngest(Object payload) {
        return rest.post()
                .uri("/rag/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);
    }

    public String ragAnswer(Object payload) {
        return rest.post()
                .uri("/rag/answer")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);
    }
}
