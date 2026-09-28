package com.tabletportals;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Animation;
import net.runelite.api.AnimationController;
import net.runelite.api.Client;
import net.runelite.api.IndexDataBase;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.Perspective;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One-shot effects (smoke puffs, the tablet shattering) built from the game's own graphics and drawn in the
 * world like any spell effect, optionally recoloured.
 */
@Singleton
class Effects
{
	private static final Logger log = LoggerFactory.getLogger(Effects.class);

	/** Cache archive holding graphic (spotanim) definitions. */
	private static final int SPOTANIM_ARCHIVE = 13;

	private static final int AMBIENT = 64;
	private static final int CONTRAST = 850;

	private final Client client;
	private final Map<Integer, GraphicDef> graphics = new HashMap<>();
	/** Graphics that couldn't be read, so we don't keep trying every time. */
	private final Map<Integer, Boolean> broken = new HashMap<>();
	private final List<RuneLiteObject> active = new ArrayList<>();

	@Inject
	Effects(Client client)
	{
		this.client = client;
	}

	/**
	 * Plays a graphic once at a spot.
	 *
	 * @param height how far above the ground, in local units
	 * @param color  tint, or null for the graphic's own colours
	 */
	void play(int graphicId, LocalPoint at, int plane, int height, int orientation, Color color)
	{
		GraphicDef g = graphic(graphicId);
		if (g == null || at == null || g.animation < 0)
		{
			return;
		}
		ModelData md = client.loadModelData(g.model);
		Animation anim = client.loadAnimation(g.animation);
		if (md == null || anim == null)
		{
			return;
		}

		md = md.shallowCopy().cloneColors();
		if (g.recolorFind != null)
		{
			for (int i = 0; i < Math.min(g.recolorFind.length, g.recolorReplace.length); i++)
			{
				md.recolor(g.recolorFind[i], g.recolorReplace[i]);
			}
		}
		if (g.retextureFind != null)
		{
			md = md.cloneTextures();
			for (int i = 0; i < Math.min(g.retextureFind.length, g.retextureReplace.length); i++)
			{
				md.retexture(g.retextureFind[i], g.retextureReplace[i]);
			}
		}
		for (int i = 0; i < Math.floorMod(g.rotation, 360) / 90; i++)
		{
			md = md.cloneVertices().rotateY90Ccw();
		}
		if (g.resizeX != 128 || g.resizeY != 128)
		{
			md = md.cloneVertices().scale(g.resizeX, g.resizeY, g.resizeX);
		}
		GraphicDef.tint(md, color);

		Model model = md.light(AMBIENT + g.ambient, CONTRAST + g.contrast, -30, -50, -30);
		if (model == null)
		{
			return;
		}

		RuneLiteObject obj = client.createRuneLiteObject();
		obj.setModel(model);
		obj.setLocation(at, plane);
		obj.setZ(Perspective.getTileHeight(client, at, plane) - height);
		obj.setOrientation(orientation);
		AnimationController once = new AnimationController(client, anim);
		once.setOnFinished(ac -> obj.setActive(false)); // one-shot: gone when the animation ends
		obj.setAnimationController(once);
		obj.setActive(true);
		active.add(obj);
	}

	/** Forgets finished effects. Call every client tick. */
	void tick()
	{
		for (Iterator<RuneLiteObject> it = active.iterator(); it.hasNext(); )
		{
			if (!it.next().isActive())
			{
				it.remove();
			}
		}
	}

	void clear()
	{
		for (RuneLiteObject o : active)
		{
			o.setActive(false);
		}
		active.clear();
	}

	GraphicDef graphic(int id)
	{
		GraphicDef cached = graphics.get(id);
		if (cached != null || broken.containsKey(id))
		{
			return cached;
		}
		IndexDataBase configs = client.getIndexConfig();
		byte[] data = configs == null ? null : configs.loadData(SPOTANIM_ARCHIVE, id);
		if (data == null)
		{
			return null; // cache not ready yet; try again later
		}
		try
		{
			GraphicDef g = GraphicDef.decode(data);
			if (g != null)
			{
				graphics.put(id, g);
			}
			else
			{
				broken.put(id, true);
			}
			return g;
		}
		catch (RuntimeException e)
		{
			log.debug("Couldn't read graphic {}", id, e);
			broken.put(id, true);
			return null;
		}
	}
}
