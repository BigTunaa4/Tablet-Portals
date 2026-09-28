package com.tabletportals;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class TabletPortalsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(TabletPortalsPlugin.class);
		RuneLite.main(args);
	}
}
