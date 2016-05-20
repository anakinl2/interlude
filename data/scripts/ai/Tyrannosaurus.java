package ai;

import l2d.game.ai.Fighter;
import l2d.game.model.L2Character;
import l2d.game.model.L2Playable;

/**
 * @author PaInKiLlEr
 *         Аи для теранозавра
 *         всегда видит игроков в режиме Silent Move
 *         Выполнено специально для L2Dream.su
 */

public class Tyrannosaurus extends Fighter
{
	public Tyrannosaurus(L2Character actor)
	{
		super(actor);
	}

	@Override
	public boolean isSilentMoveNotVisible(L2Playable target)
	{
		return true;
	}
}