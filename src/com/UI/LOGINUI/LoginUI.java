package com.UI.LOGINUI;

import com.UI.chat.ChatWindow;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.*;
import java.util.*;

public class LoginUI extends Application {
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Login");

        // Background gradient
        Stop[] stops = {
                new Stop(0, Color.web("#4ca1af")),
                new Stop(1, Color.web("#c4e0e5"))
        };
        LinearGradient bgGradient = new LinearGradient(
                0,0,1,1,true,CycleMethod.NO_CYCLE,stops
        );

        // Build your form
        VBox form = new VBox(20);
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(40));

        // Animated welcome text
        HBox welcomeBox = new HBox(5);
        welcomeBox.setAlignment(Pos.CENTER);
        String welcome = "Welcome! to Chat";
        for (int i = 0; i < welcome.length(); i++) {
            Text letter = new Text(String.valueOf(welcome.charAt(i)));
            letter.setFont(Font.font("Segoe UI", 50));
            letter.setFill(Color.WHITE);
            letter.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(400), letter);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.setDelay(Duration.millis(i * 150));
            ft.play();
            welcomeBox.getChildren().add(letter);
        }

        // Server address & nickname fields
        TextField hostField = new TextField("localhost");
        hostField.setPromptText("Server address (IP or hostname)");
        hostField.setPrefWidth(320);
        hostField.setStyle(
                "-fx-background-radius:20;" +
                        "-fx-background-color:rgba(255,255,255,0.8);" +
                        "-fx-padding:10;"
        );

        TextField nicknameField = new TextField();
        nicknameField.setPromptText("Enter your nickname");
        nicknameField.setPrefWidth(320);
        nicknameField.setStyle(
                "-fx-background-radius:20;" +
                        "-fx-background-color:rgba(255,255,255,0.8);" +
                        "-fx-padding:10;"
        );

        // Login button
        Button loginButton = new Button("Login");
        loginButton.setFont(Font.font("Segoe UI",18));
        loginButton.setTextFill(Color.WHITE);
        loginButton.setBackground(new Background(new BackgroundFill(
                Color.web("#16a085"), new CornerRadii(20), Insets.EMPTY
        )));
        loginButton.setPadding(new Insets(12,30,12,30));
        loginButton.setOnMouseEntered(e ->
                loginButton.setBackground(new Background(new BackgroundFill(
                        Color.web("#1abc9c"), new CornerRadii(20), Insets.EMPTY
                ))));
        loginButton.setOnMouseExited(e ->
                loginButton.setBackground(new Background(new BackgroundFill(
                        Color.web("#16a085"), new CornerRadii(20), Insets.EMPTY
                ))));
        loginButton.setOnAction(e -> {
            String host = hostField.getText().trim();
            String nick = nicknameField.getText().trim();
            if (host.isEmpty() || nick.isEmpty()) {
                new Alert(Alert.AlertType.WARNING,
                        "Please enter both server address and your nickname.",
                        ButtonType.OK).showAndWait();
            } else {
                try {
                    new ChatWindow(host, 5555, nick);
                    primaryStage.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    new Alert(Alert.AlertType.ERROR,
                            "Unable to connect to chat server.",
                            ButtonType.OK).showAndWait();
                }
            }
        });

        form.getChildren().addAll(
                welcomeBox,
                hostField,
                nicknameField,
                loginButton
        );

        // Wrap form in a BorderPane
        BorderPane root = new BorderPane(form);
        root.setBackground(new Background(new BackgroundFill(
                bgGradient, CornerRadii.EMPTY, Insets.EMPTY
        )));

        // Fetch local IPv4
        String localIp = getLocalIPv4();
        Label ipLabel = new Label("Your IP: " + localIp);
        ipLabel.setTextFill(Color.rgb(255,255,255,0.5));
        ipLabel.setFont(Font.font(12));
        ipLabel.setPadding(new Insets(5));

        // Place IP label in bottom-right
        StackPane bottomRight = new StackPane(ipLabel);
        bottomRight.setPadding(new Insets(0, 10, 10, 0));
        StackPane.setAlignment(ipLabel, Pos.BOTTOM_RIGHT);
        root.setBottom(bottomRight);

        // Show scene
        Scene scene = new Scene(root, 520, 460);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Returns the first non-loopback IPv4 address found, or "127.0.0.1" if none.
     */
    private String getLocalIPv4() {
        try {
            for (NetworkInterface nif : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress addr : Collections.list(nif.getInetAddresses())) {
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "127.0.0.1";
    }

    public static void main(String[] args) {
        launch(args);
    }
}
