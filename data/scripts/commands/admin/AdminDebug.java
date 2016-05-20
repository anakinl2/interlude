package commands.admin;

import java.io.File;
import java.io.FileWriter;
import java.text.Collator;
import java.util.HashMap;
import java.util.TreeSet;

import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.handler.AdminCommandHandler;
import com.lineage.game.handler.IAdminCommandHandler;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2MonsterInstance;
import com.lineage.game.tables.NpcTable;
import com.lineage.game.templates.L2NpcTemplate;


public class AdminDebug implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_dump_mobs_aggro_info,
		admin_dump_commands,
	}

	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, L2Player activeChar)
	{
		Commands command = (Commands) comm;

		if(!activeChar.getPlayerAccess().IsGM)
			return false;
		String out;

		switch(command)
		{
			case admin_dump_mobs_aggro_info:
				L2NpcTemplate[] npcs = NpcTable.getAll();
				out = "<?php\r\n";
				for(L2NpcTemplate npc : npcs)
					if(npc.isInstanceOf(L2MonsterInstance.class))
						out += "\t$monsters[" + npc.getNpcId() + "]=array('level'=>" + npc.level + ",'aggro'=>" + npc.aggroRange + ");\r\n";
				out += "?>";
				Str2File("monsters.php", out);
				activeChar.sendMessage("Monsters info dumped, checkout for monsters.php in the root of server");
				break;
			case admin_dump_commands:
				out = "Commands list:\r\n";

				HashMap<IAdminCommandHandler, TreeSet<String>> handlers = new HashMap<IAdminCommandHandler, TreeSet<String>>();
				for(String cmd : AdminCommandHandler.getInstance().getAllCommands())
				{
					IAdminCommandHandler key = AdminCommandHandler.getInstance().getAdminCommandHandler(cmd);
					if(!handlers.containsKey(key))
						handlers.put(key, new TreeSet<String>(Collator.getInstance()));
					handlers.get(key).add(cmd.replaceFirst("admin_", ""));
				}

				for(IAdminCommandHandler key : handlers.keySet())
				{
					out += "\r\n\t************** Group: " + key.getClass().getSimpleName().replaceFirst("Admin", "") + " **************\r\n";
					for(String cmd : handlers.get(key))
						out += "//" + cmd + " - \r\n";
				}
				Str2File("admin_commands.txt", out);
				activeChar.sendMessage("Commands list dumped, checkout for admin_commands.txt in the root of server");
				break;
		}

		return true;
	}

	private static void Str2File(String fileName, String data)
	{
		File file = new File(fileName);
		if(file.exists())
			file.delete();
		try
		{
			file.createNewFile();
			FileWriter save = new FileWriter(file, false);
			save.write(data);
			save.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public Enum[] getAdminCommandEnum()
	{
		return Commands.values();
	}

	public void onLoad()
	{
		AdminCommandHandler.getInstance().registerAdminCommandHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}