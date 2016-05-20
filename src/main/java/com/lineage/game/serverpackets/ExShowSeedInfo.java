package com.lineage.game.serverpackets;

import javolution.util.FastList;
import com.lineage.game.instancemanager.CastleManorManager.SeedProduction;
import com.lineage.game.model.L2Manor;

/**
 * format
 * cddd[dddddc[d]c[d]]
 * cddd[dQQQdc[d]c[d]] - Gracia Final
 */
public class ExShowSeedInfo extends L2GameServerPacket
{
	private FastList<SeedProduction> _seeds;
	private int _manorId;

	public ExShowSeedInfo(int manorId, FastList<SeedProduction> seeds)
	{
		_manorId = manorId;
		_seeds = seeds;
		if(_seeds == null)
			_seeds = new FastList<SeedProduction>();
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET); // Id
		writeH(0x1C); // SubId
		writeC(0);
		writeD(_manorId); // Manor ID
		writeD(0);
		writeD(_seeds.size());
		for(SeedProduction seed : _seeds)
		{
			writeD(seed.getId()); // Seed id
			writeD(seed.getCanProduce()); // Left to buy
			writeD(seed.getStartProduce()); // Started amount
			writeD(seed.getPrice()); // Sell Price

			writeD(L2Manor.getInstance().getSeedLevel(seed.getId())); // Seed Level

			writeC(1); // reward 1 Type
			writeD(L2Manor.getInstance().getRewardItemBySeed(seed.getId(), 1)); // Reward 1 Type Item Id

			writeC(1); // reward 2 Type
			writeD(L2Manor.getInstance().getRewardItemBySeed(seed.getId(), 2)); // Reward 2 Type Item Id
		}
	}
}