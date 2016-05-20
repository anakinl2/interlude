package l2d.game.skills.effects;

import java.util.StringTokenizer;

import javolution.util.FastList;
import l2d.game.model.L2Effect;
import l2d.game.model.L2Skill;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.skills.Env;

public class EffectCancelEffect extends L2Effect
{
	public EffectCancelEffect(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.CancelEffect;
	}

	@Override
	public void onStart()
	{
		if(getOptions() != null && getEffected() != null)
			try
			{
				int skillId = Integer.parseInt(getOptions());
				for(L2Effect e : getEffected().getEffectList().getAllEffects())
					if(e != null && e.getSkill().getId() == skillId)
					{
						if(!e.isHidden() && e.isInUse())
							getEffected().sendPacket(new SystemMessage(SystemMessage.THE_EFFECT_OF_S1_HAS_BEEN_REMOVED).addSkillName(e.getSkill().getId(), e.getSkill().getLevel()));
						e.exit();
					}
			}
			catch(NumberFormatException ex)
			{
				if(getOptions().length() > 0)
				{
					FastList<String> optionsList = new FastList<String>();
					StringTokenizer st = new StringTokenizer(getOptions().trim(), ",");
					while(st.countTokens() > 0)
						optionsList.add(st.nextToken());
					for(L2Effect e : getEffected().getEffectList().getAllEffects())
					{
						if(e == null)
							continue;
						for(String opt : optionsList)
							if(opt.equalsIgnoreCase("all-debuff"))
							{
								if(e.getSkill().isDebuff())
								{
									if(!e.isHidden() && e.isInUse())
										getEffected().sendPacket(new SystemMessage(SystemMessage.THE_EFFECT_OF_S1_HAS_BEEN_REMOVED).addSkillName(e.getSkill().getId(), e.getSkill().getLevel()));
									e.exit();
								}
							}
							else if(opt.equalsIgnoreCase("all-dance"))
							{
								if(e.getSkill().getSkillType() == L2Skill.SkillType.MUSIC)
								{
									if(!e.isHidden() && e.isInUse())
										getEffected().sendPacket(new SystemMessage(SystemMessage.THE_EFFECT_OF_S1_HAS_BEEN_REMOVED).addSkillName(e.getSkill().getId(), e.getSkill().getLevel()));
									e.exit();
								}
							}
							else if(e.getStackType().equalsIgnoreCase(opt))
							{
								if(!e.isHidden() && e.isInUse())
									getEffected().sendPacket(new SystemMessage(SystemMessage.THE_EFFECT_OF_S1_HAS_BEEN_REMOVED).addSkillName(e.getSkill().getId(), e.getSkill().getLevel()));
								e.exit();
							}
					}
				}
			}
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}

	@Override
	public void onExit()
	{}
}