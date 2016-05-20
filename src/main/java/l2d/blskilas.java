package l2d;

public class blskilas
{
	int skillId = 0;
	int skilllvl = 0;
	int power = 0;
	String name = "";

	public blskilas(int skillId2, int skilllvl2, int poweras, String name2)
	{
		skillId = skillId2;
		skilllvl = skilllvl2;
		power = poweras;
		name = name2;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public int getSkillId()
	{
		return skillId;
	}

	public void setSkillId(int skillId)
	{
		this.skillId = skillId;
	}

	public int getSkilllvl()
	{
		return skilllvl;
	}

	public void setSkilllvl(int skilllvl)
	{
		this.skilllvl = skilllvl;
	}

	public int getPower()
	{
		return power;
	}

	public void setPower(int power)
	{
		this.power = power;
	}
}
