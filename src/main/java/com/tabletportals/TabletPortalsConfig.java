package com.tabletportals;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(TabletPortalsConfig.GROUP)
public interface TabletPortalsConfig extends Config
{
	String GROUP = "tabletportals";

	@ConfigItem(
		keyName = "smashStyle",
		name = "Smash style",
		description = "How your character breaks the tablet.",
		position = 1
	)
	default SmashStyle smashStyle()
	{
		return SmashStyle.OVERHEAD_SLAM;
	}

	@ConfigItem(
		keyName = "shatter",
		name = "Tablet shatters",
		description = "Shards and dust burst from the tablet when it's smashed.",
		position = 2
	)
	default boolean shatter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "portalColour",
		name = "Portal colour",
		description = "The colour of the portal's swirl.",
		position = 3
	)
	default PortalColour portalColour()
	{
		return PortalColour.CLASSIC;
	}

	@ConfigItem(
		keyName = "customColour",
		name = "Custom colour",
		description = "Used when Portal colour is set to Custom.",
		position = 4
	)
	default Color customColour()
	{
		return new Color(200, 60, 220);
	}

	@Range(min = 60, max = 160)
	@ConfigItem(
		keyName = "portalSize",
		name = "Portal size (%)",
		description = "How big the portal is. 100% stands a little taller than your character.",
		position = 5
	)
	default int portalSize()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "smoke",
		name = "Smoke",
		description = "How much smoke billows around the portal.",
		position = 6
	)
	default SmokeAmount smoke()
	{
		return SmokeAmount.NORMAL;
	}

	@ConfigItem(
		keyName = "tintSmoke",
		name = "Colour the smoke",
		description = "Tint the smoke to match the portal.",
		position = 7
	)
	default boolean tintSmoke()
	{
		return false;
	}

	@ConfigItem(
		keyName = "exitPortal",
		name = "Step out at destination",
		description = "A portal opens where you arrive and you walk out of it.",
		position = 8
	)
	default boolean exitPortal()
	{
		return true;
	}

	@ConfigItem(
		keyName = "reflection",
		name = "Reflect the destination",
		description = "The portal shows the place you're going, like a reflection on choppy water. Each place is"
			+ " photographed the first time you arrive there, so it shows from your second trip on.",
		position = 9
	)
	default boolean reflection()
	{
		return true;
	}

	@Range(min = 10, max = 90)
	@ConfigItem(
		keyName = "reflectionStrength",
		name = "Reflection strength (%)",
		description = "How clearly the destination shows through the swirl.",
		position = 10
	)
	default int reflectionStrength()
	{
		return 45;
	}

	@ConfigItem(
		keyName = "ripple",
		name = "Ripple",
		description = "How choppy the reflection is.",
		position = 11
	)
	default Ripple ripple()
	{
		return Ripple.CHOPPY;
	}
}
