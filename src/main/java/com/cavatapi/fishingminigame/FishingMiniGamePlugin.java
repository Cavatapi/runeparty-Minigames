package com.cavatapi.fishingminigame;

import javax.inject.Inject;

import net.runelite.api.events.ClientTick;

import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
		name = "FishingMiniGame"
)
public class FishingMiniGamePlugin extends Plugin
{
	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private FishingOverlay fishingOverlay;

	private FishingGame fishingGame;

	private FishingInput fishingInput;

	@Override
	protected void startUp()
	{
		fishingGame =
				new FishingGame();

		fishingInput =
				new FishingInput();

		fishingInput.setFishingGame(
				fishingGame
		);

		fishingOverlay.setFishingGame(
				fishingGame
		);

		fishingOverlay.setFishingInput(
				fishingInput
		);

		overlayManager.add(
				fishingOverlay
		);

		mouseManager.registerMouseListener(
				fishingInput
		);
	}

	@Override
	protected void shutDown()
	{
		mouseManager.unregisterMouseListener(
				fishingInput
		);

		overlayManager.remove(
				fishingOverlay
		);

		fishingInput = null;
		fishingGame = null;
	}

	@Subscribe
	public void onClientTick(
			ClientTick clientTick)
	{
		if (fishingGame != null)
		{
			fishingGame.update();
		}
	}
}