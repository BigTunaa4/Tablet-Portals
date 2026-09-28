package com.tabletportals;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class TabletBreakTest
{
	@Test
	public void teleportTabletsCount()
	{
		assertTrue(TabletPortalsPlugin.isTeleportTabletBreak("Break", "<col=ff9040>Varrock teleport</col>"));
		assertTrue(TabletPortalsPlugin.isTeleportTabletBreak("Break", "<col=ff9040>Teleport to house</col>"));
		assertTrue(TabletPortalsPlugin.isTeleportTabletBreak("break", "Annakarl teleport"));
		assertTrue(TabletPortalsPlugin.isTeleportTabletBreak("Break", "<col=ff9040>Rimmington teleport</col>"));
	}

	@Test
	public void otherTabletsAndOptionsDont()
	{
		assertFalse(TabletPortalsPlugin.isTeleportTabletBreak("Break", "<col=ff9040>Bones to bananas</col>"));
		assertFalse(TabletPortalsPlugin.isTeleportTabletBreak("Break", "<col=ff9040>Enchant sapphire or opal</col>"));
		assertFalse(TabletPortalsPlugin.isTeleportTabletBreak("Drop", "<col=ff9040>Varrock teleport</col>"));
		assertFalse(TabletPortalsPlugin.isTeleportTabletBreak(null, null));
	}
}
