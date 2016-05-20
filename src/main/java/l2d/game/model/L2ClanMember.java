package l2d.game.model;

import java.lang.ref.WeakReference;

import l2d.db.DatabaseUtils;
import l2d.db.FiltredPreparedStatement;
import l2d.db.L2DatabaseFactory;
import l2d.db.ThreadConnection;
import l2d.game.model.L2Clan.SubPledge;

public class L2ClanMember
{
	private int _objectId;
	private WeakReference<L2Clan> _clan;
	private String _name;
	private String _title;
	private int _level;
	private int _classId;
	private int _sex;
	private WeakReference<L2Player> _player;
	private int _pledgeType;
	private int _powerGrade;
	private int _apprentice;
	private Boolean _clanLeader;

	public L2ClanMember(L2Clan clan, String name, String title, int level, int classId, int objectId, int pledgeType, int powerGrade, int apprentice, Boolean clanLeader)
	{
		_clan = new WeakReference<L2Clan>(clan);
		_name = name;
		_title = title;
		_level = level;
		_classId = classId;
		_objectId = objectId;
		_pledgeType = pledgeType;
		_powerGrade = powerGrade;
		_apprentice = apprentice;
		_clanLeader = clanLeader;
		updatePowerGradeParty(0, powerGrade);
	}

	public L2ClanMember(L2Player player)
	{
		_player = new WeakReference<L2Player>(player);
	}

	public void setPlayerInstance(L2Player player)
	{
		if(player != null)
		{
			// this is here to keep the data when the player logs off
			_clan = new WeakReference<L2Clan>(player.getClan());
			_name = player.getName();
			_title = player.getTitle();
			_level = player.getLevel();
			_classId = player.getClassId().getId();
			_objectId = player.getObjectId();
			_pledgeType = player.getPledgeType();
			_powerGrade = player.getPowerGrade();
			_apprentice = player.getApprentice();
			_clanLeader = player.isClanLeader();
			_player = new WeakReference<L2Player>(player);
		}
		else
			_player = null;
	}

	public L2Player getPlayer()
	{
		return _player == null ? null : _player.get();
	}

	public boolean isOnline()
	{
		return _player != null && _player.get() != null;
	}

	public L2Clan getClan()
	{
		if(_player != null && _player.get() != null)
			return _player.get().getClan();
		return _clan.get();
	}

	/**
	 * @return Returns the classId.
	 */
	public int getClassId()
	{
		if(_player != null && _player.get() != null)
			return _player.get().getClassId().getId();
		return _classId;
	}

	public int getSex()
	{
		if(_player != null && _player.get() != null)
			return _player.get().getSex();
		return _sex;
	}

	/**
	 * @return Returns the level.
	 */
	public int getLevel()
	{
		if(_player != null && _player.get() != null)
			return _player.get().getLevel();
		return _level;
	}

	/**
	 * @return Returns the name.
	 */
	public String getName()
	{
		if(_player != null && _player.get() != null)
			return _player.get().getName();
		return _name;
	}

	/**
	 * @return Returns the objectId.
	 */
	public int getObjectId()
	{
		if(_player != null && _player.get() != null)
			return _player.get().getObjectId();
		return _objectId;
	}

	public String getTitle()
	{
		if(_player != null && _player.get() != null)
			return _player.get().getTitle();
		return _title;
	}

	public void setTitle(String title)
	{
		_title = title;
		if(_player != null && _player.get() != null)
		{
			_player.get().setTitle(title);
			_player.get().sendChanges();
		}
		else
		{
			ThreadConnection con = null;
			FiltredPreparedStatement statement = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("UPDATE characters SET title=? WHERE obj_Id=?");
				statement.setString(1, title);
				statement.setInt(2, _objectId);
				statement.execute();
			}
			catch(Exception e)
			{}
			finally
			{
				DatabaseUtils.closeDatabaseCS(con, statement);
			}
		}
	}

	public int getPledgeType()
	{
		if(_player != null && _player.get() != null)
			return _player.get().getPledgeType();
		return _pledgeType;
	}

	public void setPledgeType(int pledgeType)
	{
		_pledgeType = pledgeType;
		if(_player != null && _player.get() != null)
			_player.get().setPledgeType(pledgeType);
		else
			updatePledgeType();
	}

	private void updatePledgeType()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE characters SET pledge_type=? WHERE obj_Id=?");
			statement.setInt(1, _pledgeType);
			statement.setInt(2, _objectId);
			statement.execute();
		}
		catch(Exception e)
		{}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public int getPowerGrade()
	{
		if(_player != null && _player.get() != null)
			return _player.get().getPowerGrade();
		return _powerGrade;
	}

	public void setPowerGrade(int powerGrade)
	{
		updatePowerGradeParty(getPowerGrade(), powerGrade);
		_powerGrade = powerGrade;
		if(_player != null && _player.get() != null)
			_player.get().setPowerGrade(powerGrade);
		else
			updatePowerGrade();
	}

	private void updatePowerGradeParty(int oldGrade, int newGrade)
	{
		if(oldGrade != 0)
			getClan().getRankPrivs(oldGrade).decParty();
		if(newGrade != 0)
			getClan().getRankPrivs(newGrade).incParty();
	}

	private void updatePowerGrade()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE characters SET pledge_rank=? WHERE obj_Id=?");
			statement.setInt(1, _powerGrade);
			statement.setInt(2, _objectId);
			statement.execute();
		}
		catch(Exception e)
		{}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	private int getApprentice()
	{
		if(_player != null && _player.get() != null)
			return _player.get().getApprentice();
		return _apprentice;
	}

	public void setApprentice(int apprentice)
	{
		_apprentice = apprentice;
		if(_player != null && _player.get() != null)
			_player.get().setApprentice(apprentice);
		else
			updateApprentice();
	}

	private void updateApprentice()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE characters SET apprentice=? WHERE obj_Id=?");
			statement.setInt(1, _apprentice);
			statement.setInt(2, _objectId);
			statement.execute();
		}
		catch(Exception e)
		{}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public String getApprenticeName()
	{
		if(getApprentice() != 0)
			if(getClan().getClanMember(getApprentice()) != null)
				return getClan().getClanMember(getApprentice()).getName();
		return "";
	}

	public boolean hasApprentice()
	{
		return getApprentice() != 0;
	}

	public int getSponsor()
	{
		if(getPledgeType() != L2Clan.SUBUNIT_ACADEMY)
			return 0;
		int id = getObjectId();
		for(L2ClanMember element : getClan().getMembers())
			if(element.getApprentice() == id)
				return element.getObjectId();
		return 0;
	}

	private String getSponsorName()
	{
		int sponsorId = getSponsor();
		if(sponsorId == 0)
			return "";
		else if(getClan().getClanMember(sponsorId) != null)
			return getClan().getClanMember(sponsorId).getName();
		return "";
	}

	public boolean hasSponsor()
	{
		return getSponsor() != 0;
	}

	public String getRelatedName()
	{
		if(getPledgeType() == L2Clan.SUBUNIT_ACADEMY)
			return getSponsorName();
		return getApprenticeName();
	}

	public boolean isClanLeader()
	{
		if(_player != null && _player.get() != null)
			return _player.get().isClanLeader();
		return _clanLeader;
	}

	public int isSubLeader()
	{
		for(SubPledge pledge : getClan().getAllSubPledges())
			if(pledge.getLeaderId() == getObjectId())
				return pledge.getType();
		return 0;
	}
}