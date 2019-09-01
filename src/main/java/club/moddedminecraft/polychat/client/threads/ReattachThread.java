package club.moddedminecraft.polychat.client.threads;

import club.moddedminecraft.polychat.client.EventListener;
import club.moddedminecraft.polychat.client.ModClass;
import club.moddedminecraft.polychat.networking.io.MessageBus;
import club.moddedminecraft.polychat.networking.io.PlayerListMessage;
import club.moddedminecraft.polychat.networking.io.ServerInfoMessage;
import club.moddedminecraft.polychat.networking.io.ServerStatusMessage;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;

public class ReattachThread extends HeartbeatThread {

    private boolean isConnected = true;

    public ReattachThread(int interval) {
        super(interval);
    }

    @Override
    protected void run() throws InterruptedException {
        try {

            if (ModClass.messageBus == null || (ModClass.messageBus.isSocketClosed())) {
                //Tells players ingame that the connection failed
                if (isConnected) {
                    isConnected = false;
                    EventListener.sendTextComponent(new TextComponentString("[PolyChat] Lost connection to main server, attempting reconnect..."));
                }

                //Stops threads if they are still running
                if (ModClass.messageBus != null) ModClass.messageBus.stop();

                //Attempts to start the connection
                ModClass.messageBus = new MessageBus(new Socket(ModClass.properties.getProperty("address"), Integer.parseInt(ModClass.properties.getProperty("port"))), EventListener::handleMessage);
                ModClass.messageBus.start();

                //If the socket was reopened, wait 3 seconds to make sure sending online message works
                if (!ModClass.messageBus.isSocketClosed()) {
                    Thread.sleep(2000);
                    EventListener.sendTextComponent(new TextComponentString("[PolyChat] Connection re-established!"));
                    sendServerOnline();
                    Thread.sleep(1000);
                    sendOnlinePlayers();
                    isConnected = true;
                }

            }
        } catch (UnknownHostException e) {
            System.out.println("Unknown host exception on reattach");
        } catch (IOException e) {
            System.out.println("IOException on reattach");
        }
    }

    public void sendServerOnline() {
        //Reports the server as starting
        ServerInfoMessage infoMessage = new ServerInfoMessage(ModClass.properties.getProperty("server_id", "DEFAULT_ID"),
                ModClass.properties.getProperty("server_name", "DEFAULT_NAME"),
                ModClass.properties.getProperty("server_address", "DEFAULT_ADDRESS"), ModClass.server.getMaxPlayers());
        ModClass.sendMessage(infoMessage);
        //Reports the server as online and ready to receive players
        ServerStatusMessage statusMessage = new ServerStatusMessage(ModClass.properties.getProperty("server_id"),
                ITextComponent.Serializer.componentToJson(ModClass.serverIdText), (short) 1);
        ModClass.sendMessage(statusMessage);
    }


    //Sends a list of all online players silently for auto reconnect
    public void sendOnlinePlayers() {
        ArrayList<String> playerList = new ArrayList<>();
        Collections.addAll(playerList, ModClass.server.getOnlinePlayerNames());
        PlayerListMessage message = new PlayerListMessage(ModClass.properties.getProperty("server_id"), playerList);
        ModClass.sendMessage(message);
    }
}
