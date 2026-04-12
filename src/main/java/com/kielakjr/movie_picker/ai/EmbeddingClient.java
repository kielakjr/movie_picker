package com.kielakjr.movie_picker.ai;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class EmbeddingClient {

    private final RestClient restClient;

    public EmbeddingClient() {
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:8000")
                .build();
    }

    public float[] getEmbedding(String text) {
        EmbeddingResponse response = restClient.post()
                .uri("/embedding")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new EmbeddingRequest(text))
                .retrieve()
                .body(EmbeddingResponse.class);
        return response.embedding();
    }

    record EmbeddingRequest(String text) {}
    record EmbeddingResponse(float[] embedding) {}
}
