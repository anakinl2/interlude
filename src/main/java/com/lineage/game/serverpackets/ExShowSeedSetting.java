package com.lineage.game.serverpackets;

import javolution.util.FastList;
import com.lineage.game.instancemanager.CastleManager;
import com.lineage.game.instancemanager.CastleManorManager;
import com.lineage.game.instancemanager.CastleManorManager.SeedProduction;
import com.lineage.game.model.L2Manor;
import com.lineage.game.model.entity.residence.Castle;

/**
 * format
 * dd[ddc[d]c[d]dddddddd]
 * dd[ddc[d]c[d]ddddQQQQ] - Gracia Final
 */
public class ExShowSeedSetting extends L2GameServerPacket
{
	private int _manorId;
	private int _count;
	private int[] _seedData; // data to send, size:_count*12

	public ExShowSeedSetting(int manorId)
	{
		_manorId = manorId;
		Castle c = CastleManager.getInstance().getCastleByIndex(_manorId);
		FastList<Integer> seeds = L2Manor.getInstance().getSeedsForCastle(_manorId);
		_count = seeds.size();
		_seedData = new int[_count * 12];
		int i = 0;
		for(int s : seeds)
		{
			_seedData[i * 12 + 0] = s;
			_seedData[i * 12 + 1] = L2Manor.getInstance().getSeedLevel(s);
			_seedData[i * 12 + 2] = L2Manor.getInstance().getRewardItemBySeed(s, 1);
			_seedData[i * 12 + 3] = L2Manor.getInstance().getRewardItemBySeed(s, 2);
			_seedData[i * 12 + 4] = L2Manor.getInstance().getSeedSaleLimit(s);
			_seedData[i * 12 + 5] = L2Manor.getInstance().getSeedBuyPrice(s);
			_seedData[i * 12 + 6] = L2Manor.getInstance().getSeedBasicPrice(s) * 60 / 100;
			_seedData[i * 12 + 7] = L2Manor.getInstance().getSeedBasicPrice(s) * 10;
			SeedProduction seedPr = c.getSeed(s, CastleManorManager.PERIOD_CURRENT);
			if(seedPr != null)
			{
				_seedData[i * 12 + 8] = seedPr.getStartProduce();
				_seedData[i * 12 + 9] = seedPr.getPrice();
			}
			else
			{
				_seedData[i * 12 + 8] = 0;
				_seedData[i * 12 + 9] = 0;
			}
			seedPr = c.getSeed(s, CastleManorManager.PERIOD_NEXT);
			if(seedPr != null)
			{
				_seedData[i * 12 + 10] = seedPr.getStartProduce();
				_seedData[i * 12 + 11] = seedPr.getPrice();
			}
			else
			{
				_seedData[i * 12 + 10] = 0;
				_seedData[i * 12 + 11] = 0;
			}
			i++;
		}
	}

	@Override
	public void writeImpl()
	{
		writeC(EXTENDED_PACKET); // Id
		writeH(0x1F); // SubId

		writeD(_manorId); // manor id
		writeD(_count); // size

		for(int i = 0; i < _count; i++)
		{
			writeD(_seedData[i * 12 + 0]); // seed id
			writeD(_seedData[i * 12 + 1]); // level

			writeC(1);
			writeD(_seedData[i * 12 + 2]); // reward 1 id

			writeC(1);
			writeD(_seedData[i * 12 + 3]); // reward 2 id

			writeD(_seedData[i * 12 + 4]); // next sale limit
			writeD(_seedData[i * 12 + 5]); // price for castle to produce 1
			writeD(_seedData[i * 12 + 6]); // min seed price
			writeD(_seedData[i * 12 + 7]); // max seed price

			writeD(_seedData[i * 12 + 8]); // today sales
			writeD(_seedData[i * 12 + 9]); // today price
			writeD(_seedData[i * 12 + 10]); // next sales
			writeD(_seedData[i * 12 + 11]); // next price
		}
	}
}