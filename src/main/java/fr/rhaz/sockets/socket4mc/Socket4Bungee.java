package fr.rhaz.sockets.socket4mc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import fr.rhaz.sockets.client.SocketClient;
import fr.rhaz.sockets.client.SocketClientApp;
import fr.rhaz.sockets.server.SocketMessenger;
import fr.rhaz.sockets.server.SocketServer;
import fr.rhaz.sockets.server.SocketServerApp;
import net.md_5.bungee.api.plugin.Event;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class Socket4Bungee extends Plugin {

	private static Socket4Bungee i;
	private Configuration config;
	public static Server sapp;
	public static Client capp;
	private SocketServer server;

	@Override
	public void onEnable() {
		i = this;
		
		config = loadConfig("config.yml", "server.yml");
		sapp = new Server();
		capp = new Client();
		
		start();
	}
	
	public boolean start(){
		server = new SocketServer(sapp,
				config.getString("name", "Bungee"), 
				config.getInt("port", 25575), 
				config.getInt("security-level", 1)
		);
		
		IOException err = server.start();
		if(err != null){
			getLogger().warning("Could not start socket server on port "+server.getPort());
			err.printStackTrace();
			return false;
		} else {
			getLogger().info("Successfully started socket server on port "+server.getPort());
			return true;
		}
	}
	
	public boolean stop(){
		IOException err = server.close();
		if(err != null){
			getLogger().warning("Could not stop socket server on port "+server.getPort());
			err.printStackTrace();
			return false;
		} else {
			getLogger().info("Successfully stopped socket server on port "+server.getPort());
			return true;
		}
	}
	
	public void restart(){
		if(stop()){
			getProxy().getScheduler().schedule(this, new Runnable(){
				@Override
				public void run() {
					start();
				}
			}, 1000, TimeUnit.MILLISECONDS);
		}
	}
	
	public Configuration loadConfig(String name, String res){
		if (!getDataFolder().exists()) getDataFolder().mkdir();
        File file = new File(getDataFolder(), name);
        if (!file.exists()) {
            try {
				Files.copy(this.getResourceAsStream(res), file.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
        } try {
			return ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
		} catch (IOException e) {
			e.printStackTrace();
		} return null;
	}
	
	public static Socket4Bungee get() {
		return i;
	}
	
	public static class Server implements SocketServerApp {
		
		public static class ServerSocketConnectEvent extends Event {
			private SocketMessenger mess;
			
			public ServerSocketConnectEvent(SocketMessenger mess){
				this.mess = mess;
			}
			
			public SocketMessenger getMessenger(){
				return mess;
			}
		}
		
		public static class ServerSocketDisconnectEvent extends Event {
			private SocketMessenger mess;
			
			public ServerSocketDisconnectEvent(SocketMessenger mess){
				this.mess = mess;
			}
			
			public SocketMessenger getMessenger(){
				return mess;
			}
		}
		
		public static class ServerSocketJSONEvent extends Event {
			private SocketMessenger mess;
			private Map<String, Object> map;
			
			public ServerSocketJSONEvent(SocketMessenger mess, Map<String, Object> map){
				this.mess = mess;
				this.map = map;
			}
			
			public String getChannel(){
				return (String) map.get("channel");
			}
			
			@Deprecated
			public String getData(){
				return getExtraString("data");
			}
			
			@SuppressWarnings("unchecked")
			public <T> T getExtra(String key, Class<T> type) {
				return (T) map.get(key);
			}
			
			public String getExtraString(String key) {
				return getExtra(key, String.class);
			}
			
			public int getExtraInt(String key) {
				return getExtra(key, int.class);
			}
			
			@SuppressWarnings("unchecked")
			public Map<String, Object> getExtraMap(String key) {
				return getExtra(key, Map.class);
			}
			
			@Deprecated
			public Map<String, Object> getMap(){
				return map;
			}
			
			public String getName(){
				return mess.getName();
			}
			
			public SocketMessenger getMessenger(){
				return mess;
			}
			
			public void write(String data){
				mess.writeJSON(getChannel(), data);
			}
		}
		
		public static class ServerSocketHandshakeEvent extends Event {
			private String name;
			private SocketMessenger mess;
			
			public ServerSocketHandshakeEvent(SocketMessenger mess, String name){
				this.mess = mess;
				this.name = name;
			}
			
			public SocketMessenger getMessenger(){
				return mess;
			}
			
			public String getName(){
				return name;
			}
		}
	
		@Override
		public void run(SocketServer server){
			plugin().getProxy().getScheduler().runAsync(i, server);
		}
			
		@Override
		public void run(SocketMessenger mess){
			plugin().getProxy().getScheduler().runAsync(i, mess);
		}
		
		@Override
		public void onConnect(SocketMessenger mess) {
			plugin().getLogger().info("Successfully connected to "+mess.getSocket().getInetAddress().getHostAddress()+" on port "+mess.getSocket().getPort());
			plugin().getProxy().getPluginManager().callEvent(new ServerSocketConnectEvent(mess));
		}
	
		@Override
		public void onHandshake(SocketMessenger mess, String name) {
			plugin().getLogger().info("Successfully handshaked with "+mess.getSocket().getInetAddress().getHostAddress()+" on port "+mess.getSocket().getPort());
			plugin().getProxy().getPluginManager().callEvent(new ServerSocketHandshakeEvent(mess, name));
		}
	
		@Override
		public void onJSON(SocketMessenger mess, Map<String, Object> map) {
			plugin().getProxy().getPluginManager().callEvent(new ServerSocketJSONEvent(mess, map));
		}
	
		@Override
		public void onDisconnect(SocketMessenger mess) {
			plugin().getProxy().getPluginManager().callEvent(new ServerSocketDisconnectEvent(mess));
		}
	
		@Override
		public void log(String err) {
			if(plugin().config.getBoolean("debug", false)) plugin().getProxy().getLogger().info(err);
		}
		
		public Socket4Bungee plugin() {
			return Socket4Bungee.get();
		}
	}
	
	public static class Client implements SocketClientApp {
		
		public static class ClientSocketConnectEvent extends Event {
			private SocketClient client;
			
			public ClientSocketConnectEvent(SocketClient client){
				this.client = client;
			}
			
			public SocketClient getClient(){
				return client;
			}
		}
		
		public static class ClientSocketDisconnectEvent extends Event {
			
			private SocketClient client;

			public ClientSocketDisconnectEvent(SocketClient client){
				this.client = client;
			}
			
			public SocketClient getClient(){
				return client;
			}
		}
		
		public static class ClientSocketJSONEvent extends Event {
			private Map<String, Object> map;
			private SocketClient client;
			
			public ClientSocketJSONEvent(SocketClient client, Map<String, Object> map){
				this.client = client;
				this.map = map;
			}
			
			public String getChannel(){
				return (String) map.get("channel");
			}
			
			@Deprecated
			public String getData(){
				return getExtraString("data");
			}
			
			@SuppressWarnings("unchecked")
			public <T> T getExtra(String key, Class<T> type) {
				return (T) map.get(key);
			}
			
			public String getExtraString(String key) {
				return getExtra(key, String.class);
			}
			
			public int getExtraInt(String key) {
				return getExtra(key, int.class);
			}
			
			@SuppressWarnings("unchecked")
			public Map<String, Object> getExtraMap(String key) {
				return getExtra(key, Map.class);
			}
			
			@Deprecated
			public Map<String, Object> getMap(){
				return map;
			}
			
			public SocketClient getClient(){
				return client;
			}
			
			public void write(String data){
				client.writeJSON(getChannel(), data);
			}
		}
		
		public static class ClientSocketHandshakeEvent extends Event {
			private SocketClient client;

			public ClientSocketHandshakeEvent(SocketClient client){
				this.client = client;
			}
			
			public SocketClient getClient() {
				return client;
			}
		}
		
		@Override
		public void run(SocketClient client) {
			plugin().getProxy().getScheduler().runAsync(i, client);
		}
		
		@Override
		public void onConnect(SocketClient client) {
			plugin().getLogger().info("Successfully connected to "+client.getSocket().getInetAddress().getHostAddress()+" on port "+client.getSocket().getPort());
			plugin().getProxy().getPluginManager().callEvent(new ClientSocketConnectEvent(client));
		}
	
		@Override
		public void onHandshake(SocketClient client) {
			plugin().getLogger().info("Successfully handshaked with "+client.getSocket().getInetAddress().getHostAddress()+" on port "+client.getSocket().getPort());
			plugin().getProxy().getPluginManager().callEvent(new ClientSocketHandshakeEvent(client));
		}
	
		@Override
		public void onJSON(SocketClient client, Map<String, Object> map) {
			plugin().getProxy().getPluginManager().callEvent(new ClientSocketJSONEvent(client, map));
		}
	
		@Override
		public void onDisconnect(SocketClient client) {
			plugin().getProxy().getPluginManager().callEvent(new ClientSocketDisconnectEvent(client));
		}
	
		@Override
		public void log(String err) {
			if(plugin().config.getBoolean("debug", false)) plugin().getProxy().getLogger().info(err);
		}
		
		public Socket4Bungee plugin() {
			return Socket4Bungee.get();
		}
	}
}
