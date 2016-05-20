package l2d.game.model;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import javolution.util.FastList;
import javolution.util.FastMap;
import l2d.game.model.L2Effect.EffectType;
import l2d.game.model.L2Skill.SkillType;
import l2d.game.serverpackets.ShortBuffStatusUpdate;
import l2d.game.skills.Stats;
import l2d.game.skills.effects.EffectTemplate;
import l2d.game.skills.funcs.Func;
import l2d.game.skills.funcs.FuncTemplate;
import l2d.game.skills.skillclasses.Effect;
import l2d.util.GArray;

public class EffectList
{
	private WeakReference<L2Character> _owner;
	private ConcurrentLinkedQueue<L2Effect> _effects;

	// private Object _lock = new Object();

	public EffectList(final L2Character owner)
	{
		setOwner(owner);
	}

	/**
	 * Возвращает число эффектов соответствующее данному скиллу
	 */
	public int getEffectsCountForSkill(final int skill_id)
	{
		if(_effects == null)
			return 0;
		int count = 0;
		for(final L2Effect e : _effects)
			if(e.getSkill().getId() == skill_id)
				count++;
		return count;
	}

	public L2Effect getEffectByType(final EffectType et)
	{
		if(_effects != null)
			for(final L2Effect e : _effects)
				if(e.getEffectType() == et)
					return e;
		return null;
	}

	public FastList<L2Effect> getEffectsBySkill(final L2Skill skill)
	{
		if(skill == null)
			return null;
		return getEffectsBySkillId(skill.getId());
	}

	public FastList<L2Effect> getEffectsBySkillId(final int skillId)
	{
		if(_effects == null)
			return null;
		final FastList<L2Effect> temp = new FastList<L2Effect>();
		for(final L2Effect e : _effects)
			if(e.getSkill().getId() == skillId)
				temp.add(e);

		return temp.size() > 0 ? temp : null;
	}

	public L2Effect getEffectByIndexAndType(final int skill_id, final EffectType type)
	{
		if(_effects == null)
			return null;
		for(final L2Effect e : _effects)
			if(e.getSkill().getId() == skill_id && e.getEffectType() == type)
				return e;
		return null;
	}

	public ConcurrentLinkedQueue<L2Effect> getAllEffects()
	{
		if(_effects == null)
			return new ConcurrentLinkedQueue<L2Effect>();
		return _effects;
	}

	public boolean isEmpty()
	{
		return _effects == null || _effects.isEmpty();
	}

	/**
	 * Возвращает первые эффекты для всех скиллов. Нужно для отображения не
	 * более чем 1 иконки для каждого скилла.
	 */
	public L2Effect[] getAllFirstEffects()
	{
		if(_effects == null)
			return new L2Effect[0];

		final FastMap<Integer, L2Effect> temp = new FastMap<Integer, L2Effect>();

		if(_effects != null)
			for(final L2Effect ef : _effects)
				if(ef != null)
					temp.put(ef.getSkill().getId(), ef);

		final Collection<L2Effect> temp2 = temp.values();

		temp2.toArray(new L2Effect[temp2.size()]);

		return temp2.toArray(new L2Effect[temp2.size()]);
	}

	/**s
	 * Ограничение на количество бафов
	 */

	private void checkBuffSlots3(L2Effect newEffect)
	{
		L2Character owner = getOwner();
		if(owner == null)
			return;
		if(_effects == null || _effects.size() < 12)
			return;

		if(isNotUsedBuffSlot(newEffect))
			return;

		int buffs = 0;
		GArray<Integer> skills = new GArray<Integer>(_effects.size());
		for(L2Effect ef : _effects)
			if(ef != null && ef.isInUse())
			{
				if(ef.getSkill().equals(newEffect.getSkill()))
					return;

				if(!isNotUsedBuffSlot(ef) && !skills.contains(ef.getSkill().getId()))
				{
					buffs++;
					skills.add(ef.getSkill().getId());
				}
			}
		if(buffs < owner.getBuffLimit())
			return;

		for(L2Effect ef : _effects)
			if(ef != null && ef.isInUse())
				if(!isNotUsedBuffSlot(ef) && ef.getSkill().isSongDance() == newEffect.getSkill().isSongDance())
				{
					stopEffect(ef.getSkill().getId());
					break;
				}
	}
	
	
	private void checkBuffSlots(L2Effect newEffect)
	{
		if(_effects == null)
			return;

		L2Character owner = getOwner();

		int slotType = getSlotType(newEffect);
		if(slotType == NONE_SLOT_TYPE)
			return;

		int size = 0;
		GArray<Integer> skillIds = new GArray<Integer>();
		for(L2Effect e : _effects)
			if(e.isInUse())
			{
				if(e.getSkill().equals(newEffect.getSkill())) // мы уже имеем эффект от этого скилла
					return;

				if(!skillIds.contains(e.getSkill().getId()))
				{
					int subType = getSlotType(e);
					if(subType == slotType)
					{
						size++;
						skillIds.add(e.getSkill().getId());
					}
				}
			}

		int limit = 0;
		switch(slotType)
		{
			case BUFF_SLOT_TYPE:
				limit = owner.getBuffLimit();
				break;
			case DEBUFF_SLOT_TYPE:
				limit = DEBUFF_LIMIT;
				break;
			case SELFBUFF_SLOT_TYPE:
				limit = owner.getBuffLimit() + DEBUFF_LIMIT;
				break;		
		}

		if(size < limit)
			return;

		owner.sendMessage("slots-" + limit + "  s-" + size);
		
		int skillId = 0;
		for(L2Effect e : _effects)
			if(e.isInUse())
				if(getSlotType(e) == slotType || (getSlotType(e) == BUFF_SLOT_TYPE || getSlotType(e) == SELFBUFF_SLOT_TYPE))
				{
					skillId = e.getSkill().getId();
					break;
				}

		if(skillId != 0)
			stopEffect(skillId);
	}
	
	private boolean isNotUsedBuffSlot(L2Effect ef)
	{
		return ef.getSkill().isLikePassive()  || ef.getSkill().isOffensive() || ef.getSkill().isToggle() || ef.getStackType().startsWith("HpRecoverCast") || ef.getStackType().startsWith("MPRecoverCast");
	}
	
	public static final int NONE_SLOT_TYPE = -1;
	public static final int BUFF_SLOT_TYPE = 0;
	public static final int MUSIC_SLOT_TYPE = 1;
	public static final int TRIGGER_SLOT_TYPE = 2;
	public static final int DEBUFF_SLOT_TYPE = 3;
	public static final int SELFBUFF_SLOT_TYPE = 4;

	public static final int DEBUFF_LIMIT = 8;

	static GArray<Integer> buffer = new GArray<Integer>();

	public static int getSlotType(L2Effect ef)
	{
		if(ef.getSkill().isPassive() || ef.getSkill().isToggle() || ef.getStackType().startsWith("HpRecoverCast") || ef.getStackType().startsWith("MPRecoverCast"))
			return NONE_SLOT_TYPE;
		else if(ef.getSkill().isOffensive())
			return DEBUFF_SLOT_TYPE;
		else if(ef.getEffected().getKnownSkill(ef.getSkill().getId()) != null)
			return SELFBUFF_SLOT_TYPE;
		else
			return BUFF_SLOT_TYPE;
	}

	public static boolean checkStackType(final EffectTemplate ef1, final EffectTemplate ef2)
	{
		if(ef1._stackType != EffectTemplate.NO_STACK && ef1._stackType.equalsIgnoreCase(ef2._stackType))
			return true;
		if(ef1._stackType != EffectTemplate.NO_STACK && ef1._stackType.equalsIgnoreCase(ef2._stackType2))
			return true;
		if(ef1._stackType2 != EffectTemplate.NO_STACK && ef1._stackType2.equalsIgnoreCase(ef2._stackType))
			return true;
		if(ef1._stackType2 != EffectTemplate.NO_STACK && ef1._stackType2.equalsIgnoreCase(ef2._stackType2))
			return true;
		return false;
	}
	
	public synchronized void addEffect(final L2Effect newEffect)
	{
		final L2Character owner = getOwner();
		if(owner == null)
			return;

		if(newEffect == null)
			return;

		// Хербы при вызванном саммоне делятся с саммоном пополам
		if((owner.isSummon() || (owner.getPet() != null && !owner.getPet().isDead() && owner.getPet().isSummon())) && (newEffect.getSkill().getId() >= 2278 && newEffect.getSkill().getId() <= 2285 || newEffect.getSkill().getId() >= 2512 && newEffect.getSkill().getId() <= 2514))
		{
			newEffect.setPeriod(newEffect.getPeriod() / 2);
			if(!owner.isSummon())
				owner.getPet().altUseSkill(newEffect.getSkill(), owner.getPet());
		}

		boolean sheduleNew = false;

		// synchronized (_lock)
		// {
		if(_effects == null)
			_effects = new ConcurrentLinkedQueue<L2Effect>();

		// затычка на баффы повышающие хп/мп
		final double hp = owner.getCurrentHp();
		final double mp = owner.getCurrentMp();
		final double cp = owner.getCurrentCp();

		// Проверка на имунность к бафам/дебафам
		if(owner.isEffectImmune() && newEffect.getEffectType() != EffectType.BuffImmunity)
		{
			final SkillType st = newEffect.getSkill().getSkillType();
			if(st == SkillType.BUFF || st == SkillType.DEBUFF)
				return;
		}

		final String stackType = newEffect.getStackType();

		if(stackType == EffectTemplate.NO_STACK)
		{
			// Удаляем такие же эффекты
			for(final L2Effect ef : _effects)
				if(ef != null && ef.isInUse() && ef.getStackType() == EffectTemplate.NO_STACK && ef.getSkill().getId() == newEffect.getSkill().getId() && ef.getEffectType() == newEffect.getEffectType())
					// Если оставшаяся длительность старого эффекта больше чем длительность нового, то оставляем старый.
					if(newEffect.getTimeLeft() > ef.getTimeLeft())
						ef.exit();
					else
						return;
		}
		else
			// Проверяем, нужно ли накладывать эффект, при совпадении StackType.
			// Новый эффект накладывается только в том случае, если у него больше StackOrder и больше длительность.
			// Если условия подходят - удаляем старый.
			for(int i = 1; i <= 3; i++)
				for(final L2Effect ef : _effects)
					if(ef != null)
					{
						if(!ef.isInUse())
						{
							ef.exit();
							continue;
						}

						if(!checkStackType(ef._template, newEffect._template))
							continue;

						if(ef.getSkill().getId() == newEffect.getSkill().getId() && ef.getEffectType() != newEffect.getEffectType())
							break;

						// Эффекты со StackOrder == -1 заменить нельзя (например, Root).
						if(ef.getStackOrder() == -1)
							return;

						// Если новый эффект слабее
						if(newEffect.getStackOrder() < ef.getStackOrder())
						{
							// Если его длительность больше то шедулить
							if(newEffect.getTimeLeft() > ef.getTimeLeft())
							{
								i = 4;
								sheduleNew = true;
								ef.scheduleNext(newEffect);
								break;
							}
							// Иначе выход
							return;
						}

						// Если эффекты равны
						else if(newEffect.getStackOrder() == ef.getStackOrder())
						{
							// Если новый эффект короче то выйти
							if(newEffect.getTimeLeft() < ef.getTimeLeft())
								return;

							// Чистим все ненужные зашедуленные
							L2Effect next = ef;
							while((next = next.getNext()) != null)
								if(newEffect.getTimeLeft() > next.getTimeLeft())
								{
									next.exit();
									break;
								}

							// Присоединяем зашедуленные эффекты от старого к новому
							if(ef.getNext() != null && !ef.getNext().isEnded())
								newEffect.scheduleNext(ef.getNext());

							// Отсоединяем зашедуленные от старого
							ef.removeNext();

							// Останавливаем старый
							ef.exit();
							break;
						}

						// Если новый эффект сильнее
						else if(newEffect.getStackOrder() > ef.getStackOrder())
						{
							// Если старый короче то просто остановить его
							if(newEffect.getTimeLeft() > ef.getTimeLeft())
							{
								ef.exit();
								break;
							}
							// Если новый короче то зашедулить старый
							owner.removeStatsOwner(ef);
							_effects.remove(ef);
							newEffect.scheduleNext(ef);
						}
					}

		if(!sheduleNew)
		{
			// Проверяем на лимиты бафов/дебафов
			checkBuffSlots(newEffect);

			// Добавляем новый эффект
			_effects.add(newEffect);

			if(newEffect.getEffector().isPlayer() && newEffect.getEffector().getDuel() != null)
				newEffect.getEffector().getDuel().onBuff((L2Player) newEffect.getEffector(), newEffect);

			// Применяем эффект к параметрам персонажа
			owner.addStatFuncs(newEffect.getStatFuncs());

			// затычка на баффы повышающие хп/мп
			for(final Func f : newEffect.getStatFuncs())
				if(f._stat == Stats.MAX_HP)
				{
					owner.setCurrentHp(hp, false);
					owner.startRegeneration();
				}
				else if(f._stat == Stats.MAX_MP)
				{
					owner.setCurrentMp(mp);
					owner.startRegeneration();
				}
				else if(f._stat == Stats.MAX_CP)
				{
					owner.setCurrentCp(cp);
					owner.startRegeneration();
				}
		}
		// }

		// Запускаем эффект
		newEffect.setInUse(true);

		// Обновляем иконки
		if(!sheduleNew)
			owner.updateEffectIcons();
	}

	/**
	 * Вызывающий метод синхронизирован, дополнительная синхронизация не нужна.
	 * 
	 * @see l2d.game.model.L2Effect#stopEffectTask()
	 * @param effect
	 *            эффект для удаления
	 */
	public void removeEffect(final L2Effect effect)
	{
		final L2Character owner = getOwner();
		if(owner == null)
			return;

		if(effect == null || _effects == null || !_effects.contains(effect))
			return;

		owner.removeStatsOwner(effect);
		effect.setInUse(false);

		_effects.remove(effect);

		if(effect.getNext() != null)
		{
			final L2Effect next = effect.getNext();
			boolean add = true;
			for(final L2Effect ef : _effects)
				if(ef != null && checkStackType(ef._template, next._template))
				{
					add = false;
					break;
				}
			if(add)
			{
				_effects.add(next);
				owner.addStatFuncs(next.getStatFuncs());
				next.updateEffects();
			}
		}

		if(effect.getStackType().equalsIgnoreCase("HpRecoverCast"))
			owner.sendPacket(new ShortBuffStatusUpdate());
		else
			owner.updateEffectIcons();
	}

	public void stopAllEffects()
	{
		final L2Character owner = getOwner();
		if(owner == null)
			return;

		if(_effects != null)
		{
			owner.setMassUpdating(true);
			for(final L2Effect e : _effects)
				if(e != null)
					e.exit();
			owner.setMassUpdating(false);
			owner.sendChanges();
			owner.updateEffectIcons();
		}
	}

	public void stopEffect(final int skillId)
	{
		if(_effects != null)
			for(final L2Effect e : _effects)
				if(e != null && e.getSkill().getId() == skillId)
					e.exit();
	}

	public void stopEffectByDisplayId(final int skillId)
	{
		if(_effects != null)
			for(final L2Effect e : _effects)
				if(e != null && e.getSkill().getDisplayId() == skillId)
					e.exit();
	}

	public void stopEffects(final EffectType type)
	{
		if(_effects != null)
			for(final L2Effect e : _effects)
				if(e.getEffectType() == type)
					e.exit();
	}

	private void setOwner(final L2Character owner)
	{
		_owner = owner == null ? null : new WeakReference<L2Character>(owner);
	}

	private L2Character getOwner()
	{
		return _owner == null ? null : _owner.get();
	}

	public L2Effect getEffectBySkillId(final int skillId)
	{
		if(_effects == null)
			return null;
		for(L2Effect e : _effects)
			if(e.getSkill().getId() == skillId)
				return e;
		return null;
	}

}