package services;

import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.ext.multilang.CustomMessage;
import l2d.game.model.L2Player;
import l2d.game.cache.Msg;
import l2d.game.tables.ItemTable;
import com.lineage.db.ThreadConnection;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.DatabaseUtils;
import com.lineage.util.Util;
import com.lineage.Config;

import java.sql.ResultSet;

public class ServiceClan extends Functions implements ScriptFile
{
	public static void clan_rename_page()
	{
		L2Player player = (L2Player) self;

		if(player == null)
			return;

		String append = "!Смена названия клана";
		append += "<br>";
		append += "<font color=\"LEVEL\">Введите новое название клана</font>";
		append += "Цена: " + Config.SERVICES_CHANGE_CLAN_PRICE + " " + ItemTable.getInstance().getTemplate(Config.SERVICES_CHANGE_CLAN_ITEM).getName() +".";
		append += "<table>";
		append += "<tr><td>Название: <edit var=\"new_clan_name\" width=80></td></tr>";
		append += "<tr><td><button value=\"Сменить\" action=\"bypass -h scripts_services.ServiceClan:clan_rename $new_clan_name\" width=80 height=15></td></tr>";
		append += "</table>";
		show(append, player);
	}

	public static void clan_rename_ok()
	{
		L2Player player = (L2Player) self;

		if(player == null)
			return;

		String append = "!Смена названия клана";
		append += "<br>";
		append += "Название успешно изменено. Изменений войдут в силу просле планового рестарта сервера.";
                show(append, player);
	}
	
	public static void clan_rename(String[] clan_name)
	{
		L2Player player = (L2Player) self;

		if(player == null)
			return;

		if(getItemCount(player, Config.SERVICES_CHANGE_CLAN_ITEM) < Config.SERVICES_CHANGE_CLAN_PRICE)
		{
			if(Config.SERVICES_CHANGE_CLAN_ITEM == 57)
				player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			else
				player.sendPacket(Msg.INCORRECT_ITEM_COUNT);
			return;
		}

		if(clan_name.length != 1)
		{
			player.sendMessage("Не коректное название");
			return;
		}

		String clanname = clan_name[0];

		if(!Util.isMatchingRegexp(clanname, Config.CLAN_NAME_TEMPLATE) && clanname.length() <= 16)
		{
			player.sendMessage("Не коректное название");
			return;
		}

		String name = clan_name[0];
	      	ThreadConnection connection = null;
		FiltredPreparedStatement offline = null;
		ResultSet rs = null;

		try
		{
			connection = L2DatabaseFactory.getInstance().getConnection();
			offline = connection.prepareStatement("SELECT clan_name FROM clan_data WHERE leader_id = ?");
			offline.setString(1, name);
			rs = offline.executeQuery();
			if(rs.next())
			{
				player.sendMessage("Такое название уже есть");
				return;
			}
		}
		catch(Exception e)
		{
			show(new CustomMessage("common.Error", player), player);
			return;
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(connection, offline, rs);
		}

		try
		{
			connection = L2DatabaseFactory.getInstance().getConnection();
			offline = connection.prepareStatement("UPDATE clan_data SET clan_name = ? WHERE leader_id = ?");
			offline.setString(1, name);
			offline.setInt(2, player.getObjectId());
			offline.executeUpdate();
		}
		catch(Exception e)
		{
			show(new CustomMessage("common.Error", player), player);
			return;
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(connection, offline);
		}
		clan_rename_ok();
	}

	public void onLoad()
	{}

	public void onReload()
	{}

	public void onShutdown()
	{}
}
