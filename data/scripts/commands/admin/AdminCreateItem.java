package commands.admin;

import l2d.ext.scripts.ScriptFile;
import l2d.game.handler.AdminCommandHandler;
import l2d.game.handler.IAdminCommandHandler;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.serverpackets.ItemList;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.ItemTable;
import l2d.util.Log;
import l2d.util.Rnd;

public class AdminCreateItem implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_itemcreate,
		admin_create_item,
		admin_spreaditem,
		admin_create_coin
	}

	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, L2Player activeChar)
	{
		Commands command = (Commands) comm;

		if(!activeChar.getPlayerAccess().UseGMShop)
			return false;

		switch(command)
		{
			case admin_itemcreate:
				AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");
				break;
			case admin_create_item:
				try
				{
					if(wordList.length == 3)
						createItem(activeChar, Integer.parseInt(wordList[1]), Integer.parseInt(wordList[2]));
					else if(wordList.length == 2)
						createItem(activeChar, Integer.parseInt(wordList[1]), 1);
				}
				catch(NumberFormatException nfe)
				{
					activeChar.sendMessage("Specify a valid number.");
				}
				catch(StringIndexOutOfBoundsException e)
				{
					activeChar.sendMessage("Can't create this item.");
				}
				AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");
				break;
			case admin_create_coin:
				try
				{
					if(wordList.length == 3)
					{
						int idval = getCoinId(wordList[1]);
						if(idval > 0)
						{
							int numval = Integer.parseInt(wordList[2]);
							createItem(activeChar, idval, numval);
						}
					}
					else if(wordList.length == 2)
					{
						String name = wordList[1];
						int idval = getCoinId(name);
						createItem(activeChar, idval, 1);
					}
				}
				catch(StringIndexOutOfBoundsException e)
				{
					activeChar.sendMessage("Usage: //create_coin <name> [amount]");
				}
				catch(NumberFormatException nfe)
				{
					activeChar.sendMessage("Specify a valid number.");
				}
				AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");
				break;
			case admin_spreaditem:
				try
				{
					int id = Integer.parseInt(wordList[1]);
					int num = (wordList.length > 2) ? Integer.parseInt(wordList[2]) : 1;
					int count = (wordList.length > 3) ? Integer.parseInt(wordList[3]) : 1;
					int radiusmin = (wordList.length > 4) ? Integer.parseInt(wordList[4]) : 0;
					int radius = (wordList.length > 5) ? Integer.parseInt(wordList[5]) : 100;
					for(int i = 0; i < num; i++)
					{
						L2ItemInstance createditem = ItemTable.getInstance().createItem(id);
						createditem.setCount(count);
						createditem.dropToTheGround(activeChar, Rnd.coordsRandomize(activeChar,radiusmin, radius));
					}
				}
				catch(NumberFormatException nfe)
				{
					activeChar.sendMessage("Specify a valid number.");
				}
				catch(StringIndexOutOfBoundsException e)
				{
					activeChar.sendMessage("Can't create this item.");
				}
				break;
		}

		return true;
	}

	public Enum[] getAdminCommandEnum()
	{
		return Commands.values();
	}

	private void createItem(L2Player activeChar, int id, int num)
	{
		L2ItemInstance createditem = ItemTable.getInstance().createItem(id);
		createditem.setCount(num);
		activeChar.getInventory().addItem(createditem);
		Log.LogItem(activeChar, Log.Adm_AddItem, createditem);
		if(!createditem.isStackable())
			for(long i = 0; i < num - 1; i++)
			{
				createditem = ItemTable.getInstance().createItem(id);
				activeChar.getInventory().addItem(createditem);
				Log.LogItem(activeChar, Log.Adm_AddItem, createditem);
			}
		activeChar.sendPacket(new ItemList(activeChar, true));
		if(id == 57)
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S1_ADENA).addNumber((int) num));
		else if(num > 1)
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S2_S1S).addItemName(id).addNumber((int) num));
		else
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S1).addItemName(id));

		Log.add("spawned " + num + " item(s) number " + id + " in inventory", "gm_ext_actions", activeChar);
	}

	private int getCoinId(String name)
	{
		int id;
		if(name.equalsIgnoreCase("adena"))
			id = 57;
		else if(name.equalsIgnoreCase("ancientadena"))
			id = 5575;
		else if(name.equalsIgnoreCase("festivaladena"))
			id = 6673;
		else if(name.equalsIgnoreCase("blueeva"))
			id = 4355;
		else if(name.equalsIgnoreCase("goldeinhasad"))
			id = 4356;
		else if(name.equalsIgnoreCase("silvershilen"))
			id = 4357;
		else if(name.equalsIgnoreCase("bloodypaagrio"))
			id = 4358;
		else if(name.equalsIgnoreCase("SanctityCrystal"))
			id = 7917;
		else
			id = 0;
		return id;
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