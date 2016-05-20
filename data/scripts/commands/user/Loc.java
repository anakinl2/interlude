package commands.user;

import l2d.ext.scripts.ScriptFile;
import l2d.game.handler.IUserCommandHandler;
import l2d.game.handler.UserCommandHandler;
import l2d.game.instancemanager.TownManager;
import l2d.game.model.L2Player;
import l2d.game.serverpackets.SystemMessage;

/**
 * Support for /loc command
 */
public class Loc implements IUserCommandHandler, ScriptFile
{
	private static final int[] COMMAND_IDS = { 0 };

	public boolean useUserCommand(int id, L2Player activeChar)
	{
		if(COMMAND_IDS[0] != id)
			return false;

		int nearestTown = TownManager.getInstance().getClosestTownNumber(activeChar);
		int msg;
		switch(nearestTown)
		{
			case 1:
				msg = SystemMessage.CURRENT_LOCATION__S1_S2_S3_NEAR_TALKING_ISLAND_VILLAGE;
				break;
			case 2:
				msg = SystemMessage.CURRENT_LOCATION__S1_S2_S3_NEAR_ELVEN_VILLAGE;
				break;
			case 3:
				msg = SystemMessage.CURRENT_LOCATION__S1_S2_S3_NEAR_DARK_ELVEN_VILLAGE;
				break;
			case 4:
				msg = SystemMessage.CURRENT_LOCATION__S1_S2_S3_NEAR_ORC_VILLAGE;
				break;
			case 5:
				msg = SystemMessage.CURRENT_LOCATION__S1_S2_S3_NEAR_DWARVEN_VILLAGE;
				break;
			case 6:
				msg = SystemMessage.CURRENT_LOCATION__S1_S2_S3_NEAR_GLUDIO_CASTLE_TOWN;
				break;
			case 7:
				msg = SystemMessage.CURRENT_LOCATION__S1_S2_S3_NEAR_GLUDIN_VILLAGE;
				break;
			case 8:
				msg = SystemMessage.CURRENT_LOCATION__S1_S2_S3_NEAR_DION_CASTLE_TOWN;
				break;
			case 9:
				msg = SystemMessage.CURRENT_LOCATION__S1_S2_S3_NEAR_GIRAN_CASTLE_TOWN;
				break;
			case 10:
				msg = SystemMessage.CURRENT_LOCATION__S1_S2_S3_NEAR_THE_TOWN_OF_OREN;
				break;
			case 11:
				msg = SystemMessage.CURRENT_LOCATION__S1_S2_S3_NEAR_ADEN_CASTLE_TOWN;
				break;
			case 12:
				msg = SystemMessage.CURRENT_LOCATION__S1_S2_S3_NEAR_HUNTERS_VILLAGE;
				break;
			case 13:
				msg = SystemMessage.CURRENT_LOCATION__S1_S2_S3_NEAR_HEINE;
				break;
			case 14:
				msg = SystemMessage.CURRENT_LOCATION__S1_S2_S3_NEAR_RUNE_VILLAGE;
				break;
			case 15:
				msg = SystemMessage.CURRENT_LOCATION__S1_S2_S3_NEAR_GODDARD_CASTLE_TOWN;
				break;
			case 16:
				msg = SystemMessage.CURRENT_LOCATION__S1_S2_S3_NEAR_THE_TOWN_OF_SCHUTTGART;
				break;
			case 17:
				msg = SystemMessage.CURRENT_LOCATION__S1_S2_S3_NEAR_KAMAEL_VILLAGE;
				break;
			case 18:
				msg = SystemMessage.CURRENT_LOCATION__S1_S2_S3_NEAR_PRIMEVAL_ISLE;
				break;
			case 19:
				msg = SystemMessage.CURRENT_LOCATION__S1_S2_S3_NEAR_FANTASY_ISLE;
				break;
			case 20:
				msg = SystemMessage.CURRENT_LOCATION__S1_S2_S3_NEAR_SOUTH_OF_WASTELANDS_CAP;
				break;
			default:
				msg = SystemMessage.CURRENT_LOCATION__S1_S2_S3_NEAR_ADEN_CASTLE_TOWN;
		}
		SystemMessage sm = new SystemMessage(msg);
		sm.addNumber(activeChar.getX());
		sm.addNumber(activeChar.getY());
		sm.addNumber(activeChar.getZ());
		activeChar.sendPacket(sm);
		return true;
	}

	public final int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}

	public void onLoad()
	{
		UserCommandHandler.getInstance().registerUserCommandHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}