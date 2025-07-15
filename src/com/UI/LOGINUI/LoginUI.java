package com.UI.LOGINUI;
import com.server.ChatServer;
import com.UI.chat.ChatWindow;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

public class LoginUI extends Application {
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Login");

        // Background gradient
        Stop[] stops = new Stop[]{
                new Stop(0, Color.web("#4ca1af")),
                new Stop(1, Color.web("#c4e0e5"))
        };
        LinearGradient bgGradient = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE, stops
        );

        VBox root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(60));
        root.setBackground(new Background(new BackgroundFill(
                bgGradient, CornerRadii.EMPTY, Insets.EMPTY
        )));

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

        // Nickname input
        TextField nicknameField = new TextField();
        nicknameField.setPromptText("Enter your nickname");
        nicknameField.setPrefWidth(320);
        nicknameField.setStyle(
                "-fx-background-radius: 20; " +
                        "-fx-background-color: rgba(255, 255, 255, 0.8); " +
                        "-fx-padding: 12;"
        );

        // Login button
        Button loginButton = new Button("Login");
        loginButton.setFont(Font.font("Segoe UI", 18));
        loginButton.setTextFill(Color.WHITE);
        loginButton.setBackground(new Background(new BackgroundFill(
                Color.web("#16a085"), new CornerRadii(20), Insets.EMPTY
        )));
        loginButton.setPadding(new Insets(12, 30, 12, 30));
        loginButton.setOnMouseEntered(e -> loginButton.setBackground(
                new Background(new BackgroundFill(
                        Color.web("#1abc9c"), new CornerRadii(20), Insets.EMPTY
                ))
        ));
        loginButton.setOnMouseExited(e -> loginButton.setBackground(
                new Background(new BackgroundFill(
                        Color.web("#16a085"), new CornerRadii(20), Insets.EMPTY
                ))
        ));
        loginButton.setOnAction(e -> {
            String nick = nicknameField.getText().trim();
            if (nick.isEmpty()) {
                new Alert(Alert.AlertType.WARNING,
                        "Please enter your nickname.", ButtonType.OK)
                        .showAndWait();
            } else {
                try {
                    // Replace with your server’s host/IP if not localhost
                    new ChatWindow("localhost", 5555, nick);
                    primaryStage.close();   // close the login window
                } catch (Exception ex) {
                    ex.printStackTrace();
                    new Alert(Alert.AlertType.ERROR,
                            "Unable to connect to chat server.",
                            ButtonType.OK).showAndWait();
                }
            }
        });


        root.getChildren().addAll(welcomeBox, nicknameField, loginButton);

        Scene scene = new Scene(root, 520, 420);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
