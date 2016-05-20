package l2d.game.model.instances;

import l2d.game.templates.L2NpcTemplate;

public final class L2TrainerInstance extends L2NpcInstance // deprecated?
{
	//private static Logger _log = Logger.getLogger(L2TrainerInstance.class.getName());

	public L2TrainerInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";
		if(val == 0)
			pom = "" + npcId;
		else
			pom = npcId + "-" + val;

		return "data/html/trainer/" + pom + ".htm";
	}
}
