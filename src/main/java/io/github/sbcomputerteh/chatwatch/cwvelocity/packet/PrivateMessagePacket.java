package io.github.sbcomputerteh.chatwatch.cwvelocity.packet;

import java.util.UUID;

// See: ChatWatchApp/Models/PrivateMessage.cs
public class PrivateMessagePacket extends ChatMessagePacket {
    public PlayerInfo Recipient;

    public PrivateMessagePacket(UUID senderId, String senderName, UUID recipientId, String recipientName, String server, String content) {
        super(senderId, senderName, server, content);
        Recipient = new PlayerInfo(recipientId, recipientName);
    }
}
