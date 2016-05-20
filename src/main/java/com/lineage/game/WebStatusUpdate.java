package com.lineage.game;

import java.io.IOException;
import java.sql.ResultSet;

import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import com.lineage.game.model.L2World;
import com.lineage.util.Files;
import cz.dhl.ftp.Ftp;
import cz.dhl.ftp.FtpConnect;
import cz.dhl.ftp.FtpFile;
import cz.dhl.ftp.FtpOutputStream;

/**
 * 
 * @author Midnex
 *
 */
public class WebStatusUpdate
{
	private  FtpConnect _connection = null;
	private  Ftp _client = new Ftp();
	private static  WebStatusUpdate _instance;
	private int _trys = 50;

	public void initConnection()
	{
		_connection = FtpConnect.newConnect("ftp://78.47.86.62/");
		_connection.setUserName("lgalloco");
		_connection.setPassWord("o44jv0XnJ8");
		try
		{
			_client.connect(_connection);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	

	public void updateOnline(boolean shutdown)
	{
		if(!_client.isConnected())
			initConnection();
		try
		{
			_trys++;
			FtpFile file = new FtpFile("/domains/l2impulse.com/public_html/templates/impulse/GameStatus.txt", _client);
			FtpOutputStream out = new FtpOutputStream(file);
			
			String write = shutdown ? "0\n0" : System.currentTimeMillis() + "\n" + L2World.getAllPlayersCount();
			out.write(write.getBytes());			
			out.close();
			
			if(_trys >= 30)
				updateTops();
		}
		catch(IOException e)
		{
		}
	}

	public void updateTops()
	{
		String html = Files.read("data/topusers.tpl");
		ThreadConnection con = null;
		FiltredStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.createStatement();
			rset = statement.executeQuery("SELECT char_name,pvpkills,clan_name FROM characters LEFT JOIN `clan_data` ON (`clanid` = `clan_id`) ORDER BY pvpkills DESC limit 5");

			int top = 0;
			while(rset.next())
			{
				top++;
				String name = rset.getString("char_name");
				int kills = rset.getInt("pvpkills");
				String clan = rset.getString("clan_name");
				
				html  = html.replaceAll("%top"+top+"_name%", name);
				html  = html.replaceAll("%top"+top+"_kills%", kills+"");
				html  = html.replaceAll("%top"+top+"_clan%", clan==null? "-" : clan);
			}
		}
		catch(Exception e)
		{}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
			sendTops(html);
			_trys = 0;
		}
	}
	
	public void sendTops(String html)
	{
		try
		{
			FtpFile file = new FtpFile("/domains/l2impulse.com/public_html/templates/impulse/topusers.tpl", _client);
			FtpOutputStream out = new FtpOutputStream(file);
			out.write(html.getBytes());
			out.close();
		}
		catch(IOException e)
		{}
	}
	
	public void onShutDown()
	{
		updateOnline(true);
		_client.disconnect();
	}
	
	public static WebStatusUpdate getInstance()
	{
		if(_instance == null)
			_instance = new WebStatusUpdate();
		return _instance;
	}
}
