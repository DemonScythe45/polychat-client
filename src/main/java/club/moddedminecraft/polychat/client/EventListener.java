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
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

public class EventListener {

    //This sends a text component to the server console and all players connected
    public static void sendTextComponent(ITextComponent component) {
        ModClass.server.sendMessage(component);

        //Loops over all players to send the message to them
        PlayerList players = ModClass.server.getPlayerList();
        for (String name : ModClass.server.getOnlinePlayerNames()) {
            try {
                players.getPlayerByUsername(name).sendMessage(component);
            } catch (NullPointerException ignored) {
            }
        }
    }

    //This gets messages sent from the main polychat process and handles them
    public static void handleMessage(Message message) {
        ITextComponent string = null;
        //Determines the content of the text component
        if (message instanceof BroadcastMessage) {
            BroadcastMessage broadcastMessage = ((BroadcastMessage) message);
            string = new TextComponentString(broadcastMessage.getPrefix());

            TextFormatting formatting = TextFormatting.WHITE;
            int color = broadcastMessage.prefixColor();
            if ((color >= 0) && (color <= 15)) formatting = TextFormatting.fromColorIndex(color);
            string.getStyle().setColor(formatting);

            TextComponentString messageContent = new TextComponentString(" " + broadcastMessage.getMessage());
            messageContent.getStyle().setColor(TextFormatting.WHITE);
            string.appendSibling(messageContent);
        } else if (message instanceof ChatMessage) {
            ChatMessage chatMessage = (ChatMessage) message;
            if (chatMessage.getComponentJson().equals("empty")) {
                string = new TextComponentString("[Discord] ");
                string.getStyle().setColor(TextFormatting.DARK_PURPLE);
                ITextComponent content = ForgeHooks.newChatWithLinks(chatMessage.getUsername() + " " + chatMessage.getMessage());
                content.getStyle().setColor(TextFormatting.RESET);
                string.appendSibling(content);
            } else {
                string = ITextComponent.Serializer.fromJsonLenient(chatMessage.getComponentJson());
            }
        } else if (message instanceof ServerStatusMessage) {
            ServerStatusMessage serverStatus = ((ServerStatusMessage) message);
            ITextComponent msgComponent = null;
            switch (serverStatus.getState()) {
                case 1:
                    msgComponent = new TextComponentString(" Server Online");
                    break;
                case 2:
                    msgComponent = new TextComponentString(" Server Offline");
                    break;
                case 3:
                    msgComponent = new TextComponentString(" Server Crashed");
                    break;
                default:
                    System.err.println("Unrecognized server state " + serverStatus.getState() + " received from " + serverStatus.getServerID());
            }
            if (msgComponent != null) {
                string = ITextComponent.Serializer.fromJsonLenient(serverStatus.getPrefixJson());
                msgComponent.getStyle().setColor(TextFormatting.WHITE);
                string.appendSibling(msgComponent);
            }
        } else if (message instanceof PlayerStatusMessage) {
            ITextComponent statusString;
            PlayerStatusMessage playerStatus = ((PlayerStatusMessage) message);
            if (!(playerStatus.getSilent())) {
                if (playerStatus.getJoined()) {
                    statusString = new TextComponentString(" " + playerStatus.getUserName() + " has joined the game");
                } else {
                    statusString = new TextComponentString(" " + playerStatus.getUserName() + " has left the game");
                }
                string = ITextComponent.Serializer.fromJsonLenient(playerStatus.getPrefixJson());
                statusString.getStyle().setColor(TextFormatting.WHITE);
                string.appendSibling(statusString);
            }
        } else if (message instanceof CommandMessage) {
            String command = ((CommandMessage) message).getCommand();
            ModClass.server.getCommandManager().executeCommand(new CommandSender(command), command);
        }

        if (string != null) sendTextComponent(string);
    }

    //This gets messages sent on this server and sends them to the main polychat process
    @SubscribeEvent
    public void recieveChatEvent(ServerChatEvent event) {
        ITextComponent newMessage = ModClass.serverIdText.createCopy();
        ITextComponent space = new TextComponentString(" ");
        space.getStyle().setColor(TextFormatting.RESET);
        space.appendSibling(event.getComponent());
        newMessage.appendSibling(space);
        event.setComponent(newMessage);
        String unformattedText = event.getComponent().getUnformattedText();
        String nameWithPrefixes = unformattedText.substring(0, unformattedText.lastIndexOf(event.getMessage()));
        ChatMessage chatMessage = new ChatMessage(nameWithPrefixes, event.getMessage(), ITextComponent.Serializer.componentToJson(event.getComponent()));
        ModClass.sendMessage(chatMessage);
    }

    //This sets the server prefix for this player on this server
    @SubscribeEvent
    public void playerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        String id = ModClass.properties.getProperty("server_id");
        PlayerStatusMessage loginMsg = new PlayerStatusMessage(event.player.getName(), id,
                ModClass.idJson, true, false);
        ModClass.sendMessage(loginMsg);
    }

    @SubscribeEvent
    public void playerRespawn(PlayerEvent.PlayerRespawnEvent event) {
    }

    @SubscribeEvent
    public void playerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        String id = ModClass.properties.getProperty("server_id");
        PlayerStatusMessage logoutMsg = new PlayerStatusMessage(event.player.getName(), id,
                ModClass.idJson, false, false);
        ModClass.sendMessage(logoutMsg);
    }
}
