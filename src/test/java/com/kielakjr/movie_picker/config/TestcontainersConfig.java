package com.kielakjr.movie_picker.config;

import com.kielakjr.movie_picker.ai.EmbeddingClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    static PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("pgvector/pgvector:pg17")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")
                .withInitScript("init-pgvector.sql");
    }

    @Bean
    EmbeddingClient embeddingClient() {
        EmbeddingClient mock = mock(EmbeddingClient.class);
        when(mock.getEmbedding(anyString())).thenReturn(new float[384]);
        return mock;
    }
}
