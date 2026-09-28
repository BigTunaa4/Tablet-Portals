package com.tabletportals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import org.junit.Test;

public class ReflectionTest
{
	@Test
	public void tabletNamesBecomeFileSafeKeys()
	{
		assertEquals("varrock-teleport", Snapshots.key("<col=ff9040>Varrock teleport</col>"));
		assertEquals("teleport-to-house", Snapshots.key("Teleport to house"));
		assertEquals("ape-atoll-teleport", Snapshots.key("<col=ff9040>Ape Atoll teleport </col>"));
		assertNull(Snapshots.key("<col=ff9040></col>"));
		assertNull(Snapshots.key(null));
	}

	@Test
	public void cutsTheCropOutOfAStretchedFrame()
	{
		// A 200x100 canvas stretched to 400x200; the right half is white.
		BufferedImage frame = new BufferedImage(400, 200, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < 200; y++)
		{
			for (int x = 200; x < 400; x++)
			{
				frame.setRGB(x, y, 0xFFFFFF);
			}
		}
		BufferedImage out = Snapshots.cut(frame, new Rectangle(100, 0, 100, 100), 200, 100);
		assertEquals(Snapshots.SIZE, out.getWidth());
		assertEquals(0xFFFFFF, out.getRGB(Snapshots.SIZE / 2, Snapshots.SIZE / 2) & 0xFFFFFF);
	}

	@Test
	public void rippleFadesAtTheRimAndMoves()
	{
		int w = 64;
		int h = 64;
		int[] src = new int[w * h];
		for (int i = 0; i < src.length; i++)
		{
			src[i] = ((i % w) * 4) << 16 | ((i / w) * 4) << 8; // a gradient, so movement shows
		}
		BufferedImage a = WaterRipple.frame(src, w, h, 0f, Ripple.CHOPPY, 0.5f, null);
		int middle = a.getRGB(w / 2, h / 2) >>> 24;
		int corner = a.getRGB(0, 0) >>> 24;
		assertTrue("solid-ish in the middle", middle > 100 && middle <= 128);
		assertEquals("gone at the rim", 0, corner);

		BufferedImage b = WaterRipple.frame(src, w, h, 0.5f, Ripple.CHOPPY, 0.5f, null);
		boolean moved = false;
		for (int y = 0; y < h && !moved; y++)
		{
			moved = a.getRGB(w / 2, y) != b.getRGB(w / 2, y);
		}
		assertTrue("ripples move over time", moved);
		assertNotEquals(Ripple.CALM.getAmplitude(), Ripple.ROUGH.getAmplitude(), 0f);
	}
}
