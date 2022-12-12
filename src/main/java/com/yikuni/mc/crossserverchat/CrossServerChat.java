package com.yikuni.mc.crossserverchat;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.yikuni.mc.crossserverchat.event.OtherServerPlayerChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.*;

public final class CrossServerChat extends JavaPlugin implements PluginMessageListener, Listener {
    public static final String mySubChannel = "csc";

    @Override
    public void onEnable() {
        // Plugin startup logic
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        Bukkit.getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);
    }

    @Override
    public void onDisable() {
        Bukkit.getMessenger().unregisterIncomingPluginChannel(this);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(this);
        // Plugin shutdown logic
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event){
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward"); // So BungeeCord knows to forward it
        out.writeUTF("ONLINE");
        out.writeUTF(CrossServerChat.mySubChannel); // The channel name to check if this your data

        ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
        DataOutputStream msgout = new DataOutputStream(msgbytes);
        try {
            msgout.writeUTF(event.getPlayer().getName() + "," + event.getMessage()); // You can do anything you want with msgout
        } catch (IOException exception){
            exception.printStackTrace();
        }
        out.writeShort(msgbytes.toByteArray().length);
        out.write(msgbytes.toByteArray());
        event.getPlayer().sendPluginMessage(JavaPlugin.getPlugin(CrossServerChat.class), "BungeeCord", out.toByteArray());
    }


    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) {
            return;
        }
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();
        getLogger().info("channel = " + subChannel);
        if (subChannel.equals(mySubChannel)) {
            // Use the code sample in the 'Response' sections below to read
            // the data.
            short len = in.readShort();
            byte[] msgbytes = new byte[len];
            in.readFully(msgbytes);

            DataInputStream msgin = new DataInputStream(new ByteArrayInputStream(msgbytes));
            try {
                String data = msgin.readUTF(); // Read the data in the same way you wrote it
                String[] split = data.split(",");
                Bukkit.getPluginManager().callEvent(new OtherServerPlayerChatEvent(split[0], split[1]));
                getLogger().info("OtherServerPlayerChatEvent is created");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
