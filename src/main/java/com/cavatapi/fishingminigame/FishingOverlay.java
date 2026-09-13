package com.cavatapi.fishingminigame;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.imageio.ImageIO;
import javax.inject.Inject;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.io.IOException;

public class FishingOverlay extends Overlay
{
    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 350;

    private static final int PLAYER_WIDTH = 80;
    private static final int PLAYER_HEIGHT = 75;

    private static final int FISH_WIDTH = 60;
    private static final int FISH_HEIGHT = 36;

    private Image fishImage;

    private FishingGame fishingGame;
    private FishingInput fishingInput;

    @Inject
    public FishingOverlay()
    {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);

        loadFishImage();
    }

    public void setFishingGame(
            FishingGame fishingGame)
    {
        this.fishingGame =
                fishingGame;
    }

    public void setFishingInput(
            FishingInput fishingInput)
    {
        this.fishingInput =
                fishingInput;
    }

    private void loadFishImage()
    {
        try
        {
            fishImage = ImageIO.read(
                    getClass().getResource(
                            "/fish/tuna.png"
                    )
            );
        }
        catch (IOException | IllegalArgumentException e)
        {
            System.out.println(
                    "Could not load tuna image."
            );

            fishImage = null;
        }
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (fishingGame == null)
        {
            return null;
        }

        int screenWidth =
                graphics.getClipBounds().width;

        int screenHeight =
                graphics.getClipBounds().height;

        int barX =
                (screenWidth / 2) -
                        (BAR_WIDTH / 2);

        int barY =
                (screenHeight / 2) -
                        (BAR_HEIGHT / 2);

        drawFishingBar(
                graphics,
                barX,
                barY
        );

        drawPlayer(
                graphics,
                barX,
                barY +
                        (int) fishingGame.getPlayerY()
        );

        drawFish(
                graphics,
                barX,
                barY +
                        (int) fishingGame
                                .getFish()
                                .getY()
        );

        drawCatchMeter(
                graphics,
                barX + 150,
                barY,
                fishingGame
                        .getCatchProgress()
                        / 100.0
        );

        drawTimeMeter(
                graphics,
                barX + 210,
                barY,
                fishingGame
                        .getEscapePercent()
        );

        drawGameResult(
                graphics,
                barX,
                barY
        );

        drawReelButton(
                graphics,
                barX,
                barY
        );

        return null;
    }

    // =========================================================
    // FISHING BAR
    // =========================================================

    private void drawFishingBar(
            Graphics2D graphics,
            int barX,
            int barY)
    {
        graphics.setColor(
                Color.BLACK
        );

        graphics.fillRect(
                barX,
                barY,
                BAR_WIDTH,
                BAR_HEIGHT
        );

        graphics.setColor(
                Color.WHITE
        );

        graphics.drawRect(
                barX,
                barY,
                BAR_WIDTH,
                BAR_HEIGHT
        );
    }

    // =========================================================
    // PLAYER BAR
    // =========================================================

    private void drawPlayer(
            Graphics2D graphics,
            int barX,
            int playerY)
    {
        int playerX =
                barX +
                        (BAR_WIDTH - PLAYER_WIDTH) / 2;

        int bevel = 8;

        // Main body
        graphics.setColor(
                new Color(
                        45,
                        160,
                        0
                )
        );

        graphics.fillRect(
                playerX,
                playerY,
                PLAYER_WIDTH,
                PLAYER_HEIGHT
        );

        // Top highlight
        graphics.setColor(
                new Color(
                        100,
                        220,
                        0
                )
        );

        graphics.fillPolygon(
                new int[]
                        {
                                playerX,
                                playerX + PLAYER_WIDTH,
                                playerX + PLAYER_WIDTH - bevel,
                                playerX + bevel
                        },
                new int[]
                        {
                                playerY,
                                playerY,
                                playerY + bevel,
                                playerY + bevel
                        },
                4
        );

        // Left highlight
        graphics.setColor(
                new Color(
                        75,
                        190,
                        0
                )
        );

        graphics.fillPolygon(
                new int[]
                        {
                                playerX,
                                playerX + bevel,
                                playerX + bevel,
                                playerX
                        },
                new int[]
                        {
                                playerY,
                                playerY + bevel,
                                playerY + PLAYER_HEIGHT - bevel,
                                playerY + PLAYER_HEIGHT
                        },
                4
        );

        // Bottom shadow
        graphics.setColor(
                new Color(
                        25,
                        110,
                        0
                )
        );

        graphics.fillPolygon(
                new int[]
                        {
                                playerX,
                                playerX + PLAYER_WIDTH,
                                playerX + PLAYER_WIDTH - bevel,
                                playerX + bevel
                        },
                new int[]
                        {
                                playerY + PLAYER_HEIGHT,
                                playerY + PLAYER_HEIGHT,
                                playerY + PLAYER_HEIGHT - bevel,
                                playerY + PLAYER_HEIGHT - bevel
                        },
                4
        );

        // Right shadow
        graphics.setColor(
                new Color(
                        35,
                        125,
                        0
                )
        );

        graphics.fillPolygon(
                new int[]
                        {
                                playerX + PLAYER_WIDTH,
                                playerX + PLAYER_WIDTH - bevel,
                                playerX + PLAYER_WIDTH - bevel,
                                playerX + PLAYER_WIDTH
                        },
                new int[]
                        {
                                playerY,
                                playerY + bevel,
                                playerY + PLAYER_HEIGHT - bevel,
                                playerY + PLAYER_HEIGHT
                        },
                4
        );
    }

    // =========================================================
    // FISH
    // =========================================================

    private void drawFish(
            Graphics2D graphics,
            int barX,
            int fishY)
    {
        int fishX =
                barX +
                        (BAR_WIDTH - FISH_WIDTH) / 2;

        if (fishImage != null)
        {
            graphics.drawImage(
                    fishImage,
                    fishX,
                    fishY,
                    FISH_WIDTH,
                    FISH_HEIGHT,
                    null
            );
        }
        else
        {
            graphics.setColor(
                    Color.ORANGE
            );

            graphics.fillOval(
                    fishX,
                    fishY,
                    FISH_WIDTH,
                    FISH_HEIGHT
            );
        }
    }

    // =========================================================
    // CATCH METER
    // =========================================================

    private void drawCatchMeter(
            Graphics2D graphics,
            int meterX,
            int meterY,
            double percent)
    {
        int meterWidth = 30;
        int meterHeight = 350;

        graphics.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        14
                )
        );

        graphics.setColor(
                Color.WHITE
        );

        graphics.drawString(
                "CATCH",
                meterX - 8,
                meterY - 10
        );

        graphics.setColor(
                Color.BLACK
        );

        graphics.fillRect(
                meterX,
                meterY,
                meterWidth,
                meterHeight
        );

        graphics.setColor(
                Color.WHITE
        );

        graphics.drawRect(
                meterX,
                meterY,
                meterWidth,
                meterHeight
        );

        int filledHeight =
                (int)
                        (
                                percent *
                                        meterHeight
                        );

        int filledY =
                meterY +
                        meterHeight -
                        filledHeight;

        graphics.setColor(
                new Color(
                        30,
                        210,
                        50
                )
        );

        graphics.fillRect(
                meterX,
                filledY,
                meterWidth,
                filledHeight
        );
    }

    // =========================================================
    // TIME METER
    // =========================================================

    private void drawTimeMeter(
            Graphics2D graphics,
            int meterX,
            int meterY,
            double percent)
    {
        int meterWidth = 30;
        int meterHeight = 350;

        graphics.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        14
                )
        );

        graphics.setColor(
                Color.WHITE
        );

        graphics.drawString(
                "TIME",
                meterX - 2,
                meterY - 10
        );

        graphics.setColor(
                Color.BLACK
        );

        graphics.fillRect(
                meterX,
                meterY,
                meterWidth,
                meterHeight
        );

        graphics.setColor(
                Color.WHITE
        );

        graphics.drawRect(
                meterX,
                meterY,
                meterWidth,
                meterHeight
        );

        int filledHeight =
                (int)
                        (
                                percent *
                                        meterHeight
                        );

        graphics.setColor(
                new Color(
                        60,
                        190,
                        235
                )
        );

        graphics.fillRect(
                meterX,
                meterY,
                meterWidth,
                filledHeight
        );
    }

    // =========================================================
    // GAME RESULT
    // =========================================================

    private void drawGameResult(
            Graphics2D graphics,
            int barX,
            int barY)
    {
        graphics.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        26
                )
        );

        if (fishingGame.isFishCaught())
        {
            graphics.setColor(
                    Color.WHITE
            );

            graphics.drawString(
                    "FISH CAUGHT!",
                    barX - 45,
                    barY - 35
            );
        }

        if (fishingGame.isFishGotAway())
        {
            graphics.setColor(
                    Color.RED
            );

            graphics.drawString(
                    "FISH GOT AWAY!",
                    barX - 65,
                    barY - 35
            );
        }
    }

    // =========================================================
    // REEL BUTTON
    // =========================================================

    private void drawReelButton(
            Graphics2D graphics,
            int barX,
            int barY)
    {
        int buttonWidth = 150;
        int buttonHeight = 50;

        int buttonX =
                barX +
                        (BAR_WIDTH / 2) -
                        (buttonWidth / 2);

        int buttonY =
                barY +
                        BAR_HEIGHT +
                        25;

        Rectangle buttonBounds =
                new Rectangle(
                        buttonX,
                        buttonY,
                        buttonWidth,
                        buttonHeight
                );

        if (fishingInput != null)
        {
            fishingInput.setReelButtonBounds(
                    buttonBounds
            );
        }

        // Outer border
        graphics.setColor(
                Color.BLACK
        );

        graphics.fillRect(
                buttonX - 4,
                buttonY - 4,
                buttonWidth + 8,
                buttonHeight + 8
        );

        // Main gray button
        graphics.setColor(
                new Color(
                        105,
                        105,
                        105
                )
        );

        graphics.fillRect(
                buttonX,
                buttonY,
                buttonWidth,
                buttonHeight
        );

        // Top bevel highlight
        graphics.setColor(
                new Color(
                        160,
                        160,
                        160
                )
        );

        graphics.fillRect(
                buttonX,
                buttonY,
                buttonWidth,
                5
        );

        // Left bevel highlight
        graphics.fillRect(
                buttonX,
                buttonY,
                5,
                buttonHeight
        );

        // Bottom bevel shadow
        graphics.setColor(
                new Color(
                        65,
                        65,
                        65
                )
        );

        graphics.fillRect(
                buttonX,
                buttonY + buttonHeight - 5,
                buttonWidth,
                5
        );

        // Right bevel shadow
        graphics.fillRect(
                buttonX + buttonWidth - 5,
                buttonY,
                5,
                buttonHeight
        );

        // REEL text
        graphics.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        20
                )
        );

        graphics.setColor(
                Color.WHITE
        );

        String text =
                "REEL";

        int textWidth =
                graphics
                        .getFontMetrics()
                        .stringWidth(text);

        int textX =
                buttonX +
                        (buttonWidth - textWidth) / 2;

        int textY =
                buttonY +
                        32;

        graphics.drawString(
                text,
                textX,
                textY
        );
    }
}