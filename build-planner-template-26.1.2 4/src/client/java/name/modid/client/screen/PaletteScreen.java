package name.modid.client.screen;

import name.modid.client.data.PaletteData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.List;

public class PaletteScreen extends Screen {

    private final Screen parent;
    private final List<String> paletteKeys;
    private int selectedIndex = 0;

    public PaletteScreen(Screen parent) {
        super(Component.literal("Palette Recommender"));
        this.parent = parent;
        this.paletteKeys = PaletteData.getPaletteKeys();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int navY = 42;

        this.addRenderableWidget(Button.builder(
            Component.literal("<"),
            btn -> selectedIndex = (selectedIndex - 1 + paletteKeys.size()) % paletteKeys.size()
        ).bounds(centerX - 140, navY, 20, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.literal(">"),
            btn -> selectedIndex = (selectedIndex + 1) % paletteKeys.size()
        ).bounds(centerX + 120, navY, 20, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("Random Palette"),
            btn -> selectedIndex = (int) (Math.random() * paletteKeys.size())
        ).bounds(centerX - 65, this.height - 30, 130, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("Back"),
            btn -> this.onClose()
        ).bounds(8, this.height - 30, 60, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        PaletteData.Palette palette = PaletteData.getPalette(paletteKeys.get(selectedIndex));
        int centerX = this.width / 2;

        g.centeredText(this.font, "Palette Recommender", centerX, 8, 0xFFD700);
        g.centeredText(this.font, palette.name(), centerX, 44, 0xFFFFFF);
        g.centeredText(this.font, palette.style(), centerX, 56, 0xAAAAAA);
        g.centeredText(this.font, palette.description(), centerX, 68, 0x88CCFF);

        String counter = (selectedIndex + 1) + " / " + paletteKeys.size();
        g.centeredText(this.font, counter, centerX, 80, 0x555555);

        int sectionX = 20;
        int sectionY = 96;
        int sectionGap = 8;

        sectionY = drawSection(g, "Primary Blocks", palette.primary(), sectionX, sectionY);
        sectionY += sectionGap;
        sectionY = drawSection(g, "Accent Blocks", palette.accent(), sectionX, sectionY);
        sectionY += sectionGap;
        sectionY = drawSection(g, "Roof Blocks", palette.roof(), sectionX, sectionY);
        sectionY += sectionGap;
        drawSection(g, "Trim & Details", palette.trim(), sectionX, sectionY);

        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    private int drawSection(GuiGraphicsExtractor g, String label, List<PaletteData.BlockEntry> blocks, int x, int y) {
        g.text(this.font, label, x, y, 0xFFD700);

        int swatchSize = 22;
        int columnWidth = 100;
        int swatchY = y + 11;

        for (int i = 0; i < blocks.size(); i++) {
            PaletteData.BlockEntry entry = blocks.get(i);
            int swatchX = x + i * columnWidth;

            int opaqueColor = 0xFF000000 | entry.color();
            g.fill(swatchX, swatchY, swatchX + swatchSize, swatchY + swatchSize, opaqueColor);

            g.fill(swatchX, swatchY, swatchX + swatchSize, swatchY + 1, 0x55FFFFFF);
            g.fill(swatchX, swatchY, swatchX + 1, swatchY + swatchSize, 0x55FFFFFF);
            g.fill(swatchX + swatchSize - 1, swatchY, swatchX + swatchSize, swatchY + swatchSize, 0x55000000);
            g.fill(swatchX, swatchY + swatchSize - 1, swatchX + swatchSize, swatchY + swatchSize, 0x55000000);

            int nameX = swatchX + swatchSize + 4;
            int nameY = swatchY + (swatchSize / 2) - 4;
            g.text(this.font, entry.name(), nameX, nameY, 0xCCCCCC);
        }

        return swatchY + swatchSize + 2;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
