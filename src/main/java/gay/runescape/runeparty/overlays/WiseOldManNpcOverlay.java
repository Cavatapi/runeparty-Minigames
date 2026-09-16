package gay.runescape.runeparty.overlays;

import gay.runescape.runeparty.GamePhase;
import gay.runescape.runeparty.RunePartyPlugin;
import gay.runescape.runeparty.SceneObjectSet;

import net.runelite.api.Animation;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Spawns a purely-decorative Wise Old Man NPC model standing one tile south of every currently-
 * marked WISE_OLD_MAN_TILE, for as long as the game itself is active -- unlike CrabRaveNpcOverlay's
 * own spawns (only up for that one mini-game's own duration), this tracks the tile's own real,
 * host-placed presence on the board directly, the same "no server-driven spawn event, every
 * client derives an identical spot from tiles it already has" reasoning that class's own doc
 * gives. Zero gameplay effect -- the actual encounter is driven entirely by landing on the tile
 * itself (see WiseOldManDialogueOverlay); this is just who's standing there. Idles on
 * WISE_OLD_MAN_IDLE_ANIMATION_ID the whole time, facing the tile itself. */
public final class WiseOldManNpcOverlay extends Overlay
{
    private final Client client;
    private final RunePartyPlugin plugin;
    private final SceneObjectSet<WorldPoint> objects;

    public WiseOldManNpcOverlay(Client client, RunePartyPlugin plugin)
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
        if (plugin.getPhase() != GamePhase.ACTIVE)
        {
            clear();
            return null;
        }

        List<WorldPoint> tiles = plugin.findWiseOldManTilePoints();
        if (tiles.isEmpty())
        {
            clear();
            return null;
        }

        Set<WorldPoint> desired = new HashSet<>(tiles);
        objects.sync(desired, WiseOldManNpcOverlay::spawnPoint, k -> RunePartyRender.loadNpcModel(client, RunePartyPlugin.WISE_OLD_MAN_NPC_ID));

        for (WorldPoint tilePoint : desired)
        {
            RuneLiteObject obj = objects.get(tilePoint);
            if (obj == null || obj.getModel() == null) continue;

            obj.setOrientation(RunePartyRender.orientationFacing(spawnPoint(tilePoint), tilePoint));

            if (obj.getAnimation() == null)
            {
                Animation anim = client.loadAnimation(RunePartyPlugin.WISE_OLD_MAN_IDLE_ANIMATION_ID);
                if (anim != null)
                {
                    obj.setShouldLoop(true);
                    obj.setAnimation(anim);
                }
            }
        }

        return null;
    }

    private static WorldPoint spawnPoint(WorldPoint tilePoint)
    {
        return tilePoint.dy(-1);
    }

    /** Despawns and forgets every Wise Old Man RuneLiteObject -- see CrabRaveNpcOverlay's own
     * clear() doc for why this is needed independently of the overlay/plugin being active. */
    public void clear()
    {
        objects.clear();
    }
}
