package l2d.game.serverpackets;

import java.util.ArrayList;

import l2d.game.model.L2Clan;
import l2d.game.model.L2SkillLearn;
import l2d.game.model.base.ClassId;
import l2d.game.tables.SkillSpellbookTable;
import l2d.game.tables.SkillTreeTable;

/**
 * [S] 91 AcquireSkillInfo
 * 
 * @author Felixx
 */
public class AcquireSkillInfo extends L2GameServerPacket
{
	private ArrayList<Req> _reqs;
	private int _id;
	private byte _level;
	private int _spCost;
	private int _mode;
	private ClassId _classId;
	private L2Clan _clan;

	class Req
	{
		public int id, type, unk;
		public int count;

		Req(final int type, final int id, final int count, final int unk)
		{
			this.id = id;// 0
			this.type = type;// 2
			this.count = count;// count spb
			this.unk = unk;// 2
		}
	}

	public AcquireSkillInfo(final int id, final byte level, final ClassId classid, final L2Clan clan)
	{
		_reqs = new ArrayList<Req>();
		_id = id;
		_level = level;
		_classId = classid;
		_clan = clan;
	}

	private void fillRequirements()
	{
		final L2SkillLearn SkillLearn = SkillTreeTable.getSkillLearn(_id, _level, _classId, _clan);
		if(SkillLearn == null)
			return;
		_spCost = _clan != null ? SkillLearn.getRepCost() : SkillLearn.getSpCost();
		if(SkillLearn.common)
			_mode = 1;
		else if(_clan != null)
			_mode = 2;
		else if(SkillLearn.transformation)
			_mode = 4;
		else
			_mode = 0;

		final Integer spb_id = SkillSpellbookTable._skillSpellbooks.get(SkillSpellbookTable.hashCode(new int[] { _id, _level }));

		if(spb_id != null)
			_reqs.add(new Req(SkillLearn.common ? 4 : _clan != null ? 2 : 99, spb_id.intValue(), SkillLearn.getItemCount(), SkillLearn.common || _clan != null ? 2 : 50));
	}

	@Override
	final public void runImpl()
	{
		fillRequirements();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x8b);
		writeD(_id);
		writeD(_level);
		writeD(_spCost);
		writeD(_mode);

		writeD(_reqs.size());

		for(final Req temp : _reqs)
		{
			writeD(temp.type);
			writeD(temp.id);
			writeD(temp.count);
			writeD(temp.unk);
		}
	}
}