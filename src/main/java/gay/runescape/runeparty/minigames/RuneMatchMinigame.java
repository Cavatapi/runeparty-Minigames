package gay.runescape.runeparty.minigames;

import gay.runescape.runeparty.RunePartyPlugin;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Rune Match
 *
 * Players search a 9x9 arena for eight matching pairs of runes.
 * The outer ring forms the arena border. Inside the arena are
 * sixteen playable matching tiles arranged as a spaced 4x4 grid.
 *
 * Rune reveals are local to each player.
 */
public class RuneMatchMinigame implements Minigame
{
    private static final Color HIDDEN_CELL = new Color(110, 110, 110);
    private static final Color OUTLINE = new Color(255, 255, 255);

    @Override
    public String getKey()
    {
        return "rune-match";
    }

    @Override
    public String getDisplayName()
    {
        return "Rune Match";
    }

    /**
     * Wheel icon: a small 4x4 representation of the matching board.
     */
    @Override
    public void drawIcon(Graphics2D g, int x, int y, int size, float alpha)
    {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255)));

        int cell = Math.max(2, size / 5);
        int gap = Math.max(1, cell / 4);

        int total = (cell * 4) + (gap * 3);
        int left = x - total / 2;
        int top = y - total / 2;

        Color old = g.getColor();

        for (int row = 0; row < 4; row++)
        {
            for (int col = 0; col < 4; col++)
            {
                int px = left + col * (cell + gap);
                int py = top + row * (cell + gap);

                Rectangle2D square =
                        new Rectangle2D.Float(px, py, cell, cell);

                g.setColor(withAlpha(HIDDEN_CELL, a));
                g.fill(square);

                g.setColor(withAlpha(OUTLINE, a));
                g.draw(square);
            }
        }

        g.setColor(old);
    }

    private static Color withAlpha(Color c, int alpha)
    {
        return new Color(
                c.getRed(),
                c.getGreen(),
                c.getBlue(),
                alpha
        );
    }

    @Override
    public JComponent createControlPanel(RunePartyPlugin plugin)
    {
        return new JPanel();
    }

    @Override
    public boolean hasSidePanelPresence()
    {
        return false;
    }
}