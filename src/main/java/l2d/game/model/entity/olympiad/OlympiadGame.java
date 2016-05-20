package l2d.game.model.entity.olympiad;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Logger;

import javolution.util.FastList;
import com.lineage.Config;
import com.lineage.ext.network.MMOConnection;
import l2d.game.instancemanager.ZoneManager;
import l2d.game.model.Inventory;
import l2d.game.model.L2Party;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.L2Spawn;
import l2d.game.model.L2Summon;
import l2d.game.model.L2World;
import l2d.game.model.L2WorldRegion;
import l2d.game.model.L2Zone;
import l2d.game.model.instances.L2CubicInstance;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.serverpackets.ExAutoSoulShot;
import l2d.game.serverpackets.ExOlympiadMode;
import l2d.game.serverpackets.ExOlympiadUserInfo;
import l2d.game.serverpackets.SkillCoolTime;
import l2d.game.serverpackets.SkillList;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.SkillTable;
import l2d.game.templates.StatsSet;
import com.lineage.util.Log;

public class OlympiadGame
{
	private static final Logger _log = Logger.getLogger(OlympiadGame.class.getName());

	public boolean _aborted;

	private int _id;
	private CompType _type;

	private String _playerOneName;
	private String _playerTwoName;

	private int _playerOneID = 0;
	private int _playerTwoID = 0;

	public L2Player _playerOne;
	public L2Player _playerTwo;

	private double _playerOneDamage = 0;
	private double _playerTwoDamage = 0;

	private List<L2Player> _players = new FastList<L2Player>();
	private int[] _playerOneLocation = new int[] { 0, 0, 0, 0 };
	private int[] _playerTwoLocation = new int[] { 0, 0, 0, 0 };

	// private int[] _stadiumPort;
	private List<L2Player> _spectators;
	private SystemMessage _sm;
	private int _winner = 0;
	private L2Spawn _buffer;
	private L2Spawn _buffer2;
	private byte _started = 0;

	public OlympiadGame(int id, CompType type, List<Integer> list)
	{
		_id = id;
		_aborted = false;
		_type = type;
		_spectators = new FastList<L2Player>();

		if(list != null && list.size() > 1)
		{
			_playerOne = L2World.getPlayer(list.get(0));
			_playerTwo = L2World.getPlayer(list.get(1));
			_players.add(_playerOne);
			_players.add(_playerTwo);

			if(_playerOne == null || _playerTwo == null)
				return;

			try
			{
				if(_playerOne.inObserverMode())
					if(_playerOne.getOlympiadGameId() > 0)
						_playerOne.leaveOlympiadObserverMode();
					else
						_playerOne.leaveObserverMode();

				if(_playerTwo.inObserverMode())
					if(_playerTwo.getOlympiadGameId() > 0)
						_playerTwo.leaveOlympiadObserverMode();
					else
						_playerTwo.leaveObserverMode();

				_playerOne.setOlympiadSide(1);
				_playerTwo.setOlympiadSide(2);
				_playerOne.setOlympiadGameId(_id);
				_playerTwo.setOlympiadGameId(_id);
				_playerOneName = _playerOne.getName();
				_playerTwoName = _playerTwo.getName();
				_playerOneID = _playerOne.getObjectId();
				_playerTwoID = _playerTwo.getObjectId();
			}
			catch(Exception e)
			{
				e.printStackTrace();
				_aborted = true;
				clearPlayers();
			}
			Log.add("Olympiad System: Game - " + id + ": " + _playerOne.getName() + " Vs " + _playerTwo.getName(), "olympiad");
		}
		else
		{
			Log.add("Player list is null for game " + _id, "olympiad");
			_aborted = true;
			clearPlayers();
		}
	}

	private void clearPlayers()
	{
		_playerOne = null;
		_playerTwo = null;
		_players = null;
		_playerOneName = "";
		_playerTwoName = "";
		_playerOneID = 0;
		_playerTwoID = 0;
	}

	public boolean portPlayersToArena() throws Exception
	{
		boolean _playerOneCrash = true;
		boolean _playerTwoCrash = true;

		try
		{
			if(_playerOne != null && _playerOne.getOlympiadGameId() != -1)
				_playerOneCrash = false;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		try
		{
			if(_playerTwo != null && _playerTwo.getOlympiadGameId() != -1)
				_playerTwoCrash = false;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		if(_playerOneCrash)
		{
			StatsSet playerOneStat;
			playerOneStat = Olympiad._nobles.get(_playerOneID);
			int playerOnePoints = playerOneStat.getInteger(Olympiad.POINTS);
			int i = playerOnePoints - playerOnePoints / 5;
			playerOneStat.set(Olympiad.POINTS, i);
			_playerOne = null;
			Log.add("Olympia Result: " + _playerOneName + " vs " + _playerTwoName + " ... " + _playerOneName + " lost " + i + " points for crash before teleport to arena", "olympiad");
		}

		if(_playerTwoCrash)
		{
			StatsSet playerTwoStat;
			playerTwoStat = Olympiad._nobles.get(_playerTwoID);
			int playerTwoPoints = playerTwoStat.getInteger(Olympiad.POINTS);
			int i = playerTwoPoints - playerTwoPoints / 5;
			playerTwoStat.set(Olympiad.POINTS, i);
			_playerTwo = null;
			Log.add("Olympia Result: " + _playerOneName + " vs " + _playerTwoName + " ... " + _playerTwoName + " lost " + i + " points for crash before teleport to arena", "olympiad");
		}

		if(_playerOneCrash || _playerTwoCrash)
		{
			if(Config.DEBUG)
				_log.warning("PortPlayers: player is null");
			_aborted = true;
			return false;
		}

		try
		{
			if(_playerOne.isDead())
				_playerOne.setIsPendingRevive(true);

			if(_playerTwo.isDead())
				_playerTwo.setIsPendingRevive(true);

			_playerOneLocation[0] = _playerOne.getX();
			_playerOneLocation[1] = _playerOne.getY();
			_playerOneLocation[2] = _playerOne.getZ();
			_playerOneLocation[3] = _playerOne.getReflection().getId();

			_playerTwoLocation[0] = _playerTwo.getX();
			_playerTwoLocation[1] = _playerTwo.getY();
			_playerTwoLocation[2] = _playerTwo.getZ();
			_playerTwoLocation[3] = _playerTwo.getReflection().getId();

			if(_playerOne.isSitting())
				_playerOne.standUp();

			if(_playerTwo.isSitting())
				_playerTwo.standUp();

			_playerOne.setTarget(null);
			_playerTwo.setTarget(null);
			_playerOne.setIsInOlympiadMode(true);
			_playerTwo.setIsInOlympiadMode(true);

			L2Zone zone = ZoneManager.getInstance().getZoneById(L2Zone.ZoneType.OlympiadStadia, 3001 + _id, false);
			if(zone == null)
			{
				_log.warning("Olympiad zone null!!!");
				return false;
			}
			if(zone.getSpawns() == null || zone.getSpawns().size() == 0)
			{
				_log.warning("Olympiad spawns null!!!");
				return false;
			}

			int[] tele = zone.getSpawns().get(0);
			_playerOne.teleToLocation(tele[0], tele[1], tele[2], 0);
			_playerTwo.teleToLocation(tele[0], tele[1], tele[2], 0);

			_playerOne.sendPacket(new ExOlympiadMode(1));
			_playerTwo.sendPacket(new ExOlympiadMode(2));
			_playerOne.sendPacket(new ExOlympiadUserInfo(_playerTwo));
			_playerTwo.sendPacket(new ExOlympiadUserInfo(_playerOne));
		}
		catch(Exception e)
		{
			Log.add("Olympia Result: " + _playerOneName + " vs " + _playerTwoName + " ...  aborted due to crash before teleport to arena", "olympiad");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public void InvisPlayers()
	{
		for(L2Player activeChar : _players)
			try
			{
				if(activeChar == null)
					continue;
				if(activeChar.isInvisible())
				{
					activeChar.setInvisible(false);
					activeChar.broadcastUserInfo(true);
					if(activeChar.getPet() != null)
						activeChar.getPet().broadcastPetInfo();
				}
				else
				{
					activeChar.setInvisible(true);
					activeChar.sendUserInfo(true);
					if(activeChar.getCurrentRegion() != null)
						for(final L2WorldRegion neighbor : activeChar.getCurrentRegion().getNeighbors())
							neighbor.removePlayerFromOtherPlayers(activeChar);
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
	}

	protected void additions()
	{
		for(L2Player player : _players)
		{
			if(player == null)
				continue;

			try
			{
				L2Skill skill;

				skill = SkillTable.getInstance().getInfo(1204, 2);
				skill.getEffects(player, player, false, false);
				player.sendPacket(new SystemMessage(SystemMessage.S1_S2S_EFFECT_CAN_BE_FELT).addSkillName(1204));

				if(!player.isMageClass())
				{
					skill = SkillTable.getInstance().getInfo(1086, 1);
					skill.getEffects(player, player, false, false);
					player.sendPacket(new SystemMessage(SystemMessage.S1_S2S_EFFECT_CAN_BE_FELT).addSkillName(1086));
				}
				else
				{
					skill = SkillTable.getInstance().getInfo(1085, 1);
					skill.getEffects(player, player, false, false);
					player.sendPacket(new SystemMessage(SystemMessage.S1_S2S_EFFECT_CAN_BE_FELT).addSkillName(1085));
				}
			}
			catch(Exception e)
			{}
		}
	}

	public void preparePlayers()
	{
		if(_aborted)
			return;

		if(_playerOne == null || _playerTwo == null)
			return;

		for(L2Player player : _players)
			try
			{
				// Remove Buffs
				player.getEffectList().stopAllEffects();

				// Remove clan skill
				if(player.getClan() != null)
					for(L2Skill skill : player.getClan().getAllSkills())
						player.removeSkill(skill, false);

				// Remove Hero Skills
				if(player.isHero())
				{
					player.removeSkillById(395);
					player.removeSkillById(396);
					player.removeSkillById(1374);
					player.removeSkillById(1375);
					player.removeSkillById(1376);
				}

				// Abort casting if player casting
				if(player.isCastingNow())
					player.abortCast();

				// Remove player from his party
				if(player.getParty() != null)
				{
					L2Party party = player.getParty();
					party.oustPartyMember(player);
				}

				// Удаляем чужие кубики
				for(L2CubicInstance cubic : player.getCubics())
					if(cubic.isGivenByOther())
						cubic.deleteMe();

				// Remove Summon's Buffs
				if(player.getPet() != null)
				{
					L2Summon summon = player.getPet();
					if(summon.isPet())
						summon.unSummon();
					else
						summon.getEffectList().stopAllEffects();
				}

				// Обновляем скилл лист, после удаления скилов
				player.sendPacket(new SkillList(player));

				// Remove Hero weapons
				L2ItemInstance wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
				if(wpn != null && wpn.isHeroItem())
				{
					player.getInventory().unEquipItem(wpn);
					player.abortAttack();
					player.refreshExpertisePenalty();
					player.broadcastUserInfo(true);
				}

				// remove bsps/sps/ss automation
				ConcurrentSkipListSet<Integer> activeSoulShots = player.getAutoSoulShot();
				for(int itemId : activeSoulShots)
				{
					player.removeAutoSoulShot(itemId);
					player.sendPacket(new ExAutoSoulShot(itemId, false));
				}

				// Разряжаем заряженные соул и спирит шоты
				L2ItemInstance weapon = player.getActiveWeaponInstance();
				if(weapon != null)
				{
					weapon.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
					weapon.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
				}

				player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
				player.setCurrentCp(player.getMaxCp());
				for(L2Skill s : player.getAllSkills())
					player.removeSkillTimeStamp(s.getId());
				player.sendPacket(new SkillCoolTime(player));
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
	}

	public void sendMessageToPlayers(boolean toBattleBegin, int nsecond)
	{
		if(!toBattleBegin)
			_sm = new SystemMessage(SystemMessage.YOU_WILL_ENTER_THE_OLYMPIAD_STADIUM_IN_S1_SECOND_S);
		else
			_sm = new SystemMessage(SystemMessage.THE_GAME_WILL_START_IN_S1_SECOND_S);

		_sm.addNumber(nsecond);
		try
		{
			for(L2Player player : _players)
				player.sendPacket(_sm);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public void portPlayersBack()
	{
		if(_playerOne == null && _playerTwo == null)
			return;

		for(L2Player player : _players)
		{
			if(player == null)
				continue;
			try
			{
				player.setIsInOlympiadMode(false);
				player.setOlympiadSide(-1);
				player.setOlympiadGameId(-1);
				player.getEffectList().stopAllEffects();
				player.setCurrentCp(player.getMaxCp());
				player.setCurrentHp(player.getMaxHp(), false);
				player.setCurrentMp(player.getMaxMp());
				// Add clan skill
				if(player.getClan() != null)
					for(L2Skill skill : player.getClan().getAllSkills())
						if(skill.getMinPledgeClass() <= player.getPledgeClass())
							player.addSkill(skill, false);

				// Add Hero Skills
				if(player.isHero())
				{
					player.addSkill(SkillTable.getInstance().getInfo(395, 1));
					player.addSkill(SkillTable.getInstance().getInfo(396, 1));
					player.addSkill(SkillTable.getInstance().getInfo(1374, 1));
					player.addSkill(SkillTable.getInstance().getInfo(1375, 1));
					player.addSkill(SkillTable.getInstance().getInfo(1376, 1));
				}
				// Обновляем скилл лист, после добавления скилов
				player.sendPacket(new SkillList(player));
				player.sendPacket(new ExOlympiadMode(0));
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}

		try
		{
			if(_playerOne != null && _playerOneLocation[0] != 0)
			{
				_playerOne.setReflection(_playerOneLocation[3]);
				_playerOne.teleToLocation(_playerOneLocation[0], _playerOneLocation[1], _playerOneLocation[2]);
			}
			else if(_playerOne != null)
			{
				_playerOne.setReflection(0);
				_playerOne.teleToClosestTown();
			}
			if(_playerTwo != null && _playerTwoLocation[0] != 0)
			{
				_playerTwo.setReflection(_playerTwoLocation[3]);
				_playerTwo.teleToLocation(_playerTwoLocation[0], _playerTwoLocation[1], _playerTwoLocation[2]);
			}
			else if(_playerTwo != null)
			{
				_playerTwo.setReflection(0);
				_playerTwo.teleToClosestTown();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public boolean validated = false;

	public void validateWinner(int mode) throws Exception
	{
		if(validated)
			// _log.info("Olympia Result: " + _playerOneName + " vs " + _playerTwoName + " ... aborted/tie due to crashes! (Double validate check)");
			return;
		validated = true;

		if(mode > 2)
		{
			_sm = new SystemMessage(SystemMessage.THE_GAME_HAS_BEEN_CANCELLED_BECAUSE_THE_OTHER_PARTY_DOES_NOT_MEET_THE_REQUIREMENTS_FOR_JOINING_THE_GAME);
			SystemMessage _sm2 = new SystemMessage(SystemMessage.YOU_CANT_JOIN_THE_OLYMPIAD_WITH_A_SUB_JOB_CHARACTER);
			for(L2Player pl : getPlayers())
				if(pl.getObjectId() == mode)
				{
					pl.sendPacket(_sm2);
					setWinner(pl.getOlympiadSide());
				}
				else
					pl.sendPacket(_sm);
		}
		else if(mode == 2)
		{
			_sm = new SystemMessage(SystemMessage.THE_GAME_HAS_BEEN_CANCELLED_BECAUSE_THE_OTHER_PARTY_ENDS_THE_GAME);
			for(L2Player pl : getPlayers())
				if(pl != null && pl.isOnline())
					pl.sendPacket(_sm);
			return;
		}

		if((_playerOne == null || _playerTwo == null) && mode == 1)
		{
			_sm = new SystemMessage(SystemMessage.YOU_WILL_GO_BACK_TO_THE_VILLAGE_IN_S1_SECOND_S);
			_sm.addNumber(20);
			broadcastMessage(_sm, true);
			return;
		}

		StatsSet playerOneStat;
		StatsSet playerTwoStat;

		playerOneStat = Olympiad._nobles.get(_playerOneID);
		playerTwoStat = Olympiad._nobles.get(_playerTwoID);

		/*
		 * if(playerOneStat == null) _log.warning("Player One Stats Null! Player Id:" + _playerOneID); if(playerTwoStat == null) _log.warning("Player Two Stats Null! Player Id:" + _playerTwoID);
		 */

		if(playerOneStat == null || playerTwoStat == null)
			return;

		int playerOneWin = playerOneStat.getInteger(Olympiad.COMP_WIN);
		int playerTwoWin = playerTwoStat.getInteger(Olympiad.COMP_WIN);
		int playerOneLoose = playerOneStat.getInteger(Olympiad.COMP_LOOSE);
		int playerTwoLoose = playerTwoStat.getInteger(Olympiad.COMP_LOOSE);
		int playerOnePlayed = playerOneStat.getInteger(Olympiad.COMP_DONE);
		int playerTwoPlayed = playerTwoStat.getInteger(Olympiad.COMP_DONE);
		int playerOnePoints = playerOneStat.getInteger(Olympiad.POINTS);
		int playerTwoPoints = playerTwoStat.getInteger(Olympiad.POINTS);

		_playerOne = L2World.getPlayer(_playerOneName);
		_playerTwo = L2World.getPlayer(_playerTwoName);
		if(_playerOne == null && _playerTwo == null)
		{
			Log.add("Olympia Result: " + _playerOneName + " vs " + _playerTwoName + " ... aborted/tie due to crashes! (Players check)", "olympiad");
			return;
		}

		MMOConnection<?> connOne = null;
		MMOConnection<?> connTwo = null;

		double playerOneHp = 0;
		try
		{
			if(_playerOne != null && _playerOne.isOnline() && _playerOne.getOlympiadGameId() != -1)
			{
				playerOneHp = _playerOne.getCurrentHp() + _playerOne.getCurrentCp();
				connOne = _playerOne.getNetConnection().getConnection();
			}
			else
			{
				if(_winner == 0)
					_winner = 1;
				_playerOne = null;
				playerOneHp = 0;
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			if(_winner == 0)
				_winner = 1;
			_playerOne = null;
			playerOneHp = 0;
		}

		double playerTwoHp = 0;
		try
		{
			if(_playerTwo != null && _playerTwo.isOnline() && _playerTwo.getOlympiadGameId() != -1)
			{
				playerTwoHp = _playerTwo.getCurrentHp() + _playerTwo.getCurrentCp();
				connTwo = _playerTwo.getNetConnection().getConnection();
			}
			else
			{
				if(_winner == 0)
					_winner = 2;
				_playerTwo = null;
				playerTwoHp = 0;
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			if(_winner == 0)
				_winner = 2;
			_playerTwo = null;
			playerTwoHp = 0;
		}

		_sm = new SystemMessage(SystemMessage.S1_HAS_WON_THE_GAME);
		SystemMessage _sm2 = new SystemMessage(SystemMessage.S1_HAS_GAINED_S2_OLYMPIAD_POINTS);
		SystemMessage _sm3 = new SystemMessage(SystemMessage.S1_HAS_LOST_S2_OLYMPIAD_POINTS);

		String result = "";

		if((_playerOne == null || connOne == null || connOne.isClosed()) && (_playerTwo == null || connTwo == null || connTwo.isClosed()))
		{
			Log.add("Olympia Result: " + _playerOneName + " vs " + _playerTwoName + " ... aborted/tie due to crashes! (Connection check)", "olympiad");
			_playerOne = null;
			_playerTwo = null;
			return;
		}

		if(_winner == 0)
			if(_playerTwo == null || connTwo == null || connTwo.isClosed())
				_winner = 2;
			else if(_playerOne == null || connOne == null || connOne.isClosed())
				_winner = 1;
		if(_winner == 0)
			if(_playerOneDamage < _playerTwoDamage)
				_winner = 2;
			else if(_playerOneDamage > _playerTwoDamage)
				_winner = 1;
		if(_winner == 2)
		{
			int pointDiff;
			if(_type == CompType.CLASSED)
				pointDiff = Math.min(playerOnePoints, playerTwoPoints) / 3;
			else
				pointDiff = Math.min(playerOnePoints, playerTwoPoints) / 5;
			if(pointDiff < 1)
				pointDiff = 1;
			playerOneStat.set(Olympiad.POINTS, playerOnePoints + pointDiff);
			playerTwoStat.set(Olympiad.POINTS, playerTwoPoints - pointDiff);
			playerOneStat.set(Olympiad.COMP_WIN, playerOneWin + 1);
			playerTwoStat.set(Olympiad.COMP_LOOSE, playerTwoLoose + 1);
			playerOneStat.set(Olympiad.COMP_DONE, playerOnePlayed + 1);
			playerTwoStat.set(Olympiad.COMP_DONE, playerTwoPlayed + 1);

			_sm.addString(_playerOneName);
			broadcastMessage(_sm, true);
			_sm2.addString(_playerOneName);
			_sm2.addNumber(pointDiff);
			broadcastMessage(_sm2, false);
			_sm3.addString(_playerTwoName);
			_sm3.addNumber(pointDiff);
			broadcastMessage(_sm3, false);

			try
			{
				result = "(" + (int) playerOneHp + "hp vs " + (int) playerTwoHp + "hp - " + (int) _playerOneDamage + " vs " + (int) _playerTwoDamage + ") " + _playerOneName + " win " + pointDiff + " points";
				L2ItemInstance item = _playerOne.getInventory().addItem(Olympiad.NOBLESSE_GATE_PASS_ID, 30, 0, "Olympiad");

				SystemMessage sm = new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S2_S1S);
				sm.addItemName(item.getItemId());
				sm.addNumber(30);
				_playerOne.sendPacket(sm);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		else if(_winner == 1)
		{
			int pointDiff;
			if(_type == CompType.CLASSED)
				pointDiff = Math.min(playerOnePoints, playerTwoPoints) / 3;
			else
				pointDiff = Math.min(playerOnePoints, playerTwoPoints) / 5;
			if(pointDiff < 1)
				pointDiff = 1;
			playerTwoStat.set(Olympiad.POINTS, playerTwoPoints + pointDiff);
			playerOneStat.set(Olympiad.POINTS, playerOnePoints - pointDiff);
			playerOneStat.set(Olympiad.COMP_LOOSE, playerOneLoose + 1);
			playerTwoStat.set(Olympiad.COMP_WIN, playerTwoWin + 1);
			playerOneStat.set(Olympiad.COMP_DONE, playerOnePlayed + 1);
			playerTwoStat.set(Olympiad.COMP_DONE, playerTwoPlayed + 1);

			_sm.addString(_playerTwoName);
			broadcastMessage(_sm, true);
			_sm2.addString(_playerTwoName);
			_sm2.addNumber(pointDiff);
			broadcastMessage(_sm2, true);
			_sm3.addString(_playerOneName);
			_sm3.addNumber(pointDiff);
			broadcastMessage(_sm3, true);

			try
			{
				result = "(" + (int) playerOneHp + "hp vs " + (int) playerTwoHp + "hp - " + (int) _playerOneDamage + " vs " + (int) _playerTwoDamage + ") " + _playerTwoName + " win " + pointDiff + " points";
				L2ItemInstance item = _playerTwo.getInventory().addItem(Olympiad.NOBLESSE_GATE_PASS_ID, 30, 0, "Olympiad");

				SystemMessage sm = new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S2_S1S);
				sm.addItemName(item.getItemId());
				sm.addNumber(30);
				_playerTwo.sendPacket(sm);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			playerOneStat.set(Olympiad.POINTS, playerOnePoints - 2);
			playerTwoStat.set(Olympiad.POINTS, playerTwoPoints - 2);
			result = "tie";
			_sm = new SystemMessage(SystemMessage.THE_GAME_ENDED_IN_A_TIE);
			broadcastMessage(_sm, true);
		}
		Log.add("Olympia Result: " + _playerOneName + " vs " + _playerTwoName + " ... " + result, "olympiad");

		Olympiad._nobles.remove(_playerOneID);
		Olympiad._nobles.remove(_playerTwoID);

		Olympiad._nobles.put(_playerOneID, playerOneStat);
		Olympiad._nobles.put(_playerTwoID, playerTwoStat);

		OlympiadDatabase.saveNobleData(_playerOneID);
		OlympiadDatabase.saveNobleData(_playerTwoID);

		_sm = new SystemMessage(SystemMessage.YOU_WILL_GO_BACK_TO_THE_VILLAGE_IN_S1_SECOND_S);
		_sm.addNumber(20);
		broadcastMessage(_sm, true);
	}

	public void deleteBuffers() throws Exception
	{
		if(_buffer != null)
		{
			_buffer.despawnAll();
			_buffer = null;
		}

		if(_buffer2 != null)
		{
			_buffer2.despawnAll();
			_buffer2 = null;
		}
	}

	public boolean makeCompetitionStart()
	{
		if(!checkPlayersOnline())
		{
			if(Config.DEBUG)
				_log.warning("CompStart: player is null");
			_aborted = true;
			return false;
		}

		_sm = new SystemMessage(SystemMessage.STARTS_THE_GAME);

		try
		{
			_playerOne.sendPacket(_sm);
			_playerTwo.sendPacket(_sm);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			_aborted = true;
			return false;
		}
		return true;
	}

	public int getId()
	{
		return _id;
	}

	public String getTitle()
	{
		String msg = "";
		msg += _playerOneName + " : " + _playerTwoName;
		return msg;
	}

	public L2Player[] getPlayers()
	{
		L2Player[] players = new L2Player[2];

		if(_playerOne == null || _playerTwo == null)
			return new L2Player[0];

		players[0] = _playerOne;
		players[1] = _playerTwo;

		return players;
	}

	public List<L2Player> getSpectators()
	{
		return _spectators;
	}

	public void addSpectator(L2Player spec)
	{
		_spectators.add(spec);
	}

	public void removeSpectator(L2Player spec)
	{
		if(_spectators != null && _spectators.contains(spec))
			_spectators.remove(spec);
	}

	public void clearSpectators()
	{
		if(_spectators != null)
		{
			for(L2Player pc : _spectators)
				try
				{
					if(!pc.inObserverMode())
						continue;
					pc.leaveOlympiadObserverMode();
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			_spectators.clear();
		}
	}

	private void broadcastMessage(SystemMessage sm, boolean toAll)
	{
		try
		{
			if(_playerOne != null)
				_playerOne.sendPacket(sm);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		try
		{
			if(_playerTwo != null)
				_playerTwo.sendPacket(sm);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		if(toAll && _spectators != null)
			for(L2Player spec : _spectators)
				try
				{
					if(spec != null)
						spec.sendPacket(sm);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
	}

	public void setWinner(int val)
	{
		_winner = val;
	}

	public void setStarted(byte val)
	{
		_started = val;
	}

	public byte getStarted()
	{
		return _started;
	}

	public void addDamage(int side, double damage)
	{
		if(side == 1)
			_playerOneDamage += damage;
		else
			_playerTwoDamage += damage;
	}

	public boolean checkPlayersOnline()
	{
		if(_playerOne != null && _playerTwo != null)
			return true;
		if(_playerOneName == null || _playerOneName.isEmpty() || _playerTwoName == null || _playerTwoName.isEmpty())
			return false;
		_playerOne = L2World.getPlayer(_playerOneName);
		_playerTwo = L2World.getPlayer(_playerTwoName);
		if(_playerOne == null || _playerTwo == null)
			return false;

		MMOConnection<?> connOne = _playerOne.getNetConnection().getConnection();
		MMOConnection<?> connTwo = _playerTwo.getNetConnection().getConnection();
		if(connOne == null || connOne.isClosed())
		{
			_playerOne = null;
			return false;
		}
		if(connTwo == null || connTwo.isClosed())
		{
			_playerTwo = null;
			return false;
		}
		return true;
	}

	public CompType getType()
	{
		return _type;
	}
}