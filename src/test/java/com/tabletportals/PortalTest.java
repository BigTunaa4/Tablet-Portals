package com.tabletportals;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class PortalTest
{
	@Test
	public void picksTheNearestBuiltSize()
	{
		assertEquals(0, Portal.nearestStep(0f));
		assertEquals(10, Portal.nearestStep(1f));
		assertEquals(12, Portal.nearestStep(1.3f));
		assertEquals(5, Portal.nearestStep(0.52f));
	}
}
