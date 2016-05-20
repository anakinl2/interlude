package l2d.game.model;

import l2d.Config;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.serverpackets.Earthquake;
import l2d.game.serverpackets.ExRedSky;
import l2d.game.serverpackets.SkillList;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.ItemTable;
import l2d.game.tables.SkillTable;
import l2d.util.Location;
import l2d.util.Rnd;

public class CursedWeapon
{
	private final String _name;
	private final int _itemId;
	private final Integer _skillId;
	private final int _skillMaxLevel;
	private int _dropRate;
	private int _durationMin;
	private int _durationMax;
	private int _durationLost;
	private int _disapearChance;
	private int _stageKills;

	public enum CursedWeaponState
	{
		NONE,
		ACTIVATED,
		DROPPED,
	}

	private CursedWeaponState _state = CursedWeaponState.NONE;

	private int _nbKills = 0;
	private long _endTime = 0;

	private int _playerId = 0;
	private L2Player _player = null;
	private L2ItemInstance _item = null;
	private int _playerKarma = 0;
	private int _playerPkKills = 0;
	private Location _loc = null;

	public CursedWeapon(final int itemId, final Integer skillId, final String name)
	{
		_name = name;
		_itemId = itemId;
		_skillId = skillId;
		_skillMaxLevel = SkillTable.getInstance().getMaxLevel(_skillId);
	}

	public void initWeapon()
	{
		_state = CursedWeaponState.NONE;
		_endTime = 0;
		_player = null;
		_playerId = 0;
		_playerKarma = 0;
		_playerPkKills = 0;
		_item = null;
		_nbKills = 0;
	}

	/** Выпадение оружия из монстра */
	public void create(final L2NpcInstance attackable, final L2Player killer)
	{
		if(attackable != null)
			if(Rnd.get(100000000) <= _dropRate)
			{
				_item = ItemTable.getInstance().createItem(_itemId);
				if(_item != null)
				{
					_player = null;
					_playerId = 0;
					_playerKarma = 0;
					_playerPkKills = 0;
					_state = CursedWeaponState.DROPPED;

					if(_endTime == 0)
						_endTime = System.currentTimeMillis() + getRndDuration() * 60000;

					_item.dropToTheGround(killer, attackable);
					_loc = _item.getLoc();

					_item.setDropTime(0);

					// RedSky and Earthquake
					final ExRedSky packet = new ExRedSky(10);
					final Earthquake eq = new Earthquake(killer.getLoc(), 30, 12);
					for(final L2Player aPlayer : L2World.getAllPlayers())
					{
						aPlayer.sendPacket(packet);
						aPlayer.sendPacket(eq);
					}
				}
			}
	}

	/**
	 * Выпадение оружия из владельца, или исчезновение с определенной вероятностью.
	 * Вызывается при смерти игрока.
	 */
	public boolean dropIt(final L2NpcInstance attackable, final L2Player killer, final L2Player owner)
	{
		if(Rnd.chance(_disapearChance))
			return false;

		if(_player == null)
			_player = owner;

		if(_player == null)
			return false;

		final L2ItemInstance oldItem = _player.getInventory().getItemByItemId(_itemId);
		if(oldItem == null)
			return false;

		final int oldCount = oldItem.getIntegerLimitedCount();
		_player.validateLocation(false); // TODO а нужно ли?

		final L2ItemInstance dropedItem = _player.getInventory().dropItem(oldItem, oldCount);
		if(dropedItem == null)
			return false;

		_player.setKarma(_playerKarma);
		_player.setPkKills(_playerPkKills);
		_player.setCursedWeaponEquippedId(0);
		_player.removeSkill(SkillTable.getInstance().getInfo(_skillId, _player.getSkillLevel(_skillId)), false);
		_player.abortAttack();

		_playerId = 0;
		_playerKarma = 0;
		_playerPkKills = 0;
		_state = CursedWeaponState.DROPPED;

		dropedItem.dropToTheGround(_player, (L2NpcInstance) null);
		_loc = dropedItem.getLoc();

		dropedItem.setDropTime(0);
		_item = dropedItem;

		_player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_DROPPED_S1).addItemName(dropedItem.getItemId()));

		_player.refreshExpertisePenalty();
		_player.broadcastUserInfo(true);

		final Earthquake eq = new Earthquake(_player.getLoc(), 30, 12);
		_player.broadcastPacket(eq);

		return true;
	}

	private void giveSkill()
	{
		int level = 1 + _nbKills / _stageKills;
		if(level > _skillMaxLevel)
			level = _skillMaxLevel;

		final L2Skill skill = SkillTable.getInstance().getInfo(_skillId, level);
		_player.addSkill(skill, false);

		if(Config.DEBUG)
			System.out.println(_player + " has been awarded with skill " + skill + " with activeClass " + _player.getActiveClass());
	}

	public void removeSkillAndAppearance()
	{

		_player.removeSkill(SkillTable.getInstance().getInfo(_skillId, _player.getSkillLevel(_skillId)), false);
		_player.sendPacket(new SkillList(_player));
	}

	/** вызывается при загрузке оружия */
	public boolean reActivate()
	{
		if(getTimeLeft() <= 0)
		{
			if(_playerId != 0)
				// to be sure, that cursed weapon will deleted in right way
				_state = CursedWeaponState.ACTIVATED;
			return false;
		}
		else if(_playerId == 0)
		{
			if(_loc == null)
				return false;

			_item = ItemTable.getInstance().createItem(_itemId);
			if(_item == null)
				return false;

			_item.dropMe(null, _loc);
			_item.setDropTime(0);

			_state = CursedWeaponState.DROPPED;
		}
		else
			_state = CursedWeaponState.ACTIVATED;
		return true;
	}

	public void activate(final L2Player player, final L2ItemInstance item)
	{
		// оружие уже в руках игрока или новый игрок
		if(_state != CursedWeaponState.ACTIVATED || _playerId != player.getObjectId())
		{
			_playerKarma = player.getKarma();
			_playerPkKills = player.getPkKills();
		}

		_state = CursedWeaponState.ACTIVATED;
		_player = player;
		_playerId = player.getObjectId();

		player.setCursedWeaponEquippedId(_itemId);
		player.setKarma(9999999);
		player.setPkKills(_nbKills);

		if(player.isInParty())
			player.getParty().oustPartyMember(player);

		if(player.isMounted())
			player.setMount(0, 0, 0);

		if(_endTime == 0)
			_endTime = System.currentTimeMillis() + getRndDuration() * 60000;

		giveSkill();

		_item = item;
		player.getInventory().equipItem(_item);
		player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EQUIPPED_YOUR_S1).addItemName(_item.getItemId()));

		player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
		player.setCurrentCp(player.getMaxCp());
		player.broadcastUserInfo(true);
	}

	public void increaseKills()
	{
		_nbKills++;

		_player.setPkKills(_nbKills);
		_player.broadcastUserInfo(true);

		if(_nbKills % _stageKills == 0 && _nbKills <= _stageKills * (_skillMaxLevel - 1))
			giveSkill();

		// Reduce time-to-live
		_endTime -= _durationLost * 60000;
	}

	public void setDisapearChance(final int disapearChance)
	{
		_disapearChance = disapearChance;
	}

	public void setDropRate(final int dropRate)
	{
		_dropRate = dropRate;
	}

	public void setDurationMin(final int duration)
	{
		_durationMin = duration;
	}

	public void setDurationMax(final int duration)
	{
		_durationMax = duration;
	}

	public void setDurationLost(final int durationLost)
	{
		_durationLost = durationLost;
	}

	public void setStageKills(final int stageKills)
	{
		_stageKills = stageKills;
	}

	public void setNbKills(final int nbKills)
	{
		_nbKills = nbKills;
	}

	public void setPlayerId(final int playerId)
	{
		_playerId = playerId;
	}

	public void setPlayerKarma(final int playerKarma)
	{
		_playerKarma = playerKarma;
	}

	public void setPlayerPkKills(final int playerPkKills)
	{
		_playerPkKills = playerPkKills;
	}

	public void setState(final CursedWeaponState state)
	{
		_state = state;
	}

	public void setEndTime(final long endTime)
	{
		_endTime = endTime;
	}

	public void setPlayer(final L2Player player)
	{
		_player = player;
	}

	public void setItem(final L2ItemInstance item)
	{
		_item = item;
	}

	public void setLoc(final Location loc)
	{
		_loc = loc;
	}

	public CursedWeaponState getState()
	{
		return _state;
	}

	public boolean isActivated()
	{
		return _state == CursedWeaponState.ACTIVATED;
	}

	public boolean isDropped()
	{
		return _state == CursedWeaponState.DROPPED;
	}

	public long getEndTime()
	{
		return _endTime;
	}

	public String getName()
	{
		return _name;
	}

	public int getItemId()
	{
		return _itemId;
	}

	public L2ItemInstance getItem()
	{
		return _item;
	}

	public Integer getSkillId()
	{
		return _skillId;
	}

	public int getPlayerId()
	{
		return _playerId;
	}

	public L2Player getPlayer()
	{
		return _player;
	}

	public int getPlayerKarma()
	{
		return _playerKarma;
	}

	public int getPlayerPkKills()
	{
		return _playerPkKills;
	}

	public int getNbKills()
	{
		return _nbKills;
	}

	public int getStageKills()
	{
		return _stageKills;
	}

	/**
	 * Возвращает позицию (x, y, z)
	 * 
	 * @return Location
	 */
	public Location getLoc()
	{
		return _loc;
	}

	public int getRndDuration()
	{
		if(_durationMin > _durationMax)
			_durationMax = 2 * _durationMin;
		return Rnd.get(_durationMin, _durationMax);
	}

	public boolean isActive()
	{
		return _state == CursedWeaponState.ACTIVATED || _state == CursedWeaponState.DROPPED;
	}

	public int getLevel()
	{
		if(_nbKills > _stageKills * _skillMaxLevel)
			return _skillMaxLevel;
		return _nbKills / _stageKills;
	}

	public long getTimeLeft()
	{
		return _endTime - System.currentTimeMillis();
	}

	public Location getWorldPosition()
	{
		if(isActivated())
		{
			if(_player != null && _player.isOnline())
				return _player.getLoc();
		}
		else if(isDropped())
			if(_item != null)
				return _item.getLoc();

		return null;
	}
}