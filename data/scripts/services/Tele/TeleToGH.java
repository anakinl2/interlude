package services.Tele;

import java.util.ArrayList;

import com.lineage.Config;
import com.lineage.ext.listeners.L2ZoneEnterLeaveListener;
import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.instancemanager.ZoneManager;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Playable;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Spawn;
import com.lineage.game.model.L2Zone;
import com.lineage.game.model.L2Zone.ZoneType;
import com.lineage.game.tables.NpcTable;
import com.lineage.game.templates.L2NpcTemplate;
import com.lineage.util.Util;

public class TeleToGH extends Functions implements ScriptFile
{
	public static L2Object self;
	public static L2Object npc;

	private static ArrayList<L2Spawn> _spawns = new ArrayList<L2Spawn>();

	private L2Zone _zone = ZoneManager.getInstance().getZoneById(ZoneType.offshore, 500014, false);
	private ZoneListener _zoneListener = new ZoneListener();

	public void onLoad()
	{
		if(Config.SERVICES_GIRAN_HARBOR_ENABLED)
		{
			try
			{

				// spawn wh keeper
				L2Spawn sp1 = new L2Spawn(NpcTable.getTemplate(30086));
				sp1.setLocx(48059);
				sp1.setLocy(186791);
				sp1.setLocz( -3512);
				sp1.setAmount(1);
				sp1.setHeading(42000);
				sp1.setRespawnDelay(5);
				sp1.init();

				_spawns.add(sp1);

				// spawn grocery trader (Helvetia)
				L2Spawn sp2 = new L2Spawn(NpcTable.getTemplate(30839));
				sp2.setLocx(48146);
				sp2.setLocy(186753);
				sp2.setLocz( -3512);
				sp2.setAmount(1);
				sp2.setHeading(42000);
				sp2.setRespawnDelay(5);
				sp2.init();
				_spawns.add(sp2);

				// spawn gk
				L2NpcTemplate t = NpcTable.getTemplate(30878);
				t.displayId = 36394;
				t.title = "Gatekeeper";
				t.ai_type = "npc";
				L2Spawn sp3 = new L2Spawn(t);
				sp3.setLocx(47984);
				sp3.setLocy(186832);
				sp3.setLocz( -3445);
				sp3.setAmount(1);
				sp3.setHeading(42000);
				sp3.setRespawnDelay(5);
				sp3.init();
				_spawns.add(sp3);

				/*
				 * //Respawn Old GK
				 * L2Spawn sp4 = SpawnTable.getInstance().getSpawnsByNpcId(30878).get(0);
				 * sp4.despawnAll();
				 * sp4.setLocx(46447);
				 * sp4.setLocy(185935);
				 * sp4.setLocz(-3583);
				 * sp4.setHeading(42000);
				 * sp4.doSpawn(true);
				 * _spawns.add(sp4);
				 */

				// spawn Orion the Cat
				L2Spawn sp5 = new L2Spawn(NpcTable.getTemplate(31860));
				sp5.setLocx(48129);
				sp5.setLocy(186828);
				sp5.setLocz( -3512);
				sp5.setAmount(1);
				sp5.setHeading(45452);
				sp5.setRespawnDelay(5);
				sp5.init();
				_spawns.add(sp5);

				// spawn blacksmith (Pushkin)
				L2Spawn sp6 = new L2Spawn(NpcTable.getTemplate(30300));
				sp6.setLocx(48102);
				sp6.setLocy(186772);
				sp6.setLocz( -3512);
				sp6.setAmount(1);
				sp6.setHeading(42000);
				sp6.setRespawnDelay(5);
				sp6.init();
				_spawns.add(sp6);
			}
			catch(SecurityException e)
			{
				e.printStackTrace();
			}
			catch(ClassNotFoundException e)
			{
				e.printStackTrace();
			}

			_zone.getListenerEngine().addMethodInvokedListener(_zoneListener);

			ZoneManager.getInstance().getZoneById(ZoneType.offshore, 500014, false).setActive(true);
			ZoneManager.getInstance().getZoneById(ZoneType.peace_zone, 500023, false).setActive(true);
			ZoneManager.getInstance().getZoneById(ZoneType.dummy, 500024, false).setActive(true);

			System.out.println("Loaded Service: Teleport to Giran Harbor");
		}
	}

	public void onReload()
	{
		_zone.getListenerEngine().removeMethodInvokedListener(_zoneListener);
		for(L2Spawn spawn : _spawns)
			spawn.despawnAll();
		_spawns.clear();
	}

	public void onShutdown()
	{}

	public void toGH()
	{
		L2Player player = (L2Player) self;

		if(player == null || !checkCondition(player))
			return;

		player.setVar("backCoords", player.getX() + " " + player.getY() + " " + player.getZ());
		player.teleToLocation(47416, 186568, -3480);
	}

	public void fromGH()
	{
		L2Player player = (L2Player) self;

		if(player == null || !checkCondition(player))
			return;

		String var = player.getVar("backCoords");
		if(var == null || var.equals(""))
		{
			teleOut(player);
			return;
		}

		String[] coords = var.split(" ");
		if(coords.length != 3)
		{
			teleOut(player);
			return;
		}

		player.teleToLocation(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]), Integer.parseInt(coords[2]));
	}

	public void teleOut(L2Player player)
	{
		player.teleToLocation(46776, 185784, -3528);
		if(player.getVar("lang@").equalsIgnoreCase("en"))
			show("I don't know from where you came here, but I can teleport you the another border side.", player);
		else
			show("Я не знаю, как Вы попали сюда, но я могу Вас отправить за ограждение.", player);
	}

	public boolean checkCondition(L2Player player)
	{
		return !(player.isActionsDisabled() || player.isSitting());
	}

	public static String DialogAppend_30059(Integer val)
	{
		return getHtmlAppends(val);
	}

	public static String DialogAppend_30080(Integer val)
	{
		return getHtmlAppends(val);
	}

	public static String DialogAppend_30177(Integer val)
	{
		return getHtmlAppends(val);
	}

	public static String DialogAppend_30233(Integer val)
	{
		return getHtmlAppends(val);
	}

	public static String DialogAppend_30256(Integer val)
	{
		return getHtmlAppends(val);
	}

	public static String DialogAppend_30320(Integer val)
	{
		return getHtmlAppends(val);
	}

	public static String DialogAppend_30848(Integer val)
	{
		return getHtmlAppends(val);
	}

	public static String DialogAppend_30878(Integer val)
	{
		return getHtmlAppends(val);
	}

	public static String DialogAppend_30899(Integer val)
	{
		return getHtmlAppends(val);
	}

	public static String DialogAppend_31210(Integer val)
	{
		return getHtmlAppends(val);
	}

	public static String DialogAppend_31275(Integer val)
	{
		return getHtmlAppends(val);
	}

	public static String DialogAppend_31320(Integer val)
	{
		return getHtmlAppends(val);
	}

	public static String DialogAppend_31964(Integer val)
	{
		return getHtmlAppends(val);
	}

	public static String DialogAppend_30006(Integer val)
	{
		return getHtmlAppends(val);
	}

	public static String DialogAppend_30134(Integer val)
	{
		return getHtmlAppends(val);
	}

	public static String DialogAppend_30146(Integer val)
	{
		return getHtmlAppends(val);
	}

	public static String DialogAppend_32163(Integer val)
	{
		return getHtmlAppends(val);
	}

	public static String DialogAppend_30576(Integer val)
	{
		return getHtmlAppends(val);
	}

	public static String DialogAppend_30540(Integer val)
	{
		return getHtmlAppends(val);
	}

	private static final String en = "<br1>[scripts_services.tele.TeleToGH:toGH @811;Giran Harbor|\"I want free admision to the Giran Harbor.\"]<br1>";
	private static final String ru = "<br1>[scripts_services.tele.TeleToGH:toGH @811;Giran Harbor|\"Я хочу бесплатно попасть в Giran Harbor.\"]<br1>";

	public static String getHtmlAppends(Integer val)
	{
		if(val != 0 || !Config.SERVICES_GIRAN_HARBOR_ENABLED)
			return "";
		L2Player player = (L2Player) self;
		if(player == null || player.getVar("lang@") == null)
			return "";
		String pl_var = player.getVar("lang@");
		if(pl_var != null && pl_var.equalsIgnoreCase("ru"))
			return ru;
		return en;
	}

	public class ZoneListener extends L2ZoneEnterLeaveListener
	{
		@Override
		public void objectEntered(L2Zone zone, L2Object object)
		{
		// обрабатывать вход в зону не надо, только выход
		}

		@Override
		public void objectLeaved(L2Zone zone, L2Object object)
		{
			L2Player player = object.getPlayer();
			if(Config.SERVICES_GIRAN_HARBOR_ENABLED && player.isVisible())
			{
				L2Playable playable = (L2Playable) object;
				double angle = Util.convertHeadingToDegree(playable.getHeading()); // угол в градусах
				double radian = Math.toRadians(angle - 90); // угол в радианах
				playable.teleToLocation((int) (playable.getX() + 50 * Math.sin(radian)), (int) (playable.getY() - 50 * Math.cos(radian)), playable.getZ());
			}
		}
	}
}