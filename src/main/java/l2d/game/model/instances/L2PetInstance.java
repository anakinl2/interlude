package l2d.game.model.instances;

import java.sql.ResultSet;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import com.lineage.ext.multilang.CustomMessage;
import l2d.game.ThreadPoolManager;
import l2d.game.cache.Msg;
import l2d.game.idfactory.IdFactory;
import l2d.game.instancemanager.CursedWeaponsManager;
import l2d.game.model.L2Character;
import l2d.game.model.L2Object;
import l2d.game.model.L2Party;
import l2d.game.model.L2PetData;
import l2d.game.model.L2Playable;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.L2Summon;
import l2d.game.model.L2World;
import l2d.game.model.PetInventory;
import l2d.game.model.base.Experience;
import l2d.game.serverpackets.InventoryUpdate;
import l2d.game.serverpackets.ItemList;
import l2d.game.serverpackets.PetDelete;
import l2d.game.serverpackets.PetInfo;
import l2d.game.serverpackets.PetItemList;
import l2d.game.serverpackets.PetStatusUpdate;
import l2d.game.serverpackets.SocialAction;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.skills.Stats;
import l2d.game.tables.PetDataTable;
import l2d.game.taskmanager.DecayTaskManager;
import l2d.game.templates.L2NpcTemplate;
import l2d.game.templates.L2Weapon;

public class L2PetInstance extends L2Summon
{
	class FeedTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				L2Player owner = getPlayer();
				if(owner == null || getCurrentFed() <= 0.10 * getMaxFed())
				{
					// Если пища закончилась, отозвать пета
					setCurrentFed((int) (0.10 * getMaxFed()));
					stopFeed();
					if(owner != null)
						owner.sendMessage(new CustomMessage("l2d.game.model.instances.L2PetInstance.UnSummonHungryPet", owner));
					unSummon();
					return;
				}

				setCurrentFed(getCurrentFed() - 1);

				L2ItemInstance food = getInventory().getItemByItemId(getFoodId());
				if(food != null && getCurrentFed() < 0.55 * getMaxFed())
				{
					int newFed = Math.min(getMaxFed(), getCurrentFed() + getMaxFed() * getAddFed() / 100);
					if(getCurrentFed() != newFed)
					{
						removeItemFromInventory(food, 1, true);
						setCurrentFed(newFed);
						sendPetStatusUpdate();
					}
				}

				sendPetStatusUpdate();
				startFeed(isInCombat());
			}
			catch(Throwable e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}

	public void sendPetStatusUpdate()
	{
		L2Player owner = getPlayer();
		if(owner != null)
			owner.sendPacket(new PetStatusUpdate(this));
	}

	public void sendPetInfo()
	{
		L2Player owner = getPlayer();
		if(owner != null)
			owner.sendPacket(new PetInfo(this, false));
	}

	public void sendItemList()
	{
		L2Player owner = getPlayer();
		if(owner != null)
			owner.sendPacket(new PetItemList(this));
	}

	protected static Logger _log = Logger.getLogger(L2PetInstance.class.getName());

	private static L2PetInstance restore(L2ItemInstance control, L2NpcTemplate template, L2Player owner)
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT objId, name, level, curHp, curMp, exp, sp, fed FROM pets WHERE item_obj_id=?");
			statement.setInt(1, control.getObjectId());
			rset = statement.executeQuery();
			L2PetInstance pet = null;
			if(!rset.next())
			{
				if(PetDataTable.isBabyPet(template.getNpcId()))
					pet = new L2PetBabyInstance(IdFactory.getInstance().getNextId(), template, owner, control);
				else
					pet = new L2PetInstance(IdFactory.getInstance().getNextId(), template, owner, control);
				return pet;
			}
			if(PetDataTable.isBabyPet(template.getNpcId()))
				pet = new L2PetBabyInstance(rset.getInt("objId"), template, owner, control, rset.getByte("level"), rset.getLong("exp"));
			else
				pet = new L2PetInstance(rset.getInt("objId"), template, owner, control, rset.getByte("level"), rset.getLong("exp"));
			pet.setRespawned(true);
			pet.setName(rset.getString("name"));
			pet.setCurrentHpMp(rset.getDouble("curHp"), rset.getInt("curMp"), true);
			pet.setCurrentCp(pet.getMaxCp());
			pet.setSp(rset.getInt("sp"));
			pet.setCurrentFed(rset.getInt("fed"));
			return pet;
		}
		catch(Exception e)
		{
			_log.warning("could not restore Pet data from item[" + control.getObjectId() + "]: " + e);
			e.printStackTrace();
			return null;
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}
	}

	public static L2PetInstance spawnPet(L2NpcTemplate template, L2Player owner, L2ItemInstance control)
	{
		L2PetInstance result = restore(control, template, owner);
		if(result != null)
			result.updateControlItem();
		return result;
	}

	private final int _controlItemObjId;
	private int _curFed;
	protected L2PetData _data;
	private Future<?> _feedTask;
	protected PetInventory _inventory;
	private byte _level;
	private boolean _respawned;
	private int lostExp;

	/**
	 * Создание нового пета
	 */
	public L2PetInstance(int objectId, L2NpcTemplate template, L2Player owner, L2ItemInstance control)
	{
		super(objectId, template, owner);

		_controlItemObjId = control.getObjectId();

		byte itemEnchant = (byte) getControlItem().getEnchantLevel();

		// Sin Eater
		if(template.npcId == PetDataTable.SIN_EATER_ID)
		{
			_level = itemEnchant;
			if(_level <= 0)
				_level = owner.getLevel();
		}
		else if(itemEnchant > 0)
			_level = itemEnchant;
		else
			_level = template.level;

		byte minLevel = (byte) PetDataTable.getMinLevel(template.npcId);
		if(_level < minLevel)
			_level = minLevel;

		_exp = getExpForThisLevel();

		_data = PetDataTable.getInstance().getInfo(template.npcId, _level);

		_inventory = new PetInventory(this);

		transferPetItems();

		startFeed(false);
	}

	/**
	 * Загрузка уже существующего пета
	 */
	public L2PetInstance(int objectId, L2NpcTemplate template, L2Player owner, L2ItemInstance control, byte currentLevel, long exp)
	{
		super(objectId, template, owner);

		_controlItemObjId = control.getObjectId();
		_exp = exp;

		// Sin Eater
		if(template.npcId == PetDataTable.SIN_EATER_ID)
		{
			_level = (byte) getControlItem().getEnchantLevel();
			if(_level <= 0)
			{
				_level = owner.getLevel();
				_exp = getExpForThisLevel();
			}
		}
		else
			_level = currentLevel == 0 ? template.level : currentLevel;

		byte minLevel = (byte) PetDataTable.getMinLevel(template.npcId);

		while(_exp >= getExpForNextLevel() && _level < Experience.getMaxLevel())
			_level++;

		while(_exp < getExpForThisLevel() && _level > minLevel)
			_level--;

		if(_level < minLevel)
		{
			_level = minLevel;
			_exp = getExpForThisLevel();
		}

		_data = PetDataTable.getInstance().getInfo(template.npcId, _level);

		_inventory = new PetInventory(this);
		_inventory.restore();

		transferPetItems();

		startFeed(false);
	}

	private void transferPetItems()
	{
		L2Player owner = getPlayer();
		if(owner == null)
			return;

		boolean transferred = false;

		for(L2ItemInstance item : owner.getInventory().getItemsList())
			if(!item.isEquipped() && (item.getCustomFlags() & L2ItemInstance.FLAG_PET_EQUIPPED) == L2ItemInstance.FLAG_PET_EQUIPPED)
			{
				if(_inventory.getTotalWeight() + item.getItem().getWeight() * item.getCount() > getMaxLoad())
				{
					owner.sendPacket(new SystemMessage(SystemMessage.EXCEEDED_PET_INVENTORYS_WEIGHT_LIMIT));
					continue;
				}
				owner.getInventory().dropItem(item, item.getCount());
				item.setCustomFlags(item.getCustomFlags() | L2ItemInstance.FLAG_PET_EQUIPPED);
				_inventory.addItem(item);
				tryEquipItem(item, false);
				transferred = true;
			}

		if(transferred)
		{
			sendItemList();
			broadcastPetInfo();
			owner.sendPacket(new ItemList(owner, false));
		}
	}

	public boolean tryEquipItem(L2ItemInstance item, boolean broadcast)
	{
		if(!item.isEquipable())
			return false;

		int petId = ((L2NpcTemplate) _template).npcId;

		if(PetDataTable.isWolf(petId) && item.getItem().isForWolf() || PetDataTable.isHatchling(petId) && item.getItem().isForHatchling() || PetDataTable.isStrider(petId) && item.getItem().isForStrider() || PetDataTable.isBabyPet(petId) && item.getItem().isForPetBaby())
		{
			if(item.isEquipped())
				_inventory.unEquipItemInSlot(item.getEquipSlot());
			else
				_inventory.equipItem(item);

			if(broadcast)
			{
				sendItemList();
				broadcastPetInfo();
			}
			return true;
		}
		return false;
	}

	@Override
	public void addExpAndSp(long addToExp, long addToSp)
	{
		addExpAndSp(addToExp, addToSp, true, true);
	}

	@Override
	public void addExpAndSp(long addToExp, long addToSp, boolean applyBonus, boolean appyToPet)
	{
		L2Player owner = getPlayer();
		if(owner == null)
			return;

		if(_exp > getMaxExp())
			_exp = getMaxExp();

		_exp += addToExp;
		_sp += addToSp;

		if(addToExp > 0 || addToSp > 0)
			owner.sendPacket(new SystemMessage(SystemMessage.THE_PET_ACQUIRED_EXPERIENCE_POINTS_OF_S1).addNumber((int) addToExp));

		int old_level = _level;

		while(_exp >= getExpForNextLevel() && _level < Experience.getMaxLevel())
			_level++;

		while(_exp < getExpForThisLevel() && _level > getMinLevel())
			_level--;

		if(old_level != _level)
		{
			updateControlItem();
			updateData();
		}

		boolean needStatusUpdate = true;

		if(old_level > _level)
		{
			owner.sendMessage(new CustomMessage("l2d.game.model.instances.L2PetInstance.PetLevelUp", owner).addNumber(_level));
			broadcastPacket(new SocialAction(getObjectId(), 15));
			setCurrentHpMp(getMaxHp(), getMaxMp());
			needStatusUpdate = false;
		}

		if(needStatusUpdate && (addToExp > 0 || addToSp > 0))
			broadcastStatusUpdate();
	}

	@Override
	public boolean consumeItem(int itemConsumeId, int itemCount)
	{
		L2ItemInstance item = getInventory().getItemByItemId(itemConsumeId);
		return !(item == null || item.getCount() < itemCount) && getInventory().destroyItem(item, itemCount, false) != null;
	}

	private void deathPenalty()
	{
		/*if(isInZoneBattle())
			return;
		int lvl = getLevel();
		double percentLost = -0.07 * lvl + 6.5;
		// Calculate the Experience loss
		lostExp = (int) Math.round((getExpForNextLevel() - getExpForThisLevel()) * percentLost / 100);
		addExpAndSp(-lostExp, 0);*/
	}

	/**
	 * Remove the Pet from DB and its associated item from the player inventory
	 */
	private void destroyControlItem()
	{
		L2Player owner = getPlayer();
		if(owner == null)
			return;
		if(getControlItemObjId() == 0)
			return;
		// pet control item no longer exists, delete the pet from the db
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id=?");
			statement.setInt(1, getControlItemObjId());
			statement.execute();
		}
		catch(Exception e)
		{
			_log.warning("could not delete pet:" + e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}

		try
		{
			L2ItemInstance removedItem = owner.getInventory().destroyItem(getControlItemObjId(), 1, true);
			L2World.removeObject(removedItem);
		}
		catch(Exception e)
		{
			_log.warning("Error while destroying control item: " + e);
		}
	}

	@Override
	public void doDie(L2Character killer)
	{
		super.doDie(killer);
		L2Player owner = getPlayer();
		if(owner == null)
		{
			onDecay();
			return;
		}
		stopFeed();
		deathPenalty();
		owner.sendPacket(new SystemMessage(SystemMessage.THE_PET_HAS_BEEN_KILLED_IF_YOU_DO_NOT_RESURRECT_IT_WITHIN_24_HOURS_THE_PETS_BODY_WILL_DISAPPEAR_ALONG_WITH_ALL_THE_PETS_ITEMS));
		DecayTaskManager.getInstance().addDecayTask(this, 86400000);
	}

	@Override
	public void doPickupItem(L2Object object)
	{
		L2Player owner = getPlayer();
		if(owner == null)
			return;

		stopMove();

		if(!(object instanceof L2ItemInstance))
		{
			owner.sendActionFailed();
			return;
		}

		L2ItemInstance target = (L2ItemInstance) object;

		if(CursedWeaponsManager.getInstance().isCursed(target.getItemId()))
		{
			owner.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_PICK_UP_S1).addItemName(target.getItemId()));
			return;
		}

		synchronized (target)
		{
			if(!target.isVisible())
			{
				owner.sendActionFailed();
				return;
			}

			if(getInventory().getTotalWeight() + target.getItem().getWeight() * target.getCount() > getMaxLoad())
			{
				owner.sendPacket(new SystemMessage(SystemMessage.EXCEEDED_PET_INVENTORYS_WEIGHT_LIMIT));
				return;
			}

			if(target.isHerb())
			{
				L2Skill[] skills = target.getItem().getAttachedSkills();
				if(skills != null && skills.length > 0)
					for(L2Skill skill : skills)
						altUseSkill(skill, this);
				target.decayMe();
				L2World.removeObject(target);
				return;
			}

			target.pickupMe(this);
		}

		if(owner.getParty() == null || owner.getParty().getLootDistribution() == L2Party.ITEM_LOOTER)
		{
			if(target.getItemId() == 57)
				owner.sendPacket(new SystemMessage(SystemMessage.S1_HAS_OBTAINED_S2_ADENA).addString("Your pet").addNumber(target.getCount()));
			else if(target.getCount() == 1)
				owner.sendPacket(new SystemMessage(SystemMessage.S1_HAS_OBTAINED_S2).addString("Your pet").addItemName(target.getItemId()));
			else
				owner.sendPacket(new SystemMessage(SystemMessage.S1_HAS_OBTAINED_S3_S2).addString("Your pet").addItemName(target.getItemId()).addNumber(target.getCount()));
			target.setCustomFlags(target.getCustomFlags() | L2ItemInstance.FLAG_PET_EQUIPPED);
			synchronized (_inventory)
			{
				_inventory.addItem(target);
			}
			sendItemList();
			sendPetInfo();
		}
		else
			owner.getParty().distributeItem(owner, target);
		broadcastPickUpMsg(target);

		setFollowStatus(getFollowStatus());
	}

	public void doRevive(double percent)
	{
		restoreExp(percent);
		doRevive();
		stopDecay();
		startFeed(false);
		setRunning();
		setFollowStatus(true);
	}

	@Override
	public int getAccuracy()
	{
		return (int) calcStat(Stats.ACCURACY_COMBAT, _data.getAccuracy(), null, null);
	}

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

	public L2ItemInstance getControlItem()
	{
		L2Player owner = getPlayer();
		if(owner == null)
			return null;
		int item_obj_id = getControlItemObjId();
		if(item_obj_id == 0)
			return null;
		return owner.getInventory().getItemByObjectId(item_obj_id);
	}

	@Override
	public int getControlItemObjId()
	{
		return _controlItemObjId;
	}

	@Override
	public int getCriticalHit(L2Character target, L2Skill skill)
	{
		return (int) calcStat(Stats.CRITICAL_BASE, _data.getCritical(), target, skill);
	}

	@Override
	public int getCurrentFed()
	{
		return _curFed;
	}

	@Override
	public int getEvasionRate(L2Character target)
	{
		return (int) calcStat(Stats.EVASION_RATE, _data.getEvasion(), target, null);
	}

	@Override
	public long getExpForNextLevel()
	{
		return PetDataTable.getInstance().getInfo(getNpcId(), (byte) (_level + 1)).getExp();
	}

	@Override
	public long getExpForThisLevel()
	{
		return PetDataTable.getInstance().getInfo(getNpcId(), _level).getExp();
	}

	public int getFoodId()
	{
		return _data.getFoodId();
	}

	public int getAddFed()
	{
		return _data.getAddFed();
	}

	@Override
	public PetInventory getInventory()
	{
		return _inventory;
	}

	@Override
	public final byte getLevel()
	{
		return _level;
	}

	@Override
	public int getMAtk(L2Character target, L2Skill skill)
	{
		return (int) calcStat(Stats.MAGIC_ATTACK, _data.getMAtk(), target, skill);
	}

	@Override
	public int getMAtkSpd()
	{
		return (int) calcStat(Stats.MAGIC_ATTACK_SPEED, _data.getCastSpeed(), null, null);
	}

	public long getMaxExp()
	{
		return PetDataTable.getInstance().getInfo(getNpcId(), Experience.getMaxLevel()).getExp();
	}

	@Override
	public int getMaxFed()
	{
		return _data.getFeedMax();
	}

	@Override
	public int getMaxHp()
	{
		return (int) calcStat(Stats.MAX_HP, _data.getHP(), null, null);
	}

	@Override
	public int getMaxLoad()
	{
		return (int) calcStat(Stats.MAX_LOAD, _data.getMaxLoad(), null, null);
	}

	@Override
	public int getMaxMp()
	{
		return (int) calcStat(Stats.MAX_MP, _data.getMP(), null, null);
	}

	@Override
	public int getMDef(L2Character target, L2Skill skill)
	{
		return (int) calcStat(Stats.MAGIC_DEFENCE, _data.getMDef(), target, skill);
	}

	public int getMinLevel()
	{
		return _data.getMinLevel();
	}

	@Override
	public int getPAtk(L2Character target)
	{
		return (int) calcStat(Stats.POWER_ATTACK, _data.getPAtk(), target, null);
	}

	@Override
	public int getPAtkSpd()
	{
		return (int) calcStat(Stats.POWER_ATTACK_SPEED, _data.getAtkSpeed(), null, null);
	}

	@Override
	public int getPDef(L2Character target)
	{
		return (int) calcStat(Stats.POWER_DEFENCE, _data.getPDef(), target, null);
	}

	@Override
	public int getRunSpeed()
	{
		return getSpeed(_data.getSpeed());
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

	public int getSkillLevel(int skillId)
	{
		if(_skills == null || _skills.get(skillId) == null)
			return -1;
		int lvl = getLevel();
		return lvl > 70 ? 7 + (lvl - 70) / 5 : lvl / 10;
	}

	@Override
	public int getSummonType()
	{
		return 2;
	}

	@Override
	public L2NpcTemplate getTemplate()
	{
		return (L2NpcTemplate) _template;
	}

	@Override
	public boolean isMountable()
	{
		return _data.isMountable();
	}

	public boolean isRespawned()
	{
		return _respawned;
	}

	@Override
	public void reduceCurrentHp(double damage, L2Character attacker, L2Skill skill, boolean awake, boolean standUp, boolean directHp, boolean canReflect)
	{
		if(attacker instanceof L2Playable && isInZoneBattle() != attacker.isInZoneBattle())
		{
			L2Player player = attacker.getPlayer();
			if(player != null)
				player.sendPacket(Msg.INVALID_TARGET);
			return;
		}

		super.reduceCurrentHp(damage, attacker, skill, awake, standUp, directHp, canReflect);

		L2Player owner = getPlayer();
		if(owner == null)
			return;

		if(!isDead() && attacker != null)
		{
			SystemMessage sm = new SystemMessage(SystemMessage.THE_PET_RECEIVED_DAMAGE_OF_S2_CAUSED_BY_S1);
			if(attacker instanceof L2NpcInstance)
				sm.addNpcName(((L2NpcInstance) attacker).getTemplate().npcId);
			else
				sm.addString(attacker.getName());
			sm.addNumber((int) damage);
			owner.sendPacket(sm);
		}
	}

	public void removeItemFromInventory(L2ItemInstance item, int count, boolean toLog)
	{
		synchronized (_inventory)
		{
			_inventory.destroyItem(item.getObjectId(), count, toLog);
		}
	}

	public void restoreExp(double percent)
	{
		if(lostExp != 0)
		{
			addExpAndSp((long) (lostExp * percent / 100.), 0);
			lostExp = 0;
		}
	}

	@Override
	public void sendChanges()
	{
		L2Player owner = getPlayer();
		if(owner == null)
			return;
		broadcastStatusUpdate();
		owner.sendPacket(new PetItemList(this));
	}

	public void setCurrentFed(int num)
	{
		_curFed = Math.min(getMaxFed(), Math.max(0, num));
	}

	public void setRespawned(boolean respawned)
	{
		_respawned = respawned;
	}

	@Override
	public void setSp(int sp)
	{
		_sp = sp;
	}

	public void startFeed(boolean battleFeed)
	{
		stopFeed();
		if(!isDead() && getPlayer() != null)
		{
			int feedTime = Math.max(1000, 60000 / (battleFeed ? _data.getFeedBattle() : _data.getFeedNormal()));
			_feedTask = ThreadPoolManager.getInstance().scheduleEffect(new FeedTask(), feedTime);
		}
	}

	private void stopFeed()
	{
		if(_feedTask != null)
		{
			_feedTask.cancel(false);
			_feedTask = null;
		}
	}

	@Override
	public void stopDecay()
	{
		DecayTaskManager.getInstance().cancelDecayTask(this);
	}

	public void store()
	{
		if(getControlItemObjId() == 0 || _exp == 0)
			return;

		String req;
		if(!isRespawned())
			req = "INSERT INTO pets (name,level,curHp,curMp,exp,sp,fed,objId,item_obj_id) VALUES (?,?,?,?,?,?,?,?,?)";
		else
			req = "UPDATE pets SET name=?,level=?,curHp=?,curMp=?,exp=?,sp=?,fed=?,objId=? WHERE item_obj_id = ?";
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(req);
			statement.setString(1, _name);
			statement.setInt(2, _level);
			statement.setDouble(3, getCurrentHp());
			statement.setDouble(4, getCurrentMp());
			statement.setLong(5, _exp);
			statement.setLong(6, _sp);
			statement.setInt(7, _curFed);
			statement.setInt(8, _objectId);
			statement.setInt(9, _controlItemObjId);
			statement.executeUpdate();
			_respawned = true;
		}
		catch(Exception e)
		{
			_log.warning("could not store pet data: " + e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	@Override
	public void onDecay()
	{
		deleteMe();
	}

	@Override
	public void deleteMe()
	{
		giveAllToOwner();
		destroyControlItem(); // this should also delete the pet from the db
		super.deleteMe();
	}

	@Override
	public synchronized void unSummon()
	{
		stopFeed();

		setTarget(null);
		stopMove();
		decayMe();
		detachAI();

		giveAllToOwner();

		L2Player owner = getPlayer();
		if(owner != null)
		{
			owner.sendPacket(new PetDelete(getObjectId(), 2));
			owner.setPet(null);
			setOwner(null);
		}

		store();

		L2World.removeObject(this);
	}

	private void giveAllToOwner()
	{
		L2Player owner = getPlayer();
		synchronized (_inventory)
		{
			for(L2ItemInstance item : _inventory.getItems())
			{
				item = _inventory.dropItem(item, item.getIntegerLimitedCount());
				if(owner != null)
					owner.getInventory().addItem(item);
				else
					item.dropMe(this, getLoc().changeZ(25));
			}
		}
	}

	public void updateControlItem()
	{
		L2ItemInstance controlItem = getControlItem();
		if(controlItem == null)
			return;
		controlItem.setEnchantLevel(_level);
		controlItem.setCustomType2(getName() == null ? 0 : 1);

		L2Player owner = getPlayer();
		if(owner != null)
			owner.sendPacket(new InventoryUpdate().addModifiedItem(controlItem));
	}

	private void updateData()
	{
		_data = PetDataTable.getInstance().getInfo(getTemplate().npcId, _level);
	}

	@Override
	public float getExpPenalty()
	{
		return .1f;
	}

	@Override
	public void displayHitMessage(L2Character target, int damage, boolean crit, boolean miss)
	{
		L2Player owner = getPlayer();
		if(owner == null)
			return;
		if(crit)
			owner.sendPacket(Msg.PETS_CRITICAL_HIT);
		if(miss)
			owner.sendPacket(new SystemMessage(SystemMessage.C1S_ATTACK_WENT_ASTRAY).addName(this));
		else
			owner.sendPacket(new SystemMessage(SystemMessage.THE_PET_GAVE_DAMAGE_OF_S1).addNumber(damage));
	}
}