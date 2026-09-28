package com.tabletportals;

/**
 * When each beat of the send-off happens, in client ticks (20 ms) from the moment the tablet is broken.
 *
 * <p>A tablet teleport moves you about three game ticks (~90 client ticks) after you break it: the game's own
 * animations are a 44-tick smash followed by a 41-tick "absorb". Everything up to stepping into the portal
 * is fitted inside that window so it is never cut short; the portal closing behind you can run over it.
 */
final class Timeline
{
	/** How long the portal takes to open. */
	static final int PORTAL_OPEN = 16;
	/** How long the walk into the portal takes (one tile at walking pace: 128 units per 30 ticks). */
	static final int WALK = 30;
	/** How long the portal takes to close. */
	static final int PORTAL_CLOSE = 10;
	/** How far in front of you the portal opens, in local units (one tile). */
	static final int PORTAL_DISTANCE = 128;
	/** Give up waiting for the teleport after this long (e.g. it was blocked) and show the player again. */
	static final int GIVE_UP = 200;

	/** When stepping out at the destination: the portal opens, then you walk out of it. */
	static final int EXIT_OPEN = 8;
	static final int EXIT_WALK = 30;

	final int impact;
	final int portalStart;
	final int[] smoke;
	final int walkStart;
	final int enter;
	final int closed;

	Timeline(SmashStyle style)
	{
		impact = style.getImpact();
		portalStart = impact + 2;
		smoke = new int[]{portalStart + 2, portalStart + 7, portalStart + 12};
		walkStart = Math.max(style.getLength(), portalStart + PORTAL_OPEN) + 2;
		enter = walkStart + WALK;
		closed = enter + PORTAL_CLOSE;
	}

	/** Portal size (0 to 1, with a little overshoot) while opening; {@code t} is ticks since it started. */
	static float opening(int t, int length)
	{
		if (t <= 0)
		{
			return 0f;
		}
		if (t >= length)
		{
			return 1f;
		}
		// Ease out with a slight overshoot, so the portal "pops" open.
		float x = t / (float) length - 1f;
		float c1 = 1.4f;
		float c3 = c1 + 1f;
		return 1f + c3 * x * x * x + c1 * x * x;
	}

	/** Portal size (1 to 0) while closing; {@code t} is ticks since it started. */
	static float closing(int t, int length)
	{
		if (t <= 0)
		{
			return 1f;
		}
		if (t >= length)
		{
			return 0f;
		}
		float x = t / (float) length;
		return 1f - x * x;
	}

	/** How far along a walk is, 0 to 1. */
	static float progress(int t, int length)
	{
		return Math.max(0f, Math.min(1f, t / (float) length));
	}
}
