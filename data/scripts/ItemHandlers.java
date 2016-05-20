import com.lineage.ext.scripts.Functions;
import l2d.game.cache.Msg;
import l2d.game.model.L2Object;
import l2d.game.model.L2Player;
import l2d.game.serverpackets.ShowXMasSeal;
import l2d.game.serverpackets.SystemMessage;
import com.lineage.util.Rnd;

public class ItemHandlers extends Functions
{
	public L2Object self;

	// Newspaper
	public void ItemHandler_19999()
	{
		show("data/html/newspaper/00000000.htm", (L2Player) self);
	}

	public void ItemHandler_5555()
	{
		((L2Player) self).sendPacket(new ShowXMasSeal(5555));
	}

	// 'Lockup Research Report' -> 'Research Report'. 'Key of Enigma' is needed.
	public void ItemHandler_8060()
	{
		if(!canBeExtracted(8060))
			return;
		if(((L2Player) self).getInventory().getCountOf(8058) > 0)
		{
			removeItem((L2Player) self, 8058, 1);
			if(((L2Player) self).getInventory().getCountOf(8059) < 1)
				addItem((L2Player) self, 8059, 1);
		}
	}
	
	// ------ Adventurer's Boxes ------

	// Adventurer's Box: C-Grade Accessory (Low Grade)
	public void ItemHandler_8534()
	{
		if(!canBeExtracted(8534))
			return;
		int[] list = new int[] { 853, 916, 884 };
		int[] chances = new int[] { 17, 17, 17 };
		int[] counts = new int[] { 1, 1, 1 };
		removeItem((L2Player) self, 8534, 1);
		extract_item_r(list, counts, chances);
	}

	// Adventurer's Box: C-Grade Accessory (Medium Grade)
	public void ItemHandler_8535()
	{
		if(!canBeExtracted(8535))
			return;
		int[] list = new int[] { 854, 917, 885 };
		int[] chances = new int[] { 17, 17, 17 };
		int[] counts = new int[] { 1, 1, 1 };
		removeItem((L2Player) self, 8535, 1);
		extract_item_r(list, counts, chances);
	}

	// Adventurer's Box: C-Grade Accessory (High Grade)
	public void ItemHandler_8536()
	{
		if(!canBeExtracted(8536))
			return;
		int[] list = new int[] { 855, 119, 886 };
		int[] chances = new int[] { 17, 17, 17 };
		int[] counts = new int[] { 1, 1, 1 };
		removeItem((L2Player) self, 8536, 1);
		extract_item_r(list, counts, chances);
	}

	// Adventurer's Box: B-Grade Accessory (Low Grade)
	public void ItemHandler_8537()
	{
		if(!canBeExtracted(8537))
			return;
		int[] list = new int[] { 856, 918, 887 };
		int[] chances = new int[] { 17, 17, 17 };
		int[] counts = new int[] { 1, 1, 1 };
		removeItem((L2Player) self, 8537, 1);
		extract_item_r(list, counts, chances);
	}

	// Adventurer's Box: B-Grade Accessory (High Grade)
	public void ItemHandler_8538()
	{
		if(!canBeExtracted(8538))
			return;
		int[] list = new int[] { 864, 926, 895 };
		int[] chances = new int[] { 17, 17, 17 };
		int[] counts = new int[] { 1, 1, 1 };
		removeItem((L2Player) self, 8538, 1);
		extract_item_r(list, counts, chances);
	}

	// Adventurer's Box: Hair Accessory
	public void ItemHandler_8539()
	{
		if(!canBeExtracted(8539))
			return;
		int[] list = new int[] { 8179, 8178, 8177 };
		int[] chances = new int[] { 10, 20, 30 };
		int[] counts = new int[] { 1, 1, 1 };
		removeItem((L2Player) self, 8539, 1);
		extract_item_r(list, counts, chances);
	}

	// Adventurer's Box: Cradle of Creation
	public void ItemHandler_8540()
	{
		if(!canBeExtracted(8540))
			return;
		removeItem((L2Player) self, 8540, 1);
		if(Rnd.chance(30))
			addItem((L2Player) self, 8175, 1);
	}

	// Quest 370: A Wiseman Sows Seeds
	public void ItemHandler_5916()
	{
		if(!canBeExtracted(5916))
			return;
		int[] list = new int[] { 5917, 5918, 5919, 5920, 736 };
		int[] counts = new int[] { 1, 1, 1, 1, 1 };
		removeItem((L2Player) self, 5916, 1);
		extract_item(list, counts);
	}

	// Quest 376: Giants Cave Exploration, Part 1
	public void ItemHandler_5944()
	{
		if(!canBeExtracted(5944))
			return;
		int[] list = new int[] {
				5922,
				5923,
				5924,
				5925,
				5926,
				5927,
				5928,
				5929,
				5930,
				5931,
				5932,
				5933,
				5934,
				5935,
				5936,
				5937,
				5938,
				5939,
				5940,
				5941,
				5942,
				5943 };
		int[] counts = new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
		removeItem((L2Player) self, 5944, 1);
		extract_item(list, counts);
	}

	// Quest 377: Giants Cave Exploration, Part 2
	public void ItemHandler_5955()
	{
		if(!canBeExtracted(5955))
			return;
		int[] list = new int[] { 5942, 5943, 5945, 5946, 5947, 5948, 5949, 5950, 5951, 5952, 5953, 5954 };
		int[] counts = new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
		removeItem((L2Player) self, 5955, 1);
		extract_item(list, counts);
	}

	// Quest 372: Legacy of Insolence
	public void ItemHandler_5966()
	{
		if(!canBeExtracted(5966))
			return;
		int[] list = new int[] { 5970, 5971, 5977, 5978, 5979, 5986, 5993, 5994, 5995, 5997, 5983, 6001 };
		int[] counts = new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
		removeItem((L2Player) self, 5966, 1);
		extract_item(list, counts);
	}

	// Quest 372: Legacy of Insolence
	public void ItemHandler_5967()
	{
		if(!canBeExtracted(5967))
			return;
		int[] list = new int[] { 5970, 5971, 5975, 5976, 5980, 5985, 5993, 5994, 5995, 5997, 5983, 6001 };
		int[] counts = new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
		removeItem((L2Player) self, 5967, 1);
		extract_item(list, counts);
	}

	// Quest 372: Legacy of Insolence
	public void ItemHandler_5968()
	{
		if(!canBeExtracted(5968))
			return;
		int[] list = new int[] { 5973, 5974, 5981, 5984, 5989, 5990, 5991, 5992, 5996, 5998, 5999, 6000, 5988, 5983, 6001 };
		int[] counts = new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
		removeItem((L2Player) self, 5968, 1);
		extract_item(list, counts);
	}

	// Quest 372: Legacy of Insolence
	public void ItemHandler_5969()
	{
		if(!canBeExtracted(5969))
			return;
		int[] list = new int[] { 5970, 5971, 5982, 5987, 5989, 5990, 5991, 5992, 5996, 5998, 5999, 6000, 5972, 6001 };
		int[] counts = new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
		removeItem((L2Player) self, 5969, 1);
		extract_item(list, counts);
	}

	// Quest 373: Supplier of Reagents
	public void ItemHandler_6007()
	{
		if(!canBeExtracted(6007))
			return;
		int[] list = new int[] { 6013, 6014, 6016, 6017, 6019 };
		int[] counts = new int[] { 2, 1, 1, 2, 1 };
		int[] chances = new int[] { 30, 20, 10, 10, 30 };
		removeItem((L2Player) self, 6007, 1);
		extract_item_r(list, counts, chances);
	}

	// Quest 373: Supplier of Reagents
	public void ItemHandler_6008()
	{
		if(!canBeExtracted(6008))
			return;
		int[] list = new int[] { 6013, 6020, 6014, 6019 };
		int[] counts = new int[] { 1, 2, 1, 1 };
		int[] chances = new int[] { 35, 20, 30, 30 };
		removeItem((L2Player) self, 6008, 1);
		extract_item_r(list, counts, chances);
	}

	// Quest 373: Supplier of Reagents
	public void ItemHandler_6009()
	{
		if(!canBeExtracted(6009))
			return;
		int[] list = new int[] { 6012, 6018, 6019, 6013 };
		int[] counts = new int[] { 1, 2, 2, 1 };
		int[] chances = new int[] { 20, 20, 20, 40 };
		removeItem((L2Player) self, 6009, 1);
		extract_item_r(list, counts, chances);
	}

	// Quest 373: Supplier of Reagents
	public void ItemHandler_6010()
	{
		if(!canBeExtracted(6010))
			return;
		int[] list = new int[] { 6017, 6020, 6015, 6016 };
		int[] counts = new int[] { 2, 2, 2, 1 };
		int[] chances = new int[] { 20, 20, 25, 35 };
		removeItem((L2Player) self, 6010, 1);
		extract_item_r(list, counts, chances);
	}

	// Quest 628: Hunt of Golden Ram
	public void ItemHandler_7725()
	{
		if(!canBeExtracted(7725))
			return;
		int[] list = new int[] { 6035, 1060, 735, 1540, 1061, 1539 };
		int[] counts = new int[] { 1, 1, 1, 1, 1, 1 };
		int[] chances = new int[] { 7, 39, 7, 3, 12, 32 };
		removeItem((L2Player) self, 7725, 1);
		extract_item_r(list, counts, chances);
	}

	// Quest 628: Hunt of Golden Ram
	public void ItemHandler_7637()
	{
		if(!canBeExtracted(7637))
			return;
		int[] list = new int[] { 4039, 4041, 4043, 4044, 4042, 4040 };
		int[] counts = new int[] { 4, 1, 4, 4, 2, 2 };
		int[] chances = new int[] { 20, 10, 20, 20, 15, 15 };
		removeItem((L2Player) self, 7637, 1);
		extract_item_r(list, counts, chances);
	}

	// Quest 628: Hunt of Golden Ram
	public void ItemHandler_7636()
	{
		if(!canBeExtracted(7636))
			return;
		int[] list = new int[] { 1875, 1882, 1880, 1874, 1877, 1881, 1879, 1876 };
		int[] counts = new int[] { 3, 3, 4, 1, 3, 1, 3, 6 };
		int[] chances = new int[] { 10, 20, 10, 10, 10, 12, 12, 16 };
		removeItem((L2Player) self, 7636, 1);
		extract_item_r(list, counts, chances);
	}

	// Looted Goods - White Cargo box
	public void ItemHandler_7629()
	{
		if(!canBeExtracted(7629))
			return;
		int[] list = new int[] { 6688, 6689, 6690, 6691, 6693, 6694, 6695, 6696, 6697, 7579, 57 };
		int[] counts = new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 330000 };
		int[] chances = new int[] { 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 10 };
		removeItem((L2Player) self, 7629, 1);
		extract_item_r(list, counts, chances);
	}

	// Looted Goods - Blue Cargo box #All chances of 8 should be 8.5, must be fixed if possible!!
	public void ItemHandler_7630()
	{
		if(!canBeExtracted(7630))
			return;
		int[] list = new int[] { 6703, 6704, 6705, 6706, 6708, 6709, 6710, 6712, 6713, 6714, 57 };
		int[] counts = new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 292000 };
		int[] chances = new int[] { 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 20 };
		removeItem((L2Player) self, 7630, 1);
		extract_item_r(list, counts, chances);
	}

	// Looted Goods - Yellow Cargo box
	public void ItemHandler_7631()
	{
		if(!canBeExtracted(7631))
			return;
		int[] list = new int[] { 6701, 6702, 6707, 6711, 57 };
		int[] counts = new int[] { 1, 1, 1, 1, 930000 };
		int[] chances = new int[] { 20, 20, 20, 20, 20 };
		removeItem((L2Player) self, 7631, 1);
		extract_item_r(list, counts, chances);
	}

	// Looted Goods - Red Filing Cabinet
	public void ItemHandler_7632()
	{
		if(!canBeExtracted(7632))
			return;
		int[] list = new int[] { 6857, 6859, 6861, 6863, 6867, 6869, 6871, 6875, 6877, 6879, 57 };
		int[] counts = new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 340000 };
		int[] chances = new int[] { 8, 9, 8, 9, 8, 9, 8, 9, 8, 9, 15 };
		removeItem((L2Player) self, 7632, 1);
		extract_item_r(list, counts, chances);
	}

	// Looted Goods - Purple Filing Cabinet
	public void ItemHandler_7633()
	{
		if(!canBeExtracted(7633))
			return;
		int[] list = new int[] { 6853, 6855, 6865, 6873, 57 };
		int[] counts = new int[] { 1, 1, 1, 1, 850000 };
		int[] chances = new int[] { 20, 20, 20, 20, 20 };
		removeItem((L2Player) self, 7633, 1);
		extract_item_r(list, counts, chances);
	}

	// Looted Goods - Brown Pouch
	public void ItemHandler_7634()
	{
		if(!canBeExtracted(7634))
			return;
		int[] list = new int[] { 1874, 1875, 1876, 1877, 1879, 1880, 1881, 1882, 57 };
		int[] counts = new int[] { 20, 20, 20, 20, 20, 20, 20, 20, 150000 };
		int[] chances = new int[] { 10, 10, 16, 11, 10, 5, 10, 18, 10 };
		removeItem((L2Player) self, 7634, 1);
		extract_item_r(list, counts, chances);
	}

	// Looted Goods - Gray Pouch
	public void ItemHandler_7635()
	{
		if(!canBeExtracted(7635))
			return;
		int[] list = new int[] { 4039, 4040, 4041, 4042, 4043, 4044, 57 };
		int[] counts = new int[] { 4, 4, 4, 4, 4, 4, 160000 };
		int[] chances = new int[] { 20, 10, 10, 10, 20, 20, 10 };
		removeItem((L2Player) self, 7635, 1);
		extract_item_r(list, counts, chances);
	}

	private void extract_item(int[] list, int[] counts)
	{
		int index = Rnd.get(list.length);
		int id = list[index];
		int count = counts[index];
		addItem((L2Player) self, id, count);
	}

	private void extract_item_r(int[] list, int[] counts, int[] chances)
	{
		int sum = 0;

		for(int i = 0; i < list.length; i++)
			sum += chances[i];

		int[] table = new int[sum];
		int k = 0;

		for(int i = 0; i < list.length; i++)
			for(int j = 0; j < chances[i]; j++)
			{
				table[k] = i;
				k++;
			}

		int i = table[Rnd.get(table.length)];
		int item = list[i];
		int count = counts[i];

		addItem((L2Player) self, item, count);
	}

	private void extract_item_r(int[] list, int[] counts, double[] chances)
	{
		int sum = 0;

		for(int i = 0; i < list.length; i++)
			sum += (int)(chances[i]*100);

		int[] table = new int[sum];
		int k = 0;

		for(int i = 0; i < list.length; i++)
			for(int j = 0; j < chances[i]*100; j++)
			{
				table[k] = i;
				k++;
			}

		int i = table[Rnd.get(table.length)];
		int item = list[i];
		int count = counts[i];

		addItem((L2Player) self, item, count);
	}

	private boolean canBeExtracted(int itemId)
	{
		L2Player player = (L2Player) self;
		if(player == null)
			return false;
		if(!player.isQuestContinuationPossible())
		{
			player.sendPacket(Msg.YOUR_INVENTORY_IS_FULL);
			player.sendPacket(new SystemMessage(SystemMessage.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS).addItemName(itemId));
			return false;
		}
		return true;
	}
}