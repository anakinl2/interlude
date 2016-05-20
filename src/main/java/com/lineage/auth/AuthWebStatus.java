package com.lineage.auth;

import java.io.IOException;

import cz.dhl.ftp.Ftp;
import cz.dhl.ftp.FtpConnect;
import cz.dhl.ftp.FtpFile;
import cz.dhl.ftp.FtpOutputStream;

public class AuthWebStatus
{
	private static AuthWebStatus _instance;
	private  FtpConnect _connection = null;
	private  Ftp _client = new Ftp();
	
	public void initConnection()
	{
		_connection = FtpConnect.newConnect("ftp://78.47.86.62/");
		_connection.setUserName("lgalloco");
		_connection.setPassWord("o44jv0XnJ8");
	}
	
	public void updateOnline(boolean shutdown)
	{
		while(true)
		{
			if(!_client.isConnected())
				initConnection();
			try
			{
				_client.connect(_connection);
				FtpFile file = new FtpFile("/domains/l2impulse.com/public_html/templates/impulse/LoginStatus.txt", _client);
				FtpOutputStream out = new FtpOutputStream(file);
				
				String write = shutdown ? "0\n0" : System.currentTimeMillis()+"";
				out.write(write.getBytes());			
				out.close();
			}
			catch(IOException e)
			{
			}			
			try
			{
				Thread.sleep(10000);
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
			if(shutdown)
				break;
		}
	}
	
	public void onShutDown()
	{
		updateOnline(true);
		_client.disconnect();
	}
	
	public static AuthWebStatus getInstance()
	{
		if(_instance == null)
			_instance = new AuthWebStatus();
		return _instance;
	}
}