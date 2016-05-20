package com.lineage.game.serverpackets;

import java.util.Map;

import javolution.util.FastMap;

import com.lineage.game.communitybbs.Manager.TopBBSManager.rankers;
import com.lineage.game.model.entity.Hero;
import com.lineage.game.model.entity.olympiad.Olympiad;
import com.lineage.game.templates.StatsSet;
import com.lineage.util.GArray;

/**
 * Format: (ch) d [SdSdSdd]
 * d: size
 * [
 * S: hero name
 * d: hero class ID
 * S: hero clan name
 * d: hero clan crest id
 * S: hero ally name
 * d: hero Ally id
 * d: count
 * ]
 */
public class ExHeroList extends L2GameServerPacket
{
	private Map<Integer, StatsSet> _heroList;
	private GArray<rankers> _topList;


	public ExHeroList()
	{
		_heroList = Hero.getInstance().getHeroes();
	}

	public ExHeroList(FastMap<Integer, GArray<rankers>> top, int clas)
	{
		_topList = new GArray<rankers>();
		if(top.containsKey(clas))
			_topList.addAll(top.get(clas));
	}

	public ExHeroList(GArray<rankers> single_list)
	{
		_topList = new GArray<rankers>();		
		_topList.addAll(single_list);
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x23);

		if(_topList != null)
		{
			writeD(_topList.size());
			int rank = 0;
			for(rankers top : _topList)
			{
				rank++;
				writeS(rank+"# - "+top.name);
				writeD(top.class_id);
				writeS(top.clan_name);
				writeD(top.clan_id);
				writeS(top.ally_name);
				writeD(top.ally_id);
				writeD(top.kills);
			}
		}
		else
		{
			writeD(_heroList.size());
			for(Integer heroId : _heroList.keySet())
			{
				StatsSet hero = _heroList.get(heroId);
				writeS(hero.getString(Olympiad.CHAR_NAME));
				writeD(hero.getInteger(Olympiad.CLASS_ID));
				writeS(hero.getString(Hero.CLAN_NAME, ""));
				writeD(hero.getInteger(Hero.CLAN_CREST, 0));
				writeS(hero.getString(Hero.ALLY_NAME, ""));
				writeD(hero.getInteger(Hero.ALLY_CREST, 0));
				writeD(hero.getInteger(Hero.COUNT));
			}
		}
	}
}