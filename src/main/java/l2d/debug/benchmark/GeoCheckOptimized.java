package l2d.debug.benchmark;

import l2d.Config;
import l2d.game.geodata.GeoEngine;

public class GeoCheckOptimized
{
	public static void main(String[] args) throws Exception
	{
		common.init();
		Config.GEOFILES_PATTERN = "(\\d{2}_\\d{2})\\.l2j";
		Config.ALLOW_DOORS = false;
		Config.COMPACT_GEO = true;
		GeoEngine.loadGeo();
		common.PromptEnterToContinue();
	}
}