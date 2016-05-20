import com.lineage.Config;
import com.lineage.ext.scripts.Functions;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.cache.Msg;
import com.lineage.game.geodata.GeoEngine;
import com.lineage.game.instancemanager.TownManager;
import com.lineage.game.instancemanager.ZoneManager;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2World;
import com.lineage.game.model.L2Zone;
import com.lineage.game.model.L2Zone.ZoneType;
import com.lineage.game.model.entity.SevenSigns;
import com.lineage.game.model.entity.residence.Castle;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.util.Location;

public class Util extends Functions implements ScriptFile 
{

	public static L2Object self;
    public static L2Object npc;

    public void onLoad() 
    {
        System.out.println("Utilites Loaded");
    }

    public void onReload() 
    {}

    public void onShutdown() 
    {}

    /**
     * Перемещает за плату в аденах
     *
     * @param x
     * @param y
     * @param z
     * @param price
     */
    public void Gatekeeper(String[] param) 
    {
        if(param.length < 4) 
        	throw new IllegalArgumentException();
        
        L2Player player = (L2Player) self;
        if(player == null) 
        	return;
        
        int price = Integer.parseInt(param[3]);

        if(player.isActionsDisabled() || player.isSitting() || player.getLastNpc() == null || player.getLastNpc().getDistance(player) > 300) 
        	return;
        
        if(price > 0 && player.getAdena() < price) 
        {
            player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
            return;
        }

        if(player.getMountType() == 2) 
        {
        	if(player.getLang().equalsIgnoreCase("ru"))
        		player.sendMessage("Телепортация верхом на виверне невозможна.");
        	else
        		player.sendMessage("Teleportation astride wyvern is impossible.");
            return;
        }

        /* Затычка, npc Mozella не ТПшит чаров уровень которых превышает заданный в конфиге
         * Off Like >= 56 lvl, данные по ограничению lvl'a устанавливаются в altsettings.properties.
         */
        if(player.getLastNpc() != null) 
        {
            int mozella_cruma = 30483; // NPC Mozella id 30483
            if(player.getLastNpc().getNpcId() == mozella_cruma && player.getLevel() >= Config.CRUMA_GATEKEEPER_LVL) 
            {
                show("data/html/teleporter/30483-no.htm", player);
                return;
            }
        }

        int x = Integer.parseInt(param[0]);
        int y = Integer.parseInt(param[1]);
        int z = Integer.parseInt(param[2]);

        // Нельзя телепортироваться в города, где идет осада
        // Узнаем, идет ли осада в ближайшем замке к точке телепортации
        Castle castle = TownManager.getInstance().getClosestTown(x, y).getCastle();
        if(castle != null && castle.getSiege().isInProgress())
        {
            // Определяем, в город ли телепортируется чар
            boolean teleToTown = false;
            int townId = 0;
            for(L2Zone town : ZoneManager.getInstance().getZoneByType(ZoneType.Town))
            {
                if(town.checkIfInZone(x, y))
                {
                    teleToTown = true;
                    townId = town.getIndex();
                    break;
                }
            }

            if(teleToTown && townId == castle.getTown())
            {
                player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_TELEPORT_TO_A_VILLAGE_THAT_IS_IN_A_SIEGE));
                return;
            }
        }

        Location pos = GeoEngine.findPointToStay(x, y, z, 10, 50);

        if(price > 0)
        {
            player.reduceAdena(price);
        }
        player.teleToLocation(pos);
    }

    
    public void PartyGatekeeper(String[] param) 
    {
        if(param.length < 4) 
        	throw new IllegalArgumentException();
        
        L2Player player = (L2Player) self;
        if(player == null) 
        	return;

        if(player.isActionsDisabled() || player.isSitting() || player.getLastNpc() == null || player.getLastNpc().getDistance(player) > 300) 
        	return;


        if(player.getMountType() == 2) 
        {
        	if(player.getLang().equalsIgnoreCase("ru"))
        		player.sendMessage("Телепортация верхом на виверне невозможна.");
        	else
        		player.sendMessage("Teleportation riding wyvern is impossible.");
            return;
        }

        int x = Integer.parseInt(param[0]);
        int y = Integer.parseInt(param[1]);
        int z = Integer.parseInt(param[2]);
        Location pos = GeoEngine.findPointToStay(x, y, z, 10, 100);

        
  			if(!player.isInParty())
  			{
  				player.sendMessage("You are not in party!");
  				return;
  			}

  			if(!player.getParty().isLeader(player))
  			{
  				player.sendMessage("You are not party leader.");
  				return;
  			}
  			
  			for(L2Player pl : L2World.getAroundPlayers(player, 1000, 200))
  				if(pl.isInParty() && pl.getParty().isLeader(player))
  					pl.teleToLocation(pos.rnd(0, 50));
  			
  			player.teleToLocation(pos.rnd(0, 50));
    }
    
    public void SSGatekeeper(String[] param)
    {
        if(param.length < 4)
        	throw new IllegalArgumentException();
        
        L2Player player = (L2Player) self;
        if(player == null)
        	return;
        
        int type = Integer.parseInt(param[3]);

        if(player.isActionsDisabled() || player.isSitting() || player.getLastNpc() == null || player.getLastNpc().getDistance(player) > 300)
        	return;

        if(type > 0)
        {
            int player_cabal = SevenSigns.getInstance().getPlayerCabal(player);
            int period = SevenSigns.getInstance().getCurrentPeriod();
            if(period == SevenSigns.PERIOD_COMPETITION && player_cabal == SevenSigns.CABAL_NULL) 
            {
                player.sendPacket(new SystemMessage(SystemMessage.USED_ONLY_DURING_A_QUEST_EVENT_PERIOD));
                return;
            }

            int winner;
            if(period == SevenSigns.PERIOD_SEAL_VALIDATION && (winner = SevenSigns.getInstance().getCabalHighestScore()) != SevenSigns.CABAL_NULL)
            {
                if(winner != player_cabal) 
                	return;
                if(type == 1 && SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_AVARICE) != player_cabal)
                    return;
                if(type == 2 && SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_GNOSIS) != player_cabal)
                    return;
                
            }
        }
        player.teleToLocation(Integer.parseInt(param[0]), Integer.parseInt(param[1]), Integer.parseInt(param[2]));
    }

    /**
     * Перемещает за определенный предмет
     *
     * @param x
     * @param y
     * @param z
     * @param count
     * @param item
     */
    public void QuestGatekeeper(String[] param) 
    {
        if(param.length < 5) 
        	throw new IllegalArgumentException();
        
        L2Player player = (L2Player) self;
        if(player == null) 
        	return;
        
        int count = Integer.parseInt(param[3]);
        int item = Integer.parseInt(param[4]);

        if(player.isActionsDisabled() || player.isSitting() || player.getLastNpc() == null || player.getLastNpc().getDistance(player) > 300)
        	return;
        
        if(count > 0) 
        {
            L2ItemInstance ii = player.getInventory().getItemByItemId(item);
            if(ii == null || ii.getIntegerLimitedCount() < count) 
            {
                player.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_ENOUGH_REQUIRED_ITEMS));
                return;
            }
            player.getInventory().destroyItem(ii, count, true);

            if(count > 1) 
            	player.sendPacket(new SystemMessage(SystemMessage.S2_S1_HAS_DISAPPEARED).addItemName(item).addNumber(count));
            else
            	player.sendPacket(new SystemMessage(SystemMessage.S1_HAS_DISAPPEARED).addItemName(item));
        }

        int x = Integer.parseInt(param[0]);
        int y = Integer.parseInt(param[1]);
        int z = Integer.parseInt(param[2]);

        Location pos = GeoEngine.findPointToStay(x, y, z, 20, 70);

        player.teleToLocation(pos);
    }

    public void ReflectionGatekeeper(String[] param) 
    {
        if(param.length < 5)
            throw new IllegalArgumentException();
        
        L2Player player = (L2Player) self;
        if(player == null) 
        	return;
       
        player.setReflection(Integer.parseInt(param[4]));
        Gatekeeper(param);
    }

    /**
     * Используется для телепортации за Newbie Token, проверяет уровень и передает
     * параметры в QuestGatekeeper
     */
    public void TokenJump(String[] param) 
    {
        L2Player player = (L2Player) self;
        if(player == null) 
        	return;
        if(player.getLevel() <= 19) 
        	QuestGatekeeper(param);
        else 
        	show("Only for newbies", player);
    }

    public void NoblessTeleport() 
    {
        L2Player player = (L2Player) self;
        if(player == null) 
        	return;
        if(player.isNoble() || Config.ALLOW_NOBLE_TP_TO_ALL) 
        	show("data/scripts/noble.htm", player);
        else
            show("data/scripts/nobleteleporter-no.htm", player);
    }

    public void PayPage(String[] param) 
    {
        if(param.length < 2) 
        	throw new IllegalArgumentException();
        
        L2Player player = (L2Player) self;
        if(player == null) 
        	return;
        
        String page = param[0];
        int item = Integer.parseInt(param[1]);
        int price = Integer.parseInt(param[2]);

        if(getItemCount(player, item) < price) 
        {
            if(item == 57) 
            	player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
            else 
            	player.sendPacket(Msg.INCORRECT_ITEM_COUNT);
            return;
        }
        removeItem(player, item, price);
        show(page, player);
    }

    public void TakeNewbieWeaponCoupon() 
    {
        L2Player player = (L2Player) self;
        if(player == null) 
        	return;
        if(player.getLevel() > 19 || player.getClassId().getLevel() > 1)
        {
        	if(player.getLang().equalsIgnoreCase("ru"))
        		show("Ваш уровень слишков высокий!", player);
        	else
        		show("Your level is too high!", player);
            return;
        }
        if(player.getLevel() < 6) 
        {
        	if(player.getLang().equalsIgnoreCase("ru"))
        		show("Ваш уровень слишком низкий!", player);
        	else
        		show("Your level is too low!", player);
            return;
        }
        if(player.getVarB("newbieweapon")) 
        {
        	if(player.getLang().equalsIgnoreCase("ru"))
        		show("Вы уже получили ваше оружие для новичка!", player);
        	else
        		show("Your already got your newbie weapon!", player);
            return;
        }
        addItem(player, 7832, 5);
        player.setVar("newbieweapon", "true");
    }

    public void TakeAdventurersArmorCoupon() 
    {
        L2Player player = (L2Player) self;
        if(player == null) 
        	return;
        if(player.getLevel() > 39 || player.getClassId().getLevel() > 2)
        {
        	if(player.getLang().equalsIgnoreCase("ru"))
        		show("Ваш уровень слишков высокий!", player);
        	else
        		show("Your level is too high!", player);
            return;
        }
        if(player.getLevel() < 20 || player.getClassId().getLevel() < 2)
        {
        	if(player.getLang().equalsIgnoreCase("ru"))
        		show("Ваш уровень слишком низкий!", player);
        	else
        		show("Your level is too low!", player);
            return;
        }
        if(player.getVarB("newbiearmor"))
        {
        	if(player.getLang().equalsIgnoreCase("ru"))
        		show("Вы уже получили ваше оружие для новичка!", player);
        	else
        		show("Your already got your newbie weapon!", player);
            return;
        }
        addItem(player, 7833, 1);
        player.setVar("newbiearmor", "true");
    }
}