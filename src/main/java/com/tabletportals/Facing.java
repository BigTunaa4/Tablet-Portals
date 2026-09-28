package com.tabletportals;

/**
 * Directions from the game's orientation angle: 2048 units to a full turn, 0 facing south, 512 west,
 * 1024 north and 1536 east. Local x grows to the east and y to the north.
 */
final class Facing
{
	private Facing()
	{
	}

	/** The step, in local units, {@code distance} ahead of something facing {@code orientation}. */
	static int[] ahead(int orientation, int distance)
	{
		double a = (orientation & 2047) * Math.PI * 2 / 2048;
		return new int[]{
			(int) Math.round(-Math.sin(a) * distance),
			(int) Math.round(-Math.cos(a) * distance),
		};
	}

	/** The step {@code distance} to the right of something facing {@code orientation}. */
	static int[] right(int orientation, int distance)
	{
		// Facing south, your right hand is to the west.
		return ahead(orientation + 512, distance);
	}
}
