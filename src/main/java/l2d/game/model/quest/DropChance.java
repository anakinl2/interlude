/**
 * 
 */
package l2d.game.model.quest;

import l2d.Config;
import l2d.game.model.L2Player;
import l2d.util.Rnd;

public class DropChance
{
	private final int item_id, chance;
	private int base_count = 0;

	public DropChance(int _item_id, int _chance)
	{
		item_id = _item_id;
		while(_chance > 100)
		{
			_chance -= 100;
			base_count++;
		}
		chance = _chance;
	}

	public int getItemId()
	{
		return item_id;
	}

	public int getChance()
	{
		return chance;
	}

	public boolean Chance()
	{
		return Rnd.chance(chance);
	}

	/*
	 * С учетом рейтов
	 */
	public boolean Chance(L2Player player)
	{
		return Rnd.chance(chance * (Config.RATE_QUESTS_REWARD + player.getBonus().RATE_QUESTS_REWARD));
	}

	public int getBaseCount()
	{
		return base_count;
	}

	/*
	 * С учетом рейтов
	 */
	public int getBaseCount(L2Player player)
	{
		return base_count * (int) (Config.RATE_QUESTS_REWARD + player.getBonus().RATE_QUESTS_REWARD);
	}

	public int getRewardCount()
	{
		return getBaseCount() + (Chance() ? 1 : 0);
	}

	/*
	 * С учетом рейтов
	 */
	public int getRewardCount(L2Player player)
	{
		return getBaseCount(player) + (Chance(player) ? 1 : 0);
	}
}