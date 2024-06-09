package io.github.sbcomputerteh.chatwatch.cwvelocity.packet;

import java.util.UUID;

public class PlayerInfo {
    public UUID ID;
    public String Username;

    public PlayerInfo(UUID id, String username) {
        ID = id;
        Username = username;
    }
}
