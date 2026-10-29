package ma.emsi.zouitina.tp0zouitina.service;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.json.*;
import jakarta.json.stream.JsonGenerator;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Dependent
public class JsonAdapterPourGemini implements Serializable {

    private String systemRole = "You are a helpful assistant.";

    // Historique JSON des questions et des réponses.
    private final List<String> historique = new ArrayList<>();

    @Inject
    private LlmClientPourGemini geminiClient;

    public void setSystemRole(String systemRole) {
        this.systemRole = systemRole;
    }

    public LlmInteraction envoyerRequete(String question)
            throws RequeteException {

        JsonObject requete = creerRequeteJson(question);
        String questionJson = prettyPrinting(requete);

        Entity<String> entity = Entity.entity(
                requete.toString(),
                MediaType.APPLICATION_JSON_TYPE
        );

        try (Response response = geminiClient.envoyerRequete(entity)) {
            String reponseBrute = response.readEntity(String.class);

            if (response.getStatus() != 200) {
                throw new RequeteException(
                        "Erreur HTTP " + response.getStatus()
                                + " : " + reponseBrute
                );
            }

            JsonObject reponseJson = lireJson(reponseBrute);
            JsonArray candidates = reponseJson.getJsonArray("candidates");

            if (candidates == null || candidates.isEmpty()) {
                throw new RequeteException(
                        "Gemini n'a retourné aucune réponse : "
                                + reponseBrute
                );
            }

            JsonObject contenu = candidates.getJsonObject(0)
                    .getJsonObject("content");

            if (contenu == null) {
                throw new RequeteException(
                        "La réponse de Gemini ne contient aucun contenu : "
                                + reponseBrute
                );
            }

            String reponseExtraite = extractReponse(contenu);

            // On conserve l'échange seulement si l'appel a réussi.
            historique.add(creerMessage("user", question).toString());
            historique.add(
                    creerMessage("model", reponseExtraite).toString()
            );

            return new LlmInteraction(
                    reponseExtraite,
                    questionJson,
                    prettyPrinting(reponseJson)
            );

        } catch (RequeteException e) {
            throw e;
        } catch (Exception e) {
            throw new RequeteException(
                    "Impossible de traiter la requête Gemini : "
                            + e.getMessage(),
                    e
            );
        }
    }

    private JsonObject creerRequeteJson(String question) {
        JsonArrayBuilder contents = Json.createArrayBuilder();

        for (String message : historique) {
            contents.add(lireJson(message));
        }

        contents.add(creerMessage("user", question));

        return Json.createObjectBuilder()
                .add("system_instruction", Json.createObjectBuilder()
                        .add("parts", Json.createArrayBuilder()
                                .add(Json.createObjectBuilder()
                                        .add("text", systemRole))))
                .add("contents", contents)
                .build();
    }

    private JsonObject creerMessage(String role, String texte) {
        return Json.createObjectBuilder()
                .add("role", role)
                .add("parts", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("text", texte)))
                .build();
    }

    private String extractReponse(JsonObject contenu)
            throws RequeteException {

        JsonArray parts = contenu.getJsonArray("parts");

        if (parts == null) {
            throw new RequeteException(
                    "La réponse de Gemini ne contient pas de texte."
            );
        }

        StringBuilder texte = new StringBuilder();

        for (JsonValue valeur : parts) {
            JsonObject partie = valeur.asJsonObject();

            if (partie.containsKey("text")
                    && !partie.getBoolean("thought", false)) {
                texte.append(partie.getString("text"));
            }
        }

        if (texte.toString().isBlank()) {
            throw new RequeteException(
                    "Gemini n'a retourné aucun texte à afficher."
            );
        }

        return texte.toString();
    }

    private JsonObject lireJson(String texte) {
        try (JsonReader reader =
                     Json.createReader(new StringReader(texte))) {
            return reader.readObject();
        }
    }

    private String prettyPrinting(JsonObject objet) {
        StringWriter sortie = new StringWriter();

        JsonWriterFactory factory = Json.createWriterFactory(
                Map.of(JsonGenerator.PRETTY_PRINTING, true)
        );

        try (JsonWriter writer = factory.createWriter(sortie)) {
            writer.writeObject(objet);
        }

        return sortie.toString();
    }
}