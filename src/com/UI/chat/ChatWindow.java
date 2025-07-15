package com.UI.chat;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.IntConsumer;

/**
 * Main chat window UI, redesigned for a clean white theme with cool accents.
 * Functionality unchanged.
 */
public class ChatWindow {
    private static final Path HISTORY_DIR =
            Paths.get(System.getProperty("user.home"), "ChatHistories");

    private final ChatClient client;
    private final String myNick;
    private final ListView<HBox> messageList = new ListView<>();
    private final StackPane messageContainer;
    private final ListView<String> friendList = new ListView<>();
    private final ListView<String> onlineList = new ListView<>();
    private final TextField inputField = new TextField();
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
    private String currentChat = "All";
    private final Map<String, List<String>> historyMap = new HashMap<>();
    private final ReactionManager reactionManager;

    public ChatWindow(String host, int port, String nickname) throws IOException {
        this.myNick = nickname;
        Files.createDirectories(HISTORY_DIR);
        historyMap.put("All", new ArrayList<>(loadHistory("All")));

        client = new ChatClient(host, port, myNick, this::onMessage, this::onUserList);
        messageContainer = new StackPane(messageList);
        reactionManager = new ReactionManager(messageContainer, client, this::refreshSingleMessage);

        friendList.setPrefWidth(200);
        friendList.getItems().add("All");
        friendList.getSelectionModel().select("All");
        friendList.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> switchChat(sel));

        onlineList.setPrefWidth(200);
        inputField.setOnKeyTyped(e -> client.sendMessage("/typing"));

        BorderPane root = buildUI();
        reloadMessages();
        Stage stage = new Stage();
        stage.setTitle("Chat — " + myNick);
        stage.setScene(new Scene(root, 1000, 700));
        stage.show();
    }

    private BorderPane buildUI() {
        // Root pane white
        BorderPane root = new BorderPane();
        root.setBackground(new Background(new BackgroundFill(
                Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY
        )));

        // Sidebar with cool accent
        VBox sidebar = new VBox(20,
                styledSectionLabel("Friends"), friendList,
                styledSectionLabel("Online"), onlineList
        );
        sidebar.setPadding(new Insets(20));
        sidebar.setPrefWidth(220);
        sidebar.setBackground(new Background(new BackgroundFill(
                Color.web("#f8f9fa"), new CornerRadii(0,0,0,10,false), Insets.EMPTY
        )));
        root.setLeft(sidebar);

        // Top bar
        Label title = new Label("Chat Room");
        title.setFont(Font.font("Segoe UI", 28));
        title.setTextFill(Color.web("#212529"));
        TextField addField = new TextField();
        addField.setPromptText("Add friend...");
        addField.setPrefWidth(250);
        Button addBtn = accentButton("+");
        addBtn.setOnAction(e -> {
            String f = addField.getText().trim();
            if (!f.isEmpty()) { client.sendMessage("/addfriend " + f); addField.clear(); }
        });
        HBox topBar = new HBox(10, title, addField, addBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(15));
        root.setTop(topBar);

        // Center messages area
        messageContainer.setBackground(new Background(new BackgroundFill(
                Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY
        )));
        messageList.setStyle(
                "-fx-background-color: white; -fx-control-inner-background: white;"
        );
        root.setCenter(messageContainer);

        // Bottom input
        inputField.setPromptText("Type a message...");
        inputField.setPrefHeight(45);
        inputField.setStyle(
                "-fx-background-radius: 22; -fx-border-radius: 22; " +
                        "-fx-border-color: #ced4da; -fx-border-width: 1;"
        );
        inputField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) sendCurrent(); });
        Button sendBtn = accentButton("Send");
        sendBtn.setOnAction(e -> sendCurrent());
        Button clearBtn = accentButton("Clear");
        clearBtn.setOnAction(e -> client.sendMessage("/clear"));
        HBox bottomBar = new HBox(10, inputField, sendBtn, clearBtn);
        bottomBar.setPadding(new Insets(15));
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        root.setBottom(bottomBar);

        return root;
    }

    private Label styledSectionLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Segoe UI", 18));
        lbl.setTextFill(Color.web("#495057"));
        return lbl;
    }

    private Button accentButton(String text) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Segoe UI", 14));
        btn.setTextFill(Color.WHITE);
        btn.setBackground(new Background(new BackgroundFill(
                Color.web("#0d6efd"), new CornerRadii(22), Insets.EMPTY
        )));
        btn.setPadding(new Insets(8, 20, 8, 20));
        btn.setOnMouseEntered(e -> btn.setBackground(new Background(new BackgroundFill(
                Color.web("#0b5ed7"), new CornerRadii(22), Insets.EMPTY
        ))));
        btn.setOnMouseExited(e -> btn.setBackground(new Background(new BackgroundFill(
                Color.web("#0d6efd"), new CornerRadii(22), Insets.EMPTY
        ))));
        return btn;
    }

    private void sendCurrent() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        inputField.clear();
        if ("All".equals(currentChat)) client.sendMessage(text);
        else client.sendPrivate(currentChat, text);
    }

    private void onMessage(String raw) {
        if ("CLEAR_HISTORY".equals(raw)) { clearCurrentHistory(); return; }
        if (raw.startsWith("USER_LIST ")) return;
        if (raw.startsWith("TYPING ")) {
            String[] p = raw.split(" ",3);
            reactionManager.userTyping(p[1], p[2]); return;
        }
        if (raw.startsWith("REACTION ")) { reactionManager.handleReaction(raw); return; }

        String chatKey = "All";
        if (raw.startsWith("(whisper) ")) {
            int idx = raw.indexOf(": ");
            chatKey = raw.substring("(whisper) ".length(), idx);
        } else if (raw.startsWith("(SYSTEM) Added friend: ")) {
            String f = raw.substring("(SYSTEM) Added friend: ".length());
            if (!friendList.getItems().contains(f)) friendList.getItems().add(f);
        }
        String fmt = String.format("[%s] %s",
                LocalTime.now().format(timeFmt), raw);
        historyMap.computeIfAbsent(chatKey, k->new ArrayList<>()).add(fmt);
        writeHistory(chatKey, fmt);
        if (chatKey.equals(currentChat)) appendMessage(fmt);
    }

    private void onUserList(List<String> users) {
        Platform.runLater(() -> onlineList.getItems().setAll(users));
    }

    private void switchChat(String chat) { currentChat=chat; reloadMessages(); }

    private void reloadMessages() {
        List<String> msgs = historyMap.computeIfAbsent(currentChat, this::loadHistory);
        Platform.runLater(() -> {
            messageList.getItems().clear();
            for (int i=0; i<msgs.size(); i++) {
                messageList.getItems().add(
                        reactionManager.wrapMessage(i, msgs.get(i), msgs.get(i))
                );
            }
        });
    }

    private void refreshSingleMessage(int idx) {
        List<String> msgs = historyMap.get(currentChat); if (idx<0||idx>=msgs.size()) return;
        String raw=msgs.get(idx);
        HBox cell = reactionManager.wrapMessage(idx, raw, raw);
        Platform.runLater(() -> messageList.getItems().set(idx,cell));
    }

    private void appendMessage(String msg) {
        Platform.runLater(() -> messageList.getItems().add(
                reactionManager.wrapMessage(messageList.getItems().size(), msg, msg)
        ));
    }

    private void clearCurrentHistory() {
        historyMap.get(currentChat).clear();
        Platform.runLater(() -> messageList.getItems().clear());
        try { Files.deleteIfExists(HISTORY_DIR.resolve(currentChat+".txt")); }
        catch(IOException e){e.printStackTrace();}
    }

    private List<String> loadHistory(String chat) {
        Path file = HISTORY_DIR.resolve(chat+".txt");
        if (Files.exists(file)) {
            try{return Files.readAllLines(file,StandardCharsets.UTF_8);}catch(IOException e){e.printStackTrace();}
        }
        return new ArrayList<>();
    }

    private void writeHistory(String chat,String line) {
        Path file = HISTORY_DIR.resolve(chat+".txt");
        try{Files.write(file,Collections.singletonList(line),StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(IOException e){e.printStackTrace();}
    }
}
