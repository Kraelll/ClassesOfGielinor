package com.ClassesOfGielinor;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CoGPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ClassesOfGielinorPlugin.class);
		RuneLite.main(args);
	}
}