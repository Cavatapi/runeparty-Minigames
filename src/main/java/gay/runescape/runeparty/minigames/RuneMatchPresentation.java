package gay.runescape.runeparty.minigames;

import gay.runescape.runeparty.RunePartyPlugin;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Client-side state and gameplay logic for Rune Match.
 *
 * Board layout:
 *
 * B B B B B B B B B
 * B R . R . R . R B
 * B . . . . . . . B
 * B R . R . R . R B
 * B . . . . . . . B
 * B R . R . R . R B
 * B . . . . . . . B
 * B R . R . R . R B
 * B B B B B B B B B
 *
 * B = border
 * R = Rune Match card
 * . = normal arena floor
 */
public class RuneMatchPresentation implements MinigamePresentationFeature
{
    private static final int ARENA_SIZE = 9;

    private final RunePartyPlugin plugin;

    private WorldPoint arenaCenter;

    private final List<WorldPoint> arenaTiles = new ArrayList<>();
    private final List<WorldPoint> borderTiles = new ArrayList<>();
    private final List<WorldPoint> runeTiles = new ArrayList<>();
    private final List<WorldPoint> floorTiles = new ArrayList<>();

    public RuneMatchPresentation(RunePartyPlugin plugin)
    {
        this.plugin = plugin;
    }

    /**
     * Builds a 9x9 Rune Match arena centered on the supplied WorldPoint.
     */
    public void buildArena(WorldPoint center)
    {
        reset();

        if (center == null)
        {
            return;
        }

        arenaCenter = center;

        int startX = center.getX() - 4;
        int startY = center.getY() - 4;
        int plane = center.getPlane();

        for (int y = 0; y < ARENA_SIZE; y++)
        {
            for (int x = 0; x < ARENA_SIZE; x++)
            {
                WorldPoint point = new WorldPoint(
                        startX + x,
                        startY + y,
                        plane
                );

                arenaTiles.add(point);

                if (isBorderPosition(x, y))
                {
                    borderTiles.add(point);
                }
                else if (isRunePosition(x, y))
                {
                    runeTiles.add(point);
                }
                else
                {
                    floorTiles.add(point);
                }
            }
        }
    }

    /**
     * Outer edge of the 9x9 arena.
     */
    private boolean isBorderPosition(int x, int y)
    {
        return x == 0
                || x == ARENA_SIZE - 1
                || y == 0
                || y == ARENA_SIZE - 1;
    }

    /**
     * Rune cards occupy every other interior position:
     *
     * x = 1, 3, 5, 7
     * y = 1, 3, 5, 7
     *
     * This produces exactly 16 cards.
     */
    private boolean isRunePosition(int x, int y)
    {
        return x >= 1 && x <= 7
                && y >= 1 && y <= 7
                && x % 2 == 1
                && y % 2 == 1;
    }

    public WorldPoint getArenaCenter()
    {
        return arenaCenter;
    }

    public List<WorldPoint> getArenaTiles()
    {
        return Collections.unmodifiableList(arenaTiles);
    }

    public List<WorldPoint> getBorderTiles()
    {
        return Collections.unmodifiableList(borderTiles);
    }

    public List<WorldPoint> getRuneTiles()
    {
        return Collections.unmodifiableList(runeTiles);
    }

    public List<WorldPoint> getFloorTiles()
    {
        return Collections.unmodifiableList(floorTiles);
    }

    /**
     * Returns the card index at this WorldPoint, or null if the
     * point isn't one of Rune Match's 16 card positions.
     *
     * Indices are 0-15.
     */
    public Integer getRuneIndex(WorldPoint point)
    {
        if (point == null)
        {
            return null;
        }

        int index = runeTiles.indexOf(point);
        return index >= 0 ? index : null;
    }

    public void reset()
    {
        arenaCenter = null;

        arenaTiles.clear();
        borderTiles.clear();
        runeTiles.clear();
        floorTiles.clear();
    }
}