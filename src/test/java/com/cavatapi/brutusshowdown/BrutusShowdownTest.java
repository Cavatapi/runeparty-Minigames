package com.cavatapi.brutusshowdown;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BrutusShowdownTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(
                BrutusShowdownPlugin.class
        );

        RuneLite.main(args);
    }
}