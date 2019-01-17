# PolyChat Client

This is built using "gradlew build" and uses the recommended Forge build 14.23.5.2768 for MC 1.12.2.

PolyChat Client is a mod that is placed in an MC server and connects to a running instance of Server using NetworkLibrary from <https://github.com/john01dav/polychat>.
To compile this project, place NetworkLibrary.jar in a folder named "libs" at the top level of this project directory.

NetworkLibrary.jar will also need to be placed in the mods folder of the desired MC server, so it is loaded for this mod to use.
Chat, player join/leave events, and server online/offline/crashed events are broadcast and received using this system.

## Config file for Client
`address`: This is the IP address of a running instance of Server to connect to.

`port`: This is the port of a running instance of Server.

`server_id`: This is used to identify the server of origin for chat messages as well as for player and server state events.  It is used for specifying a server in listplayers in Discord.

`server_name`: This is the full name of the server reported in listall.  An example is "All The Mods 3."

`server_address`: This is the address MC clients can use to connect to this server, displayed in listall.

`id_color`: This is the numerical id between 0 and 15 of the color to make the server id.  Any number outside this range will be discarded and white used as a default.


#### Color IDs:
| Color        	| ID 	|
|--------------	|----	|
| BLACK        	| 0  	|
|  DARK_BLUE   	| 1  	|
| DARK_GREEN   	| 2  	|
| DARK_AQUA    	| 3  	|
| DARK_RED     	| 4  	|
| DARK_PURPLE  	| 5  	|
| GOLD         	| 6  	|
| GRAY         	| 7  	|
| DARK_GRAY    	| 8  	|
| BLUE         	| 9  	|
| GREEN        	| 10 	|
| AQUA         	| 11 	|
| RED          	| 12 	|
| LIGHT_PURPLE 	| 13 	|
| YELLOW       	| 14 	|
| WHITE        	| 15 	|