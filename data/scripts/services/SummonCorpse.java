package services;

import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.util.Files;
import com.lineage.util.GArray;
import com.lineage.util.Location;
import com.lineage.util.Rnd;

/**
 * Используется на Primeval Isle NPC Vervato (id: 32104)
 * 
 * @Date: 27/6/2007
 */
public class SummonCorpse extends Functions implements ScriptFile
{
	public static L2Object self;
	public static L2Object npc;
	private static int SUMMON_PRICE = 200000;

	private static String EnFilePatch = "data/html/default/";

	public void onLoad()
	{}

	public void onReload()
	{}

	public void onShutdown()
	{}

	/**
	 * Телепортирует все труппы, находящиеся в группе в данный момент
	 * 
	 * @return
	 */
	public void doSummon()
	{
		L2Player player = (L2Player) self;

		if( !player.isInParty())
		{
				show(Files.read(EnFilePatch + "32104-2.htm", player), player);
			return;
		}

		int counter = 0;
		GArray<L2Player> partyMembers = player.getParty().getPartyMembers();
		for(L2Player partyMember : partyMembers)
			if(partyMember != null && partyMember.isDead())
			{
				counter++;
				if(player.getAdena() < SUMMON_PRICE)
				{
						show(Files.read(EnFilePatch + "32104-3.htm", player), player);
					return;
				}
				player.reduceAdena(SUMMON_PRICE);
				Location coords = new Location(11255 + Rnd.get( -20, 20), -23370 + Rnd.get( -20, 20), -3649);
				partyMember.summonCharacterRequest(player.getName(), coords, 0);
			}

		if(counter == 0)
				show(Files.read(EnFilePatch + "32104-2.htm", player), player);
		else
			show(Files.read(EnFilePatch + "32104-4.htm", player), player);
	}
}