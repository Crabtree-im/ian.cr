package name.modid.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class BlockCalculatorScreen extends Screen {

    private final Screen parent;

    // ── Shape modes ───────────────────────────────────────────────────────────
    private static final String[] SHAPES = {
        "Hollow Box", "Solid Box", "Floor/Ceiling",
        "4 Walls", "Gabled Roof", "Cylinder", "Arch"
    };
    private int selectedShape = 0;

    // ── Input fields ──────────────────────────────────────────────────────────
    private EditBox fieldW, fieldH, fieldD, fieldThick;

    // ── Results ───────────────────────────────────────────────────────────────
    private final List<ResultLine> results = new ArrayList<>();
    private String errorMsg = null;

    record ResultLine(String label, int count, int color) {}

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int LEFT_X   = 8;
    private static final int LEFT_W   = 160;
    private static final int RIGHT_X  = 176;
    private static final int TOP_Y    = 44;
    private static final int INPUT_H  = 16;
    private static final int LABEL_H  = 10;

    public BlockCalculatorScreen(Screen parent) {
        super(Component.literal("Block Calculator"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int fy = TOP_Y + 10;

        // ── Shape selector buttons ────────────────────────────────────────────
        int shapeBtnW = LEFT_W / SHAPES.length;
        int tabY = 22;
        // Put tabs in 2 rows since 7 won't fit in 1 row at LEFT_W
        int halfShapes = (SHAPES.length + 1) / 2;
        for (int i = 0; i < SHAPES.length; i++) {
            final int idx = i;
            int row = i / halfShapes;
            int col = i % halfShapes;
            int bx = LEFT_X + col * ((LEFT_W) / halfShapes);
            int bw = LEFT_W / halfShapes - 2;
            this.addRenderableWidget(Button.builder(
                Component.literal(SHAPES[i]),
                btn -> { selectedShape = idx; calculate(); }
            ).bounds(bx, tabY + row * 20, bw, 16).build());
        }

        fy = TOP_Y + 46;

        // ── Input fields ──────────────────────────────────────────────────────
        fieldW = makeField(fy, "Width (X)"); fy += INPUT_H + LABEL_H + 4;
        fieldH = makeField(fy, "Height (Y)"); fy += INPUT_H + LABEL_H + 4;
        fieldD = makeField(fy, "Depth (Z)");  fy += INPUT_H + LABEL_H + 4;
        fieldThick = makeField(fy, "Wall thickness");

        fieldW.setValue("10");
        fieldH.setValue("8");
        fieldD.setValue("10");
        fieldThick.setValue("1");

        // ── Calculate button ──────────────────────────────────────────────────
        this.addRenderableWidget(Button.builder(
            Component.literal("Calculate →"),
            btn -> calculate()
        ).bounds(LEFT_X, fy + INPUT_H + 8, LEFT_W, 18).build());

        // ── Clear button ──────────────────────────────────────────────────────
        this.addRenderableWidget(Button.builder(
            Component.literal("Clear"),
            btn -> { fieldW.setValue(""); fieldH.setValue(""); fieldD.setValue(""); results.clear(); errorMsg = null; }
        ).bounds(LEFT_X, fy + INPUT_H + 30, LEFT_W / 2 - 2, 16).build());

        // ── Back button ───────────────────────────────────────────────────────
        this.addRenderableWidget(Button.builder(
            Component.literal("← Back"),
            btn -> this.onClose()
        ).bounds(LEFT_X, this.height - 22, 80, 18).build());

        calculate(); // initial calculation
    }

    private EditBox makeField(int y, String hint) {
        EditBox box = new EditBox(this.font, LEFT_X, y + LABEL_H, LEFT_W, INPUT_H,
            Component.literal(hint));
        box.setMaxLength(8);
        box.setHint(Component.literal(hint + "..."));
        box.setResponder(s -> calculate());
        this.addRenderableWidget(box);
        return box;
    }

    // ── Calculation engine ────────────────────────────────────────────────────

    private void calculate() {
        results.clear();
        errorMsg = null;

        int w, h, d, t;
        try {
            w = parse(fieldW);
            h = parse(fieldH);
            d = parse(fieldD);
            t = parse(fieldThick);
            if (w <= 0 || h <= 0 || d <= 0 || t <= 0) { errorMsg = "All values must be > 0"; return; }
            if (t > Math.min(w, Math.min(h, d)) / 2) { errorMsg = "Thickness too large for dimensions"; return; }
        } catch (NumberFormatException e) {
            errorMsg = "Enter numbers only";
            return;
        }

        switch (selectedShape) {
            case 0 -> calcHollowBox(w, h, d, t);
            case 1 -> calcSolidBox(w, h, d);
            case 2 -> calcFloor(w, d);
            case 3 -> calcFourWalls(w, h, d, t);
            case 4 -> calcGabledRoof(w, h, d);
            case 5 -> calcCylinder(w, h);
            case 6 -> calcArch(w, h, t);
        }
    }

    private int parse(EditBox box) {
        String v = box.getValue().trim();
        if (v.isEmpty()) return 1;
        return Integer.parseInt(v);
    }

    // Hollow box = 4 walls + floor + ceiling, all t blocks thick
    private void calcHollowBox(int w, int h, int d, int t) {
        int floor   = w * d;
        int ceiling = w * d;
        // Walls (subtracting floor/ceiling overlap)
        int wallNS  = w * h * t * 2;
        int wallEW  = (d - 2 * t) * h * t * 2;
        // Subtract interior of floor/ceiling for wall connections
        int total   = floor + ceiling + wallNS + wallEW;
        // Interior volume
        int intW = Math.max(0, w - 2 * t);
        int intH = Math.max(0, h - 2 * t);
        int intD = Math.max(0, d - 2 * t);
        int interior = intW * intH * intD;

        addResult("Floor", floor, 0xFFBB8855);
        addResult("Ceiling", ceiling, 0xFF998866);
        addResult("N/S Walls (×2)", wallNS, 0xFF445566);
        addResult("E/W Walls (×2)", wallEW, 0xFF556677);
        addResult("Interior (air)", interior, 0xFF333333);
        int solid = total;
        addResult("TOTAL blocks", solid, 0xFFFFD700);
        addResult("Interior volume", intW + "×" + intH + "×" + intD + " = " + interior + " air", 0xFF444444);
    }

    private void calcSolidBox(int w, int h, int d) {
        int total = w * h * d;
        addResult("Total blocks", total, 0xFFFFD700);
        addResult("W × H × D", w + " × " + h + " × " + d, 0xFF888888);
    }

    private void calcFloor(int w, int d) {
        int full = w * d;
        int slabs = full; // could do slabs instead
        addResult("Full blocks (1 layer)", full, 0xFFBB8855);
        addResult("Slabs (2 layers = 1 block)", (full + 1) / 2, 0xFF99AA77);
        addResult("Area", w + " × " + d + " = " + full, 0xFF666666);
    }

    private void calcFourWalls(int w, int h, int d, int t) {
        // N and S walls (full width)
        int ns = w * h * t * 2;
        // E and W walls (minus corners to avoid double-counting)
        int ew = (d - 2 * t) * h * t * 2;
        int total = ns + ew;
        addResult("N/S Walls (×2)", ns, 0xFF445566);
        addResult("E/W Walls (×2)", ew, 0xFF556677);
        addResult("TOTAL blocks", total, 0xFFFFD700);
        addResult("Perimeter length", 2 * (w + d) - 4 * t + " blocks", 0xFF666666);
    }

    // Gabled roof: stair-step approximation
    private void calcGabledRoof(int w, int h, int d) {
        // Each layer is one step inward from each side
        // Steps = w/2 (we only care about the shorter span for pitch)
        int steps = (w + 1) / 2;
        int ridgeLen = d;
        int totalBlocks = 0;
        List<int[]> layers = new ArrayList<>();
        int curW = w;
        for (int layer = 0; layer < steps; layer++) {
            int rowCount = curW * ridgeLen;
            layers.add(new int[]{layer + 1, curW, rowCount});
            totalBlocks += rowCount;
            curW -= 2;
            if (curW <= 0) break;
        }
        // Show first 4 layers
        for (int i = 0; i < Math.min(layers.size(), 4); i++) {
            int[] l = layers.get(i);
            addResult("Layer " + l[0] + " (" + l[1] + " wide)", l[2], 0xFF887755 + i * 0x111100);
        }
        if (layers.size() > 4) {
            addResult("... (" + (layers.size() - 4) + " more layers)", 0, 0xFF555555);
        }
        addResult("TOTAL stair blocks", totalBlocks, 0xFFFFD700);
        addResult("Ridge length", ridgeLen + " blocks", 0xFF666666);
        addResult("Gable fill (×2)", steps * h / 2 + " blocks approx", 0xFF886644);
    }

    // Cylinder: pi*r^2 per layer approximation
    private void calcCylinder(int diameter, int height) {
        double r = diameter / 2.0;
        // Count actual blocks in circle using midpoint algorithm
        int circleArea = countCircleBlocks(diameter);
        int hollowArea = diameter >= 3 ? countCircleBlocks(diameter - 2) : 0;
        int wallBlocks = circleArea - hollowArea;

        addResult("Diameter", diameter + " blocks", 0xFF888888);
        addResult("Circle area (per layer)", circleArea, 0xFFBB8855);
        addResult("Hollow interior (per layer)", hollowArea, 0xFF333333);
        addResult("Wall ring (per layer)", wallBlocks, 0xFF556677);
        addResult("Full cylinder (" + height + " layers)", circleArea * height, 0xFFFFD700);
        addResult("Hollow cylinder (" + height + " layers)", wallBlocks * height, 0xFF88AACC);
    }

    private int countCircleBlocks(int diameter) {
        if (diameter <= 0) return 0;
        int r = diameter / 2;
        double center = (diameter - 1) / 2.0;
        int count = 0;
        for (int x = 0; x < diameter; x++) {
            for (int z = 0; z < diameter; z++) {
                double dx = x - center, dz = z - center;
                if (dx * dx + dz * dz <= r * r + 0.5) count++;
            }
        }
        return count;
    }

    // Arch: semicircular arch of given width and height
    private void calcArch(int span, int rise, int thickness) {
        double cx = (span - 1) / 2.0;
        double a = span / 2.0, b = rise;
        int blockCount = 0;
        List<int[]> heightMap = new ArrayList<>(); // blocks per column

        for (int x = 0; x < span; x++) {
            double nx = (x - cx) / a;
            double y = b * Math.sqrt(Math.max(0, 1 - nx * nx));
            int col = (int) Math.round(y);
            heightMap.add(new int[]{x, col});
        }
        for (int[] col : heightMap) blockCount += col[1] * thickness;

        addResult("Span (width)", span + " blocks", 0xFF888888);
        addResult("Rise (height)", rise + " blocks", 0xFF888888);
        addResult("Thickness (depth)", thickness + " blocks", 0xFF888888);
        addResult("Arch blocks", blockCount, 0xFFFFD700);
        addResult("Opening area", "~" + (span * rise / 2) + " blocks (approx)", 0xFF445566);
    }

    private void addResult(String label, int count, int color) {
        results.add(new ResultLine(label, count, color));
    }
    private void addResult(String label, String value, int color) {
        results.add(new ResultLine(label + ": " + value, -1, color));
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        // Header
        g.centeredText(this.font, "Block Calculator", this.width / 2, 8, 0xFFD700);

        // Left panel
        g.fill(LEFT_X, TOP_Y, LEFT_X + LEFT_W, this.height - 26, 0x88000000);
        g.fill(LEFT_X, TOP_Y, LEFT_X + LEFT_W, TOP_Y + 1, 0x44FFFFFF);

        // Labels for fields
        int labelY = TOP_Y + 46;
        int tabH = ((SHAPES.length + 1) / 2) * 20;
        labelY += tabH - 20;

        // ── Render field labels above each EditBox ────────────────────────────
        drawFieldLabel(g, fieldW, "Width (X blocks)");
        drawFieldLabel(g, fieldH, "Height (Y blocks)");
        drawFieldLabel(g, fieldD, "Depth (Z blocks)");
        drawFieldLabel(g, fieldThick, "Wall thickness");

        // Shape label
        g.text(this.font, "Shape: " + SHAPES[selectedShape], LEFT_X + 2, TOP_Y + 4, 0xFFD700);

        // Right panel
        int rx = RIGHT_X;
        int rw = this.width - rx - 8;
        int rpTop = TOP_Y;
        int rpBot = this.height - 26;
        g.fill(rx, rpTop, rx + rw, rpBot, 0x88000000);
        g.fill(rx, rpTop, rx + rw, rpTop + 1, 0x44FFFFFF);

        g.text(this.font, "Results \u2014 " + SHAPES[selectedShape], rx + 4, rpTop + 4, 0xFFD700);
        g.fill(rx + 4, rpTop + 14, rx + rw - 4, rpTop + 15, 0x33FFFFFF);

        if (errorMsg != null) {
            g.text(this.font, "\u26a0 " + errorMsg, rx + 6, rpTop + 22, 0xFFCC4444);
        } else {
            int dy = rpTop + 20;
            int totalLine = -1;

            for (int i = 0; i < results.size(); i++) {
                ResultLine r = results.get(i);
                if (dy + 11 > rpBot - 6) break;

                boolean isTotal = r.label().startsWith("TOTAL");
                if (isTotal) {
                    // Divider above total
                    g.fill(rx + 4, dy - 2, rx + rw - 4, dy - 1, 0x44FFFFFF);
                    dy += 2;
                }

                // Label
                g.text(this.font, r.label(), rx + 6, dy, isTotal ? 0xFFD700 : r.color());

                // Count (right-aligned)
                if (r.count() >= 0) {
                    String countStr = formatCount(r.count());
                    g.text(this.font, countStr, rx + rw - 6 - this.font.width(countStr), dy,
                        isTotal ? 0xFFFFFF : 0xCCCCCC);

                    // Small bar chart
                    if (!isTotal && r.count() > 0 && totalLine < 0) {
                        // find total for bar ratio
                    }
                }

                dy += isTotal ? 13 : 11;
            }

            // Find the total line count for bar proportions
            int totalBlocks = 0;
            for (ResultLine r : results) {
                if (r.label().startsWith("TOTAL") && r.count() >= 0) { totalBlocks = r.count(); break; }
            }

            // Draw mini stacked bar if we have results
            if (totalBlocks > 0 && !results.isEmpty()) {
                int barY = rpBot - 22;
                int barX = rx + 4;
                int barW = rw - 8;
                int barH = 10;
                g.fill(barX, barY, barX + barW, barY + barH, 0x33333333);
                int bx = barX;
                for (ResultLine r : results) {
                    if (r.count() <= 0 || r.label().startsWith("TOTAL") || r.label().contains("interior")) continue;
                    int seg = (int)((long) r.count() * barW / totalBlocks);
                    if (seg > 0) {
                        g.fill(bx, barY, bx + seg, barY + barH, r.color() | 0xFF000000);
                        bx += seg;
                    }
                }
                g.text(this.font, "↑ material breakdown", barX, barY + 12, 0x444444);
            }
        }

        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    private void drawFieldLabel(GuiGraphicsExtractor g, EditBox box, String label) {
        if (box == null) return;
        g.text(this.font, label, box.getX(), box.getY() - LABEL_H, 0x888888);
    }

    private String formatCount(int n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 1_000)     return String.format("%.1fK", n / 1_000.0);
        return String.valueOf(n);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        return super.mouseClicked(event, consumed);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
