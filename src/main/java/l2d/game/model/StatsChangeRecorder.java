package l2d.game.model;

import l2d.Config;
import l2d.game.serverpackets.NickNameChanged;
import l2d.game.serverpackets.StatusUpdate;

@SuppressWarnings({ "nls", "unqualified-field-access", "boxing" })
public class StatsChangeRecorder
{
	private L2Player _activeChar;
	private int _accuracy;
	private int _attackSpeed;
	private int _castSpeed;
	private int _criticalHit;
	private int _evasion;
	private int _magicAttack;
	private int _magicDefence;
	private int _maxCp;
	private int _maxHp;
	private int _maxLoad;
	private int _curLoad;
	private int _maxMp;
	private int _physicAttack;
	private int _physicDefence;
	private int[] _attackElement;
	private int _defenceFire;
	private int _defenceWater;
	private int _defenceWind;
	private int _defenceEarth;
	private int _defenceHoly;
	private int _defenceUnholy;

	private int _level;
	private long _exp;
	private int _sp;
	private int _karma;
	private int _pk;
	private int _pvp;

	private int _runSpeed;
	private int _abnormalEffects;

	private String _title;

	public StatsChangeRecorder(L2Player activeChar)
	{
		_activeChar = activeChar;
		refreshSaves();
	}

	public void refreshSaves()
	{
		if(_activeChar == null)
			return;

		_accuracy = _activeChar.getAccuracy();
		_attackSpeed = _activeChar.getPAtkSpd();
		_castSpeed = _activeChar.getMAtkSpd();
		_criticalHit = _activeChar.getCriticalHit(null, null);
		_evasion = _activeChar.getEvasionRate(null);
		_magicAttack = _activeChar.getMAtk(null, null);
		_magicDefence = _activeChar.getMDef(null, null);
		_maxCp = _activeChar.getMaxCp();
		_maxHp = _activeChar.getMaxHp();
		_maxLoad = _activeChar.getMaxLoad();
		_curLoad = _activeChar.getCurrentLoad();
		_maxMp = _activeChar.getMaxMp();
		_physicAttack = _activeChar.getPAtk(null);
		_physicDefence = _activeChar.getPDef(null);
		_attackElement = _activeChar.getAttackElement();
		_defenceFire = _activeChar.getDefenceFire();
		_defenceWater = _activeChar.getDefenceWater();
		_defenceWind = _activeChar.getDefenceWind();
		_defenceEarth = _activeChar.getDefenceEarth();
		_defenceHoly = _activeChar.getDefenceHoly();
		_defenceUnholy = _activeChar.getDefenceUnholy();

		_level = _activeChar.getLevel();
		_exp = _activeChar.getExp();
		_sp = _activeChar.getSp();
		_karma = _activeChar.getKarma();

		_runSpeed = _activeChar.getRunSpeed();
		_pk = _activeChar.getPkKills();
		_pvp = _activeChar.getPvpKills();
		_abnormalEffects = _activeChar.getAbnormalEffect(); //TODO: почему-то мне кажется что его можно отослать отдельным пакетом
		_title = _activeChar.getTitle();
	}

	public void sendChanges()
	{
		if(_activeChar == null)
			return;

		// Броадкаст UserInfo и charInfo();
		if(needsUserInfoBroadcast())
		{
			_activeChar.broadcastUserInfo(!Config.BROADCAST_STATS_INTERVAL);
			return;
		}

		sendGlobalInfo();
		sendPartyInfo();
		sendSelfInfo();

		refreshSaves();
	}

	/**
	 * Отправляет броадкастом инфу всем игрокам
	 */
	private void sendGlobalInfo()
	{
		StatusUpdate globalUpdate = new StatusUpdate(_activeChar.getObjectId());

		if(_karma != _activeChar.getKarma())
			globalUpdate.addAttribute(StatusUpdate.KARMA, _activeChar.getKarma());

		if(globalUpdate.hasAttributes())
			_activeChar.broadcastPacket(globalUpdate);

		// Проверка тайтла
		if(_title == null && _activeChar.getTitle() != null || _title != null && !_title.equals(_activeChar.getTitle()))
			_activeChar.broadcastPacketToOthers(new NickNameChanged(_activeChar));
	}

	/**
	 * Отправляет инфу парти игрока.
	 * Если парти нет, то отправляет лично игроку
	 */
	private void sendPartyInfo()
	{
		// Эти статы нужно рассылать только для партии игрока
		StatusUpdate partyUpdate = new StatusUpdate(_activeChar.getObjectId());

		if(_maxCp != _activeChar.getMaxCp())
			partyUpdate.addAttribute(StatusUpdate.MAX_CP, _activeChar.getMaxCp());

		if(_maxHp != _activeChar.getMaxHp())
			partyUpdate.addAttribute(StatusUpdate.MAX_HP, _activeChar.getMaxHp());

		if(_maxMp != _activeChar.getMaxMp())
			partyUpdate.addAttribute(StatusUpdate.MAX_MP, _activeChar.getMaxMp());

		L2Party party = _activeChar.getParty();
		if(partyUpdate.hasAttributes())
			if(party != null)
				party.broadcastToPartyMembers(partyUpdate);
			else
				_activeChar.sendPacket(partyUpdate);
	}

	/**
	 * Отправляет инфу только игроку
	 */
	private void sendSelfInfo()
	{
		// Количество exp, sp, pk и левел - характеристики о которых другие игроки не обязаны знать
		if(_pk != _activeChar.getPkKills() || _pvp != _activeChar.getPvpKills() || _exp != _activeChar.getExp() || _sp != _activeChar.getSp())
		{
			_activeChar.sendUserInfo(false);
			return;
		}

		// Проверка тайтла
		if(_title == null && _activeChar.getTitle() != null)
		{
			_activeChar.sendUserInfo(false);
			return;
		}
		else if(_title != null && !_title.equals(_activeChar.getTitle()))
		{
			_activeChar.sendUserInfo(false);
			return;
		}

		if(_accuracy != _activeChar.getAccuracy())
		{
			_activeChar.sendUserInfo(false);
			return;
		}

		if(_criticalHit != _activeChar.getCriticalHit(null, null))
		{
			_activeChar.sendUserInfo(false);
			return;
		}

		if(_evasion != _activeChar.getEvasionRate(null))
		{
			_activeChar.sendUserInfo(false);
			return;
		}

		if(_magicAttack != _activeChar.getMAtk(null, null))
		{
			_activeChar.sendUserInfo(false);
			return;
		}

		if(_magicDefence != _activeChar.getMDef(null, null))
		{
			_activeChar.sendUserInfo(false);
			return;
		}

		if(_maxLoad != _activeChar.getMaxLoad())
		{
			_activeChar.sendUserInfo(false);
			return;
		}

		if(_curLoad != _activeChar.getCurrentLoad())
		{
			_activeChar.sendUserInfo(false);
			return;
		}

		if(_physicAttack != _activeChar.getPAtk(null))
		{
			_activeChar.sendUserInfo(false);
			return;
		}

		if(_physicDefence != _activeChar.getPDef(null))
		{
			_activeChar.sendUserInfo(false);
			return;
		}

		int[] attElement = _activeChar.getAttackElement();
		if(_attackElement != null && attElement == null || _attackElement == null && attElement != null || _attackElement != null && attElement != null && (_attackElement[0] != attElement[0] || _attackElement[1] != attElement[1]))
		{
			_activeChar.sendUserInfo(false);
			return;
		}

		if(_defenceFire != _activeChar.getDefenceFire())
		{
			_activeChar.sendUserInfo(false);
			return;
		}

		if(_defenceWater != _activeChar.getDefenceWater())
		{
			_activeChar.sendUserInfo(false);
			return;
		}

		if(_defenceWind != _activeChar.getDefenceWind())
		{
			_activeChar.sendUserInfo(false);
			return;
		}

		if(_defenceEarth != _activeChar.getDefenceEarth())
		{
			_activeChar.sendUserInfo(false);
			return;
		}

		if(_defenceHoly != _activeChar.getDefenceHoly())
		{
			_activeChar.sendUserInfo(false);
			return;
		}

		if(_defenceUnholy != _activeChar.getDefenceUnholy())
		{
			_activeChar.sendUserInfo(false);
			return;
		}

		if(_level != _activeChar.getLevel())
			_activeChar.sendUserInfo(false);
	}

	/**
	 * Проверяет нужно ли делать UserInfo broadcast. Дорогостоящая операция.
	 * @return true если нужно.
	 */
	private boolean needsUserInfoBroadcast()
	{
		return _runSpeed != _activeChar.getRunSpeed() || _abnormalEffects != _activeChar.getAbnormalEffect() || _attackSpeed != _activeChar.getPAtkSpd() || _castSpeed != _activeChar.getMAtkSpd();
	}
}