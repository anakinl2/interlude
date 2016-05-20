package com.lineage.game.clientpackets;

import java.util.List;

import com.lineage.game.instancemanager.RaidBossSpawnManager;
import javolution.util.FastList;
import javolution.util.FastMap;
import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.ExGetBossRecord;
import com.lineage.game.serverpackets.ExGetBossRecord.BossRecordInfo;

/**
 * Format: (ch) d
 */
public class RequestGetBossRecord extends L2GameClientPacket
{
	@SuppressWarnings("unused")
	private int _bossID;

	@Override
	public void readImpl()
	{
		_bossID = readD(); // always 0?
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		int totalPoints = 0;
		int ranking = 0;

		if(activeChar == null)
			return;

		List<BossRecordInfo> list = new FastList<BossRecordInfo>();
		FastMap<Integer, Integer> points = RaidBossSpawnManager.getInstance().getPointsByOwnerId(activeChar.getObjectId());
		if(points != null && !points.isEmpty())
			for(int bossId : points.keySet())
				switch(bossId)
				{
					case -1:
						ranking = points.get(bossId);
						break;
					case 0:
						totalPoints = points.get(bossId);
						break;
					default:
						list.add(new BossRecordInfo(bossId, points.get(bossId), 0));
				}

		activeChar.sendPacket(new ExGetBossRecord(ranking, totalPoints, list));
	}
}