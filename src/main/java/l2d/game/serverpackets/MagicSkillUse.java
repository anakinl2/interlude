package l2d.game.serverpackets;

import l2d.game.model.L2Character;

/**
 * Format: dddddddddh [h] h [ddd]
 * Пример пакета:
 * 48
 * 86 99 00 4F 86 99 00 4F
 * EF 08 00 00 01 00 00 00
 * 00 00 00 00 00 00 00 00
 * F9 B5 FF FF 7D E0 01 00 68 F3 FF FF
 * 00 00 00 00
 */
public class MagicSkillUse extends L2GameServerPacket
{
	private int _targetId;
	private int _skillId;
	private int _skillLevel;
	private int _hitTime;
	private long _reuseDelay;
	private int _chaId, _x, _y, _z;

	public MagicSkillUse(final L2Character cha, final L2Character target, final int skillId, final int skillLevel, final int hitTime, final long reuseDelay)
	{
		_chaId = cha.getObjectId();
		_targetId = target.getObjectId();
		_skillId = skillId;
		_skillLevel = skillLevel;
		_hitTime = hitTime;
		_reuseDelay = reuseDelay;
		_x = cha.getX();
		_y = cha.getY();
		_z = cha.getZ();
	}

	/**
	 * Выбранный таргет, выполнит анимацию каста скилла.
	 * 
	 * @param cha
	 *            - Кто выполняет анимацию.
	 * @param skillId
	 *            - Какой ID Скилла.
	 * @param skillLevel
	 *            - Какой Level скилла.
	 * @param hitTime
	 *            - Скорость выполнения.
	 * @param reuseDelay
	 *            - Какой откат ставим.
	 */
	public MagicSkillUse(final L2Character cha, final int skillId, final int skillLevel, final int hitTime, final long reuseDelay)
	{
		_chaId = cha.getObjectId();
		_targetId = cha.getTargetId();
		_skillId = skillId;
		_skillLevel = skillLevel;
		_hitTime = hitTime;
		_reuseDelay = reuseDelay;
		_x = cha.getX();
		_y = cha.getY();
		_z = cha.getZ();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x48);
		writeD(_chaId);
		writeD(_targetId);
		writeD(_skillId);
		writeD(_skillLevel);
		writeD(_hitTime);
		writeD((int) _reuseDelay);
		writeD(_x);
		writeD(_y);
		writeD(_z);

		writeH(0x00); // количество елементов чего-то [h]
		writeH(0x00); // количество елементов чего-то [ddd]
	}
}