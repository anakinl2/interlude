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

public class ExpandWarhouse extends Functions implements ScriptFile
{
	public static L2Object self;
	public static L2Object npc;

	public void get()
	{
		L2Player player = (L2Player) self;
		if(player == null)
			return;

		if(!Config.SERVICES_EXPAND_WAREHOUSE_ENABLED)
		{
			show("Сервис отключен.", player);
			return;
		}

		L2Item item = ItemTable.getInstance().getTemplate(Config.SERVICES_EXPAND_WAREHOUSE_ITEM);
		L2ItemInstance pay = player.getInventory().getItemByItemId(item.getItemId());
		if(pay != null && pay.getCount() >= Config.SERVICES_EXPAND_WAREHOUSE_PRICE)
		{
			player.getInventory().destroyItem(pay, Config.SERVICES_EXPAND_WAREHOUSE_PRICE, true);
			player.setExpandWarehouse(player.getExpandWarehouse() + 1);
			player.setVar("ExpandWarehouse", String.valueOf(player.getExpandWarehouse()));
			player.sendMessage("Warehouse capacity is now " + player.getWarehouseLimit());
		}
		else if(Config.SERVICES_EXPAND_WAREHOUSE_ITEM == 57)
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

		L2Item item = ItemTable.getInstance().getTemplate(Config.SERVICES_EXPAND_WAREHOUSE_ITEM);

		String out = "";

		out += "<html><body>Расширение склада";
		out += "<br><br><table>";
		out += "<tr><td>Текущий размер:</td><td>" + player.getWarehouseLimit() + "</td></tr>";
		out += "<tr><td>Стоимость слота:</td><td>" + Config.SERVICES_EXPAND_WAREHOUSE_PRICE + " " + item.getName() + "</td></tr>";
		out += "</table><br><br>";
		out += "<button width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\" action=\"bypass -h scripts_services.ExpandWarhouse:get\" value=\"Расширить\">";
		out += "</body></html>";

		show(out, player);
	}

	public void onLoad()
	{
		System.out.println("Loaded Service: Expand Warehouse");
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}