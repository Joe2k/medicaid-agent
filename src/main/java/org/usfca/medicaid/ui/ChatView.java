package org.usfca.medicaid.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
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

@Route("")
@PageTitle("Medicaid Assistant")
public class ChatView extends VerticalLayout {

    private final RagService ragService;
    private final List<String> conversationHistory;
    private final VerticalLayout messageContainer;
    private final TextArea inputField;
    private final Button sendButton;

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

        setInputEnabled(false);

        try {
            String response = ragService.generateResponse(userMessage, new ArrayList<>(conversationHistory));
            conversationHistory.add("Assistant: " + response);
            appendMessage("Assistant", response);
        } catch (Exception ex) {
            Notification.show("Something went wrong: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
        } finally {
            inputField.clear();
            inputField.focus();
            setInputEnabled(true);
        }
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
}

