package com.tabletportals;

/** How much smoke billows around the portal. */
public enum SmokeAmount
{
	NONE("None", 0),
	LIGHT("Light", 1),
	NORMAL("Normal", 3),
	THICK("Thick", 5);

	private final String label;
	private final int puffs;

	SmokeAmount(String label, int puffs)
	{
		this.label = label;
		this.puffs = puffs;
	}

	int getPuffs()
	{
		return puffs;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
