package com.cavatapi.fishingminigame;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class FishingMiniGameTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(
                FishingMiniGamePlugin.class
        );

        RuneLite.main(args);
    }
}