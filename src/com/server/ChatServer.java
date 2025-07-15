package com.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final int PORT = 5555;
    // All connected clients
    private static final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    // All active nicknames
    private static final Set<String> userNames = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Chat server running on port " + PORT);

        while (true) {
            Socket clientSock = serverSocket.accept();
            ClientHandler handler = new ClientHandler(clientSock);
            clients.add(handler);
            new Thread(handler).start();
        }
    }

    /** Broadcast a message to everyone */
    static void broadcast(String msg) {
        for (ClientHandler c : clients) {
            c.send(msg);
        }
    }

    /** Send the current user-list to everyone */
    static void broadcastUserList() {
        String csv = String.join(",", userNames);
        for (ClientHandler c : clients) {
            c.send("USER_LIST " + csv);
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private final BufferedReader in;
        private final PrintWriter out;
        private String nickname;

        ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out    = new PrintWriter(socket.getOutputStream(), true);
        }

        @Override
        public void run() {
            try {
                // 1) Read and register nickname
                nickname = in.readLine();
                userNames.add(nickname);

                // 2) Announce join & send updated user list
                broadcast("** " + nickname + " joined the chat **");
                broadcastUserList();

                // 3) Main message loop
                String line;
                while ((line = in.readLine()) != null) {

                    // --- Typing indicator ---
                    if (line.equals("/typing")) {
                        // Notify all clients that this user is typing in the global chat
                        broadcast("TYPING " + nickname + " All");
                        continue;
                    }

                    // --- Add Friend ---
                    if (line.startsWith("/addfriend ")) {
                        String friend = line.substring(11).trim();
                        if (userNames.contains(friend)) {
                            out.println("(SYSTEM) Added friend: " + friend);
                        } else {
                            out.println("(SYSTEM) User '" + friend + "' not found");
                        }
                        continue;
                    }

                    // --- Clear History ---
                    if (line.equals("/clear")) {
                        out.println("CLEAR_HISTORY");
                        continue;
                    }

                    // --- Whisper ---
                    if (line.startsWith("/w ")) {
                        String[] parts = line.split(" ", 3);
                        if (parts.length == 3) {
                            String target = parts[1], msg = parts[2];
                            sendPrivate(target, nickname + ": " + msg);
                        }
                        continue;
                    }

                    // --- React to a message ---
                    if (line.startsWith("/react ")) {
                        String[] parts = line.split(" ", 3);
                        if (parts.length == 3) {
                            String idx   = parts[1];
                            String emoji = parts[2];
                            broadcast("REACTION " + idx + " " + emoji + " " + nickname);
                        }
                        continue;
                    }

                    // --- Broadcast normal chat line ---
                    broadcast(nickname + ": " + line);
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // Cleanup on disconnect
                clients.remove(this);
                userNames.remove(nickname);
                broadcast("** " + nickname + " left the chat **");
                broadcastUserList();
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        /** Send a message to this client */
        void send(String msg) {
            out.println(msg);
        }

        /** Send a private (whisper) message */
        private void sendPrivate(String targetNick, String msg) {
            for (ClientHandler c : clients) {
                if (c.nickname.equals(targetNick)) {
                    // to recipient
                    c.send("(whisper) " + msg);
                    // echo back to sender
                    this.send("(whisper to " + targetNick + ") " +
                            msg.substring(msg.indexOf(':') + 1).trim());
                    return;
                }
            }
            out.println("(SYSTEM) User '" + targetNick + "' not found.");
        }
    }
}
