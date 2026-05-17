package name.modid.client.screen;

import name.modid.client.data.InspirationData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class InspirationBrowserScreen extends Screen {

    private final Screen parent;
    private String selectedCategory;
    private List<InspirationData.InspirationEntry> entries;
    private int selectedEntryIndex = 0;
    private int scrollOffset = 0;

    private static final int ENTRY_HEIGHT = 22;
    private static final int LIST_X = 10;
    private static final int LIST_W = 140;
    private static final int DETAIL_X = 158;

    public InspirationBrowserScreen(Screen parent) {
        super(Component.literal("Inspiration Browser"));
        this.parent = parent;
        this.selectedCategory = InspirationData.CATEGORIES.get(0);
        this.entries = InspirationData.getCategory(selectedCategory);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int catY = 22;
        int catBtnW = (this.width - 20) / InspirationData.CATEGORIES.size() - 2;

        // Category tab buttons
        for (int i = 0; i < InspirationData.CATEGORIES.size(); i++) {
            final String cat = InspirationData.CATEGORIES.get(i);
            int x = 10 + i * (catBtnW + 2);
            this.addRenderableWidget(Button.builder(
                Component.literal(cat),
                btn -> selectCategory(cat)
            ).bounds(x, catY, catBtnW, 18).build());
        }

        // Scroll buttons
        this.addRenderableWidget(Button.builder(
            Component.literal("▲"),
            btn -> { if (scrollOffset > 0) scrollOffset--; }
        ).bounds(LIST_X, this.height - 44, LIST_W / 2 - 2, 18).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("▼"),
            btn -> { if (scrollOffset < entries.size() - visibleRows()) scrollOffset++; }
        ).bounds(LIST_X + LIST_W / 2 + 2, this.height - 44, LIST_W / 2 - 2, 18).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("← Back"),
            btn -> this.onClose()
        ).bounds(8, this.height - 24, 80, 18).build());
    }

    private void selectCategory(String cat) {
        selectedCategory = cat;
        entries = InspirationData.getCategory(cat);
        selectedEntryIndex = 0;
        scrollOffset = 0;
    }

    private int visibleRows() {
        int listTop = 46;
        int listBottom = this.height - 50;
        return Math.max(1, (listBottom - listTop) / ENTRY_HEIGHT);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        // Header
        g.centeredText(this.font, "Inspiration Browser", this.width / 2, 8, 0xFFD700);

        int listTop = 46;
        int listBottom = this.height - 50;

        // List panel background
        g.fill(LIST_X, listTop, LIST_X + LIST_W, listBottom, 0x88000000);
        g.fill(LIST_X, listTop, LIST_X + LIST_W, listTop + 1, 0x44FFFFFF);

        // Entry list
        int visible = visibleRows();
        for (int i = 0; i < visible; i++) {
            int idx = i + scrollOffset;
            if (idx >= entries.size()) break;
            InspirationData.InspirationEntry entry = entries.get(idx);
            int entryY = listTop + i * ENTRY_HEIGHT;

            boolean selected = idx == selectedEntryIndex;
            if (selected) {
                g.fill(LIST_X + 1, entryY, LIST_X + LIST_W - 1, entryY + ENTRY_HEIGHT, 0x88337799);
            }

            // Clickable area — detect hover
            boolean hovered = mouseX >= LIST_X && mouseX <= LIST_X + LIST_W
                && mouseY >= entryY && mouseY < entryY + ENTRY_HEIGHT;
            int nameColor = selected ? 0xFFD700 : (hovered ? 0xDDDDDD : 0xAAAAAA);

            g.text(this.font, entry.name(), LIST_X + 4, entryY + 6, nameColor);
        }

        // Detail panel background
        int detailW = this.width - DETAIL_X - 10;
        g.fill(DETAIL_X, listTop, DETAIL_X + detailW, listBottom, 0x88000000);
        g.fill(DETAIL_X, listTop, DETAIL_X + detailW, listTop + 1, 0x44FFFFFF);

        if (!entries.isEmpty()) {
            InspirationData.InspirationEntry entry = entries.get(selectedEntryIndex);
            int dx = DETAIL_X + 8;
            int dy = listTop + 8;
            int maxW = detailW - 16;

            // Category badge
            g.fill(dx - 2, dy - 2, dx + this.font.width(entry.category()) + 6, dy + 10, 0x88334455);
            g.text(this.font, entry.category(), dx, dy, 0x88CCFF);
            dy += 14;

            // Name
            g.text(this.font, entry.name(), dx, dy, 0xFFFFFF);
            dy += 14;

            // Divider
            g.fill(dx, dy, DETAIL_X + detailW - 8, dy + 1, 0x44FFFFFF);
            dy += 6;

            // Description wrapped
            List<String> descLines = wrapString(entry.description(), maxW);
            for (String line : descLines) {
                if (dy + 10 > listBottom - 8) break;
                g.text(this.font, line, dx, dy, 0xCCCCCC);
                dy += 11;
            }

            dy += 8;
            if (dy + 10 < listBottom - 8) {
                g.text(this.font, "Tips:", dx, dy, 0xFFD700);
                dy += 12;
                for (String tip : entry.tips()) {
                    List<String> tipLines = wrapString("• " + tip, maxW);
                    for (String tl : tipLines) {
                        if (dy + 10 > listBottom - 8) break;
                        g.text(this.font, tl, dx, dy, 0xAAFF88);
                        dy += 11;
                    }
                }
            }
        }

        // Scroll indicator
        if (entries.size() > 0) {
            String scrollInfo = (selectedEntryIndex + 1) + " / " + entries.size();
            g.centeredText(this.font, scrollInfo, LIST_X + LIST_W / 2, this.height - 50, 0x555555);
        }

        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        double mouseX = event.x();
        double mouseY = event.y();
        int listTop = 46;
        int listBottom = this.height - 50;

        if (mouseX >= LIST_X && mouseX <= LIST_X + LIST_W
            && mouseY >= listTop && mouseY < listBottom) {
            int row = (int)((mouseY - listTop) / ENTRY_HEIGHT);
            int idx = row + scrollOffset;
            if (idx >= 0 && idx < entries.size()) {
                selectedEntryIndex = idx;
                return true;
            }
        }
        return super.mouseClicked(event, consumed);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY < 0) {
            if (scrollOffset < entries.size() - visibleRows()) scrollOffset++;
        } else {
            if (scrollOffset > 0) scrollOffset--;
        }
        return true;
    }

    private List<String> wrapString(String text, int maxWidth) {
        List<String> result = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (this.font.width(candidate) > maxWidth) {
                if (!line.isEmpty()) {
                    result.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    result.add(word);
                }
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
