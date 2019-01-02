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

import club.moddedminecraft.polychat.networking.io.Message;
import club.moddedminecraft.polychat.networking.io.MessageBus;
import club.moddedminecraft.polychat.networking.io.ServerInfoMessage;
import club.moddedminecraft.polychat.networking.io.ServerStatusMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.network.NetworkCheckHandler;
import net.minecraftforge.fml.relauncher.Side;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Properties;

@Mod(modid = ModClass.MODID, name = ModClass.NAME, version = ModClass.VERSION)
public class ModClass
{
    public static final String MODID = "polychat";
    public static final String NAME = "Poly Chat Client";
    public static final String VERSION = "1.0";
    //Used to determine whether the server cleanly shutdown or crashed
    public static boolean shutdownClean = false;
    //Used to determine whether to send a connection lost warning in game
    public static boolean isConnected = true;
    public static MinecraftServer server;
    public static Properties properties;
    public static MessageBus messageBus = null;
    public static Thread reattachThread;

    //Forces the server to allow clients to join without the mod installed on their client
    @NetworkCheckHandler
    public boolean checkClient(Map<String, String> map, Side side) {
        return true;
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        //Registers game event listener class
        MinecraftForge.EVENT_BUS.register(new EventListener());
        reattachThread = new Thread(this::reattachThread);
        //Sets up the config values
        handleConfiguration(event.getModConfigurationDirectory());
        //Registers the shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownHook));
    }

    //Reattaches to the main server should it restart
    public void reattachThread() {
        try {
            while (true) {
                try  {
                    Thread.sleep(1000);
                    if (messageBus == null || (messageBus.isSocketClosed())) {
                        if (isConnected) {
                            isConnected = false;
                            EventListener.sendTextComponent(new TextComponentString("[PolyChat] Lost connection to main server, attempting reconnect..."));
                        }
                        if (messageBus != null) messageBus.stop();
                        messageBus = new MessageBus(new Socket(properties.getProperty("address"), Integer.parseInt(properties.getProperty("port"))), EventListener::handleMessage);
                        messageBus.start();
                        if (!messageBus.isSocketClosed()) {
                            Thread.sleep(2000);
                            sendServerOnline();
                            EventListener.sendTextComponent(new TextComponentString("[PolyChat] Connection re-established!"));
                            isConnected = true;
                        }
                    }
                } catch (UnknownHostException e) {
                    System.out.println("Unknown host exception on reattach");
                } catch (IOException e) {
                    System.out.println("IOException on reattach");
                }
            }
        }catch (InterruptedException ignored) {
            System.out.println("Reattach interrupted, stopping...");
        }
    }

    //Makes sure that the server offline message gets sent
    public void shutdownHook() {
        reattachThread.interrupt();
        short exitVal;
        if (shutdownClean) {
            exitVal = 2;
        }else {
            exitVal = 3;
        }
        ServerStatusMessage statusMessage = new ServerStatusMessage(properties.getProperty("server_id"), exitVal);
        ModClass.sendMessage(statusMessage);
        try {
            //Makes sure message has time to send
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {}
        messageBus.stop();
    }

    //Put in a method to be used in two places
    public void sendServerOnline() {
        //Reports the server as starting
        ServerInfoMessage infoMessage = new ServerInfoMessage(properties.getProperty("server_id"),
                properties.getProperty("server_name"),
                properties.getProperty("server_address"), server.getMaxPlayers());
        ModClass.sendMessage(infoMessage);
        //Reports the server as online and ready to receive players
        ServerStatusMessage statusMessage = new ServerStatusMessage(properties.getProperty("server_id"), (short) 1);
        ModClass.sendMessage(statusMessage);
    }

    //Contains null pointer exceptions from a failed connection to the main server
    public static void sendMessage(Message message) {
        try {
            messageBus.sendMessage(message);
        }catch (NullPointerException ignored) {}
    }

    @EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        server = event.getServer();
    }

    @EventHandler
    public void onStarted(FMLServerStartedEvent event) {
        //Connects to the main polychat server
        handleClientConnection();
        reattachThread.start();
        sendServerOnline();
    }

    @EventHandler
    public void onStopped(FMLServerStoppingEvent event) {
        shutdownClean = true;
    }

    public void handleConfiguration(File modConfigDir) {
        ModClass.properties = new Properties();
        File config = new File(modConfigDir, "polychat.properties");

        if (config.exists() && config.isFile()) {
            try (FileInputStream istream = new FileInputStream(config)) {
                ModClass.properties.load(istream);
            } catch (IOException e) {
                System.err.println("Error loading configuration file!");
                e.printStackTrace();
            }
        }else{
            ModClass.properties.setProperty("address", "127.0.0.1");
            ModClass.properties.setProperty("port", "25566");
            ModClass.properties.setProperty("server_id", "empty");
            ModClass.properties.setProperty("server_name", "empty");
            ModClass.properties.setProperty("server_address", "empty");
            try (FileOutputStream ostream = new FileOutputStream(config)) {
                ModClass.properties.store(ostream, null);
            } catch (IOException e) {
                System.err.println("Error saving new configuration file!");
                e.printStackTrace();
            }
        }
    }

    //Initiates the connection to the main polychat server and sets up the message callback
    public static void handleClientConnection() {
        try {
            messageBus = new MessageBus(new Socket(properties.getProperty("address"), Integer.parseInt(properties.getProperty("port"))), EventListener::handleMessage);
            messageBus.start();
        } catch (IOException e) {
            System.err.println("Failed to establish polychat connection!");
            e.printStackTrace();
        }
    }
}
