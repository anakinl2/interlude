package l2d.game.clientpackets;

import com.lineage.ext.mods.ClassChange;
import com.lineage.ext.mods.balancer.Balancer;
import l2d.game.model.L2Player;
import l2d.game.serverpackets.TutorialCloseHtml;

public class RequestTutorialLinkHtml extends L2GameClientPacket
{

	String _bypass;

	@Override
	public void readImpl()
	{
		_bypass = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getActiveChar();
		if(player == null)
			return;

		if(_bypass.startsWith("_guide"))
		{
			_bypass = _bypass.replaceAll("%", " ");

			if(_bypass.length() < 5)
			{
				_log.warning("Bad Script bypass!");
				return;
			}
			ClassChange.useBypass(player, _bypass);
		}
		else if(_bypass.startsWith("b_b"))
		{
			_bypass = _bypass.replaceAll("%", " ");
			Balancer.usebypass(player, _bypass);
		}
		else if(_bypass.startsWith("close"))
			player.sendPacket(new TutorialCloseHtml());
	}
}