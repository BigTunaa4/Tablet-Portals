package com.tabletportals;

import java.awt.Color;

/** The colour of the portal's swirl. Classic keeps the game's own purple. */
public enum PortalColour
{
	CLASSIC("Classic purple", null),
	ARCANE("Arcane blue", new Color(70, 140, 255)),
	EMERALD("Emerald", new Color(40, 200, 110)),
	INFERNO("Inferno", new Color(255, 90, 30)),
	GOLDEN("Golden", new Color(255, 200, 50)),
	VOID("Void", new Color(60, 60, 70)),
	CUSTOM("Custom", null);

	private final String label;
	private final Color color;

	PortalColour(String label, Color color)
	{
		this.label = label;
		this.color = color;
	}

	/** The tint for this choice, or null to keep the original colours. */
	Color resolve(Color custom)
	{
		return this == CUSTOM ? custom : color;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
