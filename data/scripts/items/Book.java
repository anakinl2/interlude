package items;

import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.handler.IItemHandler;
import com.lineage.game.handler.ItemHandler;
import com.lineage.game.model.L2Playable;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.serverpackets.RadarControl;
import com.lineage.util.Location;

public class Book implements IItemHandler, ScriptFile
{
	private static final int[] _itemIds = {
			5588,	// Tutorial Guide
			6317,	// Mixing Manual
			7561,	// Fishing Manual
			7063,	// Map - Forest of the Dead
			7064,	// Lidia''s Diary
			7065,	// Lidia''s Letter
			7066,	// Contract
			7082,	// Guild Report
			7083,	// Guild Report
			7084,	// Guild Report
			7085,	// Guild Report
			7086,	// Guild Report
			7087,	// Tattered Book
			7088,	// Tattered Book
			7089,	// Tattered Book
			7090,	// Tattered Book
			7091,	// Guild Report
			7092,	// Guild Report
			7093,	// Sculpture of the Fallen Knight
			7094,	// Record of Mother Tree Guardian
			7095,	// Criticism of Biel''s Song
			7096,	// The Last Days of Swordsman Iron
			7097,	// Contemplation of White Wing Army
			7098,	// Record of Traitor Muhark
			7099,	// Secret Book of Khavatari
			7100,	// Importance of Strain
			7101,	// Danger of Ice Spirit
			7102,	// Guardian Angel of the Tablet - Vol. 1
			7103,	// Guardian Angel of the Tablet - Vol. 1
			7104,	// Guardian Angel of the Tablet - Vol. 1
			7105,	// Guardian Angel of the Tablet - Vol. 2
			7106,	// Guardian Angel of the Tablet - Vol. 2
			7107,	// Guardian Angel of the Tablet - Vol. 2
			7108,	// Guild''s Secret Report
			7109,	// Guild''s Secret Report
			7110,	// Research Report
			7111,	// Research Report
			7112	// Research Report
	};

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if(!playable.isPlayer())
			return;

		L2Player activeChar = (L2Player) playable;
		Functions.show("data/html/help/" + item.getItemId() + ".htm", activeChar);
		if(item.getItemId() == 7063)
			activeChar.sendPacket(new RadarControl(0, 2, new Location(51995, -51265, -3104)));
		activeChar.sendActionFailed();
	}

	public int[] getItemIds()
	{
		return _itemIds;
	}

	public void onLoad()
	{
		ItemHandler.getInstance().registerItemHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}