package l2d.game.model;

public class L2ShortCut
{
	public final static int TYPE_ITEM = 1;
	public final static int TYPE_SKILL = 2;
	public final static int TYPE_ACTION = 3;
	public final static int TYPE_MACRO = 4;
	public final static int TYPE_RECIPE = 5;
	public final static int TYPE_TPBOOKMARK = 6; // Gracia Final

	public final int slot;
	public final int page;
	public final int type;
	public final int id;
	public final int level;

	public L2ShortCut(int slot, int page, int type, int id, int level)
	{
		this.slot = slot;
		this.page = page;
		this.type = type;
		this.id = id;
		this.level = level;
	}

	@Override
	public String toString()
	{
		return "ShortCut: " + slot + "/" + page + " ( " + type + "," + id + "," + level + ")";
	}
}