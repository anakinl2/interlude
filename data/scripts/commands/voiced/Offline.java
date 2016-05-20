package commands.voiced;

import com.lineage.Config;
import com.lineage.ext.multilang.CustomMessage;
import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import l2d.game.handler.IVoicedCommandHandler;
import l2d.game.handler.VoicedCommandHandler;
import l2d.game.model.L2Player;
import l2d.game.model.L2Zone;
import l2d.game.model.entity.olympiad.Olympiad;

public class Offline extends Functions implements IVoicedCommandHandler, ScriptFile
{
	private String[] _commandList = new String[] { "offline", "ghost" };

	public void onLoad()
	{
		if(Config.SERVICES_OFFLINE_TRADE_ALLOW)
			VoicedCommandHandler.getInstance().registerVoicedCommandHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}

	public boolean useVoicedCommand(String command, L2Player activeChar, String args)
	{
		if(activeChar.getOlympiadGameId() != -1 || Olympiad.isRegisteredInComp(activeChar))
		{
			activeChar.sendActionFailed();
			return false;
		}

		if(activeChar.getLevel() < Config.SERVICES_OFFLINE_TRADE_MIN_LEVEL)
		{
			show(new CustomMessage("scripts.commands.user.offline.LowLevel", activeChar).addNumber(Config.SERVICES_OFFLINE_TRADE_MIN_LEVEL), activeChar);
			return false;
		}

		if(!activeChar.isInStoreMode())
		{
			show(new CustomMessage("scripts.commands.user.offline.IncorrectUse", activeChar), activeChar);
			return false;
		}

		if(activeChar.getNoChannelRemained() > 0)
		{
			show(new CustomMessage("scripts.commands.user.offline.BanChat", activeChar), activeChar);
			return false;
		}

		if(activeChar.isActionBlocked(L2Zone.BLOCKED_ACTION_PRIVATE_STORE))
		{
			activeChar.sendMessage(new CustomMessage("trade.OfflineNoTradeZone", activeChar));
			return false;
		}

		if(Config.SERVICES_OFFLINE_TRADE_PRICE > 0 && Config.SERVICES_OFFLINE_TRADE_PRICE_ITEM > 0)
		{
			if(getItemCount(activeChar, Config.SERVICES_OFFLINE_TRADE_PRICE_ITEM) < Config.SERVICES_OFFLINE_TRADE_PRICE)
			{
				show(new CustomMessage("scripts.commands.user.offline.NotEnough", activeChar).addItemName(Config.SERVICES_OFFLINE_TRADE_PRICE_ITEM).addNumber(Config.SERVICES_OFFLINE_TRADE_PRICE), activeChar);
				return false;
			}
			removeItem(activeChar, Config.SERVICES_OFFLINE_TRADE_PRICE_ITEM, Config.SERVICES_OFFLINE_TRADE_PRICE);
		}

		if(activeChar.getPet() != null)
			activeChar.getPet().unSummon();

		activeChar.offline();
		return true;
	}

	public String[] getVoicedCommandList()
	{
		return _commandList;
	}
}