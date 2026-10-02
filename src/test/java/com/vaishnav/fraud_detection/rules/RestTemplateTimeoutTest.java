package com.vaishnav.fraud_detection.rules;

import com.sun.net.httpserver.HttpServer;
import com.vaishnav.fraud_detection.config.AppConfig;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestTemplateTimeoutTest {

    @Test
    void shouldGiveUpWhenTheServerAnswersTooSlowly() throws Exception {
        // a tiny local server that waits 3 seconds before answering
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/slow", exchange -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        try {
            int port = server.getAddress().getPort();
            String url = "http://localhost:" + port + "/slow";

            // this RestTemplate only waits half a second
            RestTemplate restTemplate = AppConfig.createRestTemplate(500, 500);

            long start = System.nanoTime();

            assertThrows(ResourceAccessException.class,
                    () -> restTemplate.getForObject(url, String.class));

            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            // it gave up long before the server's 3 seconds were over
            assertTrue(elapsedMs < 2500, "took too long: " + elapsedMs + " ms");
        } finally {
            server.stop(0);
        }
    }
}
