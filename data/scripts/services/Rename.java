package services;

import java.sql.ResultSet;

import com.lineage.Config;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import com.lineage.ext.multilang.CustomMessage;
import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import l2d.game.cache.Msg;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import com.lineage.util.Log;
import com.lineage.util.Util;

public class Rename extends Functions implements ScriptFile
{
	public static L2Object self;
	public static L2Object npc;

	public static void rename_page()
	{
		L2Player player = (L2Player) self;
		if(player == null)
			return;
		
		String append = "!Rename";
		append += "<br>";
		append += "<font color=\"LEVEL\">" + new CustomMessage("scripts.services.Rename.RenameFor", self).addString(Util.formatAdena(Config.SERVICES_CHANGE_NICK_PRICE)).addItemName(Config.SERVICES_CHANGE_NICK_ITEM) + "</font>";
		append += "<table>";
		append += "<tr><td>" + new CustomMessage("scripts.services.Rename.NewName", self) + ": <edit var=\"new_name\" width=80></td></tr>";
		append += "<tr><td><button value=\"" + new CustomMessage("scripts.services.Rename.RenameButton", self) + "\" action=\"bypass -h scripts_services.Rename:rename $new_name\" width=80 height=15></td></tr>";
		append += "</table>";
		show(append, player);
	}

	public static void changesex_page()
	{
		L2Player player = (L2Player) self;
		if(player == null)
			return;
		
		String append = "Sex changing";
		append += "<br>";
		append += "<font color=\"LEVEL\">" + new CustomMessage("scripts.services.SexChange.SexChangeFor", self).addString(Util.formatAdena(Config.SERVICES_CHANGE_SEX_PRICE)).addItemName(Config.SERVICES_CHANGE_SEX_ITEM) + "</font>";
		append += "<table>";
		append += "<tr><td><button value=\"" + new CustomMessage("scripts.services.SexChange.Button", self) + "\" action=\"bypass -h scripts_services.Rename:changesex\" width=80 height=15></td></tr>";
		append += "</table>";
		show(append, player);
	}

	public static void rename(String[] args)
	{
		L2Player player = (L2Player) self;
		if(player == null)
			return;
		
		if(args.length != 1)
		{
			show(new CustomMessage("scripts.services.Rename.incorrectinput", player), player);
			return;
		}

		String name = args[0];
		if(!Util.isMatchingRegexp(name, Config.CNAME_TEMPLATE) && name.length() <= 16)
		{
			show(new CustomMessage("scripts.services.Rename.incorrectinput", player), player);
			return;
		}

		if(getItemCount(player, Config.SERVICES_CHANGE_NICK_ITEM) < Config.SERVICES_CHANGE_NICK_PRICE)
		{
			if(Config.SERVICES_CHANGE_NICK_ITEM == 57)
				player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			else
				player.sendPacket(Msg.INCORRECT_ITEM_COUNT);
			return;
		}

		ThreadConnection con = null;
		FiltredPreparedStatement offline = null;
		ResultSet rs = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			offline = con.prepareStatement("SELECT char_name FROM characters WHERE char_name = ?");
			offline.setString(1, name);
			rs = offline.executeQuery();
			if(rs.next())
			{
				show(new CustomMessage("scripts.services.Rename.Thisnamealreadyexists", player), player);
				return;
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			show(new CustomMessage("common.Error", player), player);
			return;
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, offline, rs);
		}

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			offline = con.prepareStatement("UPDATE characters SET char_name = ? WHERE obj_Id = ?");
			offline.setString(1, name);
			offline.setInt(2, player.getObjectId());
			offline.executeUpdate();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			show(new CustomMessage("common.Error", player), player);
			return;
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, offline);
		}

		removeItem(player, Config.SERVICES_CHANGE_NICK_ITEM, Config.SERVICES_CHANGE_NICK_PRICE);

		String oldName = player.getName();
		player.reName(name);
		Log.add("Character " + oldName + " renamed to " + name, "renames");
		show(new CustomMessage("scripts.services.Rename.changedname", player).addString(oldName).addString(name), player);
	}

	public static void changesex()
	{
		L2Player player = (L2Player) self;
		if(player == null)
			return;

		if(getItemCount(player, Config.SERVICES_CHANGE_SEX_ITEM) < Config.SERVICES_CHANGE_SEX_PRICE)
		{
			if(Config.SERVICES_CHANGE_SEX_ITEM == 57)
				player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			else
				player.sendPacket(Msg.INCORRECT_ITEM_COUNT);
			return;
		}

		ThreadConnection con = null;
		FiltredPreparedStatement offline = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			offline = con.prepareStatement("UPDATE characters SET sex = ? WHERE obj_Id = ?");
			offline.setInt(1, player.getSex() == 1 ? 0 : 1);
			offline.setInt(2, player.getObjectId());
			offline.executeUpdate();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			show(new CustomMessage("common.Error", player), player);
			return;
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, offline);
		}

		player.setHairColor(0);
		player.setHairStyle(0);
		player.setFace(0);
		removeItem(player, Config.SERVICES_CHANGE_SEX_ITEM, Config.SERVICES_CHANGE_SEX_PRICE);
		player.logout(false, false, false);
		Log.add("Character " + player + " sex changed to " + (player.getSex() == 1 ? "male" : "female"), "renames");
	}

	public void onLoad()
	{
		System.out.println("Loaded Service: Nick change");
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}