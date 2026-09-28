package com.tabletportals;

import net.runelite.api.Player;

/**
 * Makes a player walk on the spot, on your screen only, by swapping all their movement animations for their
 * walk, and puts them back afterwards. The walking copy drawn by {@link Walker} takes its model from the
 * player, so this is what makes the copy's legs move.
 */
final class Poser
{
	/** The player's own movement animations while posed, or null. */
	private int[] saved;
	private int walk = -1;

	boolean isPosed()
	{
		return saved != null;
	}

	/** Starts walking on the spot. Cheap to call again. */
	void apply(Player player)
	{
		// First time, or the game has reset the animations (e.g. after the teleport): capture them afresh.
		if (saved == null || player.getIdlePoseAnimation() != walk)
		{
			saved = new int[]{
				player.getIdlePoseAnimation(),
				player.getWalkAnimation(),
				player.getRunAnimation(),
				player.getIdleRotateLeft(),
				player.getIdleRotateRight(),
				player.getWalkRotateLeft(),
				player.getWalkRotateRight(),
				player.getWalkRotate180(),
			};
			walk = saved[1];
		}

		player.setIdlePoseAnimation(walk);
		player.setWalkAnimation(walk);
		player.setRunAnimation(walk);
		player.setIdleRotateLeft(walk);
		player.setIdleRotateRight(walk);
		player.setWalkRotateLeft(walk);
		player.setWalkRotateRight(walk);
		player.setWalkRotate180(walk);
		hold(player);
	}

	/** Makes sure the walk is showing this frame (the game can switch the pose back between ticks). */
	void hold(Player player)
	{
		if (saved == null)
		{
			return;
		}
		if (player.getPoseAnimation() != walk)
		{
			player.setPoseAnimation(walk);
			player.setPoseAnimationFrame(0);
		}
	}

	/** Puts the player's own animations back. */
	void restore(Player player)
	{
		if (player != null && saved != null)
		{
			player.setIdlePoseAnimation(saved[0]);
			player.setWalkAnimation(saved[1]);
			player.setRunAnimation(saved[2]);
			player.setIdleRotateLeft(saved[3]);
			player.setIdleRotateRight(saved[4]);
			player.setWalkRotateLeft(saved[5]);
			player.setWalkRotateRight(saved[6]);
			player.setWalkRotate180(saved[7]);
			player.setPoseAnimation(saved[0]);
			player.setPoseAnimationFrame(0);
		}
		saved = null;
		walk = -1;
	}

	/** Forgets without touching the player (after logout or a world hop). */
	void forget()
	{
		saved = null;
		walk = -1;
	}
}
