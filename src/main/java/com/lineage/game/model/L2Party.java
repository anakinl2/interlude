package com.lineage.game.model;

import java.util.ArrayList;
import java.util.concurrent.Future;

import com.lineage.Config;
import com.lineage.game.ThreadPoolManager;
import com.lineage.game.cache.Msg;
import com.lineage.game.instancemanager.PartyRoomManager;
import com.lineage.game.model.base.Experience;
import com.lineage.game.model.entity.DimensionalRift;
import com.lineage.game.model.entity.SevenSignsFestival.SevenSignsFestival;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.serverpackets.L2GameServerPacket;
import com.lineage.game.serverpackets.PartyMemberPosition;
import com.lineage.game.serverpackets.PartySmallWindowAdd;
import com.lineage.game.serverpackets.PartySmallWindowAll;
import com.lineage.game.serverpackets.PartySmallWindowDelete;
import com.lineage.game.serverpackets.PartySpelled;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.skills.Stats;
import com.lineage.game.tables.ItemTable;
import com.lineage.util.GArray;
import com.lineage.util.Location;
import com.lineage.util.Log;
import com.lineage.util.Rnd;

public class L2Party
{
	private final GArray<L2Player> _members = new GArray<L2Player>();
	private int _partyLvl = 0;
	private int _partySqLevelSum = 0;
	private int _itemDistribution = 0;
	private int _itemOrder = 0;
	private DimensionalRift _dr;
	private Reflection _reflection;
	private L2CommandChannel _commandChannel;

	public static final int ITEM_LOOTER = 0;
	public static final int ITEM_RANDOM = 1;
	public static final int ITEM_RANDOM_SPOIL = 2;
	public static final int ITEM_ORDER = 3;
	public static final int ITEM_ORDER_SPOIL = 4;

	public static final int MAX_SIZE = 9;
	public static final int MAX_INSTANCE_SIZE = 6;

	public float _rateExp;
	public float _rateSp;
	public float _rateDrop;
	public float _rateAdena;
	public float _rateSpoil;
	private Future<?> _positionBroadcastTask = null;
	private PartyMemberPosition _positionPacket;

	private static final int PARTY_POSITION_BROADCAST = 9000;

	/**
	 * constructor ensures party has always one member - leader
	 * 
	 * @param leader
	 *            создатель парти
	 * @param itemDistribution
	 *            режим распределения лута
	 */
	public L2Party(final L2Player leader, final int itemDistribution)
	{
		_itemDistribution = itemDistribution;
		_members.add(leader);
		_partyLvl = leader.getLevel();
		_partySqLevelSum = _partyLvl * _partyLvl;
	}

	/**
	 * @return number of party members
	 */
	public int getMemberCount()
	{
		return _members.size();
	}

	/**
	 * @return all party members
	 */
	public GArray<L2Player> getPartyMembers()
	{
		return _members;
	}

	public ArrayList<L2Playable> getPartyMembersWithPets()
	{
		final ArrayList<L2Playable> ret = new ArrayList<L2Playable>(_members);
		for(final L2Player p : _members)
			if(p.getPet() != null)
				ret.add(p.getPet());
		return ret;
	}

	public L2Player getRandomMember()
	{
		return _members.get(Rnd.get(_members.size()));
	}

	/**
	 * @return random member from party
	 */
	private L2Player getRandomMemberInRange(final L2Player player, final L2ItemInstance item, final int range)
	{
		final ArrayList<L2Player> ret = new ArrayList<L2Player>();

		for(final L2Player member : _members)
			if(member.isInRange(player, range) && !member.isDead() && member.getInventory().validateCapacity(item) && member.getInventory().validateWeight(item))
				ret.add(member);

		if(ret.size() > 0)
			return ret.get(Rnd.get(ret.size()));

		return null;
	}

	/**
	 * @return next item looter
	 */
	private L2Player getNextLooterInRange(final L2Player player, final L2ItemInstance item, final int range)
	{
		int antiloop = _members.size();
		while(--antiloop > 0)
		{
			final int looter = _itemOrder;
			_itemOrder++;
			if(_itemOrder > _members.size() - 1)
				_itemOrder = 0;

			final L2Player ret = looter < _members.size() ? _members.get(looter) : player;

			if(ret.isInRange(player, range) && !ret.isDead())
				return ret;
		}
		return player;
	}

	/**
	 * true if player is party leader
	 * 
	 * @param player
	 * @return
	 */
	public boolean isLeader(final L2Player player)
	{
		return _members.get(0).equals(player);
	}

	/**
	 * Returns the Object ID for the party leader to be used as a unique identifier of this party
	 * 
	 * @return int
	 */
	public int getPartyLeaderOID()
	{
		return _members.get(0).getObjectId();
	}

	/**
	 * Возвращает лидера партии
	 * 
	 * @return L2Player Лидер партии
	 */
	public L2Player getPartyLeader()
	{
		return _members.get(0);
	}

	/**
	 * Broadcasts packet to every party member
	 * 
	 * @param msg
	 *            packet to broadcast
	 */
	public void broadcastToPartyMembers(final L2GameServerPacket msg)
	{
		synchronized (_members)
		{
			for(final L2Player member : _members)
				member.sendPacket(msg);
		}
	}

	/**
	 * Рассылает текстовое сообщение всем членам группы
	 * 
	 * @param msg
	 *            сообщение
	 */
	public void broadcastMessageToPartyMembers(final String msg)
	{
		broadcastToPartyMembers(SystemMessage.sendString(msg));
	}

	/**
	 * Рассылает пакет всем членам группы исключая указанного персонажа<BR><BR>
	 */
	public void broadcastToPartyMembers(final L2Player exclude, final L2GameServerPacket msg)
	{
		synchronized (_members)
		{
			for(final L2Player member : _members)
				if(!member.equals(exclude))
					member.sendPacket(msg);
		}
	}

	public void broadcastToPartyMembersInRange(final L2Player player, final L2GameServerPacket msg, final int range)
	{
		synchronized (_members)
		{
			for(final L2Player member : _members)
				if(player.isInRange(member, range))
					member.sendPacket(msg);
		}
	}

	public boolean containsMember(final L2Character cha)
	{
		final L2Player player = cha.getPlayer();
		if(player == null)
			return false;

		for(final L2Player member : _members)
			if(member.getObjectId() == cha.getObjectId())
				return true;

		return false;
	}

	/**
	 * adds new member to party
	 * 
	 * @param player
	 *            L2Player to add
	 */
	public void addPartyMember(L2Player player)
	{
		// sends new member party window for all members
		// we do all actions before adding member to a list, this speeds things up a little
		player.sendPacket(new PartySmallWindowAll(_members, player));

		broadcastToPartyMembers(player, new PartySpelled(player, true));
		for(final L2Player member : _members)
			player.sendPacket(new PartySpelled(member, true));

		player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_JOINED_S1S_PARTY).addString(_members.get(0).getName()));
		broadcastToPartyMembers(new SystemMessage(SystemMessage.S1_HAS_JOINED_THE_PARTY).addString(player.getName()));
		broadcastToPartyMembers(new PartySmallWindowAdd(player));

		synchronized (_members)
		{
			_members.add(player);
		}

		recalculatePartyData();

		player.updateEffectIcons();
		if(player.getPet() != null)
			player.getPet().updateEffectIcons();

		// Если партия уже в СС, то вновь прибывшем посылаем пакет открытия окна СС
		if(isInCommandChannel())
			player.sendPacket(Msg.ExMPCCOpen);

		if(isInDimensionalRift())
			_dr.partyMemberInvited();

		if(player.getPartyRoom() > 0)
		{
			final PartyRoom room = PartyRoomManager.getInstance().getRooms().get(player.getPartyRoom());
			if(room != null)
				room.updateInfo();
		}

		if(_positionBroadcastTask == null)
			_positionBroadcastTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new PositionBroadcast(), PARTY_POSITION_BROADCAST / 2, PARTY_POSITION_BROADCAST);
	}

	/**
	 * removes player from party
	 * 
	 * @param player
	 *            L2Player to remove
	 */
	private void removePartyMember(final L2Player player)
	{
		if(player == null || !_members.contains(player))
			return;

		synchronized (_members)
		{
			_members.remove(player);
		}

		recalculatePartyData();

		if(player.isFestivalParticipant())
			SevenSignsFestival.getInstance().updateParticipants(player, this);

		// Отсылаемы вышедшему пакет закрытия СС
		if(isInCommandChannel())
			player.sendPacket(Msg.ExMPCCClose);

		player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_WITHDRAWN_FROM_THE_PARTY));
		player.sendPacket(Msg.PartySmallWindowDeleteAll);
		player.setParty(null);

		broadcastToPartyMembers(new SystemMessage(SystemMessage.S1_HAS_LEFT_THE_PARTY).addString(player.getName()));
		broadcastToPartyMembers(new PartySmallWindowDelete(player));

		if(isInDimensionalRift())
			_dr.partyMemberExited(player);
		if(isInReflection() && player.getReflection().getId() == getReflection().getId())
			player.teleToLocation(getReflection().getReturnLoc(), 0);

		if(player.getDuel() != null)
			player.getDuel().onRemoveFromParty(player);

		for(final L2Player member : getPartyMembers())
			if(member.getForceBuff() != null && member.getForceBuff().getTarget() == player)
				member.getForceBuff().delete();

		if(_members.size() == 1)
		{
			final L2Player lastMember = _members.get(0);

			if(lastMember.getDuel() != null)
				lastMember.getDuel().onRemoveFromParty(lastMember);

			// Если в партии остался 1 человек, то удаляем ее из СС
			if(isInCommandChannel())
				_commandChannel.removeParty(this);

			lastMember.setParty(null);

			if(isInReflection())
			{
				// lastMember.teleToLocation(getReflection().getReturnLoc(), 0);
				// getReflection().stopCollapseTimer();
				// getReflection().collapse();
				getReflection().startCollapseTimer(60000);
				if(lastMember.getReflection().getId() == getReflection().getId())
					lastMember.broadcastPacket(new SystemMessage(SystemMessage.THIS_DUNGEON_WILL_EXPIRE_IN_S1_MINUTES).addNumber(1));

				setReflection(null);
			}
		}
		else if(isInCommandChannel() && _commandChannel.getChannelLeader() == player)
			_commandChannel.setChannelLeader(_members.get(0));

		if(player.getPartyRoom() > 0)
		{
			final PartyRoom room = PartyRoomManager.getInstance().getRooms().get(player.getPartyRoom());
			if(room != null)
				room.updateInfo();
		}
	}

	/**
	 * Change party leader (used for string arguments)
	 * 
	 * @param name
	 *            имя нового лидера парти
	 */
	public void changePartyLeader(final String name)
	{
		final L2Player new_leader = getPlayerByName(name);

		synchronized (_members)
		{
			final L2Player current_leader = _members.get(0);

			if(new_leader == null || current_leader == null)
				return;

			if(current_leader.equals(new_leader))
			{
				current_leader.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_TRANSFER_RIGHTS_TO_YOURSELF));
				return;
			}

			if(!_members.contains(new_leader))
			{
				current_leader.sendPacket(new SystemMessage(SystemMessage.YOU_CAN_TRANSFER_RIGHTS_ONLY_TO_ANOTHER_PARTY_MEMBER));
				return;
			}

			// Меняем местами нового и текущего лидера
			final int idx = _members.indexOf(new_leader);
			_members.set(0, new_leader);
			_members.set(idx, current_leader);

			updateLeaderInfo();

			if(isInCommandChannel() && _commandChannel.getChannelLeader() == current_leader)
				_commandChannel.setChannelLeader(new_leader);
		}
	}

	public void updateLeaderInfo()
	{
		synchronized (_members)
		{
			final SystemMessage msg = new SystemMessage(SystemMessage.S1_HAS_BECOME_A_PARTY_LEADER).addString(_members.get(0).getName());
			for(final L2Player member : _members) // индивидуальные пакеты - удаления и инициализация пати
			{
				member.sendPacket(Msg.PartySmallWindowDeleteAll); // Удаляем все окошки
				member.sendPacket(new PartySmallWindowAll(_members, member)); // Показываем окошки
				member.sendPacket(msg); // Сообщаем о смене лидера
			}
		}
	}

	/**
	 * finds a player in the party by name
	 * 
	 * @param name
	 *            имя для поиска
	 * @return найденый L2Player или null если не найдено
	 */
	private L2Player getPlayerByName(final String name)
	{
		for(final L2Player pl : _members)
			if(pl.getName().equalsIgnoreCase(name))
				return pl;
		return null;
	}

	/**
	 * Oust player from party
	 * 
	 * @param player
	 *            L2Player которого выгоняют
	 */
	public void oustPartyMember(final L2Player player)
	{
		if(!_members.contains(player))
			return;

		if(isLeader(player))
		{
			removePartyMember(player);
			if(_members.size() > 1)
				updateLeaderInfo();
		}
		else
			removePartyMember(player);
	}

	/**
	 * Oust player from party Overloaded method that takes player's name as
	 * parameter
	 * 
	 * @param name
	 *            имя игрока для изгнания
	 */
	public void oustPartyMember(final String name)
	{
		oustPartyMember(getPlayerByName(name));
	}

	/**
	 * distribute item(s) to party members
	 * 
	 * @param player
	 * @param item
	 */
	public void distributeItem(final L2Player player, final L2ItemInstance item)
	{
		distributeItem(player, item, null);
	}

	public void distributeItem(final L2Player player, final L2ItemInstance item, final L2NpcInstance fromNpc)
	{
		L2Player target = player;

		switch(_itemDistribution)
		{
			case ITEM_RANDOM:
			case ITEM_RANDOM_SPOIL:
				target = getRandomMemberInRange(player, item, Config.ALT_PARTY_DISTRIBUTION_RANGE);
				break;
			case ITEM_ORDER:
			case ITEM_ORDER_SPOIL:
				target = getNextLooterInRange(player, item, Config.ALT_PARTY_DISTRIBUTION_RANGE);
				break;
			case ITEM_LOOTER:
			default:
				target = player;
				break;
		}

		if(target == null)
		{
			item.dropToTheGround(player, fromNpc);
			return;
		}

		if(!target.getInventory().validateWeight(item))
		{
			target.sendActionFailed();
			target.sendPacket(Msg.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
			item.dropToTheGround(target, fromNpc);
			return;
		}

		if(!target.getInventory().validateCapacity(item))
		{
			target.sendActionFailed();
			target.sendPacket(Msg.YOUR_INVENTORY_IS_FULL);
			item.dropToTheGround(player, fromNpc);
			return;
		}

		if(item.getCount() == 1)
		{
			SystemMessage smsg;
			if(item.getEnchantLevel() > 0)
			{
				smsg = new SystemMessage(SystemMessage.YOU_HAVE_OBTAINED__S1S2);
				smsg.addNumber(item.getEnchantLevel());
				smsg.addItemName(item.getItemId());
			}
			else
			{
				smsg = new SystemMessage(SystemMessage.YOU_HAVE_OBTAINED_S1);
				smsg.addItemName(item.getItemId());
			}

			target.sendPacket(smsg);

			if(item.getEnchantLevel() > 0)
			{
				smsg = new SystemMessage(SystemMessage.S1_HAS_OBTAINED__S2S3);
				smsg.addString(target.getName());
				smsg.addNumber(item.getEnchantLevel());
				smsg.addItemName(item.getItemId());
			}
			else
			{
				smsg = new SystemMessage(SystemMessage.S1_HAS_OBTAINED_S2);
				smsg.addString(target.getName());
				smsg.addItemName(item.getItemId());
			}

			broadcastToPartyMembers(target, smsg);
		}
		else
		{
			SystemMessage smsg = new SystemMessage(SystemMessage.YOU_HAVE_OBTAINED_S2_S1);
			smsg.addItemName(item.getItemId());
			smsg.addNumber(item.getCount());
			target.sendPacket(smsg);

			smsg = new SystemMessage(SystemMessage.S1_HAS_OBTAINED_S3_S2);
			smsg.addString(target.getName());
			smsg.addItemName(item.getItemId());
			smsg.addNumber(item.getCount());
			broadcastToPartyMembers(target, smsg);
		}

		// Remove the L2ItemInstance from the world and send server->client GetItem packets
		item.pickupMe(target);

		final L2ItemInstance item2 = target.getInventory().addItem(item);
		Log.LogItem(target, fromNpc, Log.GetItemInPaty, item2);

		target.sendChanges();
	}

	/**
	 * distribute adena to party members
	 * 
	 * @param adena
	 *            инстанс адены для распределения
	 */
	public void distributeAdena(final L2ItemInstance adena, final L2Player player)
	{
		distributeAdena(adena, null, player);
	}

	public void distributeAdena(final L2ItemInstance adena, final L2NpcInstance fromNpc, final L2Player player)
	{
		final SystemMessage smsg = new SystemMessage(SystemMessage.YOU_HAVE_OBTAINED_S1_ADENA);

		final int totalAdena = adena.getIntegerLimitedCount();

		final ArrayList<L2Player> _membersInRange = new ArrayList<L2Player>();

		if(adena.getCount() < _members.size())
			_membersInRange.add(player);
		else
			for(final L2Player p : _members)
				if(p.equals(player) || player.isInRange(p, Config.ALT_PARTY_DISTRIBUTION_RANGE) && !p.isDead())
					_membersInRange.add(p);

		final int amount = totalAdena / _membersInRange.size();
		final int ost = totalAdena % _membersInRange.size();

		for(final L2Player member : _membersInRange)
		{
			smsg.addNumber(member.equals(player) ? amount + ost : amount);

			final L2ItemInstance newAdena = ItemTable.getInstance().createItem(57);
			newAdena.setCount(member.equals(player) ? amount + ost : amount);

			final L2ItemInstance item2 = member.getInventory().addItem(newAdena);
			if(fromNpc == null)
				Log.LogItem(member, Log.GetItemInPaty, item2);
			else
				Log.LogItem(member, fromNpc, Log.GetItemInPaty, item2);

			member.sendPacket(smsg);
		}
	}

	/**
	 * Distribute Experience and SP rewards to L2Player Party members in the known area of the last attacker.<BR><BR>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Get the L2Player owner of the L2SummonInstance (if necessary) </li>
	 * <li>Calculate the Experience and SP reward distribution rate </li>
	 * <li>Add Experience and SP to the L2Player </li><BR><BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T GIVE rewards to L2PetInstance</B></FONT><BR><BR>
	 * 
	 * @param xpReward
	 *            The Experience reward to distribute
	 * @param spReward
	 *            The SP reward to distribute
	 * @param rewardedMembers
	 *            The list of L2Player to reward and LSummonInstance whose owner must be reward
	 * @param lastAttacker
	 *            The L2Character that has killed the L2NpcInstance
	 */
	public void distributeXpAndSp(final long xpReward, final long spReward, final ArrayList<L2Player> rewardedMembers, final L2Character lastAttacker, final L2NpcInstance target)
	{
		recalculatePartyData();

		final ArrayList<L2Player> mtr = new ArrayList<L2Player>();
		int minPartyLevel = -1;
		int maxPartyLevel = -1;
		double partyLvlSum = 0;

		// создаём список тех кто рядом
		for(final L2Player member : rewardedMembers)
		{
			if(!lastAttacker.isInRange(member, Config.ALT_PARTY_DISTRIBUTION_RANGE))
				continue;

			if(minPartyLevel == -1 || member.getLevel() < minPartyLevel)
				minPartyLevel = member.getLevel();

			if(maxPartyLevel == -1 || member.getLevel() > maxPartyLevel)
				maxPartyLevel = member.getLevel();

			partyLvlSum += member.getLevel();

			mtr.add(member);
		}

		if(maxPartyLevel - minPartyLevel > 20)
		{
			for(final L2Player member : rewardedMembers)
				if(member.getLevel() < maxPartyLevel - 20)
					mtr.remove(member);
			partyLvlSum = 0;
			for(final L2Player member : mtr)
				partyLvlSum += member.getLevel();
		}

		// бонус за пати
		final double bonus = bonuses[mtr.size()];

		// количество эксп и сп для раздачи на всех
		final double XP = xpReward * bonus;
		final double SP = spReward * bonus;

		for(final L2Player member : mtr)
		{
			final double lvlPenalty = Experience.penaltyModifier(target.calculateLevelDiffForDrop(member.getLevel()), 9);

			// отдаем его часть с учетом пенальти
			double memberXp = XP * lvlPenalty * member.getLevel() / partyLvlSum;
			double memberSp = SP * lvlPenalty * member.getLevel() / partyLvlSum;

			// больше чем соло не дадут
			if(memberXp > xpReward)
				memberXp = xpReward;
			if(memberSp > spReward)
				memberSp = spReward;

			if(memberXp > 0)
				memberXp *= Config.RATE_XP_PARTY * member.getRateExp();
			if(memberSp > 0)
				memberSp *= Config.RATE_SP_PARTY * member.getRateSp();

			member.addExpAndSp((long) memberXp, (long) memberSp, false, true);

			// Начисление душ камаэлянам
			final double neededExp = member.calcStat(Stats.SOULS_CONSUME_EXP, 0, null, null);
			if(neededExp > 0 && memberXp > neededExp)
				member.setConsumedSouls(member.getConsumedSouls() + 1, target);
		}

		recalculatePartyData();
	}

	public synchronized void recalculatePartyData()
	{
		_partySqLevelSum = 0;
		_partyLvl = 0;
		final float rateExp = 0;
		final float rateSp = 0;
		final float rateDrop = 0;
		final float rateAdena = 0;
		final float rateSpoil = 0;
		byte count = 0;
		for(final L2Player member : _members)
		{
			final int level = member.getLevel();
			_partySqLevelSum += level * level;
			_partyLvl = Math.max(_partyLvl, level);
			// rateExp += member.getBonus().RATE_XP;
			// rateSp += member.getBonus().RATE_SP;
			// rateDrop += member.getBonus().RATE_DROP_ITEMS;
			// rateAdena += member.getBonus().RATE_DROP_ADENA;
			// rateSpoil += member.getBonus().RATE_DROP_SPOIL;
			count++;
		}
		_rateExp = rateExp / count;
		_rateSp = rateSp / count;
		_rateDrop = rateDrop / count;
		_rateAdena = rateAdena / count;
		_rateSpoil = rateSpoil / count;
	}

	private static final double[] bonuses = { 1.00, 1.00, 1.30, 1.39, 1.50, 1.54, 1.58, 1.63, 1.67, 1.71 };

	public int getLevel()
	{
		return _partyLvl;
	}

	public int getLootDistribution()
	{
		return _itemDistribution;
	}

	public boolean isDistributeSpoilLoot()
	{
		boolean rv = false;

		if(_itemDistribution == ITEM_RANDOM_SPOIL || _itemDistribution == ITEM_ORDER_SPOIL)
			rv = true;

		return rv;
	}

	public boolean isInDimensionalRift()
	{
		return _dr != null;
	}

	public void setDimensionalRift(final DimensionalRift dr)
	{
		_dr = dr;
	}

	public DimensionalRift getDimensionalRift()
	{
		return _dr;
	}

	public boolean isInReflection()
	{
		return _reflection != null;
	}

	public void setReflection(final Reflection reflection)
	{
		_reflection = reflection;
	}

	public Reflection getReflection()
	{
		return _reflection;
	}

	public boolean isInCommandChannel()
	{
		return _commandChannel != null;
	}

	public L2CommandChannel getCommandChannel()
	{
		return _commandChannel;
	}

	public void setCommandChannel(final L2CommandChannel channel)
	{
		_commandChannel = channel;
	}

	/**
	 * Телепорт всей пати в одну точку (x,y,z)
	 */
	public void Teleport(final int x, final int y, final int z)
	{
		TeleportParty(getPartyMembers(), new Location(x, y, z));
	}

	/**
	 * Телепорт всей пати в одну точку dest
	 */
	public void Teleport(final Location dest)
	{
		TeleportParty(getPartyMembers(), dest);
	}

	/**
	 * Телепорт всей пати на территорию, игроки расставляются рандомно по территории
	 */
	public void Teleport(final L2Territory territory)
	{
		RandomTeleportParty(getPartyMembers(), territory);
	}

	/**
	 * Телепорт всей пати на территорию, лидер попадает в точку dest, а все остальные относительно лидера
	 */
	public void Teleport(final L2Territory territory, Location dest)
	{
		TeleportParty(getPartyMembers(), territory, dest);
	}

	public static void TeleportParty(final GArray<L2Player> gArray, final Location dest)
	{
		for(final L2Player _member : gArray)
		{
			if(_member == null)
				continue;
			_member.teleToLocation(dest);
		}
	}

	public static void TeleportParty(final GArray<L2Player> gArray, final L2Territory territory, final Location dest)
	{
		if(!territory.isInside(dest.x, dest.y))
		{
			Log.add("TeleportParty: dest is out of territory", "errors");
			Thread.dumpStack();
			return;
		}
		final int base_x = gArray.get(0).getX();
		final int base_y = gArray.get(0).getY();

		for(final L2Player _member : gArray)
		{
			if(_member == null)
				continue;
			int diff_x = _member.getX() - base_x;
			int diff_y = _member.getY() - base_y;
			final Location loc = new Location(dest.x + diff_x, dest.y + diff_y, dest.z);
			while(!territory.isInside(loc.x, loc.y))
			{
				diff_x = loc.x - dest.x;
				diff_y = loc.y - dest.y;
				if(diff_x != 0)
					loc.x -= diff_x / Math.abs(diff_x);
				if(diff_y != 0)
					loc.y -= diff_y / Math.abs(diff_y);
			}
			_member.teleToLocation(loc);
		}
	}

	public static void RandomTeleportParty(GArray<L2Player> gArray, final L2Territory territory)
	{
		for(final L2Player _member : gArray)
		{
			final int[] _loc = territory.getRandomPoint();
			if(_member == null || _loc == null)
				continue;
			_member.teleToLocation(_loc[0], _loc[1], _loc[2]);
		}
	}

	private class PositionBroadcast implements Runnable
	{
		@Override
		public void run()
		{
			if(_positionPacket == null)
				_positionPacket = new PartyMemberPosition(L2Party.this);
			else
				_positionPacket.reuse(L2Party.this);
			broadcastToPartyMembers(L2Party.this._positionPacket);
		}
	}
}