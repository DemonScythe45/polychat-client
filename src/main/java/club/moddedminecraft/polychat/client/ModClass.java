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

import club.moddedminecraft.polychat.networking.io.MessageBus;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkCheckHandler;
import net.minecraftforge.fml.relauncher.Side;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.Properties;

@Mod(modid = ModClass.MODID, name = ModClass.NAME, version = ModClass.VERSION)
public class ModClass
{
    public static final String MODID = "polychat";
    public static final String NAME = "Poly Chat Client";
    public static final String VERSION = "1.0";
    public static MinecraftServer server;
    public static Properties properties;
    public static MessageBus messageBus;

    //Forces the server to allow clients to join without the mod installed on their client
    @NetworkCheckHandler
    public boolean checkClient(Map<String, String> map, Side side) {
        return true;
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new EventListener());
        handleConfiguration(event.getModConfigurationDirectory());
        handleClientConnection();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {

    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {

    }

    @EventHandler
    public void onServerStart(FMLServerStartingEvent event) {
        server = event.getServer();

    }

    public void handleConfiguration(File modConfigDir) {
        ModClass.properties = new Properties();
        File config = new File(modConfigDir, "polychat.properties");

        if (config.exists() && config.isFile()) {
            try (FileInputStream istream = new FileInputStream(config)) {
                ModClass.properties.load(istream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            ModClass.properties.setProperty("address", "127.0.0.1");
            ModClass.properties.setProperty("port", "25566");
            ModClass.properties.setProperty("server_id", "empty");
            try (FileOutputStream ostream = new FileOutputStream(config)) {
                ModClass.properties.store(ostream, null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //Initiates the connection to the main polychat server and sets up the message callback
    public static void handleClientConnection() {
        try {
            messageBus = new MessageBus(new Socket(properties.getProperty("address"), Integer.parseInt(properties.getProperty("port"))), EventListener::distributeMessage);
            messageBus.start();
        } catch (IOException e) {
            System.err.println("Failed to establish polychat connection!");
            e.printStackTrace();
        }
    }
}
