package l2d.game.loginservercon;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.util.logging.Logger;

import javolution.util.FastList;
import l2d.Config;
import l2d.auth.crypt.ConnectionCrypt;
import l2d.auth.crypt.ConnectionCryptDummy;
import l2d.auth.crypt.NewCrypt;
import l2d.game.ThreadPoolManager;
import l2d.game.loginservercon.gspackets.AuthRequest;
import l2d.game.loginservercon.gspackets.BlowFishKey;
import l2d.game.loginservercon.gspackets.GameServerBasePacket;
import l2d.game.loginservercon.lspackets.LoginServerBasePacket;
import l2d.util.Rnd;
import l2d.util.Util;

/**
 * @Author: Death
 * @Date: 13/11/2007
 * @Time: 16:42:49
 */
public class AttLS
{
	private static final Logger log = Logger.getLogger(AttLS.class.getName());

	private final FastList<GameServerBasePacket> sendPacketQueue = FastList.newInstance();
	private final ByteBuffer readBuffer = ByteBuffer.allocate(64 * 1024).order(ByteOrder.LITTLE_ENDIAN);

	private final SelectionKey key;
	private final LSConnection con;
	private ConnectionCrypt crypt;
	private RSACrypt rsa;
	private boolean licenseShown = true;

	public AttLS(SelectionKey key, LSConnection con)
	{
		this.key = key;
		this.con = con;
	}

	public void sendPacket(GameServerBasePacket packet)
	{

		if(LSConnection.DEBUG_GS_LS)
			log.info("GS Debug: Trying to add packet to sendQueue");

		if(!key.isValid())
			return;

		synchronized (sendPacketQueue)
		{
			if(crypt == null || con.isShutdown())
				return;

			sendPacketQueue.addLast(packet);
		}

		if(LSConnection.DEBUG_GS_LS)
			log.info("GS Debug: packet added.");
	}

	public void processData()
	{
		ByteBuffer buf = readBuffer;

		int position = buf.position();
		if(position < 2) // У нас недостаточно данных для получения длинны пакета
			return;

		// Получаем длинну пакета
		int lenght = Util.getPacketLength(buf.get(0), buf.get(1));

		// Пакетик не дошел целиком, ждем дальше
		if(lenght > position)
			return;

		byte[] data = new byte[position];
		for(int i = 0; i < position; i++)
			data[i] = buf.get(i);

		buf.clear();

		while((lenght = Util.getPacketLength(data[0], data[1])) <= data.length)
		{
			data = processPacket(data, lenght);
			if(data.length < 2)
				break;
		}

		buf.put(data);
	}

	private byte[] processPacket(byte[] data, int lenght)
	{
		byte[] remaining = new byte[data.length - lenght];
		byte[] packet = new byte[lenght - 2];

		System.arraycopy(data, 2, packet, 0, lenght - 2);
		System.arraycopy(data, lenght, remaining, 0, remaining.length);

		LoginServerBasePacket runnable = PacketHandler.handlePacket(packet, this);
		if(runnable != null)
		{
			if(LSConnection.DEBUG_GS_LS)
				log.info("GameServer: Reading packet from login: " + runnable.getClass().getSimpleName());
			ThreadPoolManager.getInstance().executeLSGSPacket(runnable);
		}

		return remaining;
	}

	public void initCrypt()
	{
		if(LSConnection.DEBUG_GS_LS)
			log.info("GS Debug: Initializing crypt.");

		byte[] data = null;
		if(Config.GAME_SERVER_LOGIN_CRYPT)
		{
			data = new byte[Rnd.get(15, 30)];
			for(int i = 0; i < data.length; i++)
				data[i] = (byte) Rnd.get(256);
		}

		synchronized (sendPacketQueue)
		{
			crypt = data == null ? ConnectionCryptDummy.instance : new NewCrypt(data);
			sendPacketQueue.addFirst(new AuthRequest());
			sendPacketQueue.addFirst(new BlowFishKey(data, this));
		}

		if(LSConnection.DEBUG_GS_LS)
			log.info("GS Debug: Crypt initialized, packets added to sendQueue");
	}

	public void initRSA(byte[] data)
	{
		byte[] wholeKey = new byte[129];
		wholeKey[0] = 0;
		System.arraycopy(data, 0, wholeKey, 1, 128);
		try
		{
			rsa = new RSACrypt(wholeKey);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public RSACrypt getRsa()
	{
		return rsa;
	}

	public void close()
	{
		FastList.recycle(sendPacketQueue);
	}

	public FastList<GameServerBasePacket> getSendPacketQueue()
	{
		return sendPacketQueue;
	}

	public byte[] encrypt(byte[] data) throws IOException
	{
		return crypt.crypt(data);
	}

	public byte[] decrypt(byte[] data) throws IOException
	{
		if(crypt == null)
			return data;
		return crypt.decrypt(data);
	}

	public ByteBuffer getReadBuffer()
	{
		return readBuffer;
	}

	public LSConnection getCon()
	{
		return con;
	}

	public SelectionKey getKey()
	{
		return key;
	}

	public boolean isLicenseShown()
	{
		return licenseShown;
	}

	public void setLicenseShown(boolean licenseShown)
	{
		this.licenseShown = licenseShown;
	}
}
