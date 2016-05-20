package items;

import java.util.ArrayList;

import l2d.ext.scripts.ScriptFile;
import l2d.game.cache.Msg;
import l2d.game.handler.IItemHandler;
import l2d.game.handler.ItemHandler;
import l2d.game.model.L2Playable;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.L2SkillLearn;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.serverpackets.ExStorageMaxCount;
import l2d.game.serverpackets.SkillList;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.SkillSpellbookTable;
import l2d.game.tables.SkillTable;
import l2d.game.tables.SkillTreeTable;

public class Spellbooks implements IItemHandler, ScriptFile
{
	private static int[] _itemIds = null;

	public Spellbooks()
	{
		_itemIds = new int[SkillSpellbookTable.getSpellbookHandlers().size()];
		int i = 0;
		for(int id : SkillSpellbookTable.getSpellbookHandlers().keySet())
		{
			_itemIds[i] = id;
			i++;
		}
	}

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if(playable == null || !playable.isPlayer())
			return;
		L2Player player = (L2Player) playable;

		if(item == null || item.getIntegerLimitedCount() < 1)
		{
			player.sendPacket(Msg.INCORRECT_ITEM_COUNT);
			return;
		}

		ArrayList<Integer> skill_ids = SkillSpellbookTable.getSpellbookHandlers().get(item.getItemId());

		for(int skill_id : skill_ids)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(skill_id, 1);

			player.setSkillLearningClassId(player.getClassId());

			if(player.getSkillLevel(skill_id) > 0)
				continue;

			if(!(skill.isCommon() || SkillTreeTable.getInstance().isSkillPossible(player, skill_id, 1)))
				continue;

			L2SkillLearn SkillLearn = SkillTreeTable.getSkillLearn(skill_id, 1, player.getClassId(), null);

			if(player.getLevel() < SkillLearn.minLevel)
				return;

			int _requiredSp = SkillTreeTable.getInstance().getSkillCost(player, skill);

			if(player.getSp() >= _requiredSp || SkillLearn.common || SkillLearn.transformation)
			{
				L2ItemInstance ri = player.getInventory().destroyItem(item, 1, true);
				player.sendPacket(new SystemMessage(SystemMessage.S1_HAS_DISAPPEARED).addItemName(ri.getItemId()));
				player.addSkill(skill, true);
				if(!SkillLearn.common && !SkillLearn.transformation)
					player.setSp(player.getSp() - _requiredSp);
				player.updateStats();
				player.sendChanges();
				if(SkillLearn.common)
					player.sendPacket(new ExStorageMaxCount(player));
				player.sendPacket(new SkillList(player));
			}
			else
			{
				player.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_ENOUGH_SP_TO_LEARN_SKILLS));
				return;
			}
		}
	}

	public int[] getItemIds()
	{
		return _itemIds;
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