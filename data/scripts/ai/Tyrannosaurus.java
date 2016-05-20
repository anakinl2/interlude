package ai;

import com.lineage.game.ai.Fighter;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Playable;

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