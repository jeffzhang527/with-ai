package com.UI.chat;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Manages “X is typing…” indicators per chat.
 */
public class TypingIndicatorManager {
    private final StackPane parentPane;
    private final Label indicatorLabel;
    private final Map<String, Timer> timers = new HashMap<>();

    public TypingIndicatorManager(StackPane parentPane) {
        this.parentPane = parentPane;
        this.indicatorLabel = new Label();
        indicatorLabel.setStyle("-fx-text-fill: gray; -fx-padding: 5;");
        StackPane.setAlignment(indicatorLabel, javafx.geometry.Pos.BOTTOM_LEFT);
        parentPane.getChildren().add(indicatorLabel);
        indicatorLabel.setVisible(false);
    }

    /**
     * Call when you receive a “TYPING sender chatKey” message.
     */
    public void userTyping(String sender, String chatKey) {
        // Only show if this indicator is for the current chat
        indicatorLabel.setText(sender + " is typing...");
        indicatorLabel.setVisible(true);

        // Cancel any previous timer for that sender
        timers.computeIfPresent(sender, (s, t) -> {
            t.cancel();
            return null;
        });

        // Hide after 2 seconds of inactivity
        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    indicatorLabel.setVisible(false);
                });
            }
        }, 2000);
        timers.put(sender, timer);
    }
}
