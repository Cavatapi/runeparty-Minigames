package gay.runescape.runeparty.overlays;

import gay.runescape.runeparty.GamePhase;
import gay.runescape.runeparty.RosterReducer;
import gay.runescape.runeparty.RunePartyPlugin;

import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/** A conversation with the Wise Old Man, drawn over the chatbox in the game's own dialog style --
 * chathead, speaker name, greeting text, and clickable options -- for the mini-encounter opened by
 * landing on his tile (see the server's own wise_old_man_encounter_opened doc). Only ever shows
 * for the local player actually mid-encounter (see WiseOldManPresentation#getEncounterRsn) -- every
 * other seated client sees nothing of this box at all, only the eventual "X stole ... from Y!"
 * announcement once (if) it resolves as a steal (see AnnouncementOverlay#renderWiseOldManStolen).
 * <p>
 * Drawn rather than driven through the game's real dialog interface, same reasoning the Follower
 * Buddy plugin's own FollowerDialog gives (https://github.com/MikeSpatol/follower-buddy): the real
 * dialog widgets only exist while the game itself has a conversation open, and no script exists
 * for a plugin to open one on demand. The chathead is genuine even so -- ChatheadRenderer projects
 * the Wise Old Man's own real NPC chathead model (adapted from that same plugin's own
 * ChatheadRenderer/GouraudRasterizer/GameColourTable) -- just a much simpler flow than that
 * plugin's own branching conversation engine, since this is a fixed two-screen choice (which
 * action, then which target), not an authored script.
 * <p>
 * Two screens: ACTION_CHOICE (the greeting + steal-coins/steal-golden-gnome/decline options -- the
 * Golden Gnome option is hidden outright when the local player can't afford it or nobody else
 * holds one, per this feature's own confirmed design) and TARGET_SELECT (every eligible other
 * seated player as its own clickable row, simply paginated if there are more than fit on one
 * screen -- up to MAX_PLAYERS - 1 candidates is small enough that this never needs to be
 * sophisticated). Submits via RunePartyPlugin#submitWiseOldManChoice the instant a target (or
 * "Never mind") is clicked, then closes itself immediately rather than waiting for the server's
 * own confirming WISE_OLD_MAN_DISMISSED -- same "set optimistically on submit" latitude
 * goldenGnomePurchasedThisTurn's own doc describes. */
public final class WiseOldManDialogueOverlay extends Overlay
{
    private static final int CHATHEAD_SIZE = 130;
    private static final int CHATBOX_SPRITE_ID = 1017; // the real client's own chatbox background sprite
    // Real widget bounds when available (see computeBounds) -- this fallback only covers the rare
    // frame it isn't (not yet loaded, or hidden). 519x165 is the real chatbox's own fixed-layout
    // size; BUTTON_STRIP (matching Follower Buddy's own FollowerDialog#BUTTON_STRIP) is the strip
    // along its bottom edge -- the filter/report/friend-chat button row -- that both the real
    // dialog interface and this one have to stop short of rather than draw over.
    private static final int FALLBACK_WIDTH = 519;
    private static final int FALLBACK_HEIGHT = 165;
    private static final int BUTTON_STRIP = 23;
    private static final int TEXT_LEFT = 140; // clears the chathead portrait on the left
    private static final int TEXT_RIGHT_MARGIN = 20;
    private static final int NAME_TOP_OFFSET = 22;
    private static final int BODY_TOP_OFFSET = 44;
    private static final int OPTION_ROW_HEIGHT = 18;
    private static final int OPTIONS_TOP_OFFSET = 78;
    private static final int TARGETS_PER_PAGE = 5;

    private static final Color PARCHMENT = new Color(0x4c3a28);
    private static final Color BORDER = new Color(0, 0, 0);
    private static final Color NAME_COLOR = new Color(0xff9040);
    private static final Color BODY_COLOR = Color.WHITE;
    private static final Color OPTION_COLOR = new Color(0x8888ff);
    private static final Color OPTION_HOVER_COLOR = Color.WHITE;

    private enum Screen { ACTION_CHOICE, TARGET_SELECT }

    private final Client client;
    private final RunePartyPlugin plugin;
    private final MouseManager mouseManager;
    private final SpriteManager spriteManager;
    private final RosterReducer roster;

    // All fields below are touched from both the client thread (render(), every frame) and the
    // AWT event thread (clickAdapter, on a real mouse click) -- volatile (or, for the two arrays,
    // a fresh array swapped in atomically each frame) rather than synchronized, same latitude
    // JadPresentation's own cross-thread fields take: a click landing one frame stale just means
    // it's ignored or acts on the previous frame's geometry, never a torn read.
    private volatile boolean open = false;
    // The last encounterRsn actually seen (including null) -- see render()'s own doc for why local
    // UI state resets exactly when this changes, not whenever the box merely isn't open.
    private volatile String lastSeenEncounterRsn = null;
    private volatile Screen screen = Screen.ACTION_CHOICE;
    private volatile String pendingActionChoice = null; // "steal_coins" | "steal_golden_gnome", set once picked on screen 1
    private volatile int targetPage = 0;
    private volatile boolean submitted = false; // set the instant a final choice goes out -- see this class's own doc
    private volatile Rectangle bounds = new Rectangle();
    private volatile Rectangle[] optionBounds = new Rectangle[0];
    private volatile Runnable[] optionCallbacks = new Runnable[0];
    // Deferred click action, consumed on the very next render() call (client thread) rather than
    // run directly inside mousePressed (AWT thread) -- same safety idiom FollowerDialog's own
    // pendingAction field follows, since plugin.submitWiseOldManChoice/screen transitions have no
    // business running off the client thread.
    private volatile Runnable pendingClick;

    // The Wise Old Man's own chathead portrait, rendered once and cached forever -- his model
    // never changes, unlike a custom composed appearance that can animate its own jaw mid-line
    // (see ChatheadRenderer's own doc for why the plugin this was adapted from can't just do this).
    private BufferedImage chatheadImage;

    private final MouseAdapter clickAdapter = new MouseAdapter()
    {
        @Override
        public MouseEvent mousePressed(MouseEvent event)
        {
            if (!open || !SwingUtilities.isLeftMouseButton(event)) return event;
            if (!bounds.contains(event.getPoint())) return event; // outside the box -- let it pass through untouched

            Rectangle[] rects = optionBounds;
            Runnable[] callbacks = optionCallbacks;
            for (int i = 0; i < rects.length; i++)
            {
                if (rects[i] != null && rects[i].contains(event.getPoint()) && callbacks[i] != null)
                {
                    Runnable callback = callbacks[i];
                    pendingClick = callback;
                    break;
                }
            }

            // Consumed either way -- the whole box is a dead zone for the game's own click-to-walk
            // underneath it, same as the real dialog interface is.
            event.consume();
            return event;
        }
    };

    public WiseOldManDialogueOverlay(Client client, RunePartyPlugin plugin, MouseManager mouseManager, SpriteManager spriteManager, RosterReducer roster)
    {
        this.client = client;
        this.plugin = plugin;
        this.mouseManager = mouseManager;
        this.spriteManager = spriteManager;
        this.roster = roster;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    public void register()
    {
        mouseManager.registerMouseListener(clickAdapter);
    }

    public void unregister()
    {
        mouseManager.unregisterMouseListener(clickAdapter);
    }

    @Override
    public Dimension render(Graphics2D g)
    {
        String self = plugin.getLocalRsn();
        String encounterRsn = plugin.getWiseOldManEncounterRsn();

        // Fresh local UI state (screen/pendingActionChoice/targetPage/submitted) exactly when the
        // real encounterRsn itself changes -- a genuinely new encounter opening, or none at all
        // anymore. Deliberately NOT keyed off shouldBeOpen/open below: those also go false the
        // instant a final choice is submitted, well before the server's own confirming
        // WISE_OLD_MAN_DISMISSED actually lands and changes encounterRsn -- resetting `submitted`
        // on that transition instead would flip it back to false while encounterRsn is still the
        // local player's own, reopening the box for one or more stray frames until the real
        // dismissal finally arrives.
        if (!java.util.Objects.equals(encounterRsn, lastSeenEncounterRsn))
        {
            lastSeenEncounterRsn = encounterRsn;
            resetForNextEncounter();
        }

        boolean shouldBeOpen = plugin.getPhase() == GamePhase.ACTIVE
            && self != null && self.equalsIgnoreCase(encounterRsn)
            && !submitted
            && System.currentTimeMillis() >= plugin.getWiseOldManRevealAt();

        open = shouldBeOpen;
        if (!shouldBeOpen)
        {
            return null;
        }

        Runnable click = pendingClick;
        pendingClick = null;
        if (click != null) click.run();
        if (submitted) return null; // the click above just submitted a final choice -- nothing left to draw this frame

        bounds = computeBounds();

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawBackground(g);
        drawChathead(g);
        drawName(g);

        if (screen == Screen.ACTION_CHOICE)
        {
            drawActionChoice(g, self);
        }
        else
        {
            drawTargetSelect(g);
        }

        return null;
    }

    /** The real chatbox widget's own current on-screen rectangle, minus BUTTON_STRIP off its
     * bottom edge -- adapts automatically to whichever layout (fixed/resizable, classic/modern,
     * with or without the side panels open) the player's actually using, same reasoning
     * FollowerDialog's own doc gives for checking the real widget first rather than always trusting
     * a fixed guess: a hardcoded rectangle sized for the default fixed layout sat noticeably lower
     * than the real chat surface under other layouts, covering the report/friend-chat button row
     * beneath it instead of stopping short like the real dialog interface does. Falls back to that
     * same fixed guess only on the rare frame the real widget isn't available at all (not loaded
     * yet, or hidden -- e.g. the player's closed the chat panel). */
    private Rectangle computeBounds()
    {
        Widget chat = client.getWidget(InterfaceID.CHATBOX, 0);
        Rectangle area = chat != null && !chat.isHidden()
            ? chat.getBounds()
            : new Rectangle(0, client.getCanvasHeight() - FALLBACK_HEIGHT, FALLBACK_WIDTH, FALLBACK_HEIGHT);
        return new Rectangle(area.x, area.y, area.width, Math.max(0, area.height - BUTTON_STRIP));
    }

    private void resetForNextEncounter()
    {
        screen = Screen.ACTION_CHOICE;
        pendingActionChoice = null;
        targetPage = 0;
        submitted = false;
        pendingClick = null;
        optionBounds = new Rectangle[0];
        optionCallbacks = new Runnable[0];
    }

    private void drawBackground(Graphics2D g)
    {
        BufferedImage sprite = spriteManager.getSprite(CHATBOX_SPRITE_ID, 0);
        if (sprite != null)
        {
            g.drawImage(sprite, bounds.x, bounds.y, bounds.width, bounds.height, null);
        }
        else
        {
            g.setColor(PARCHMENT);
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
        g.setColor(BORDER);
        g.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
    }

    private BufferedImage chathead()
    {
        if (chatheadImage != null) return chatheadImage;
        Model model = RunePartyRender.loadNpcChatheadModel(client, RunePartyPlugin.WISE_OLD_MAN_NPC_ID);
        if (model == null) return null; // not cached yet -- keep retrying every frame until it resolves
        chatheadImage = ChatheadRenderer.render(model, CHATHEAD_SIZE, CHATHEAD_SIZE);
        return chatheadImage;
    }

    private void drawChathead(Graphics2D g)
    {
        BufferedImage face = chathead();
        if (face == null) return;
        g.drawImage(face, bounds.x + 4, bounds.y + 2, null);
    }

    private void drawName(Graphics2D g)
    {
        g.setFont(FontManager.getRunescapeBoldFont());
        g.setColor(NAME_COLOR);
        g.drawString("Wise Old Man", bounds.x + TEXT_LEFT, bounds.y + NAME_TOP_OFFSET);
    }

    private void drawActionChoice(Graphics2D g, String self)
    {
        int coins = roster.getCoins(self);
        boolean canAffordGnome = coins >= RunePartyPlugin.WISE_OLD_MAN_GNOME_STEAL_COST;
        boolean anyoneHasGnome = roster.seatedPlayers().stream()
            .anyMatch(p -> !p.rsn.equalsIgnoreCase(self) && p.goldenGnomeCount > 0);
        boolean canOfferGnomeSteal = canAffordGnome && anyoneHasGnome;

        String greeting = canAffordGnome
            ? "Hello! Would you like to steal coins from another player (free)? Or steal a Golden Gnome (73 coins)?"
            : "It appears you don't have enough coins to do that.";

        int textWidth = bounds.width - TEXT_LEFT - TEXT_RIGHT_MARGIN;
        drawWrappedText(g, greeting, bounds.x + TEXT_LEFT, bounds.y + BODY_TOP_OFFSET, textWidth);

        List<String> labels = new ArrayList<>();
        List<Runnable> callbacks = new ArrayList<>();

        labels.add("Steal coins from another player (free)");
        callbacks.add(() -> beginTargetSelect("steal_coins"));

        if (canOfferGnomeSteal)
        {
            labels.add("Steal a Golden Gnome (73 coins)");
            callbacks.add(() -> beginTargetSelect("steal_golden_gnome"));
        }

        labels.add("Never mind");
        callbacks.add(() -> submitFinalChoice("decline", null));

        drawOptionRows(g, labels, callbacks, bounds.y + OPTIONS_TOP_OFFSET);
    }

    private void beginTargetSelect(String action)
    {
        pendingActionChoice = action;
        screen = Screen.TARGET_SELECT;
        targetPage = 0;
    }

    private void drawTargetSelect(Graphics2D g)
    {
        String self = plugin.getLocalRsn();
        boolean gnomeSteal = "steal_golden_gnome".equals(pendingActionChoice);

        List<String> candidates = new ArrayList<>();
        for (RosterReducer.RosterEntry p : roster.seatedPlayers())
        {
            if (self != null && p.rsn.equalsIgnoreCase(self)) continue;
            if (gnomeSteal && p.goldenGnomeCount <= 0) continue;
            candidates.add(p.rsn);
        }

        String prompt = gnomeSteal ? "Steal a Golden Gnome from who?" : "Steal coins from who?";
        int textWidth = bounds.width - TEXT_LEFT - TEXT_RIGHT_MARGIN;
        drawWrappedText(g, prompt, bounds.x + TEXT_LEFT, bounds.y + BODY_TOP_OFFSET, textWidth);

        int pageCount = Math.max(1, (candidates.size() + TARGETS_PER_PAGE - 1) / TARGETS_PER_PAGE);
        int page = Math.min(targetPage, pageCount - 1);
        int start = page * TARGETS_PER_PAGE;
        int end = Math.min(candidates.size(), start + TARGETS_PER_PAGE);

        List<String> labels = new ArrayList<>();
        List<Runnable> callbacks = new ArrayList<>();

        for (int i = start; i < end; i++)
        {
            String targetRsn = candidates.get(i);
            labels.add(targetRsn);
            callbacks.add(() -> submitFinalChoice(pendingActionChoice, targetRsn));
        }

        if (candidates.isEmpty())
        {
            labels.add("(nobody eligible)");
            callbacks.add(() -> { });
        }
        else if (pageCount > 1)
        {
            labels.add((page + 1) + "/" + pageCount + " -- more...");
            int nextPage = (page + 1) % pageCount;
            callbacks.add(() -> targetPage = nextPage);
        }

        labels.add("Back");
        callbacks.add(() -> { screen = Screen.ACTION_CHOICE; pendingActionChoice = null; });

        drawOptionRows(g, labels, callbacks, bounds.y + OPTIONS_TOP_OFFSET);
    }

    private void submitFinalChoice(String action, String target)
    {
        submitted = true;
        plugin.submitWiseOldManChoice(action, target);
    }

    /** Draws one clickable row per (label, callback) pair, top to bottom starting at {@code top},
     * and publishes their hit-boxes/callbacks (as a single atomic array swap each) for the next
     * mouse click to test against -- see clickAdapter's own doc for why this is a fresh array
     * rather than a mutation of the previous frame's. */
    private void drawOptionRows(Graphics2D g, List<String> labels, List<Runnable> callbacks, int top)
    {
        Rectangle[] rects = new Rectangle[labels.size()];
        net.runelite.api.Point mouse = client.getMouseCanvasPosition();

        Font font = FontManager.getRunescapeFont();
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();

        int y = top;
        for (int i = 0; i < labels.size(); i++)
        {
            Rectangle row = new Rectangle(bounds.x + TEXT_LEFT, y - fm.getAscent(),
                bounds.width - TEXT_LEFT - TEXT_RIGHT_MARGIN, OPTION_ROW_HEIGHT);
            rects[i] = row;

            boolean hovered = mouse != null && row.contains(mouse.getX(), mouse.getY());
            g.setColor(hovered ? OPTION_HOVER_COLOR : OPTION_COLOR);
            g.drawString(labels.get(i), row.x, y);

            y += OPTION_ROW_HEIGHT;
        }

        optionBounds = rects;
        optionCallbacks = callbacks.toArray(new Runnable[0]);
    }

    /** Plain greedy word-wrap against {@code maxWidth}, drawn top-down from {@code top} -- good
     * enough for the short, fixed lines this dialogue ever shows (no need for the real client's
     * own bitmap-font metrics the way a pixel-perfect port would). */
    private void drawWrappedText(Graphics2D g, String text, int x, int top, int maxWidth)
    {
        g.setFont(FontManager.getRunescapeFont());
        g.setColor(BODY_COLOR);
        FontMetrics fm = g.getFontMetrics();

        StringBuilder line = new StringBuilder();
        int y = top;
        for (String word : text.split(" "))
        {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (fm.stringWidth(candidate) > maxWidth && line.length() > 0)
            {
                g.drawString(line.toString(), x, y);
                y += fm.getHeight();
                line = new StringBuilder(word);
            }
            else
            {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0) g.drawString(line.toString(), x, y);
    }
}
