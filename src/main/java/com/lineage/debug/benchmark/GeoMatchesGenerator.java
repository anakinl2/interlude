package com.lineage.debug.benchmark;

import com.lineage.Config;
import com.lineage.game.geodata.GeoEngine;

public class GeoMatchesGenerator
{
	public static void main(String[] args) throws Exception
	{
		common.init();
		Config.GEOFILES_PATTERN = "(\\d{2}_\\d{2})\\.l2j";
		Config.ALLOW_DOORS = false;
		GeoEngine.loadGeo();
		common.GC();
		GeoEngine.genBlockMatches(0); //TODO
		if(common.YesNoPrompt("Do you want to delete temproary geo checksums files?"))
			GeoEngine.deleteChecksumFiles();
		common.PromptEnterToContinue();
	}
}