package com.cavatapi.brutusshowdown;

import com.google.inject.Provides;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.inject.Inject;
import javax.swing.SwingUtilities;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Model;
import net.runelite.api.Player;
import net.runelite.api.Renderable;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;

import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.Hooks;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
        name = "Brutus Showdown",
        description = "A RuneLite minigame inspired by Sundown Showdown",
        tags = {"minigame", "brutus", "showdown"}
)
public class BrutusShowdownPlugin extends Plugin
{
    // ---------------------------------------------------------
    // MODELS
    // ---------------------------------------------------------

    // Brutus
    private static final int BRUTUS_MODEL_ID = 60113;

    // Vanescula Drakan
    private static final int TEST_DODGER_MODEL_ID = 39681;

    // Wise Old Man - possible future use
    // Raw model ID: 187

    // ---------------------------------------------------------
    // ROUND SETTINGS
    // ---------------------------------------------------------

    public static final int ROUND_TIME_SECONDS = 8;
    public static final int COUNTDOWN_SECONDS = 3;

    // ---------------------------------------------------------
    // ARENA
    // ---------------------------------------------------------

    public static final int ARENA_MIN_X = 2991;
    public static final int ARENA_MAX_X = 3012;

    public static final int ARENA_MIN_Y = 3376;
    public static final int ARENA_MAX_Y = 3380;

    public static final int ARENA_PLANE = 0;

    // ---------------------------------------------------------
    // BRUTUS START
    // ---------------------------------------------------------

    public static final int BRUTUS_START_MIN_X = 2992;
    public static final int BRUTUS_START_MAX_X = 2994;

    public static final int BRUTUS_START_MIN_Y = 3377;
    public static final int BRUTUS_START_MAX_Y = 3379;

    // ---------------------------------------------------------
    // ACTIVE ARENA
    // ---------------------------------------------------------

    public static final int ACTIVE_ARENA_MIN_X = 2995;
    public static final int ACTIVE_ARENA_MAX_X = 3009;

    public static final int ACTIVE_ARENA_MIN_Y = 3377;
    public static final int ACTIVE_ARENA_MAX_Y = 3379;

    // ---------------------------------------------------------
    // DODGER START
    // ---------------------------------------------------------

    public static final int DODGER_START_MIN_X = 3010;
    public static final int DODGER_START_MAX_X = 3011;

    public static final int DODGER_START_MIN_Y = 3377;
    public static final int DODGER_START_MAX_Y = 3379;

    // ---------------------------------------------------------
    // TEST DODGER POSITION
    // ---------------------------------------------------------

    public static final int TEST_DODGER_X = 3010;
    public static final int TEST_DODGER_Y = 3378;

    // ---------------------------------------------------------
    // FINISH
    // ---------------------------------------------------------

    public static final int FINISH_X = 3012;

    public static final int FINISH_MIN_Y = 3377;
    public static final int FINISH_MAX_Y = 3379;

    // ---------------------------------------------------------
    // ROUND STATE
    // ---------------------------------------------------------

    public enum RoundState
    {
        WAITING,
        COUNTDOWN,
        PLAYING,
        SUCCESS,
        TIME_UP
    }

    private RoundState roundState = RoundState.WAITING;

    private long countdownStartTime;
    private long roundStartTime;

    // ---------------------------------------------------------
    // SCORE
    // ---------------------------------------------------------

    private int brutusScore = 0;
    private int dodgerScore = 0;

    private boolean roundScored = false;

    // ---------------------------------------------------------
    // COLLISION
    // ---------------------------------------------------------

    private boolean dodgerHit = false;

    private long dodgerHitTime = 0;

    public static final long HIT_MESSAGE_DURATION_MS = 1500;

    // ---------------------------------------------------------
    // BUTTON BOUNDS
    // ---------------------------------------------------------

    private final Rectangle replayButtonBounds =
            new Rectangle();

    private final Rectangle resetScoreButtonBounds =
            new Rectangle();

    // ---------------------------------------------------------
    // INJECTIONS
    // ---------------------------------------------------------

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private Hooks hooks;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private MouseManager mouseManager;

    @Inject
    private BrutusShowdownOverlay overlay;

    @Inject
    private BrutusShowdownConfig config;

    // ---------------------------------------------------------
    // OBJECTS
    // ---------------------------------------------------------

    private RuneLiteObject brutusDisguise;
    private RuneLiteObject testDodgerObject;

    private final Hooks.RenderableDrawListener drawListener =
            this::shouldDraw;

    // ---------------------------------------------------------
    // MOUSE LISTENER
    // ---------------------------------------------------------

    private final MouseAdapter mouseListener =
            new MouseAdapter()
            {
                @Override
                public MouseEvent mouseClicked(
                        MouseEvent event
                )
                {
                    if (!SwingUtilities.isLeftMouseButton(event))
                    {
                        return event;
                    }

                    if (!isRoundOver())
                    {
                        return event;
                    }

                    if (replayButtonBounds.contains(
                            event.getPoint()
                    ))
                    {
                        replayRound();

                        event.consume();

                        return event;
                    }

                    if (resetScoreButtonBounds.contains(
                            event.getPoint()
                    ))
                    {
                        resetScores();

                        event.consume();

                        return event;
                    }

                    return event;
                }
            };

    // ---------------------------------------------------------
    // START
    // ---------------------------------------------------------

    @Override
    protected void startUp()
    {
        System.out.println(
                "Brutus Showdown started!"
        );

        resetGameState();

        hooks.registerRenderableDrawListener(
                drawListener
        );

        mouseManager.registerMouseListener(
                mouseListener
        );

        overlayManager.add(overlay);

        clientThread.invokeLater(() ->
        {
            createBrutusDisguise();
            createTestDodger();
        });
    }

    // ---------------------------------------------------------
    // STOP
    // ---------------------------------------------------------

    @Override
    protected void shutDown()
    {
        hooks.unregisterRenderableDrawListener(
                drawListener
        );

        mouseManager.unregisterMouseListener(
                mouseListener
        );

        overlayManager.remove(overlay);

        clientThread.invokeLater(() ->
        {
            removeBrutusDisguise();
            removeTestDodger();
        });

        roundState = RoundState.WAITING;

        System.out.println(
                "Brutus Showdown stopped!"
        );
    }

    // ---------------------------------------------------------
    // RESET GAME STATE
    // ---------------------------------------------------------

    private void resetGameState()
    {
        roundState = RoundState.WAITING;

        dodgerHit = false;
        dodgerHitTime = 0;

        roundScored = false;
    }

    // ---------------------------------------------------------
    // CREATE BRUTUS
    // ---------------------------------------------------------

    private void createBrutusDisguise()
    {
        if (client.getGameState()
                != GameState.LOGGED_IN)
        {
            return;
        }

        Player player =
                client.getLocalPlayer();

        if (player == null)
        {
            return;
        }

        removeBrutusDisguise();

        Model brutusModel =
                client.loadModel(
                        BRUTUS_MODEL_ID
                );

        if (brutusModel == null)
        {
            System.out.println(
                    "Failed to load Brutus model."
            );

            return;
        }

        brutusDisguise =
                client.createRuneLiteObject();

        brutusDisguise.setModel(
                brutusModel
        );

        brutusDisguise.setLocation(
                player.getLocalLocation(),
                player.getWorldLocation()
                        .getPlane()
        );

        brutusDisguise.setOrientation(
                player.getCurrentOrientation()
        );

        brutusDisguise.setActive(true);
    }

    // ---------------------------------------------------------
    // REMOVE BRUTUS
    // ---------------------------------------------------------

    private void removeBrutusDisguise()
    {
        if (brutusDisguise != null)
        {
            brutusDisguise.setActive(false);

            brutusDisguise = null;
        }
    }

    // ---------------------------------------------------------
    // CREATE VANESCULA
    // ---------------------------------------------------------

    private void createTestDodger()
    {
        if (client.getGameState()
                != GameState.LOGGED_IN)
        {
            return;
        }

        removeTestDodger();

        Model dodgerModel =
                client.loadModel(
                        TEST_DODGER_MODEL_ID
                );

        if (dodgerModel == null)
        {
            System.out.println(
                    "Failed to load Vanescula model."
            );

            return;
        }

        WorldPoint worldPoint =
                new WorldPoint(
                        TEST_DODGER_X,
                        TEST_DODGER_Y,
                        ARENA_PLANE
                );

        LocalPoint localPoint =
                LocalPoint.fromWorld(
                        client,
                        worldPoint
                );

        if (localPoint == null)
        {
            return;
        }

        testDodgerObject =
                client.createRuneLiteObject();

        testDodgerObject.setModel(
                dodgerModel
        );

        testDodgerObject.setLocation(
                localPoint,
                ARENA_PLANE
        );

        testDodgerObject.setOrientation(
                1536
        );

        testDodgerObject.setActive(true);
    }

    // ---------------------------------------------------------
    // REMOVE VANESCULA
    // ---------------------------------------------------------

    private void removeTestDodger()
    {
        if (testDodgerObject != null)
        {
            testDodgerObject.setActive(false);

            testDodgerObject = null;
        }
    }

    // ---------------------------------------------------------
    // CLIENT TICK
    // ---------------------------------------------------------

    @Subscribe
    public void onClientTick(
            ClientTick event
    )
    {
        Player player =
                client.getLocalPlayer();

        if (player == null)
        {
            return;
        }

        if (brutusDisguise != null)
        {
            brutusDisguise.setLocation(
                    player.getLocalLocation(),
                    player.getWorldLocation()
                            .getPlane()
            );

            brutusDisguise.setOrientation(
                    player.getCurrentOrientation()
            );
        }

        updateRound(player);
    }

    // ---------------------------------------------------------
    // HIDE REAL PLAYER
    // ---------------------------------------------------------

    private boolean shouldDraw(
            Renderable renderable,
            boolean drawingUI
    )
    {
        if (renderable instanceof Player)
        {
            Player player =
                    (Player) renderable;

            if (player
                    == client.getLocalPlayer())
            {
                return brutusDisguise == null;
            }
        }

        return true;
    }

    // ---------------------------------------------------------
    // ROUND UPDATE
    // ---------------------------------------------------------

    private void updateRound(
            Player player
    )
    {
        switch (roundState)
        {
            case WAITING:
                updateWaiting(player);
                break;

            case COUNTDOWN:
                updateCountdown();
                break;

            case PLAYING:
                updatePlaying(player);
                break;

            case SUCCESS:
            case TIME_UP:
                scoreRound();
                break;
        }
    }

    // ---------------------------------------------------------
    // WAITING
    // ---------------------------------------------------------

    private void updateWaiting(
            Player player
    )
    {
        if (isInBrutusStart(player))
        {
            startCountdown();
        }
    }

    // ---------------------------------------------------------
    // COUNTDOWN
    // ---------------------------------------------------------

    private void startCountdown()
    {
        roundState =
                RoundState.COUNTDOWN;

        countdownStartTime =
                System.currentTimeMillis();

        dodgerHit = false;
        dodgerHitTime = 0;
        roundScored = false;

        createTestDodger();

        System.out.println(
                "Countdown started."
        );
    }

    private void updateCountdown()
    {
        long elapsed =
                System.currentTimeMillis()
                        - countdownStartTime;

        if (elapsed
                >= COUNTDOWN_SECONDS
                * 1000L)
        {
            startRound();
        }
    }

    // ---------------------------------------------------------
    // START ROUND
    // ---------------------------------------------------------

    private void startRound()
    {
        roundState =
                RoundState.PLAYING;

        roundStartTime =
                System.currentTimeMillis();

        System.out.println("GO!");
    }

    // ---------------------------------------------------------
    // PLAYING
    // ---------------------------------------------------------

    private void updatePlaying(
            Player player
    )
    {
        if (!dodgerHit
                && isCollidingWithTestDodger(
                player))
        {
            hitTestDodger();
        }

        if (hasBrutusCrossedFinish(
                player))
        {
            roundState =
                    RoundState.SUCCESS;

            scoreRound();

            return;
        }

        if (getRemainingTimeMillis() <= 0)
        {
            roundState =
                    RoundState.TIME_UP;

            scoreRound();
        }
    }

    // ---------------------------------------------------------
    // COLLISION
    // ---------------------------------------------------------

    public boolean isCollidingWithTestDodger(
            Player player
    )
    {
        if (player == null
                || dodgerHit)
        {
            return false;
        }

        WorldPoint point =
                player.getWorldLocation();

        return point.getPlane()
                == ARENA_PLANE
                && point.getX()
                == TEST_DODGER_X
                && point.getY()
                == TEST_DODGER_Y;
    }

    private void hitTestDodger()
    {
        dodgerHit = true;

        dodgerHitTime =
                System.currentTimeMillis();

        removeTestDodger();

        System.out.println(
                "HIT! Vanescula eliminated."
        );
    }

    // ---------------------------------------------------------
    // SCORE ROUND
    // ---------------------------------------------------------

    private void scoreRound()
    {
        if (roundScored)
        {
            return;
        }

        roundScored = true;

        if (dodgerHit)
        {
            brutusScore++;

            System.out.println(
                    "Point to Brutus."
            );
        }
        else
        {
            dodgerScore++;

            System.out.println(
                    "Point to Dodger."
            );
        }

        System.out.println(
                "Score: Brutus "
                        + brutusScore
                        + " - Dodger "
                        + dodgerScore
        );
    }

    // ---------------------------------------------------------
    // REPLAY
    // ---------------------------------------------------------

    public void replayRound()
    {
        if (!isRoundOver())
        {
            return;
        }

        roundState =
                RoundState.WAITING;

        dodgerHit = false;
        dodgerHitTime = 0;
        roundScored = false;

        createTestDodger();

        System.out.println(
                "Round ready for replay."
        );
    }

    // ---------------------------------------------------------
    // RESET SCORE
    // ---------------------------------------------------------

    public void resetScores()
    {
        brutusScore = 0;
        dodgerScore = 0;

        System.out.println(
                "Score reset."
        );
    }

    // ---------------------------------------------------------
    // BUTTON BOUNDS
    // ---------------------------------------------------------

    public void setReplayButtonBounds(
            Rectangle bounds
    )
    {
        replayButtonBounds.setBounds(
                bounds
        );
    }

    public void setResetScoreButtonBounds(
            Rectangle bounds
    )
    {
        resetScoreButtonBounds.setBounds(
                bounds
        );
    }

    // ---------------------------------------------------------
    // GETTERS
    // ---------------------------------------------------------

    public RoundState getRoundState()
    {
        return roundState;
    }

    public boolean isRoundOver()
    {
        return roundState
                == RoundState.SUCCESS
                || roundState
                == RoundState.TIME_UP;
    }

    public int getBrutusScore()
    {
        return brutusScore;
    }

    public int getDodgerScore()
    {
        return dodgerScore;
    }

    public boolean isDodgerHit()
    {
        return dodgerHit;
    }

    public boolean shouldShowHitMessage()
    {
        return dodgerHit
                && System.currentTimeMillis()
                - dodgerHitTime
                <= HIT_MESSAGE_DURATION_MS;
    }

    // ---------------------------------------------------------
    // COUNTDOWN VALUE
    // ---------------------------------------------------------

    public int getCountdownNumber()
    {
        if (roundState
                != RoundState.COUNTDOWN)
        {
            return 0;
        }

        long elapsed =
                System.currentTimeMillis()
                        - countdownStartTime;

        long remaining =
                COUNTDOWN_SECONDS
                        * 1000L
                        - elapsed;

        return Math.max(
                (int) Math.ceil(
                        remaining / 1000.0
                ),
                0
        );
    }

    // ---------------------------------------------------------
    // TIMER
    // ---------------------------------------------------------

    public long getRemainingTimeMillis()
    {
        if (roundState
                != RoundState.PLAYING)
        {
            return ROUND_TIME_SECONDS
                    * 1000L;
        }

        long elapsed =
                System.currentTimeMillis()
                        - roundStartTime;

        long remaining =
                ROUND_TIME_SECONDS
                        * 1000L
                        - elapsed;

        return Math.max(
                remaining,
                0
        );
    }

    public double getRemainingTimeSeconds()
    {
        return getRemainingTimeMillis()
                / 1000.0;
    }

    // ---------------------------------------------------------
    // ARENA CHECKS
    // ---------------------------------------------------------

    public boolean isInBrutusStart(
            Player player
    )
    {
        if (player == null)
        {
            return false;
        }

        WorldPoint point =
                player.getWorldLocation();

        return point.getPlane()
                == ARENA_PLANE
                && point.getX()
                >= BRUTUS_START_MIN_X
                && point.getX()
                <= BRUTUS_START_MAX_X
                && point.getY()
                >= BRUTUS_START_MIN_Y
                && point.getY()
                <= BRUTUS_START_MAX_Y;
    }

    public boolean hasBrutusCrossedFinish(
            Player player
    )
    {
        if (player == null)
        {
            return false;
        }

        WorldPoint point =
                player.getWorldLocation();

        return point.getPlane()
                == ARENA_PLANE
                && point.getX()
                >= FINISH_X
                && point.getY()
                >= FINISH_MIN_Y
                && point.getY()
                <= FINISH_MAX_Y;
    }

    // ---------------------------------------------------------
    // CONFIG
    // ---------------------------------------------------------

    @Provides
    BrutusShowdownConfig provideConfig(
            ConfigManager configManager
    )
    {
        return configManager.getConfig(
                BrutusShowdownConfig.class
        );
    }
}