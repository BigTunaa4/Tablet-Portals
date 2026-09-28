package com.tabletportals;

import net.runelite.api.gameval.AnimationID;

/**
 * How your character breaks the tablet. Timings are in client ticks (20 ms), read from the animations in the
 * game cache: {@code length} is how long the animation runs and {@code impact} is the frame where the hands
 * come down, which is when the tablet shatters.
 */
public enum SmashStyle
{
	OVERHEAD_SLAM("Overhead slam", AnimationID.DRAGON_WARHAMMER_SA_PLAYER, 40, 22),
	QUICK_CRUSH("Quick crush", AnimationID.SLAYER_GRANITE_MAUL_SPECIAL_ATTACK, 18, 8),
	CLASSIC("Classic", AnimationID.POH_SMASH_MAGIC_TABLET, 44, 24);

	private final String label;
	private final int animation;
	private final int length;
	private final int impact;

	SmashStyle(String label, int animation, int length, int impact)
	{
		this.label = label;
		this.animation = animation;
		this.length = length;
		this.impact = impact;
	}

	public int getAnimation()
	{
		return animation;
	}

	public int getLength()
	{
		return length;
	}

	public int getImpact()
	{
		return impact;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
