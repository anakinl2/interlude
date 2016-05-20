package com.lineage.game.model.instances;

import java.util.concurrent.ScheduledFuture;

import com.lineage.game.ThreadPoolManager;
import com.lineage.game.instancemanager.FourSepulchersManager;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Skill;
import com.lineage.game.serverpackets.NpcSay;
import com.lineage.game.tables.SkillTable;
import com.lineage.game.templates.L2NpcTemplate;

/**
 * L2SepulcherMonsterInstance
 * 
 * @author: Ameron
 */
public class L2SepulcherMonsterInstance extends L2MonsterInstance
{
	public int mysteriousBoxId = 0;
	private ScheduledFuture _victimShout;
	private ScheduledFuture _victimSpawnKeyBoxTask;
	private ScheduledFuture _changeImmortalTask;
	private ScheduledFuture _onDeadEventTask;

	public L2SepulcherMonsterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();
		switch(getNpcId())
		{
			case 18150:
			case 18151:
			case 18152:
			case 18153:
			case 18154:
			case 18155:
			case 18156:
			case 18157:

				if(_victimSpawnKeyBoxTask != null)
					_victimSpawnKeyBoxTask.cancel(true);
				_victimSpawnKeyBoxTask = ThreadPoolManager.getInstance().scheduleEffect(new VictimSpawnKeyBox(this), 300000);
				if(_victimShout != null)
					_victimShout.cancel(true);
				_victimShout = ThreadPoolManager.getInstance().scheduleEffect(new VictimShout(this), 5000);
				break;

			case 18196:
			case 18197:
			case 18198:
			case 18199:
			case 18200:
			case 18201:
			case 18202:
			case 18203:
			case 18204:
			case 18205:
			case 18206:
			case 18207:
			case 18208:
			case 18209:
			case 18210:
			case 18211:
				break;

			case 18231:
			case 18232:
			case 18233:
			case 18234:
			case 18235:
			case 18236:
			case 18237:
			case 18238:
			case 18239:
			case 18240:
			case 18241:
			case 18242:
			case 18243:
				if(_changeImmortalTask != null)
					_changeImmortalTask.cancel(true);
				_changeImmortalTask = ThreadPoolManager.getInstance().scheduleEffect(new ChangeImmortal(this), 1600);

				break;
			case 18256:
				//
				break;
			case 25339:
			case 25342:
			case 25346:
			case 25349:
				// setIsRaid(true);
				break;
		}
	}

	@Override
	public void doDie(L2Character killer)
	{
		switch(getNpcId())
		{
			case 18120:
			case 18121:
			case 18122:
			case 18123:
			case 18124:
			case 18125:
			case 18126:
			case 18127:
			case 18128:
			case 18129:
			case 18130:
			case 18131:
			case 18149:
			case 18158:
			case 18159:
			case 18160:
			case 18161:
			case 18162:
			case 18163:
			case 18164:
			case 18165:
			case 18183:
			case 18184:
			case 18212:
			case 18213:
			case 18214:
			case 18215:
			case 18216:
			case 18217:
			case 18218:
			case 18219:
				if(_onDeadEventTask != null)
					_onDeadEventTask.cancel(true);
				_onDeadEventTask = ThreadPoolManager.getInstance().scheduleEffect(new OnDeadEvent(this), 3500);
				break;

			case 18150:
			case 18151:
			case 18152:
			case 18153:
			case 18154:
			case 18155:
			case 18156:
			case 18157:
				if(_victimSpawnKeyBoxTask != null)
				{
					_victimSpawnKeyBoxTask.cancel(true);
					_victimSpawnKeyBoxTask = null;
				}
				if(_onDeadEventTask != null)
					_onDeadEventTask.cancel(true);
				_onDeadEventTask = ThreadPoolManager.getInstance().scheduleEffect(new OnDeadEvent(this), 3500);
				if(_victimShout != null)
				{
					_victimShout.cancel(true);
					_victimShout = null;
				}
				break;

			case 18141:
			case 18142:
			case 18143:
			case 18144:
			case 18145:
			case 18146:
			case 18147:
			case 18148:
				if(FourSepulchersManager.getInstance().isViscountMobsAnnihilated(mysteriousBoxId))
				{
					if(_onDeadEventTask != null)
						_onDeadEventTask.cancel(true);
					_onDeadEventTask = ThreadPoolManager.getInstance().scheduleEffect(new OnDeadEvent(this), 3500);
				}
				break;

			case 18220:
			case 18221:
			case 18222:
			case 18223:
			case 18224:
			case 18225:
			case 18226:
			case 18227:
			case 18228:
			case 18229:
			case 18230:
			case 18231:
			case 18232:
			case 18233:
			case 18234:
			case 18235:
			case 18236:
			case 18237:
			case 18238:
			case 18239:
			case 18240:
				if(FourSepulchersManager.getInstance().isDukeMobsAnnihilated(mysteriousBoxId))
				{
					if(_onDeadEventTask != null)
						_onDeadEventTask.cancel(true);
					_onDeadEventTask = ThreadPoolManager.getInstance().scheduleEffect(new OnDeadEvent(this), 3500);
				}
				break;

			case 25339:
			case 25342:
			case 25346:
			case 25349:
				// see L2SepulcherBossInstance
				break;
		}
	}

	@Override
	public void deleteMe()
	{
		if(_victimSpawnKeyBoxTask != null)
		{
			_victimSpawnKeyBoxTask.cancel(true);
			_victimSpawnKeyBoxTask = null;
		}
		if(_onDeadEventTask != null)
		{
			_onDeadEventTask.cancel(true);
			_onDeadEventTask = null;
		}

		super.deleteMe();
	}

	private class VictimShout implements Runnable
	{
		private L2SepulcherMonsterInstance _activeChar;

		public VictimShout(L2SepulcherMonsterInstance activeChar)
		{
			_activeChar = activeChar;
		}

		@Override
		public void run()
		{
			if(_activeChar.isDead())
				return;

			if(!_activeChar.isVisible())
				return;

			broadcastPacket(new NpcSay(_activeChar, 0, "прости меня!"));
		}
	}

	private class VictimSpawnKeyBox implements Runnable
	{
		private L2SepulcherMonsterInstance _activeChar;

		public VictimSpawnKeyBox(L2SepulcherMonsterInstance activeChar)
		{
			_activeChar = activeChar;
		}

		@Override
		public void run()
		{
			if(_activeChar.isDead())
				return;

			if(!_activeChar.isVisible())
				return;

			FourSepulchersManager.getInstance().spawnKeyBox(_activeChar);
			broadcastPacket(new NpcSay(_activeChar, 0, "спасибо что спас меня!"));
			if(_victimShout != null)
				_victimShout.cancel(true);
		}
	}

	private class OnDeadEvent implements Runnable
	{
		L2SepulcherMonsterInstance _activeChar;

		public OnDeadEvent(L2SepulcherMonsterInstance activeChar)
		{
			_activeChar = activeChar;
		}

		@Override
		public void run()
		{
			switch(_activeChar.getNpcId())
			{
				case 18120:
				case 18121:
				case 18122:
				case 18123:
				case 18124:
				case 18125:
				case 18126:
				case 18127:
				case 18128:
				case 18129:
				case 18130:
				case 18131:
				case 18149:
				case 18158:
				case 18159:
				case 18160:
				case 18161:
				case 18162:
				case 18163:
				case 18164:
				case 18165:
				case 18183:
				case 18184:
				case 18212:
				case 18213:
				case 18214:
				case 18215:
				case 18216:
				case 18217:
				case 18218:
				case 18219:
					FourSepulchersManager.getInstance().spawnKeyBox(_activeChar);
					break;
				case 18150:
				case 18151:
				case 18152:
				case 18153:
				case 18154:
				case 18155:
				case 18156:
				case 18157:
					FourSepulchersManager.getInstance().spawnExecutionerOfHalisha(_activeChar);
					break;
				case 18141:
				case 18142:
				case 18143:
				case 18144:
				case 18145:
				case 18146:
				case 18147:
				case 18148:
					FourSepulchersManager.getInstance().spawnMonster(_activeChar.mysteriousBoxId);
					break;
				case 18220:
				case 18221:
				case 18222:
				case 18223:
				case 18224:
				case 18225:
				case 18226:
				case 18227:
				case 18228:
				case 18229:
				case 18230:
				case 18231:
				case 18232:
				case 18233:
				case 18234:
				case 18235:
				case 18236:
				case 18237:
				case 18238:
				case 18239:
				case 18240:
					FourSepulchersManager.getInstance().spawnArchonOfHalisha(_activeChar.mysteriousBoxId);
					break;
				case 25339:
				case 25342:
				case 25346:
				case 25349:
					// FourSepulchersManager.getInstance().spawnEmperorsGraveNpc(_activeChar.mysteriousBoxId);
					break;
			}
		}
	}

	private class ChangeImmortal implements Runnable
	{
		private L2SepulcherMonsterInstance activeChar;

		public ChangeImmortal(L2SepulcherMonsterInstance mob)
		{
			activeChar = mob;
		}

		@Override
		public void run()
		{
			L2Skill fp = SkillTable.getInstance().getInfo(4616, 1); // Invulnerable by petrification
			fp.getEffects(activeChar, activeChar, false, false);
		}
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return true;
	}
}