package com.lineage.game.serverpackets;

import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;
import com.lineage.game.instancemanager.CastleManager;
import com.lineage.game.instancemanager.CastleManorManager.CropProcure;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2ItemInstance;

public class SellListProcure extends L2GameServerPacket
{
	private int _money;
	private Map<L2ItemInstance, Integer> _sellList = new FastMap<L2ItemInstance, Integer>(); //TODO: Long
	private List<CropProcure> _procureList = new FastList<CropProcure>();
	private int _castle;

	public SellListProcure(L2Player player, int castleId)
	{
		_money = player.getAdena();
		_castle = castleId;
		_procureList = CastleManager.getInstance().getCastleByIndex(_castle).getCropProcure(0);
		for(CropProcure c : _procureList)
		{
			L2ItemInstance item = player.getInventory().getItemByItemId(c.getId());
			if(item != null && c.getAmount() > 0)
				_sellList.put(item, c.getAmount());
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xE9);
		writeD(_money);
		writeD(0x00); // lease ?
		writeH(_sellList.size()); // list size

		for(L2ItemInstance item : _sellList.keySet())
		{
			writeH(item.getItem().getType1());
			writeD(item.getObjectId());
			writeD(item.getItemId());
			writeD(_sellList.get(item)); // count
			writeH(item.getItem().getType2ForPackets());
			writeH(0); // size of [dhhh]
			writeD(0); // price, u shouldnt get any adena for crops, only raw materials
		}
	}
}