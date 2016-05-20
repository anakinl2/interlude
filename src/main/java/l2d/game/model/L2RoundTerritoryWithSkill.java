package l2d.game.model;

import java.util.logging.Logger;

public class L2RoundTerritoryWithSkill extends L2RoundTerritory
{
	private static final Logger _log = Logger.getLogger(L2RoundTerritoryWithSkill.class.getName());

	private final L2Character _effector;
	private final L2Skill _skill;

	public L2RoundTerritoryWithSkill(int id, int centerX, int centerY, int radius, int zMin, int zMax, L2Character effector, L2Skill skill)
	{
		super(id, centerX, centerY, radius, zMin, zMax);
		_effector = effector;
		_skill = skill;
		if(_skill == null)
			_log.severe("L2RoundTerritoryWithSkill with null skill: " + getId());
	}

	@Override
	public void doEnter(L2Object obj)
	{
		if(_effector == null || obj == null || _skill == null)
			return;

		if(!isInside(obj.getX(), obj.getY(), obj.getZ()))
			return;

		if(obj.isCharacter())
		{
			L2Character effected = (L2Character) obj;
			if(_skill.checkTarget(_effector, effected, null, false, false) == null)
				_skill.getEffects(_effector, effected, false, false);
		}
	}

	@Override
	public void doLeave(L2Object obj, boolean notify)
	{
		if(_effector == null || obj == null)
			return;

		if(obj.isCharacter())
			((L2Character) obj).getEffectList().stopEffect(_skill.getId());
	}
}