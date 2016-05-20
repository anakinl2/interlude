package com.lineage.game.model.instances;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import com.lineage.game.ThreadPoolManager;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Effect;
import com.lineage.game.model.L2Playable;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.L2Skill.SkillType;
import com.lineage.game.serverpackets.MagicSkillUse;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.SkillTable;
import com.lineage.util.Rnd;

public class L2CubicInstance
{
	protected static final Logger _log = Logger.getLogger(L2CubicInstance.class.getName());

	public static enum CubicType
	{
		STORM_CUBIC(1, 14, 80),
		VAMPIRIC_CUBIC(2, 14, 67),
		LIFE_CUBIC(3, -1, 55),
		VIPER_CUBIC(4, 14, 55),
		PHANTOM_CUBIC(5, 14, 55),
		BINDING_CUBIC(6, 14, 55),
		AQUA_CUBIC(7, 14, 67),
		SPARK_CUBIC(8, 14, 55),
		ATTRACTIVE_CUBIC(9, 11, 85),
		SMART_CUBIC_EVATEMPLAR(10, 5, 30),
		SMART_CUBIC_SHILLIENTEMPLAR(11, 5, 30),
		SMART_CUBIC_ARCANALORD(12, 5, 30),
		SMART_CUBIC_ELEMENTALMASTER(13, 5, 30),
		SMART_CUBIC_SPECTRALMASTER(14, 5, 30);

		public final int id;
		public final int delay;
		public final int chance;

		private CubicType(int id, int delay, int chance)
		{
			this.id = id;
			this.delay = delay;
			this.chance = chance;
		}

		public static CubicType getType(int id)
		{
			for(CubicType type : values())
				if(type.id == id)
					return type;
			return null;
		}
	}

	/** Оффсет для корректного сохранения кубиков в базе */
	public static final int CUBIC_STORE_OFFSET = 1000000;

	private WeakReference<L2Player> _owner;
	private WeakReference<L2Character> _target;

	private CubicType _type;
	private int _level = 1;

	private ArrayList<L2Skill> _offensiveSkills = new ArrayList<L2Skill>();
	private ArrayList<L2Skill> _healSkills = new ArrayList<L2Skill>();

	private Future _disappearTask;
	private Future _actionTask;
	private Future _healTask;
	private Future _cureTask;
	private long _starttime;
	private long _lifetime;
	private boolean _givenByOther;

	public L2CubicInstance(final L2Player owner, final int id, final int level, final int lifetime, final boolean givenByOther)
	{
		_type = CubicType.getType(id);
		_level = level;
		_givenByOther = givenByOther;
		_owner = new WeakReference<L2Player>(owner);

		switch(_type)
		{
			case STORM_CUBIC:
				_offensiveSkills.add(SkillTable.getInstance().getInfo(4049, level));
				break;
			case VAMPIRIC_CUBIC:
				_offensiveSkills.add(SkillTable.getInstance().getInfo(4050, level));
				break;
			case LIFE_CUBIC:
				_healSkills.add(SkillTable.getInstance().getInfo(4051, level));
				break;
			case VIPER_CUBIC:
				_offensiveSkills.add(SkillTable.getInstance().getInfo(4052, level));
				break;
			case PHANTOM_CUBIC:
				_offensiveSkills.add(SkillTable.getInstance().getInfo(4053, level));
				_offensiveSkills.add(SkillTable.getInstance().getInfo(4054, level));
				_offensiveSkills.add(SkillTable.getInstance().getInfo(4055, level));
				break;
			case BINDING_CUBIC:
				_offensiveSkills.add(SkillTable.getInstance().getInfo(4164, level));
				break;
			case AQUA_CUBIC:
				_offensiveSkills.add(SkillTable.getInstance().getInfo(4165, level));
				break;
			case SPARK_CUBIC:
				_offensiveSkills.add(SkillTable.getInstance().getInfo(4166, level));
				break;
			case ATTRACTIVE_CUBIC:
				_offensiveSkills.add(SkillTable.getInstance().getInfo(5115, level));
				_offensiveSkills.add(SkillTable.getInstance().getInfo(5116, level));
				break;
			case SMART_CUBIC_ARCANALORD:
				_healSkills.add(SkillTable.getInstance().getInfo(4051, 7)); // Cubic Heal
				_offensiveSkills.add(SkillTable.getInstance().getInfo(4165, 9)); // Icy Air
				new Cure().run();
				break;
			case SMART_CUBIC_ELEMENTALMASTER:
				_offensiveSkills.add(SkillTable.getInstance().getInfo(4049, 8)); // Cubic Storm Strike
				_offensiveSkills.add(SkillTable.getInstance().getInfo(4166, 9)); // Shock
				new Cure().run();
				break;
			case SMART_CUBIC_SPECTRALMASTER:
				_offensiveSkills.add(SkillTable.getInstance().getInfo(4049, 8)); // Cubic Storm Strike
				_offensiveSkills.add(SkillTable.getInstance().getInfo(4052, 6)); // Poison
				new Cure().run();
				break;
			case SMART_CUBIC_EVATEMPLAR:
				_offensiveSkills.add(SkillTable.getInstance().getInfo(4053, 8)); // Decrease P.Atk
				_offensiveSkills.add(SkillTable.getInstance().getInfo(4165, 9)); // Icy Air
				new Cure().run();
				break;
			case SMART_CUBIC_SHILLIENTEMPLAR:
				_offensiveSkills.add(SkillTable.getInstance().getInfo(4049, 8)); // Cubic Storm Strike
				_offensiveSkills.add(SkillTable.getInstance().getInfo(5115, 4)); // Cubic Hate
				new Cure().run();
				break;
		}

		for(L2Skill skill : _offensiveSkills)
			if(skill.getCastRange() != 900)
				skill.setCastRange(900); // Костыль, но так проще
		for(L2Skill skill : _healSkills)
			if(skill.getCastRange() != 900)
				skill.setCastRange(900); // Костыль, но так проще

		if(!_healSkills.isEmpty())
			new Heal().run();

		if(_disappearTask == null)
			_disappearTask = ThreadPoolManager.getInstance().scheduleEffect(new Disappear(), lifetime); // disappear in 20 mins
		_starttime = System.currentTimeMillis();
		_lifetime = lifetime;
	}

	public void doAction(L2Character target)
	{
		WeakReference<L2Player> owner_ref = _owner;
		if(owner_ref == null)
			return;
		L2Player owner = owner_ref.get();
		if(owner == null)
			return;
		WeakReference<L2Character> old_target_ref = _target;
		L2Character old_target = null;
		if(old_target_ref != null)
			old_target = old_target_ref.get();
		if(old_target != null && (old_target == target || owner == target))
			return;
		stopAction();
		_target = new WeakReference<L2Character>(target);
		_actionTask = ThreadPoolManager.getInstance().scheduleEffect(new Action(), 2000);
	}

	public CubicType getType()
	{
		return _type;
	}

	public int getId()
	{
		return _type.id;
	}

	public int getLevel()
	{
		return _level;
	}

	public boolean isGivenByOther()
	{
		return _givenByOther;
	}

	public long lifeLeft()
	{
		return _lifetime - (System.currentTimeMillis() - _starttime);
	}

	public void stopAction()
	{
		_target = null;
		if(_actionTask != null)
		{
			_actionTask.cancel(false);
			_actionTask = null;
		}
		if(_healTask != null)
		{
			_healTask.cancel(false);
			_healTask = null;
		}
	}

	public void cancelDisappear()
	{
		if(_disappearTask != null)
		{
			_disappearTask.cancel(false);
			_disappearTask = null;
		}
	}

	public void deleteMe()
	{
		if(_cureTask != null)
		{
			_cureTask.cancel(false);
			_cureTask = null;
		}
		stopAction();
		cancelDisappear();
		WeakReference<L2Player> owner_ref = _owner;
		L2Player owner = null;
		if(owner_ref != null)
			owner = owner_ref.get();
		if(owner != null)
		{
			owner.delCubic(this);
			owner.broadcastUserInfo(true);
		}
		_owner = null;
		_offensiveSkills = null;
		_healSkills = null;
	}

	private class Action implements Runnable
	{
		@Override
		public void run()
		{
			WeakReference<L2Player> owner_ref = _owner;
			if(owner_ref == null)
			{
				deleteMe();
				return;
			}
			L2Player owner = owner_ref.get();
			if(owner == null || owner.isDead())
			{
				deleteMe();
				return;
			}
			WeakReference<L2Character> target_ref = _target;
			L2Character target = null;
			if(target_ref != null)
				target = target_ref.get();
			if(target == null || target.isDead() || !owner.isInRangeZ(target, 900))
			{
				stopAction();
				return;
			}
			try
			{
				L2Skill skill = _offensiveSkills.get(Rnd.get(_offensiveSkills.size()));
				if(Rnd.chance(_type.chance) && skill.checkCondition(owner, target, false, false, true))
					owner.altUseSkill(skill, target);
			}
			catch(final Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				_actionTask = ThreadPoolManager.getInstance().scheduleEffect(this, _type.delay * owner.calculateAttackDelay());
			}
		}
	}

	private class Heal implements Runnable
	{
		@Override
		public void run()
		{
			WeakReference<L2Player> owner_ref = _owner;
			if(owner_ref == null)
			{
				deleteMe();
				return;
			}
			L2Player owner = owner_ref.get();
			if(owner == null || owner.isDead())
			{
				deleteMe();
				return;
			}
			try
			{
				for(L2Skill skill : _healSkills)
					if(skill.getSkillType() == SkillType.HEAL)
					{
						L2Character target = owner;
						if(owner.getParty() != null)
						{
							for(L2Playable member : owner.getParty().getPartyMembersWithPets())
								if(member != null && !member.isDead() && member.getCurrentHpRatio() < target.getCurrentHpRatio() && owner.isInRangeZ(member, skill.getCastRange()))
									target = member;
						}
						else if(owner.getPet() != null && !owner.getPet().isDead() && owner.getPet().getCurrentHpRatio() < owner.getCurrentHpRatio())
							target = owner.getPet();
						double hpp = target.getCurrentHpPercents();
						if(hpp < 95 && Rnd.chance(hpp > 60 ? 44 : hpp > 30 ? 66 : 100))
							owner.altUseSkill(skill, target);
					}
			}
			catch(final Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				_healTask = ThreadPoolManager.getInstance().scheduleEffect(this, Rnd.get(8, 12) * owner.calculateAttackDelay());
			}
		}
	}

	private class Cure implements Runnable
	{
		@Override
		public void run()
		{
			WeakReference<L2Player> owner_ref = _owner;
			if(owner_ref == null)
			{
				deleteMe();
				return;
			}
			L2Player owner = owner_ref.get();
			if(owner == null || owner.isDead())
			{
				deleteMe();
				return;
			}
			try
			{
				boolean use = false;
				for(L2Effect e : owner.getEffectList().getAllEffects())
					if(e.getSkill().isOffensive() && e.getSkill().isCancelable() && !e._template._applyOnCaster)
					{
						owner.sendPacket(new SystemMessage(SystemMessage.THE_EFFECT_OF_S1_HAS_BEEN_REMOVED).addSkillName(e.getSkill().getDisplayId(), e.getSkill().getDisplayLevel()));
						e.exit();
						use = true;
					}
				if(use)
					owner.broadcastPacket(new MagicSkillUse(owner, 5579, 1, 0, 0));
			}
			catch(final Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				_cureTask = ThreadPoolManager.getInstance().scheduleEffect(this, Rnd.get(6, 8) * owner.calculateAttackDelay());
			}
		}
	}

	private class Disappear implements Runnable
	{
		@Override
		public void run()
		{
			deleteMe();
		}
	}
}