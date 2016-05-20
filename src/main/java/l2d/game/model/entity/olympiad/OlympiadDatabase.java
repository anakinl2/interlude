package l2d.game.model.entity.olympiad;

import java.sql.ResultSet;
import java.util.Calendar;
import java.util.List;

import javolution.util.FastList;
import l2d.db.DatabaseUtils;
import l2d.db.FiltredPreparedStatement;
import l2d.db.L2DatabaseFactory;
import l2d.db.ThreadConnection;
import l2d.game.Announcements;
import l2d.game.instancemanager.ServerVariables;
import l2d.game.model.L2World;
import l2d.game.model.base.ClassId;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.templates.StatsSet;

public class OlympiadDatabase
{
	public static synchronized void loadNobles()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(Olympiad.OLYMPIAD_LOAD_NOBLES);
			rset = statement.executeQuery();

			while(rset.next())
			{
				StatsSet statDat = new StatsSet();
				int charId = rset.getInt(Olympiad.CHAR_ID);
				statDat.set(Olympiad.CLASS_ID, rset.getInt(Olympiad.CLASS_ID));
				statDat.set(Olympiad.CHAR_NAME, rset.getString(Olympiad.CHAR_NAME));
				statDat.set(Olympiad.POINTS, rset.getInt(Olympiad.POINTS));
				statDat.set(Olympiad.POINTS_PAST, rset.getInt(Olympiad.POINTS_PAST));
				statDat.set(Olympiad.COMP_DONE, rset.getInt(Olympiad.COMP_DONE));
				statDat.set(Olympiad.COMP_WIN, rset.getInt(Olympiad.COMP_WIN));
				statDat.set(Olympiad.COMP_LOOSE, rset.getInt(Olympiad.COMP_LOOSE));
				statDat.set("to_save", false);

				Olympiad._nobles.put(charId, statDat);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}
	}

	/**
	 * Сбрасывает информацию о ноблесах, сохраняя очки за предыдущий период
	 */
	public static synchronized void cleanupNobles()
	{
		Olympiad._log.info("Olympiad: Clearing nobles...");
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(Olympiad.OLYMPIAD_CLEANUP_NOBLES);
			statement.execute();
		}
		catch(Exception e)
		{
			Olympiad._log.warning("Olympiad System: Couldn't cleanup nobles table");
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}

		for(Integer nobleId : Olympiad._nobles.keySet())
		{
			StatsSet nobleInfo = Olympiad._nobles.get(nobleId);
			int points = nobleInfo.getInteger(Olympiad.POINTS);
			nobleInfo.set(Olympiad.POINTS, Olympiad.DEFAULT_POINTS);
			nobleInfo.set(Olympiad.POINTS_PAST, points);
			nobleInfo.set(Olympiad.COMP_DONE, 0);
			nobleInfo.set(Olympiad.COMP_WIN, 0);
			nobleInfo.set(Olympiad.COMP_LOOSE, 0);
			nobleInfo.set("to_save", false);
		}
	}

	public static List<String> getClassLeaderBoard(int classId)
	{
		List<String> names = new FastList<String>();

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(Olympiad.GET_EACH_CLASS_LEADER);
			statement.setInt(1, classId);
			rset = statement.executeQuery();

			while(rset.next())
				names.add(rset.getString(Olympiad.CHAR_NAME));

			return names;
		}
		catch(Exception e)
		{
			Olympiad._log.warning("Olympiad System: Couldnt get heros from db: ");
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}

		return names;
	}

	public static synchronized void sortHerosToBe()
	{
		if(Olympiad._period != 1)
			return;

		Olympiad._heroesToBe = new FastList<StatsSet>();

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			StatsSet hero;

			for(ClassId id : ClassId.values())
				if(id.level() == 3)
				{
					statement = con.prepareStatement(Olympiad.OLYMPIAD_GET_HEROS);
					statement.setInt(1, id.getId());
					rset = statement.executeQuery();

					if(rset.next())
					{
						hero = new StatsSet();
						hero.set(Olympiad.CLASS_ID, id.getId());
						hero.set(Olympiad.CHAR_ID, rset.getInt(Olympiad.CHAR_ID));
						hero.set(Olympiad.CHAR_NAME, rset.getString(Olympiad.CHAR_NAME));

						Olympiad._heroesToBe.add(hero);
					}
					DatabaseUtils.closeDatabaseSR(statement, rset);
				}
		}
		catch(Exception e)
		{
			Olympiad._log.warning("Olympiad System: Couldnt heros from db");
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}
	}

	public static synchronized void saveNobleData(int nobleId)
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			StatsSet nobleInfo = Olympiad._nobles.get(nobleId);

			int classId = nobleInfo.getInteger(Olympiad.CLASS_ID);
			String charName = nobleInfo.getString(Olympiad.CHAR_NAME);
			int points = nobleInfo.getInteger(Olympiad.POINTS);
			int points_past = nobleInfo.getInteger(Olympiad.POINTS_PAST);
			int compDone = nobleInfo.getInteger(Olympiad.COMP_DONE);
			int compWin = nobleInfo.getInteger(Olympiad.COMP_WIN);
			int compLoose = nobleInfo.getInteger(Olympiad.COMP_LOOSE);
			boolean toSave = nobleInfo.getBool("to_save");

			if(toSave)
			{
				statement = con.prepareStatement(Olympiad.OLYMPIAD_SAVE_NOBLES);
				statement.setInt(1, nobleId);
				statement.setInt(2, classId);
				statement.setString(3, charName);
				statement.setInt(4, points);
				statement.setInt(5, points_past);
				statement.setInt(6, compDone);
				statement.setInt(7, compWin);
				statement.setInt(8, compLoose);
				statement.execute();
				DatabaseUtils.closeStatement(statement);

				nobleInfo.set("to_save", false);
			}
			else
			{
				statement = con.prepareStatement(Olympiad.OLYMPIAD_UPDATE_NOBLES);
				statement.setInt(1, points);
				statement.setInt(2, points_past);
				statement.setInt(3, compDone);
				statement.setInt(4, compWin);
				statement.setInt(5, compLoose);
				statement.setInt(6, nobleId);
				statement.execute();
				DatabaseUtils.closeStatement(statement);
			}
		}
		catch(Exception e)
		{
			Olympiad._log.warning("Olympiad System: Couldnt save noble info in db for player " + L2World.getPlayer(nobleId).getName());
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public static synchronized void saveNobleData()
	{
		if(Olympiad._nobles == null)
			return;
		for(Integer nobleId : Olympiad._nobles.keySet())
			saveNobleData(nobleId);
	}

	public static synchronized void setNewOlympiadEnd()
	{
		SystemMessage sm = new SystemMessage(SystemMessage.OLYMPIAD_PERIOD_S1_HAS_STARTED);
		sm.addNumber(Olympiad._currentCycle);

		Announcements.getInstance().announceToAll(sm);

		Calendar currentTime = Calendar.getInstance();
		currentTime.add(Calendar.HOUR, 336);
		currentTime.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
		currentTime.set(Calendar.AM_PM, Calendar.AM);
		currentTime.set(Calendar.HOUR, 12);
		currentTime.set(Calendar.MINUTE, 0);

		//	currentTime.set(Calendar.DAY_OF_WEEK, 7);
		//	currentTime.add(Calendar., 1);
		//	currentTime.set(Calendar.HOUR_OF_DAY, 00);
		//	currentTime.set(Calendar.MINUTE, 00);
		//		currentTime.set(Calendar.DAY_OF_MONTH, 1);
		////		currentTime.add(Calendar.MONTH, 1);
		//		currentTime.set(Calendar.HOUR_OF_DAY, 00);
		//		currentTime.set(Calendar.MINUTE, 00);
		Olympiad._olympiadEnd = currentTime.getTimeInMillis();

		Calendar nextChange = Calendar.getInstance();
		Olympiad._nextWeeklyChange = nextChange.getTimeInMillis() + Olympiad.WEEKLY_PERIOD;

		Olympiad._isOlympiadEnd = false;
	}

	public static void save()
	{
		saveNobleData();
		ServerVariables.set("Olympiad_CurrentCycle", Olympiad._currentCycle);
		ServerVariables.set("Olympiad_Period", Olympiad._period);
		ServerVariables.set("Olympiad_End", Olympiad._olympiadEnd);
		ServerVariables.set("Olympiad_ValdationEnd", Olympiad._validationEnd);
		ServerVariables.set("Olympiad_NextWeeklyChange", Olympiad._nextWeeklyChange);
	}
}