package l2d.game.serverpackets;

import javolution.util.FastList;
import com.lineage.Config;
import l2d.game.model.L2Multisell.MultiSellListContainer;
import l2d.game.model.base.MultiSellEntry;
import l2d.game.model.base.MultiSellIngredient;
import l2d.game.tables.ItemTable;
import l2d.game.templates.L2Item;

public class MultiSellList extends L2GameServerPacket
{
	protected int _page;
	protected int _finished;

	private int _listId;
	private FastList<MultiSellEntry> _possiblelist = new FastList<MultiSellEntry>();

	public MultiSellList(MultiSellListContainer list, int page, int finished)
	{
		_possiblelist = list.getEntries();
		_listId = list.getListId();
		_page = page;
		_finished = finished;
	}

	@Override
	protected final void writeImpl()
	{
		// ddddd (dchddddddddddhh (ddhdhdddddddddd)(dhdhdddddddddd))
		writeC(0xd0);
		writeD(_listId); // list id
		writeD(_page); // page
		writeD(_finished); // finished
		writeD(Config.MULTISELL_SIZE); // size of pages
		writeD(_possiblelist != null ? _possiblelist.size() : 0); //list lenght

		if(_possiblelist == null)
			return;

		for(FastList.Node<MultiSellEntry> n = _possiblelist.head(), end = _possiblelist.tail(); (n = n.getNext()) != end;)
		{
			MultiSellEntry ent = n.getValue();

			writeD(ent.getEntryId());
			writeD(0x00); // C6
			writeD(0x00); // C6
			writeC(ent.getProduction().get(0).isStackable() ? 0x01 : 0x00);
			writeH(ent.getProduction().size());
			writeH(ent.getIngredients().size());

			for(MultiSellIngredient prod : ent.getProduction())
			{
				L2Item template = ItemTable.getInstance().getTemplate(prod.getItemId());
				if(template == null)
					continue;

				writeH(prod.getItemId());
				writeD(template.getBodyPart());
				writeH(template.getType2());
				writeD((int) Math.min(prod.getItemCount(), Integer.MAX_VALUE));
				writeH(prod.getItemEnchant());
				writeD(0x00);
				writeD(0x00);
			}

			for(FastList.Node<MultiSellIngredient> sn = ent.getIngredients().head(), send = ent.getIngredients().tail(); (sn = sn.getNext()) != send;)
			{
				MultiSellIngredient i = sn.getValue();
				int itemId = i.getItemId();
				final L2Item item = itemId != L2Item.ITEM_ID_CLAN_REPUTATION_SCORE && itemId != L2Item.ITEM_ID_PC_BANG_POINTS ? ItemTable.getInstance().getTemplate(i.getItemId()) : null;

				writeH(itemId);
				writeH(itemId != L2Item.ITEM_ID_CLAN_REPUTATION_SCORE && itemId != L2Item.ITEM_ID_PC_BANG_POINTS ? item.getType2() : 0xffff);
				writeD((int) i.getItemCount());
				writeH((itemId != L2Item.ITEM_ID_CLAN_REPUTATION_SCORE && itemId != L2Item.ITEM_ID_PC_BANG_POINTS ? item.getType2() : 0x00) <= L2Item.TYPE2_ACCESSORY ? i.getItemEnchant() : 0);
				writeD(0x00);
				writeD(0x00);
			}
		}
	}
}