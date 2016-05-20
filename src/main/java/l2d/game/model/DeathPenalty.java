package l2d.game.model;

import com.lineage.Config;
import l2d.game.serverpackets.EtcStatusUpdate;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.SkillTable;
import com.lineage.util.Rnd;

public class DeathPenalty
{
	private L2Player _player;
	private byte _level;
	private static final int _skillId = 5076;
	private static final int _fortuneOfNobleseSkillId = 1325;
	private static final int _charmOfLuckSkillId = 2168;
	private boolean _hasCharmOfLuck;

	public DeathPenalty(L2Player player, byte level)
	{
		_player = player;
		_level = level;
	}

	public L2Player getPlayer()
	{
		return _player;
	}

	/*
	 * For common usage
	 */
	public int getLevel()
	{
		// Some checks if admin set incorrect value at database
		if(_level > 15)
			_level = 15;

		if(_level < 0)
			_level = 0;

		return Config.ALLOW_DEATH_PENALTY ? _level : 0;
	}

	/*
	 * Used only when saving DB if admin for some reasons disabled it in config after it was enabled.
	 * In if we will use getLevel() it will be reseted to 0
	 */
	public int getLevelOnSaveDB()
	{
		if(_level > 15)
			_level = 15;

		if(_level < 0)
			_level = 0;

		return _level;
	}

	public void notifyDead(L2Character killer)
	{
		if(!Config.ALLOW_DEATH_PENALTY)
			return;

		if(_hasCharmOfLuck)
		{
			_hasCharmOfLuck = false;
			return;
		}

		if(_player.getLevel() <= 9)
			return;

		int karmaBonus = _player.getKarma() / Config.ALT_DEATH_PENALTY_KARMA_PENALTY;
		if(karmaBonus < 0)
			karmaBonus = 0;

		if(Rnd.chance(Config.ALT_DEATH_PENALTY_CHANCE + karmaBonus) && !(killer instanceof L2Playable))
			addLevel();
	}

	public void restore()
	{
		L2Skill remove = getCurrentSkill();

		if(remove != null)
			_player.removeSkill(remove, true);

		if(!Config.ALLOW_DEATH_PENALTY)
			return;

		if(getLevel() > 0)
		{
			_player.addSkill(SkillTable.getInstance().getInfo(_skillId, getLevel()), false);
			_player.sendPacket(new SystemMessage(SystemMessage.THE_LEVEL_S1_DEATH_PENALTY_WILL_BE_ASSESSED).addNumber(getLevel()));
		}
		_player.sendPacket(new EtcStatusUpdate(_player));
		_player.broadcastUserInfo(true);
	}

	public void addLevel()
	{
		if(getLevel() >= 15) //maximum level reached
			return;

		if(getLevel() != 0)
		{
			L2Skill remove = getCurrentSkill();

			if(remove != null)
				_player.removeSkill(remove, true);
		}

		_level++;

		_player.addSkill(SkillTable.getInstance().getInfo(_skillId, getLevel()), false);
		_player.sendPacket(new EtcStatusUpdate(_player));
		_player.broadcastUserInfo(true);
		_player.sendPacket(new SystemMessage(SystemMessage.THE_LEVEL_S1_DEATH_PENALTY_WILL_BE_ASSESSED).addNumber(getLevel()));
	}

	public void reduceLevel()
	{
		if(getLevel() <= 0)
			return;

		L2Skill remove = getCurrentSkill();

		if(remove != null)
			_player.removeSkill(remove, true);

		_level--;

		if(getLevel() > 0)
		{
			_player.addSkill(SkillTable.getInstance().getInfo(_skillId, getLevel()), false);
			_player.sendPacket(new EtcStatusUpdate(_player));
			_player.broadcastUserInfo(true);
			_player.sendPacket(new SystemMessage(SystemMessage.THE_LEVEL_S1_DEATH_PENALTY_WILL_BE_ASSESSED).addNumber(getLevel()));
		}
		else
		{
			_player.sendPacket(new EtcStatusUpdate(_player));
			_player.broadcastUserInfo(true);
			_player.sendPacket(new SystemMessage(SystemMessage.THE_DEATH_PENALTY_HAS_BEEN_LIFTED));
		}
	}

	public L2Skill getCurrentSkill()
	{
		for(L2Skill s : _player.getAllSkills())
			if(s.getId() == _skillId)
				return s;
		return null;
	}

	public void checkCharmOfLuck()
	{
		for(L2Effect e : _player.getEffectList().getAllEffects())
			if(e.getSkill().getId() == _charmOfLuckSkillId || e.getSkill().getId() == _fortuneOfNobleseSkillId)
			{
				_hasCharmOfLuck = true;
				return;
			}

		_hasCharmOfLuck = false;
	}
}