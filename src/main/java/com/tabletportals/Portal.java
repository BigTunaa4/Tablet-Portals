package com.tabletportals;

import java.awt.Color;
import net.runelite.api.Animation;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;

/**
 * The swirling portal: the game's own portal swirl (the one inside house and Clan Wars portals), standing on
 * its own without a frame, with its spinning animation. It can grow and shrink; the sizes are built ahead of
 * time so nothing is rebuilt while it animates.
 */
final class Portal
{
	/** The portal swirl model, and its spin animation, as used by the house portal objects. */
	static final int SWIRL_MODEL = 11233;
	static final int SWIRL_ANIMATION = 3174;

	/** The swirl model spans y -315..-55 (up is negative) and sits at z -37..-21; its middle is here. */
	private static final int MODEL_CENTRE_Y = -185;
	private static final int MODEL_CENTRE_Z = -29;
	private static final int MODEL_HALF_HEIGHT = 130;
	/** At 100% size the swirl is 0.9x the game's own: 234 units tall, a little taller than a player. */
	private static final float BASE_SCALE = 0.9f;
	/** How high the bottom edge floats above the ground once fully open. */
	private static final int HOVER = 30;

	/** The sizes (fractions of fully open) that are built; the nearest is shown. */
	static final float[] STEPS = {0.05f, 0.12f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1f, 1.06f, 1.12f};

	private final RuneLiteObject obj;
	private final Model[] models = new Model[STEPS.length];
	private int shown = -1;

	private Portal(RuneLiteObject obj)
	{
		this.obj = obj;
	}

	/** Builds a portal, or returns null if the game hasn't loaded the models yet. */
	static Portal build(Client client, Color tint, int sizePercent)
	{
		ModelData base = client.loadModelData(SWIRL_MODEL);
		Animation spin = client.loadAnimation(SWIRL_ANIMATION);
		if (base == null)
		{
			return null;
		}
		base = base.shallowCopy().cloneColors();
		GraphicDef.tint(base, tint);

		float full = BASE_SCALE * sizePercent / 100f;
		// Once open, the middle of the swirl sits here, so it hovers just above the ground at any size.
		int centreY = -Math.round(HOVER + MODEL_HALF_HEIGHT * full);

		RuneLiteObject obj = client.createRuneLiteObject();
		Portal p = new Portal(obj);
		for (int i = 0; i < STEPS.length; i++)
		{
			int k = Math.max(1, Math.round(128 * full * STEPS[i]));
			ModelData md = base.shallowCopy().cloneVertices()
				.translate(0, -MODEL_CENTRE_Y, -MODEL_CENTRE_Z) // centre the swirl on the origin
				.scale(k, k, k) // grow from the middle
				.translate(0, centreY, 0);
			p.models[i] = md.light(ModelData.DEFAULT_AMBIENT, ModelData.DEFAULT_CONTRAST,
				ModelData.DEFAULT_X, ModelData.DEFAULT_Y, ModelData.DEFAULT_Z);
			if (p.models[i] == null)
			{
				return null;
			}
		}
		obj.setModel(p.models[0]);
		if (spin != null)
		{
			obj.setAnimation(spin); // loops by default
		}
		return p;
	}

	static int nearestStep(float size)
	{
		int best = 0;
		for (int i = 1; i < STEPS.length; i++)
		{
			if (Math.abs(STEPS[i] - size) < Math.abs(STEPS[best] - size))
			{
				best = i;
			}
		}
		return best;
	}

	/** Puts the portal at a spot, facing the same way as whoever walks into (or out of) it. */
	void place(LocalPoint at, int plane, int orientation)
	{
		obj.setLocation(at, plane);
		obj.setOrientation(orientation);
	}

	/** Shows the portal at a size from 0 (gone) to 1 (fully open), a little over 1 for the pop. */
	void setSize(float size)
	{
		if (size < 0.03f)
		{
			obj.setActive(false);
			shown = -1;
			return;
		}
		int i = nearestStep(size);
		if (i != shown)
		{
			obj.setModel(models[i]);
			shown = i;
		}
		if (!obj.isActive())
		{
			obj.setActive(true);
		}
	}

	void remove()
	{
		obj.setActive(false);
		shown = -1;
	}

	LocalPoint location()
	{
		return obj.getLocation();
	}
}
