package com.cavatapi.fishingminigame;

import java.util.Random;

public class FishingGame
{
    public static final int BAR_HEIGHT = 350;

    public static final int PLAYER_HEIGHT = 75;

    private static final double GRAVITY = 0.35;
    private static final double REEL_FORCE = 0.65;

    private static final double MAX_FALL_SPEED = 6.0;
    private static final double MAX_RISE_SPEED = -5.0;

    private static final double HARD_LANDING_SPEED = 4.5;
    private static final double HARD_BOUNCE_SPEED = -5.0;

    private static final double CATCH_GAIN = 0.6;
    private static final double CATCH_LOSS = 0.25;

    private static final double MAX_ESCAPE_TIME = 5.0;

    private static final double UPDATE_SECONDS = 0.020;

    private double playerY = 200;
    private double playerVelocity = 0;

    private boolean reelHeld = false;
    private boolean hasBounced = false;

    private Fish fish;

    private boolean fishInsidePlayer = false;

    private double catchProgress = 0;

    private double escapeTimeRemaining =
            MAX_ESCAPE_TIME;

    private double totalGameTime = 0;

    private boolean fishCaught = false;
    private boolean fishGotAway = false;

    public FishingGame()
    {
        reset();
    }

    public void reset()
    {
        playerY = 200;
        playerVelocity = 0;

        reelHeld = false;
        hasBounced = false;

        fishInsidePlayer = false;

        catchProgress = 0;

        escapeTimeRemaining =
                MAX_ESCAPE_TIME;

        totalGameTime = 0;

        fishCaught = false;
        fishGotAway = false;

        createNewFish();
    }

    private void createNewFish()
    {
        long roundSeed =
                new Random().nextLong();

        fish = new Fish(
                50,
                0,
                BAR_HEIGHT - 36,
                60,
                36,
                FishBehavior.randomBehavior(
                        roundSeed
                )
        );
    }

    public void update()
    {
        if (fishCaught || fishGotAway)
        {
            return;
        }

        totalGameTime +=
                UPDATE_SECONDS;

        updatePlayer();

        fish.update();

        checkFishOverlap();

        updateCatchProgress();

        updateEscapeTimer();
    }

    private void updatePlayer()
    {
        playerVelocity +=
                GRAVITY;

        if (reelHeld)
        {
            playerVelocity -=
                    REEL_FORCE;
        }

        if (playerVelocity >
                MAX_FALL_SPEED)
        {
            playerVelocity =
                    MAX_FALL_SPEED;
        }

        if (playerVelocity <
                MAX_RISE_SPEED)
        {
            playerVelocity =
                    MAX_RISE_SPEED;
        }

        playerY +=
                playerVelocity;

        if (playerY < 0)
        {
            playerY = 0;

            if (playerVelocity < 0)
            {
                playerVelocity = 0;
            }
        }

        int bottomLimit =
                BAR_HEIGHT -
                        PLAYER_HEIGHT;

        if (playerY > bottomLimit)
        {
            playerY =
                    bottomLimit;

            if (playerVelocity >=
                    HARD_LANDING_SPEED &&
                    !hasBounced)
            {
                playerVelocity =
                        HARD_BOUNCE_SPEED;

                hasBounced = true;
            }
            else
            {
                playerVelocity = 0;
            }
        }

        if (reelHeld &&
                playerY <
                        bottomLimit - 10)
        {
            hasBounced = false;
        }
    }

    private void checkFishOverlap()
    {
        double playerTop =
                playerY;

        double playerBottom =
                playerY +
                        PLAYER_HEIGHT;

        double fishTop =
                fish.getY();

        double fishBottom =
                fish.getY() +
                        fish.getHeight();

        /*
         * Entire fish must be inside
         * the green rectangle.
         */
        fishInsidePlayer =
                fishTop >= playerTop &&
                        fishBottom <=
                                playerBottom;
    }

    private void updateCatchProgress()
    {
        if (fishInsidePlayer)
        {
            catchProgress +=
                    CATCH_GAIN;
        }
        else
        {
            catchProgress -=
                    CATCH_LOSS;
        }

        if (catchProgress < 0)
        {
            catchProgress = 0;
        }

        if (catchProgress >= 100)
        {
            catchProgress = 100;

            fishCaught = true;

            reelHeld = false;

            System.out.println(
                    "Fish caught in " +
                            String.format(
                                    "%.2f",
                                    totalGameTime
                            ) +
                            " seconds."
            );
        }
    }

    private void updateEscapeTimer()
    {
        if (!fishInsidePlayer)
        {
            escapeTimeRemaining -=
                    UPDATE_SECONDS;
        }

        if (escapeTimeRemaining <= 0)
        {
            escapeTimeRemaining = 0;

            fishGotAway = true;

            reelHeld = false;

            System.out.println(
                    "Fish escaped after " +
                            String.format(
                                    "%.2f",
                                    totalGameTime
                            ) +
                            " seconds."
            );
        }
    }

    public void setReelHeld(
            boolean reelHeld)
    {
        this.reelHeld =
                reelHeld;
    }

    public double getPlayerY()
    {
        return playerY;
    }

    public Fish getFish()
    {
        return fish;
    }

    public double getCatchProgress()
    {
        return catchProgress;
    }

    public double getEscapePercent()
    {
        return escapeTimeRemaining /
                MAX_ESCAPE_TIME;
    }

    public double getTotalGameTime()
    {
        return totalGameTime;
    }

    public boolean isFishCaught()
    {
        return fishCaught;
    }

    public boolean isFishGotAway()
    {
        return fishGotAway;
    }

    public boolean isFishInsidePlayer()
    {
        return fishInsidePlayer;
    }
}