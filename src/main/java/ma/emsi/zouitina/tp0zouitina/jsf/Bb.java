package ma.emsi.zouitina.tp0zouitina.jsf;

import ma.emsi.zouitina.tp0zouitina.service.JsonAdapterPourGemini;
import ma.emsi.zouitina.tp0zouitina.service.LlmInteraction;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@ViewScoped
public class Bb implements Serializable {

    private String roleSysteme;
    private boolean roleSystemeChangeable = true;
    private List<SelectItem> listeRolesSysteme;

    private String question;
    private String reponse;
    private StringBuilder conversation = new StringBuilder();

    private boolean debug;
    private String texteRequeteJson;
    private String texteReponseJson;

    @Inject
    private JsonAdapterPourGemini jsonAdapter;

    public String envoyer() {
        FacesContext context = FacesContext.getCurrentInstance();

        if (question == null || question.isBlank()) {
            context.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Question vide",
                    "Veuillez saisir une question."
            ));
            return null;
        }

        try {
            if (conversation.isEmpty()) {
                jsonAdapter.setSystemRole(roleSysteme);
            }

            LlmInteraction interaction =
                    jsonAdapter.envoyerRequete(question);

            reponse = interaction.reponseExtraite();
            texteRequeteJson = interaction.questionJson();
            texteReponseJson = interaction.reponseJson();

            roleSystemeChangeable = false;
            afficherConversation();

        } catch (Exception e) {
            reponse = "Erreur : " + e.getMessage();
            texteRequeteJson = "";
            texteReponseJson = "";

            context.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Problème avec l'API Gemini",
                    reponse
            ));
        }

        return null;
    }

    public String nouveauChat() {
        return "index?faces-redirect=true";
    }

    private void afficherConversation() {
        conversation.append("== User:\n")
                .append(question)
                .append("\n== Serveur:\n")
                .append(reponse)
                .append("\n\n");
    }

    public List<SelectItem> getRolesSysteme() {
        if (listeRolesSysteme == null) {
            listeRolesSysteme = new ArrayList<>();

            listeRolesSysteme.add(new SelectItem(
                    """
                    You are a helpful assistant.
                    Answer the user's questions clearly and accurately.
                    """,
                    "Assistant"
            ));

            listeRolesSysteme.add(new SelectItem(
                    """
                    You are an English-French interpreter.
                    Translate French texts into English and English texts
                    into French. For one to three words, also provide
                    examples of usage in English.
                    """,
                    "Traducteur Anglais-Français"
            ));

            listeRolesSysteme.add(new SelectItem(
                    """
                    You are a travel guide.
                    When the user names a country or city, describe
                    its main attractions and give an approximate
                    meal price, specifying the currency.
                    """,
                    "Guide touristique"
            ));
        }

        return listeRolesSysteme;
    }

    public String getRoleSysteme() {
        return roleSysteme;
    }

    public void setRoleSysteme(String roleSysteme) {
        this.roleSysteme = roleSysteme;
    }

    public boolean isRoleSystemeChangeable() {
        return roleSystemeChangeable;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getReponse() {
        return reponse;
    }

    public void setReponse(String reponse) {
        this.reponse = reponse;
    }

    public String getConversation() {
        return conversation.toString();
    }

    public void setConversation(String conversation) {
        this.conversation = new StringBuilder(
                conversation == null ? "" : conversation
        );
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getTexteRequeteJson() {
        return texteRequeteJson;
    }

    public void setTexteRequeteJson(String texteRequeteJson) {
        this.texteRequeteJson = texteRequeteJson;
    }

    public String getTexteReponseJson() {
        return texteReponseJson;
    }

    public void setTexteReponseJson(String texteReponseJson) {
        this.texteReponseJson = texteReponseJson;
    }
}