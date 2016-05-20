package commands.admin;

import com.lineage.ext.multilang.CustomMessage;
import com.lineage.ext.scripts.ScriptFile;
import l2d.game.geodata.GeoEngine;
import l2d.game.handler.AdminCommandHandler;
import l2d.game.handler.IAdminCommandHandler;
import l2d.game.model.L2Player;

public class AdminGeodata implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_geo_z,
		admin_geo_type,
		admin_geo_nswe,
		admin_geo_los,
		admin_geo_load,
		admin_geo_info,
		admin_geo_dump
	}

	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, L2Player activeChar)
	{
		Commands command = (Commands) comm;

		if(!activeChar.getPlayerAccess().CanReload)
			return false;

		switch(command)
		{
			case admin_geo_z:
				activeChar.sendMessage("GeoEngine: Geo_Z = " + GeoEngine.getHeight(activeChar.getLoc()) + " Loc_Z = " + activeChar.getZ());
				break;
			case admin_geo_type:
				int type = GeoEngine.getType(activeChar.getX(), activeChar.getY());
				activeChar.sendMessage("GeoEngine: Geo_Type = " + type);
				break;
			case admin_geo_nswe:
				String result = "";
				byte nswe = GeoEngine.getNSWE(activeChar.getX(), activeChar.getY(), activeChar.getZ());
				if((nswe & 8) == 0)
					result += " N";
				if((nswe & 4) == 0)
					result += " S";
				if((nswe & 2) == 0)
					result += " W";
				if((nswe & 1) == 0)
					result += " E";
				activeChar.sendMessage("GeoEngine: Geo_NSWE -> " + nswe + "->" + result);
				break;
			case admin_geo_los:
				if(activeChar.getTarget() != null)
					if(GeoEngine.canSeeTarget(activeChar, activeChar.getTarget(),false))
						activeChar.sendMessage("GeoEngine: Can See Target");
					else
						activeChar.sendMessage("GeoEngine: Can't See Target");
				else
					activeChar.sendMessage("None Target!");
				break;
			case admin_geo_load:
				if(wordList.length != 3)
					activeChar.sendMessage("Usage: //geo_load <regionX> <regionY>");
				else
					try
					{
						byte rx = Byte.parseByte(wordList[1]);
						byte ry = Byte.parseByte(wordList[2]);
						if(GeoEngine.LoadGeodataFile(rx, ry))
							activeChar.sendMessage("GeoEngine: Регион [" + rx + "," + ry + "] успешно загружен");
						else
							activeChar.sendMessage("GeoEngine: Регион [" + rx + "," + ry + "] не загрузился");
					}
					catch(Exception e)
					{
						activeChar.sendMessage(new CustomMessage("common.Error", activeChar));
					}
				break;
			case admin_geo_info:
				/**
				GeoEngine.getInfo(activeChar, activeChar.getX(), activeChar.getY(), activeChar.getZ());
				 */
				activeChar.sendMessage("Временно не доступно.");
				break;
			case admin_geo_dump:
				if(wordList.length > 2)
				{
					GeoEngine.DumpGeodataFileMap(Byte.parseByte(wordList[1]), Byte.parseByte(wordList[2]));
					activeChar.sendMessage("Квадрат геодаты сохранен " + wordList[1] + "_" + wordList[2]);
				}
				GeoEngine.DumpGeodataFile(activeChar.getX(), activeChar.getY());
				activeChar.sendMessage("Текущий квадрат геодаты сохранен");
				break;
		}

		return true;
	}

	public Enum[] getAdminCommandEnum()
	{
		return Commands.values();
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