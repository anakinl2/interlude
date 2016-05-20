package com.lineage.game.model;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import com.lineage.game.serverpackets.ExAutoSoulShot;
import com.lineage.game.serverpackets.ShortCutInit;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.SkillTable;

public class ShortCuts
{
	private static Logger _log = Logger.getLogger(ShortCuts.class.getName());

	private L2Player _owner;
	private ConcurrentHashMap<Integer, L2ShortCut> _shortCuts = new ConcurrentHashMap<Integer, L2ShortCut>();

	public ShortCuts(L2Player owner)
	{
		_owner = owner;
	}

	public Collection<L2ShortCut> getAllShortCuts()
	{
		return _shortCuts.values();
	}

	public L2ShortCut getShortCut(int slot, int page)
	{
		L2ShortCut sc = _shortCuts.get(slot + page * 12);
		// verify shortcut
		if(sc != null && sc.type == L2ShortCut.TYPE_ITEM)
			if(_owner.getInventory().getItemByObjectId(sc.id) == null)
			{
				_owner.sendPacket(new SystemMessage(SystemMessage.THERE_ARE_NO_MORE_ITEMS_IN_THE_SHORTCUT));
				deleteShortCut(sc.slot, sc.page);
				sc = null;
			}
		return sc;
	}

	public void registerShortCut(L2ShortCut shortcut)
	{
		L2ShortCut oldShortCut = _shortCuts.put(shortcut.slot + 12 * shortcut.page, shortcut);
		registerShortCutInDb(shortcut, oldShortCut);
	}

	private synchronized void registerShortCutInDb(L2ShortCut shortcut, L2ShortCut oldShortCut)
	{
		if(oldShortCut != null)
			deleteShortCutFromDb(oldShortCut);

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("REPLACE INTO character_shortcuts SET char_obj_id=?,slot=?,page=?,type=?,shortcut_id=?,level=?,class_index=?");
			statement.setInt(1, _owner.getObjectId());
			statement.setInt(2, shortcut.slot);
			statement.setInt(3, shortcut.page);
			statement.setInt(4, shortcut.type);
			statement.setInt(5, shortcut.id);
			statement.setInt(6, shortcut.level);
			statement.setInt(7, _owner.getActiveClassId());
			statement.execute();
		}
		catch(Exception e)
		{
			_log.log(Level.WARNING, "could not store shortcuts:", e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	/**
	 * @param shortcut
	 */
	private void deleteShortCutFromDb(L2ShortCut shortcut)
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("DELETE FROM character_shortcuts WHERE char_obj_id=? AND slot=? AND page=? AND class_index=?");
			statement.setInt(1, _owner.getObjectId());
			statement.setInt(2, shortcut.slot);
			statement.setInt(3, shortcut.page);
			statement.setInt(4, _owner.getActiveClassId());
			statement.execute();
		}
		catch(Exception e)
		{
			_log.log(Level.WARNING, "could not delete shortcuts:", e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	/**
	 * Удаляет ярлык с пользовательской панели по номеру страницы и слота.
	 * @param slot
	 * @param page
	 */
	public void deleteShortCut(int slot, int page)
	{
		L2ShortCut old = _shortCuts.remove(slot + page * 12);
		if(old == null)
			return;
		deleteShortCutFromDb(old);
		// При удалении с панели скила, на оффе шлется полный инит ярлыков
		// Обработка удаления предметных ярлыков - клиент сайд.
		if(old.type == L2ShortCut.TYPE_SKILL)
		{
			_owner.sendPacket(new ShortCutInit(_owner));
			for(int shotId : _owner.getAutoSoulShot())
				_owner.sendPacket(new ExAutoSoulShot(shotId, true));
		}
	}

	/**
	 * Удаляет ярлык предмета с пользовательской панели.
	 * @param objectId
	 */
	public void deleteShortCutByObjectId(int objectId)
	{
		for(L2ShortCut shortcut : _shortCuts.values())
			if(shortcut != null && shortcut.type == L2ShortCut.TYPE_ITEM && shortcut.id == objectId)
				deleteShortCut(shortcut.slot, shortcut.page);
	}

	/**
	 * Удаляет ярлык скила с пользовательской панели.
	 * @param skillId
	 */
	public void deleteShortCutBySkillId(int skillId)
	{
		for(L2ShortCut shortcut : _shortCuts.values())
			if(shortcut != null && shortcut.type == L2ShortCut.TYPE_SKILL && shortcut.id == skillId)
				deleteShortCut(shortcut.slot, shortcut.page);
	}

	public void restore()
	{
		_shortCuts.clear();
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT char_obj_id, slot, page, type, shortcut_id, level FROM character_shortcuts WHERE char_obj_id=? AND class_index=?");
			statement.setInt(1, _owner.getObjectId());
			statement.setInt(2, _owner.getActiveClassId());
			rset = statement.executeQuery();
			while(rset.next())
			{
				int slot = rset.getInt("slot");
				int page = rset.getInt("page");
				int type = rset.getInt("type");
				int id = rset.getInt("shortcut_id");
				int level = rset.getInt("level");

				L2ShortCut sc = new L2ShortCut(slot, page, type, id, level);
				_shortCuts.put(slot + page * 12, sc);
			}
		}
		catch(Exception e)
		{
			_log.log(Level.WARNING, "could not store shortcuts:", e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}

		// Проверка ярлыков
		for(L2ShortCut sc : _shortCuts.values())
			// Удаляем ярлыки на предметы, которых нету в инвентаре
			if(sc.type == L2ShortCut.TYPE_ITEM)
			{
				if(_owner.getInventory().getItemByObjectId(sc.id) == null)
					deleteShortCut(sc.slot, sc.page);
			}
			// Удаляем ярлыки на все скилы, которых нет у чара
			else if(sc.type == L2ShortCut.TYPE_SKILL)
			{
				L2Skill s = SkillTable.getInstance().getInfo(sc.id, sc.level);
				if(s == null || !s.isValidateable()) // Пропускаем скилы не подлежащие валидейту
					continue;
				if(_owner._skills == null || !_owner._skills.containsKey(sc.id))
					deleteShortCut(sc.slot, sc.page);
			}
	}
}