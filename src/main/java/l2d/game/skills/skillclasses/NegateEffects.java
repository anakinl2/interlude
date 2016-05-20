package l2d.game.skills.skillclasses;

import javolution.util.FastList;
import l2d.game.model.L2Character;
import l2d.game.model.L2Effect;
import l2d.game.model.L2Effect.EffectType;
import l2d.game.model.L2Skill;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.skills.Formulas;
import l2d.game.templates.StatsSet;

public class NegateEffects extends L2Skill
{
	private final EffectType[] _negateEffects;
	private final String[] _negateStackType;
	private final int _negatePower;
	private final boolean _onlyPhisical;
	private final boolean _negateDebuffs;

	public NegateEffects(StatsSet set)
	{
		super(set);

		String[] negateEffectsString = set.getString("negateEffects", "").split(" ");
		_negateEffects = new EffectType[negateEffectsString.length];
		for(int i = 0; i < negateEffectsString.length; i++)
			if(!negateEffectsString[i].isEmpty())
				_negateEffects[i] = Enum.valueOf(EffectType.class, negateEffectsString[i]);

		_negateStackType = set.getString("negateStackType", "").split(" ");

		_negatePower = set.getInteger("negatePower", Integer.MAX_VALUE);
		_onlyPhisical = set.getBool("onlyPhisical", false);
		_negateDebuffs = set.getBool("negateDebuffs", true);
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{
		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = n.getValue();

			if(!_negateDebuffs && !Formulas.calcSkillSuccess(activeChar, target, this))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.C1_HAS_RESISTED_YOUR_S2).addString(target.getName()).addSkillName(getId(), getLevel()));
				continue;
			}

			for(EffectType stat : _negateEffects)
				negateEffectAtPower(target, stat);

			for(String stat : _negateStackType)
				negateEffectAtPower(target, stat);

			getEffects(activeChar, target, getActivateRate() > 0, false);
		}

		if(isSSPossible())
			activeChar.unChargeShots(isMagic());
	}

	private void negateEffectAtPower(L2Character target, EffectType type)
	{
		for(L2Effect e : target.getEffectList().getAllEffects())
		{
			L2Skill skill = e.getSkill();
			if(_onlyPhisical && skill.isMagic() || !skill.isCancelable() || skill.isOffensive() && !_negateDebuffs)
				continue;
			if(e.getEffectType() == type && e.getStackOrder() <= _negatePower)
				e.exit();
		}
	}

	private void negateEffectAtPower(L2Character target, String stackType)
	{
		for(L2Effect e : target.getEffectList().getAllEffects())
		{
			L2Skill skill = e.getSkill();
			if(_onlyPhisical && skill.isMagic() || !skill.isCancelable() || skill.isOffensive() && !_negateDebuffs)
				continue;
			if(e.getStackType().equalsIgnoreCase(stackType) && e.getStackOrder() <= _negatePower)
				e.exit();
		}
	}
}