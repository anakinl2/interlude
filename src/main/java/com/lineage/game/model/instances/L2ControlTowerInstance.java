package com.lineage.game.model.instances;

import static com.lineage.game.model.L2Zone.ZoneType.Siege;
import com.lineage.game.idfactory.IdFactory;
import com.lineage.game.instancemanager.SiegeManager;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Clan;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.entity.siege.Siege;
import com.lineage.game.model.entity.siege.castle.CastleSiege;
import com.lineage.game.serverpackets.MyTargetSelected;
import com.lineage.game.serverpackets.StatusUpdate;
import com.lineage.game.serverpackets.ValidateLocation;
import com.lineage.game.tables.NpcTable;
import com.lineage.game.templates.L2NpcTemplate;

public class L2ControlTowerInstance extends L2NpcInstance
{
	private CastleSiege _siege;

	private L2FakeTowerInstance _fakeTower;

	private int _maxHp;

	@Override
	public int getMaxHp()
	{
		return _maxHp;
	}

	public L2ControlTowerInstance(int objectId, L2NpcTemplate template, CastleSiege siege, int maxHp)
	{
		super(objectId, template);
		_siege = siege;
		_maxHp = maxHp;
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		if(attacker == null)
			return false;
		L2Player player = attacker.getPlayer();
		if(player == null)
			return false;
		L2Clan clan = player.getClan();
		return !(clan != null && _siege == clan.getSiege() && clan.isDefender());
	}

	public boolean isAttackable(L2Character attacker)
	{
		return isAutoAttackable(attacker);
	}

	@Override
	public void onAction(L2Player player)
	{
		if(this != player.getTarget())
		{
			player.setTarget(this);
			player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));
			StatusUpdate su = new StatusUpdate(getObjectId());
			su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
			su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
			player.sendPacket(su);
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));
			if(isAutoAttackable(player))
				player.getAI().Attack(this, false);
			else
				player.sendActionFailed();
		}
	}

	/**
	 * Вызывает обработку смерти у вышек.
	 * @param killer убийца
	 */
	@Override
	public void doDie(L2Character killer)
	{
		onDeath();
		super.doDie(killer);
	}

	/**
	 * Спавнит фэйковую вышку на месте умершей
	 */
	@Override
	public void onDecay()
	{
		super.onDecay();
		spawnFakeTower();
	}

	/**
	 * Убирает фэйковую вышку на месте новорожденной
	 */
	@Override
	public void spawnMe()
	{
		unSpawnFakeTower();
		super.spawnMe();
	}

	/**
	 * Обработка умирания вышки
	 */
	public void onDeath()
	{
		if(isInZone(Siege))
		{
			Siege siege = SiegeManager.getSiege(this, true);
			if(siege != null)
				siege.killedCT();
		}
	}

	/**
	 * Спавнит фэйковую вышку на месте умершей настоящей.
	 * Создается новый инстанс, и привязывается к текущему инстансу.
	 */
	public void spawnFakeTower()
	{
		if(_fakeTower == null)
		{
			L2FakeTowerInstance tower = new L2FakeTowerInstance(IdFactory.getInstance().getNextId(), NpcTable.getTemplate(getFakeTowerNpcId()));
			tower.spawnMe(getLoc());
			_fakeTower = tower;
		}
		else
		{
			_fakeTower.decayMe();
			_fakeTower.spawnMe();
		}
	}

	/**
	 * Убирает с мира фэйковую вышку которая относится к данному инстансу.
	 * Ссылка на обьект не обнуляется, т.к. он еше будет использован в перспективе
	 */
	public void unSpawnFakeTower()
	{
		if(_fakeTower == null)
			return;

		_fakeTower.decayMe();
	}

	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}

	/**
	 * Осадные вышки должны быть уязвимы во время осады, во время осады включается осадная зона
	 * Вывод - если не в осадной зоне, то неуязвимая
	 * @return уязвимая ли вышка
	 */
	@Override
	public boolean isInvul()
	{
		Siege siege = SiegeManager.getSiege(this, true);
		return siege == null || !siege.isInProgress();
	}

	@Override
	public boolean isFearImmune()
	{
		return true;
	}

	@Override
	public boolean isParalyzeImmune()
	{
		return true;
	}

	@Override
	public boolean isLethalImmune()
	{
		return true;
	}

	/**
	 * Возвращает ID Фэйковой вышки которая спавнится после смерти настоящей.
	 * Для Life Control Tower это 13003
	 * Для Flame Control Tower это 13005
	 * @return Fake Tower NPC ID
	 */
	private int getFakeTowerNpcId()
	{
		return getNpcId() + 1;
	}
}