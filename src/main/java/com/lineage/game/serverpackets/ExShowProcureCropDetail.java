package com.lineage.game.serverpackets;

import javolution.util.FastMap;
import com.lineage.game.instancemanager.CastleManager;
import com.lineage.game.instancemanager.CastleManorManager;
import com.lineage.game.instancemanager.CastleManorManager.CropProcure;
import com.lineage.game.model.entity.residence.Castle;

/**
 * format
 * dd[dddc]
 * dd[dQQc] - Gracia Final
 */
public class ExShowProcureCropDetail extends L2GameServerPacket
{
	private int _cropId;
	private FastMap<Integer, CropProcure> _castleCrops;

	public ExShowProcureCropDetail(int cropId)
	{
		_cropId = cropId;
		_castleCrops = new FastMap<Integer, CropProcure>();

		for(Castle c : CastleManager.getInstance().getCastles().values())
		{
			CropProcure cropItem = c.getCrop(_cropId, CastleManorManager.PERIOD_CURRENT);
			if(cropItem != null && cropItem.getAmount() > 0)
				_castleCrops.put(c.getId(), cropItem);
		}
	}

	@Override
	public void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x22);

		writeD(_cropId); // crop id
		writeD(_castleCrops.size()); // size

		for(int manorId : _castleCrops.keySet())
		{
			CropProcure crop = _castleCrops.get(manorId);
			writeD(manorId); // manor name

			writeD(crop.getAmount()); // buy residual
			writeD(crop.getPrice()); // buy price

			writeC(crop.getReward()); // reward type
		}
	}
}