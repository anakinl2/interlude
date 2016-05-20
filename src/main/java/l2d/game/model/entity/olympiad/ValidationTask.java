package l2d.game.model.entity.olympiad;

import l2d.game.Announcements;
import l2d.game.model.entity.Hero;

public class ValidationTask implements Runnable
{
	@Override
	public void run()
	{
		OlympiadDatabase.sortHerosToBe();
		Olympiad.giveHeroBonus();
		OlympiadDatabase.saveNobleData(); //Сохраняем героев-ноблесов, получивших бонус в виде очков
		if(Hero.getInstance().computeNewHeroes(Olympiad._heroesToBe))
			Olympiad._log.warning("Olympiad: Error while computing new heroes!");
		Announcements.getInstance().announceToAll("Olympiad Validation Period has ended");
		Olympiad._period = 0;
		Olympiad._currentCycle++;
		OlympiadDatabase.cleanupNobles();
		OlympiadDatabase.setNewOlympiadEnd();
		Olympiad.init();
	}
}