package l2d.game.skills.conditions;

import l2d.game.model.L2Player;
import l2d.game.skills.Env;

public class ConditionPlayerState extends Condition
{
	public enum CheckPlayerState
	{
		RESTING,
		MOVING,
		RUNNING,
		STANDING,
		FLYING,
		COMBAT,
		COMBAT_PVP
	}

	private final CheckPlayerState _check;

	private final boolean _required;

	public ConditionPlayerState(CheckPlayerState check, boolean required)
	{
		_check = check;
		_required = required;
	}

	@Override
	public boolean testImpl(Env env)
	{
		switch(_check)
		{
			case COMBAT:
				if(env.character.isPlayer())
					return ((L2Player) env.character).isInCombat() == _required;
				return !_required;
			case COMBAT_PVP:
				if(env.character.isPlayer())
					return (((L2Player) env.character).getPvpFlag() != 0) == _required;
				return !_required;
			case RESTING:
				if(env.character.isPlayer())
					return ((L2Player) env.character).isSitting() == _required;
				return !_required;
			case MOVING:
				return (env.character.isMoving && !env.character.isRunning()) == _required;
			case RUNNING:
				return (env.character.isMoving && env.character.isRunning()) == _required;
			case STANDING:
				if(env.character.isPlayer())
					return ((L2Player) env.character).isSitting() != _required && env.character.isMoving != _required;
				return env.character.isMoving != _required;
			case FLYING:
				if(env.character.isPlayer())
					return env.character.isFlying() == _required;
				return !_required;
		}
		return !_required;
	}
}
