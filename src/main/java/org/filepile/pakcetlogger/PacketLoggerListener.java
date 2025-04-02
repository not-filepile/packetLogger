package org.filepile.pakcetlogger;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PacketLoggerListener extends PacketAdapter {

    private final JavaPlugin plugin;
    private static final Set<PacketType> IGNORED_PACKETS = new HashSet<>(Arrays.asList(
            PacketType.Play.Client.LOOK,
            PacketType.Play.Client.POSITION,
            PacketType.Play.Client.POSITION_LOOK,
            PacketType.Play.Client.LOOK,
            PacketType.Play.Client.FLYING,
            PacketType.Play.Client.KEEP_ALIVE,
            PacketType.Play.Client.ARM_ANIMATION,
            PacketType.Play.Client.BLOCK_DIG,
            PacketType.Play.Client.BLOCK_PLACE,
            PacketType.Play.Client.TELEPORT_ACCEPT,
            PacketType.Play.Client.GROUND,
            PacketType.Play.Client.ENTITY_ACTION

    ));

    private static PacketType[] getClientPacketTypes() {
        Set<PacketType> clientPackets = new HashSet<>();
        try {
            Class<?> clientClass = PacketType.Play.Client.class;
            for (java.lang.reflect.Field field : clientClass.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) && field.getType().equals(PacketType.class)) {
                    PacketType pt = (PacketType) field.get(null);
                    clientPackets.add(pt);
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        clientPackets.removeAll(IGNORED_PACKETS);
        return clientPackets.toArray(new PacketType[0]);
    }

    public PacketLoggerListener(JavaPlugin plugin) {
        super(plugin, getClientPacketTypes());
        this.plugin = plugin;
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        PacketType type = event.getPacketType();

        if (IGNORED_PACKETS.contains(type)) {
            return;
        }

        String logEntry;
        if (type.equals(PacketType.Play.Client.CHAT)) {
            String message = event.getPacket().getStrings().read(0);
            if (message.startsWith("/")) {
                logEntry = String.format("[%d] Packet: %s, Command: %s%n",
                        System.currentTimeMillis(), type, message);
            } else if (message.startsWith("/skill")) {
                logEntry = String.format("[%d] Packet: %s, chat: %s%n",
                        System.currentTimeMillis(), type, message);
            } else {
                logEntry = String.format("[%d] Packet: %s, Chat: %s%n",
                        System.currentTimeMillis(), type, message);
            }
        } else {
            logEntry = String.format("[%d] Packet: %s, Data: %s%n",
                    System.currentTimeMillis(), type, event.getPacket().getStrings().read(0));
        }

        File logFolder = new File(plugin.getDataFolder(), "logs");
        if (!logFolder.exists()) {
            logFolder.mkdirs();
        }
        File playerLog = new File(logFolder, player.getUniqueId().toString() + ".txt");

        try (FileWriter writer = new FileWriter(playerLog, true)) {
            writer.write(logEntry);
        } catch (IOException e) {
            plugin.getLogger().severe(player.getName() + "로깅 실패");
            e.printStackTrace();
        }
    }
}