package l2d.game.serverpackets;

import java.util.HashSet;

import l2d.game.model.L2Player;
import l2d.game.model.instances.L2HennaInstance;

public class HennaEquipList extends L2GameServerPacket
{
	private int char_adena, HennaEmptySlots;
	private HashSet<L2HennaInstance> availHenna = new HashSet<L2HennaInstance>();

	public HennaEquipList(L2Player player, L2HennaInstance[] hennaEquipList)
	{
		char_adena = player.getAdena();
		HennaEmptySlots = player.getHennaEmptySlots();
		for(L2HennaInstance element : hennaEquipList)
			if(player.getInventory().getItemByItemId(element.getItemIdDye()) != null)
				availHenna.add(element);
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xe2);

		writeD(char_adena);
		writeD(HennaEmptySlots);
		if(availHenna.size() != 0)
		{
			writeD(availHenna.size());
			for(L2HennaInstance henna : availHenna)
			{
				writeD(henna.getSymbolId()); //symbolid
				writeD(henna.getItemIdDye()); //itemid of dye
				writeD(henna.getAmountDyeRequire()); //amount of dye require
				writeD(henna.getPrice()); //amount of aden require
				writeD(1); //meet the requirement or not
			}
		}
		else
		{
			writeD(0x01);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
		}
	}
}