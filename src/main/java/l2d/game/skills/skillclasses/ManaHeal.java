package l2d.game.skills.skillclasses;

import javolution.util.FastList;
import com.lineage.Config;
import l2d.game.cache.Msg;
import l2d.game.model.L2Character;
import l2d.game.model.L2Skill;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.skills.Stats;
import l2d.game.templates.StatsSet;

public class ManaHeal extends L2Skill
{
	public ManaHeal(StatsSet set)
	{
		super(set);
	}

	@Override
	public boolean checkCondition(L2Character activeChar, L2Character target, boolean forceUse, boolean dontMove, boolean first)
	{
		if(!isHandler() && getTargetType() == SkillTargetType.TARGET_ONE && (target == null || target.getSkillLevel(_id) > 0))
		{
			activeChar.sendPacket(Msg.THAT_IS_THE_INCORRECT_TARGET);
			return false;
		}

		return super.checkCondition(activeChar, target, forceUse, dontMove, first);
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{
		double mp = _power + 0.1 * _power * Math.sqrt(activeChar.getMAtk(null, null) / 333);

		int sps = isSSPossible() ? activeChar.getChargedSpiritShot() : 0;
		if(sps > 0 && Config.MANAHEAL_SPS_BONUS)
			mp *= sps == 2 ? 1.5 : 1.3;

		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = n.getValue();
			if(target.isDead() || target.isHealBlocked())
				continue;

			double maxNewMp = mp * target.calcStat(Stats.MANAHEAL_EFFECTIVNESS, 100, null, null) / 100;

			// Обработка разницы в левелах при речардже. Учитывыется разница уровня скилла и уровня цели.
			// 1013 = id скилла recharge. Для сервиторов не проверено убавление маны, пока оставлено так как есть.
			int diff = target.getLevel() - getMagicLevel();
			if(getId() == 1013 && diff > 5)
				if(diff < 20)
					maxNewMp = maxNewMp / 100 * (100 - diff * 5);
				else
					maxNewMp = 0;

			if(maxNewMp == 0)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.S1_HAS_FAILED).addSkillName(_id, getDisplayLevel()));
				getEffects(activeChar, target, getActivateRate() > 0, false);
				continue;
			}

			double addToMp = Math.min(Math.max(0, target.getMaxMp() - target.getCurrentMp()), maxNewMp);

			if(addToMp > 0)
				target.setCurrentMp(addToMp + target.getCurrentMp());
			if(target.isPlayer())
				if(activeChar != target)
					target.sendPacket(new SystemMessage(SystemMessage.XS2S_MP_HAS_BEEN_RESTORED_BY_S1).addString(activeChar.getName()).addNumber(Math.round(addToMp)));
				else
					activeChar.sendPacket(new SystemMessage(SystemMessage.S1_MPS_HAVE_BEEN_RESTORED).addNumber(Math.round(addToMp)));
			getEffects(activeChar, target, getActivateRate() > 0, false);
		}

		if(isSSPossible())
			activeChar.unChargeShots(isMagic());
	}
}
