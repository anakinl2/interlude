package events.enchanter;

import javolution.text.TextBuilder;
import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.Announcements;
import com.lineage.game.instancemanager.ServerVariables;
import com.lineage.Config;
import com.lineage.game.model.Inventory;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.serverpackets.InventoryUpdate;
import com.lineage.game.serverpackets.NpcHtmlMessage;
import com.lineage.game.tables.ItemTable;
import com.lineage.util.Files;
import com.lineage.util.Log;

public class Main extends Functions implements ScriptFile
{
	private static boolean _active = false;

	private static boolean isActive()
	{
		return ServerVariables.getString("Enchanter", "off").equalsIgnoreCase("on");
	}

	public void onLoad()
	{
		if(isActive())
		{
			_active = true;
			System.out.println("Loaded Event: Enchanter [state: activated]");
		}
		else
			System.out.println("Loaded Event: Enchanter [state: deactivated]");
	}

	public void onReload()
	{}

	public void onShutdown()
	{}

	public void startEvent()
	{
		L2Player player = (L2Player) self;
		if(!player.getPlayerAccess().IsEventGm)
			return;

		if(!isActive())
		{
			ServerVariables.set("Enchanter", "on");
			_active = true;
			System.out.println("Event: Enchanter started.");
			Announcements.getInstance().announceByCustomMessage("scripts.events.enchanter.Main.AnnounceEventStarted", null);
		}
		else
			player.sendMessage("Event 'Enchanter' already started.");
		_active = true;
		show(Files.read("data/html/admin/events.htm", player), player);
	}

	public void stopEvent()
	{
		L2Player player = (L2Player) self;
		if(!player.getPlayerAccess().IsEventGm)
			return;
		if(isActive())
		{
			ServerVariables.unset("Enchanter");
			System.out.println("Event: Enchanter stopped.");
			Announcements.getInstance().announceByCustomMessage("scripts.events.enchanter.Main.AnnounceEventStoped", null);
		}
		else
			player.sendMessage("Event 'Enchanter' not started.");
		_active = false;
		show(Files.read("data/html/admin/events.htm", player), player);
	}

	public void Chalange()
	{	
		L2Player player = (L2Player) self;
		if(!_active)
		{ 
			if(((L2Player) self).getVar("lang@").equalsIgnoreCase("ru"))
			{
			player.sendMessage("Евент временно отключен.");
			}
			else
			{
				player.sendMessage("Event temporarily disabled.");
			}
			return;
		}
		String name=ItemTable.getInstance().getTemplate(Config.ENCHANT_MASTER_PRICE_ID).getName();
		NpcHtmlMessage replay= new NpcHtmlMessage(5);
		TextBuilder replyMSG = new TextBuilder("<html>");
		if(((L2Player) self).getVar("lang@").equalsIgnoreCase("ru"))
		{
		replyMSG.append("Мастер заточки:<br>");
		replyMSG.append("Каждая заточка бижутерии стоит: "+Config.ENCHANT_MASTER_JEWEL_PRICE+" "+name+"<br>");
		replyMSG.append("Каждая заточка брони стоит: "+Config.ENCHANT_MASTER_ARMOR_PRICE+" "+name+"<br>");
		replyMSG.append("Каждая заточка оружия стоит: "+Config.ENCHANT_MASTER_WEAPON_PRICE+" "+name+"<br>");		
		replyMSG.append("<br>");
		replyMSG.append("<center>[Допустимая заточка: 0-"+Config.ENCHANT_MASTER_MAX+"]</center>");
		replyMSG.append("<center><edit var=\"menu_command\" width=100 height=15><br></center>");
		replyMSG.append("<center><button value=\"Нижнее белье\" action=\"bypass -h scripts_events.enchanter.Main:enchant 1 $menu_command\" width=100 height=20 back=\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
		replyMSG.append("<center><button value=\"Шлем\" action=\"bypass -h scripts_events.enchanter.Main:enchant 2 $menu_command\" width=100 height=20 back=\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
		/** TODO replyMSG.append("<center><button value=\"Cloak\" action=\"bypass -h scripts_events.enchanter.Main:enchant 3 $menu_command\" width=100 height=20 back=\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>"); **/
		replyMSG.append("<center><button value=\"Ожерелье\" action=\"bypass -h scripts_events.enchanter.Main:enchant 4 $menu_command\" width=100 height=20 back=\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
		replyMSG.append("<center><button value=\"Оружие\" action=\"bypass -h scripts_events.enchanter.Main:enchant 5 $menu_command\" width=100 height=20 back=\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
		replyMSG.append("<center><button value=\"Верхняя броня\" action=\"bypass -h scripts_events.enchanter.Main:enchant 6 $menu_command\" width=100 height=20 back=\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
		replyMSG.append("<center><button value=\"Щит\" action=\"bypass -h scripts_events.enchanter.Main:enchant 7 $menu_command\" width=100 height=20 back=\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
		replyMSG.append("<center><button value=\"Правая серьга\" action=\"bypass -h scripts_events.enchanter.Main:enchant 8 $menu_command\" width=100 height=20 back=\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
		replyMSG.append("<center><button value=\"Левая серьга\" action=\"bypass -h scripts_events.enchanter.Main:enchant 9 $menu_command\" width=100 height=20 back=\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
		replyMSG.append("<center><button value=\"Перчатки\" action=\"bypass -h scripts_events.enchanter.Main:enchant 10 $menu_command\" width=100 height=20 back=\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
		replyMSG.append("<center><button value=\"Нижняя броня\" action=\"bypass -h scripts_events.enchanter.Main:enchant 11 $menu_command\" width=100 height=20 back=\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
		replyMSG.append("<center><button value=\"Обувь\" action=\"bypass -h scripts_events.enchanter.Main:enchant 12 $menu_command\" width=100 height=20 back=\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
		replyMSG.append("<center><button value=\"Правое кольцо\" action=\"bypass -h scripts_events.enchanter.Main:enchant 13 $menu_command\" width=100 height=20 back=\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
		replyMSG.append("<center><button value=\"Левое кольцо\" action=\"bypass -h scripts_events.enchanter.Main:enchant 14 $menu_command\" width=100 height=20 back=\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
		replyMSG.append("<br>");
		}
		if(((L2Player) self).getVar("lang@").equalsIgnoreCase("en"))
		{
			replyMSG.append("Enchant Master<br>");
			replyMSG.append("Each enchant Jewelery price: "+Config.ENCHANT_MASTER_JEWEL_PRICE+" "+name+"<br>");
			replyMSG.append("Each enchant Armor price: "+Config.ENCHANT_MASTER_ARMOR_PRICE+" "+name+"<br>");
			replyMSG.append("Each enchant Weapon price: "+Config.ENCHANT_MASTER_WEAPON_PRICE+" "+name+"<br>");		
			replyMSG.append("<br>");
			replyMSG.append("<center>[Access enchant 0-"+Config.ENCHANT_MASTER_MAX+"]</center>");
			replyMSG.append("<center><edit var=\"menu_command\" width=100 height=15><center><br>");
			replyMSG.append("<center><button value=\"Underwear\" action=\"bypass -h scripts_events.enchanter.Main:enchant 1 $menu_command\" width=100 height=20 back=\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
			replyMSG.append("<center><button value=\"Helmet\" action=\"bypass -h scripts_events.enchanter.Main:enchant 2 $menu_command\" width=100 height=20 back=\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
			/** TODO replyMSG.append("<button value=\"Cloak\" action=\"bypass -h scripts_events.enchanter.Main:enchant 3 $menu_command\" width=100 height=20 back=\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>"); **/
			replyMSG.append("<center><button value=\"Necklace\" action=\"bypass -h scripts_events.enchanter.Main:enchant 4 $menu_command\" width=100 height=20 back=\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
			replyMSG.append("<center><button value=\"Weapon\" action=\"bypass -h scripts_events.enchanter.Main:enchant 5 $menu_command\" width=100 height=20 back=\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
			replyMSG.append("<center><button value=\"Chest\" action=\"bypass -h scripts_events.enchanter.Main:enchant 6 $menu_command\" width=100 height=20 back=\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
			replyMSG.append("<center><button value=\"Shield\" action=\"bypass -h scripts_events.enchanter.Main:enchant 7 $menu_command\" width=100 height=20 back=\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
			replyMSG.append("<center><button value=\"Right Earring\" action=\"bypass -h scripts_events.enchanter.Main:enchant 8 $menu_command\" width=100 height=20 back=\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
			replyMSG.append("<center><button value=\"Left Earring\" action=\"bypass -h scripts_events.enchanter.Main:enchant 9 $menu_command\" width=100 height=20 back=\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
			replyMSG.append("<center><button value=\"Gloves\" action=\"bypass -h scripts_events.enchanter.Main:enchant 10 $menu_command\" width=100 height=20 back=\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
			replyMSG.append("<center><button value=\"Leggings\" action=\"bypass -h scripts_events.enchanter.Main:enchant 11 $menu_command\" width=100 height=20 back=\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
			replyMSG.append("<center><button value=\"Boots\" action=\"bypass -h scripts_events.enchanter.Main:enchant 12 $menu_command\" width=100 height=20 back=\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
			replyMSG.append("<center><button value=\"Right Ring\" action=\"bypass -h scripts_events.enchanter.Main:enchant 13 $menu_command\" width=100 height=20 back=\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
			replyMSG.append("<center><button value=\"Left Ring\" action=\"bypass -h scripts_events.enchanter.Main:enchant 14 $menu_command\" width=100 height=20 back=\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
			replyMSG.append("</html>");
		}
		replay.setHtml(replyMSG.toString());
		player.sendPacket(replay);
	}
	public void enchant(String[] var)
	{	
		try {
		L2Player player = (L2Player) self;
		int type=Integer.parseInt(var[0]);
		int value=Integer.parseInt(var[1]);
		if(value>Config.ENCHANT_MASTER_MAX || value<0)
			
		{
			if(((L2Player) self).getVar("lang@").equalsIgnoreCase("en"))
			{
			player.sendMessage("Enchant value from 0 to "+Config.ENCHANT_MASTER_MAX);
			}
			else
			{
			player.sendMessage("Введите уровень заточки от 0 до "+Config.ENCHANT_MASTER_MAX);
			}
			
			
			return;
		}
		switch(type)
		{
			case 1:
				if(!checkCondition(player,Config.ENCHANT_MASTER_ARMOR_PRICE))
				{
					return;
				}else
				{	
					doItFaggot(Inventory.PAPERDOLL_UNDER,value,Config.ENCHANT_MASTER_ARMOR_PRICE);
					break;
				}
			case 2:
				if(!checkCondition(player,Config.ENCHANT_MASTER_ARMOR_PRICE))
				{
					return;
				}else
				{	
					doItFaggot(Inventory.PAPERDOLL_HEAD,value,Config.ENCHANT_MASTER_ARMOR_PRICE);
					break;
				}
/** TODO				
			case 3:
				if(!checkCondition(player,Config.ENCHANT_MASTER_ARMOR_PRICE))
				{
					return;
				}else
				{	
					doItFaggot(Inventory.PAPERDOLL_CLOAK,value,Config.ENCHANT_MASTER_ARMOR_PRICE);
				
					break;
				} **/
			case 4:
				if(!checkCondition(player,Config.ENCHANT_MASTER_ARMOR_PRICE))
				{
					return;
				}else
				{	
					doItFaggot(Inventory.PAPERDOLL_NECK,value,Config.ENCHANT_MASTER_ARMOR_PRICE);
					break;
				}
			case 5:
				if(!checkCondition(player,Config.ENCHANT_MASTER_WEAPON_PRICE))
				{
					return;
				}else
				{	
					doItFaggot(Inventory.PAPERDOLL_RHAND,value,Config.ENCHANT_MASTER_WEAPON_PRICE);
					break;
				}
			case 6:
				if(!checkCondition(player,Config.ENCHANT_MASTER_ARMOR_PRICE))
				{
					return;
				}else
				{	
					doItFaggot(Inventory.PAPERDOLL_CHEST,value,Config.ENCHANT_MASTER_ARMOR_PRICE);
					break;
				}
			case 7:
				if(!checkCondition(player,Config.ENCHANT_MASTER_ARMOR_PRICE))
				{
					return;
				}else
				{	
					doItFaggot(Inventory.PAPERDOLL_LHAND,value,Config.ENCHANT_MASTER_ARMOR_PRICE);
					break;
				}
			case 8:
				if(!checkCondition(player,Config.ENCHANT_MASTER_ARMOR_PRICE))
				{
					return;
				}else
				{	
					doItFaggot(Inventory.PAPERDOLL_REAR,value,Config.ENCHANT_MASTER_ARMOR_PRICE);
					break;
				}
			case 9:
				if(!checkCondition(player,Config.ENCHANT_MASTER_ARMOR_PRICE))
				{
					return;
				}else
				{	
					doItFaggot(Inventory.PAPERDOLL_LEAR,value,Config.ENCHANT_MASTER_ARMOR_PRICE);
					break;
				}
			case 10:
				if(!checkCondition(player,Config.ENCHANT_MASTER_ARMOR_PRICE))
				{
					return;
				}else
				{	
					doItFaggot(Inventory.PAPERDOLL_GLOVES,value,Config.ENCHANT_MASTER_ARMOR_PRICE);
					break;
				}
			case 11:
				if(!checkCondition(player,Config.ENCHANT_MASTER_ARMOR_PRICE))
				{
					return;
				}else
				{	
					doItFaggot(Inventory.PAPERDOLL_LEGS,value,Config.ENCHANT_MASTER_ARMOR_PRICE);
					break;
				}
			case 12:
				if(!checkCondition(player,Config.ENCHANT_MASTER_ARMOR_PRICE))
				{
					return;
				}else
				{	
					doItFaggot(Inventory.PAPERDOLL_FEET,value,Config.ENCHANT_MASTER_ARMOR_PRICE);
					break;
				}
			case 13:
				if(!checkCondition(player,Config.ENCHANT_MASTER_ARMOR_PRICE))
				{
					return;
				}else
				{	
					doItFaggot(Inventory.PAPERDOLL_RFINGER,value,Config.ENCHANT_MASTER_ARMOR_PRICE);
					break;
				}
			case 14:
				if(!checkCondition(player,Config.ENCHANT_MASTER_ARMOR_PRICE))
				{
					return;
				}else
				{	
					doItFaggot(Inventory.PAPERDOLL_LFINGER,value,Config.ENCHANT_MASTER_ARMOR_PRICE);
					break;
				}
		
		}
		} 
		catch (NumberFormatException e)
		{
			L2Player player = (L2Player) self;
			if(((L2Player) self).getVar("lang@").equalsIgnoreCase("ru"))
			{
			player.sendMessage("Введите уровень заточки от 0 до "+Config.ENCHANT_MASTER_MAX);
			}
			else
			{
			player.sendMessage("Enchant value from 0 to "+Config.ENCHANT_MASTER_MAX);	
			}				
			}
		catch (ArrayIndexOutOfBoundsException e)
		{
			L2Player player = (L2Player) self;
			if(((L2Player) self).getVar("lang@").equalsIgnoreCase("ru"))
			{
			player.sendMessage("Введите уровень заточки от 0 до "+Config.ENCHANT_MASTER_MAX);
			}
			else
			{
			player.sendMessage("Enchant value from 0 to "+Config.ENCHANT_MASTER_MAX);	
			}				
			}
	}

	public void doItFaggot(byte slot2,int ench,int price)
	{	L2Player player = (L2Player) self;
		int pr=ench*price;		
			if(!checkCondition(player,pr ))
			return;

		L2ItemInstance Slot = null;

		L2ItemInstance Type = player.getInventory().getPaperdollItem(slot2);

		if(Type != null && Type.getEquipSlot() == slot2)
			Slot = Type;
		else
		{
			// for bows and double handed weapons
			Type = player.getInventory().getPaperdollItem(slot2);
			if(Type != null && Type.getEquipSlot() == slot2)
				Slot = Type;
		}

		if(Slot != null)
		{
			removeItem(player, Config.ENCHANT_MASTER_PRICE_ID, pr);

			player.getInventory().unEquipItemInSlot(Slot.getEquipSlot());
			Slot.setEnchantLevel(ench);
			player.getInventory().equipItem(Slot);

			player.sendPacket(new InventoryUpdate().addModifiedItem(Slot));
			player.sendChanges();

			// сообщение
			if(((L2Player) self).getVar("lang@").equalsIgnoreCase("ru"))
				player.sendMessage("Ваш "+Slot.getItem().getName()+" был заточен на " + ench + ".");
			else
				player.sendMessage("Your "+Slot.getItem().getName()+" has been enchanted to " + ench + ".");

			Log.add(player + " enchant " + Slot, "Enchanter");
		}
	}
	public static boolean checkCondition(L2Player player, int ItemsCount)
	{
		synchronized (player)
		{
			String name=ItemTable.getInstance().getTemplate(Config.ENCHANT_MASTER_PRICE_ID).getName();
			L2ItemInstance Items = player.getInventory().getItemByItemId(Config.ENCHANT_MASTER_PRICE_ID);
			if(!_active || player == null|| Items==null)
				return false;

			

			if(Items == null)
				if(((L2Player) self).getVar("lang@").equalsIgnoreCase("ru"))
				{
				player.sendMessage("Нехватает "+name);
				}
				else
				{
					player.sendMessage("You have not "+name);
				}
			if(player.isActionsDisabled() || player.isSitting() || player.getLastNpc().getDistance(player) > 300)
				return false;
			if(player.getLastNpc().getNpcId() != npc.getNpcId())
				return false;

			if(ItemsCount != 0 && Items.getCount() < ItemsCount)
			{
				if(((L2Player) self).getVar("lang@").equalsIgnoreCase("ru"))
				{
				player.sendMessage("Нехватает "+name);
				}
				else
				{
					player.sendMessage("You have not"+name);
				}
				player.sendActionFailed();
				return false;
			}

			return true;
		}
	}
}