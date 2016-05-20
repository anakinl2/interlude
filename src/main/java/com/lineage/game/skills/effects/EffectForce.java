package com.lineage.game.skills.effects;

import java.util.logging.Logger;

import com.lineage.game.model.L2Effect;
import com.lineage.game.model.L2Skill;
import com.lineage.game.skills.Env;
import com.lineage.game.tables.SkillTable;
import com.lineage.util.Util;

public class EffectForce extends L2Effect
{
	static final Logger _log = Logger.getLogger(EffectForce.class.getName());

	public int forces = 0;
	private int _range = -1;

	public EffectForce(Env env, EffectTemplate template)
	{
		super(env, template);
		forces = getSkill().getLevel();
		_range = 1000;
	}

	@Override
	public boolean onActionTime()
	{
		if(!Util.checkIfInRange(_range, getEffector(), getEffected(), true))
			getEffector().abortCast();
		return true;
	}

	public void increaseForce()
	{
		if(forces < 3)
		{
			forces++;
			updateBuff();
		}
	}

	public void decreaseForce()
	{
		forces--;
		if(forces < 1)
			exit();
		else
			updateBuff();
	}

	public void updateBuff()
	{
		exit();
		L2Skill newSkill = SkillTable.getInstance().getInfo(getSkill().getId(), forces);
		newSkill.getEffects(getEffector(), getEffected(), false, false);
	}

	public int getForceCount()
	{
		return forces;
	}
}