package services;

import com.lineage.Config;
import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import l2d.game.cache.Msg;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.tables.ItemTable;
import l2d.game.templates.L2Item;

public class NickColor extends Functions implements ScriptFile
{
	public static L2Object self;
	public static L2Object npc;

	public void list()
	{
		StringBuilder append = new StringBuilder();
		append.append("Вы можете изменить цвет вашего ника за небольшую плату ").append(Config.SERVICES_CHANGE_NICK_COLOR_PRICE).append(" ").append(ItemTable.getInstance().getTemplate(Config.SERVICES_CHANGE_NICK_COLOR_ITEM).getName()).append(".");
		append.append("<br>Доступные цвета:<br>");
		for(String color : Config.SERVICES_CHANGE_NICK_COLOR_LIST)
			append.append("<br><a action=\"bypass -h scripts_services.NickColor:change ").append(color).append("\"><font color=\"").append(color.substring(4, 6) + color.substring(2, 4) + color.substring(0, 2)).append("\">").append(color.substring(4, 6) + color.substring(2, 4) + color.substring(0, 2)).append("</font></a>");
		append.append("<br><a action=\"bypass -h scripts_services.NickColor:change FFFFFF\"><font color=\"FFFFFF\">Вернуть обратно (бесплатно)</font></a>");
		show(append.toString(), (L2Player) self);
	}

	public void change(String[] param)
	{
		L2Player player = (L2Player) self;

		if(param[0].equalsIgnoreCase("FFFFFF"))
		{
			player.setNameColor(Integer.decode("0xFFFFFF"));
			player.broadcastUserInfo(true);
			return;
		}

		L2Item item = ItemTable.getInstance().getTemplate(Config.SERVICES_CHANGE_NICK_COLOR_ITEM);
		L2ItemInstance pay = player.getInventory().getItemByItemId(item.getItemId());
		if(pay != null && pay.getCount() >= Config.SERVICES_CHANGE_NICK_COLOR_PRICE)
		{
			player.getInventory().destroyItem(pay, Config.SERVICES_CHANGE_NICK_COLOR_PRICE, true);
			player.setNameColor(Integer.decode("0x" + param[0]));
			player.broadcastUserInfo(true);
		}
		else if(Config.SERVICES_CHANGE_NICK_COLOR_ITEM == 57)
			player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
		else
			player.sendPacket(Msg.INCORRECT_ITEM_COUNT);
	}

	public void onLoad()
	{
		System.out.println("Loaded Service: Nick color change");
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}