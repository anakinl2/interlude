package l2d.game.model;

import java.util.concurrent.Future;
import java.util.logging.Logger;

import l2d.game.ThreadPoolManager;
import l2d.game.geodata.GeoEngine;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.model.instances.L2PenaltyMonsterInstance;
import l2d.game.serverpackets.ExFishingHpRegen;
import l2d.game.serverpackets.ExFishingStartCombat;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.ItemTable;
import l2d.game.tables.NpcTable;
import l2d.game.templates.L2NpcTemplate;
import l2d.util.Location;
import l2d.util.Rnd;

public class L2Fishing implements Runnable
{
	protected static Logger _log = Logger.getLogger(L2Fishing.class.getName());
	private L2Player _fisher;
	private int _time;
	private int _stop = 0;
	private int _gooduse = 0;
	private int _anim = 0;
	private int _mode = 0;
	private int _deceptiveMode = 0;
	private Future<?> _fishAItask;
	// Fish datas
	private int _fishID;
	private int _fishMaxHP;
	private int _fishCurHP;
	private double _regenHP;
	private boolean _isUpperGrade;

	@Override
	public void run()
	{
		final L2Player fisher = _fisher;
		if(fisher == null)
			return;
		if(_fishCurHP >= _fishMaxHP * 2)
		{
			// The fish got away
			fisher.sendPacket(new SystemMessage(SystemMessage.THE_FISH_GOT_AWAY));
			doDie(false);
		}
		else if(_time <= 0)
		{
			// Time is up, so that fish got away
			fisher.sendPacket(new SystemMessage(SystemMessage.TIME_IS_UP_SO_THAT_FISH_GOT_AWAY));
			doDie(false);
		}
		else
			AiTask();
	}

	// =========================================================
	public L2Fishing(final L2Player fisher, final FishData fish, final boolean isNoob, final boolean isUpperGrade)
	{
		_fisher = fisher;

		_fishMaxHP = fish.getHP();
		_fishCurHP = _fishMaxHP;
		_regenHP = fish.getHpRegen();
		_fishID = fish.getId();
		_time = fish.getCombatTime() / 1000;
		_isUpperGrade = isUpperGrade;
		int lureType;
		if(isUpperGrade)
		{
			_deceptiveMode = Rnd.chance(10) ? 1 : 0;
			lureType = 2;
		}
		else
		{
			_deceptiveMode = 0;
			lureType = isNoob ? 0 : 1;
		}
		_mode = Rnd.chance(20) ? 1 : 0;

		final ExFishingStartCombat efsc = new ExFishingStartCombat(fisher, _time, _fishMaxHP, _mode, lureType, _deceptiveMode);
		fisher.broadcastPacket(efsc);

		// Succeeded in getting a bite
		fisher.sendPacket(new SystemMessage(SystemMessage.SUCCEEDED_IN_GETTING_A_BITE));

		if(_fishAItask == null)
			_fishAItask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(this, 1000, 1000);

	}

	public void changeHp(final int hp, final int pen)
	{
		_fishCurHP -= hp;
		if(_fishCurHP < 0)
			_fishCurHP = 0;

		final L2Player fisher = _fisher;
		if(fisher != null)
			fisher.broadcastPacket(new ExFishingHpRegen(fisher, _time, _fishCurHP, _mode, _gooduse, _anim, pen, _deceptiveMode));

		_gooduse = 0;
		_anim = 0;
		if(_fishCurHP > _fishMaxHP * 2)
		{
			_fishCurHP = _fishMaxHP * 2;
			doDie(false);
		}
		else if(_fishCurHP == 0)
			doDie(true);
	}

	public void doDie(final boolean win)
	{
		if(_fishAItask != null)
		{
			_fishAItask.cancel(false);
			_fishAItask = null;
		}

		final L2Player fisher = _fisher;
		if(fisher == null)
			return;

		if(win)
			if(Rnd.chance(5))
				penaltyMonster();
			else
			{
				fisher.sendPacket(new SystemMessage(SystemMessage.SUCCEEDED_IN_FISHING));
				final L2ItemInstance item = ItemTable.getInstance().createItem(_fishID);
				fisher.getInventory().addItem(item);
				fisher.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_OBTAINED_S1).addItemName(_fishID));
			}
		fisher.endFishing(win);

		_fisher = null;
	}

	protected void AiTask()
	{
		_time--;

		if(_mode == 1 && _deceptiveMode == 0 || _mode == 0 && _deceptiveMode == 1)
			_fishCurHP += (int) _regenHP;

		if(_stop == 0)
		{
			_stop = 1;
			if(Rnd.chance(30))
				_mode = _mode == 0 ? 1 : 0;

			if(_isUpperGrade)
				if(Rnd.chance(10))
					_deceptiveMode = _deceptiveMode == 0 ? 1 : 0;
		}
		else
			_stop--;

		final ExFishingHpRegen efhr = new ExFishingHpRegen(_fisher, _time, _fishCurHP, _mode, 0, _anim, 0, _deceptiveMode);
		if(_anim != 0)
			_fisher.broadcastPacket(efhr);
		else
			_fisher.sendPacket(efhr);
	}

	public void UseRealing(final int dmg, final int pen)
	{
		final L2Player fisher = _fisher;
		if(fisher == null)
			return;
		_anim = 2;
		if(Rnd.chance(10))
		{
			fisher.sendPacket(new SystemMessage(SystemMessage.FISH_HAS_RESISTED));
			_gooduse = 0;
			changeHp(0, pen);
			return;
		}
		if(_mode == 1)
		{
			if(_deceptiveMode == 0)
			{
				// Reeling is successful, Damage: $s1
				fisher.sendPacket(new SystemMessage(SystemMessage.REELING_IS_SUCCESSFUL_DAMAGE_S1).addNumber(dmg));
				if(pen == 50)
					fisher.sendPacket(new SystemMessage(SystemMessage.YOUR_REELING_WAS_SUCCESSFUL_MASTERY_PENALTYS1_).addNumber(pen));

				_gooduse = 1;
				changeHp(dmg, pen);
			}
			else
			{
				// Reeling failed, Damage: $s1
				fisher.sendPacket(new SystemMessage(SystemMessage.REELING_FAILED_DAMAGE_S1).addNumber(dmg));
				_gooduse = 2;
				changeHp(-dmg, pen);
			}
		}
		else if(_deceptiveMode == 0)
		{
			// fisher failed, Damage: $s1
			fisher.sendPacket(new SystemMessage(SystemMessage.REELING_FAILED_DAMAGE_S1).addNumber(dmg));
			_gooduse = 2;
			changeHp(-dmg, pen);
		}
		else
		{
			// Reeling is successful, Damage: $s1
			fisher.sendPacket(new SystemMessage(SystemMessage.REELING_IS_SUCCESSFUL_DAMAGE_S1).addNumber(dmg));
			if(pen == 50)
				fisher.sendPacket(new SystemMessage(SystemMessage.REELING_IS_SUCCESSFUL_DAMAGE_S1).addNumber(pen));

			_gooduse = 1;
			changeHp(dmg, pen);
		}
	}

	public void UsePomping(final int dmg, final int pen)
	{
		final L2Player fisher = _fisher;
		if(fisher == null)
			return;
		_anim = 1;
		if(Rnd.chance(10))
		{
			fisher.sendPacket(new SystemMessage(SystemMessage.FISH_HAS_RESISTED));
			_gooduse = 0;
			changeHp(0, pen);
			return;
		}
		if(_mode == 0)
		{
			if(_deceptiveMode == 0)
			{
				// Pumping is successful. Damage: $s1
				fisher.sendPacket(new SystemMessage(SystemMessage.PUMPING_IS_SUCCESSFUL_DAMAGE_S1).addNumber(dmg));
				if(pen == 50)
					fisher.sendPacket(new SystemMessage(SystemMessage.YOUR_PUMPING_WAS_SUCCESSFUL_MASTERY_PENALTYS1_).addNumber(pen));

				_gooduse = 1;
				changeHp(dmg, pen);
			}
			else
			{
				// Pumping failed, Regained: $s1
				fisher.sendPacket(new SystemMessage(SystemMessage.PUMPING_FAILED_DAMAGE_S1).addNumber(dmg));
				_gooduse = 2;
				changeHp(-dmg, pen);
			}
		}
		else if(_deceptiveMode == 0)
		{
			// Pumping failed, Regained: $s1
			fisher.sendPacket(new SystemMessage(SystemMessage.PUMPING_FAILED_DAMAGE_S1).addNumber(dmg));
			_gooduse = 2;
			changeHp(-dmg, pen);
		}
		else
		{
			// Pumping is successful. Damage: $s1
			fisher.sendPacket(new SystemMessage(SystemMessage.PUMPING_IS_SUCCESSFUL_DAMAGE_S1).addNumber(dmg));
			if(pen == 50)
				fisher.sendPacket(new SystemMessage(SystemMessage.YOUR_PUMPING_WAS_SUCCESSFUL_MASTERY_PENALTYS1_).addNumber(pen));

			_gooduse = 1;
			changeHp(dmg, pen);
		}
	}

	private void penaltyMonster()
	{
		final L2Player fisher = _fisher;
		if(fisher == null)
			return;

		final int lvl = (int) Math.round(fisher.getLevel() * 0.1);
		int npcid;

		fisher.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_CAUGHT_A_MONSTER));
		switch(lvl)
		{
			case 0:
			case 1:
				npcid = 18319;
				break;
			case 2:
				npcid = 18320;
				break;
			case 3:
				npcid = 18321;
				break;
			case 4:
				npcid = 18322;
				break;
			case 5:
				npcid = 18323;
				break;
			case 6:
				npcid = 18324;
				break;
			case 7:
				npcid = 18325;
				break;
			case 8:
				npcid = 18326;
				break;
			default:
				npcid = 18319;
				break;
		}

		final L2NpcTemplate temp = NpcTable.getTemplate(npcid);
		if(temp != null)
		{
			L2Spawn spawn;
			try
			{
				spawn = new L2Spawn(temp);
				Location def = fisher.getFishLoc();
				if(!GeoEngine.canMoveWithCollision(fisher.getX(), fisher.getY(), fisher.getZ(), def.x, def.y, def.z))
					def = fisher.getLoc();
				spawn.setLoc(def);
				spawn.setAmount(1);
				spawn.setHeading(fisher.getHeading() - 32768);
				spawn.stopRespawn();
				final L2PenaltyMonsterInstance monster = (L2PenaltyMonsterInstance) spawn.doSpawn(true);
				if(fisher.getReflection().getId() != 0)
					monster.setReflection(fisher.getReflection());
				monster.SetPlayerToKill(fisher);
			}
			catch(final Exception e)
			{
				_log.warning("Could not spawn Penalty Monster " + npcid + ", exception: " + e);
				e.printStackTrace();
			}
		}
	}
}