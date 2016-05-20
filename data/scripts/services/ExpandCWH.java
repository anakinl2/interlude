package services;

import com.lineage.Config;
import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.cache.Msg;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.tables.ItemTable;
import com.lineage.game.templates.L2Item;

public class ExpandCWH extends Functions implements ScriptFile
{
	public static L2Object self;
	public static L2Object npc;

	public void get()
	{
		L2Player player = (L2Player) self;
		if(player == null)
			return;

		if(!Config.SERVICES_EXPAND_CWH_ENABLED)
		{
			show("Сервис отключен.", player);
			return;
		}

		if(player.getClan() == null)
		{
			player.sendMessage("You must be in clan.");
			return;
		}

		L2Item item = ItemTable.getInstance().getTemplate(Config.SERVICES_EXPAND_CWH_ITEM);
		L2ItemInstance pay = player.getInventory().getItemByItemId(item.getItemId());
		if(pay != null && pay.getCount() >= Config.SERVICES_EXPAND_CWH_PRICE)
		{
			player.getInventory().destroyItem(pay, Config.SERVICES_EXPAND_CWH_PRICE, true);
			player.getClan().setWhBonus(player.getClan().getWhBonus() + 1);
			player.sendMessage("Warehouse capacity is now " + (Config.WAREHOUSE_SLOTS_CLAN + player.getClan().getWhBonus()));
		}
		else if(Config.SERVICES_EXPAND_CWH_ITEM == 57)
			player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
		else
			player.sendPacket(Msg.INCORRECT_ITEM_COUNT);

		show();
	}

	public void show()
	{
		L2Player player = (L2Player) self;
		if(player == null)
			return;

		if(!Config.SERVICES_EXPAND_WAREHOUSE_ENABLED)
		{
			show("Сервис отключен.", player);
			return;
		}

		if(player.getClan() == null)
		{
			player.sendMessage("You must be in clan.");
			return;
		}

		L2Item item = ItemTable.getInstance().getTemplate(Config.SERVICES_EXPAND_CWH_ITEM);

		String out = "";

		out += "<html><body>Расширение кланового склада";
		out += "<br><br><table>";
		out += "<tr><td>Текущий размер:</td><td>" + (Config.WAREHOUSE_SLOTS_CLAN + player.getClan().getWhBonus()) + "</td></tr>";
		out += "<tr><td>Стоимость слота:</td><td>" + Config.SERVICES_EXPAND_CWH_PRICE + " " + item.getName() + "</td></tr>";
		out += "</table><br><br>";
		out += "<button width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\" action=\"bypass -h scripts_services.ExpandCWH:get\" value=\"Расширить\">";
		out += "</body></html>";

		show(out, player);
	}

	public void onLoad()
	{
		System.out.println("Loaded Service: Expand CWH");
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}