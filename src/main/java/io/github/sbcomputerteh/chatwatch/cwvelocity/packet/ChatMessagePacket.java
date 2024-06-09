package io.github.sbcomputerteh.chatwatch.cwvelocity.packet;

import java.time.Instant;
import java.util.UUID;

// See: ChatWatchApp/Models/ChatMessage.cs
public class ChatMessagePacket {
    public String Timestamp;
    public PlayerInfo Sender;
    public String Server;
    public String Content;

    public ChatMessagePacket(UUID playerId, String playerName, String server, String content) {
        Timestamp = Instant.now().toString();
        Sender = new PlayerInfo(playerId, playerName);
        Server = server;
        Content = content;
    }
}
