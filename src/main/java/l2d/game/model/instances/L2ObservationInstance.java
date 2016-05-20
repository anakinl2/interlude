package l2d.game.model.instances;

import static l2d.game.model.L2Zone.ZoneType.Siege;

import java.util.StringTokenizer;

import l2d.game.cache.Msg;
import l2d.game.instancemanager.ZoneManager;
import l2d.game.model.L2Player;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.templates.L2NpcTemplate;

public final class L2ObservationInstance extends L2NpcInstance
{
	//private static Logger _log = Logger.getLogger(L2ObservationInstance.class.getName());

	/**
	 * @param template
	 */
	public L2ObservationInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		// first do the common stuff
		// and handle the commands that all NPC classes know
		super.onBypassFeedback(player, command);

		if(command.startsWith("observeSiege"))
		{
			String val = command.substring(13);
			StringTokenizer st = new StringTokenizer(val);
			st.nextToken(); // Bypass cost

			if(ZoneManager.getInstance().checkIfInZone(Siege, Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken())))
				doObserve(player, val);
			else
				player.sendPacket(new SystemMessage(SystemMessage.OBSERVATION_IS_ONLY_POSSIBLE_DURING_A_SIEGE));
		}
		else if(command.startsWith("observe"))
			doObserve(player, command.substring(8));
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";
		if(val == 0)
			pom = "" + npcId;
		else
			pom = npcId + "-" + val;

		return "data/html/observation/" + pom + ".htm";
	}

	private void doObserve(L2Player player, String val)
	{
		StringTokenizer st = new StringTokenizer(val);
		int cost = Integer.parseInt(st.nextToken());
		int x = Integer.parseInt(st.nextToken());
		int y = Integer.parseInt(st.nextToken());
		int z = Integer.parseInt(st.nextToken());
		if(player.getAdena() < cost)
			player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
		else if(player.enterObserverMode(x, y, z))
			player.reduceAdena(cost);
		player.sendActionFailed();
	}
}