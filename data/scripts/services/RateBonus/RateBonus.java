package services.RateBonus;

import java.util.Date;

import com.lineage.Config;
import com.lineage.db.mysql;
import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.cache.Msg;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.tables.ItemTable;
import com.lineage.game.templates.L2Item;
import com.lineage.util.Files;

public class RateBonus extends Functions implements ScriptFile
{
	public static L2Object self;
	public static L2Object npc;

	public void list()
	{
		L2Player player = (L2Player) self;
		String html;
		if(player.getNetConnection().getBonus() == 1)
		{
			html = Files.read("data/scripts/services/RateBonus/RateBonus.htm", player);

			String add = new String();
			for(int i = 0; i < Config.SERVICES_RATE_BONUS_DAYS.length; i++)
				add += "<a action=\"bypass -h scripts_services.RateBonus.RateBonus:get " + i + "\">" //
						+ ((int) (Config.SERVICES_RATE_BONUS_VALUE[i] * 100 - 100)) + //
						"% for " + Config.SERVICES_RATE_BONUS_DAYS[i] + //
						" days - " + Config.SERVICES_RATE_BONUS_PRICE[i] + //
						" " + ItemTable.getInstance().getTemplate(Config.SERVICES_RATE_BONUS_ITEM[i]).getName() + "</a><br>";

			html = html.replaceFirst("%toreplace%", add);
		}
		else if(player.getNetConnection().getBonus() > 1)
		{
			long endtime = player.getNetConnection().getBonusExpire();
			if(endtime >= 0)
				html = Files.read("data/scripts/services/RateBonus/RateBonusAlready.htm", player).replaceFirst("endtime", new Date(endtime * 1000).toString());
			else
				html = Files.read("data/scripts/services/RateBonus/RateBonusInfinite.htm", player);
		}
		else
			html = Files.read("data/scripts/services/RateBonus/RateBonusNo.htm", player);
		show(html, player);
	}

	public void get(String[] param)
	{
		L2Player player = (L2Player) self;

		int i = Integer.parseInt(param[0]);

		L2Item item = ItemTable.getInstance().getTemplate(Config.SERVICES_RATE_BONUS_ITEM[i]);
		L2ItemInstance pay = player.getInventory().getItemByItemId(item.getItemId());
		if(pay != null && pay.getCount() >= Config.SERVICES_RATE_BONUS_PRICE[i])
		{
			player.getInventory().destroyItem(pay, Config.SERVICES_RATE_BONUS_PRICE[i], true);
			mysql.set("UPDATE `" + Config.GAME_SERVER_LOGIN_DB + "`.`accounts` SET `bonus` = '" + Config.SERVICES_RATE_BONUS_VALUE[i] + "',`bonus_expire` = UNIX_TIMESTAMP()+" + Config.SERVICES_RATE_BONUS_DAYS[i] + "*24*60*60 WHERE `login` = '" + player.getAccountName() + "'");
			player.getNetConnection().setBonus(Config.SERVICES_RATE_BONUS_VALUE[i]);
			player.getNetConnection().setBonusExpire(System.currentTimeMillis() / 1000 + Config.SERVICES_RATE_BONUS_DAYS[i] * 24 * 60 * 60);
			player.restoreBonus();
			if(player.getParty() != null)
				player.getParty().recalculatePartyData();
			show(Files.read("data/scripts/services/RateBonus/RateBonusGet.htm", player), player);
		}
		else if(Config.SERVICES_RATE_BONUS_ITEM[i] == 57)
			player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
		else
			player.sendPacket(Msg.INCORRECT_ITEM_COUNT);
	}

	public void howtogetcol()
	{
		show("data/scripts/services/RateBonus/howtogetcol.htm", (L2Player) self);
	}

	public void onLoad()
	{
		System.out.println("Loaded Service: Rate bonus");
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}