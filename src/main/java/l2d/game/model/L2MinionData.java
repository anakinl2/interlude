package l2d.game.model;

/**
 * This class defines the spawn data of a Minion type
 * In a group mob, there are one master called RaidBoss and several slaves called Minions.
 *
 * <B><U> Data</U> :</B><BR><BR>
 * <li>_minionId : The Identifier of the L2Minion to spawn </li>
 * <li>_minionAmount :  The number of this Minion Type to spawn </li><BR><BR>
 *
 */
public class L2MinionData
{

	/** The Identifier of the L2Minion */
	private short _minionId;

	/** The number of this Minion Type to spawn */
	private byte _minionAmount;

	/**
	 * Set the Identifier of the Minion to spawn.<BR><BR>
	 *
	 * @param if The L2Character Identifier to spawn
	 *
	 */
	public void setMinionId(int id)
	{
		_minionId = (short) id;
	}

	/**
	 * Return the Identifier of the Minion to spawn.<BR><BR>
	 */
	public short getMinionId()
	{
		return _minionId;
	}

	/**
	 * Set the amount of this Minion type to spawn.<BR><BR>
	 *
	 * @param amount The quantity of this Minion type to spawn
	 *
	 */
	public void setAmount(int amount)
	{
		_minionAmount = (byte) amount;
	}

	/**
	 * Return the amount of this Minion type to spawn.<BR><BR>
	 */
	public byte getAmount()
	{
		return _minionAmount;
	}

}
