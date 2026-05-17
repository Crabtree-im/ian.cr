package name.modid.client.screen;

import name.modid.client.data.BlueprintData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class BlueprintLibraryScreen extends Screen {

    private final Screen parent;

    private String selectedCategory;
    private List<BlueprintData.Blueprint> entries;
    private int selectedIndex = 0;
    private int listScroll = 0;
    private int detailScroll = 0;

    // Layout
    private static final int LIST_X    = 8;
    private static final int LIST_W    = 130;
    private static final int DETAIL_X  = 146;
    private static final int PANEL_TOP = 44;
    private static final int ENTRY_H   = 26;

    // Difficulty colors
    private static final int COL_BEGINNER     = 0xFF44BB44;
    private static final int COL_INTERMEDIATE = 0xFFDDAA33;
    private static final int COL_ADVANCED     = 0xFFCC4444;

    public BlueprintLibraryScreen(Screen parent) {
        super(Component.literal("Blueprint Library"));
        this.parent = parent;
        this.selectedCategory = BlueprintData.CATEGORIES.get(0);
        this.entries = BlueprintData.getCategory(selectedCategory);
    }

    @Override
    protected void init() {
        // Category tab buttons
        int catBtnW = (this.width - 16) / BlueprintData.CATEGORIES.size() - 2;
        for (int i = 0; i < BlueprintData.CATEGORIES.size(); i++) {
            final String cat = BlueprintData.CATEGORIES.get(i);
            int bx = 8 + i * (catBtnW + 2);
            this.addRenderableWidget(Button.builder(
                Component.literal(cat),
                btn -> selectCategory(cat)
            ).bounds(bx, 22, catBtnW, 18).build());
        }

        int listBottom = this.height - 28;

        // List scroll
        this.addRenderableWidget(Button.builder(Component.literal("▲"),
            btn -> { if (listScroll > 0) listScroll--; }
        ).bounds(LIST_X, listBottom - 20, LIST_W / 2 - 2, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("▼"),
            btn -> { if (listScroll < entries.size() - visibleListRows()) listScroll++; }
        ).bounds(LIST_X + LIST_W / 2 + 2, listBottom - 20, LIST_W / 2 - 2, 18).build());

        // Detail scroll
        int detailW = this.width - DETAIL_X - 8;
        this.addRenderableWidget(Button.builder(Component.literal("▲"),
            btn -> { if (detailScroll > 0) detailScroll--; }
        ).bounds(DETAIL_X + detailW - 22, listBottom - 20, 20, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("▼"),
            btn -> { if (detailScroll < maxDetailScroll()) detailScroll++; }
        ).bounds(DETAIL_X + detailW - 22, listBottom - 42, 20, 18).build());

        // Back
        this.addRenderableWidget(Button.builder(Component.literal("← Back"),
            btn -> this.onClose()
        ).bounds(8, this.height - 22, 80, 18).build());
    }

    private void selectCategory(String cat) {
        selectedCategory = cat;
        entries = BlueprintData.getCategory(cat);
        selectedIndex = 0;
        listScroll = 0;
        detailScroll = 0;
    }

    private int visibleListRows() {
        return Math.max(1, (this.height - PANEL_TOP - 50) / ENTRY_H);
    }

    private int maxDetailScroll() {
        if (entries.isEmpty()) return 0;
        BlueprintData.Blueprint bp = entries.get(selectedIndex);
        // each step line ~11px, header ~80px
        int totalLines = 8 + bp.steps().size() * 2;
        int visible = (this.height - PANEL_TOP - 50) / 11;
        return Math.max(0, totalLines - visible);
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int listBottom = this.height - 28;
        int listTop    = PANEL_TOP;
        int listAreaBottom = listBottom - 22;
        int detailW    = this.width - DETAIL_X - 8;
        int detailBottom = listAreaBottom;

        // Header
        g.centeredText(this.font, "Blueprint Library", this.width / 2, 8, 0xFFD700);

        // ── LEFT: Entry list ──────────────────────────────────────────────────
        g.fill(LIST_X, listTop, LIST_X + LIST_W, listAreaBottom, 0x88000000);
        g.fill(LIST_X, listTop, LIST_X + LIST_W, listTop + 1, 0x44FFFFFF);

        int visRows = visibleListRows();
        for (int i = 0; i < visRows; i++) {
            int idx = i + listScroll;
            if (idx >= entries.size()) break;
            BlueprintData.Blueprint bp = entries.get(idx);
            int ey = listTop + i * ENTRY_H;
            boolean selected = idx == selectedIndex;
            boolean hovered = mouseX >= LIST_X && mouseX < LIST_X + LIST_W
                && mouseY >= ey && mouseY < ey + ENTRY_H;

            if (selected)      g.fill(LIST_X + 1, ey, LIST_X + LIST_W - 1, ey + ENTRY_H, 0x88336699);
            else if (hovered)  g.fill(LIST_X + 1, ey, LIST_X + LIST_W - 1, ey + ENTRY_H, 0x33336699);

            // Blueprint name
            String name = bp.name().length() > 16 ? bp.name().substring(0, 15) + "…" : bp.name();
            g.text(this.font, name, LIST_X + 4, ey + 4, selected ? 0xFFFFFF : 0xCCCCCC);

            // Dimensions + difficulty badge
            String dims = bp.dimensions();
            if (dims.length() > 14) dims = dims.substring(0, 13) + "…";
            g.text(this.font, dims, LIST_X + 4, ey + 14, 0x666666);

            int diffColor = difficultyColor(bp.difficulty());
            int dotX = LIST_X + LIST_W - 8;
            g.fill(dotX, ey + 9, dotX + 6, ey + 15, diffColor);
        }

        if (entries.isEmpty()) {
            g.centeredText(this.font, "No blueprints.", LIST_X + LIST_W / 2, listTop + 30, 0x444444);
        }

        // ── RIGHT: Detail panel ───────────────────────────────────────────────
        g.fill(DETAIL_X, listTop, DETAIL_X + detailW, detailBottom, 0x88000000);
        g.fill(DETAIL_X, listTop, DETAIL_X + detailW, listTop + 1, 0x44FFFFFF);

        if (!entries.isEmpty()) {
            BlueprintData.Blueprint bp = entries.get(selectedIndex);
            int dx = DETAIL_X + 6;
            int contentW = detailW - 34; // leave room for scroll buttons on right
            int dy = listTop + 6 - detailScroll * 11;

            // Category badge
            g.fill(dx, dy, dx + this.font.width(bp.category()) + 8, dy + 11, 0x66334455);
            g.text(this.font, bp.category(), dx + 4, dy + 1, 0x88CCFF);
            dy += 14;

            // Name
            g.text(this.font, bp.name(), dx, dy, 0xFFFFFF);
            dy += 13;

            // Stat row
            int diffColor = difficultyColor(bp.difficulty());
            g.text(this.font, bp.difficulty(), dx, dy, diffColor);
            int afterDiff = dx + this.font.width(bp.difficulty()) + 8;
            g.text(this.font, "  |  " + bp.dimensions(), afterDiff, dy, 0x666666);
            if (bp.blockEstimate() > 0) {
                g.text(this.font, "  ~" + bp.blockEstimate() + " blocks", afterDiff + this.font.width("  |  " + bp.dimensions()), dy, 0x555566);
            }
            dy += 13;

            // Material
            g.text(this.font, "Material: " + bp.material(), dx, dy, 0xAABBAA);
            dy += 13;

            // Divider
            g.fill(dx, dy, DETAIL_X + contentW, dy + 1, 0x33FFFFFF);
            dy += 6;

            // Description wrapped
            List<String> descLines = wrap(bp.description(), contentW);
            for (String line : descLines) {
                if (dy > detailBottom - 6) break;
                if (dy >= listTop + 4) g.text(this.font, line, dx, dy, 0xBBBBBB);
                dy += 11;
            }
            dy += 6;

            // Steps header
            if (dy >= listTop + 4 && dy < detailBottom - 6) {
                g.text(this.font, "Build Steps:", dx, dy, 0xFFD700);
            }
            dy += 13;

            for (int s = 0; s < bp.steps().size(); s++) {
                List<String> stepLines = wrap((s + 1) + ". " + bp.steps().get(s), contentW);
                for (int ln = 0; ln < stepLines.size(); ln++) {
                    if (dy > detailBottom - 6) break;
                    if (dy >= listTop + 4) {
                        int textColor = ln == 0 ? 0xCCEECC : 0x889988;
                        g.text(this.font, stepLines.get(ln), ln == 0 ? dx : dx + 6, dy, textColor);
                    }
                    dy += 11;
                }
                dy += 2; // gap between steps
            }

            // Scroll indicator
            if (detailScroll > 0 || maxDetailScroll() > 0) {
                String scrollHint = "▲▼ scroll";
                g.text(this.font, scrollHint, DETAIL_X + detailW - 8 - this.font.width(scrollHint), listTop + 2, 0x444444);
            }
        } else {
            g.centeredText(this.font, "Select a blueprint", DETAIL_X + detailW / 2, listTop + 40, 0x444444);
        }

        // Legend
        g.fill(LIST_X, listBottom - 4, LIST_X + 6, listBottom + 2, COL_BEGINNER);
        g.text(this.font, "Beginner", LIST_X + 8, listBottom - 4, 0x555555);
        g.fill(LIST_X + 60, listBottom - 4, LIST_X + 66, listBottom + 2, COL_INTERMEDIATE);
        g.text(this.font, "Intermediate", LIST_X + 68, listBottom - 4, 0x555555);

        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        double x = event.x();
        double y = event.y();
        int listTop = PANEL_TOP;
        int listAreaBottom = this.height - 50;

        if (x >= LIST_X && x < LIST_X + LIST_W
            && y >= listTop && y < listAreaBottom) {
            int row = (int)((y - listTop) / ENTRY_H) + listScroll;
            if (row >= 0 && row < entries.size()) {
                selectedIndex = row;
                detailScroll = 0;
                return true;
            }
        }
        return super.mouseClicked(event, consumed);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= LIST_X && mouseX < LIST_X + LIST_W) {
            if (scrollY < 0) { if (listScroll < entries.size() - visibleListRows()) listScroll++; }
            else             { if (listScroll > 0) listScroll--; }
        } else if (mouseX >= DETAIL_X) {
            if (scrollY < 0) { if (detailScroll < maxDetailScroll()) detailScroll++; }
            else             { if (detailScroll > 0) detailScroll--; }
        }
        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int difficultyColor(String diff) {
        return switch (diff) {
            case "Beginner"     -> COL_BEGINNER;
            case "Intermediate" -> COL_INTERMEDIATE;
            case "Advanced"     -> COL_ADVANCED;
            default             -> 0x888888;
        };
    }

    private List<String> wrap(String text, int maxW) {
        List<String> result = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (this.font.width(candidate) > maxW) {
                if (!line.isEmpty()) { result.add(line.toString()); line = new StringBuilder(word); }
                else result.add(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) result.add(line.toString());
        return result;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
