package com.lineage.auth;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.lineage.auth.packet.server.Init;
import com.lineage.ext.network.HeaderInfo;
import com.lineage.ext.network.IAcceptFilter;
import com.lineage.ext.network.IClientFactory;
import com.lineage.ext.network.IMMOExecutor;
import com.lineage.ext.network.MMOConnection;
import com.lineage.ext.network.ReceivablePacket;
import com.lineage.ext.network.TCPHeaderHandler;

public class SelectorHelper extends TCPHeaderHandler<L2LoginClient> implements IMMOExecutor<L2LoginClient>, IClientFactory<L2LoginClient>, IAcceptFilter
{
	private ThreadPoolExecutor _generalPacketsThreadPool;

	public SelectorHelper()
	{
		super(null);
		_generalPacketsThreadPool = new ThreadPoolExecutor(4, 6, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	}

	@Override
	public void execute(ReceivablePacket<L2LoginClient> packet)
	{
		_generalPacketsThreadPool.execute(packet);
	}

	@Override
	public L2LoginClient create(MMOConnection<L2LoginClient> con)
	{
		L2LoginClient client = new L2LoginClient(con);
		client.sendPacket(new Init(client));
		return client;
	}

	@Override
	public boolean accept(SocketChannel sc)
	{
		return !LoginController.getInstance().isBannedAddress(sc.socket().getInetAddress());
	}

	@SuppressWarnings("unchecked")
	@Override
	public HeaderInfo<L2LoginClient> handleHeader(SelectionKey key, ByteBuffer buf)
	{
		if(buf.remaining() >= 2)
		{
			int dataPending = (buf.getShort() & 0xffff) - 2;
			L2LoginClient client = ((MMOConnection<L2LoginClient>) key.attachment()).getClient();
			return getHeaderInfoReturn().set(0, dataPending, false, client);
		}
		L2LoginClient client = ((MMOConnection<L2LoginClient>) key.attachment()).getClient();
		return getHeaderInfoReturn().set(2 - buf.remaining(), 0, false, client);
	}

}