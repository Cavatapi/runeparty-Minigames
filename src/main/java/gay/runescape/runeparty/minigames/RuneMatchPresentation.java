package gay.runescape.runeparty.minigames;

import gay.runescape.runeparty.RunePartyPlugin;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Client-side state and gameplay logic for Rune Match.
 *
 * The server supplies a 7x7 arena made from RUNE_MATCH_TILEs.
 * The client sorts those tiles into a stable grid and uses
 * every other tile for the 16 Rune Match cards.
 *
 * Board layout:
 *
 * R . R . R . R
 * . . . . . . .
 * R . R . R . R
 * . . . . . . .
 * R . R . R . R
 * . . . . . . .
 * R . R . R . R
 *
 * R = Rune Match card
 * . = normal arena floor
 */
public class RuneMatchPresentation implements MinigamePresentationFeature
{
    private static final int ARENA_SIZE = 7;
    private static final int EXPECTED_ARENA_TILES = 49;
    private static final int EXPECTED_RUNE_TILES = 16;

    /*
     * Model IDs for the eight rune types.
     * Each model appears exactly twice.
     */
    public static final int FIRE_RUNE = 554;
    public static final int WATER_RUNE = 555;
    public static final int AIR_RUNE = 556;
    public static final int EARTH_RUNE = 557;
    public static final int MIND_RUNE = 558;
    public static final int DEATH_RUNE = 560;
    public static final int CHAOS_RUNE = 562;
    public static final int BLOOD_RUNE = 565;

    private final RunePartyPlugin plugin;

    private WorldPoint arenaCenter;

    private final List<WorldPoint> arenaTiles = new ArrayList<>();
    private final List<WorldPoint> runeTiles = new ArrayList<>();
    private final List<WorldPoint> floorTiles = new ArrayList<>();

    /*
     * runeAssignments.get(0) corresponds to runeTiles.get(0),
     * runeAssignments.get(1) corresponds to runeTiles.get(1), etc.
     */
    private final List<Integer> runeAssignments = new ArrayList<>();

    private long boardSeed;

    public RuneMatchPresentation(RunePartyPlugin plugin)
    {
        this.plugin = plugin;
    }

    /**
     * Reads the current RUNE_MATCH_TILE board from the TileReducer.
     *
     * A temporary seed is generated locally for now. Later, the
     * multiplayer server can provide the same seed to every client.
     */
    public boolean buildBoardFromServerTiles()
    {
        return buildBoardFromServerTiles(System.nanoTime());
    }

    /**
     * Reads and sorts the 49 server-provided RUNE_MATCH_TILEs and
     * creates the 16 rune positions using a deterministic seed.
     */
    public boolean buildBoardFromServerTiles(long seed)
    {
        List<WorldPoint> serverTiles = plugin.findRuneMatchTilePoints();

        if (serverTiles == null || serverTiles.size() != EXPECTED_ARENA_TILES)
        {
            return false;
        }

        reset();

        /*
         * Sort by Y first, then X.
         *
         * This gives us a stable row-major 7x7 grid regardless of
         * the order TileReducer.snapshot() happens to return.
         */
        List<WorldPoint> sortedTiles = new ArrayList<>(serverTiles);

        sortedTiles.sort(
                Comparator.comparingInt(WorldPoint::getY)
                        .thenComparingInt(WorldPoint::getX)
        );

        /*
         * Verify that the received tiles actually form a complete
         * 7x7 square before accepting the board.
         */
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (WorldPoint point : sortedTiles)
        {
            minX = Math.min(minX, point.getX());
            maxX = Math.max(maxX, point.getX());
            minY = Math.min(minY, point.getY());
            maxY = Math.max(maxY, point.getY());
        }

        if (maxX - minX + 1 != ARENA_SIZE ||
                maxY - minY + 1 != ARENA_SIZE)
        {
            reset();
            return false;
        }

        arenaCenter = new WorldPoint(
                minX + 3,
                minY + 3,
                sortedTiles.get(0).getPlane()
        );

        boardSeed = seed;

        /*
         * Classify every tile by its position inside the 7x7 grid.
         */
        for (WorldPoint point : sortedTiles)
        {
            int x = point.getX() - minX;
            int y = point.getY() - minY;

            arenaTiles.add(point);

            if (isRunePosition(x, y))
            {
                runeTiles.add(point);
            }
            else
            {
                floorTiles.add(point);
            }
        }

        if (runeTiles.size() != EXPECTED_RUNE_TILES)
        {
            reset();
            return false;
        }

        generateRuneAssignments(seed);

        return true;
    }

    /**
     * Creates two copies of each rune model and shuffles them.
     */
    private void generateRuneAssignments(long seed)
    {
        runeAssignments.clear();

        addPair(FIRE_RUNE);
        addPair(WATER_RUNE);
        addPair(AIR_RUNE);
        addPair(EARTH_RUNE);
        addPair(MIND_RUNE);
        addPair(DEATH_RUNE);
        addPair(CHAOS_RUNE);
        addPair(BLOOD_RUNE);

        Collections.shuffle(runeAssignments, new Random(seed));
    }

    private void addPair(int runeModelId)
    {
        runeAssignments.add(runeModelId);
        runeAssignments.add(runeModelId);
    }

    /**
     * Rune cards occupy every other tile:
     *
     * (0,0) (2,0) (4,0) (6,0)
     * (0,2) (2,2) (4,2) (6,2)
     * (0,4) (2,4) (4,4) (6,4)
     * (0,6) (2,6) (4,6) (6,6)
     */
    private boolean isRunePosition(int x, int y)
    {
        return x % 2 == 0 && y % 2 == 0;
    }

    public boolean isBoardBuilt()
    {
        return arenaTiles.size() == EXPECTED_ARENA_TILES
                && runeTiles.size() == EXPECTED_RUNE_TILES
                && runeAssignments.size() == EXPECTED_RUNE_TILES;
    }

    public WorldPoint getArenaCenter()
    {
        return arenaCenter;
    }

    public List<WorldPoint> getArenaTiles()
    {
        return Collections.unmodifiableList(arenaTiles);
    }

    public List<WorldPoint> getRuneTiles()
    {
        return Collections.unmodifiableList(runeTiles);
    }

    public List<WorldPoint> getFloorTiles()
    {
        return Collections.unmodifiableList(floorTiles);
    }

    public List<Integer> getRuneAssignments()
    {
        return Collections.unmodifiableList(runeAssignments);
    }

    public Integer getRuneModelId(int index)
    {
        if (index < 0 || index >= runeAssignments.size())
        {
            return null;
        }

        return runeAssignments.get(index);
    }

    public Integer getRuneModelId(WorldPoint point)
    {
        Integer index = getRuneIndex(point);

        if (index == null)
        {
            return null;
        }

        return getRuneModelId(index);
    }

    /**
     * Returns the logical card index (0-15), or null if the supplied
     * point isn't one of the Rune Match card tiles.
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

    public long getBoardSeed()
    {
        return boardSeed;
    }

    @Override
    public void reset()
    {
        arenaCenter = null;
        boardSeed = 0L;

        arenaTiles.clear();
        runeTiles.clear();
        floorTiles.clear();
        runeAssignments.clear();
    }
}