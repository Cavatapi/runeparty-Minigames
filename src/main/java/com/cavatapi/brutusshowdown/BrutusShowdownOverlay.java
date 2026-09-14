package com.cavatapi.brutusshowdown;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;

import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public class BrutusShowdownOverlay extends Overlay
{
    private final Client client;
    private final BrutusShowdownPlugin plugin;

    private static final Color ARENA_BORDER_COLOR =
            new Color(255, 255, 255, 220);

    private static final Color BRUTUS_START_COLOR =
            new Color(255, 0, 0, 100);

    private static final Color ACTIVE_ARENA_COLOR =
            new Color(255, 255, 0, 45);

    private static final Color DODGER_START_COLOR =
            new Color(0, 100, 255, 100);

    private static final Color FINISH_LINE_COLOR =
            new Color(0, 255, 0, 140);

    @Inject
    public BrutusShowdownOverlay(
            Client client,
            BrutusShowdownPlugin plugin
    )
    {
        this.client = client;
        this.plugin = plugin;

        setPosition(
                OverlayPosition.DYNAMIC
        );

        setLayer(
                OverlayLayer.ABOVE_SCENE
        );
    }

    @Override
    public Dimension render(
            Graphics2D graphics
    )
    {
        if (client.getPlane()
                != BrutusShowdownPlugin.ARENA_PLANE)
        {
            return null;
        }

        drawArenaBorder(graphics);

        drawZone(
                graphics,
                BrutusShowdownPlugin.BRUTUS_START_MIN_X,
                BrutusShowdownPlugin.BRUTUS_START_MAX_X,
                BrutusShowdownPlugin.BRUTUS_START_MIN_Y,
                BrutusShowdownPlugin.BRUTUS_START_MAX_Y,
                BRUTUS_START_COLOR
        );

        drawZone(
                graphics,
                BrutusShowdownPlugin.ACTIVE_ARENA_MIN_X,
                BrutusShowdownPlugin.ACTIVE_ARENA_MAX_X,
                BrutusShowdownPlugin.ACTIVE_ARENA_MIN_Y,
                BrutusShowdownPlugin.ACTIVE_ARENA_MAX_Y,
                ACTIVE_ARENA_COLOR
        );

        drawZone(
                graphics,
                BrutusShowdownPlugin.DODGER_START_MIN_X,
                BrutusShowdownPlugin.DODGER_START_MAX_X,
                BrutusShowdownPlugin.DODGER_START_MIN_Y,
                BrutusShowdownPlugin.DODGER_START_MAX_Y,
                DODGER_START_COLOR
        );

        drawZone(
                graphics,
                BrutusShowdownPlugin.FINISH_X,
                BrutusShowdownPlugin.FINISH_X,
                BrutusShowdownPlugin.FINISH_MIN_Y,
                BrutusShowdownPlugin.FINISH_MAX_Y,
                FINISH_LINE_COLOR
        );

        drawScore(graphics);

        drawRoundInformation(graphics);

        if (plugin.shouldShowHitMessage())
        {
            drawCenteredText(
                    graphics,
                    "HIT!",
                    150,
                    Color.ORANGE,
                    48
            );
        }

        if (plugin.isRoundOver())
        {
            drawEndButtons(graphics);
        }

        return null;
    }

    // ---------------------------------------------------------
    // SCORE
    // ---------------------------------------------------------

    private void drawScore(
            Graphics2D graphics
    )
    {
        String score =
                "Brutus "
                        + plugin.getBrutusScore()
                        + "  -  "
                        + plugin.getDodgerScore()
                        + " Dodger";

        drawCenteredText(
                graphics,
                score,
                25,
                Color.WHITE,
                20
        );
    }

    // ---------------------------------------------------------
    // ROUND INFORMATION
    // ---------------------------------------------------------

    private void drawRoundInformation(
            Graphics2D graphics
    )
    {
        switch (plugin.getRoundState())
        {
            case WAITING:

                drawCenteredText(
                        graphics,
                        "Stand in the red zone to start",
                        55,
                        Color.WHITE,
                        22
                );

                break;

            case COUNTDOWN:

                int number =
                        plugin.getCountdownNumber();

                if (number > 0)
                {
                    drawCenteredText(
                            graphics,
                            String.valueOf(number),
                            110,
                            Color.YELLOW,
                            60
                    );
                }

                break;

            case PLAYING:

                double time =
                        plugin.getRemainingTimeSeconds();

                Color timerColor =
                        time <= 3.0
                                ? Color.RED
                                : Color.WHITE;

                drawCenteredText(
                        graphics,
                        String.format(
                                "Time: %.1f",
                                time
                        ),
                        55,
                        timerColor,
                        30
                );

                break;

            case SUCCESS:

                drawCenteredText(
                        graphics,
                        plugin.isDodgerHit()
                                ? "BRUTUS SCORES!"
                                : "DODGER SURVIVES!",
                        90,
                        plugin.isDodgerHit()
                                ? Color.GREEN
                                : Color.CYAN,
                        42
                );

                break;

            case TIME_UP:

                drawCenteredText(
                        graphics,
                        plugin.isDodgerHit()
                                ? "BRUTUS SCORES!"
                                : "DODGER SCORES!",
                        90,
                        plugin.isDodgerHit()
                                ? Color.GREEN
                                : Color.CYAN,
                        42
                );

                break;
        }
    }

    // ---------------------------------------------------------
    // END BUTTONS
    // ---------------------------------------------------------

    private void drawEndButtons(
            Graphics2D graphics
    )
    {
        int canvasWidth =
                client.getCanvasWidth();

        int buttonWidth = 150;
        int buttonHeight = 35;

        int replayX =
                canvasWidth / 2
                        - buttonWidth
                        - 5;

        int resetX =
                canvasWidth / 2
                        + 5;

        int buttonY = 120;

        Rectangle replayBounds =
                new Rectangle(
                        replayX,
                        buttonY,
                        buttonWidth,
                        buttonHeight
                );

        Rectangle resetBounds =
                new Rectangle(
                        resetX,
                        buttonY,
                        buttonWidth,
                        buttonHeight
                );

        plugin.setReplayButtonBounds(
                replayBounds
        );

        plugin.setResetScoreButtonBounds(
                resetBounds
        );

        drawButton(
                graphics,
                replayBounds,
                "Replay"
        );

        drawButton(
                graphics,
                resetBounds,
                "Reset Score"
        );
    }

    // ---------------------------------------------------------
    // BUTTON
    // ---------------------------------------------------------

    private void drawButton(
            Graphics2D graphics,
            Rectangle bounds,
            String text
    )
    {
        graphics.setColor(
                new Color(
                        30,
                        30,
                        30,
                        220
                )
        );

        graphics.fillRect(
                bounds.x,
                bounds.y,
                bounds.width,
                bounds.height
        );

        graphics.setColor(
                Color.WHITE
        );

        graphics.drawRect(
                bounds.x,
                bounds.y,
                bounds.width,
                bounds.height
        );

        Font oldFont =
                graphics.getFont();

        Font font =
                new Font(
                        "Arial",
                        Font.BOLD,
                        16
                );

        graphics.setFont(font);

        FontMetrics metrics =
                graphics.getFontMetrics();

        int textX =
                bounds.x
                        + (bounds.width
                        - metrics.stringWidth(text))
                        / 2;

        int textY =
                bounds.y
                        + ((bounds.height
                        - metrics.getHeight())
                        / 2)
                        + metrics.getAscent();

        graphics.drawString(
                text,
                textX,
                textY
        );

        graphics.setFont(
                oldFont
        );
    }

    // ---------------------------------------------------------
    // CENTERED TEXT
    // ---------------------------------------------------------

    private void drawCenteredText(
            Graphics2D graphics,
            String text,
            int y,
            Color color,
            int fontSize
    )
    {
        Font oldFont =
                graphics.getFont();

        Font font =
                new Font(
                        "Arial",
                        Font.BOLD,
                        fontSize
                );

        graphics.setFont(font);

        FontMetrics metrics =
                graphics.getFontMetrics();

        int x =
                (client.getCanvasWidth()
                        - metrics.stringWidth(text))
                        / 2;

        graphics.setColor(
                Color.BLACK
        );

        graphics.drawString(
                text,
                x + 2,
                y + 2
        );

        graphics.setColor(
                color
        );

        graphics.drawString(
                text,
                x,
                y
        );

        graphics.setFont(
                oldFont
        );
    }

    // ---------------------------------------------------------
    // BORDER
    // ---------------------------------------------------------

    private void drawArenaBorder(
            Graphics2D graphics
    )
    {
        for (
                int x =
                BrutusShowdownPlugin.ARENA_MIN_X;
                x <=
                        BrutusShowdownPlugin.ARENA_MAX_X;
                x++
        )
        {
            drawBorderTile(
                    graphics,
                    x,
                    BrutusShowdownPlugin.ARENA_MIN_Y
            );

            drawBorderTile(
                    graphics,
                    x,
                    BrutusShowdownPlugin.ARENA_MAX_Y
            );
        }

        for (
                int y =
                BrutusShowdownPlugin.ARENA_MIN_Y + 1;
                y <
                        BrutusShowdownPlugin.ARENA_MAX_Y;
                y++
        )
        {
            drawBorderTile(
                    graphics,
                    BrutusShowdownPlugin.ARENA_MIN_X,
                    y
            );

            drawBorderTile(
                    graphics,
                    BrutusShowdownPlugin.ARENA_MAX_X,
                    y
            );
        }
    }

    // ---------------------------------------------------------
    // ZONE
    // ---------------------------------------------------------

    private void drawZone(
            Graphics2D graphics,
            int minX,
            int maxX,
            int minY,
            int maxY,
            Color color
    )
    {
        for (int x = minX;
             x <= maxX;
             x++)
        {
            for (int y = minY;
                 y <= maxY;
                 y++)
            {
                drawFilledTile(
                        graphics,
                        x,
                        y,
                        color
                );
            }
        }
    }

    // ---------------------------------------------------------
    // BORDER TILE
    // ---------------------------------------------------------

    private void drawBorderTile(
            Graphics2D graphics,
            int x,
            int y
    )
    {
        WorldPoint point =
                new WorldPoint(
                        x,
                        y,
                        BrutusShowdownPlugin.ARENA_PLANE
                );

        LocalPoint localPoint =
                LocalPoint.fromWorld(
                        client,
                        point
                );

        if (localPoint == null)
        {
            return;
        }

        Polygon polygon =
                Perspective.getCanvasTilePoly(
                        client,
                        localPoint
                );

        if (polygon == null)
        {
            return;
        }

        OverlayUtil.renderPolygon(
                graphics,
                polygon,
                ARENA_BORDER_COLOR
        );
    }

    // ---------------------------------------------------------
    // FILLED TILE
    // ---------------------------------------------------------

    private void drawFilledTile(
            Graphics2D graphics,
            int x,
            int y,
            Color color
    )
    {
        WorldPoint point =
                new WorldPoint(
                        x,
                        y,
                        BrutusShowdownPlugin.ARENA_PLANE
                );

        LocalPoint localPoint =
                LocalPoint.fromWorld(
                        client,
                        point
                );

        if (localPoint == null)
        {
            return;
        }

        Polygon polygon =
                Perspective.getCanvasTilePoly(
                        client,
                        localPoint
                );

        if (polygon == null)
        {
            return;
        }

        graphics.setColor(color);

        graphics.fillPolygon(
                polygon
        );

        Color outline =
                new Color(
                        color.getRed(),
                        color.getGreen(),
                        color.getBlue(),
                        200
                );

        OverlayUtil.renderPolygon(
                graphics,
                polygon,
                outline
        );
    }
}