package l2d.game.skills.skillclasses;

import javolution.util.FastList;
import l2d.game.model.L2Character;
import l2d.game.model.L2Clan;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.L2Zone;
import l2d.game.skills.Formulas;
import l2d.game.templates.StatsSet;

public class PDam extends L2Skill
{
	private int _numSouls;

	public PDam(final StatsSet set)
	{
		super(set);
		_numSouls = set.getInteger("numSouls", 0);
	}

	@Override
	public void useSkill(final L2Character activeChar, final FastList<L2Character> targets)
	{
		final boolean ss = activeChar.getChargedSoulShot() && isSSPossible();

		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = n.getValue();
			if(activeChar.isInZone(L2Zone.ZoneType.Siege) && activeChar.getPlayer() != null)
			{
				L2Clan clan = activeChar.getPlayer().getClan();
				if(clan == null && !activeChar.getTarget().isPlayer())
					return;
				if(!clan.getSiege().isInProgress() && !activeChar.getTarget().isPlayer())
					return;
			}
			if(target.isDead())
				continue;

			if(target.checkReflectSkill(activeChar, this))
				target = activeChar;
			double damage = 0;
			if(getPower() > 0) // Если == 0 значит скилл "отключен"
			{
				if(activeChar.isPlayer() && target.isPlayer())
				{
					L2Player p = target.getPlayer();
					if(p.isInOlympiadMode() || p.isInDuel() || p.isInZoneBattle() || p.isInZone(L2Zone.ZoneType.Siege) || p.getPvpFlag() > 0)
						damage = Formulas.calcPhysDam(activeChar, target, this, false, false, ss).damage;
				}
				if(damage == 0)
					damage = Formulas.calcPhysDam(activeChar, target, this, false, false, ss).damage;
				target.reduceCurrentHp(damage, activeChar, this, true, true, false, true);
			}
			getEffects(activeChar, target, getActivateRate() > 0, false);

			if(isSuicideAttack())
				activeChar.doDie(null);
		}

		if(_numSouls > 0 && activeChar.isPlayer())
			activeChar.getPlayer().setConsumedSouls(activeChar.getPlayer().getConsumedSouls() + _numSouls, null);
		if(isSSPossible())
			activeChar.unChargeShots(isMagic());
	}
}