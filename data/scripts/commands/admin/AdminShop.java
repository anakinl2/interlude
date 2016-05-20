package commands.admin;

import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.TradeController;
import com.lineage.game.handler.AdminCommandHandler;
import com.lineage.game.handler.IAdminCommandHandler;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2TradeList;
import com.lineage.game.model.L2World;
import com.lineage.game.serverpackets.BuyList;

@SuppressWarnings("unused")
public class AdminShop implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_buy, admin_gmshop, admin_tax
	}

	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, L2Player activeChar)
	{
		Commands command = (Commands) comm;

		if( !activeChar.getPlayerAccess().UseGMShop)
			return false;

		if(fullString.startsWith("admin_buy"))
			try
			{
				handleBuyRequest(activeChar, fullString.substring(10));
			}
			catch(IndexOutOfBoundsException e)
			{
				activeChar.sendMessage("Please specify buylist.");
			}
		else if(fullString.equals("admin_gmshop"))
			AdminHelpPage.showHelpPage(activeChar, "gmshops.htm");
		else if(fullString.equals("admin_tax"))
			activeChar.sendMessage("TaxSum: " + L2World.getTaxSum());

		return true;
	}

	public Enum[] getAdminCommandEnum()
	{
		return Commands.values();
	}

	private void handleBuyRequest(L2Player activeChar, String command)
	{
		int val = -1;

		try
		{
			val = Integer.parseInt(command);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		L2TradeList list = TradeController.getInstance().getBuyList(val);

		if(list != null)
		{
			BuyList bl = new BuyList(list, activeChar);
			activeChar.sendPacket(bl);
		}

		activeChar.sendActionFailed();
	}

	public void onLoad()
	{
		AdminCommandHandler.getInstance().registerAdminCommandHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}