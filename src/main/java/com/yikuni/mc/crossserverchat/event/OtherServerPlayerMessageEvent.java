package com.yikuni.mc.crossserverchat.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;


public class OtherServerPlayerMessageEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancel = false;
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList(){
        return handlers;
    }

    private final String msg;
    private final String sender;
    private final String to;
    public OtherServerPlayerMessageEvent(String sender, String to, String msg) {
        this.msg = msg;
        this.sender = sender;
        this.to = to;
    }

    public String getTo() {
        return to;
    }

    public String getMsg() {
        return msg;
    }

    public String getSender() {
        return sender;
    }

    @Override
    public boolean isCancelled() {
        return cancel;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }
}
