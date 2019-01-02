/*
 *  This file is part of PolyChat Client.
 *  *
 *  * Copyright © 2018 DemonScythe45
 *  *
 *  * PolyChat Client is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU Lesser General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * PolyChat Client is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public License
 *  * along with PolyChat Client. If not, see <https://www.gnu.org/licenses/>.
 *
 */
package club.moddedminecraft.polychat.client;

import club.moddedminecraft.polychat.networking.io.*;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

public class EventListener {
    //This gets messages sent on this server and sends them to the main polychat process
    @SubscribeEvent
    public void recieveChatEvent(ServerChatEvent event) {
        String nameWithPrefixes = event.getComponent().getUnformattedText().replace(event.getMessage(), "");
        ChatMessage chatMessage = new ChatMessage(nameWithPrefixes, event.getMessage(), ITextComponent.Serializer.componentToJson(event.getComponent()));
        ModClass.sendMessage(chatMessage);
    }

    //This sets the server prefix for this player on this server
    @SubscribeEvent
    public void playerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        String id = ModClass.properties.getProperty("server_id");
        if (!id.equals("empty")) {
            event.player.addPrefix(new TextComponentString(id));
        }
        PlayerStatusMessage loginMsg = new PlayerStatusMessage(event.player.getName(), id, true);
        ModClass.sendMessage(loginMsg);
    }

    @SubscribeEvent
    public void playerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        String id = ModClass.properties.getProperty("server_id");
        if (!id.equals("empty")) {
            event.player.addPrefix(new TextComponentString(id));
        }
    }

    @SubscribeEvent
    public void playerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        String id = ModClass.properties.getProperty("server_id");
        PlayerStatusMessage logoutMsg = new PlayerStatusMessage(event.player.getName(), id, false);
        ModClass.sendMessage(logoutMsg);
    }

    //This sends a text component to the server console and all players connected
    public static void sendTextComponent(ITextComponent component) {
        ModClass.server.sendMessage(component);

        //Loops over all players to send the message to them
        PlayerList players = ModClass.server.getPlayerList();
        for (String name : ModClass.server.getOnlinePlayerNames()) {
            try {
                players.getPlayerByUsername(name).sendMessage(component);
            }catch (NullPointerException ignored) {}
        }
    }

    //This gets messages sent from the main polychat process and handles them
    public static void handleMessage(Message message) {
        ITextComponent string = null;
        //Determines the content of the text component
        if (message instanceof BroadcastMessage) {
            string = new TextComponentString(((BroadcastMessage) message).getMessage());
        }else if (message instanceof ChatMessage){
            ChatMessage chatMessage = (ChatMessage) message;
            if (chatMessage.getComponentJson().equals("empty")) {
                string = new TextComponentString(chatMessage.getUsername() + " " + chatMessage.getMessage());
            }else {
                string = ITextComponent.Serializer.fromJsonLenient(chatMessage.getComponentJson());
            }
        }else if (message instanceof ServerStatusMessage) {
            ServerStatusMessage serverStatus = ((ServerStatusMessage) message);
            switch (serverStatus.getState()) {
                case 1:
                    string = new TextComponentString(serverStatus.getServerID() + " Server Online");
                    break;
                case 2:
                    string = new TextComponentString(serverStatus.getServerID() + " Server Offline");
                    break;
                case 3:
                    string = new TextComponentString(serverStatus.getServerID() + " Server Crashed");
                    break;
                default:
                    System.err.println("Unrecognized server state " + serverStatus.getState() + " received from " + serverStatus.getServerID());
            }
        }else if (message instanceof PlayerStatusMessage) {
            String statusString;
            PlayerStatusMessage playerStatus = ((PlayerStatusMessage) message);
            if (playerStatus.getJoined()) {
                statusString = playerStatus.getServerID() + " " + playerStatus.getUserName() + " has joined the game";
            }else {
                statusString = playerStatus.getServerID() + " " + playerStatus.getUserName() + " has left the game";
            }
            string = new TextComponentString(statusString);
        }

        if (string != null) sendTextComponent(string);
    }
}
