package com.lineage.game.serverpackets;

import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Object;

/**
 * sample
 * 06 8f19904b 2522d04b 00000000 80 950c0000 4af50000 08f2ffff 0000    - 0 damage (missed 0x80)
 * 06 85071048 bc0e504b 32000000 10 fc41ffff fd240200 a6f5ffff 0100 bc0e504b 33000000 10                                     3....

 * format
 * dddc dddh (ddc)
 *
 */
public class Attack extends L2GameServerPacket
{
	private class Hit
	{
		int _targetId;
		int _damage;
		int _flags;

		Hit(L2Object target, int damage, boolean miss, boolean crit, boolean shld)
		{
			_targetId = target.getObjectId();
			_damage = damage;
			if(_soulshot)
				_flags |= 0x10 | _grade;
			if(crit)
				_flags |= 0x20;
			if(shld)
				_flags |= 0x40;
			if(miss)
				_flags |= 0x80;

		}
	}

	// dh
	public final int _attackerId;
	public final boolean _soulshot;
	private int _grade;
	private int _x;
	private int _y;
	private int _z;
	private Hit[] hits;

	/**
	 * @param attacker the attacker L2Character
	 * @param ss true if useing SoulShots
	 */
	public Attack(L2Character attacker, boolean ss, int grade)
	{
		_attackerId = attacker.getObjectId();
		_soulshot = ss;
		_grade = grade;
		_x = attacker.getX();
		_y = attacker.getY();
		_z = attacker.getZ();
		hits = new Hit[0];
	}

	/**
	 * Add this hit (target, damage, miss, critical, shield) to the Server-Client packet Attack.<BR><BR>
	 */
	public void addHit(L2Object target, int damage, boolean miss, boolean crit, boolean shld)
	{
		// Get the last position in the hits table
		int pos = hits.length;

		// Create a new Hit object
		Hit[] tmp = new Hit[pos + 1];

		// Add the new Hit object to hits table
		System.arraycopy(hits, 0, tmp, 0, hits.length);
		tmp[pos] = new Hit(target, damage, miss, crit, shld);
		hits = tmp;
	}

	/**
	 * Return True if the Server-Client packet Attack conatins at least 1 hit.<BR><BR>
	 */
	public boolean hasHits()
	{
		return hits.length > 0;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x05);

		writeD(_attackerId);
		writeD(hits[0]._targetId);
		writeD(hits[0]._damage);
		writeC(hits[0]._flags);
		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeH(hits.length - 1);
		for(int i = 1; i < hits.length; i++)
		{
			writeD(hits[i]._targetId);
			writeD(hits[i]._damage);
			writeC(hits[i]._flags);
		}
	}
}