package l2d.game.skills.skillclasses;

import javolution.util.FastList;
import l2d.Config;
import l2d.ext.multilang.CustomMessage;
import l2d.game.cache.Msg;
import l2d.game.model.L2Character;
import l2d.game.model.L2Manor;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.model.instances.L2MonsterInstance;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.ItemTable;
import l2d.game.templates.StatsSet;
import l2d.util.Rnd;

public class Sowing extends L2Skill
{
	@Override
	public boolean checkCondition(L2Character activeChar, L2Character target, boolean forceUse, boolean dontMove, boolean first)
	{
		// Проверки в хэндлере
		return super.checkCondition(activeChar, target, forceUse, dontMove, first);
	}

	public Sowing(StatsSet set)
	{
		super(set);
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{
		if(!activeChar.isPlayer())
			return;

		L2Player player = (L2Player) activeChar;
		int seed_id = player.getUseSeed();
		// remove seed from inventory
		L2ItemInstance seedItem = player.getInventory().getItemByItemId(seed_id);
		if(seedItem != null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.S1_HAS_DISAPPEARED).addItemName(seed_id));
			player.getInventory().destroyItem(seedItem, 1, true);
		}
		else
		{
			activeChar.sendPacket(Msg.SYSTEM_ERROR);
			return;
		}

		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = n.getValue();
			L2MonsterInstance monster = (L2MonsterInstance) target;
			if(monster.isSeeded())
				continue;

			// обработка
			double SuccessRate = Config.MANOR_SOWING_BASIC_SUCCESS;

			double diffPlayerTarget = Math.abs(activeChar.getLevel() - target.getLevel());
			double diffSeedTarget = Math.abs(L2Manor.getInstance().getSeedLevel(seed_id) - target.getLevel());

			// Штраф, на разницу уровней между мобом и игроком
			// 5% на каждый уровень при разнице >5 - по умолчанию
			if(diffPlayerTarget > Config.MANOR_DIFF_PLAYER_TARGET)
				SuccessRate -= (diffPlayerTarget - Config.MANOR_DIFF_PLAYER_TARGET) * Config.MANOR_DIFF_PLAYER_TARGET_PENALTY;

			// Штраф, на разницу уровней между семечкой и мобом
			// 5% на каждый уровень при разнице >5 - по умолчанию
			if(diffSeedTarget > Config.MANOR_DIFF_SEED_TARGET)
				SuccessRate -= (diffSeedTarget - Config.MANOR_DIFF_SEED_TARGET) * Config.MANOR_DIFF_SEED_TARGET_PENALTY;

			if(ItemTable.getInstance().getTemplate(seed_id).isAltSeed())
				SuccessRate *= Config.MANOR_SOWING_ALT_BASIC_SUCCESS / Config.MANOR_SOWING_BASIC_SUCCESS;

			// Минимальный шанс успеха всегда 1%
			if(SuccessRate < 1)
				SuccessRate = 1;

			if(Config.SKILLS_SHOW_CHANCE && !player.getVarB("SkillsHideChance"))
				activeChar.sendMessage(new CustomMessage("l2d.game.skills.skillclasses.Sowing.Chance", activeChar).addNumber((int) SuccessRate));

			if(Rnd.chance(SuccessRate))
			{
				monster.setSeeded(seedItem.getItem(), player);
				activeChar.sendPacket(Msg.THE_SEED_WAS_SUCCESSFULLY_SOWN);
			}
			else
				activeChar.sendPacket(Msg.THE_SEED_WAS_NOT_SOWN);
		}
	}
}