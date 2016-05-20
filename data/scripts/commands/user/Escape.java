package commands.user;

import com.lineage.ext.multilang.CustomMessage;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.handler.IUserCommandHandler;
import com.lineage.game.handler.UserCommandHandler;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.tables.SkillTable;

/**
 * Support for /unstuck command
 */
public class Escape implements IUserCommandHandler, ScriptFile
{
	private static final int[] COMMAND_IDS = { 52 };

	public boolean useUserCommand(int id, L2Player activeChar)
	{
		if(id != COMMAND_IDS[0])
			return false;

		if(activeChar.isMovementDisabled() || activeChar.isInOlympiadMode())
			return false;

		if(activeChar.getTeleMode() != 0 || activeChar.getUnstuck() != 0)
		{
			activeChar.sendMessage(new CustomMessage("common.TryLater", activeChar));
			return false;
		}

		if(activeChar.getDuel() != null)
		{
			activeChar.sendMessage(new CustomMessage("common.RecallInDuel", activeChar));
			return false;
		}
		
		if(activeChar.isInJail())
		{
			activeChar.sendMessage(new CustomMessage("l2d.game.skills.skillclasses.Recall.Here", activeChar));
			return false;
		}

		activeChar.abortCast();
		activeChar.abortAttack();
		activeChar.stopMove();

		L2Skill skill;
		if(activeChar.getPlayerAccess().FastUnstuck)
			skill = SkillTable.getInstance().getInfo(1050, 2);
		else
		{
			skill = SkillTable.getInstance().getInfo(2099, 1);
			skill.setHitTime(30000);
			skill.setName("Unstuck - 30s.");
		}

		if(skill != null && skill.checkCondition(activeChar, activeChar, false, false, true))
			activeChar.getAI().Cast(skill, activeChar, false, true);

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
