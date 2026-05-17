package name.modid.client.screen;

import name.modid.client.data.PromptData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class BuildPromptScreen extends Screen {

    private final Screen parent;
    private String selectedThemeKey;
    private String currentPrompt = "";
    private final List<String> promptHistory = new ArrayList<>();
    private int historyIndex = -1;

    // Track theme button positions so we can draw the selection highlight
    private final List<int[]> themeBtnBounds = new ArrayList<>(); // {x, y, w, h}

    public BuildPromptScreen(Screen parent) {
        super(Component.literal("Build Prompt Generator"));
        this.parent = parent;
        this.selectedThemeKey = PromptData.getThemeKeys().get(0);
    }

    @Override
    protected void init() {
        themeBtnBounds.clear();

        List<String> themeKeys = PromptData.getThemeKeys();
        int themesPerRow = 4;
        int btnW = 88;
        int btnH = 20;
        int hGap = 4;
        int totalRowWidth = themesPerRow * btnW + (themesPerRow - 1) * hGap;
        int themeStartX = this.width / 2 - totalRowWidth / 2;
        int themeStartY = 22;

        for (int i = 0; i < themeKeys.size(); i++) {
            final String key = themeKeys.get(i);
            PromptData.ThemeData theme = PromptData.getTheme(key);
            int col = i % themesPerRow;
            int row = i / themesPerRow;
            int x = themeStartX + col * (btnW + hGap);
            int y = themeStartY + row * (btnH + 4);

            themeBtnBounds.add(new int[]{x, y, btnW, btnH, i});

            this.addRenderableWidget(Button.builder(
                Component.literal(theme.displayName()),
                btn -> { selectedThemeKey = key; }
            ).bounds(x, y, btnW, btnH).build());
        }

        int centerX = this.width / 2;
        int controlsY = this.height - 28;

        this.addRenderableWidget(Button.builder(
            Component.literal("Generate"),
            btn -> generatePrompt()
        ).bounds(centerX - 130, controlsY, 80, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("Any Theme"),
            btn -> {
                currentPrompt = PromptData.getRandomPromptAny();
                promptHistory.add(currentPrompt);
                historyIndex = promptHistory.size() - 1;
            }
        ).bounds(centerX - 42, controlsY, 84, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("< Prev"),
            btn -> {
                if (historyIndex > 0) {
                    historyIndex--;
                    currentPrompt = promptHistory.get(historyIndex);
                }
            }
        ).bounds(centerX + 48, controlsY, 52, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("Next >"),
            btn -> {
                if (historyIndex < promptHistory.size() - 1) {
                    historyIndex++;
                    currentPrompt = promptHistory.get(historyIndex);
                }
            }
        ).bounds(centerX + 106, controlsY, 52, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("Back"),
            btn -> this.onClose()
        ).bounds(8, controlsY, 50, 20).build());
    }

    private void generatePrompt() {
        currentPrompt = PromptData.getRandomPrompt(selectedThemeKey);
        promptHistory.add(currentPrompt);
        historyIndex = promptHistory.size() - 1;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int centerX = this.width / 2;
        List<String> themeKeys = PromptData.getThemeKeys();

        // ── Title ──────────────────────────────────────────────────────────
        g.centeredText(this.font, "Build Prompt Generator", centerX, 8, 0xFFD700);

        // ── Selected-theme highlight (drawn before super so buttons paint over) ──
        for (int[] b : themeBtnBounds) {
            int idx = b[4];
            if (idx < themeKeys.size() && themeKeys.get(idx).equals(selectedThemeKey)) {
                // Cyan glow border around the active theme button
                g.fill(b[0] - 1, b[1] - 1, b[0] + b[2] + 1, b[1] + b[3] + 1, 0xFF00CCFF);
            }
        }

        // ── Prompt display box ─────────────────────────────────────────────
        int boxPad = 12;
        int boxX   = boxPad;
        int boxW   = this.width - boxPad * 2;
        int boxY   = 70;
        int boxH   = Math.max(60, this.height - 115);

        g.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xAA000000);
        g.fill(boxX, boxY, boxX + boxW, boxY + 1, 0x66FFD700);
        g.fill(boxX, boxY + boxH - 1, boxX + boxW, boxY + boxH, 0x33FFD700);

        // Active theme label inside the box top
        String themeLabel = PromptData.getTheme(selectedThemeKey).displayName() + " theme";
        g.text(this.font, themeLabel, boxX + 6, boxY + 5, 0x88DDFF);

        if (currentPrompt.isEmpty()) {
            // Hint text
            g.centeredText(this.font, "Pick a theme above, then press Generate", centerX, boxY + boxH / 2 - 4, 0x555555);
        } else {
            // Word-wrapped prompt text
            List<String> lines = wrapText(currentPrompt, boxW - 16);
            int lineH    = 11;
            int totalH   = lines.size() * lineH;
            int textY    = boxY + (boxH - totalH) / 2;
            for (String line : lines) {
                g.centeredText(this.font, line, centerX, textY, 0xFFFFFF);
                textY += lineH;
            }

            // History counter
            String counter = (historyIndex + 1) + " / " + promptHistory.size();
            g.text(this.font, counter, boxX + boxW - this.font.width(counter) - 4, boxY + boxH - 11, 0x444444);
        }

        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    /** Word-wrap to a list of plain Strings (safe for all g.centeredText overloads). */
    private List<String> wrapText(String text, int maxWidth) {
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
