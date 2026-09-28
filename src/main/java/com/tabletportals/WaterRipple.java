package com.tabletportals;

import java.awt.image.BufferedImage;

/**
 * Turns a picture into a frame of a choppy water reflection: rows sway from side to side, the picture bobs up
 * and down a little, bands of light roll across it, and it fades out softly towards the edge of the portal.
 * Pure pixel maths, so it can be tested and previewed away from the game.
 */
final class WaterRipple
{
	private WaterRipple()
	{
	}

	/**
	 * Renders one frame.
	 *
	 * @param src      the picture, already scaled to {@code w} x {@code h} ({@link #scaled})
	 * @param time     seconds, for the motion
	 * @param strength how solid the reflection is at its middle, 0 to 1
	 */
	static BufferedImage frame(int[] src, int w, int h, float time, Ripple ripple, float strength, BufferedImage reuse)
	{
		BufferedImage out = reuse != null && reuse.getWidth() == w && reuse.getHeight() == h
			? reuse : new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		int[] row = new int[w];
		float amp = ripple.getAmplitude() * h;
		float s = time * ripple.getSpeed();
		float cx = (w - 1) / 2f;
		float cy = (h - 1) / 2f;

		for (int y = 0; y < h; y++)
		{
			// Sideways sway: a few waves of different sizes, so it looks choppy rather than a neat wiggle.
			float dx = amp * (float) (Math.sin(y * 0.11 + s * 3.1)
				+ 0.5 * Math.sin(y * 0.29 - s * 4.3)
				+ 0.25 * Math.sin(y * 0.63 + s * 7.7));
			// The surface bobbing up and down.
			int sy = clamp(Math.round(y + amp * 0.45f * (float) Math.sin(y * 0.07 + s * 2.3)), 0, h - 1);
			// Light rolling across the water.
			float wave = (float) Math.sin(y * 0.33 - s * 5.0 + Math.sin(y * 0.05 + s) * 2.0);
			float light = 1f + 0.10f * wave;
			float band = wave > 0.85f ? (wave - 0.85f) / 0.15f * 0.28f : 0f;

			float ny = (y - cy) / cy;
			for (int x = 0; x < w; x++)
			{
				int sx = clamp(Math.round(x + dx), 0, w - 1);
				int c = src[sy * w + sx];
				// Glints: short, broken highlights along the crest of a wave, not whole lines.
				float glint = band == 0f ? 0f : band * Math.max(0f, (float) Math.sin(x * 0.09 + y * 0.21 + s * 4.0));
				int r = shade((c >> 16) & 255, light, glint);
				int g = shade((c >> 8) & 255, light, glint);
				int b = shade(c & 255, light, glint);

				// Fade softly towards the rim of the portal.
				float nx = (x - cx) / cx;
				float d = (float) Math.sqrt(nx * nx + ny * ny);
				float a = strength * (1f - smoothstep(0.55f, 1f, d));
				row[x] = (clamp(Math.round(a * 255), 0, 255) << 24) | (r << 16) | (g << 8) | b;
			}
			out.setRGB(0, y, w, 1, row, 0, w);
		}
		return out;
	}

	/** The picture's pixels at a size, for {@link #frame}. */
	static int[] scaled(BufferedImage img, int w, int h)
	{
		BufferedImage s = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		java.awt.Graphics2D g = s.createGraphics();
		g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(img, 0, 0, w, h, null);
		g.dispose();
		return s.getRGB(0, 0, w, h, null, 0, w);
	}

	private static int shade(int v, float light, float glint)
	{
		float f = v * light + (255 - v) * glint;
		return clamp(Math.round(f), 0, 255);
	}

	static float smoothstep(float e0, float e1, float x)
	{
		float t = Math.max(0f, Math.min(1f, (x - e0) / (e1 - e0)));
		return t * t * (3 - 2 * t);
	}

	private static int clamp(int v, int lo, int hi)
	{
		return v < lo ? lo : (v > hi ? hi : v);
	}
}
