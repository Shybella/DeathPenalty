package fr.rhaz.sockets.socket4mc;

public class Socket4MC {
	
	public static Socket4Bukkit bukkit(){
		return Socket4Bukkit.get();
	}
	
	public static Socket4Bungee bungee(){
		return Socket4Bungee.get();
	}
}
