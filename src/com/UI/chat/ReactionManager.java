package com.UI.chat;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

/**
 * Manages emoji reactions on chat messages and "user is typing" indicators.
 */
public class ReactionManager {
    // message index -> (emoji -> set of users)
    private final Map<Integer, Map<String, Set<String>>> reactions = new ConcurrentHashMap<>();
    private final StackPane messageContainer;
    private final ChatClient client;
    private final IntConsumer refreshCallback;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * @param messageContainer  the Pane containing message list (for typing indicator)
     * @param client            the ChatClient instance to send commands
     * @param refreshCallback   callback to refresh a specific message index
     */
    public ReactionManager(StackPane messageContainer, ChatClient client, IntConsumer refreshCallback) {
        this.messageContainer = messageContainer;
        this.client = client;
        this.refreshCallback = refreshCallback;
    }

    /**
     * Show "sender is typing..." at bottom-left for 2 seconds.
     */
    public void userTyping(String sender, String chatKey) {
        Platform.runLater(() -> {
            Label label = new Label(sender + " is typing...");
            label.setStyle("-fx-text-fill: gray; -fx-padding: 5;");
            StackPane.setAlignment(label, Pos.BOTTOM_LEFT);
            messageContainer.getChildren().add(label);
            scheduler.schedule(() -> Platform.runLater(() -> messageContainer.getChildren().remove(label)),
                    2, TimeUnit.SECONDS);
        });
    }

    /**
     * Wraps a message String into an HBox with reaction bubbles.
     */
    public HBox wrapMessage(int index, String raw, String display) {
        Label text = new Label(display);
        text.setWrapText(true);
        HBox box = new HBox(5, text);
        box.setOnMouseClicked(evt -> {
            if (evt.getButton() == MouseButton.SECONDARY) {
                showMenu(index, evt.getScreenX(), evt.getScreenY());
            }
        });
        Map<String, Set<String>> map = reactions.getOrDefault(index, Collections.emptyMap());
        map.forEach((emoji, users) -> box.getChildren().add(new Label(emoji + " " + users.size())));
        return box;
    }

    /**
     * Show emoji selection menu and send reaction command.
     */
    private void showMenu(int idx, double x, double y) {
        ContextMenu menu = new ContextMenu();
        List<String> emojis = List.of("👍","❤️","😂","🎉","😢");
        for (String e : emojis) {
            MenuItem mi = new MenuItem(e);
            mi.setOnAction(a -> sendReaction(idx, e));
            menu.getItems().add(mi);
        }
        menu.show(messageContainer.getScene().getWindow(), x, y);
    }

    /**
     * Sends a /react command for the given message index and emoji.
     */
    private void sendReaction(int idx, String emoji) {
        client.sendMessage("/react " + idx + " " + emoji);
    }

    /**
     * Handles incoming "REACTION <idx> <emoji> <user>" updates.
     */
    public void handleReaction(String raw) {
        String[] parts = raw.split(" ", 4);
        int idx = Integer.parseInt(parts[1]);
        String emoji = parts[2], user = parts[3];
        reactions
                .computeIfAbsent(idx, i -> new ConcurrentHashMap<>())
                .computeIfAbsent(emoji, k -> ConcurrentHashMap.newKeySet())
                .add(user);
        Platform.runLater(() -> refreshCallback.accept(idx));
    }
}
