package org.usfca.medicaid.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.usfca.medicaid.service.RagService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Route("")
@PageTitle("Medicaid Assistant")
public class ChatView extends VerticalLayout {

    private final RagService ragService;
    private final List<String> conversationHistory;
    private final VerticalLayout messageContainer;
    private final TextArea inputField;
    private final Button sendButton;
    private Div loadingIndicator;

    public ChatView(RagService ragService) {
        this.ragService = ragService;
        this.conversationHistory = new ArrayList<>();

        addClassName("chat-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Medicaid Chat Assistant");

        messageContainer = new VerticalLayout();
        messageContainer.addClassName("messages");
        messageContainer.setSpacing(false);
        messageContainer.setPadding(false);
        messageContainer.setWidthFull();

        Scroller scroller = new Scroller(messageContainer);
        scroller.setSizeFull();

        inputField = new TextArea();
        inputField.setWidthFull();
        inputField.setPlaceholder("Ask a question about Minnesota Medicaid...");
        inputField.setMinHeight("120px");

        sendButton = new Button("Send", event -> handleSend());
        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout inputBar = new HorizontalLayout(inputField, sendButton);
        inputBar.setWidthFull();
        inputBar.setPadding(false);
        inputBar.setSpacing(true);
        inputBar.setAlignItems(Alignment.END);

        add(title, scroller, inputBar);
        expand(scroller);

        inputField.focus();
    }

    private void handleSend() {
        String userMessage = inputField.getValue() != null ? inputField.getValue().trim() : "";
        if (userMessage.isEmpty()) {
            return;
        }

        appendMessage("You", userMessage);
        conversationHistory.add("User: " + userMessage);

        String messageCopy = userMessage;
        inputField.clear();
        setInputEnabled(false);

        showLoadingIndicator();

        UI currentUI = UI.getCurrent();

        CompletableFuture.supplyAsync(() -> {
            try {
                String result = ragService.generateResponse(messageCopy, new ArrayList<>(conversationHistory));
                return result;
            } catch (Exception ex) {
                System.err.println("Error in RAG service: " + ex.getMessage());
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
        }).thenAccept(response -> {
            currentUI.access(() -> {
                try {
                    conversationHistory.add("Assistant: " + response);
                    hideLoadingIndicator();
                    appendMessage("Assistant", response);
                    setInputEnabled(true);
                    inputField.focus();
                } catch (Exception e) {
                    System.err.println("Error updating UI: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }).exceptionally(ex -> {
            System.err.println("Exception in async chain: " + ex.getMessage());
            currentUI.access(() -> {
                hideLoadingIndicator();
                Notification.show("Something went wrong: " + ex.getCause().getMessage(), 5000, Notification.Position.MIDDLE);
                setInputEnabled(true);
                inputField.focus();
            });
            return null;
        });
    }

    private void appendMessage(String speaker, String text) {
        Paragraph message = new Paragraph(speaker + ": " + text);
        message.getElement().getThemeList().add("small");
        message.setWidthFull();
        messageContainer.add(message);
    }

    private void setInputEnabled(boolean enabled) {
        inputField.setEnabled(enabled);
        sendButton.setEnabled(enabled);
    }
    
    private void showLoadingIndicator() {
        if (loadingIndicator == null) {
            loadingIndicator = new Div();
            loadingIndicator.addClassName("loading-indicator");

            Span assistantLabel = new Span("Assistant: ");
            
            Span dot1 = new Span("●");
            Span dot2 = new Span("●");
            Span dot3 = new Span("●");
            
            dot1.addClassName("loading-dot");
            dot2.addClassName("loading-dot");
            dot3.addClassName("loading-dot");

            dot1.getStyle().set("animation-delay", "0s");
            dot2.getStyle().set("animation-delay", "0.2s");
            dot3.getStyle().set("animation-delay", "0.4s");
            
            Div dotsContainer = new Div(dot1, dot2, dot3);
            dotsContainer.getStyle()
                .set("display", "inline-block")
                .set("margin-left", "5px");
            
            loadingIndicator.add(assistantLabel, dotsContainer);

            loadingIndicator.getElement().getThemeList().add("small");
            loadingIndicator.setWidthFull();
        }
        
        messageContainer.add(loadingIndicator);

        getElement().executeJs(
            "const style = document.createElement('style');" +
            "style.textContent = `" +
            ".loading-dot {" +
            "  animation: blink 1.4s infinite both;" +
            "  margin: 0 2px;" +
            "}" +
            "@keyframes blink {" +
            "  0%, 80%, 100% { opacity: 0.3; }" +
            "  40% { opacity: 1; }" +
            "}" +
            "`;" +
            "if (!document.querySelector('style[data-loading-animation]')) {" +
            "  style.setAttribute('data-loading-animation', 'true');" +
            "  document.head.appendChild(style);" +
            "}"
        );
    }
    
    private void hideLoadingIndicator() {
        if (loadingIndicator != null) {
            messageContainer.remove(loadingIndicator);
            loadingIndicator = null;
        }
    }
}

