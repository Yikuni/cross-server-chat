package com.yikuni.mc.crossserverchat;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.yikuni.mc.crossserverchat.event.OtherServerPlayerChatEvent;
import com.yikuni.mc.crossserverchat.event.OtherServerPlayerMessageEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.*;

public final class CrossServerChat extends JavaPlugin implements PluginMessageListener, Listener {
    public static final String mySubChannel = "csc";
    public static enum MessageType{
        CHAT("chat"),
        PRIVATE_MESSAGE("msg")
        ;
        private final String type;
        MessageType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

    }

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
            msgout.writeUTF(MessageType.CHAT.type + "," + event.getPlayer().getName() + "," + event.getMessage()); // You can do anything you want with msgout
        } catch (IOException exception){
            exception.printStackTrace();
        }
        out.writeShort(msgbytes.toByteArray().length);
        out.write(msgbytes.toByteArray());
        event.getPlayer().sendPluginMessage(JavaPlugin.getPlugin(CrossServerChat.class), "BungeeCord", out.toByteArray());
    }

    @EventHandler
    public void onPlayerMessage(PlayerCommandPreprocessEvent event){
        if (event.getMessage().startsWith("/t") || event.getMessage().startsWith("/tell") || event.getMessage().startsWith("/msg")){
            if (event.getMessage().length() - event.getMessage().trim().length() <= 1) return;
            event.setCancelled(true);
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Forward"); // So BungeeCord knows to forward it
            out.writeUTF("ONLINE");
            out.writeUTF(CrossServerChat.mySubChannel); // The channel name to check if this your data

            ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
            DataOutputStream msgout = new DataOutputStream(msgbytes);
            int firstSpace = event.getMessage().indexOf(' ');
            int secondSpace = event.getMessage().indexOf(' ', firstSpace + 1);
            try {
                msgout.writeUTF(MessageType.PRIVATE_MESSAGE.type + "," + event.getPlayer().getName() + "," +
                        event.getMessage().substring(firstSpace + 1, secondSpace) + "," +event.getMessage().substring(secondSpace + 1)); // You can do anything you want with msgout
            } catch (IOException exception){
                exception.printStackTrace();
            }
            out.writeShort(msgbytes.toByteArray().length);
            out.write(msgbytes.toByteArray());
            event.getPlayer().sendPluginMessage(JavaPlugin.getPlugin(CrossServerChat.class), "BungeeCord", out.toByteArray());
        }
    }


    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) {
            return;
        }
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();
        if (subChannel.equals(mySubChannel)) {
            // Use the code sample in the 'Response' sections below to read
            // the data.
            short len = in.readShort();
            byte[] msgbytes = new byte[len];
            in.readFully(msgbytes);

            DataInputStream msgin = new DataInputStream(new ByteArrayInputStream(msgbytes));
            try {
                String data = msgin.readUTF(); // Read the data in the same way you wrote it
                int index = data.indexOf(',');
                String type = data.substring(0, index);
                data = data.substring(index + 1);
                if (MessageType.CHAT.type.equals(type)){
                    // Chat类型消息
                    index = data.indexOf(',');
                    Bukkit.getPluginManager().callEvent(new OtherServerPlayerChatEvent(data.substring(0, index), data.substring(index + 1)));
                }else  if (MessageType.PRIVATE_MESSAGE.type.equals(type)){
                    // /t player msg
                    index = data.indexOf(',');  // 0~index from
                    int index2 = data.indexOf(',', index + 1); // index ~ index2 to
                    String from = data.substring(0, index);
                    String to = data.substring(index + 1, index2);
                    String msg = data.substring(index2 + 1);
                    Bukkit.getPluginManager().callEvent(new OtherServerPlayerMessageEvent(from, to, msg));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
