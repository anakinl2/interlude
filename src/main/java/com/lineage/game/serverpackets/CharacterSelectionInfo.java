package com.lineage.game.serverpackets;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.lineage.Config;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import com.lineage.game.model.CharSelectInfoPackage;
import com.lineage.game.network.L2GameClient;
import com.lineage.game.tables.CharTemplateTable;
import com.lineage.game.templates.L2PlayerTemplate;
import com.lineage.util.AutoBan;

/**
 * [S] 09 CharSelectInfo
 * <b>Format:</b> d (SdSddddddddddffdQdddddddddddddddddddddddddddddddddddddddffdddchhd)
 * @author Felixx
 */
public class CharacterSelectionInfo extends L2GameServerPacket
{
	// d (SdSddddddddddffdQdddddddddddddddddddddddddddddddddddddddffdddchhd)
	private static Logger _log = Logger.getLogger(CharacterSelectionInfo.class.getName());

	private String _loginName;
	private int _sessionId, _activeId;
	private CharSelectInfoPackage[] _characterPackages;

	public CharacterSelectionInfo(String loginName, int sessionId)
	{
		_sessionId = sessionId;
		_loginName = loginName;
		_characterPackages = loadCharacterSelectInfo(loginName);
		_activeId = -1;
	}

	public CharacterSelectionInfo(String loginName, int sessionId, int activeId)
	{
		_sessionId = sessionId;
		_loginName = loginName;
		_characterPackages = loadCharacterSelectInfo(loginName);
		_activeId = activeId;
	}

	public CharSelectInfoPackage[] getCharInfo()
	{
		return _characterPackages;
	}

	@Override
	protected final void writeImpl()
	{
		int size = _characterPackages != null ? _characterPackages.length : 0;

		writeC(0x13);
		writeD(size);

		long lastAccess = 0L;
		if(_activeId == -1)
			for(int i = 0; i < size; i++)
				if(lastAccess < _characterPackages[i].getLastAccess())
				{
					lastAccess = _characterPackages[i].getLastAccess();
					_activeId = i;
				}

		for(int i = 0; i < size; i++)
		{
			CharSelectInfoPackage charInfoPackage = _characterPackages[i];

			writeS(charInfoPackage.getName()); // Имя чара
			writeD(charInfoPackage.getCharId()); // ID Чара
			writeS(_loginName); // Логин
			writeD(_sessionId); // ID Сессии
			writeD(charInfoPackage.getClanId()); // ID Клана
			writeD(0x00); // TODO Неизвестно

			writeD(charInfoPackage.getSex()); // Пол
			writeD(charInfoPackage.getRace()); // Расса
			writeD(charInfoPackage.getClassId());

			writeD(0x01); // active ??

			//writeD(charInfoPackage.getX()); // x
			//writeD(charInfoPackage.getY()); // y
			//writeD(charInfoPackage.getZ()); // z
			writeD(0x00); // x
			writeD(0x00); // y
			writeD(0x00); // z

			writeF(charInfoPackage.getCurrentHp()); // hp cur
			writeF(charInfoPackage.getCurrentMp()); // mp cur

			writeD(charInfoPackage.getSp()); // Очки обучения
			writeQ(charInfoPackage.getExp()); // Опыт
			writeD(charInfoPackage.getLevel()); // Уровень
			writeD(charInfoPackage.getKarma()); // Карма
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);

			for(byte PAPERDOLL_ID : UserInfo.PAPERDOLL_ORDER)
				writeD(charInfoPackage.getPaperdollObjectId(PAPERDOLL_ID));

			for(byte PAPERDOLL_ID : UserInfo.PAPERDOLL_ORDER)
				writeD(charInfoPackage.getPaperdollItemId(PAPERDOLL_ID));

			writeD(charInfoPackage.getHairStyle()); // Стиль волос
			writeD(charInfoPackage.getHairColor()); // Цвет волос
			writeD(charInfoPackage.getFace()); // Лицо

			writeF(charInfoPackage.getMaxHp()); // Макс Жизней
			writeF(charInfoPackage.getMaxMp()); // Макс Маны

			writeD(charInfoPackage.getAccessLevel() > -100 ? charInfoPackage.getDeleteTimer() : -1); // Дней до удаления Чара
			writeD(charInfoPackage.getClassId()); // ID Класса

			// Последний выбранный чар
			if(i == _activeId)
				writeD(0x01);
			else
				writeD(0x00); // C3 - Автовыбор чара

			writeC(charInfoPackage.getEnchantEffect() > 127 ? 127 : charInfoPackage.getEnchantEffect()); // Эффект заточки

			writeD(charInfoPackage.getAugmentationId()); // Аунментация
		}
	}

	public static CharSelectInfoPackage[] loadCharacterSelectInfo(String loginName)
	{
		CharSelectInfoPackage charInfopackage;
		ArrayList<CharSelectInfoPackage> characterList = new ArrayList<CharSelectInfoPackage>();

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet pl_rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM characters AS c LEFT JOIN character_subclasses AS cs ON (c.obj_Id=cs.char_obj_id AND cs.isBase=1) WHERE account_name=? LIMIT 7");
			statement.setString(1, loginName);
			pl_rset = statement.executeQuery();

			while(pl_rset.next()) // Заполняем
			{
				charInfopackage = restoreChar(pl_rset, pl_rset);
				if(charInfopackage != null)
					characterList.add(charInfopackage);
			}
		}
		catch(Exception e)
		{
			_log.log(Level.WARNING, "could not restore charinfo:", e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, pl_rset);
		}

		return characterList.toArray(new CharSelectInfoPackage[characterList.size()]);
	}

	private static CharSelectInfoPackage restoreChar(ResultSet chardata, ResultSet charclass)
	{
		CharSelectInfoPackage charInfopackage = null;
		try
		{
			int objectId = chardata.getInt("obj_Id");
			int classid = charclass.getInt("class_id");
			boolean female = chardata.getInt("sex") == 1;
			L2PlayerTemplate templ = CharTemplateTable.getInstance().getTemplate(classid, female);
			if(templ == null)
			{
				_log.log(Level.WARNING, "restoreChar fail | templ == null | objectId: " + objectId + " | classid: " + classid + " | female: " + female);
				return null;
			}
			String name = chardata.getString("char_name");
			charInfopackage = new CharSelectInfoPackage(objectId, name);
			charInfopackage.setLevel(charclass.getInt("level"));
			charInfopackage.setMaxHp(charclass.getInt("maxHp"));
			charInfopackage.setCurrentHp(charclass.getDouble("curHp"));
			charInfopackage.setMaxMp(charclass.getInt("maxMp"));
			charInfopackage.setCurrentMp(charclass.getDouble("curMp"));

			charInfopackage.setFace(chardata.getInt("face"));
			charInfopackage.setHairStyle(chardata.getInt("hairstyle"));
			charInfopackage.setHairColor(chardata.getInt("haircolor"));
			charInfopackage.setSex(female ? 1 : 0);

			charInfopackage.setExp(charclass.getLong("exp"));
			charInfopackage.setSp(charclass.getInt("sp"));
			charInfopackage.setClanId(chardata.getInt("clanid"));

			charInfopackage.setKarma(chardata.getInt("karma"));
			charInfopackage.setRace(templ.race.ordinal());
			charInfopackage.setClassId(classid);
			long deletetime = chardata.getLong("deletetime");
			int deletedays = 0;
			if(Config.DELETE_DAYS > 0)
				if(deletetime > 0)
				{
					deletetime = (int) (System.currentTimeMillis() / 1000 - deletetime);
					deletedays = (int) (deletetime / 3600 / 24);
					if(deletedays >= Config.DELETE_DAYS)
					{
						L2GameClient.deleteFromClan(objectId, charInfopackage.getClanId());
						L2GameClient.deleteCharByObjId(objectId);
						return null;
					}
					deletetime = Config.DELETE_DAYS * 3600 * 24 - deletetime;
				}
				else
					deletetime = 0;
			charInfopackage.setDeleteTimer((int) deletetime);
			charInfopackage.setLastAccess(chardata.getLong("lastAccess") * 1000);
			charInfopackage.setAccessLevel(chardata.getInt("accesslevel"));

			if(charInfopackage.getAccessLevel() < 0 && !AutoBan.isBanned(objectId))
				charInfopackage.setAccessLevel(0);
		}
		catch(Exception e)
		{
			_log.log(Level.INFO, "", e);
		}

		return charInfopackage;
	}
}