package l2d.game.serverpackets;

import javolution.util.FastList;
import l2d.game.instancemanager.CastleManorManager.CropProcure;
import l2d.game.model.L2Manor;

/**
 * Format:
 * cddd[ddddcdc[d]c[d]]
 *
 */

public class ExShowCropInfo extends L2GameServerPacket
{
	private FastList<CropProcure> _crops;
	private int _manorId;

	public ExShowCropInfo(int manorId, FastList<CropProcure> crops)
	{
		_manorId = manorId;
		_crops = crops;
		if(_crops == null)
			_crops = new FastList<CropProcure>();
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET); // Id
		writeH(0x1D); // SubId
		writeC(0);
		writeD(_manorId); // Manor ID
		writeD(0);
		writeD(_crops.size());
		for(CropProcure crop : _crops)
		{
			writeD(crop.getId()); // Crop id
			writeD(crop.getAmount()); // Buy residual
			writeD(crop.getStartAmount()); // Buy
			writeD(crop.getPrice()); // Buy price
			writeC(crop.getReward()); // Reward
			writeD(L2Manor.getInstance().getSeedLevelByCrop(crop.getId())); // Seed Level

			writeC(1); // rewrad 1 Type
			writeD(L2Manor.getInstance().getRewardItem(crop.getId(), 1)); // Rewrad 1 Type Item Id

			writeC(1); // rewrad 2 Type
			writeD(L2Manor.getInstance().getRewardItem(crop.getId(), 2)); // Rewrad 2 Type Item Id
		}
	}
}