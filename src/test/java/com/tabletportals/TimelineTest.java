package com.tabletportals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class TimelineTest
{
	/** The game moves you about 3 game ticks (~90 client ticks) after breaking the tablet. */
	private static final int TELEPORT_ARRIVES = 85;

	@Test
	public void everyStyleIsInsideThePortalBeforeTheTeleport()
	{
		for (SmashStyle style : SmashStyle.values())
		{
			Timeline tl = new Timeline(style);
			assertTrue(style + " portal opens after the tablet shatters", tl.portalStart > tl.impact);
			assertTrue(style + " walk starts once the smash is over", tl.walkStart >= style.getLength());
			assertTrue(style + " walk starts once the portal is open", tl.walkStart >= tl.portalStart + Timeline.PORTAL_OPEN);
			assertTrue(style + " steps in before the teleport (" + tl.enter + ")", tl.enter < TELEPORT_ARRIVES);
			for (int s : tl.smoke)
			{
				assertTrue(s > tl.portalStart && s < tl.walkStart);
			}
		}
	}

	@Test
	public void portalOpensWithAPopAndCloses()
	{
		assertEquals(0f, Timeline.opening(0, 16), 1e-6);
		assertEquals(1f, Timeline.opening(16, 16), 1e-6);
		float max = 0;
		for (int t = 0; t <= 16; t++)
		{
			max = Math.max(max, Timeline.opening(t, 16));
		}
		assertTrue("overshoots a little", max > 1.02f && max < 1.15f);

		assertEquals(1f, Timeline.closing(0, 10), 1e-6);
		assertEquals(0f, Timeline.closing(10, 10), 1e-6);
		for (int t = 1; t <= 10; t++)
		{
			assertTrue(Timeline.closing(t, 10) <= Timeline.closing(t - 1, 10));
		}
	}

	@Test
	public void walkProgressIsClamped()
	{
		assertEquals(0f, Timeline.progress(-5, 30), 1e-6);
		assertEquals(0.5f, Timeline.progress(15, 30), 1e-6);
		assertEquals(1f, Timeline.progress(99, 30), 1e-6);
	}
}
