package name.modid.client.screen;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.*;

public class FloorplanDesignerScreen extends Screen {

    private final Screen parent;

    // ── Brush types ───────────────────────────────────────────────────────────
    static final int EMPTY = 0, FLOOR = 1, WALL = 2, DOOR = 3,
                     WINDOW = 4, STAIRS = 5, WATER = 6, GARDEN = 7;
    static final int BRUSH_COUNT = 8;

    static final String[] BRUSH_LABELS = {
        "Erase", "Floor", "Wall", "Door", "Window", "Stairs", "Water", "Garden"
    };

    // Mutable per-brush colors (player can change these)
    int[] brushColors = {
        0xFF111111, // EMPTY
        0xFFBB8855, // FLOOR
        0xFF445566, // WALL
        0xFF774422, // DOOR
        0xFF88BBDD, // WINDOW
        0xFFDDAA33, // STAIRS
        0xFF3366AA, // WATER
        0xFF336633, // GARDEN
    };

    // ── Color picker ──────────────────────────────────────────────────────────
    private int colorPickerForBrush = -1; // -1 = closed
    private static final int PICK_COLS = 8;
    private static final int PICK_ROWS = 6;
    private static final int PICK_SWATCH = 14;

    static final int[] COLOR_PALETTE = {
        // Whites & greys
        0xFFFFFFFF, 0xFFDDDDDD, 0xFFBBBBBB, 0xFF999999, 0xFF777777, 0xFF555555, 0xFF333333, 0xFF111111,
        // Browns & tans
        0xFFEDD9A3, 0xFFCC9955, 0xFFAA6633, 0xFF7A4422, 0xFF552211, 0xFFBBAA88, 0xFF998866, 0xFF776644,
        // Reds & pinks
        0xFFFF4444, 0xFFCC2222, 0xFF881111, 0xFFFFAAAA, 0xFFFF88AA, 0xFFCC4488, 0xFF882266, 0xFF441133,
        // Oranges & yellows
        0xFFFFAA00, 0xFFFF7700, 0xFFDD5500, 0xFFFFEE00, 0xFFDDCC00, 0xFFBBAA00, 0xFFFFDD88, 0xFFEEBB44,
        // Greens
        0xFF44FF44, 0xFF22AA22, 0xFF115511, 0xFF88FF88, 0xFF44BB44, 0xFF227722, 0xFF99CC55, 0xFF558833,
        // Blues & cyans
        0xFF4488FF, 0xFF2255CC, 0xFF113388, 0xFF44CCFF, 0xFF22AACC, 0xFF115588, 0xFF88BBDD, 0xFF224466,
    };

    // ── Grid state ────────────────────────────────────────────────────────────
    private int gridW = 20;
    private int gridH = 20;
    private int[][] grid = new int[gridH][gridW];
    private int selectedBrush = FLOOR;

    // ── Layout constants ──────────────────────────────────────────────────────
    private static final int TOOLBAR_X = 8;
    private static final int TOOLBAR_W = 82;
    private static final int TOOLBAR_BRUSH_H = 22;
    private static final int GRID_MARGIN_LEFT = TOOLBAR_X + TOOLBAR_W + 8;
    private static final int GRID_TOP = 32;

    // ── Computed layout (updated in recomputeLayout) ──────────────────────────
    private int cellSize = 10;
    private int gridPixelLeft;
    private int gridPixelTop;

    private static final Gson GSON = new GsonBuilder().create();

    public FloorplanDesignerScreen(Screen parent) {
        super(Component.literal("Floorplan Designer"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        recomputeLayout();

        // ── Grid size controls ────────────────────────────────────────────────
        int ctrlY = 8;
        int cx = GRID_MARGIN_LEFT;

        this.addRenderableWidget(Button.builder(Component.literal("W-"),
            btn -> { if (gridW > 5) { gridW--; resizeGrid(); recomputeLayout(); } }
        ).bounds(cx, ctrlY, 22, 16).build()); cx += 24;

        this.addRenderableWidget(Button.builder(Component.literal("W+"),
            btn -> { if (gridW < 60) { gridW++; resizeGrid(); recomputeLayout(); } }
        ).bounds(cx, ctrlY, 22, 16).build()); cx += 28;

        this.addRenderableWidget(Button.builder(Component.literal("H-"),
            btn -> { if (gridH > 5) { gridH--; resizeGrid(); recomputeLayout(); } }
        ).bounds(cx, ctrlY, 22, 16).build()); cx += 24;

        this.addRenderableWidget(Button.builder(Component.literal("H+"),
            btn -> { if (gridH < 60) { gridH++; resizeGrid(); recomputeLayout(); } }
        ).bounds(cx, ctrlY, 22, 16).build()); cx += 28;

        this.addRenderableWidget(Button.builder(Component.literal("Clear"),
            btn -> clearGrid()
        ).bounds(cx, ctrlY, 38, 16).build()); cx += 42;

        this.addRenderableWidget(Button.builder(Component.literal("Save"),
            btn -> saveFloorplan()
        ).bounds(cx, ctrlY, 34, 16).build()); cx += 38;

        this.addRenderableWidget(Button.builder(Component.literal("Load"),
            btn -> loadFloorplan()
        ).bounds(cx, ctrlY, 34, 16).build());

        // ── Back button ───────────────────────────────────────────────────────
        this.addRenderableWidget(Button.builder(Component.literal("← Back"),
            btn -> this.onClose()
        ).bounds(8, this.height - 22, 80, 18).build());

        loadFloorplan(); // auto-load saved state
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private void recomputeLayout() {
        int availW = this.width - GRID_MARGIN_LEFT - 10;
        int availH = this.height - GRID_TOP - 28;
        int csW = Math.max(4, availW / gridW);
        int csH = Math.max(4, availH / gridH);
        cellSize = Math.min(csW, csH);
        int totalGridW = cellSize * gridW;
        int totalGridH = cellSize * gridH;
        gridPixelLeft = GRID_MARGIN_LEFT + (availW - totalGridW) / 2;
        gridPixelTop  = GRID_TOP + (availH - totalGridH) / 2;
    }

    private void resizeGrid() {
        int[][] newGrid = new int[gridH][gridW];
        for (int r = 0; r < Math.min(gridH, grid.length); r++) {
            for (int c = 0; c < Math.min(gridW, grid[r].length); c++) {
                newGrid[r][c] = grid[r][c];
            }
        }
        grid = newGrid;
    }

    private void clearGrid() {
        grid = new int[gridH][gridW];
    }

    // ── Paint helpers ─────────────────────────────────────────────────────────

    private void paintAt(double screenX, double screenY, int brush) {
        int col = (int)((screenX - gridPixelLeft) / cellSize);
        int row = (int)((screenY - gridPixelTop) / cellSize);
        if (row >= 0 && row < gridH && col >= 0 && col < gridW) {
            grid[row][col] = brush;
        }
    }

    private boolean isOverToolbar(double x, double y) {
        return x >= TOOLBAR_X && x <= TOOLBAR_X + TOOLBAR_W
            && y >= GRID_TOP && y < GRID_TOP + BRUSH_COUNT * TOOLBAR_BRUSH_H;
    }

    private void handleToolbarClick(double x, double y) {
        int relY = (int)(y - GRID_TOP);
        int brushIdx = relY / TOOLBAR_BRUSH_H;
        if (brushIdx < 0 || brushIdx >= BRUSH_COUNT) return;

        double relX = x - TOOLBAR_X;
        if (relX >= 3 && relX <= 19) {
            // Clicking the color swatch toggles the picker for that brush
            colorPickerForBrush = (colorPickerForBrush == brushIdx) ? -1 : brushIdx;
        } else {
            selectedBrush = brushIdx;
            colorPickerForBrush = -1;
        }
    }

    // ── Color picker helpers ──────────────────────────────────────────────────

    private int[] getPickerOrigin(int brushIdx) {
        int px = TOOLBAR_X + TOOLBAR_W + 4;
        int rowY = GRID_TOP + brushIdx * TOOLBAR_BRUSH_H;
        int py = Math.max(GRID_TOP, Math.min(this.height - PICK_ROWS * PICK_SWATCH - 24, rowY));
        return new int[]{px, py};
    }

    private boolean isOverPickerPopup(double x, double y) {
        if (colorPickerForBrush < 0) return false;
        int[] o = getPickerOrigin(colorPickerForBrush);
        int popupW = PICK_COLS * PICK_SWATCH + 4;
        int popupH = PICK_ROWS * PICK_SWATCH + 20;
        return x >= o[0] && x < o[0] + popupW && y >= o[1] && y < o[1] + popupH;
    }

    private int getPickerSwatchAt(double x, double y) {
        if (colorPickerForBrush < 0) return -1;
        int[] o = getPickerOrigin(colorPickerForBrush);
        int sx = o[0] + 2, sy = o[1] + 14;
        if (x < sx || x >= sx + PICK_COLS * PICK_SWATCH
            || y < sy || y >= sy + PICK_ROWS * PICK_SWATCH) return -1;
        int col = (int)((x - sx) / PICK_SWATCH);
        int row = (int)((y - sy) / PICK_SWATCH);
        int idx = row * PICK_COLS + col;
        return (idx >= 0 && idx < COLOR_PALETTE.length) ? idx : -1;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int gridRight  = gridPixelLeft + gridW * cellSize;
        int gridBottom = gridPixelTop  + gridH * cellSize;

        // Title
        g.centeredText(this.font, "Floorplan Designer", this.width / 2, 2, 0xFFD700);

        // Grid size label (right of controls)
        g.text(this.font, gridW + " × " + gridH, GRID_MARGIN_LEFT + 230, 12, 0x888888);

        // ── Toolbar ───────────────────────────────────────────────────────────
        g.fill(TOOLBAR_X, GRID_TOP, TOOLBAR_X + TOOLBAR_W,
               GRID_TOP + BRUSH_COUNT * TOOLBAR_BRUSH_H, 0x88000000);
        g.fill(TOOLBAR_X, GRID_TOP, TOOLBAR_X + TOOLBAR_W, GRID_TOP + 1, 0x44FFFFFF);

        for (int i = 0; i < BRUSH_COUNT; i++) {
            int by = GRID_TOP + i * TOOLBAR_BRUSH_H;
            boolean selected = i == selectedBrush;
            boolean hovered = mouseX >= TOOLBAR_X && mouseX <= TOOLBAR_X + TOOLBAR_W
                && mouseY >= by && mouseY < by + TOOLBAR_BRUSH_H;

            if (selected) g.fill(TOOLBAR_X, by, TOOLBAR_X + TOOLBAR_W, by + TOOLBAR_BRUSH_H, 0x88337799);
            else if (hovered) g.fill(TOOLBAR_X, by, TOOLBAR_X + TOOLBAR_W, by + TOOLBAR_BRUSH_H, 0x33FFFFFF);

            // Color swatch (click to open color picker)
            boolean swatchHovered = mouseX >= TOOLBAR_X + 3 && mouseX <= TOOLBAR_X + 19
                && mouseY >= by + 4 && mouseY < by + TOOLBAR_BRUSH_H - 3;
            g.fill(TOOLBAR_X + 3, by + 4, TOOLBAR_X + 19, by + TOOLBAR_BRUSH_H - 3, brushColors[i]);
            if (swatchHovered || colorPickerForBrush == i)
                g.fill(TOOLBAR_X + 3, by + 4, TOOLBAR_X + 19, by + TOOLBAR_BRUSH_H - 3, 0x55FFFFFF);
            // Small edit corner dot
            g.fill(TOOLBAR_X + 15, by + 4, TOOLBAR_X + 19, by + 8, 0xAAFFFFFF);
            // Label
            g.text(this.font, BRUSH_LABELS[i], TOOLBAR_X + 21, by + 7,
                selected ? 0xFFFFFF : 0x999999);
        }

        // ── Grid border ───────────────────────────────────────────────────────
        g.fill(gridPixelLeft - 1, gridPixelTop - 1, gridRight + 1, gridBottom + 1, 0xFF333333);

        // ── Grid cells ────────────────────────────────────────────────────────
        for (int r = 0; r < gridH; r++) {
            for (int c = 0; c < gridW; c++) {
                int type = grid[r][c];
                int cx = gridPixelLeft + c * cellSize;
                int cy = gridPixelTop  + r * cellSize;
                int color = brushColors[type];
                // Leave a 1px gap between cells (the border shows through)
                g.fill(cx, cy, cx + cellSize - 1, cy + cellSize - 1, color);
            }
        }

        // ── Grid lines (only when cells are large enough to see them) ─────────
        if (cellSize >= 7) {
            for (int r = 0; r <= gridH; r++) {
                int ly = gridPixelTop + r * cellSize;
                g.fill(gridPixelLeft, ly, gridRight, ly + 1, 0x22FFFFFF);
            }
            for (int c = 0; c <= gridW; c++) {
                int lx = gridPixelLeft + c * cellSize;
                g.fill(lx, gridPixelTop, lx + 1, gridBottom, 0x22FFFFFF);
            }
        }

        // ── Hover highlight ───────────────────────────────────────────────────
        if (mouseX >= gridPixelLeft && mouseX < gridRight
            && mouseY >= gridPixelTop && mouseY < gridBottom) {
            int hc = (mouseX - gridPixelLeft) / cellSize;
            int hr = (mouseY - gridPixelTop) / cellSize;
            int hx = gridPixelLeft + hc * cellSize;
            int hy = gridPixelTop  + hr * cellSize;
            g.fill(hx, hy, hx + cellSize - 1, hy + cellSize - 1, 0x55FFFFFF);
            // Coordinate label at bottom-left of grid
            g.text(this.font, hc + ", " + hr, gridPixelLeft, gridBottom + 4, 0x555555);
        }

        // ── Bottom hint ───────────────────────────────────────────────────────
        g.text(this.font, "Left-click/drag: paint   Right-click/drag: erase   Click swatch to recolor",
            gridPixelLeft + 40, gridBottom + 4, 0x444444);

        // ── Color picker popup ────────────────────────────────────────────────
        if (colorPickerForBrush >= 0) {
            int[] o = getPickerOrigin(colorPickerForBrush);
            int px = o[0], py = o[1];
            int popupW = PICK_COLS * PICK_SWATCH + 4;
            int popupH = PICK_ROWS * PICK_SWATCH + 20;

            // Panel background + border
            g.fill(px, py, px + popupW, py + popupH, 0xEE111111);
            g.fill(px, py, px + popupW, py + 1, 0xAAFFD700);
            g.fill(px, py, px + 1, py + popupH, 0xAAFFD700);
            g.fill(px + popupW - 1, py, px + popupW, py + popupH, 0xAAFFD700);
            g.fill(px, py + popupH - 1, px + popupW, py + popupH, 0xAAFFD700);

            // Header
            g.text(this.font, BRUSH_LABELS[colorPickerForBrush] + " color:", px + 3, py + 3, 0xFFD700);

            // Swatches
            int sx = px + 2, sy = py + 14;
            for (int row = 0; row < PICK_ROWS; row++) {
                for (int col = 0; col < PICK_COLS; col++) {
                    int idx = row * PICK_COLS + col;
                    int swX = sx + col * PICK_SWATCH;
                    int swY = sy + row * PICK_SWATCH;
                    g.fill(swX, swY, swX + PICK_SWATCH - 1, swY + PICK_SWATCH - 1, COLOR_PALETTE[idx]);
                    // Hover highlight
                    if (mouseX >= swX && mouseX < swX + PICK_SWATCH - 1
                        && mouseY >= swY && mouseY < swY + PICK_SWATCH - 1) {
                        g.fill(swX, swY, swX + PICK_SWATCH - 1, swY + PICK_SWATCH - 1, 0x66FFFFFF);
                    }
                    // White border on currently selected color
                    if (COLOR_PALETTE[idx] == brushColors[colorPickerForBrush]) {
                        g.fill(swX, swY, swX + PICK_SWATCH - 1, swY + 1, 0xFFFFFFFF);
                        g.fill(swX, swY, swX + 1, swY + PICK_SWATCH - 1, 0xFFFFFFFF);
                        g.fill(swX, swY + PICK_SWATCH - 2, swX + PICK_SWATCH - 1, swY + PICK_SWATCH - 1, 0xFFFFFFFF);
                        g.fill(swX + PICK_SWATCH - 2, swY, swX + PICK_SWATCH - 1, swY + PICK_SWATCH - 1, 0xFFFFFFFF);
                    }
                }
            }
        }

        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        double x = event.x();
        double y = event.y();

        // ── Color picker popup takes priority ─────────────────────────────
        if (colorPickerForBrush >= 0) {
            int swatchIdx = getPickerSwatchAt(x, y);
            if (swatchIdx >= 0) {
                brushColors[colorPickerForBrush] = COLOR_PALETTE[swatchIdx];
                colorPickerForBrush = -1;
                return true;
            }
            if (!isOverPickerPopup(x, y)) {
                colorPickerForBrush = -1;
                // fall through — allow toolbar / grid interaction
            } else {
                return true; // consumed by popup
            }
        }

        if (event.button() == 1) {
            // Right click = erase
            paintAt(x, y, EMPTY);
            return true;
        }

        if (isOverToolbar(x, y)) {
            handleToolbarClick(x, y);
            return true;
        }

        paintAt(x, y, selectedBrush);
        return super.mouseClicked(event, consumed);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        double x = event.x();
        double y = event.y();

        colorPickerForBrush = -1; // dragging always closes the picker

        if (event.button() == 1) {
            paintAt(x, y, EMPTY);
        } else {
            paintAt(x, y, selectedBrush);
        }
        return true;
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private Path getFloorplanFile() {
        return Minecraft.getInstance().gameDirectory.toPath()
            .resolve("architects_notebook")
            .resolve("floorplan.json");
    }

    private void saveFloorplan() {
        try {
            Path file = getFloorplanFile();
            Files.createDirectories(file.getParent());

            JsonObject obj = new JsonObject();
            obj.addProperty("w", gridW);
            obj.addProperty("h", gridH);
            JsonArray colors = new JsonArray();
            for (int c : brushColors) colors.add(c);
            obj.add("colors", colors);
            JsonArray rows = new JsonArray();
            for (int[] row : grid) {
                JsonArray rowArr = new JsonArray();
                for (int cell : row) rowArr.add(cell);
                rows.add(rowArr);
            }
            obj.add("cells", rows);
            Files.writeString(file, GSON.toJson(obj));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadFloorplan() {
        try {
            Path file = getFloorplanFile();
            if (!Files.exists(file)) return;
            JsonObject obj = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            gridW = obj.get("w").getAsInt();
            gridH = obj.get("h").getAsInt();
            if (obj.has("colors")) {
                JsonArray colors = obj.getAsJsonArray("colors");
                for (int i = 0; i < Math.min(colors.size(), brushColors.length); i++) {
                    brushColors[i] = colors.get(i).getAsInt();
                }
            }
            grid = new int[gridH][gridW];
            JsonArray rows = obj.getAsJsonArray("cells");
            for (int r = 0; r < Math.min(rows.size(), gridH); r++) {
                JsonArray row = rows.get(r).getAsJsonArray();
                for (int c = 0; c < Math.min(row.size(), gridW); c++) {
                    grid[r][c] = row.get(c).getAsInt();
                }
            }
            recomputeLayout();
        } catch (IOException | JsonSyntaxException | IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClose() {
        saveFloorplan(); // auto-save on close
        this.minecraft.setScreen(parent);
    }
}
