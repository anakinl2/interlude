package l2d.game.skills.skillclasses;

import javolution.util.FastList;
import l2d.game.geodata.GeoEngine;
import l2d.game.model.L2Character;
import l2d.game.model.L2Skill;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.skills.Formulas;
import l2d.game.skills.Formulas.AttackInfo;
import l2d.game.templates.StatsSet;
import com.lineage.util.Location;

public class FatalBlow extends L2Skill
{
	private final boolean _onCrit;
	private final boolean _directHp;
	private final boolean _turner;

	public FatalBlow(final StatsSet set)
	{
		super(set);
		_onCrit = set.getBool("onCrit", false);
		_directHp = set.getBool("directHp", false);
		_turner = set.getBool("turner", false);
	}

	@Override
	public void useSkill(final L2Character activeChar, final FastList<L2Character> targets)
	{
		final boolean ss = activeChar.getChargedSoulShot() && isSSPossible();
		if(ss)
			activeChar.unChargeShots(false);

		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = n.getValue();
			if(target.isDead())
				continue;

			if(_turner && !target.isInvul())
			{
				final int posX = target.getX();
				final int posY = target.getY();
				int signx = -1;
				int signy = -1;
				if(target.getX() > target.getX())
					signx = 1;
				if(target.getY() > target.getY())
					signy = 1;

				target.stopMove();
				target.setHeading(target, false);

				if(!target.isMonster())
					target.setTarget(null);
				target.setRunning();

				final Location loc = GeoEngine.moveCheck(target.getX(), target.getY(), target.getZ(), posX + signx * 40, posY + signy * 40);
				target.moveToLocation(loc, 0, false);

				target.sendPacket(new SystemMessage(SystemMessage.S1_S2S_EFFECT_CAN_BE_FELT).addSkillName(_displayId, _displayLevel));
			}

			if(_onCrit && !Formulas.calcBlow(activeChar, target, this))
			{
				getEffects(activeChar, target, getActivateRate() > 0, false);
				activeChar.sendPacket(new SystemMessage(SystemMessage.C1S_ATTACK_WENT_ASTRAY).addName(activeChar));
				continue;
			}

			if(target.checkReflectSkill(activeChar, this))
				target = activeChar;

			if(getPower() > 0) // Если == 0 значит скилл "отключен"
			{
				final AttackInfo info = Formulas.calcPhysDam(activeChar, target, this, false, true, ss);
				target.reduceCurrentHp(info.damage, activeChar, this, true, true, info.lethal ? false : _directHp, true);
			}

			getEffects(activeChar, target, getActivateRate() > 0, false);
		}
	}
}