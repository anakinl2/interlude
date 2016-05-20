package l2d.game.serverpackets;

import java.util.ArrayList;
import java.util.Collection;

import l2d.game.model.L2Character;

public class MagicSkillLaunched extends L2GameServerPacket
{
	private final int _casterId;
	private final int _skillId;
	private final int _skillLevel;
	private final Collection<L2Character> _targets;
	private final boolean _isOffensive;

	public boolean isOffensive()
	{
		return _isOffensive;
	}

	public MagicSkillLaunched(final int casterId, final int skillId, final int skillLevel, final L2Character target, final boolean isOffensive)
	{
		_casterId = casterId;
		_skillId = skillId;
		_skillLevel = skillLevel;
		_targets = new ArrayList<L2Character>();
		_targets.add(target);
		_isOffensive = isOffensive;
	}

	public MagicSkillLaunched(final int casterId, final int skillId, final int skillLevel, final Collection<L2Character> targets, final boolean isOffensive)
	{
		_casterId = casterId;
		_skillId = skillId;
		_skillLevel = skillLevel;
		_targets = targets;
		_isOffensive = isOffensive;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x76);
		writeD(_casterId);
		writeD(_skillId);
		writeD(_skillLevel);
		writeD(_targets.size());
		for(final L2Character target : _targets)
			if(target != null)
				writeD(target.getObjectId());
	}
}