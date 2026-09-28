package com.tabletportals;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.RuneLite;
import net.runelite.client.ui.DrawManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pictures of where each tablet takes you, for the portal's reflection. The game only has the area around you
 * loaded, so a destination can't be drawn before you get there; instead a picture is taken of the view when
 * you arrive, and shown in the portal the next time you use that tablet. Pictures are small and kept in
 * {@code .runelite/tablet-portals}, one per tablet.
 */
@Singleton
class Snapshots
{
	private static final Logger log = LoggerFactory.getLogger(Snapshots.class);

	/** Size of a stored picture. The portal is roughly round, so they're square. */
	static final int SIZE = 192;
	private static final File DIR = new File(RuneLite.RUNELITE_DIR, "tablet-portals");

	private final Client client;
	private final DrawManager drawManager;
	private final ScheduledExecutorService executor;

	private final Map<String, BufferedImage> loaded = new HashMap<>();
	/** Tablets with no picture on disk, so we don't keep looking. */
	private final Set<String> missing = new HashSet<>();
	/** Destinations already re-photographed since logging in (one fresh picture per session is plenty). */
	private final Set<String> refreshed = new HashSet<>();

	@Inject
	Snapshots(Client client, DrawManager drawManager, ScheduledExecutorService executor)
	{
		this.client = client;
		this.drawManager = drawManager;
		this.executor = executor;
	}

	/** A file-safe key for a tablet, from its name as shown in the menu. */
	static String key(String tabletName)
	{
		if (tabletName == null)
		{
			return null;
		}
		String k = tabletName.replaceAll("<[^>]*>", "").trim().toLowerCase().replaceAll("[^a-z0-9]+", "-");
		k = k.replaceAll("^-+|-+$", "");
		return k.isEmpty() ? null : k;
	}

	/** The picture of a tablet's destination, or null if you haven't arrived there with the plugin yet. */
	BufferedImage get(String key)
	{
		if (key == null)
		{
			return null;
		}
		BufferedImage img = loaded.get(key);
		if (img != null || missing.contains(key))
		{
			return img;
		}
		File f = file(key);
		if (!f.isFile())
		{
			missing.add(key);
			return null;
		}
		try
		{
			img = ImageIO.read(f);
		}
		catch (IOException e)
		{
			log.debug("Couldn't read {}", f, e);
		}
		if (img == null)
		{
			missing.add(key);
			return null;
		}
		loaded.put(key, img);
		return img;
	}

	/** Whether a destination should be photographed on this arrival. */
	boolean wants(String key)
	{
		return key != null && (get(key) == null || !refreshed.contains(key));
	}

	/** Photographs the view around the player and keeps it as the picture for {@code key}. */
	void capture(String key)
	{
		if (key == null)
		{
			return;
		}
		refreshed.add(key);
		grab(img ->
		{
			loaded.put(key, img);
			missing.remove(key);
			executor.execute(() -> save(key, img));
		});
	}

	/** Photographs the view around the player, for this trip only (the exit portal shows where you left). */
	void grab(Consumer<BufferedImage> done)
	{
		Rectangle crop = cropAroundPlayer();
		if (crop == null)
		{
			return;
		}
		int canvasW = client.getCanvasWidth();
		int canvasH = client.getCanvasHeight();
		drawManager.requestNextFrameListener(frame ->
		{
			BufferedImage img = cut(frame, crop, canvasW, canvasH);
			if (img != null)
			{
				done.accept(img);
			}
		});
	}

	/** A square of the game view centred a little above the player, in canvas pixels. */
	private Rectangle cropAroundPlayer()
	{
		Player me = client.getLocalPlayer();
		LocalPoint lp = me == null ? null : me.getLocalLocation();
		if (lp == null)
		{
			return null;
		}
		Point p = Perspective.localToCanvas(client, lp, me.getWorldView().getPlane(), 60);
		if (p == null)
		{
			return null;
		}
		int vx = client.getViewportXOffset();
		int vy = client.getViewportYOffset();
		int vw = client.getViewportWidth();
		int vh = client.getViewportHeight();
		int size = Math.round(Math.min(vw, vh) * 0.75f);
		if (size < 32)
		{
			return null;
		}
		int x = p.getX() - size / 2;
		int y = p.getY() - size * 3 / 5; // more sky and scenery above you than ground below
		x = Math.max(vx, Math.min(x, vx + vw - size));
		y = Math.max(vy, Math.min(y, vy + vh - size));
		return new Rectangle(x, y, size, size);
	}

	/** Cuts the square out of a frame (which may be stretched) and shrinks it to {@link #SIZE}. */
	static BufferedImage cut(Image frame, Rectangle crop, int canvasW, int canvasH)
	{
		int fw = frame.getWidth(null);
		int fh = frame.getHeight(null);
		if (fw <= 0 || fh <= 0 || canvasW <= 0 || canvasH <= 0)
		{
			return null;
		}
		double sx = fw / (double) canvasW;
		double sy = fh / (double) canvasH;
		int x0 = (int) Math.round(crop.x * sx);
		int y0 = (int) Math.round(crop.y * sy);
		int x1 = (int) Math.round((crop.x + crop.width) * sx);
		int y1 = (int) Math.round((crop.y + crop.height) * sy);

		BufferedImage out = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = out.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.drawImage(frame, 0, 0, SIZE, SIZE, x0, y0, x1, y1, null);
		g.dispose();
		return out;
	}

	private void save(String key, BufferedImage img)
	{
		try
		{
			if (!DIR.isDirectory() && !DIR.mkdirs())
			{
				return;
			}
			ImageIO.write(img, "png", file(key));
		}
		catch (IOException e)
		{
			log.debug("Couldn't save the picture for {}", key, e);
		}
	}

	private static File file(String key)
	{
		return new File(DIR, key + ".png");
	}

	/** Forgets which destinations were refreshed (on logout), keeping the pictures. */
	void newSession()
	{
		refreshed.clear();
	}
}
