package io.github.sbcomputerteh.chatwatch.cwvelocity;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.sbcomputerteh.chatwatch.cwvelocity.packet.ChatMessagePacket;
import io.github.sbcomputerteh.chatwatch.cwvelocity.packet.PrivateMessagePacket;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.swing.text.html.Option;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

@Plugin(
        id = "cwvelocity",
        name = "ChatWatch Client",
        version = BuildConstants.VERSION
)
public class CWVelocity {
    @Inject private Logger logger;
    @Inject private ProxyServer proxy;
    @Inject @DataDirectory private Path dataPath;

    private final Queue<ChatMessagePacket> packetQueue;
    private final Gson gson;
    private final HttpClient http;
    private CWConfig config;

    public Logger getLogger() { return logger; }
    public ProxyServer getProxy() { return proxy; }

    public CWVelocity() {
        packetQueue = new ArrayBlockingQueue<>(100); // more than we'll probably need but eh
        gson = new Gson();
        http = HttpClient.newHttpClient();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        if(!Files.exists(dataPath)) {
            try {
                Files.createDirectories(dataPath);
            } catch (IOException e) {
                logger.error("Couldn't create data dir!", e);
            }
        }
        config = CWConfig.load(dataPath);

        // getProxy().getEventManager().register(this, this);
        getProxy().getScheduler()
                        .buildTask(this, this::processQueue)
                        .repeat(250, TimeUnit.MILLISECONDS)
                        .schedule();

        logger.info("Set up ChatWatch for Velocity! Using plugin ver {}", BuildConstants.VERSION);
    }

    @Subscribe
    public void onPlayerChat(@NotNull PlayerChatEvent ev) {
        UUID id = ev.getPlayer().getUniqueId();
        String name = ev.getPlayer().getUsername();
        String server = ev
                .getPlayer()
                .getCurrentServer()
                .map(s -> s.getServerInfo().getName())
                .orElse("Unknown");
        String msg = ev.getMessage();

        sendToServer(new ChatMessagePacket(id, name, server, msg));
    }

    @Subscribe
    public void onCommandExecute(@NotNull CommandExecuteEvent ev) {
        if(!(ev.getCommandSource() instanceof Player p)) {
            return;
        }

        String[] parts = ev.getCommand().replace("/", "").trim().split(" ");
        String command = parts[0];

        if(!config.messageCommands.contains(command)) {
            return;
        }
        if(parts.length < 3) {
            // make sure the command at least has a target and message
            return;
        }

        String recipientName = parts[1];
        Optional<UUID> recipientId = getProxy().getPlayer(recipientName).map(Player::getUniqueId);

        if(recipientId.isEmpty()) {
            // user probably typo-ed or something
            return;
        }

        UUID senderId = p.getUniqueId();
        String senderName = p.getUsername();
        String senderServer = p.getCurrentServer().map(v -> v.getServerInfo().getName()).orElse("Unknown");
        String text = String.join(" ",  Arrays.copyOfRange(parts, 2, parts.length));

        sendToServer(new PrivateMessagePacket(senderId, senderName, recipientId.get(), recipientName, senderServer, text));
    }

    public void sendToServer(ChatMessagePacket packet) {
        packetQueue.add(packet);
    }

    private void processQueue() {
        if(packetQueue.isEmpty()) return;
        ChatMessagePacket pkt = packetQueue.remove();

        String json = gson.toJson(pkt);
        logger.info("Packet: {}", json);

        String uriPath;
        if(pkt instanceof PrivateMessagePacket) {
            uriPath = "/Ingest/Private";
        } else {
            uriPath = "/Ingest/Chat";
        }

        URI uri;
        try {
            uri = new URI(
                    "http",
                    null,
                    config.serverAddress,
                    config.serverPort,
                    uriPath,
                    null,
                    null);
        } catch (URISyntaxException e) {
            logger.error("Invalid IP and/or port specified in config!");
            return;
        }

        HttpRequest req = HttpRequest
                .newBuilder(uri)
                .setHeader("User-Agent", "ChatWatch/Velocity " + BuildConstants.VERSION)
                .setHeader("Content-Type", "application/json; charset=UTF-8")
                .timeout(Duration.ofSeconds(3))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        try {
            // The ingest API returns a string of the generated message ID, but we don't really need that here
            http.send(req, HttpResponse.BodyHandlers.discarding());
        } catch (IOException | InterruptedException e) {
            // Much better to handle this gracefully than bring the whole proxy
            // crashing down over one failed chat message :3
            logger.error("Failed to send the request!", e);
        }
    }
}
