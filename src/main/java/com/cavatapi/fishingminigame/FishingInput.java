package com.cavatapi.fishingminigame;

import net.runelite.client.input.MouseAdapter;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;

public class FishingInput extends MouseAdapter
{
    private FishingGame fishingGame;

    private Rectangle reelButtonBounds =
            new Rectangle();

    public void setFishingGame(
            FishingGame fishingGame)
    {
        this.fishingGame =
                fishingGame;
    }

    public void setReelButtonBounds(
            Rectangle bounds)
    {
        this.reelButtonBounds =
                bounds;
    }

    @Override
    public MouseEvent mousePressed(
            MouseEvent event)
    {
        if (fishingGame == null)
        {
            return event;
        }

        if (event.getButton() ==
                MouseEvent.BUTTON1 &&
                reelButtonBounds.contains(
                        event.getPoint()))
        {
            fishingGame.setReelHeld(true);

            event.consume();
        }

        return event;
    }

    @Override
    public MouseEvent mouseReleased(
            MouseEvent event)
    {
        if (fishingGame != null)
        {
            fishingGame.setReelHeld(false);
        }

        return event;
    }

    @Override
    public MouseEvent mouseExited(
            MouseEvent event)
    {
        if (fishingGame != null)
        {
            fishingGame.setReelHeld(false);
        }

        return event;
    }
}