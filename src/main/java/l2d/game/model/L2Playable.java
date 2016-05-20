package l2d.game.model;

import static l2d.game.model.L2Zone.ZoneType.Siege;
import static l2d.game.model.L2Zone.ZoneType.peace_zone;
import javolution.util.FastList;
import l2d.Config;
import l2d.ext.mods.balancer.Balancer;
import l2d.ext.mods.balancer.Balancer.bflag;
import l2d.ext.multilang.CustomMessage;
import l2d.game.ai.CtrlEvent;
import l2d.game.ai.CtrlIntention;
import l2d.game.cache.Msg;
import l2d.game.geodata.GeoEngine;
import l2d.game.instancemanager.SiegeManager;
import l2d.game.model.L2Skill.SkillTargetType;
import l2d.game.model.L2Skill.SkillType;
import l2d.game.model.L2Zone.ZoneType;
import l2d.game.model.entity.Duel;
import l2d.game.model.entity.Duel.DuelState;
import l2d.game.model.entity.siege.Siege;
import l2d.game.model.instances.L2ArtefactInstance;
import l2d.game.model.instances.L2DoorInstance;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.model.instances.L2SiegeGuardInstance;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.skills.Stats;
import l2d.game.tables.SkillTable;
import l2d.game.templates.L2CharTemplate;
import l2d.game.templates.L2EtcItem;
import l2d.game.templates.L2Weapon;
import l2d.game.templates.L2Weapon.WeaponType;
import l2d.util.Rnd;

public abstract class L2Playable extends L2Character
{
	/**
	 * this если Player или owner если Summon
	 */
	private L2Player _owner;
	private boolean _isSilentMoving = false;

	private long _checkAggroTimestamp = 0;

	public L2Playable(final int objectId, final L2CharTemplate template, final L2Player owner)
	{
		super(objectId, template);
		setOwner(owner == null ? (L2Player) this : owner);
	}

	@Override
	public boolean isAttackable()
	{
		return true;
	}

	public abstract Inventory getInventory();

	/**
	 * Проверяет, выставлять ли PvP флаг для игрока.<BR><BR>
	 */
	@Override
	public boolean checkPvP(final L2Character target, final L2Skill skill)
	{
		if(isDead() || target == null || getOwner() == null || target == this || target == getOwner() || target == getOwner().getPet() || getOwner().getKarma() > 0)
			return false;

		if(skill != null)
		{
			if(skill.altUse())
				return false;
			if(skill.getSkillType().equals(SkillType.BEAST_FEED))
				return false;
			if(skill.getTargetType() == SkillTargetType.TARGET_UNLOCKABLE)
				return false;
			if(skill.getTargetType() == SkillTargetType.TARGET_CHEST)
				return false;
		}

		// Проверка на дуэли... Мэмбэры одной дуэли не флагаются
		if(getDuel() != null && getDuel() == target.getDuel())
			return false;

		if(isInZone(peace_zone) || target.isInZone(peace_zone) || isInZoneBattle() || target.isInZoneBattle())
			return false;
		if(isInZone(Siege) && target.isInZone(Siege))
			return false;
		if(skill == null || skill.isOffensive())
		{
			if(target.getKarma() > 0)
				return false;
			else if(target instanceof L2Playable)
				return true;
		}
		else if(target.getPvpFlag() > 0 || target.getKarma() > 0 || target.isMonster())
			return true;

		return false;
	}

	/**
	 * Проверяет, можно ли атаковать цель (для физ атак)
	 */
	public boolean checkAttack(final L2Character target)
	{
		if(target == null || target.isDead())
		{
			getOwner().sendPacket(Msg.INVALID_TARGET);
			return false;
		}

		if(!isInRange(target, 2000))
		{
			getOwner().sendPacket(Msg.YOUR_TARGET_IS_OUT_OF_RANGE);
			return false;
		}

		if(target instanceof L2DoorInstance && !((L2DoorInstance) target).isAttackable(this))
		{
			getOwner().sendPacket(Msg.INVALID_TARGET);
			return false;
		}

		if(target.paralizeOnAttack(getOwner()))
		{
			if(Config.PARALIZE_ON_RAID_DIFF)
				paralizeMe(target);
			return false;
		}

		if(!GeoEngine.canSeeTarget(this, target, false) || getReflection() != target.getReflection())
		{
			getOwner().sendPacket(Msg.CANNOT_SEE_TARGET);
			return false;
		}

		if(target instanceof L2Playable)
		{
			// Нельзя атаковать того, кто находится на арене, если ты сам не на арене
			if(isInZoneBattle() != target.isInZoneBattle())
			{
				getOwner().sendPacket(Msg.INVALID_TARGET);
				return false;
			}

			// Если цель либо атакующий находится в мирной зоне - атаковать нельзя
			if((isInZonePeace() || target.isInZonePeace()) && !getOwner().getPlayerAccess().PeaceAttack)
			{
				getOwner().sendPacket(Msg.YOU_MAY_NOT_ATTACK_THIS_TARGET_IN_A_PEACEFUL_ZONE);
				return false;
			}
			if(getOwner().isInOlympiadMode() && !getOwner().isOlympiadCompStart())
				return false;
			
			L2Player pcAttacker = target.getPlayer();
			if(pcAttacker != null && pcAttacker != getOwner())
			{
				if(pcAttacker.getTeam() > 0 && pcAttacker.isChecksForTeam() && getOwner().getTeam() == 0) // Запрет на атаку/баф участником эвента незарегистрированного игрока
					return false;
				if(getOwner().getTeam() > 0 && getOwner().isChecksForTeam() && pcAttacker.getTeam() == 0) // Запрет на атаку/баф участника эвента незарегистрированным игроком
					return false;
				if(getOwner().getTeam() > 0 && getOwner().isChecksForTeam() && pcAttacker.getTeam() > 0 && pcAttacker.isChecksForTeam() && getOwner().getTeam() == pcAttacker.getTeam()) // Свою команду атаковать нельзя
					return false;
			}
		}
		return true;
	}

	@Override
	public void doAttack(final L2Character target)
	{
		if(isAMuted() || isAttackingNow())
		{
			getOwner().sendActionFailed();
			return;
		}

		if(getOwner().inObserverMode())
		{
			getOwner().sendMessage(new CustomMessage("l2d.game.model.L2Playable.OutOfControl.ObserverNoAttack", getOwner()));
			return;
		}

		if(!checkAttack(target))
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null, null);
			getOwner().sendActionFailed();
			return;
		}

		// Прерывать дуэли если цель не дуэлянт
		if(getDuel() != null)
			if(target.getDuel() != getDuel())
				getDuel().setDuelState(getPlayer(), DuelState.Interrupted);
			else if(getDuel().getDuelState(getOwner()) == DuelState.Interrupted)
			{
				getOwner().sendPacket(Msg.INVALID_TARGET);
				return;
			}

		final L2Weapon weaponItem = getActiveWeaponItem();

		if(weaponItem != null && (weaponItem.getItemType() == WeaponType.BOW))
		{
			double bowMpConsume = weaponItem.getMpConsume();
			if(bowMpConsume > 0)
			{
				// cheap shot SA
				final double chance = calcStat(Stats.MP_USE_BOW_CHANCE, 0., null, null);
				if(chance > 0 && Rnd.chance(chance))
					bowMpConsume = calcStat(Stats.MP_USE_BOW, bowMpConsume, null, null);

				if(_currentMp < bowMpConsume)
				{
					getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null, null);
					getOwner().sendPacket(Msg.NOT_ENOUGH_MP);
					getOwner().sendActionFailed();
					return;
				}

				reduceCurrentMp(bowMpConsume, null);
			}

			if(!getOwner().checkAndEquipArrows())
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null, null);
				getOwner().sendPacket(getOwner().getActiveWeaponInstance().getItemType() == WeaponType.BOW ? Msg.YOU_HAVE_RUN_OUT_OF_ARROWS : Msg.NOT_ENOUGH_BOLTS);
				getOwner().sendActionFailed();
				return;
			}
		}

		super.doAttack(target);
	}

	@Override
	public int getPAtkSpd()
	{
		return isPlayer() ? Balancer.getModify(bflag.attackSpeed, Math.max((int) calcStat(Stats.POWER_ATTACK_SPEED, calcStat(Stats.ATK_BASE, _template.basePAtkSpd, null, null), null, null), 1), getPlayer().getClassId().getId()) : Math.max((int) calcStat(Stats.POWER_ATTACK_SPEED, calcStat(Stats.ATK_BASE, _template.basePAtkSpd, null, null), null, null), 1);
	}

	@Override
	public int getPAtk(final L2Character target)
	{
		final double init = getActiveWeaponInstance() == null ? _template.basePAtk : 0;
		return (int) calcStat(Stats.POWER_ATTACK, init, target, null);
	}

	@Override
	public int getMAtk(final L2Character target, final L2Skill skill)
	{
		if(skill != null && skill.getMatak() > 0)
			return skill.getMatak();
		final double init = getActiveWeaponInstance() == null ? _template.baseMAtk : 0;
		return isPlayer() ? Balancer.getModify(bflag.matak, (int) calcStat(Stats.MAGIC_ATTACK, init, target, skill), getPlayer().getClassId().getId()) : (int) calcStat(Stats.MAGIC_ATTACK, init, target, skill);
	}

	@Override
	public boolean isAutoAttackable(final L2Character attacker)
	{
		if(attacker == null || getOwner() == null || attacker == this || attacker == getOwner() || isDead() || attacker.isAlikeDead())
			return false;

		if(getOwner().isInOlympiadMode() && !getOwner().isOlympiadCompStart())
			return false;

		L2Player pcAttacker1 = attacker.getPlayer();
		if(pcAttacker1 != null && pcAttacker1 != attacker)
		{
			if(pcAttacker1.getTeam() > 0 && pcAttacker1.isChecksForTeam() && getOwner().getTeam() == 0) // Запрет на атаку/баф участником эвента незарегистрированного игрока
				return false;
			if(getOwner().getTeam() > 0 && getOwner().isChecksForTeam() && pcAttacker1.getTeam() == 0) // Запрет на атаку/баф участника эвента незарегистрированным игроком
				return false;
			if(getOwner().getTeam() > 0 && getOwner().isChecksForTeam() && pcAttacker1.getTeam() > 0 && pcAttacker1.isChecksForTeam() && getOwner().getTeam() == pcAttacker1.getTeam()) // Свою команду атаковать нельзя
				return false;
		}
		// Автоатака на дуэлях, только враг и только если он еше не проиграл.
		if(getDuel() != null && attacker.getDuel() == getDuel())
		{
			// Тут не может быть ClassCastException у attacker, т.к. L2Character.getDuel() всегда возвращает null
			final L2Player enemy = attacker.isPlayer() ? (L2Player) attacker : ((L2Summon) attacker).getPlayer();

			if(enemy.getDuel().getTeamForPlayer(enemy) != getDuel().getTeamForPlayer(getOwner()) && getDuel().getDuelState(getOwner()) == Duel.DuelState.Fighting && getDuel().getDuelState(enemy) == Duel.DuelState.Fighting)
				return true;
		}

		if(!GeoEngine.canSeeTarget(attacker, this, false) || getReflection() != attacker.getReflection())
			return false;

		// TODO: check for friendly mobs
		if(attacker.isMonster())
			return true;
		final L2Player pcAttacker = attacker.getPlayer();
		final L2Clan clan1 = getOwner().getClan();
		if(pcAttacker != null)
		{
			final L2Party party1 = getOwner().getParty();
			final L2Party party2 = pcAttacker.getParty();
			if(party1 != null && party1 == party2)
				return false;
			// Если цель либо атакующий находится в мирной зоне - атаковать нельзя
			if((isInZonePeace() || pcAttacker.isInZonePeace()) && !getOwner().getPlayerAccess().PeaceAttack)
				if(getKarma() <= 0 && pcAttacker.getKarma() <= 0)
					return false;
			if(isInZoneBattle() && attacker.isInZoneBattle())
				return true;
			final L2Clan clan2 = pcAttacker.getClan();
			if(clan1 != null && clan2 != null)
			{
				if(clan1.getClanId() == clan2.getClanId())
					return false;
				if(pcAttacker.atMutualWarWith(getOwner()))
					return true;
			}
			if(isInZone(ZoneType.Siege) && attacker.isInZone(ZoneType.Siege))
			{
				if(clan1 == null || clan2 == null)
					return true;
				if(!clan1.isDefender() && !clan1.isAttacker())
					return true;
				if(!clan2.isDefender() && !clan2.isAttacker())
					return true;
				if(clan1 != clan2 && !(clan1.isDefender() && clan2.isDefender()))
					return true;
				return false;
			}
			if(getOwner().getKarma() > 0 || getOwner().getPvpFlag() != 0)
				return true;
		}
		else if(attacker instanceof L2SiegeGuardInstance)
			if(clan1 != null)
			{
				final Siege siege = SiegeManager.getSiege(this, true);
				return siege != null && siege.checkIsAttacker(clan1);
			}
		return false;
	}

	@Override
	public int getKarma()
	{
		if(getOwner() == null)
			return 0;
		return getOwner().getKarma();
	}

	@Override
	public void callSkill(final L2Skill skill, final FastList<L2Character> targets, final boolean useActionSkills)
	{
		final FastList<L2Character> toRemove = new FastList<L2Character>();

		if(useActionSkills && !skill.altUse() && !skill.getSkillType().equals(SkillType.BEAST_FEED))
			for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
			{
				if(n == null)
					break;
				final L2Character target = n.getValue();

				if(target.isInvul() && skill.isOffensive() && !(target instanceof L2ArtefactInstance))
					toRemove.add(target);

				if(!skill.isOffensive())
				{
					if(target instanceof L2Playable && target != getPet() && !(this instanceof L2Summon && target == getPlayer()))
					{
						final int aggro = skill.getEffectPoint() != 0 ? skill.getEffectPoint() : Math.max(1, (int) skill.getPower());
						for(final L2NpcInstance monster : target.getHateList().keySet())
							if(monster != null && !monster.isDead() && monster.isInRange(this, 2000) && monster.getAI().getIntention() == CtrlIntention.AI_INTENTION_ATTACK)
								if(!skill.isHandler() && monster.paralizeOnAttack(getOwner()))
								{
									if(Config.PARALIZE_ON_RAID_DIFF)
										paralizeMe(monster);
									return;
								}
								else if(monster.hasAI())
								{
									monster.getAI().notifyEvent(CtrlEvent.EVT_SEE_SPELL, skill, this);
									monster.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, this, aggro);
								}
					}
				}
				else if(target instanceof L2NpcInstance)
					// mobs will hate on debuff
					if(target.paralizeOnAttack(getOwner()))
					{
						if(Config.PARALIZE_ON_RAID_DIFF)
							paralizeMe(target);
						return;
					}
					else if(target.hasAI())
					{
						target.getAI().notifyEvent(CtrlEvent.EVT_SEE_SPELL, skill, this);
						if(!skill.isAI())
						{
							final int damage = skill.getEffectPoint() != 0 ? skill.getEffectPoint() : 1;
							target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, this, damage);
							target.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, this, damage);
						}
					}
				// Check for PvP Flagging / Drawing Aggro
				if(checkPvP(target, skill))
					startPvPFlag(target);
			}

		for(final L2Character cha : toRemove)
			targets.remove(cha);

		super.callSkill(skill, targets, useActionSkills);
	}

	@Override
	public void setXYZ(final int x, final int y, final int z, final boolean MoveTask)
	{
		super.setXYZ(x, y, z, MoveTask);

		if(!MoveTask || getOwner() == null || isAlikeDead() || isInvul() || !isVisible() || getCurrentRegion() == null)
			return;

		final long now = System.currentTimeMillis();
		if(now - _checkAggroTimestamp < Config.AGGRO_CHECK_INTERVAL || getOwner().getNonAggroTime() > now)
			return;

		_checkAggroTimestamp = now;
		if(getAI().getIntention() == CtrlIntention.AI_INTENTION_FOLLOW && (!isPlayer() || getFollowTarget() != null && getFollowTarget().getPlayer() != null && !getFollowTarget().getPlayer().isSilentMoving()))
			return;

		for(final L2NpcInstance obj : L2World.getAroundNpc(this))
			if(obj != null && obj.hasAI())
				obj.getAI().checkAggression(this);
	}

	/**
	 * Оповещает других игроков о поднятии вещи
	 * 
	 * @param item
	 *            предмет который был поднят
	 */
	public void broadcastPickUpMsg(final L2ItemInstance item)
	{
		if(item == null || getPlayer() == null || getPlayer().isInvisible())
			return;

		if(item.isEquipable() && !(item.getItem() instanceof L2EtcItem))
		{
			SystemMessage msg = null;
			final String player_name = getPlayer().getName();
			if(item.getEnchantLevel() > 0)
			{
				final int msg_id = isPlayer() ? SystemMessage.ATTENTION_S1_PICKED_UP__S2_S3 : SystemMessage.ATTENTION_S1_PET_PICKED_UP__S2_S3;
				msg = new SystemMessage(msg_id).addString(player_name).addNumber(item.getEnchantLevel()).addItemName(item.getItemId());
			}
			else
			{
				final int msg_id = isPlayer() ? SystemMessage.ATTENTION_S1_PICKED_UP_S2 : SystemMessage.ATTENTION_S1_PET_PICKED_UP__S2_S3;
				msg = new SystemMessage(msg_id).addString(player_name).addItemName(item.getItemId());
			}
			getPlayer().sendPacket(msg);
			getPlayer().broadcastPacketToOthers(msg);
		}
	}

	public void paralizeMe(final L2Character effector)
	{
		final L2Skill revengeSkill = SkillTable.getInstance().getInfo(L2Skill.SKILL_RAID_CURSE, 1);
		revengeSkill.getEffects(effector, getOwner(), false, false);
		if(getOwner() != this)
			revengeSkill.getEffects(effector, this, false, false);
	}

	/**
	 * Set the Silent Moving mode Flag.<BR><BR>
	 */
	public void setSilentMoving(final boolean flag)
	{
		_isSilentMoving = flag;
	}

	/**
	 * @return True if the Silent Moving mode is active.<BR><BR>
	 */
	public boolean isSilentMoving()
	{
		return _isSilentMoving;
	}

	protected void setOwner(final L2Player owner)
	{
		_owner = owner;
	}

	private L2Player getOwner()
	{
		return _owner;
	}

	@Override
	public L2Player getPlayer()
	{
		return getOwner();
	}
}