package commands.admin;

import l2d.ext.scripts.ScriptFile;
import l2d.game.cache.Msg;
import l2d.game.handler.AdminCommandHandler;
import l2d.game.handler.IAdminCommandHandler;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;

public class AdminPolymorph implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_polyself, //
		admin_polymorph, //
		admin_polymorph_radius, //
		admin_poly, //
		admin_poly_radius, //
		admin_unpolyself, //
		admin_unpolymorph, //
		admin_unpolymorph_radius, //
		admin_unpoly, //
		admin_unpoly_radius //
	}

	@SuppressWarnings("unchecked")
	public boolean useAdminCommand(final Enum comm, final String[] wordList, final String fullString, final L2Player activeChar)
	{
		final Commands command = (Commands) comm;

		if( !activeChar.getPlayerAccess().CanPolymorph)
			return false;

		L2Object target = activeChar.getTarget();

		if(target == null || !target.isPlayer())
			target = activeChar;

		switch(command)
		{
			case admin_polyself:
			{
				target = activeChar;
			}
			case admin_polymorph:
			case admin_poly:
			{
				if(target == null || !target.isPlayer())
				{
					activeChar.sendPacket(Msg.INVALID_TARGET);
					return false;
				}
				if(wordList.length != 3)
				{
					activeChar.sendMessage("USAGE: //poly <int id> <String type:[npc|item]>");
					return false;
				}
				try
				{
					final int id = Integer.parseInt(wordList[1]);
					int type = L2Object.POLY_NPC;
					if(wordList.length > 2 && (wordList[2].equalsIgnoreCase("item") || wordList[2].equalsIgnoreCase("i")))
						type = L2Object.POLY_ITEM;
					target.setPolyInfo(type, id);
				}
				catch(final Exception e)
				{
					e.printStackTrace();
					return false;
				}
				break;
			}
			case admin_unpolyself:
			{
				target = activeChar;
			}
			case admin_unpolymorph:
			case admin_unpoly:
			{
				if(target == null || !target.isPlayer())
				{
					activeChar.sendPacket(Msg.INVALID_TARGET);
					return false;
				}
				target.setPolyInfo(0, 0);
				break;
			}
			case admin_polymorph_radius:
			case admin_poly_radius:
			{
				if(wordList.length != 4)
				{
					activeChar.sendMessage("USAGE: //poly <int id> <String type:[npc|item]> <int radius>");
					return false;
				}
				final int id = Integer.parseInt(wordList[1]);
				final int radius = Integer.parseInt(wordList[3]);
				for(final L2Object element : L2World.getAroundPlayers(activeChar, radius, 200))
				{
					try
					{
						int type = L2Object.POLY_NPC;
						if(wordList.length > 2 && (wordList[2].equalsIgnoreCase("item") || wordList[2].equalsIgnoreCase("i")))
							type = L2Object.POLY_ITEM;
						element.setPolyInfo(type, id);
					}
					catch(final Exception e)
					{
						e.printStackTrace();
						return false;
					}
				}
				break;
			}
			case admin_unpolymorph_radius:
			case admin_unpoly_radius:
			{
				if(wordList.length != 2)
				{
					activeChar.sendMessage("USAGE: //unpoly <int radius>");
					return false;
				}
				final int radius = Integer.parseInt(wordList[1]);
				for(final L2Object element : L2World.getAroundPlayers(activeChar, radius, 200))
				{
					element.setPolyInfo(0, 0);
				}
				break;
			}
		}

		return true;
	}

	@SuppressWarnings("unchecked")
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