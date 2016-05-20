package l2d.game.serverpackets;

import javolution.util.FastList;
import l2d.game.instancemanager.CastleManager;
import l2d.game.instancemanager.CastleManorManager;
import l2d.game.instancemanager.CastleManorManager.CropProcure;
import l2d.game.model.L2Manor;
import l2d.game.model.entity.residence.Castle;

/**
 * format
 * dd[ddc[d]c[d]ddddddcddc]
 * dd[ddc[d]c[d]ddddQQcQQc] - Gracia Final
 */
public class ExShowCropSetting extends L2GameServerPacket
{
	private int _manorId;
	private int _count;
	private int[] _cropData; // data to send, size:_count*14

	public ExShowCropSetting(int manorId)
	{
		_manorId = manorId;
		Castle c = CastleManager.getInstance().getCastleByIndex(_manorId);
		FastList<Integer> crops = L2Manor.getInstance().getCropsForCastle(_manorId);
		_count = crops.size();
		_cropData = new int[_count * 14];
		int i = 0;
		for(int cr : crops)
		{
			_cropData[i * 14 + 0] = cr;
			_cropData[i * 14 + 1] = L2Manor.getInstance().getSeedLevelByCrop(cr);
			_cropData[i * 14 + 2] = L2Manor.getInstance().getRewardItem(cr, 1);
			_cropData[i * 14 + 3] = L2Manor.getInstance().getRewardItem(cr, 2);
			_cropData[i * 14 + 4] = L2Manor.getInstance().getCropPuchaseLimit(cr);
			_cropData[i * 14 + 5] = 0; // Looks like not used
			_cropData[i * 14 + 6] = L2Manor.getInstance().getCropBasicPrice(cr) * 60 / 100;
			_cropData[i * 14 + 7] = L2Manor.getInstance().getCropBasicPrice(cr) * 10;
			CropProcure cropPr = c.getCrop(cr, CastleManorManager.PERIOD_CURRENT);
			if(cropPr != null)
			{
				_cropData[i * 14 + 8] = cropPr.getStartAmount();
				_cropData[i * 14 + 9] = cropPr.getPrice();
				_cropData[i * 14 + 10] = cropPr.getReward();
			}
			else
			{
				_cropData[i * 14 + 8] = 0;
				_cropData[i * 14 + 9] = 0;
				_cropData[i * 14 + 10] = 0;
			}
			cropPr = c.getCrop(cr, CastleManorManager.PERIOD_NEXT);
			if(cropPr != null)
			{
				_cropData[i * 14 + 11] = cropPr.getStartAmount();
				_cropData[i * 14 + 12] = cropPr.getPrice();
				_cropData[i * 14 + 13] = cropPr.getReward();
			}
			else
			{
				_cropData[i * 14 + 11] = 0;
				_cropData[i * 14 + 12] = 0;
				_cropData[i * 14 + 13] = 0;
			}
			i++;
		}
	}

	@Override
	public void writeImpl()
	{
		writeC(EXTENDED_PACKET); // Id
		writeH(0x20); // SubId

		writeD(_manorId); // manor id
		writeD(_count); // size

		for(int i = 0; i < _count; i++)
		{
			writeD(_cropData[i * 14 + 0]); // crop id
			writeD(_cropData[i * 14 + 1]); // seed level

			writeC(1);
			writeD(_cropData[i * 14 + 2]); // reward 1 id

			writeC(1);
			writeD(_cropData[i * 14 + 3]); // reward 2 id

			writeD(_cropData[i * 14 + 4]); // next sale limit
			writeD(_cropData[i * 14 + 5]); // ???
			writeD(_cropData[i * 14 + 6]); // min crop price
			writeD(_cropData[i * 14 + 7]); // max crop price

			writeD(_cropData[i * 14 + 8]); // today buy
			writeD(_cropData[i * 14 + 9]); // today price
			writeC(_cropData[i * 14 + 10]); // today reward
			writeD(_cropData[i * 14 + 11]); // next buy
			writeD(_cropData[i * 14 + 12]); // next price

			writeC(_cropData[i * 14 + 13]); // next reward
		}
	}
}