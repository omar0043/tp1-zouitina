package ma.emsi.zouitina.tp0zouitina.service;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

@Dependent
public class LlmClientPourGemini implements Serializable {

    public Response envoyerRequete(Entity<String> requestEntity) {
        String key = System.getenv("GEMINI_KEY");

        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "La variable GEMINI_KEY est absente."
            );
        }

        return getTarget()
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header("x-goog-api-key", key)
                .post(requestEntity);
    }

    private transient Client clientRest;

    private WebTarget getTarget() {
        if (clientRest == null) {
            clientRest = ClientBuilder.newBuilder()
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .build();
        }

        return clientRest.target(
                "https://generativelanguage.googleapis.com/v1beta/"
                        + "models/gemini-3.5-flash-lite:generateContent"
        );
    }

    @PreDestroy
    public void closeClient() {
        if (clientRest != null) {
            clientRest.close();
            clientRest = null;
        }
    }
}