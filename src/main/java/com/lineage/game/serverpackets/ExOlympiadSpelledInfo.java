package com.lineage.game.serverpackets;

import java.util.List;

import javolution.util.FastList;
import com.lineage.game.model.L2Player;

public class ExOlympiadSpelledInfo extends L2GameServerPacket
{
	// chdd(dhd)
	private int char_obj_id = 0;
	private List<Effect> _effects;

	class Effect
	{
		int skillId;
		int dat;
		int duration;

		public Effect(int skillId, int dat, int duration)
		{
			this.skillId = skillId;
			this.dat = dat;
			this.duration = duration;
		}
	}

	public ExOlympiadSpelledInfo()
	{
		_effects = new FastList<Effect>();
	}

	public void addEffect(int skillId, int dat, int duration)
	{
		_effects.add(new Effect(skillId, dat, duration));
	}

	public void addSpellRecivedPlayer(L2Player cha)
	{
		if(cha != null)
			char_obj_id = cha.getObjectId();
	}

	@Override
	protected final void writeImpl()
	{
		if(char_obj_id == 0)
			return;

		writeC(EXTENDED_PACKET);
		writeH(0x2a);

		writeD(char_obj_id);
		writeD(_effects.size());
		for(Effect temp : _effects)
		{
			writeD(temp.skillId);
			writeH(temp.dat);
			writeD(temp.duration);
		}
	}
}