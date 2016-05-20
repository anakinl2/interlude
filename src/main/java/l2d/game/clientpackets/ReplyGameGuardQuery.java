package l2d.game.clientpackets;

/**
 * <b>Format</b> cdddd
 * @author Felixx
 */
public class ReplyGameGuardQuery extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		if(getClient() != null)
			getClient().setGameGuardOk(true);
	}
}