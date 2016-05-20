package com.lineage.game.model.instances;

import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import com.lineage.Config;
import com.lineage.ext.scripts.Events;
import com.lineage.game.ThreadPoolManager;
import com.lineage.game.ai.CtrlIntention;
import com.lineage.game.ai.L2CharacterAI;
import com.lineage.game.ai.L2StaticObjectAI;
import com.lineage.game.geodata.GeoEngine;
import com.lineage.game.idfactory.IdFactory;
import com.lineage.game.instancemanager.SiegeManager;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Clan;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.L2Summon;
import com.lineage.game.model.L2Territory;
import com.lineage.game.model.L2World;
import com.lineage.game.model.entity.SevenSigns;
import com.lineage.game.model.entity.residence.Castle;
import com.lineage.game.model.entity.residence.ClanHall;
import com.lineage.game.model.entity.residence.Residence;
import com.lineage.game.model.entity.siege.Siege;
import com.lineage.game.model.entity.siege.SiegeClan;
import com.lineage.game.network.L2GameClient;
import com.lineage.game.serverpackets.DoorStatusUpdate;
import com.lineage.game.serverpackets.MyTargetSelected;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.serverpackets.ValidateLocation;
import com.lineage.game.templates.L2CharTemplate;
import com.lineage.game.templates.L2Weapon;

public class L2DoorInstance extends L2Character
{
	protected static Logger _log = Logger.getLogger(L2DoorInstance.class.getName());

	protected final int _doorId;
	protected final String _name;
	private boolean open;
	public boolean geoOpen;
	private boolean _geodata = true;
	private boolean _unlockable;
	private boolean _isHPVisible;
	private boolean _siegeWeaponOlnyAttackable;
	private Residence _siegeUnit;
	private int upgradeHp;
	public L2Territory pos;
	public int key;
	public byte level = 1;
	public boolean killable;
	
	
	protected int _autoActionDelay = -1;
	@SuppressWarnings("unchecked")
	private ScheduledFuture _autoActionTask;

	private long _allow_time_open = 0; // храним время в которое можно будет открывать дверь вновь

	@Override
	public L2CharacterAI getAI()
	{
		if(_ai == null)
			_ai = new L2StaticObjectAI(this);
		return _ai;
	}

	public L2DoorInstance(final int objectId, final L2CharTemplate template, final int doorId, final String name, final boolean unlockable, final boolean showHp)
	{
		super(objectId, template);
		_doorId = doorId;
		_name = name;
		_unlockable = unlockable;
		_isHPVisible = showHp;
		geoOpen = true;
		pos = new L2Territory(doorId);
	}

	public boolean isUnlockable()
	{
		return _unlockable;
	}

	@Override
	public byte getLevel()
	{
		return level;
	}

	/**
	 * @return Returns the doorId.
	 */
	public int getDoorId()
	{
		return _doorId;
	}

	/**
	 * @return Returns the open.
	 */
	public boolean isOpen()
	{
		return open;
	}

	/**
	 * @param open
	 *            The open to set.
	 */
	public synchronized void setOpen(final boolean open)
	{
		this.open = open;
	}

	/**
	 * Sets the delay in milliseconds for automatic opening/closing
	 * of this door instance.
	 * <BR>
	 * <B>Note:</B> A value of -1 cancels the auto open/close task.
	 * 
	 * @param actionDelay
	 *            время задержки между действием
	 */
	public void setAutoActionDelay(final int actionDelay)
	{
		if(_autoActionDelay == actionDelay)
			return;

		if(_autoActionTask != null)
		{
			_autoActionTask.cancel(false);
			_autoActionTask = null;
		}

		if(actionDelay > -1)
			_autoActionTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new AutoOpenClose(), actionDelay, actionDelay);

		_autoActionDelay = actionDelay;
	}

	public int getDamage()
	{
		final int dmg = 6 - (int) Math.ceil(getCurrentHpRatio() * 6);
		if(dmg > 6)
			return 6;
		if(dmg < 0)
			return 0;
		return dmg;
	}

	// TODO разобраться
	public boolean isEnemyOf(final L2Character cha)
	{
		return true;
	}

	@Override
	public boolean isAutoAttackable(final L2Character attacker)
	{
		if(attacker == null)
			return false;
		final L2Player player = attacker.getPlayer();
		if(player == null)
			return false;
		final L2Clan clan = player.getClan();
		if(clan != null && SiegeManager.getSiege(this, true) == clan.getSiege() && clan.isDefender())
			return false;
		if(clan != null && getSiegeUnit() != null && clan.getClanId() == getSiegeUnit().getOwnerId())
			return false;
		return isAttackable();
	}

	public boolean isAttackable(final L2Character attacker)
	{
		if(isSiegeWeaponOnlyAttackable() && (!(attacker instanceof L2Summon) || !((L2Summon) attacker).isSiegeWeapon()))
			return false;
		return isAutoAttackable(attacker);
	}

	@Override
	public boolean isAttackable()
	{
		return isHPVisible();
	}

	@Override
	public void updateAbnormalEffect()
	{}

	/**
	 * Return null.<BR><BR>
	 */
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		return null;
	}

	@Override
	public L2Weapon getActiveWeaponItem()
	{
		return null;
	}

	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}

	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		return null;
	}

	@Override
	public void onAction(final L2Player player)
	{
		if(player == null)
			return;

		if(Events.onAction(player, this))
			return;

		if(player.getTarget() instanceof L2DoorInstance)
			switch(getDoorId())
			{
				case 19250001:
				case 19250002:
				{
					if(isNeedItemToOpenDoor(player, 9850, 1) || isNeedItemToOpenDoor(player, 9851, 1) || isNeedItemToOpenDoor(player, 9852, 1) || isNeedItemToOpenDoor(player, 9853, 1))
						switchOpenClose();
					break;
				}
			}

		if(this != player.getTarget())
		{
			player.setTarget(this);
			player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel()));

			if(isAutoAttackable(player))
				player.sendPacket(new DoorStatusUpdate(this));

			// correct location
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel()));
			if(isAutoAttackable(player))
				player.getAI().Attack(this, false);
			else if(!isInRange(player, INTERACTION_DISTANCE))
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			else
			{
				if(!Config.ALLOW_CH_DOOR_OPEN_ON_CLICK || getSiegeUnit() == null || getSiegeUnit().getSiege() == null || player.getClan() == null || player.getClanId() != getSiegeUnit().getOwnerId())
				{
					player.sendActionFailed();
					return;
				}
				if(getSiegeUnit() instanceof Castle && (player.getClanPrivileges() & L2Clan.CP_CS_OPEN_DOOR) == L2Clan.CP_CS_OPEN_DOOR)
					switchOpenClose();
				else if(getSiegeUnit() instanceof ClanHall && (player.getClanPrivileges() & L2Clan.CP_CH_OPEN_DOOR) == L2Clan.CP_CH_OPEN_DOOR)
					switchOpenClose();

				player.sendActionFailed();
			}
		}
	}

	/**
	 * Метод используется в открытии дверей, если требуются итемы в рюкзаке.
	 * 
	 * @param player
	 *            - Игрок.
	 * @param itemId
	 *            - ID Итема.
	 * @param count
	 *            - Количество итемов.
	 * @return - Можно открыть или нет. <p>
	 */
	private boolean isNeedItemToOpenDoor(final L2Player player, final int itemId, final int count)
	{
		if(player.getInventory().getItemByItemId(itemId) == null)
			return false;
		else if(player.getInventory().getItemByItemId(itemId).getCount() >= count)
			return true;
		return false;
	}

	/**
	 * Метод закрывающий / открывающий дверь.
	 */
	public void switchOpenClose()
	{
		if(!isOpen())
			openMe();
		else
			closeMe();
	}

	public void onActionShift(final L2GameClient client)
	{
		final L2Player player = client.getActiveChar();

		if(Events.onActionShift(player, this))
			return;

		player.sendActionFailed();
	}

	@Override
	public void broadcastStatusUpdate()
	{
		DoorStatusUpdate su = new DoorStatusUpdate(this);
		for(final L2Player player : L2World.getAroundPlayers(this))
			if(player != null)
				player.sendPacket(su);
	}

	public void onOpen()
	{
		scheduleCloseMe(60000);
	}

	public void onClose()
	{
		closeMe();
	}

	/**
	 * Вызывает задание на закрытие двери через заданное время.
	 * 
	 * @param delay
	 *            - Время в миллисекундах
	 */
	public final void scheduleCloseMe(final long delay)
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new CloseTask(), delay);
	}

	public final void closeMe()
	{
		synchronized (this)
		{
			if(!isOpen() || isDead())
				return;

			open = false;
		}

		setGeoOpen(false);
		broadcastStatusUpdate();
	}

	public final void openMe()
	{
		synchronized (this)
		{
			if(isOpen() || isDead())
				return;

			open = true;
		}

		setGeoOpen(true);
		broadcastStatusUpdate();
	}

	@Override
	public String toString()
	{
		return "door " + _doorId;
	}

	public String getDoorName()
	{
		return _name;
	}

	public void setSiegeUnit(final Residence siegeUnit)
	{
		_siegeUnit = siegeUnit;
	}

	public Residence getSiegeUnit()
	{
		return _siegeUnit;
	}

	@Override
	public void doDie(final L2Character killer)
	{
		final Siege s = SiegeManager.getSiege(this, true);
		if(s != null)
		{
			for(final SiegeClan sc : s.getDefenderClans().values())
			{
				final L2Clan clan = sc.getClan();
				if(clan != null)
					for(final L2Player player : clan.getOnlineMembers(0))
						if(player != null)
							player.sendPacket(new SystemMessage(SystemMessage.THE_CASTLE_GATE_HAS_BEEN_BROKEN_DOWN));
			}

			for(final SiegeClan sc : s.getAttackerClans().values())
			{
				final L2Clan clan = sc.getClan();
				if(clan != null)
					for(final L2Player player : clan.getOnlineMembers(0))
						if(player != null)
							player.sendPacket(new SystemMessage(SystemMessage.THE_CASTLE_GATE_HAS_BEEN_BROKEN_DOWN));
			}
		}

		setGeoOpen(true);

		super.doDie(killer);
	}

	@Override
	public void spawnMe()
	{
		super.spawnMe();
		if(!isOpen() && geoOpen)
			setGeoOpen(false);
		closeMe();
	}

	public boolean isHPVisible()
	{
		return _isHPVisible;
	}

	public void setHPVisible(final boolean val)
	{
		_isHPVisible = val;
	}

	@Override
	public int getMaxHp()
	{
		return super.getMaxHp() + upgradeHp;
	}

	public void setUpgradeHp(final int hp)
	{
		upgradeHp = hp;
	}

	public int getUpgradeHp()
	{
		return upgradeHp;
	}

	@Override
	public int getPDef(final L2Character target)
	{
		switch(SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE))
		{
			case SevenSigns.CABAL_DAWN:
				return (int) (super.getPDef(target) * 1.2);
			case SevenSigns.CABAL_DUSK:
				return (int) (super.getPDef(target) * 0.3);
			default:
				return super.getPDef(target);
		}
	}

	@Override
	public int getMDef(final L2Character target, final L2Skill skill)
	{
		switch(SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE))
		{
			case SevenSigns.CABAL_DAWN:
				return (int) (super.getMDef(target, skill) * 1.2);
			case SevenSigns.CABAL_DUSK:
				return (int) (super.getMDef(target, skill) * 0.3);
			default:
				return super.getMDef(target, skill);
		}
	}

	/**
	 * Двери на осадах уязвимы во время осады.
	 * Остальные двери не уязвимы вообще.
	 * 
	 * @return инвульная ли дверь.
	 */
	@Override
	public boolean isInvul()
	{
		if(killable)
			return false;
		
		final Siege siege = SiegeManager.getSiege(this, true);
		return siege == null || !siege.isInProgress();
	}

	public int getDoorHeight()
	{
		return pos.getZmax() - pos.getZmin() & 0xfff0;
	}

	/**
	 * Дверь/стена может быть атаоквана только осадным орудием
	 * 
	 * @return true если дверь/стену можно атаковать только осадным орудием
	 */
	public boolean isSiegeWeaponOnlyAttackable()
	{
		return _siegeWeaponOlnyAttackable;
	}

	/**
	 * Устанавливает двери/стене признак возможности атаковать только осадным оружием
	 * 
	 * @param val
	 *            true - дверь/стену можно атаковать только осадным орудием
	 */
	public void setSiegeWeaponOlnyAttackable(final boolean val)
	{
		_siegeWeaponOlnyAttackable = val;
	}

	/**
	 * Устанавливает значение закрытости\открытости в геодате<br>
	 * 
	 * @param val
	 *            новое значение
	 */
	private void setGeoOpen(final boolean val)
	{
		if(geoOpen == val)
			return;

		geoOpen = val;

		if(!getGeodata())
			return;

		if(val)
			GeoEngine.openDoor(pos);
		else
			GeoEngine.closeDoor(pos);
	}

	private class CloseTask implements Runnable
	{
		@Override
		public void run()
		{
			onClose();
		}
	}

	/**
	 * Manages the auto open and closing of a door.
	 */
	private class AutoOpenClose implements Runnable
	{
		@Override
		public void run()
		{
			String doorAction;
			if(!isOpen())
			{
				doorAction = "opened";
				openMe();
			}
			else
			{
				doorAction = "closed";
				closeMe();
			}

			if(Config.DEBUG)
				_log.info("Auto " + doorAction + " door ID " + _doorId + " (" + _name + ") for " + _autoActionDelay / 1000 / 60 + " minute(s).");
		}
	}

	public void setGeodata(final boolean value)
	{
		_geodata = value;
	}

	public boolean getGeodata()
	{
		return _geodata;
	}

	@Override
	public L2DoorInstance clone()
	{
		final L2DoorInstance door = new L2DoorInstance(IdFactory.getInstance().getNextId(), _template, _doorId, _name, _unlockable, _isHPVisible);
		door.setXYZInvisible(getX(), getY(), getZ());
		door.setCurrentHpMp(door.getMaxHp(), door.getMaxMp(), true);
		door.setOpen(false);
		door.setSiegeWeaponOlnyAttackable(_siegeWeaponOlnyAttackable);
		door.setGeodata(_geodata);
		door.pos = pos;
		door.level = level;
		door.key = key;
		return door;
	}

	@Override
	public boolean isFearImmune()
	{
		return true;
	}

	@Override
	public boolean isParalyzeImmune()
	{
		return true;
	}

	@Override
	public boolean isLethalImmune()
	{
		return true;
	}

	public void setAllowOpenTime(long time)
	{
		_allow_time_open = time;
	}

	public boolean allowOpen()
	{
		return System.currentTimeMillis() >= _allow_time_open;
	}
}