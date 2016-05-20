package l2d.game.model.instances;

import java.lang.ref.WeakReference;

import l2d.game.ai.CtrlEvent;
import l2d.game.ai.CtrlIntention;
import l2d.game.clientpackets.Say2C;
import l2d.game.model.L2Character;
import l2d.game.model.L2Player;
import l2d.game.model.L2Spawn;
import l2d.game.serverpackets.Say2;
import l2d.game.tables.SpawnTable;
import l2d.game.templates.L2NpcTemplate;
import com.lineage.util.Rnd;

public class L2PenaltyMonsterInstance extends L2MonsterInstance
{
	private WeakReference<L2Player> ptk;

	public L2PenaltyMonsterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public L2Character getMostHated()
	{
		L2Player p = getPtk();
		L2Character p2 = super.getMostHated();
		if(p == null)
			return p2;
		if(p2 == null)
			return p;
		return getDistance3D(p) > getDistance3D(p2) ? p2 : p;
	}

	// FIXME: Never used...
	public void NotifyPlayerDead()
	{
		// Monster kill player and can by deleted
		deleteMe();

		L2Spawn spawn = getSpawn();
		if(spawn != null)
		{
			spawn.stopRespawn();
			SpawnTable.getInstance().deleteSpawn(spawn, false);
		}
	}

	public void SetPlayerToKill(L2Player ptk)
	{
		setPtk(ptk);
		if(Rnd.get(100) <= 80)
			broadcastPacket(new Say2(getObjectId(), Say2C.ALL, getName(), "mmm your bait was delicious"));
		getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, ptk, 10);
		getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, ptk);
	}

	@Override
	public void doDie(L2Character killer)
	{
		if(Rnd.get(100) <= 75)
		{
			Say2 cs = new Say2(getObjectId(), Say2C.ALL, getName(), "I will tell fishes not to take your bait");
			broadcastPacket(cs);
		}
		super.doDie(killer);
	}

	public L2Player getPtk()
	{
		if(ptk == null)
			return null;

		L2Player p = ptk.get();
		if(p == null)
			ptk = null;

		return p;
	}

	public void setPtk(L2Player ptk)
	{
		this.ptk = new WeakReference<L2Player>(ptk);
	}
}