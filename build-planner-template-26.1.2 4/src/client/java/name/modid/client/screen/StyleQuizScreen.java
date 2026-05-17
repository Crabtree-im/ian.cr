package name.modid.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import name.modid.client.data.PromptData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StyleQuizScreen extends Screen {

    private final Screen parent;

    // ── Questions & options ───────────────────────────────────────────────────
    private static final String[] QUESTIONS = {
        "What primary material calls to you?",
        "How large do your builds tend to be?",
        "Which atmosphere fits your vision?",
        "What landscape inspires you most?",
        "What era speaks to you?",
        "What matters most in a great build?"
    };

    private static final String[][] OPTIONS = {
        { "Stone & Brick",          "Wood & Logs",        "Sand & Terracotta",       "Metal & Nether"       },
        { "Tiny  (< 10 blocks)",    "Cozy cottage",       "Large fortress",          "Massive cityscape"    },
        { "Cozy & Warm",            "Grand & Imposing",   "Whimsical & Magical",     "Dark & Mysterious"    },
        { "Forest & Jungle",        "Mountain & Snow",    "Ocean & Tropics",         "Desert & Mesa"        },
        { "Medieval / Fantasy",     "Ancient / Mystical", "Industrial / Steam",      "Futuristic / Space"   },
        { "Fine architectural detail", "Impressive scale","Bold color & palette",    "Narrative & storytelling" }
    };

    // ── Scoring tables ────────────────────────────────────────────────────────
    // Palette indices:
    //  0=Cobblestone Keep  1=Dark Oak Manor   2=Spruce Cabin      3=Sandstone Temple
    //  4=Jungle Outpost    5=Nether Fortress  6=End Citadel       7=Ocean Monument
    //  8=Ice Palace        9=Cherry Blossom  10=Autumn Harvest   11=Birch Cottage
    // 12=Industrial Steam
    //
    // PAL_VOTES[q][a] = flat pairs of {paletteIndex, score, paletteIndex, score, ...}

    private static final int[][][] PAL_VOTES = {
        // Q0: material
        { {0,3, 1,1},          // Stone & Brick
          {2,3, 11,2, 1,1},    // Wood & Logs
          {3,3, 4,1},          // Sand & Terracotta
          {12,3, 5,2}          // Metal & Nether
        },
        // Q1: size
        { {2,3, 9,2},          // Tiny
          {11,3, 10,2, 2,1},   // Cozy cottage
          {0,3, 1,2},          // Large fortress
          {12,2, 6,1}          // Massive cityscape
        },
        // Q2: atmosphere
        { {9,3, 2,2, 10,2},    // Cozy & Warm
          {0,3, 8,2},          // Grand & Imposing
          {9,2, 3,2, 6,2},     // Whimsical & Magical
          {5,3, 1,2}           // Dark & Mysterious
        },
        // Q3: landscape
        { {11,3, 4,2, 1,1},    // Forest & Jungle
          {8,3, 0,2},          // Mountain & Snow
          {7,3, 4,2, 3,1},     // Ocean & Tropics
          {3,3, 5,1}           // Desert & Mesa
        },
        // Q4: era
        { {0,3, 1,2},          // Medieval / Fantasy
          {3,3, 4,2},          // Ancient / Mystical
          {12,3, 5,1},         // Industrial / Steam
          {6,3, 7,1}           // Futuristic / Space
        },
        // Q5: what matters
        { {9,2, 11,2},         // Fine detail
          {0,2, 8,2},          // Impressive scale
          {10,3, 9,2},         // Bold color
          {5,2, 1,2, 3,1}      // Narrative
        }
    };

    // Theme indices: 0=fantasy  1=medieval  2=sci-fi  3=cozy
    //               4=industrial  5=gothic  6=nordic  7=tropical
    private static final String[] THEME_KEYS   = {
        "fantasy", "medieval", "sci-fi", "cozy", "industrial", "gothic", "nordic", "tropical"
    };
    private static final String[] THEME_LABELS = {
        "Fantasy", "Medieval", "Sci-Fi", "Cozy", "Industrial", "Gothic", "Nordic", "Tropical"
    };

    // THM_VOTES[q][a] = flat pairs of {themeIndex, score, ...}
    private static final int[][][] THM_VOTES = {
        // Q0: material
        { {5,2, 1,2},          // Stone  → gothic + medieval
          {3,3, 6,2},          // Wood   → cozy + nordic
          {0,1, 7,2},          // Sand   → fantasy + tropical
          {4,3, 2,2}           // Metal  → industrial + sci-fi
        },
        // Q1: size
        { {3,3},               // Tiny     → cozy
          {3,2, 6,1},          // Cottage  → cozy + nordic
          {1,2, 5,1},          // Fortress → medieval + gothic
          {4,2, 2,2}           // Massive  → industrial + sci-fi
        },
        // Q2: atmosphere
        { {3,3},               // Cozy      → cozy
          {1,2, 5,1},          // Grand     → medieval + gothic
          {0,3},               // Whimsical → fantasy
          {5,3, 4,1}           // Dark      → gothic + industrial
        },
        // Q3: landscape
        { {3,2, 7,1},          // Forest   → cozy + tropical
          {6,3, 1,1},          // Mountain → nordic + medieval
          {7,3},               // Ocean    → tropical
          {0,2, 7,1}           // Desert   → fantasy + tropical
        },
        // Q4: era
        { {1,3, 0,2},          // Medieval  → medieval + fantasy
          {0,3},               // Ancient   → fantasy
          {4,3},               // Industrial→ industrial
          {2,3, 4,1}           // Futuristic→ sci-fi + industrial
        },
        // Q5: what matters
        { {3,2, 1,1},          // Detail    → cozy + medieval
          {1,2, 5,1},          // Scale     → medieval + gothic
          {7,2, 0,1},          // Color     → tropical + fantasy
          {5,2, 0,1}           // Narrative → gothic + fantasy
        }
    };

    // ── Display data ──────────────────────────────────────────────────────────
    private static final String[] PALETTE_NAMES = {
        "Cobblestone Keep", "Dark Oak Manor", "Spruce Cabin", "Sandstone Temple",
        "Jungle Outpost", "Nether Fortress", "End Citadel", "Ocean Monument",
        "Ice Palace", "Cherry Blossom Garden", "Autumn Harvest", "Birch Forest Cottage",
        "Industrial Steam"
    };

    private static final int[] PALETTE_COLORS = {
        0xFF9A9A9A, 0xFF553322, 0xFFAA8855, 0xFFDDBB66,
        0xFF336633, 0xFFCC4422, 0xFF8855AA, 0xFF3366CC,
        0xFF99DDFF, 0xFFFFAABB, 0xFFCC8833, 0xFFCCDD99,
        0xFF887766
    };

    private static final int[] THEME_COLORS = {
        0xFF9966CC, // fantasy  - purple
        0xFFBB9944, // medieval - gold
        0xFF44BBCC, // sci-fi   - cyan
        0xFF88BB66, // cozy     - green
        0xFF996655, // industrial - red-brown
        0xFF664466, // gothic   - dark purple
        0xFF99BBDD, // nordic   - ice blue
        0xFF33BB88  // tropical - teal
    };

    // ── State ─────────────────────────────────────────────────────────────────
    private enum Phase { QUIZ, RESULT }
    private Phase phase = Phase.QUIZ;
    private int questionIdx = 0;
    private final int[] selected = new int[QUESTIONS.length];

    // Result
    private String resultPalette;
    private int    resultPaletteColor;
    private String resultThemeLabel;
    private int    resultThemeColor;
    private String resultPrompt;

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int PANEL_W = 290;
    private static final int PANEL_H = 240;
    private static final int OPT_H   = 26;
    private static final int OPT_GAP = 4;

    public StyleQuizScreen(Screen parent) {
        super(Component.literal("Style Quiz"));
        this.parent = parent;
        Arrays.fill(selected, -1);
    }

    // ── Widget init ───────────────────────────────────────────────────────────

    @Override
    protected void init() {
        if (phase == Phase.QUIZ) initQuiz();
        else initResult();
    }

    private void initQuiz() {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int panelX = cx - PANEL_W / 2;
        // Options start just below center-ish
        int optY = cy - 10;

        for (int i = 0; i < OPTIONS[questionIdx].length; i++) {
            final int idx = i;
            this.addRenderableWidget(Button.builder(
                Component.literal(OPTIONS[questionIdx][i]),
                btn -> selectOption(idx)
            ).bounds(panelX + 10, optY + i * (OPT_H + OPT_GAP), PANEL_W - 20, OPT_H).build());
        }

        int navY = optY + OPTIONS[questionIdx].length * (OPT_H + OPT_GAP) + 4;

        if (questionIdx > 0) {
            this.addRenderableWidget(Button.builder(
                Component.literal("← Back"),
                btn -> { questionIdx--; rebuildWidgets(); }
            ).bounds(cx - 120, navY, 60, 16).build());
        }

        this.addRenderableWidget(Button.builder(
            Component.literal("Close"),
            btn -> onClose()
        ).bounds(cx + 55, navY, 60, 16).build());
    }

    private void initResult() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        this.addRenderableWidget(Button.builder(
            Component.literal("Retake Quiz"),
            btn -> {
                phase = Phase.QUIZ;
                questionIdx = 0;
                Arrays.fill(selected, -1);
                rebuildWidgets();
            }
        ).bounds(cx - 105, cy + 84, 95, 18).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("Close"),
            btn -> onClose()
        ).bounds(cx + 10, cy + 84, 95, 18).build());
    }

    // ── Logic ─────────────────────────────────────────────────────────────────

    private void selectOption(int idx) {
        selected[questionIdx] = idx;
        if (questionIdx < QUESTIONS.length - 1) {
            questionIdx++;
            rebuildWidgets();
        } else {
            computeResult();
            phase = Phase.RESULT;
            rebuildWidgets();
        }
    }

    private void computeResult() {
        int[] palScore = new int[PALETTE_NAMES.length];
        int[] thmScore = new int[THEME_KEYS.length];

        for (int q = 0; q < QUESTIONS.length; q++) {
            int a = selected[q];
            if (a < 0 || q >= PAL_VOTES.length || a >= PAL_VOTES[q].length) continue;

            int[] pv = PAL_VOTES[q][a];
            for (int i = 0; i + 1 < pv.length; i += 2) {
                if (pv[i] >= 0 && pv[i] < palScore.length) palScore[pv[i]] += pv[i + 1];
            }

            int[] tv = THM_VOTES[q][a];
            for (int i = 0; i + 1 < tv.length; i += 2) {
                if (tv[i] >= 0 && tv[i] < thmScore.length) thmScore[tv[i]] += tv[i + 1];
            }
        }

        int bestPal = 0, bestThm = 0;
        for (int i = 1; i < palScore.length; i++) if (palScore[i] > palScore[bestPal]) bestPal = i;
        for (int i = 1; i < thmScore.length; i++) if (thmScore[i] > thmScore[bestThm]) bestThm = i;

        resultPalette      = PALETTE_NAMES[bestPal];
        resultPaletteColor = PALETTE_COLORS[bestPal];
        resultThemeLabel   = THEME_LABELS[bestThm];
        resultThemeColor   = THEME_COLORS[bestThm];
        resultPrompt       = PromptData.getRandomPrompt(THEME_KEYS[bestThm]);
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int panelX = cx - PANEL_W / 2;
        int panelY = cy - PANEL_H / 2;

        // Background
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, 0x88000000);
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + 1, 0x55FFD700);
        g.fill(panelX, panelY + PANEL_H - 1, panelX + PANEL_W, panelY + PANEL_H, 0x33FFD700);

        if (phase == Phase.QUIZ) renderQuiz(g, cx, cy, panelX, panelY);
        else renderResult(g, cx, cy, panelX, panelY);

        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    private void renderQuiz(GuiGraphicsExtractor g, int cx, int cy, int panelX, int panelY) {
        // Title
        g.centeredText(this.font, "Build Style Quiz", cx, panelY + 5, 0xFFD700);

        // Progress bar
        int pbX = panelX + 10, pbY = panelY + 16, pbW = PANEL_W - 36;
        g.fill(pbX, pbY, pbX + pbW, pbY + 4, 0x33FFFFFF);
        int filled = (int) ((float) questionIdx / QUESTIONS.length * pbW);
        g.fill(pbX, pbY, pbX + filled, pbY + 4, 0xFFFFD700);
        g.text(this.font, (questionIdx + 1) + "/" + QUESTIONS.length, pbX + pbW + 4, pbY, 0x888888);

        // Divider
        g.fill(panelX + 10, panelY + 24, panelX + PANEL_W - 10, panelY + 25, 0x22FFFFFF);

        // Question
        g.centeredText(this.font, QUESTIONS[questionIdx], cx, cy - 28, 0xFFFFFF);
        g.centeredText(this.font, "Click an answer to continue", cx, cy - 16, 0x555555);
    }

    private void renderResult(GuiGraphicsExtractor g, int cx, int cy, int panelX, int panelY) {
        g.centeredText(this.font, "Your Build Style", cx, panelY + 5, 0xFFD700);
        g.fill(panelX + 10, panelY + 16, panelX + PANEL_W - 10, panelY + 17, 0x33FFFFFF);

        int y = panelY + 24;

        // Palette recommendation
        g.text(this.font, "Recommended Palette", panelX + 14, y, 0x888888);
        y += 11;
        g.fill(panelX + 14, y + 1, panelX + 22, y + 9, resultPaletteColor);
        g.text(this.font, resultPalette, panelX + 27, y, resultPaletteColor);
        y += 18;

        // Theme recommendation
        g.text(this.font, "Recommended Theme", panelX + 14, y, 0x888888);
        y += 11;
        g.fill(panelX + 14, y + 1, panelX + 22, y + 9, resultThemeColor);
        g.text(this.font, resultThemeLabel, panelX + 27, y, resultThemeColor);
        y += 20;

        // Divider
        g.fill(panelX + 14, y, panelX + PANEL_W - 14, y + 1, 0x33FFFFFF);
        y += 8;

        // Prompt
        g.text(this.font, "Build Prompt Idea", panelX + 14, y, 0x888888);
        y += 11;
        for (String line : wrapText(resultPrompt, PANEL_W - 36)) {
            if (y > panelY + PANEL_H - 36) break;
            g.text(this.font, line, panelX + 14, y, 0xDDDDDD);
            y += 10;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
