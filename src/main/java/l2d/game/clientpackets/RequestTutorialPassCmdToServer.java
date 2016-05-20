package l2d.game.clientpackets;

import l2d.game.instancemanager.QuestManager;
import l2d.game.model.L2Player;
import l2d.game.model.quest.Quest;

public class RequestTutorialPassCmdToServer extends L2GameClientPacket
{
	// format: cS

	String _bypass = null;

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

		Quest tutorial = QuestManager.getQuest(255);

		if(tutorial != null)
		{
			player.processQuestEvent(tutorial.getName(), _bypass);
			player.sendMessage("Visgi Q veikia   +" + _bypass);
		}
	}
}