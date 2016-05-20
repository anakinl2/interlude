package commands.voiced;

import com.lineage.ext.multilang.CustomMessage;
import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.geodata.GeoEngine;
import com.lineage.game.handler.IVoicedCommandHandler;
import com.lineage.game.handler.VoicedCommandHandler;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.skills.skillclasses.Call;

public class Relocate extends Functions implements IVoicedCommandHandler, ScriptFile
{
	public static L2Object self;
	public static L2NpcInstance npc;
	public static int SUMMON_PRICE = 5;

	private final String[] _commandList = new String[] { "km-all-to-me" };

	public String[] getVoicedCommandList()
	{
		return _commandList;
	}

	public boolean useVoicedCommand(String command, L2Player activeChar, String target)
	{
		if(command.equalsIgnoreCase("km-all-to-me"))
		{
			if(!activeChar.isClanLeader())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.ONLY_THE_CLAN_LEADER_IS_ENABLED));
				return false;
			}

			SystemMessage msg = Call.canSummonHere(activeChar);
			if(msg != null)
			{
				activeChar.sendPacket(msg);
				return false;
			}

			if(activeChar.isAlikeDead())
			{
				activeChar.sendMessage(new CustomMessage("scripts.commands.voiced.Relocate.Dead", activeChar));
				return false;
			}

			L2Player[] clan = activeChar.getClan().getOnlineMembers(activeChar.getObjectId());

			for(L2Player pl : clan)
				if(Call.canBeSummoned(pl) == null)
					// Спрашиваем, согласие на призыв
					pl.summonCharacterRequest(activeChar.getName(), GeoEngine.findPointToStay(activeChar.getX(), activeChar.getY(), activeChar.getZ(), 100, 150), SUMMON_PRICE);

			return true;
		}
		return false;
	}

	public void onLoad()
	{
		VoicedCommandHandler.getInstance().registerVoicedCommandHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}