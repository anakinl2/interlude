package com.lineage.game.model;

import java.util.concurrent.Future;
import java.util.logging.Logger;

import com.lineage.game.ThreadPoolManager;
import com.lineage.game.skills.effects.EffectForce;
import com.lineage.game.tables.SkillTable;

public class ForceBuff
{
	static final Logger _log = Logger.getLogger(ForceBuff.class.getName());

	private L2Character _caster;
	private L2Character _target;
	private L2Skill _skill;
	private L2Skill _force;
	private Future<?> _task;

	public L2Character getCaster()
	{
		return _caster;
	}

	public L2Character getTarget()
	{
		return _target;
	}

	public L2Skill getSkill()
	{
		return _skill;
	}

	public L2Skill getForce()
	{
		return _force;
	}

	public ForceBuff(final L2Character caster, final L2Character target, final L2Skill skill)
	{
		_caster = caster;
		_target = target;
		_skill = skill;
		_force = SkillTable.getInstance().getInfo(skill.getForceId(), 1);

		for(final L2Effect e : getTarget().getEffectList().getAllEffects())
			if(e.getSkill().getId() == getForce().getId())
			{
				final EffectForce ef = (EffectForce) e;
				if(ef.forces >= 3)
					return;
			}

		final Runnable r = new Runnable(){
			@Override
			public void run()
			{
				final int forceId = getForce().getId();
				boolean create = true;
				for(final L2Effect e : getTarget().getEffectList().getAllEffects())
					if(e.getSkill().getId() == forceId)
					{
						((EffectForce) e).increaseForce();
						create = false;
						break;
					}

				if(create)
					getForce().getEffects(_caster, getTarget(), false, false);
			}
		};

		_task = ThreadPoolManager.getInstance().scheduleEffect(r, 3300);
	}

	public void delete()
	{
		if(_task != null)
		{
			_task.cancel(false);
			_task = null;
		}

		final int toDeleteId = getForce().getId();

		for(final L2Effect e : _target.getEffectList().getAllEffects())
			if(e.getSkill().getId() == toDeleteId)
			{
				((EffectForce) e).decreaseForce();
				break;
			}

		_caster.setForceBuff(null);
		_caster.abortCast();
	}
}