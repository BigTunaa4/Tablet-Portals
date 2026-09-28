package com.tabletportals;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Draws a picture of the other end in the portal's swirl, like a reflection on choppy water. The picture is
 * laid over the swirl's outline on screen, so it follows the portal as it opens, closes and turns with the
 * camera. Your character walking in front of the portal is kept in front of the reflection.
 */
class ReflectionOverlay extends Overlay
{
	/** Points around the rim of the swirl used to find its outline on screen. */
	private static final int RIM_POINTS = 32;
	/** The picture fills a little less than the swirl, so the swirl's edge still shows around it. */
	private static final float INSET = 0.86f;
	/** The biggest the reflection is drawn, in pixels; bigger is scaled up (keeps it cheap when zoomed in). */
	private static final int MAX_DRAW = 256;

	private final Client client;
	private final TabletPortalsConfig config;
	private final long startNanos = System.nanoTime();

	private Portal portal;
	private LocalPoint at;
	private int plane;
	private int orientation;
	private BufferedImage picture;
	private Walker walker;

	/** The picture scaled to the last drawn size, and a reusable frame buffer. */
	private int[] scaled;
	private int scaledW;
	private int scaledH;
	private BufferedImage frame;

	@Inject
	ReflectionOverlay(Client client, TabletPortalsConfig config)
	{
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(PRIORITY_LOW);
	}

	/** Shows {@code picture} in {@code portal}; {@code walker} (may be null) is kept in front of it. */
	void show(Portal portal, LocalPoint at, int plane, int orientation, BufferedImage picture, Walker walker)
	{
		this.portal = portal;
		this.at = at;
		this.plane = plane;
		this.orientation = orientation;
		this.walker = walker;
		if (this.picture != picture)
		{
			this.picture = picture;
			scaled = null;
		}
	}

	void hide()
	{
		portal = null;
		picture = null;
		walker = null;
		scaled = null;
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		Portal p = portal;
		if (p == null || picture == null || at == null || !config.reflection())
		{
			return null;
		}
		float radius = p.radius() * INSET;
		if (radius < 8f)
		{
			return null;
		}

		Polygon rim = rim(p, radius);
		if (rim == null)
		{
			return null;
		}
		Rectangle box = rim.getBounds();
		if (box.width < 6 || box.height < 6)
		{
			return null;
		}

		Area clip = new Area(rim);
		Shape inFront = walkerInFront();
		if (inFront != null)
		{
			clip.subtract(new Area(inFront));
		}

		// Render the ripple at up to MAX_DRAW pixels and let Java scale it the rest of the way.
		float shrink = Math.min(1f, MAX_DRAW / (float) Math.max(box.width, box.height));
		int w = Math.max(8, Math.round(box.width * shrink));
		int h = Math.max(8, Math.round(box.height * shrink));
		if (scaled == null || scaledW != w || scaledH != h)
		{
			scaled = WaterRipple.scaled(picture, w, h);
			scaledW = w;
			scaledH = h;
		}
		float seconds = (System.nanoTime() - startNanos) / 1e9f;
		float strength = config.reflectionStrength() / 100f * Math.min(1f, p.getSize());
		frame = WaterRipple.frame(scaled, w, h, seconds, config.ripple(), strength, frame);

		Shape oldClip = g.getClip();
		Composite oldComposite = g.getComposite();
		Object oldInterp = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
		g.clip(clip);
		g.setComposite(AlphaComposite.SrcOver);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(frame, box.x, box.y, box.width, box.height, null);
		g.setClip(oldClip);
		g.setComposite(oldComposite);
		if (oldInterp != null)
		{
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterp);
		}
		return null;
	}

	/** The swirl's outline on screen: a circle in the portal's plane, projected. */
	private Polygon rim(Portal p, float radius)
	{
		int centre = p.centreHeight();
		Polygon poly = new Polygon();
		for (int i = 0; i < RIM_POINTS; i++)
		{
			double a = i * Math.PI * 2 / RIM_POINTS;
			int side = (int) Math.round(Math.cos(a) * radius);
			int up = (int) Math.round(Math.sin(a) * radius);
			int[] d = Facing.right(orientation, side);
			LocalPoint lp = new LocalPoint(at.getX() + d[0], at.getY() + d[1], at.getWorldView());
			Point s = Perspective.localToCanvas(client, lp, plane, centre + up);
			if (s == null)
			{
				return null;
			}
			poly.addPoint(s.getX(), s.getY());
		}
		return poly;
	}

	/** Your character's outline on screen, if they're between the camera and the portal. */
	private Shape walkerInFront()
	{
		Walker wk = walker;
		if (wk == null || !wk.isShown())
		{
			return null;
		}
		LocalPoint w = wk.getLocation();
		if (w == null)
		{
			return null;
		}
		long camX = client.getCameraX();
		long camY = client.getCameraY();
		long toWalker = sq(w.getX() - camX) + sq(w.getY() - camY);
		long toPortal = sq(at.getX() - camX) + sq(at.getY() - camY);
		if (toWalker >= toPortal)
		{
			return null; // behind the portal: the reflection covers them, which is right
		}

		// A rough body outline facing the camera, a bit generous so no reflection spills over the character.
		double vx = w.getX() - camX;
		double vy = w.getY() - camY;
		double len = Math.max(1, Math.sqrt(vx * vx + vy * vy));
		double px = -vy / len;
		double py = vx / len;
		int[][] body = {{-50, 0}, {50, 0}, {60, 110}, {45, 225}, {-45, 225}, {-60, 110}};
		Polygon poly = new Polygon();
		for (int[] b : body)
		{
			LocalPoint lp = new LocalPoint((int) Math.round(w.getX() + px * b[0]),
				(int) Math.round(w.getY() + py * b[0]), w.getWorldView());
			Point s = Perspective.localToCanvas(client, lp, plane, b[1]);
			if (s == null)
			{
				return null;
			}
			poly.addPoint(s.getX(), s.getY());
		}
		return poly;
	}

	private static long sq(long v)
	{
		return v * v;
	}
}
