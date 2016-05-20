package com.lineage.game.model.entity.SevenSignsFestival;

import com.lineage.util.Location;
import com.lineage.util.Rnd;

public class FestivalSpawn
{
	public Location loc;
	public int npcId;

	FestivalSpawn(Location loc)
	{
		this.loc = loc;
		// Generate a random heading if no positive one given.
		this.loc.h = loc.h < 0 ? Rnd.get(65536) : loc.h;
		npcId = -1;
	}

	FestivalSpawn(int[] spawnData)
	{
		loc = new Location(spawnData[0], spawnData[1], spawnData[2], spawnData[3] < 0 ? Rnd.get(65536) : spawnData[3]);
		if(spawnData.length > 4)
			npcId = spawnData[4];
		else
			npcId = -1;
	}
}