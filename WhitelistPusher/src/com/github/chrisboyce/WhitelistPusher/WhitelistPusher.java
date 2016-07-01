package com.github.chrisboyce.WhitelistPusher;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;


public class WhitelistPusher extends JavaPlugin{

	String referralSalt;
	String statusSalt;
	private Connection con;
	@Override
	public void onDisable() {
		
	}
	
	@Override
	public void onEnable() {
		initConfig();
		try {
			connect();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void initConfig(){
		FileConfiguration config = getConfig();
	
		config.addDefault("database.mysql.host", "127.0.0.1");
		config.addDefault("database.mysql.username","root");
		config.addDefault("database.mysql.password","");
		config.addDefault("database.mysql.port", "3306");
		config.addDefault("database.mysql.database","minecraft");
		config.addDefault("database.mysql.table", "users");
		config.addDefault("salt.referral", GenerateString(50));
		config.addDefault("salt.status",GenerateString(50));
		this.getConfig().options().copyDefaults(true);
		saveConfig();
		referralSalt = config.getString("salt.referral");
		statusSalt = config.getString("salt.status");
	}
	
	public static String GenerateString(int length){
		Random r = new Random();
		return new BigInteger(length, r).toString(32);
	}
	
	private void connect() throws ClassNotFoundException, SQLException{
		FileConfiguration config = getConfig();
		Class.forName("com.mysql.jdbc.Driver");
		String url =
			"jdbc:mysql://" + config.getString("database.mysql.host") + ":" + 
			config.getString("database.mysql.port")+ "/" +
			config.getString("database.mysql.database");
		 con = DriverManager.getConnection(
		 url,config.getString("database.mysql.username"),config.getString("database.mysql.password"));
	}
	
	public static String MD5Sum(String input){
		try{
			MessageDigest md = MessageDigest.getInstance("MD5");
	        
	 
	        byte[] mdbytes = md.digest(input.getBytes());
	 
	        
	        StringBuffer sb = new StringBuffer();
	        for (int i = 0; i < mdbytes.length; i++) {
	          sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
	        }
	 
	        return sb.toString();
	 
		}catch(Exception e){
			e.printStackTrace();
			return "";
		}
	}
	
	private void whitelistUser(Player senderPlayer,String player){
		whitelistUser(senderPlayer, player,senderPlayer.getName());
	}
	
	private void whitelistUser(Player senderPlayer,String player, String referrer){
		int playerId = getPlayerID(player);
		int referrerId = getPlayerID(referrer);
		
		if(playerId != 0){
			senderPlayer.sendMessage("User [" + player + "] already exists.");
			return;
		}
		if(referrerId == 0){
			senderPlayer.sendMessage("Referring player [" + referrer + "] could not be found");
			return;
		}
		
		try {
			PreparedStatement p = con.prepareStatement(
					"INSERT INTO users SET username = ?, " +
					"referred_by_id = ?," +
					"referral_key = ?," +
					"status = 'approved'," +
					"date_created = NOW()," +
					"status_key = ?," + 
					"email = 'n/a'," +
					"comments = ''");
			p.setString(1, player);
			p.setInt(2, referrerId);
			p.setString(3,MD5Sum(referralSalt + player));
			p.setString(4,MD5Sum(statusSalt + player));
			p.executeUpdate();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		CommandSender console = getServer().getConsoleSender();
		getServer().dispatchCommand(console, "whitelist add " + player);
		getServer().dispatchCommand(console, "pex group user1 user add " + player);
		
		
	}
	public int getPlayerID(String player){
		try{
			PreparedStatement p = con.prepareStatement("SELECT * FROM users WHERE username = ?");
			p.setString(1,player);
			ResultSet rs = p.executeQuery();
			
			while(rs.next()){
				return rs.getInt("id");
			}
		} catch(SQLException e){
			e.printStackTrace();
		}
		return 0;
	}
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
    	Player commandPlayer = (Player) sender;
    	if(args.length == 1){
    		whitelistUser(commandPlayer,args[0]);
    		return true;
    	}else if(args.length == 2){
    		whitelistUser(commandPlayer,args[0],args[1]);
    		return true;
    	}
    	return false; 
    }



}
