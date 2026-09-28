package com.tabletportals;

import java.awt.Color;
import net.runelite.api.JagexColor;
import net.runelite.api.ModelData;

/**
 * A graphic (spotanim) definition read from the game cache, so its model can be drawn as a
 * {@link net.runelite.api.RuneLiteObject} anywhere, and recoloured. The opcode layout follows the game's
 * cache format; the handling of opcode 10 (a flag with no data) is from Rapid Mounts by RapidUrsa
 * (BSD 2-Clause, see THIRD_PARTY_NOTICES.md).
 */
final class GraphicDef
{
	int model;
	int animation = -1;
	int resizeX = 128;
	int resizeY = 128;
	int rotation;
	int ambient;
	int contrast;
	short[] recolorFind;
	short[] recolorReplace;
	short[] retextureFind;
	short[] retextureReplace;

	static GraphicDef decode(byte[] data)
	{
		Reader in = new Reader(data);
		GraphicDef g = new GraphicDef();
		for (int op = in.u8(); op != 0; op = in.u8())
		{
			switch (op)
			{
				case 1:
					g.model = in.u16();
					break;
				case 2:
					g.animation = in.u16();
					break;
				case 3:
					g.model = in.i32();
					break;
				case 4:
					g.resizeX = in.u16();
					break;
				case 5:
					g.resizeY = in.u16();
					break;
				case 6:
					g.rotation = in.u16();
					break;
				case 7:
					g.ambient = in.u8();
					break;
				case 8:
					g.contrast = in.u8();
					break;
				case 9:
					in.skipString();
					break;
				case 10:
					break;
				case 40:
				{
					int n = in.u8();
					g.recolorFind = new short[n];
					g.recolorReplace = new short[n];
					for (int i = 0; i < n; i++)
					{
						g.recolorFind[i] = (short) in.u16();
						g.recolorReplace[i] = (short) in.u16();
					}
					break;
				}
				case 41:
				{
					int n = in.u8();
					g.retextureFind = new short[n];
					g.retextureReplace = new short[n];
					for (int i = 0; i < n; i++)
					{
						g.retextureFind[i] = (short) in.u16();
						g.retextureReplace[i] = (short) in.u16();
					}
					break;
				}
				case 42:
					in.u16(); // whole-model recolour; not needed here
					break;
				default:
					throw new IllegalStateException("unknown graphic opcode " + op);
			}
		}
		return g.model > 0 ? g : null;
	}

	/**
	 * Recolours every face to the given colour's hue and saturation, keeping each face's own lightness,
	 * so the model keeps its shading and just changes colour. Does nothing for a null colour.
	 */
	static void tint(ModelData md, Color color)
	{
		if (color == null)
		{
			return;
		}
		short[] colors = md.getFaceColors();
		if (colors == null)
		{
			return;
		}
		short target = JagexColor.rgbToHSL(color.getRGB(), 1.0);
		int hue = JagexColor.unpackHue(target);
		int sat = JagexColor.unpackSaturation(target);
		for (int i = 0; i < colors.length; i++)
		{
			colors[i] = tintColor(colors[i], hue, sat);
		}
	}

	static short tintColor(short hsl, int hue, int sat)
	{
		return JagexColor.packHSL(hue, sat, JagexColor.unpackLuminance(hsl));
	}

	private static final class Reader
	{
		private final byte[] data;
		private int pos;

		Reader(byte[] data)
		{
			this.data = data;
		}

		int u8()
		{
			return data[pos++] & 0xFF;
		}

		int u16()
		{
			return (u8() << 8) | u8();
		}

		int i32()
		{
			return (u16() << 16) | u16();
		}

		void skipString()
		{
			while (data[pos++] != 0)
			{
				// skip to the terminating zero
			}
		}
	}
}
