package l2d.game.model;

import l2d.game.ThreadPoolManager;
import com.lineage.util.Location;

public class L2ClanGate
{
	private static final long DISAPPEAR_TIME_MSEC = 2 * 60 * 1000; // время жизни врат - 2 min
	private boolean _isActive = false; // активны ли врата
	private Location _gateLoc = null; // точка телепорта
	private L2Clan _gateClan = null; // клан, для которого устанавливается клан гейт

	public L2ClanGate(L2Player creator)
	{
		_gateLoc = creator.getLoc();
		_gateClan = creator.getClan();
		// создается только клан лидером
		if(_gateClan.getLeaderId() != creator.getObjectId())
			return;
		ThreadPoolManager.getInstance().scheduleGeneral(new L2ClanGateDisappearTask(this), DISAPPEAR_TIME_MSEC);
		_isActive = true;
		synchronized (_gateClan)
		{
			_gateClan.setClanGate(this);
		}
	}

	public boolean destroyClanGate()
	{
		if(!_isActive)
			return false;
		_isActive = false;
		_gateLoc = null;
		synchronized (_gateClan)
		{
			_gateClan.setClanGate(null);
		}
		_gateClan = null;
		return true;
	}

	public boolean teleportMemberThroughGate(L2Player player)
	{
		// только через активные врата
		if(!_isActive)
			return false;
		// только члена клана
		if(player.getClanId() != _gateClan.getClanId())
			return false;
		// телепорт
		player.teleToLocation(_gateLoc);
		return true;
	}

	public class L2ClanGateDisappearTask implements Runnable
	{
		private L2ClanGate _gate;

		public L2ClanGateDisappearTask(L2ClanGate gate)
		{
			_gate = gate;
		}

		@Override
		public void run()
		{
			_gate.destroyClanGate();
		}
	}
}
