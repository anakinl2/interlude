package items;

import javolution.util.FastList;

import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.ai.CtrlEvent;
import com.lineage.game.handler.IItemHandler;
import com.lineage.game.handler.ItemHandler;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Playable;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.serverpackets.MagicSkillLaunched;
import com.lineage.game.serverpackets.MagicSkillUse;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.SkillTable;

/**
 * Spirit of The Lake используется для синего летающего дракона Fafurion Kindred, этой итемой нужно его лечить что бы не умер.
 */
public class SpiritLake implements IItemHandler, ScriptFile
{
	private static final int FAFURION = 18482; // Fafurion Kindred
	private static final int SPIRIT_OF_THE_LAKE = 9689; // Spirit of The Lake
	private static final int SPIRIT_OF_THE_LAKE_SKILL = 2368; // Skill to heal
	private static final int[] ITEM_IDS = {SPIRIT_OF_THE_LAKE};

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if(playable == null || !playable.isPlayer())
			return;
		L2Player activeChar = (L2Player) playable;
		if((activeChar.getTarget() == null) || !(activeChar.getTarget() instanceof L2NpcInstance))
			return;

		L2NpcInstance target = (L2NpcInstance) activeChar.getTarget();

		if(target.getNpcId() != FAFURION)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.THAT_IS_THE_INCORRECT_TARGET));
			return;
		}

		if(item.getItemId() == SPIRIT_OF_THE_LAKE)
		{
			L2Skill sl_skill = SkillTable.getInstance().getInfo(SPIRIT_OF_THE_LAKE_SKILL, 1);
			if(sl_skill != null)
			{
				activeChar.getInventory().destroyItemByItemId(SPIRIT_OF_THE_LAKE, 1, false);
				FastList<L2Character> targets = new FastList<L2Character>();
				targets.add(target);
				// display Heal animation in client
				activeChar.broadcastPacket(new MagicSkillUse(activeChar, 1011, 1, 1000, 500)); // Лечим...
				activeChar.broadcastPacket(new MagicSkillLaunched(activeChar.getObjectId(), 1011, 1, targets, false)); // Лечим...
				sl_skill.useSkill(activeChar, targets);
				if(target.hasAI())
					target.getAI().notifyEvent(CtrlEvent.EVT_SEE_SPELL, sl_skill, activeChar);
			}
		}
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}

	public void onLoad()
	{
		ItemHandler.getInstance().registerItemHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}