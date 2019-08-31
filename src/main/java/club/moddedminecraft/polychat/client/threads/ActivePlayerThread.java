package club.moddedminecraft.polychat.client.threads;

import club.moddedminecraft.polychat.client.ModClass;
import club.moddedminecraft.polychat.networking.io.PlayerListMessage;

import java.util.ArrayList;
import java.util.Collections;

public class ActivePlayerThread extends HeartbeatThread {

    private final String serverID;

    public ActivePlayerThread(int interval, String serverID) {
        super(interval);
        this.serverID = serverID;
    }

    @Override
    protected void run() throws InterruptedException {
        ArrayList<String> onlinePlayers = getPlayers();
        PlayerListMessage message = new PlayerListMessage(serverID, onlinePlayers);
        ModClass.sendMessage(message);
    }

    private ArrayList<String> getPlayers() {
        ArrayList<String> playerList = new ArrayList<>();
        Collections.addAll(playerList, ModClass.server.getOnlinePlayerNames());
        return playerList;
    }

}
