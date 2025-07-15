package com.UI.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * ChatClient handles:
 *  - connecting to the server
 *  - sending broadcast and private messages
 *  - receiving both chat lines and USER_LIST updates
 */
public class ChatClient {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final Consumer<String> messageHandler;
    private final Consumer<List<String>> userListHandler;

    /**
     * @param host            server hostname
     * @param port            server port
     * @param nickname        this client's nickname
     * @param messageHandler  called for any non-USER_LIST line
     * @param userListHandler called when a \"USER_LIST ...\" line is received
     */
    public ChatClient(
            String host,
            int port,
            String nickname,
            Consumer<String> messageHandler,
            Consumer<List<String>> userListHandler
    ) throws IOException {
        this.socket          = new Socket(host, port);
        this.in              = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out             = new PrintWriter(socket.getOutputStream(), true);
        this.messageHandler  = messageHandler;
        this.userListHandler = userListHandler;

        // announce ourselves
        out.println(nickname);

        // listen in background
        new Thread(this::listen).start();
    }

    /** Listen for server lines and route them appropriately. */
    private void listen() {
        String line;
        try {
            while ((line = in.readLine()) != null) {
                if (line.startsWith("USER_LIST ")) {
                    String csv = line.substring("USER_LIST ".length());
                    List<String> users = Arrays.asList(csv.split(","));
                    userListHandler.accept(users);
                } else {
                    messageHandler.accept(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Broadcast a message to everyone. */
    public void sendMessage(String text) {
        out.println(text);
    }

    /**
     * Send a private (whisper) message.
     * @param recipient whom to send to
     * @param text      the message body
     */
    public void sendPrivate(String recipient, String text) {
        out.println("/w " + recipient + " " + text);
    }

    /** Gracefully close the connection. */
    public void close() throws IOException {
        socket.close();
    }
}

