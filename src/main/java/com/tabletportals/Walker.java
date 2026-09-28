package com.tabletportals;

import java.util.function.Consumer;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.RuneLiteObjectController;
import net.runelite.api.coords.LocalPoint;

/**
 * A copy of your character drawn walking from one spot to another, while the real one is hidden. The model
 * is taken from the player every frame, so it looks exactly like you, gear and all, mid-stride.
 */
final class Walker extends RuneLiteObjectController
{
	private final Client client;
	private final Player player;
	/** Run just before the model is taken, to keep the player in their walk for this frame. */
	private final Consumer<Player> beforeDraw;

	private LocalPoint from;
	private LocalPoint to;
	private int plane;

	Walker(Client client, Player player, Consumer<Player> beforeDraw)
	{
		this.client = client;
		this.player = player;
		this.beforeDraw = beforeDraw;
	}

	void walk(LocalPoint from, LocalPoint to, int plane, int orientation)
	{
		this.from = from;
		this.to = to;
		this.plane = plane;
		setOrientation(orientation);
		moveTo(0f);
		if (!client.isRuneLiteObjectRegistered(this))
		{
			client.registerRuneLiteObject(this);
		}
	}

	/** Moves along the walk, 0 at the start and 1 at the end. */
	void moveTo(float progress)
	{
		if (from == null || to == null)
		{
			return;
		}
		int x = Math.round(from.getX() + (to.getX() - from.getX()) * progress);
		int y = Math.round(from.getY() + (to.getY() - from.getY()) * progress);
		LocalPoint at = new LocalPoint(x, y, from.getWorldView());
		setLocation(at, plane);
		setZ(Perspective.getTileHeight(client, at, plane));
	}

	void remove()
	{
		if (client.isRuneLiteObjectRegistered(this))
		{
			client.removeRuneLiteObject(this);
		}
	}

	boolean isFor(Player p)
	{
		return p == player;
	}

	@Override
	public Model getModel()
	{
		beforeDraw.accept(player);
		return player.getModel();
	}
}
