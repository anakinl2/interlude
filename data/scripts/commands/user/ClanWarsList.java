package commands.user;

import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.handler.IUserCommandHandler;
import com.lineage.game.handler.UserCommandHandler;
import com.lineage.game.model.L2Clan;
import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.SystemMessage;

import java.util.List;

import javolution.util.FastList;

/**
 * Support for /attacklist /underattacklist /warlist commands
 */
public class ClanWarsList implements IUserCommandHandler, ScriptFile
{
	private static final int[] COMMAND_IDS = { 88, 89, 90 };

	public boolean useUserCommand(int id, L2Player activeChar)
	{
		if(id != COMMAND_IDS[0] && id != COMMAND_IDS[1] && id != COMMAND_IDS[2])
			return false;

		L2Clan clan = activeChar.getClan();
		if(clan == null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.NOT_JOINED_IN_ANY_CLAN));
			return false;
		}

		SystemMessage sm;
		List<L2Clan> data = new FastList<L2Clan>();
		if(id == 88)
		{
			// attack list
			activeChar.sendPacket(new SystemMessage(SystemMessage._ATTACK_LIST_));
			data = clan.getEnemyClans();
		}
		else if(id == 89)
		{
			// under attack list
			activeChar.sendPacket(new SystemMessage(SystemMessage._UNDER_ATTACK_LIST_));
			data = clan.getAttackerClans();
		}
		else
		// id = 90
		{
			// war list
			activeChar.sendPacket(new SystemMessage(SystemMessage._WAR_LIST_));
			for(L2Clan c : clan.getEnemyClans())
				if(clan.getAttackerClans().contains(c))
					data.add(c);
		}

		for(L2Clan c : data)
		{
			String clanName = c.getName();
			int ally_id = c.getAllyId();
			if(ally_id > 0)
			{
				// target with ally
				sm = new SystemMessage(SystemMessage.S1_S2_ALLIANCE);
				sm.addString(clanName);
				sm.addString(c.getAlliance().getAllyName());
			}
			else
			{
				// target without ally
				sm = new SystemMessage(SystemMessage.S1_NO_ALLIANCE_EXISTS);
				sm.addString(clanName);
			}
			activeChar.sendPacket(sm);
		}
		// =========================
		sm = new SystemMessage(SystemMessage.__EQUALS__);
		activeChar.sendPacket(sm);
		return true;
	}

	public int[] getUserCommandList()
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