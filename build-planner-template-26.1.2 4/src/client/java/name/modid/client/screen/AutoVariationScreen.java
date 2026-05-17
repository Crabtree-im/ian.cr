package name.modid.client.screen;

import name.modid.client.data.PaletteData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Auto-Variation Generator
 *
 * Pick a base palette → see 4 labelled variations as a comparison table.
 * Each row is one variation; columns show Primary / Accent / Roof / Trim
 * block swatches.  Cyan-bordered swatches differ from the base palette.
 */
public class AutoVariationScreen extends Screen {

    private final Screen parent;

    // ── Palette list ──────────────────────────────────────────────────────────
    private final List<String> paletteKeys;
    private       int          baseIndex = 0;

    // ── Variation state ───────────────────────────────────────────────────────
    private final List<Variation> variations = new ArrayList<>();
    private       long            seed       = 0xBEEF_CAFEabcdL;

    // ── Mood names ────────────────────────────────────────────────────────────
    private static final String[] MOODS = {
        "Weathered", "Pristine",    "Ancient",  "Overgrown",
        "Gilded",    "Ashen",       "Frostbitten", "Sunbleached",
        "Shadowed",  "Verdant",     "Rusted",   "Enchanted"
    };

    // ── Recipe descriptions (what each preset swaps) ──────────────────────────
    private static final String[] RECIPE_LABELS = {
        "New accent blocks",
        "New roof blocks",
        "New accent + roof",
        "Full palette restyle"
    };

    // ── Layout constants ──────────────────────────────────────────────────────
    private static final int SWATCH      = 14;   // px per swatch square
    private static final int SWATCH_GAP  = 2;    // px gap between swatches
    private static final int SECTION_SEP = 6;    // px between palette sections
    private static final int LABEL_W     = 74;   // left label column width
    private static final int ROW_H       = 24;   // height of each swatch row

    // Section start X positions (computed once per frame from LABEL_W)
    // Primary(3) | Accent(3) | Roof(2) | Trim(3) — each swatch = SWATCH+SWATCH_GAP
    private int secPrimaryX;
    private int secAccentX;
    private int secRoofX;
    private int secTrimX;

    // ── Inner data ────────────────────────────────────────────────────────────
    record Variation(String mood, String recipe,
                     List<PaletteData.BlockEntry> primary,
                     List<PaletteData.BlockEntry> accent,
                     List<PaletteData.BlockEntry> roof,
                     List<PaletteData.BlockEntry> trim) {}

    // ── Constructor ───────────────────────────────────────────────────────────
    public AutoVariationScreen(Screen parent) {
        super(Component.literal("Auto-Variation Generator"));
        this.parent      = parent;
        this.paletteKeys = PaletteData.getPaletteKeys();
        computeSectionX();
        regenerate();
    }

    private void computeSectionX() {
        int sw = SWATCH + SWATCH_GAP;   // 16
        secPrimaryX = LABEL_W + 4;
        secAccentX  = secPrimaryX + 3 * sw + SECTION_SEP;
        secRoofX    = secAccentX  + 3 * sw + SECTION_SEP;
        secTrimX    = secRoofX    + 2 * sw + SECTION_SEP;
    }

    // ── Init ──────────────────────────────────────────────────────────────────
    @Override
    protected void init() {
        // Palette < > navigation
        this.addRenderableWidget(Button.builder(
            Component.literal("<"),
            btn -> { baseIndex = (baseIndex - 1 + paletteKeys.size()) % paletteKeys.size(); regenerate(); }
        ).bounds(8, 28, 16, 16).build());

        this.addRenderableWidget(Button.builder(
            Component.literal(">"),
            btn -> { baseIndex = (baseIndex + 1) % paletteKeys.size(); regenerate(); }
        ).bounds(this.width - 24, 28, 16, 16).build());

        // Regenerate button — right side
        this.addRenderableWidget(Button.builder(
            Component.literal("\u21bb New Variations"),
            btn -> { seed = System.nanoTime(); regenerate(); }
        ).bounds(this.width / 2 + 10, 28, 110, 16).build());

        // Back
        this.addRenderableWidget(Button.builder(
            Component.literal("\u2190 Back"),
            btn -> onClose()
        ).bounds(8, this.height - 22, 80, 18).build());
    }

    // ── Variation generator ───────────────────────────────────────────────────
    private void regenerate() {
        variations.clear();
        PaletteData.Palette base    = PaletteData.getPalette(paletteKeys.get(baseIndex));
        List<String>        allKeys = PaletteData.getPaletteKeys();
        Random              rng     = new Random(seed + baseIndex * 31L);

        // Four recipes: each swaps a different combination of layers
        // {swapAccent, swapRoof, swapTrim, swapPrimary}
        int[][] recipes = {
            {1, 0, 0, 0},
            {0, 1, 0, 0},
            {1, 1, 0, 0},
            {1, 1, 1, 1},
        };

        String[] moodPool = shuffledMoods(rng);

        for (int v = 0; v < 4; v++) {
            int[] r = recipes[v];
            variations.add(new Variation(
                moodPool[v],
                RECIPE_LABELS[v],
                r[3] == 1 ? blendPrimary(base, allKeys, rng) : base.primary(),
                r[0] == 1 ? pickFrom(allKeys, "accent", rng, base) : base.accent(),
                r[1] == 1 ? pickFrom(allKeys, "roof",   rng, base) : base.roof(),
                r[2] == 1 ? pickFrom(allKeys, "trim",   rng, base) : base.trim()
            ));
        }
    }

    private List<PaletteData.BlockEntry> pickFrom(List<String> keys, String role,
                                                    Random rng, PaletteData.Palette base) {
        for (int attempt = 0; attempt < 8; attempt++) {
            String key = keys.get(rng.nextInt(keys.size()));
            PaletteData.Palette p = PaletteData.getPalette(key);
            List<PaletteData.BlockEntry> list = getRole(p, role);
            if (!list.equals(getRole(base, role))) return list;
        }
        return getRole(PaletteData.getPalette(keys.get(rng.nextInt(keys.size()))), role);
    }

    private List<PaletteData.BlockEntry> blendPrimary(PaletteData.Palette base,
                                                       List<String> keys, Random rng) {
        List<PaletteData.BlockEntry> result = new ArrayList<>(base.primary());
        PaletteData.Palette donor = PaletteData.getPalette(keys.get(rng.nextInt(keys.size())));
        if (!donor.primary().isEmpty()) result.set(result.size() - 1, donor.primary().get(0));
        return result;
    }

    private List<PaletteData.BlockEntry> getRole(PaletteData.Palette p, String role) {
        return switch (role) {
            case "accent" -> p.accent();
            case "roof"   -> p.roof();
            case "trim"   -> p.trim();
            default       -> p.primary();
        };
    }

    private String[] shuffledMoods(Random rng) {
        String[] pool = MOODS.clone();
        for (int i = pool.length - 1; i >= 1; i--) {
            int j = rng.nextInt(i + 1);
            String tmp = pool[i]; pool[i] = pool[j]; pool[j] = tmp;
        }
        return pool;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────
    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        PaletteData.Palette base = PaletteData.getPalette(paletteKeys.get(baseIndex));
        int cx = this.width / 2;

        // ── Title bar ──────────────────────────────────────────────────────
        g.centeredText(this.font, "Auto-Variation Generator", cx, 8, 0xFFD700);
        g.centeredText(this.font, "Cyan border = block changed from base", cx, 18, 0x555555);

        // ── Palette picker row ─────────────────────────────────────────────
        g.centeredText(this.font, base.name(),  cx, 30, 0xFFFFFF);
        g.centeredText(this.font, base.style(), cx, 40, 0x88CCFF);

        // ── Section column headers ─────────────────────────────────────────
        int headerY = 56;
        g.fill(4, headerY - 2, this.width - 4, headerY + 10, 0x55000000);
        g.text(this.font, "Variation",  4,           headerY, 0x888888);
        g.text(this.font, "Primary",    secPrimaryX, headerY, 0xFFD700);
        g.text(this.font, "Accent",     secAccentX,  headerY, 0xFFD700);
        g.text(this.font, "Roof",       secRoofX,    headerY, 0xFFD700);
        g.text(this.font, "Trim",       secTrimX,    headerY, 0xFFD700);

        // ── Base palette row ───────────────────────────────────────────────
        int baseY = 70;
        g.fill(4, baseY - 1, this.width - 4, baseY + ROW_H, 0x66111111);
        g.fill(4, baseY - 1, this.width - 4, baseY, 0x44FFD700);
        g.text(this.font, "Base", 6, baseY + 5, 0xAAAAAA);
        drawSwatchSection(g, base.primary(), null, secPrimaryX, baseY + 3);
        drawSwatchSection(g, base.accent(),  null, secAccentX,  baseY + 3);
        drawSwatchSection(g, base.roof(),    null, secRoofX,    baseY + 3);
        drawSwatchSection(g, base.trim(),    null, secTrimX,    baseY + 3);

        // ── Divider ────────────────────────────────────────────────────────
        int divY = baseY + ROW_H + 2;
        g.fill(4, divY, this.width - 4, divY + 1, 0x44FFD700);

        // ── Variation rows ─────────────────────────────────────────────────
        int rowY = divY + 4;
        for (int v = 0; v < variations.size(); v++) {
            Variation var = variations.get(v);
            boolean alt = (v % 2 == 1);
            g.fill(4, rowY, this.width - 4, rowY + ROW_H, alt ? 0x44111122 : 0x44111111);

            // Label column: mood name (gold) + recipe (grey)
            g.text(this.font, var.mood(),   6, rowY + 3,  0xFFD700);
            g.text(this.font, var.recipe(), 6, rowY + 13, 0x555555);

            // Swatch columns — pass base sections so changed blocks get cyan border
            drawSwatchSection(g, var.primary(), base.primary(), secPrimaryX, rowY + 5);
            drawSwatchSection(g, var.accent(),  base.accent(),  secAccentX,  rowY + 5);
            drawSwatchSection(g, var.roof(),    base.roof(),    secRoofX,    rowY + 5);
            drawSwatchSection(g, var.trim(),    base.trim(),    secTrimX,    rowY + 5);

            rowY += ROW_H + 2;
        }

        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    /**
     * Draw a row of coloured swatches at (startX, y).
     * If baseSect is non-null, swatches whose colour differs from the
     * corresponding base entry receive a cyan border.
     */
    private void drawSwatchSection(GuiGraphicsExtractor g,
                                    List<PaletteData.BlockEntry> sect,
                                    List<PaletteData.BlockEntry> baseSect,
                                    int startX, int y) {
        int sw = SWATCH;
        int step = sw + SWATCH_GAP;
        for (int i = 0; i < sect.size(); i++) {
            PaletteData.BlockEntry e = sect.get(i);
            int sx = startX + i * step;
            g.fill(sx, y, sx + sw, y + sw, 0xFF000000 | e.color());

            boolean changed = baseSect != null
                && (i >= baseSect.size() || e.color() != baseSect.get(i).color());

            if (changed) {
                g.fill(sx,          y,          sx + sw,     y + 1,      0xFF00FFFF);
                g.fill(sx,          y,          sx + 1,      y + sw,     0xFF00FFFF);
                g.fill(sx + sw - 1, y,          sx + sw,     y + sw,     0xFF00FFFF);
                g.fill(sx,          y + sw - 1, sx + sw,     y + sw,     0xFF00FFFF);
            } else {
                g.fill(sx, y, sx + sw, y + 1, 0x33FFFFFF);
                g.fill(sx, y, sx + 1, y + sw, 0x33FFFFFF);
            }
        }
    }

    // ── Close ─────────────────────────────────────────────────────────────────
    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
