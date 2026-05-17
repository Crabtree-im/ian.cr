package name.modid.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class BuildPlannerDashboard extends Screen {

    private final Screen parent;

    public BuildPlannerDashboard(Screen parent) {
        super(Component.literal("Architect's Notebook"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX  = this.width / 2;
        int btnW     = 112;   // each column button width
        int btnH     = 20;
        int gap      = 4;
        int slot     = btnH + gap;
        int leftX    = centerX - 116;
        int rightX   = centerX + 4;
        int startY   = this.height / 2 - 76;

        // ── Left column (6 planning features) ─────────────────────────────
        this.addRenderableWidget(Button.builder(
            Component.literal("Build Prompt"),
            btn -> this.minecraft.setScreen(new BuildPromptScreen(this))
        ).bounds(leftX, startY, btnW, btnH).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("Palette Recommender"),
            btn -> this.minecraft.setScreen(new PaletteScreen(this))
        ).bounds(leftX, startY + slot, btnW, btnH).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("Inspiration Browser"),
            btn -> this.minecraft.setScreen(new InspirationBrowserScreen(this))
        ).bounds(leftX, startY + slot * 2, btnW, btnH).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("Projects & Notes"),
            btn -> this.minecraft.setScreen(new ProjectNotesScreen(this))
        ).bounds(leftX, startY + slot * 3, btnW, btnH).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("Build Timeline"),
            btn -> this.minecraft.setScreen(new BuildTimelineScreen(this))
        ).bounds(leftX, startY + slot * 4, btnW, btnH).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("Floorplan Designer"),
            btn -> this.minecraft.setScreen(new FloorplanDesignerScreen(this))
        ).bounds(leftX, startY + slot * 5, btnW, btnH).build());

        // ── Right column (5 tools + close) ────────────────────────────────
        this.addRenderableWidget(Button.builder(
            Component.literal("Blueprint Library"),
            btn -> this.minecraft.setScreen(new BlueprintLibraryScreen(this))
        ).bounds(rightX, startY, btnW, btnH).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("Block Calculator"),
            btn -> this.minecraft.setScreen(new BlockCalculatorScreen(this))
        ).bounds(rightX, startY + slot, btnW, btnH).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("Style Quiz"),
            btn -> this.minecraft.setScreen(new StyleQuizScreen(this))
        ).bounds(rightX, startY + slot * 2, btnW, btnH).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("Silhouette Generator"),
            btn -> this.minecraft.setScreen(new SilhouetteGeneratorScreen(this))
        ).bounds(rightX, startY + slot * 3, btnW, btnH).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("Auto-Variations"),
            btn -> this.minecraft.setScreen(new AutoVariationScreen(this))
        ).bounds(rightX, startY + slot * 4, btnW, btnH).build());

        // Close below both columns
        this.addRenderableWidget(Button.builder(
            Component.literal("Close"),
            btn -> this.onClose()
        ).bounds(centerX - 50, startY + slot * 6 + 2, 100, 18).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int centerX = this.width / 2;
        int topY    = this.height / 2 - 76;

        int panelX = centerX - 122;
        int panelW = 244;
        int panelH = 6 * 24 + 62;   // 6 rows + title + close padding
        int panelY = topY - 30;
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0x88000000);
        g.fill(panelX, panelY, panelX + panelW, panelY + 1, 0x55FFD700);
        g.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, 0x55FFD700);

        // Column headers
        g.centeredText(this.font, "Architect's Notebook", centerX, panelY + 8, 0xFFD700);
        g.centeredText(this.font, "The Creative Director for Builders", centerX, panelY + 20, 0xAAAAAA);
        g.text(this.font, "Planning",  centerX - 116, topY - 8, 0x666666);
        g.text(this.font, "Tools",     centerX + 4,   topY - 8, 0x666666);

        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
