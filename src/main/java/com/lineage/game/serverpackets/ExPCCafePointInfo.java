package com.lineage.game.serverpackets;

import com.lineage.game.model.L2Player;

/**
 * Format: ch ddcdc
 */
public class ExPCCafePointInfo extends L2GameServerPacket
{
	private int mAddPoint, mPeriodType, pointType, pcBangPoints;
	private int remainTime; // Оставшееся время в часах

	public ExPCCafePointInfo(L2Player player)
	{
		pcBangPoints = player.getPcBangPoints();
	}

	/* thx to kid ;)


	    public ExPCCafePointInfo(L2PcInstance user, int modify, boolean add, int hour, boolean _double)
	 {
	     _cha = user;
	     m_AddPoint = modify;
	     if(add)
	     {
	         m_PeriodType = 1;
	         PointType = 1;
	     } else
	     if(add && _double)
	     {
	         m_PeriodType = 1;
	         PointType = 0;
	     } else
	     {
	         m_PeriodType = 2;
	         PointType = 2;
	     }
	     RemainTime = hour;
	 }
	 */

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x32);
		writeD(pcBangPoints);
		writeD(mAddPoint);
		writeC(mPeriodType);
		writeD(remainTime);
		writeC(pointType);
	}
}