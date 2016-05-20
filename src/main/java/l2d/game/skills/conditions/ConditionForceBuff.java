package l2d.game.skills.conditions;

import l2d.game.model.L2Character;
import l2d.game.model.L2Effect;
import l2d.game.model.L2Effect.EffectType;
import l2d.game.skills.Env;
import l2d.game.skills.effects.EffectForce;

public class ConditionForceBuff extends Condition
{
	private int _battleForces;
	private int _spellForces;

	public ConditionForceBuff(int[] forces)
	{
		_battleForces = forces[0];
		_spellForces = forces[1];
	}

	public ConditionForceBuff(int battle, int spell)
	{
		_battleForces = battle;
		_spellForces = spell;
	}

	@Override
	public boolean testImpl(Env env)
	{
		L2Character character = env.character;
		if(character == null)
			return false;
		if(character.getAccessLevel() >= 100)
			return true;
		if(_battleForces > 0)
		{
			L2Effect battleForce = character.getEffectList().getEffectByType(EffectType.BattleForce);
			if(battleForce == null || ((EffectForce) battleForce).forces < _battleForces)
				return false;
		}
		if(_spellForces > 0)
		{
			L2Effect spellForce = character.getEffectList().getEffectByType(EffectType.SpellForce);
			if(spellForce == null || ((EffectForce) spellForce).forces < _spellForces)
				return false;
		}
		return true;
	}
}