package com.lineage.game.serverpackets;

import javolution.util.FastList;
import javolution.util.FastMap;
import com.lineage.game.instancemanager.CastleManorManager.CropProcure;
import com.lineage.game.model.L2Manor;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2ItemInstance;

/**
 * format
 * dd[dddc[d]c[d]dddcd]
 * dd[dddc[d]c[d]dQQcQ] - Gracia Final
 */
public class ExShowSellCropList extends L2GameServerPacket
{
	private int _manorId = 1;
	private FastMap<Integer, L2ItemInstance> _cropsItems;
	private FastMap<Integer, CropProcure> _castleCrops;

	public ExShowSellCropList(L2Player player, int manorId, FastList<CropProcure> crops)
	{
		_manorId = manorId;
		_castleCrops = new FastMap<Integer, CropProcure>();
		_cropsItems = new FastMap<Integer, L2ItemInstance>();

		FastList<Integer> allCrops = L2Manor.getInstance().getAllCrops();
		for(int cropId : allCrops)
		{
			L2ItemInstance item = player.getInventory().getItemByItemId(cropId);
			if(item != null)
				_cropsItems.put(cropId, item);
		}

		for(CropProcure crop : crops)
			if(_cropsItems.containsKey(crop.getId()) && crop.getAmount() > 0)
				_castleCrops.put(crop.getId(), crop);

	}

	@Override
	public void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x21);

		writeD(_manorId); // manor id
		writeD(_cropsItems.size()); // size

		for(L2ItemInstance item : _cropsItems.values())
		{
			writeD(item.getObjectId()); // Object id
			writeD(item.getItemId()); // crop id
			writeD(L2Manor.getInstance().getSeedLevelByCrop(item.getItemId())); // seed level

			writeC(1);
			writeD(L2Manor.getInstance().getRewardItem(item.getItemId(), 1)); // reward 1 id

			writeC(1);
			writeD(L2Manor.getInstance().getRewardItem(item.getItemId(), 2)); // reward 2 id

			if(_castleCrops.containsKey(item.getItemId()))
			{
				CropProcure crop = _castleCrops.get(item.getItemId());
				writeD(_manorId); // manor

				writeD(crop.getAmount()); // buy residual
				writeD(crop.getPrice()); // buy price
				writeC(crop.getReward()); // reward
			}
			else
			{
				writeD(0xFFFFFFFF); // manor
				writeD(0); // buy residual
				writeD(0); // buy price
				writeC(0); // reward
			}
			writeD(item.getIntegerLimitedCount());
		}
	}
}