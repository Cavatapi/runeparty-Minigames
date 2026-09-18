package gay.runescape.runeparty.models;

import gay.runescape.runeparty.SceneObjectSet;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;

import java.util.Map;

/**
 * Handles the visible rune models used by Rune Match.
 *
 * Each visible rune is keyed by its Rune Match tile index (0-15).
 * The RuneSpawn contains the tile's WorldPoint and the model ID
 * that should be displayed there.
 */
public final class RuneMatchRuneModel
{
    private final Client client;
    private final SceneObjectSet<Integer> objects;

    public RuneMatchRuneModel(Client client)
    {
        this.client = client;
        this.objects = new SceneObjectSet<>(client);
    }

    /**
     * Synchronizes the visible Rune Match rune models.
     *
     * Key:
     *     Rune Match tile index (0-15)
     *
     * Value:
     *     WorldPoint + model ID for the rune on that tile
     */
    public void update(Map<Integer, RuneSpawn> runes)
    {
        objects.sync(
                runes.keySet(),

                index ->
                {
                    RuneSpawn rune = runes.get(index);

                    if (rune == null)
                    {
                        return null;
                    }

                    return rune.point;
                },

                index ->
                {
                    RuneSpawn rune = runes.get(index);

                    if (rune == null)
                    {
                        return null;
                    }

                    return client.loadModel(rune.modelId);
                }
        );
    }

    /**
     * Removes every Rune Match model from the scene.
     */
    public void clear()
    {
        objects.clear();
    }

    /**
     * Describes one rune currently being displayed.
     */
    public static final class RuneSpawn
    {
        public final WorldPoint point;
        public final int modelId;

        public RuneSpawn(WorldPoint point, int modelId)
        {
            this.point = point;
            this.modelId = modelId;
        }
    }
}