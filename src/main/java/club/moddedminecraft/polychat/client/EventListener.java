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

import club.moddedminecraft.polychat.networking.io.BroadcastMessage;
import club.moddedminecraft.polychat.networking.io.ChatMessage;
import club.moddedminecraft.polychat.networking.io.Message;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

public class EventListener {
    //This gets messages sent on this server and sends them to the main polychat process
    @SubscribeEvent
    public void recieveChatEvent(ServerChatEvent event) {
        System.out.println("Server chat event received");
        String nameWithPrefixes = event.getComponent().getUnformattedText().replace(event.getMessage(), "");
        ChatMessage chatMessage = new ChatMessage("", nameWithPrefixes, event.getMessage());
        ModClass.messageBus.sendMessage(chatMessage);
    }

    //This sets the server prefix for this player on this server
    @SubscribeEvent
    public void playerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        String id = ModClass.properties.getProperty("server_id");
        if (!id.equals("empty")) {
            event.player.addPrefix(new TextComponentString(id));
        }
    }

    //This gets messages sent from the main polychat process and distributes them to players on this server
    public static void distributeMessage(Message message) {
        TextComponentString string;
        if (message instanceof BroadcastMessage) {
            string = new TextComponentString(((BroadcastMessage) message).getMessage());
        }else{
            ChatMessage chatMessage = (ChatMessage) message;
            string = new TextComponentString(chatMessage.getPrefix() + chatMessage.getUsername() + " " + chatMessage.getMessage());
        }

        ModClass.server.sendMessage(string);

        PlayerList players = ModClass.server.getPlayerList();
        for (String name : ModClass.server.getOnlinePlayerNames()) {
            try {
                players.getPlayerByUsername(name).sendMessage(string);
            }catch (NullPointerException ignored) {}
        }
    }
}
