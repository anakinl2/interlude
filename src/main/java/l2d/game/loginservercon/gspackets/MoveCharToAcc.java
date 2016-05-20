package l2d.game.loginservercon.gspackets;

import com.lineage.auth.gameservercon.lspackets.ServerBasePacket;

/**
 * @Author: Abaddon
 */
public class MoveCharToAcc extends ServerBasePacket
{
	public MoveCharToAcc(String player, String oldacc, String newacc, String pass)
	{
		writeC(0x0c);
		writeS(player);
		writeS(oldacc);
		writeS(newacc);
		writeS(pass);
	}
}