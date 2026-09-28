package com.tabletportals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class GraphicDefTest
{
	private static byte[] hex(String s)
	{
		byte[] b = new byte[s.length() / 2];
		for (int i = 0; i < b.length; i++)
		{
			b[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
		}
		return b;
	}

	@Test
	public void readsTheTabletSparkleFromTheCache()
	{
		// Graphic 678 as stored in the game cache: model 12734, animation 4072.
		GraphicDef g = GraphicDef.decode(hex("03000031be020fe800"));
		assertEquals(12734, g.model);
		assertEquals(4072, g.animation);
		assertEquals(128, g.resizeX);
	}

	@Test
	public void readsResizesLightingAndWholeModelRecolour()
	{
		// Graphic 188 (large smoke puff) plus a lighting pair and an opcode-42 recolour.
		GraphicDef g = GraphicDef.decode(hex("01" + "0c04" + "02" + "028e" + "04" + "0100" + "07" + "32" + "08" + "32" + "2a" + "1234" + "00"));
		assertEquals(3076, g.model);
		assertEquals(654, g.animation);
		assertEquals(256, g.resizeX);
		assertEquals(50, g.ambient);
		assertEquals(50, g.contrast);
	}

	@Test
	public void noModelMeansNothingToDraw()
	{
		assertNull(GraphicDef.decode(hex("020fe800")));
	}
}
