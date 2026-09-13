package gay.runescape.runeparty.overlays;

import gay.runescape.runeparty.GamePhase;
import gay.runescape.runeparty.RunePartyPlugin;
import gay.runescape.runeparty.SceneObjectSet;

import net.runelite.api.Animation;
import net.runelite.api.Client;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Spawns a single, purely-decorative Gemstone Crab NPC model (composition {@link
 * RunePartyPlugin#GEMSTONE_CRAB_NPC_ID}) dead center of the Crab Rave arena for as long as that
 * mini-game is active -- the arena's own atmosphere otherwise comes from its single merged outline
 * plus the randomly-flashing "club lighting" cells (see TileOverlay#renderCrabRaveTile), not a
 * crowd of crabs. Zero gameplay effect, so unlike JaddyDuelModel (which spawns the same shape of
 * thing for a real duel) there's no recolor, no health bar, no death sequence, and no
 * server-driven spawn event: the crab's own real-world position is derived purely from the
 * currently-marked CRAB_RAVE_TILE tiles' own bounding box (see {@link #relativeOffsets}), the same
 * "client derives shared geometry from tiles it already has, no wire data needed" reasoning every
 * other arena mini-game's own client geometry already relies on -- since placement has no gameplay
 * consequence, every client landing on the identical spot is enough, no broadcast required.
 * <p>
 * Idles on {@link RunePartyPlugin#GEMSTONE_CRAB_IDLE_ANIMATION_ID} in a loop the whole time --
 * applied once per freshly-spawned object (checked via {@code getAnimation() == null}, simpler
 * than JaddyDuelModel's own boolean-flag idiom since there's no second, later animation this class
 * ever needs to switch to). */
public final class CrabRaveNpcOverlay extends Overlay
{
    // One crab, dead center of the arena -- GRID_SIZE is 8 (minigames/crab_rave.py), so (4, 4) is
    // the closest single tile to the block's own true center (3.5, 3.5). Per the user's own
    // explicit call: a single centerpiece crab, not a crowd -- the "rave" now reads through the
    // dance floor's own randomly-flashing club lighting (see TileOverlay#renderCrabRaveTile)
    // instead of multiple crabs.
    private static final int[][] RELATIVE_OFFSETS =
    {
        {4, 4},
    };

    private final Client client;
    private final RunePartyPlugin plugin;
    private final SceneObjectSet<Integer> objects;

    public CrabRaveNpcOverlay(Client client, RunePartyPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;
        this.objects = new SceneObjectSet<>(client);

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D g)
    {
        if (plugin.getPhase() != GamePhase.ACTIVE || !plugin.isCrabRaveActive())
        {
            clear();
            return null;
        }

        List<WorldPoint> spots = relativeOffsets();
        if (spots.isEmpty())
        {
            clear();
            return null;
        }

        Set<Integer> desired = new HashSet<>();
        for (int i = 0; i < spots.size(); i++) desired.add(i);

        objects.sync(desired, i -> spots.get(i), i -> RunePartyRender.loadNpcModel(client, RunePartyPlugin.GEMSTONE_CRAB_NPC_ID));

        for (Integer i : desired)
        {
            RuneLiteObject obj = objects.get(i);
            if (obj == null || obj.getModel() == null) continue;
            if (obj.getAnimation() == null)
            {
                Animation anim = client.loadAnimation(RunePartyPlugin.GEMSTONE_CRAB_IDLE_ANIMATION_ID);
                if (anim != null)
                {
                    obj.setShouldLoop(true);
                    obj.setAnimation(anim);
                }
            }
        }

        return null;
    }

    /** Every crab's own real-world spawn point, derived from the current CRAB_RAVE_TILE arena's
     * own bounding box plus {@link #RELATIVE_OFFSETS} -- empty if the board isn't actually
     * swapped to the arena right now (e.g. mid board-swap, or the round already ended). */
    private List<WorldPoint> relativeOffsets()
    {
        List<WorldPoint> tiles = plugin.findCrabRaveTilePoints();
        if (tiles.isEmpty()) return List.of();

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int plane = tiles.get(0).getPlane();
        for (WorldPoint p : tiles)
        {
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
        }

        List<WorldPoint> spots = new ArrayList<>();
        for (int[] offset : RELATIVE_OFFSETS)
        {
            spots.add(new WorldPoint(minX + offset[0], minY + offset[1], plane));
        }
        return spots;
    }

    /** Despawns and forgets every Gemstone Crab RuneLiteObject -- a RuneLiteObject otherwise stays
     * registered with the client independently of this overlay or even the plugin being active. */
    public void clear()
    {
        objects.clear();
    }
}
