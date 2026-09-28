package com.tabletportals;

import static org.junit.Assert.assertArrayEquals;
import org.junit.Test;

public class FacingTest
{
	@Test
	public void aheadFollowsTheCompass()
	{
		assertArrayEquals(new int[]{0, -128}, Facing.ahead(0, 128));    // south
		assertArrayEquals(new int[]{-128, 0}, Facing.ahead(512, 128));  // west
		assertArrayEquals(new int[]{0, 128}, Facing.ahead(1024, 128));  // north
		assertArrayEquals(new int[]{128, 0}, Facing.ahead(1536, 128));  // east
		assertArrayEquals(new int[]{0, 128}, Facing.ahead(0, -128));    // behind someone facing south
	}

	@Test
	public void rightHandSide()
	{
		assertArrayEquals(new int[]{-100, 0}, Facing.right(0, 100));    // facing south, right is west
		assertArrayEquals(new int[]{100, 0}, Facing.right(1024, 100));  // facing north, right is east
	}
}
