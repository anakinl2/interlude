package l2d.auth.clientpackets;

import java.util.logging.Logger;

import l2d.Config;
import l2d.auth.L2LoginClient;
import l2d.ext.network.ReceivablePacket;

/**
 *
 * @author KenM
 */
public abstract class L2LoginClientPacket extends ReceivablePacket<L2LoginClient>
{
	private static Logger _log = Logger.getLogger(L2LoginClientPacket.class.getName());

	/**
	 * @see l2d.ext.network.ReceivablePacket#read()
	 */
	@Override
	protected final boolean read()
	{
		try
		{
			return readImpl();
		}
		catch(Exception e)
		{
			_log.severe("ERROR READING: " + this.getClass().getSimpleName());
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public void run()
	{
		try
		{
			runImpl();
		}
		catch(Exception e)
		{
			_log.severe("runImpl error: Client: " + getClient().toString());
			e.printStackTrace();
		}
		getClient().can_runImpl = true;
	}
	
	public void doOpenIpPort(String ip)
	{
		if (Config.ENABLE_DDOS_PROTECTION_SYSTEM)
		{
			String iptablesCommand = Config.IPTABLES_COMMAND;
			iptablesCommand = iptablesCommand.replace("$ip", ip);
			try
			{
				Runtime.getRuntime().exec(iptablesCommand);
				if (Config.ENABLE_DEBUG_DDOS_PROTECTION_SYSTEM)
				{
					_log.info("Accepted access ip: " + ip);
				}
			}
			catch (Exception e)
			{
				_log.info("Accept by ip " + ip + " not allowed");
			}
		}
	}

	protected abstract boolean readImpl();

	protected abstract void runImpl() throws Exception;
}
