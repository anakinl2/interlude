package com.lineage.game.model;

import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import com.lineage.Config;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import com.lineage.db.mysql;
import com.lineage.ext.multilang.CustomMessage;
import com.lineage.game.cache.CrestCache;
import com.lineage.game.cache.Msg;
import com.lineage.game.instancemanager.CastleManager;
import com.lineage.game.model.entity.siege.Siege;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.model.instances.L2ItemInstance.ItemClass;
import com.lineage.game.serverpackets.L2GameServerPacket;
import com.lineage.game.serverpackets.PledgeReceiveSubPledgeCreated;
import com.lineage.game.serverpackets.PledgeShowInfoUpdate;
import com.lineage.game.serverpackets.PledgeShowMemberListAll;
import com.lineage.game.serverpackets.PledgeShowMemberListDeleteAll;
import com.lineage.game.serverpackets.PledgeSkillList;
import com.lineage.game.serverpackets.PledgeSkillListAdd;
import com.lineage.game.serverpackets.PledgeStatusChanged;
import com.lineage.game.serverpackets.SkillList;
import com.lineage.game.tables.ClanTable;
import com.lineage.game.tables.SkillTable;
import com.lineage.util.Log;

public class L2Clan
{
	private static final Logger _log = Logger.getLogger(L2Clan.class.getName());

	private String _name;
	private int _clanId;
	private L2ClanMember _leader = null;
	FastMap<Integer, L2ClanMember> _members = new FastMap<Integer, L2ClanMember>();

	private int _allyId;
	private byte _level;
	private int _hasCastle = 0;
	private int _hasFortress = 0;
	private int _hiredGuards;
	private int _hasHideout = 0;
	private int _crestId;
	private int _crestLargeId;

	private long _expelledMemberTime;
	private long _leavedAllyTime;
	private long _dissolvedAllyTime;

	// all these in milliseconds
	public static long EXPELLED_MEMBER_PENALTY = 1 * 60 * 60 * 1000;
	public static long LEAVED_ALLY_PENALTY = 12 * 60 * 60 * 1000;
	public static long DISSOLVED_ALLY_PENALTY = 2 * 24 * 60 * 60 * 1000;

	private ClanWarehouse _warehouse = new ClanWarehouse(this);
	private int _whBonus = -1;

	private List<L2Clan> _atWarWith = new FastList<L2Clan>();
	private List<L2Clan> _underAttackFrom = new FastList<L2Clan>();

	protected FastMap<Integer, L2Skill> _skills = new FastMap<Integer, L2Skill>();
	protected FastMap<Integer, RankPrivs> _Privs = new FastMap<Integer, RankPrivs>();
	protected FastMap<Integer, SubPledge> _SubPledges = new FastMap<Integer, SubPledge>();

	private int _reputation = 0;

	// Clan Privileges
	public static final int CP_NOTHING = 0;
	public static final int CP_CL_JOIN_CLAN = 2; // Join clan
	public static final int CP_CL_GIVE_TITLE = 4; // Give a title
	public static final int CP_CL_VIEW_WAREHOUSE = 8; // View warehouse content
	public static final int CP_CL_MANAGE_RANKS = 16; // manage clan ranks
	public static final int CP_CL_PLEDGE_WAR = 32;
	public static final int CP_CL_DISMISS = 64;
	public static final int CP_CL_REGISTER_CREST = 128; // Register clan crest
	public static final int CP_CL_MASTER_RIGHTS = 256;
	public static final int CP_CL_MANAGE_LEVELS = 512;
	public static final int CP_CH_OPEN_DOOR = 1024; // open a door
	public static final int CP_CH_OTHER_RIGHTS = 2048; // ??
	public static final int CP_CH_AUCTION = 4096;
	public static final int CP_CH_DISMISS = 8192; // Выгнать чужаков из КХ
	public static final int CP_CH_SET_FUNCTIONS = 16384;
	public static final int CP_CS_OPEN_DOOR = 32768;
	public static final int CP_CS_MANOR_ADMIN = 65536; // ???
	public static final int CP_CS_MANAGE_SIEGE = 131072;
	public static final int CP_CS_USE_FUNCTIONS = 262144;
	public static final int CP_CS_DISMISS = 524288; // Выгнать чужаков из замка
	public static final int CP_CS_TAXES = 1048576;
	public static final int CP_CS_MERCENARIES = 2097152;
	public static final int CP_CS_SET_FUNCTIONS = 4194304;
	public static final int CP_ALL = 8388606;

	// Sub-unit types
	public static final int SUBUNIT_ACADEMY = -1;
	public static final int SUBUNIT_NONE = 0;
	public static final int SUBUNIT_ROYAL1 = 100;
	public static final int SUBUNIT_ROYAL2 = 200;
	public static final int SUBUNIT_KNIGHT1 = 1001;
	public static final int SUBUNIT_KNIGHT2 = 1002;
	public static final int SUBUNIT_KNIGHT3 = 2001;
	public static final int SUBUNIT_KNIGHT4 = 2002;

	private final static ClanReputationComparator REPUTATION_COMPARATOR = new ClanReputationComparator();
	/** Количество мест в таблице рангов кланов */
	private final static int REPUTATION_PLACES = 100;

	private L2ClanGate _clanGate = null; // врата клансуммона

	private String _notice;

	@SuppressWarnings("unused")
	private boolean _noticeEnabled = true;

	/**
	 * Конструктор используется только внутри для восстановления из базы
	 */
	private L2Clan(final int clanId)
	{
		_clanId = clanId;
		InitializePrivs();
	}

	public L2Clan(final int clanId, final String clanName, final L2ClanMember leader)
	{
		_clanId = clanId;
		_name = clanName;
		InitializePrivs();
		setLeader(leader);
		insertNotice();
	}

	public int getClanId()
	{
		return _clanId;
	}

	public void setClanId(final int clanId)
	{
		_clanId = clanId;
	}

	public int getLeaderId()
	{
		return _leader != null ? _leader.getObjectId() : 0;
	}

	public L2ClanMember getLeader()
	{
		return _leader;
	}

	public void setLeader(final L2ClanMember leader)
	{
		_leader = leader;
		_members.put(leader.getObjectId(), leader);
	}

	public String getLeaderName()
	{
		return _leader.getName();
	}

	public String getName()
	{
		return _name;
	}

	public void setName(final String name)
	{
		_name = name;
	}

	private void addClanMember(final L2ClanMember member)
	{
		_members.put(member.getObjectId(), member);
	}

	public void addClanMember(final L2Player player)
	{
		addClanMember(new L2ClanMember(this, player.getName(), player.getTitle(), player.getLevel(), player.getClassId().getId(), player.getObjectId(), player.getPledgeType(), player.getPowerGrade(), player.getApprentice(), false));
	}

	public L2ClanMember getClanMember(final int id)
	{
		return _members.get(id);
	}

	public L2ClanMember getClanMember(final String name)
	{
		for(final L2ClanMember member : _members.values())
			if(member.getName().equals(name))
				return member;
		return null;
	}

	public int getMembersCount()
	{
		return _members.size();
	}

	public void flush()
	{
		for(final L2ClanMember member : getMembers())
			removeClanMember(member.getObjectId());
		for(final L2ItemInstance item : _warehouse.listItems(ItemClass.ALL))
			_warehouse.destroyItem(item.getItemId(), item.getIntegerLimitedCount());
		if(_hasCastle != 0)
			CastleManager.getInstance().getCastleByIndex(_hasCastle).changeOwner(null);
	}

	public void removeClanMember(final int id)
	{
		if(id == getLeaderId())
			return;
		final L2ClanMember exMember = _members.remove(id);
		if(exMember == null)
			return;
		final SubPledge sp = _SubPledges.get(exMember.getPledgeType());
		if(sp != null && sp.getLeaderId() == exMember.getObjectId()) // subpledge leader
			sp.setLeaderId(0); // clan leader has to assign another one, via villagemaster
		if(exMember.hasSponsor())
			getClanMember(exMember.getSponsor()).setApprentice(0);
		removeMemberInDatabase(exMember);
	}

	public L2ClanMember[] getMembers()
	{
		return _members.values().toArray(new L2ClanMember[_members.size()]);
	}

	public L2Player[] getOnlineMembers(final int exclude)
	{
		final List<L2Player> result = new FastList<L2Player>();
		for(final L2ClanMember temp : _members.values())
			if(temp.isOnline() && temp.getObjectId() != exclude)
				result.add(temp.getPlayer());

		return result.toArray(new L2Player[result.size()]);

	}

	public int getAllyId()
	{
		return _allyId;
	}

	public byte getLevel()
	{
		return _level;
	}

	/**
	 * Возвращает замок, которым владеет клан
	 * 
	 * @return ID замка
	 */
	public int getHasCastle()
	{
		return _hasCastle;
	}

	/**
	 * Возвращает кланхолл, которым владеет клан
	 * 
	 * @return ID кланхолла
	 */
	public int getHasHideout()
	{
		return _hasHideout;
	}

	public void setAllyId(final int allyId)
	{
		_allyId = allyId;
	}

	/**
	 * Устанавливает замок, которым владеет клан.<BR>
	 * Одновременно владеть и замком и крепостью нельзя
	 * 
	 * @param castle
	 *            ID замка
	 */
	public void setHasCastle(final int castle)
	{
		if(_hasFortress == 0)
			_hasCastle = castle;
	}

	/**
	 * Устанавливает крепость, которой владеет клан.<BR>
	 * Одновременно владеть и крепостью и замком нельзя
	 * 
	 * @param fortress
	 *            ID крепости
	 */
	public void setHasFortress(final int fortress)
	{
		if(_hasCastle == 0)
			_hasFortress = fortress;
	}

	public void setHasHideout(final int hasHideout)
	{
		_hasHideout = hasHideout;
	}

	public void setLevel(final byte level)
	{
		_level = level;
	}

	public boolean isMember(final Integer id)
	{
		return _members.containsKey(id);
	}

	public void updateClanInDB()
	{
		if(getLeaderId() == 0)
		{
			_log.warning("updateClanInDB with empty LeaderId");
			Thread.dumpStack();
			return;
		}

		if(getClanId() == 0)
		{
			_log.warning("updateClanInDB with empty ClanId");
			Thread.dumpStack();
			return;
		}
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clan_data SET leader_id=?,ally_id=?,reputation_score=?,expelled_member=?,leaved_ally=?,dissolved_ally=?,clan_level=?,warehouse=? WHERE clan_id=?");
			statement.setInt(1, getLeaderId());
			statement.setInt(2, getAllyId());
			statement.setInt(3, getReputationScore());
			statement.setLong(4, getExpelledMemberTime() / 1000);
			statement.setLong(5, getLeavedAllyTime() / 1000);
			statement.setLong(6, getDissolvedAllyTime() / 1000);
			statement.setInt(7, _level);
			statement.setInt(8, getWhBonus());
			statement.setInt(9, getClanId());
			statement.execute();

			if(Config.DEBUG)
				_log.fine("Clan data saved in db: " + getClanId());
		}
		catch(final Exception e)
		{
			_log.warning("error while updating clan '" + getClanId() + "' data in db: " + e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public void store()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO clan_data (clan_id,clan_name,clan_level,hasCastle,hasFortress,hasHideout,ally_id,leader_id,expelled_member,leaved_ally,dissolved_ally) values (?,?,?,?,?,?,?,?,?,?,?)");
			statement.setInt(1, _clanId);
			statement.setString(2, _name);
			statement.setInt(3, _level);
			statement.setInt(4, _hasCastle);
			statement.setInt(5, _hasFortress);
			statement.setInt(6, _hasHideout);
			statement.setInt(7, _allyId);
			statement.setInt(8, getLeaderId());
			statement.setLong(9, getExpelledMemberTime() / 1000);
			statement.setLong(10, getLeavedAllyTime() / 1000);
			statement.setLong(11, getDissolvedAllyTime() / 1000);
			statement.execute();
			DatabaseUtils.closeStatement(statement);

			statement = con.prepareStatement("UPDATE characters SET clanid=?,pledge_type=0 WHERE obj_Id=?");
			statement.setInt(1, getClanId());
			statement.setInt(2, getLeaderId());
			statement.execute();

			if(Config.DEBUG)
				_log.fine("New clan saved in db: " + getClanId());
		}
		catch(final Exception e)
		{
			_log.warning("error while saving new clan to db " + e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	private void removeMemberInDatabase(final L2ClanMember member)
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE characters SET clanid=0, pledge_type=0, pledge_rank=0, lvl_joined_academy=0, apprentice=0, title='', leaveclan=? WHERE obj_Id=?");
			statement.setLong(1, System.currentTimeMillis() / 1000);
			statement.setInt(2, member.getObjectId());
			statement.execute();

			if(Config.DEBUG)
				_log.fine("clan member removed in db: " + getClanId());
		}
		catch(final Exception e)
		{
			_log.warning("error while removing clan member in db " + e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public static L2Clan restore(final int clanId)
	{
		if(clanId == 0) // no clan
			return null;

		L2Clan clan = null;
		int leaderId = 0;

		ThreadConnection con1 = null;
		FiltredPreparedStatement statement1 = null;
		ResultSet clanData = null;

		try
		{
			con1 = L2DatabaseFactory.getInstance().getConnection();
			statement1 = con1.prepareStatement("SELECT * FROM clan_data where clan_id=?");
			statement1.setInt(1, clanId);
			clanData = statement1.executeQuery();

			if(clanData.next())
			{
				clan = new L2Clan(clanId);
				clan.setName(clanData.getString("clan_name"));
				clan.setLevel(clanData.getByte("clan_level"));
				clan.setHasCastle(clanData.getByte("hasCastle"));
				clan.setHasFortress(clanData.getByte("hasFortress"));
				clan.setHasHideout(clanData.getInt("hasHideout"));
				clan.setAllyId(clanData.getInt("ally_id"));
				clan._reputation = clanData.getInt("reputation_score");
				clan._auctionBiddedAt = clanData.getInt("auction_bid_at");
				clan.setExpelledMemberTime(clanData.getLong("expelled_member") * 1000);
				clan.setLeavedAllyTime(clanData.getLong("leaved_ally") * 1000);
				clan.setDissolvedAllyTime(clanData.getLong("dissolved_ally") * 1000);
				clan.setWhBonus(clanData.getInt("warehouse"));

				leaderId = clanData.getInt("leader_id");
			}
			else
			{
				_log.warning("L2Clan.java clan " + clanId + " does't exist");
				return null;
			}

			if(clan.getName() == null)
				_log.config("null name for clan?? " + clanId);
		}
		catch(final Exception e)
		{
			_log.warning("error while restoring clan " + e);
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con1, statement1, clanData);
		}

		if(clan == null)
		{
			_log.fine("Clan " + clanId + " does't exist");
			return null;
		}

		if(leaderId == 0)
		{
			_log.fine("Not found leader for clan: " + clanId);
			return null;
		}

		ThreadConnection con2 = null;
		FiltredPreparedStatement statement2 = null;
		ResultSet clanMembers = null;

		try
		{
			con2 = L2DatabaseFactory.getInstance().getConnection();
			statement2 = con2.prepareStatement(//
			"SELECT `c`.`char_name` AS `char_name`," + //
			"`s`.`level` AS `level`," + //
			"`s`.`class_id` AS `classid`," + //
			"`c`.`obj_Id` AS `obj_id`," + //
			"`c`.`title` AS `title`," + //
			"`c`.`pledge_type` AS `pledge_type`," + //
			"`c`.`pledge_rank` AS `pledge_rank`," + //
			"`c`.`apprentice` AS `apprentice` " + //
			"FROM `characters` `c` " + //
			"LEFT JOIN `character_subclasses` `s` ON (`s`.`char_obj_id` = `c`.`obj_Id` AND `s`.`isBase` = '1') " + //
			"WHERE `c`.`clanid`=? ORDER BY `c`.`lastaccess` DESC");

			statement2.setInt(1, clanId);
			clanData = statement2.executeQuery();

			statement2.setInt(1, clan.getClanId());
			clanMembers = statement2.executeQuery();

			while(clanMembers.next())
			{
				final L2ClanMember member = new L2ClanMember(clan, clanMembers.getString("char_name"), clanMembers.getString("title"), clanMembers.getInt("level"), clanMembers.getInt("classid"), clanMembers.getInt("obj_id"), clanMembers.getInt("pledge_type"), clanMembers.getInt("pledge_rank"), clanMembers.getInt("apprentice"), clanMembers.getInt("obj_id") == leaderId);
				if(member.getObjectId() == leaderId)
					clan.setLeader(member);
				else
					clan.addClanMember(member);
			}

			if(clan.getLeader() == null)
				_log.severe("Clan " + clan.getName() + " have no leader!");
		}
		catch(final Exception e)
		{
			_log.warning("Error while restoring clan members for clan: " + clanId + " " + e);
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con2, statement2, clanMembers);
		}

		clan.restoreSkills();
		clan.restoreSubPledges();
		clan.restoreRankPrivs();
		clan.setCrestId(CrestCache.getPledgeCrestId(clanId));
		clan.setCrestLargeId(CrestCache.getPledgeCrestLargeId(clanId));

		return clan;
	}

	public void broadcastToOnlineMembers(final L2GameServerPacket packet)
	{
		for(final L2ClanMember member : _members.values())
			if(member.isOnline())
				member.getPlayer().sendPacket(packet);
	}

	public void broadcastToOtherOnlineMembers(final L2GameServerPacket packet, final L2Player player)
	{
		for(final L2ClanMember member : _members.values())
			if(member.isOnline() && member.getPlayer() != player)
				member.getPlayer().sendPacket(packet);
	}

	@Override
	public String toString()
	{
		return getName();
	}

	public void setCrestId(final int newcrest)
	{
		_crestId = newcrest;
	}

	public int getCrestId()
	{
		return _crestId;
	}

	public boolean hasCrest()
	{
		return _crestId > 0;
	}

	public int getCrestLargeId()
	{
		return _crestLargeId;
	}

	public void setCrestLargeId(final int newcrest)
	{
		_crestLargeId = newcrest;
	}

	public boolean hasCrestLarge()
	{
		return _crestLargeId > 0;
	}

	/**
	 * Возвращает инстанс адены в КВХ
	 * 
	 * @return
	 */
	public L2ItemInstance getAdena()
	{
		return _warehouse.findItemId(57);
	}

	public ClanWarehouse getWarehouse()
	{
		return _warehouse;
	}

	public int getHiredGuards()
	{
		return _hiredGuards;
	}

	public void incrementHiredGuards()
	{
		_hiredGuards++;
	}

	public int isAtWar()
	{
		if(_atWarWith != null && _atWarWith.size() > 0)
			return 1;
		return 0;
	}

	public int isAtWarOrUnderAttack()
	{
		if(_atWarWith != null && _atWarWith.size() > 0 || _underAttackFrom != null && _underAttackFrom.size() > 0)
			return 1;
		return 0;
	}

	public boolean isAtWarWith(final Integer id)
	{
		final L2Clan clan = ClanTable.getInstance().getClan(id);
		if(_atWarWith != null && _atWarWith.size() > 0)
			if(_atWarWith.contains(clan))
				return true;
		return false;
	}

	public boolean isUnderAttackFrom(final Integer id)
	{
		final L2Clan clan = ClanTable.getInstance().getClan(id);
		if(_underAttackFrom != null && _underAttackFrom.size() > 0)
			if(_underAttackFrom.contains(clan))
				return true;
		return false;
	}

	public void setEnemyClan(final L2Clan clan)
	{
		_atWarWith.add(clan);
	}

	public void deleteEnemyClan(final L2Clan clan)
	{
		_atWarWith.remove(clan);
	}

	// clans that are attacking this clan
	public void setAttackerClan(final L2Clan clan)
	{
		_underAttackFrom.add(clan);
	}

	public void deleteAttackerClan(final L2Clan clan)
	{
		_underAttackFrom.remove(clan);
	}

	public List<L2Clan> getEnemyClans()
	{
		return _atWarWith;
	}

	public int getWarsCount()
	{
		return _atWarWith.size();
	}

	public List<L2Clan> getAttackerClans()
	{
		return _underAttackFrom;
	}

	public void broadcastClanStatus(final boolean updateList, final boolean needUserInfo)
	{
		for(final L2ClanMember member : _members.values())
			if(member.isOnline())
			{
				if(updateList)
				{
					member.getPlayer().sendPacket(new PledgeShowMemberListDeleteAll());
					member.getPlayer().sendPacket(new PledgeShowMemberListAll(this, member.getPlayer()));
				}
				member.getPlayer().sendPacket(new PledgeShowInfoUpdate(this));
				if(needUserInfo)
					member.getPlayer().broadcastUserInfo(true);
			}
	}

	public L2Alliance getAlliance()
	{
		return _allyId == 0 ? null : ClanTable.getInstance().getAlliance(_allyId);
	}

	public void setExpelledMemberTime(final long time)
	{
		_expelledMemberTime = time;
	}

	public long getExpelledMemberTime()
	{
		return _expelledMemberTime;
	}

	public void setExpelledMember()
	{
		_expelledMemberTime = System.currentTimeMillis();
		updateClanInDB();
	}

	public void setLeavedAllyTime(final long time)
	{
		_leavedAllyTime = time;
	}

	public long getLeavedAllyTime()
	{
		return _leavedAllyTime;
	}

	public void setLeavedAlly()
	{
		_leavedAllyTime = System.currentTimeMillis();
		updateClanInDB();
	}

	public void setDissolvedAllyTime(final long time)
	{
		_dissolvedAllyTime = time;
	}

	public long getDissolvedAllyTime()
	{
		return _dissolvedAllyTime;
	}

	public void setDissolvedAlly()
	{
		_dissolvedAllyTime = System.currentTimeMillis();
		updateClanInDB();
	}

	public boolean canInvite()
	{
		return System.currentTimeMillis() - _expelledMemberTime >= EXPELLED_MEMBER_PENALTY;
	}

	public boolean canJoinAlly()
	{
		return System.currentTimeMillis() - _leavedAllyTime >= LEAVED_ALLY_PENALTY;
	}

	public boolean canCreateAlly()
	{
		return System.currentTimeMillis() - _dissolvedAllyTime >= DISSOLVED_ALLY_PENALTY;
	}

	public int getRank()
	{
		final L2Clan[] clans = ClanTable.getInstance().getClans();
		Arrays.sort(clans, REPUTATION_COMPARATOR);

		final int place = 1;
		for(int i = 0; i < clans.length; i++)
		{
			if(i == REPUTATION_PLACES)
				return 0;

			final L2Clan clan = clans[i];
			if(clan == this)
				return place + i;
		}

		return 0;
	}

	public int getReputationScore()
	{
		return _reputation;
	}

	public void setReputationScore(final int rep)
	{
		if(_reputation >= 0 && rep < 0)
		{
			broadcastToOnlineMembers(Msg.SINCE_THE_CLAN_REPUTATION_SCORE_HAS_DROPPED_TO_0_OR_LOWER_YOUR_CLAN_SKILLS_WILL_BE_DE_ACTIVATED);
			final L2Skill[] skills = getAllSkills();
			for(final L2ClanMember member : _members.values())
				if(member.isOnline())
					for(final L2Skill sk : skills)
						member.getPlayer().removeSkill(sk, false);
		}
		else if(_reputation < 0 && rep >= 0)
		{
			broadcastToOnlineMembers(Msg.THE_CLAN_SKILL_WILL_BE_ACTIVATED_BECAUSE_THE_CLANS_REPUTATION_SCORE_HAS_REACHED_TO_0_OR_HIGHER);
			final L2Skill[] skills = getAllSkills();
			for(final L2ClanMember member : _members.values())
				if(member.isOnline())
					for(final L2Skill sk : skills)
					{
						member.getPlayer().sendPacket(new PledgeSkillListAdd(sk.getId(), sk.getLevel()));
						if(sk.getMinPledgeClass() <= member.getPlayer().getPledgeClass())
							member.getPlayer().addSkill(sk, false);
					}
		}

		_reputation = rep;

		updateClanInDB();
	}

	public int incReputation(int inc, final boolean Rate, final String source)
	{
		if(_level < 5)
			return 0;

		if(Rate && Math.abs(inc) <= Config.RATE_CLAN_REP_SCORE_MAX_AFFECTED)
			inc = Math.round(inc * Config.RATE_CLAN_REP_SCORE);

		setReputationScore(_reputation + inc);
		Log.add(_name + "|" + inc + "|" + _reputation + "|" + source, "clan_reputation");

		return inc;
	}

	/* ============================ clan skills stuff ============================ */

	private void restoreSkills()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			// Retrieve all skills of this L2Player from the database
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT skill_id,skill_level FROM clan_skills WHERE clan_id=?");
			statement.setInt(1, getClanId());
			rset = statement.executeQuery();

			// Go though the recordset of this SQL query
			while(rset.next())
			{
				final int id = rset.getInt("skill_id");
				final int level = rset.getInt("skill_level");
				// Create a L2Skill object for each record
				final L2Skill skill = SkillTable.getInstance().getInfo(id, level);
				// Add the L2Skill object to the L2Clan _skills
				_skills.put(skill.getId(), skill);
			}
		}
		catch(final Exception e)
		{
			_log.warning("Could not restore clan skills: " + e);
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}
	}

	/** used to retrieve all skills */
	public final L2Skill[] getAllSkills()
	{
		if(_reputation < 0)
			return new L2Skill[0];

		return _skills.values().toArray(new L2Skill[_skills.values().size()]);
	}

	/** used to add a new skill to the list, send a packet to all online clan members, update their stats and store it in db */
	public L2Skill addNewSkill(final L2Skill newSkill, final boolean store)
	{
		L2Skill oldSkill = null;
		if(newSkill != null)
		{
			// Replace oldSkill by newSkill or Add the newSkill
			oldSkill = _skills.put(newSkill.getId(), newSkill);

			if(store)
			{
				ThreadConnection con = null;
				FiltredPreparedStatement statement = null;

				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();

					if(oldSkill != null)
					{
						statement = con.prepareStatement("UPDATE clan_skills SET skill_level=? WHERE skill_id=? AND clan_id=?");
						statement.setInt(1, newSkill.getLevel());
						statement.setInt(2, oldSkill.getId());
						statement.setInt(3, getClanId());
						statement.execute();
					}
					else
					{
						statement = con.prepareStatement("INSERT INTO clan_skills (clan_id,skill_id,skill_level,skill_name) VALUES (?,?,?,?)");
						statement.setInt(1, getClanId());
						statement.setInt(2, newSkill.getId());
						statement.setInt(3, newSkill.getLevel());
						statement.setString(4, newSkill.getName());
						statement.execute();
					}
				}
				catch(final Exception e)
				{
					_log.warning("Error could not store char skills: " + e);
				}
				finally
				{
					DatabaseUtils.closeDatabaseCS(con, statement);
				}
			}

			for(final L2ClanMember temp : _members.values())
				if(temp.isOnline() && temp.getPlayer() != null)
				{
					temp.getPlayer().sendPacket(new PledgeSkillListAdd(newSkill.getId(), newSkill.getLevel()));
					if(newSkill.getMinRank() <= temp.getPlayer().getPledgeClass())
						temp.getPlayer().addSkill(newSkill, false);
				}
		}

		return oldSkill;
	}

	/**
	 * Удаляет скилл у клана, без удаления из базы. Используется для удаления скилов резиденций.
	 * После удаления скила(ов) необходимо разослать boarcastSkillListToOnlineMembers()
	 * 
	 * @param skill
	 */
	public void removeSkill(final L2Skill skill)
	{
		_skills.remove(skill.getId());
		for(final L2ClanMember temp : _members.values())
			if(temp.isOnline() && temp.getPlayer() != null)
				temp.getPlayer().removeSkill(skill);
	}

	public void boarcastSkillListToOnlineMembers()
	{
		for(final L2ClanMember temp : _members.values())
			if(temp.isOnline() && temp.getPlayer() != null)
				addAndShowSkillsToPlayer(temp.getPlayer());
	}

	public void addAndShowSkillsToPlayer(final L2Player activeChar)
	{
		if(_reputation < 0)
			return;

		activeChar.sendPacket(new PledgeSkillList(this));

		for(final L2Skill s : _skills.values())
		{
			if(s == null)
				continue;
			activeChar.sendPacket(new PledgeSkillListAdd(s.getId(), s.getLevel()));
			if(s.getMinRank() <= activeChar.getPledgeClass())
				activeChar.addSkill(s, false);
		}
		activeChar.sendPacket(new SkillList(activeChar));
	}

	/* ============================ clan subpledges stuff ============================ */

	public class SubPledge
	{
		private int _type;
		private int _leaderId;
		private String _name;

		public SubPledge(final int type, final int leaderId, final String name)
		{
			_type = type;
			_leaderId = leaderId;
			_name = name;
		}

		public int getType()
		{
			return _type;
		}

		public String getName()
		{
			return _name;
		}

		public int getLeaderId()
		{
			return _leaderId;
		}

		public void setLeaderId(final int leaderId)
		{
			_leaderId = leaderId;
			ThreadConnection con = null;
			FiltredPreparedStatement statement = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("UPDATE clan_subpledges SET leader_id=? WHERE clan_id=? and type=?");
				statement.setInt(1, _leaderId);
				statement.setInt(2, getClanId());
				statement.setInt(3, _type);
				statement.execute();
			}
			catch(final Exception e)
			{}
			finally
			{
				DatabaseUtils.closeDatabaseCS(con, statement);
			}
		}

		public String getLeaderName()
		{
			for(final L2ClanMember member : _members.values())
				if(member.getObjectId() == _leaderId)
					return member.getName();
			return "";
		}
	}

	public final boolean isAcademy(final int pledgeType)
	{
		return pledgeType == SUBUNIT_ACADEMY;
	}

	public final boolean isRoyalGuard(final int pledgeType)
	{
		return pledgeType == SUBUNIT_ROYAL1 || pledgeType == SUBUNIT_ROYAL2;
	}

	public final boolean isOrderOfKnights(final int pledgeType)
	{
		return pledgeType == SUBUNIT_KNIGHT1 || pledgeType == SUBUNIT_KNIGHT2 || pledgeType == SUBUNIT_KNIGHT3 || pledgeType == SUBUNIT_KNIGHT4;
	}

	public int getAffiliationRank(final int pledgeType)
	{
		if(isAcademy(pledgeType))
			return 9;
		else if(isOrderOfKnights(pledgeType))
			return 8;
		else if(isRoyalGuard(pledgeType))
			return 7;
		else
			return 6;
	}

	public final SubPledge getSubPledge(final int pledgeType)
	{
		if(_SubPledges == null)
			return null;

		return _SubPledges.get(pledgeType);
	}

	public final void addSubPledge(final SubPledge sp, final boolean updateDb)
	{
		_SubPledges.put(sp.getType(), sp);

		if(updateDb)
		{
			broadcastToOnlineMembers(new PledgeReceiveSubPledgeCreated(sp));
			ThreadConnection con = null;
			FiltredPreparedStatement statement = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("INSERT INTO `clan_subpledges` (clan_id,type,leader_id,name) VALUES (?,?,?,?)");
				statement.setInt(1, getClanId());
				statement.setInt(2, sp.getType());
				statement.setInt(3, sp.getLeaderId());
				statement.setString(4, sp.getName());
				statement.execute();
			}
			catch(final Exception e)
			{
				_log.warning("Could not store clan Sub pledges: " + e);
				e.printStackTrace();
			}
			finally
			{
				DatabaseUtils.closeDatabaseCS(con, statement);
			}
		}
	}

	public int createSubPledge(final L2Player player, int pledgeType, final int leaderId, final String name)
	{
		final int temp = pledgeType;
		pledgeType = getAvailablePledgeTypes(pledgeType);

		if(pledgeType == SUBUNIT_NONE)
		{
			if(temp == SUBUNIT_ACADEMY)
				player.sendPacket(Msg.YOUR_CLAN_HAS_ALREADY_ESTABLISHED_A_CLAN_ACADEMY);
			else
				player.sendMessage("You can't create any more sub-units of this type");
			return SUBUNIT_NONE;
		}

		switch(pledgeType)
		{
			case SUBUNIT_ACADEMY:
				break;
			case SUBUNIT_ROYAL1:
			case SUBUNIT_ROYAL2:
				if(getReputationScore() < 5000)
				{
					player.sendPacket(Msg.THE_CLAN_REPUTATION_SCORE_IS_TOO_LOW);
					return SUBUNIT_NONE;
				}
				incReputation(-5000, false, "SubunitCreate");
				broadcastToOnlineMembers(new PledgeShowInfoUpdate(this));
				broadcastToOnlineMembers(new PledgeStatusChanged(this));
				break;
			case SUBUNIT_KNIGHT1:
			case SUBUNIT_KNIGHT2:
			case SUBUNIT_KNIGHT3:
			case SUBUNIT_KNIGHT4:
				if(getReputationScore() < 10000)
				{
					player.sendPacket(Msg.THE_CLAN_REPUTATION_SCORE_IS_TOO_LOW);
					return SUBUNIT_NONE;
				}
				incReputation(-10000, false, "SubunitCreate");
				broadcastToOnlineMembers(new PledgeShowInfoUpdate(this));
				broadcastToOnlineMembers(new PledgeStatusChanged(this));
				break;
		}

		addSubPledge(new SubPledge(pledgeType, leaderId, name), true);
		return pledgeType;
	}

	public int getAvailablePledgeTypes(int pledgeType)
	{
		if(pledgeType == SUBUNIT_NONE)
			return SUBUNIT_NONE;

		if(_SubPledges.get(pledgeType) != null)
			switch(pledgeType)
			{
				case SUBUNIT_ACADEMY:
					return 0;
				case SUBUNIT_ROYAL1:
					pledgeType = getAvailablePledgeTypes(SUBUNIT_ROYAL2);
					break;
				case SUBUNIT_ROYAL2:
					return 0;
				case SUBUNIT_KNIGHT1:
					pledgeType = getAvailablePledgeTypes(SUBUNIT_KNIGHT2);
					break;
				case SUBUNIT_KNIGHT2:
					pledgeType = getAvailablePledgeTypes(SUBUNIT_KNIGHT3);
					break;
				case SUBUNIT_KNIGHT3:
					pledgeType = getAvailablePledgeTypes(SUBUNIT_KNIGHT4);
					break;
				case SUBUNIT_KNIGHT4:
					return 0;
			}
		return pledgeType;
	}

	private void restoreSubPledges()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM clan_subpledges WHERE clan_id=?");
			statement.setInt(1, getClanId());
			rset = statement.executeQuery();

			// Go though the recordset of this SQL query
			while(rset.next())
			{
				final int type = rset.getInt("type");
				final int leaderId = rset.getInt("leader_id");
				final String name = rset.getString("name");
				final SubPledge pledge = new SubPledge(type, leaderId, name);
				addSubPledge(pledge, false);
			}
		}
		catch(final Exception e)
		{
			_log.warning("Could not restore clan SubPledges: " + e);
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}
	}

	/** used to retrieve all subPledges */
	public final SubPledge[] getAllSubPledges()
	{
		return _SubPledges.values().toArray(new SubPledge[_SubPledges.values().size()]);
	}

	public int getSubPledgeLimit(final int pledgeType)
	{
		int limit;
		switch(_level)
		{
			case 0:
				limit = 10;
				break;
			case 1:
				limit = 15;
				break;
			case 2:
				limit = 20;
				break;
			case 3:
				limit = 30;
				break;
			default:
				limit = 40;
				break;
		}
		switch(pledgeType)
		{
			case SUBUNIT_ACADEMY:
			case SUBUNIT_ROYAL1:
			case SUBUNIT_ROYAL2:
				limit = 20;
				break;
			case SUBUNIT_KNIGHT1:
			case SUBUNIT_KNIGHT2:
			case SUBUNIT_KNIGHT3:
			case SUBUNIT_KNIGHT4:
				limit = getLevel() >= 9 ? 25 : 10;
				break;
		}
		return limit;
	}

	public int getSubPledgeMembersCount(final int pledgeType)
	{
		int result = 0;
		for(final L2ClanMember temp : _members.values())
			if(temp.getPledgeType() == pledgeType)
				result++;
		return result;
	}

	public int getSubPledgeLeaderId(final int pledgeType)
	{
		return _SubPledges.get(pledgeType).getLeaderId();
	}

	/* ============================ clan privilege ranks stuff ============================ */

	public class RankPrivs
	{
		private int _rank;
		private int _party;
		private int _privs;

		public RankPrivs(final int rank, final int party, final int privs)
		{
			_rank = rank;
			_party = party;
			_privs = privs;
		}

		public int getRank()
		{
			return _rank;
		}

		public int getParty()
		{
			return _party;
		}

		public void setParty(final int party)
		{
			_party = party;
		}

		public int getPrivs()
		{
			return _privs;
		}

		public void incParty()
		{
			_party++;
		}

		public void decParty()
		{
			_party--;
			if(_party < 0)
				_party = 0;
		}

		public void setPrivs(final int privs)
		{
			_privs = privs;
		}
	}

	private void restoreRankPrivs()
	{
		if(_Privs == null)
			InitializePrivs();
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			// Retrieve all skills of this L2Player from the database
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT privilleges,rank,party FROM clan_privs WHERE clan_id=?");
			statement.setInt(1, getClanId());
			rset = statement.executeQuery();

			// Go though the recordset of this SQL query
			while(rset.next())
			{
				final int rank = rset.getInt("rank");
				// int party = rset.getInt("party"); - unused?
				final int privileges = rset.getInt("privilleges");
				// noinspection ConstantConditions
				final RankPrivs p = _Privs.get(rank);
				if(p != null)
					p.setPrivs(privileges);
				else
					_log.warning("Invalid rank value (" + rank + "), please check clan_privs table");
			}
		}
		catch(final Exception e)
		{
			_log.warning("Could not restore clan privs by rank: " + e);
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}
	}

	public void InitializePrivs()
	{
		for(int i = 1; i < 10; i++)
			_Privs.put(i, new RankPrivs(i, 0, CP_NOTHING));
	}

	public void updatePrivsForRank(final int rank)
	{
		for(final L2ClanMember member : _members.values())
			if(member.isOnline() && member.getPlayer() != null && member.getPlayer().getPowerGrade() == rank)
			{
				if(member.getPlayer().isClanLeader())
					continue;
				member.getPlayer().sendUserInfo(false);
			}
	}

	public RankPrivs getRankPrivs(final int rank)
	{
		/*
		 * int priv = 0;
		 * for(RankPrivs rp : _Privs.values())
		 * if(rp._rank <= rank)
		 * priv |= rank;
		 * return priv;
		 */
		if(rank < 1 || rank > 9)
			return null;
		if(_Privs.get(rank) == null)
			setRankPrivs(rank, 0);
		return _Privs.get(rank);
	}

	public void setRankPrivs(final int rank, final int privs)
	{
		if(rank < 1 || rank > 9)
			return;

		if(_Privs.get(rank) != null)
			_Privs.get(rank).setPrivs(privs);
		else
			_Privs.put(rank, new RankPrivs(rank, 0, privs));

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			// _log.warning("requested store clan privs in db for rank: " + rank + ", privs: " + privs);
			// Retrieve all skills of this L2Player from the database
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("REPLACE INTO clan_privs (clan_id,rank,party,privilleges) VALUES (?,?,?,?)");
			statement.setInt(1, getClanId());
			statement.setInt(2, rank);
			statement.setInt(3, _Privs.get(rank).getParty());
			statement.setInt(4, privs);
			statement.execute();
		}
		catch(final Exception e)
		{
			_log.warning("Could not store clan privs for rank: " + e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	/** used to retrieve all privilege ranks */
	public final RankPrivs[] getAllRankPrivs()
	{
		if(_Privs == null)
			return new RankPrivs[0];
		return _Privs.values().toArray(new RankPrivs[_Privs.values().size()]);
	}

	private int _auctionBiddedAt = 0;

	public int getAuctionBiddedAt()
	{
		return _auctionBiddedAt;
	}

	public void setAuctionBiddedAt(final int id)
	{
		_auctionBiddedAt = id;
		// store changes to DB
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clan_data SET auction_bid_at=? WHERE clan_id=?");
			statement.setInt(1, id);
			statement.setInt(2, getClanId());
			statement.execute();
		}
		catch(final Exception e)
		{
			_log.warning("Could not store auction for clan: " + e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public void sendMessageToAll(final String message)
	{
		for(final L2ClanMember member : _members.values())
			if(member.isOnline() && member.getPlayer() != null)
				member.getPlayer().sendMessage(message);
	}

	public void sendMessageToAll(final String message, final String message_ru)
	{
		for(final L2ClanMember member : _members.values())
			if(member.isOnline() && member.getPlayer() != null)
			{
				final L2Player player = member.getPlayer();
				if(player.getVar("lang@") == null || player.getVar("lang@").equalsIgnoreCase("en") || message_ru.equals(""))
					player.sendMessage(message);
				else
					player.sendMessage(message_ru);
			}
	}

	private Siege _siege;
	private boolean _isDefender;
	private boolean _isAttacker;

	public void setSiege(final Siege siege)
	{
		_siege = siege;
	}

	public Siege getSiege()
	{
		return _siege;
	}

	public void setDefender(final boolean b)
	{
		_isDefender = b;
	}

	public void setAttacker(final boolean b)
	{
		_isAttacker = b;
	}

	public boolean isDefender()
	{
		return _isDefender;
	}

	public boolean isAttacker()
	{
		return _isAttacker;
	}

	private static class ClanReputationComparator implements Comparator<L2Clan>
	{
		public int compare(final L2Clan o1, final L2Clan o2)
		{
			return o2.getReputationScore() - o1.getReputationScore();
		}
	}

	public void insertNotice()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO clan_notices (clanID, notice, enabled) values (?,?,?)");
			statement.setInt(1, getClanId());
			statement.setString(2, "Change me");
			statement.setString(3, "false");
			statement.execute();
		}
		catch(final Exception e)
		{
			System.out.println("BBS: Error while creating clan notice for clan " + getClanId() + "");
			if(e.getMessage() != null)
				System.out.println("BBS: Exception = " + e.getMessage() + "");
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public String getNotice()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT notice FROM clan_notices WHERE clanID=?");
			statement.setInt(1, getClanId());
			rset = statement.executeQuery();
			while(rset.next())
				_notice = rset.getString("notice");
		}
		catch(final Exception e)
		{
			System.out.println("BBS: Error while getting notice from DB for clan " + getClanId() + "");
			if(e.getMessage() != null)
				System.out.println("BBS: Exception = " + e.getMessage() + "");
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}
		return _notice;
	}

	public String getNoticeForBBS()
	{
		String notice = "";
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT notice FROM clan_notices WHERE clanID=?");
			statement.setInt(1, getClanId());
			rset = statement.executeQuery();
			while(rset.next())
				notice = rset.getString("notice");
		}
		catch(final Exception e)
		{
			System.out.println("BBS: Error while getting notice from DB for clan " + getClanId() + "");
			if(e.getMessage() != null)
				System.out.println("BBS: Exception = " + e.getMessage() + "");
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}
		return notice.replaceAll("<br>", "\n");
	}

	/**
	 * Назначить новое сообщение
	 */
	public void setNotice(String notice)
	{
		notice = notice.replaceAll("\n", "<br>");

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clan_notices SET notice=? WHERE clanID=?");
			statement.setString(1, notice);
			statement.setInt(2, getClanId());
			statement.execute();
			_notice = notice;
		}
		catch(final Exception e)
		{
			System.out.println("BBS: Error while saving notice for clan " + getClanId() + "");
			if(e.getMessage() != null)
				System.out.println("BBS: Exception = " + e.getMessage() + "");
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	/**
	 * Включено или нет?
	 */
	public boolean isNoticeEnabled()
	{
		String result = "";
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT enabled FROM clan_notices WHERE clanID=?");
			statement.setInt(1, getClanId());
			rset = statement.executeQuery();

			while(rset.next())
				result = rset.getString("enabled");
		}
		catch(final Exception e)
		{
			System.out.println("BBS: Error while reading _noticeEnabled for clan " + getClanId() + "");
			if(e.getMessage() != null)
				System.out.println("BBS: Exception = " + e.getMessage() + "");
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}
		if(result.isEmpty())
			insertNotice();
		else if(result.compareToIgnoreCase("true") == 0)
			return true;
		return false;
	}

	/**
	 * Включить/выключить
	 */
	public void setNoticeEnabled(final boolean noticeEnabled)
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clan_notices SET enabled=? WHERE clanID=?");
			if(noticeEnabled)
				statement.setString(1, "true");
			else
				statement.setString(1, "false");
			statement.setInt(2, getClanId());
			statement.execute();
		}
		catch(final Exception e)
		{
			System.out.println("BBS: Error while updating notice status for clan " + getClanId() + "");
			if(e.getMessage() != null)
				System.out.println("BBS: Exception = " + e.getMessage() + "");
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
		_noticeEnabled = noticeEnabled;
	}

	public int getWhBonus()
	{
		return _whBonus;
	}

	public void setWhBonus(final int i)
	{
		if(_whBonus != -1)
			mysql.set("UPDATE `clan_data` SET `warehouse` = '" + i + "' WHERE `clan_id`=" + getClanId());
		_whBonus = i;
	}

	/**
	 * @return Возвратит врата клансуммона, или null, если нет врат
	 */
	public L2ClanGate getClanGate()
	{
		return _clanGate;
	}

	/**
	 * Устанавливает врата клансуммона клану
	 * 
	 * @param clanGate
	 *            - инстанс врат для установки
	 */
	public void setClanGate(L2ClanGate clanGate)
	{
		_clanGate = clanGate;
		if(_clanGate != null)
			for(L2ClanMember temp : _members.values())
				if(temp.isOnline())
				{
					L2Player tempPlayer = temp.getPlayer();
					tempPlayer.sendMessage(new CustomMessage("com.lineage.game.model.L2Clan.L2ClanGate.Activated", tempPlayer));
				}
	}
}