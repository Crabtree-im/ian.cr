package name.modid.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SilhouetteGeneratorScreen extends Screen {

    private final Screen parent;

    // ── Archetype data ────────────────────────────────────────────────────────
    private static final String[] STYLES = {
        "Castle", "Tower", "Cottage", "Cathedral", "Nordic Hall",
        "Pagoda", "Lighthouse", "Gatehouse", "Manor", "Wizard Tower"
    };

    private static final String[] STYLE_NOTES = {
        "A fortified stronghold with corner towers, a central keep, and crenellated battlements. "
            + "Use stone bricks, deepslate, and iron for a classic look.",
        "A tall, narrow defensive structure for hilltops and cliff edges. "
            + "Taper each floor slightly inward to add visual interest.",
        "A low, warm dwelling with a steeply pitched roof. "
            + "Combine oak logs, stone, and warm-toned terracotta for an inviting feel.",
        "A grand religious structure with soaring twin spires, pointed arches, and stained-glass windows. "
            + "Dark stone and purple tones reinforce the gothic silhouette.",
        "A long great hall with a steep roof, common in Viking-inspired builds. "
            + "Spruce logs, stone, and thatching create the right atmosphere.",
        "A ceremonial tiered tower — each floor narrower than the last. "
            + "Bamboo blocks, polished stone, and paper-lantern details work beautifully.",
        "A coastal beacon with a wide stone base tapering to a tall shaft and a glowing lamp room. "
            + "Use prismarine and sea lanterns for coastal authenticity.",
        "A fortified city entrance flanked by twin towers and a broad arched passage. "
            + "Pair rough stone with iron-barred windows and a working portcullis.",
        "A symmetrical grand estate with matching wings, a pillared entrance, and formal garden wings. "
            + "Smooth quartz, white concrete, and stripped logs suit the aesthetic.",
        "An impossibly slender tower reaching for the sky with a conical hat. "
            + "Magenta glass, crying obsidian, and purpur blocks complete the look."
    };

    private static final int[] STYLE_COLORS = {
        0xFF7A8899,  // Castle      - slate blue-grey
        0xFF886699,  // Tower       - dusty purple
        0xFFAA8855,  // Cottage     - warm oak
        0xFF664477,  // Cathedral   - deep violet
        0xFF7799AA,  // Nordic Hall - ice blue
        0xFF998855,  // Pagoda      - earthy gold
        0xFF5588AA,  // Lighthouse  - sea blue
        0xFF779966,  // Gatehouse   - mossy stone
        0xFFCC9966,  // Manor       - sandstone
        0xFF5544AA   // Wizard Tower - arcane violet
    };

    // ── Drawing constants ─────────────────────────────────────────────────────
    private static final int COLS   = 40;   // number of columns in profile
    private static final int MAX_H  = 60;   // max height units
    private static final int COL_W  = 4;    // px per column
    private static final int UNIT_H = 2;    // px per height unit
    private static final int DRAW_W = COLS * COL_W;    // 160px
    private static final int DRAW_H = MAX_H * UNIT_H;  // 120px

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int LEFT_X  = 8;
    private static final int LEFT_W  = 130;
    private static final int RIGHT_X = 146;
    private static final int TOP_Y   = 44;

    // ── State ─────────────────────────────────────────────────────────────────
    private int selectedStyle = 0;
    private int randomSeed    = 12345;
    private int[] heights     = new int[COLS];

    public SilhouetteGeneratorScreen(Screen parent) {
        super(Component.literal("Silhouette Generator"));
        this.parent = parent;
        regenerate();
    }

    // ── Widget init ───────────────────────────────────────────────────────────

    @Override
    protected void init() {
        int btnH   = 16;
        int btnGap = 3;
        int btnY   = TOP_Y + 14;

        for (int i = 0; i < STYLES.length; i++) {
            final int idx = i;
            this.addRenderableWidget(Button.builder(
                Component.literal(STYLES[i]),
                btn -> { selectedStyle = idx; regenerate(); }
            ).bounds(LEFT_X, btnY + i * (btnH + btnGap), LEFT_W, btnH).build());
        }

        int afterList = btnY + STYLES.length * (btnH + btnGap) + 6;

        this.addRenderableWidget(Button.builder(
            Component.literal("\u21bb Randomize"),
            btn -> { randomSeed = (int)(Math.random() * 999983); regenerate(); }
        ).bounds(LEFT_X, afterList, LEFT_W, 18).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("\u2190 Back"),
            btn -> onClose()
        ).bounds(LEFT_X, this.height - 22, 80, 18).build());
    }

    private void regenerate() {
        heights = buildProfile(selectedStyle, randomSeed);
    }

    // ── Profile builders ──────────────────────────────────────────────────────

    private int[] buildProfile(int style, int seed) {
        int[] h = new int[COLS];
        Random rng = new Random(seed);
        switch (style) {
            case 0 -> castle(h, rng);
            case 1 -> tower(h, rng);
            case 2 -> cottage(h, rng);
            case 3 -> cathedral(h, rng);
            case 4 -> nordicHall(h, rng);
            case 5 -> pagoda(h, rng);
            case 6 -> lighthouse(h, rng);
            case 7 -> gatehouse(h, rng);
            case 8 -> manor(h, rng);
            case 9 -> wizardTower(h, rng);
        }
        applyNoise(h, rng, 2);
        return h;
    }

    /** Set all columns in [x1..x2] to at least `height`. */
    private void fill(int[] h, int x1, int x2, int height) {
        for (int i = Math.max(0, x1); i <= Math.min(COLS - 1, x2); i++)
            h[i] = Math.max(h[i], height);
    }

    /** Alternate crenellations on [x1..x2] at a given base height. */
    private void battlements(int[] h, int x1, int x2, int base, int notch, Random rng) {
        int period = 2 + rng.nextInt(2);
        for (int i = x1; i <= x2; i++)
            h[i] = Math.max(h[i], ((i - x1) % (period * 2) < period) ? base : base - notch);
    }

    private void applyNoise(int[] h, Random rng, int range) {
        for (int i = 0; i < h.length; i++)
            h[i] = Math.max(2, h[i] + rng.nextInt(range * 2 + 1) - range);
    }

    // ── Archetype definitions ─────────────────────────────────────────────────

    private void castle(int[] h, Random rng) {
        int towerH = 38 + rng.nextInt(10);
        int wallH  = 18 + rng.nextInt(6);
        int keepH  = 30 + rng.nextInt(8);
        fill(h, 0, 6, towerH);
        fill(h, 33, 39, towerH);
        fill(h, 7, 32, wallH);
        fill(h, 16, 23, keepH);
        battlements(h, 0, 6, towerH + 3, 3, rng);
        battlements(h, 33, 39, towerH + 3, 3, rng);
        battlements(h, 7, 32, wallH + 2, 2, rng);
    }

    private void tower(int[] h, Random rng) {
        int body = 50 + rng.nextInt(8);
        fill(h, 10, 29, 14 + rng.nextInt(5));
        fill(h, 15, 24, body);
        battlements(h, 15, 24, body + 3, 3, rng);
    }

    private void cottage(int[] h, Random rng) {
        int walls  = 12 + rng.nextInt(5);
        int peak   = walls + 14 + rng.nextInt(8);
        int center = 18 + rng.nextInt(4);
        fill(h, 4, 35, walls);
        for (int i = 4; i <= 35; i++) {
            int dist  = Math.abs(i - center);
            h[i] = Math.max(h[i], Math.max(walls, peak - dist * 2));
        }
        // Chimney
        int cx = center + 5 + rng.nextInt(4);
        fill(h, cx, cx + 1, peak - 4 + rng.nextInt(4));
    }

    private void cathedral(int[] h, Random rng) {
        int spire = 52 + rng.nextInt(8);
        int nave  = 38 + rng.nextInt(6);
        int aisle = 20 + rng.nextInt(5);
        fill(h, 5, 9,   spire);
        fill(h, 30, 34, spire);
        h[7]  = Math.min(MAX_H - 1, spire + 5 + rng.nextInt(3));
        h[32] = Math.min(MAX_H - 1, spire + 5 + rng.nextInt(3));
        fill(h, 10, 29, aisle);
        fill(h, 14, 25, nave);
    }

    private void nordicHall(int[] h, Random rng) {
        int walls  = 14 + rng.nextInt(5);
        int peak   = walls + 18 + rng.nextInt(8);
        int center = 19;
        fill(h, 2, 37, walls);
        for (int i = 2; i <= 37; i++) {
            int dist = Math.abs(i - center);
            h[i] = Math.max(h[i], Math.max(walls, peak - dist * 2));
        }
        h[0] = h[2]; h[1] = h[2]; h[38] = h[37]; h[39] = h[37];
    }

    private void pagoda(int[] h, Random rng) {
        // Tier 1
        int t1base = 8 + rng.nextInt(4);
        fill(h, 4, 35, t1base);
        int t1top = t1base + 8 + rng.nextInt(4);
        for (int i = 4; i <= 35; i++) h[i] = Math.max(h[i], t1top - Math.abs(i - 19) / 3);
        // Tier 2
        int t2base = t1top + 3;
        fill(h, 9, 30, t2base);
        int t2top = t2base + 8;
        for (int i = 9; i <= 30; i++) h[i] = Math.max(h[i], t2top - Math.abs(i - 19) / 3);
        // Tier 3
        int t3base = t2top + 3;
        fill(h, 14, 25, t3base);
        int t3top = t3base + 7;
        for (int i = 14; i <= 25; i++) h[i] = Math.max(h[i], t3top - Math.abs(i - 19) / 3);
        // Finial
        fill(h, 18, 21, Math.min(MAX_H - 1, t3top + 5 + rng.nextInt(3)));
    }

    private void lighthouse(int[] h, Random rng) {
        int baseH  = 14 + rng.nextInt(4);
        int shaftH = 46 + rng.nextInt(8);
        fill(h, 8, 31, baseH);
        fill(h, 16, 23, shaftH);
        fill(h, 14, 25, shaftH + 4);
        h[19] = Math.min(MAX_H - 1, shaftH + 8 + rng.nextInt(3));
        h[20] = h[19];
    }

    private void gatehouse(int[] h, Random rng) {
        int towerH = 36 + rng.nextInt(10);
        int archH  = 18 + rng.nextInt(6);
        fill(h, 0, 10, towerH);
        fill(h, 29, 39, towerH);
        fill(h, 11, 28, archH);
        battlements(h, 0, 10, towerH + 3, 3, rng);
        battlements(h, 29, 39, towerH + 3, 3, rng);
        battlements(h, 11, 28, archH + 2, 2, rng);
    }

    private void manor(int[] h, Random rng) {
        int wingH = 20 + rng.nextInt(5);
        int bodyH = 26 + rng.nextInt(6);
        int pedH  = bodyH + 8 + rng.nextInt(5);
        fill(h, 0, 7, wingH);
        fill(h, 32, 39, wingH);
        fill(h, 8, 31, bodyH);
        for (int i = 15; i <= 24; i++) h[i] = Math.max(h[i], pedH - Math.abs(i - 19));
        fill(h, 3, 4,   wingH + 6 + rng.nextInt(4));
        fill(h, 35, 36, wingH + 6 + rng.nextInt(4));
    }

    private void wizardTower(int[] h, Random rng) {
        int base   = 14 + rng.nextInt(4);
        int towerH = 54 + rng.nextInt(6);
        fill(h, 8, 31, base);
        fill(h, 17, 22, towerH);
        // Balcony ring
        fill(h, 15, 24, towerH / 2 + 3);
        h[15] = towerH / 2; h[24] = towerH / 2;
        // Conical hat
        for (int i = 16; i <= 23; i++)
            h[i] = Math.max(h[i], Math.min(MAX_H - 1, towerH + 6 - Math.abs(i - 19) * 3));
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.centeredText(this.font, "Silhouette Generator", this.width / 2, 8, 0xFFD700);
        g.centeredText(this.font, "Form before function \u2014 find your building's shape", this.width / 2, 19, 0x555555);

        // Left panel
        g.fill(LEFT_X, TOP_Y, LEFT_X + LEFT_W, this.height - 26, 0x88000000);
        g.fill(LEFT_X, TOP_Y, LEFT_X + LEFT_W, TOP_Y + 1, 0x44FFFFFF);
        g.text(this.font, "Building Type", LEFT_X + 4, TOP_Y + 4, 0xFFD700);

        // Highlight active style
        int btnH = 16, btnGap = 3;
        int selY  = TOP_Y + 14 + selectedStyle * (btnH + btnGap);
        g.fill(LEFT_X, selY, LEFT_X + LEFT_W, selY + btnH, 0x33FFD700);

        // Right panel
        int rx    = RIGHT_X;
        int rw    = this.width - rx - 8;
        int rpTop = TOP_Y;
        int rpBot = this.height - 26;
        g.fill(rx, rpTop, rx + rw, rpBot, 0x88000000);
        g.fill(rx, rpTop, rx + rw, rpTop + 1, 0x44FFFFFF);
        g.text(this.font, STYLES[selectedStyle], rx + 4, rpTop + 4, STYLE_COLORS[selectedStyle]);

        // ── Silhouette drawing area ────────────────────────────────────────────
        int drawX      = rx + (rw - DRAW_W) / 2;
        int drawBottom = rpTop + 28 + DRAW_H;

        // Sky background
        g.fill(drawX, drawBottom - DRAW_H, drawX + DRAW_W, drawBottom, 0xFF0D1B2A);

        // Faint horizon glow
        g.fill(drawX, drawBottom - 18, drawX + DRAW_W, drawBottom - 14, 0xFF1A2E1E);
        g.fill(drawX, drawBottom - 14, drawX + DRAW_W, drawBottom - 10, 0xFF152813);

        // Ground strip
        g.fill(drawX, drawBottom, drawX + DRAW_W, drawBottom + 3, 0xFF3A4A2A);

        // Silhouette columns
        int silColor = STYLE_COLORS[selectedStyle] | 0xFF000000;
        // Slightly lighter top edge for depth
        int edgeColor = blendColor(silColor, 0xFFFFFFFF, 0.25f);

        for (int col = 0; col < COLS; col++) {
            int colH  = Math.min(heights[col], MAX_H) * UNIT_H;
            int colX  = drawX + col * COL_W;
            int colY  = drawBottom - colH;
            // Body
            g.fill(colX, colY + 1, colX + COL_W, drawBottom, silColor);
            // Top edge highlight
            g.fill(colX, colY, colX + COL_W, colY + 1, edgeColor);
        }

        // ── Notes ─────────────────────────────────────────────────────────────
        int noteY = drawBottom + 8;
        g.fill(rx + 4, noteY - 3, rx + rw - 4, noteY - 2, 0x22FFFFFF);

        for (String line : wrapText(STYLE_NOTES[selectedStyle], rw - 14)) {
            if (noteY + 10 > rpBot - 4) break;
            g.text(this.font, line, rx + 6, noteY, 0x999999);
            noteY += 10;
        }

        // Seed display (small, bottom-right of draw area)
        String seedStr = "seed: " + randomSeed;
        g.text(this.font, seedStr,
            drawX + DRAW_W - this.font.width(seedStr),
            drawBottom + 1, 0x333333);

        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    /** Linear blend: 0.0 = full a, 1.0 = full b */
    private int blendColor(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = (int)(ar + (br - ar) * t);
        int gr2 = (int)(ag + (bg - ag) * t);
        int bl = (int)(ab + (bb - ab) * t);
        return 0xFF000000 | (r << 16) | (gr2 << 8) | bl;
    }

    private List<String> wrapText(String text, int maxPx) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            String test = cur.isEmpty() ? w : cur + " " + w;
            if (this.font.width(test) <= maxPx) {
                cur = new StringBuilder(test);
            } else {
                if (!cur.isEmpty()) lines.add(cur.toString());
                cur = new StringBuilder(w);
            }
        }
        if (!cur.isEmpty()) lines.add(cur.toString());
        return lines;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        return super.mouseClicked(event, consumed);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
