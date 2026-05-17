# Architect's Notebook — Mod Development Log

## Project Overview
A Minecraft Fabric mod (for MC 26.1.2) that fills the missing "planning phase" gap.
Most mods help you *execute* a build. This mod helps you *plan* one — before a single real block is placed.

Press **B** in-game to open the Architect's Notebook dashboard.

---

## Current Status: Tier 3 — Complete ✓ + Auto-Variation Generator ✓ (11 screens live)

### Environment
- **Minecraft Version:** 26.1.2
- **Mod Loader:** Fabric (loader 0.19.2)
- **Fabric API:** 0.146.1+26.1.2
- **Java Version:** 21
- **Build System:** Gradle with Fabric Loom 1.16-SNAPSHOT
- **IDE:** IntelliJ IDEA
- **Package:** `name.modid.client` *(rename to proper namespace is deferred)*

---

## File Map

```
src/
  main/java/name/modid/
    BuildPlanner.java               ← server-side mod initializer (stock)
  client/java/name/modid/client/
    BuildPlannerClient.java         ← B-key GLFW polling, opens dashboard
    screen/
      BuildPlannerDashboard.java    ← main hub, 10 buttons
      BuildPromptScreen.java        ← prompt generator
      PaletteScreen.java            ← palette browser
      InspirationBrowserScreen.java ← inspiration browser
      ProjectNotesScreen.java       ← project notes & management
      BuildTimelineScreen.java      ← phase checklist
      FloorplanDesignerScreen.java  ← 2D grid painter
      BlueprintLibraryScreen.java   ← step-by-step blueprints
      BlockCalculatorScreen.java    ← block count calculator
      StyleQuizScreen.java          ← style quiz & recommender
      SilhouetteGeneratorScreen.java← silhouette visualizer
      AutoVariationScreen.java      ← palette auto-variation generator
    data/
      PromptData.java               ← 8 themes × 15 prompts
      PaletteData.java              ← 13 palettes
      InspirationData.java          ← 5 categories, 45 entries
      ProjectData.java              ← JSON CRUD for projects
      BlueprintData.java            ← 7 categories, 26 blueprints
```

---

## Screens — Full Reference

### `BuildPlannerDashboard`
- Main hub opened by the B key
- 10 active buttons, one per feature; Close at the bottom
- Dark panel with gold border + title/subtitle text

### `BuildPromptScreen`
- 8 theme tabs: Fantasy, Medieval, Sci-Fi, Cozy, Industrial, Gothic, Nordic, Tropical
- 15 prompts per theme (120 total) sourced from `PromptData`
- **Randomize** picks a random prompt from the selected theme
- **Prev / Next** navigate through history
- Detail panel renders the current prompt with word-wrap

### `PaletteScreen`
- 13 themed block palettes sourced from `PaletteData`
- Scrollable list on left; detail panel on right with:
  - Color swatch (filled rectangle) per block
  - Block name + hex color code
- **Random Palette** button picks a random one

### `InspirationBrowserScreen`
- 5 category tabs: Roofs, Windows, Shapes, Walls, Interiors
- Scrollable list of entries (45 total) on left
- Detail panel on right shows title, description, and bulleted tips
- Mouse-wheel scroll on both panels independently

### `ProjectNotesScreen`
- Create named projects with: theme, biome, free-text notes
- Projects saved as JSON to `architects_notebook/projects/<name>.json` via `ProjectData`
- Left panel: scrollable project list + Delete button
- Right panel: EditBox fields (name, theme, biome, notes); **Enable Editing** toggle; **Save Project**

### `BuildTimelineScreen`
- Per-project phase checklist loaded from the same JSON as `ProjectNotesScreen`
- Click a phase checkbox to toggle done/undone
- ✕ button deletes a phase
- Progress bar fills proportionally to completed phases
- **Add Phase** EditBox + button appends new phases
- Auto-saves on every change via `ProjectData`

### `FloorplanDesignerScreen`
- 2D top-down grid painter, default 20×20, resize up to 60×60
- 8 brush types: EMPTY · FLOOR · WALL · DOOR · WINDOW · STAIRS · WATER · GARDEN
- **Custom per-brush color picker:** click the brush color swatch in the toolbar to open a 48-color popup (6 rows × 8 cols: greys, browns, reds, orange/yellow, greens, blues); click to apply; click outside to dismiss
- Click-and-drag paints; drag closes the color picker
- **+** / **−** buttons resize the grid
- Grid saved as JSON to `architects_notebook/floorplan.json`; auto-saves on close

### `BlueprintLibraryScreen`
- 7 category tabs: Towers, Walls, Gatehouses, Bridges, Foundations, Interiors, Roofs
- 26 total blueprints sourced from `BlueprintData`
- Left list shows difficulty dot (green/yellow/red) + name
- Detail panel: name, dimensions, material, estimated blocks, description, numbered steps
- Both panels independently mouse-wheel scrollable

### `BlockCalculatorScreen`
- 7 shape modes selectable via two rows of small buttons:
  - **Hollow Box** — floor + ceiling + four walls broken out separately; shows interior air volume
  - **Solid Box** — W × H × D total
  - **Floor/Ceiling** — full blocks vs. slabs for a flat plane
  - **4 Walls** — N/S + E/W walls with corner deduplication
  - **Gabled Roof** — stair-step layer breakdown by width
  - **Cylinder** — midpoint circle algorithm; full and hollow ring per layer + full height
  - **Arch** — elliptical arch curve; block count by column height × thickness
- 4 EditBox inputs: Width, Height, Depth, Wall thickness — live recalculate on each keystroke
- Right panel: labeled result rows (right-aligned counts with K/M shorthand)
- Stacked color bar at panel bottom showing material proportions
- Error messages for invalid or oversized thickness

### `StyleQuizScreen`
- 6 multiple-choice questions: material → size → atmosphere → landscape → era → priority
- Each answer casts weighted votes across all 13 palettes and all 8 themes simultaneously
- **← Back** navigates to the previous question
- After question 6: tallies scores, shows:
  - Recommended palette (name + color swatch)
  - Recommended theme (name + color swatch)
  - A random build prompt from `PromptData` matching the recommended theme
- **Retake Quiz** resets state and runs again (new prompt drawn each time)

### `SilhouetteGeneratorScreen`
- 10 building archetype buttons (left panel): Castle, Tower, Cottage, Cathedral, Nordic Hall, Pagoda, Lighthouse, Gatehouse, Manor, Wizard Tower
- Each archetype generates a **40-column height profile** using integer math:
  - Walls, towers, battlements, pitched roofs, tiered floors, conical hats — all built as column heights
  - ±2 noise applied per column for natural variation
- **↻ Randomize** picks a new seed → re-runs the profile, different shape each time
- Rendered as a **pixel-art silhouette** (`g.fill()`, 4px wide × 2px tall per unit) against a dark sky with horizon glow
- Each archetype has a distinct thematic color; top edge brightened for depth
- Style notes below describe materials + real build tips for that archetype
- Seed number shown bottom-right of the drawing area

---

## Data Classes — Full Reference

### `PromptData`
- `ThemeData` record: `displayName`, `List<String> prompts`
- `getThemeKeys()` — ordered list of 8 keys
- `getTheme(key)` — returns `ThemeData`
- `getRandomPrompt(themeKey)` — random prompt from a theme
- `getRandomPromptAny()` — random prompt from any theme, prefixed with theme name

### `PaletteData`
- 13 palettes each with a name and list of `{blockName, hexColor}` entries
- `getPaletteNames()`, `getPalette(name)` — list and lookup

### `InspirationData`
- `Entry` record: `title`, `description`, `List<String> tips`
- `CATEGORIES` list of 5 category names
- `getEntries(category)` — returns all entries for a category

### `ProjectData`
- Inner classes: `Project` (name, theme, biome, notes, createdDate, `List<TimelinePhase>` phases), `TimelinePhase` (name, done)
- `saveProject(Project)` — writes to `architects_notebook/projects/<sanitized_name>.json`
- `loadProject(name)`, `listProjectNames()`, `deleteProject(name)`, `projectExists(name)`
- `sanitize(name)` — strips illegal filename characters

### `BlueprintData`
- `Blueprint` record: category, name, dimensions, material, blockEstimate, difficulty (1/2/3), description, `List<String> steps`
- 26 blueprints across 7 categories
- `getCategory(String)` switch returns list; `CATEGORIES` ordered list

---

## Planned Feature Roadmap

### Tier 1 — ✓ Complete
- [x] Build Prompt Generator
- [x] Block Palette Recommender
- [x] Inspiration Browser
- [x] Build Projects & Notes
- [x] Build Timeline

### Tier 2 — ✓ Complete
- [x] Floorplan Designer — 2D grid; 8 brush types; per-brush color picker; 60×60 max; auto-save
- [x] Blueprint Library — 7 categories / 26 blueprints; difficulty; scrollable steps
- [ ] Site Analyzer *(deferred — requires world access API)*

### Tier 3 — ✓ Complete
- [x] Block Calculator — 7 shapes; live inputs; stacked bar chart; K/M formatting
- [x] Style Quiz — 6 questions; weighted scoring → palette + theme + prompt
- [x] Silhouette Generator — 10 archetypes; seeded noise; pixel-art render; style notes
- [x] Auto-Variation Generator — pick base palette → 4 mood-tagged variations; cyan highlights changed blocks
- [ ] 3D Sketch Mode *(requires block placement API — deferred)*

---

## Possible Next Session Work
1. **Auto-Variation Generator** — select a palette; generate 3–5 stylistic variations with altered secondary blocks and accent choices; display side-by-side swatches
2. **Package rename** — `name.modid` → `dev.icrabtree.architectsnotebook` (affects all Java files + `fabric.mod.json`)
3. **`fabric.mod.json` polish** — mod name, description, authors, icon, homepage
4. **Site Analyzer** — if world-access API can be safely reached from client side

---

## Build Notes & Lessons Learned — MC 26.1.2 + Fabric

### build.gradle rules
- Use plain `implementation` for `fabric-loader` and `fabric-api` — `modImplementation` does NOT exist in this Loom version
- Do NOT add a `mappings` line — MC 26.x bundles official mappings automatically; adding `loom.officialMojangMappings()` crashes with "non-obfuscated environment"

### Screen / GUI API
MC 26.1.2 uses a completely new render pipeline. Tutorials for older versions are wrong.

| What you need | Correct API |
|---|---|
| Screen base class | `net.minecraft.client.gui.screens.Screen` |
| Button | `net.minecraft.client.gui.components.Button` |
| Text component | `net.minecraft.network.chat.Component` |
| Render method override | `extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick)` |
| Add interactive widget | `this.addRenderableWidget(...)` |
| Draw centered text | `g.centeredText(this.font, "text", x, y, 0xRRGGBB)` |
| Draw left-aligned text | `g.text(this.font, "text", x, y, 0xRRGGBB)` |
| Fill rectangle | `g.fill(x1, y1, x2, y2, 0xAARRGGBB)` |
| `this.client` | `this.minecraft` |
| `this.textRenderer` | `this.font` |
| `close()` | `onClose()` |
| `addDrawableChild()` | `addRenderableWidget()` |

### Mouse input API
```java
// CORRECT — MC 26.x signature:
@Override
public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
    double x = event.x();
    double y = event.y();
    int button = event.button(); // 0=left, 1=right, 2=middle
    return super.mouseClicked(event, consumed);
}

// WRONG — old signature (compile error in 26.x):
// public boolean mouseClicked(double x, double y, int button)

// Drag:
// mouseDragged(MouseButtonEvent event, double deltaX, double deltaY)

// Scroll:
// mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
```

### DO NOT call extractBackground() manually
Called automatically before `extractRenderState()`. Calling it again causes:
`IllegalStateException: Can only blur once per frame` → game crash.

### Keybinding
`KeyBindingHelper` does not exist in Fabric API 0.146.1+26.1.2. Use GLFW polling:
```java
ClientTickEvents.END_CLIENT_TICK.register(client -> {
    boolean isDown = GLFW.glfwGetKey(client.getWindow().handle(), GLFW.GLFW_KEY_B) == GLFW.GLFW_PRESS;
    if (isDown && !keyWasDown) { /* open screen */ }
    keyWasDown = isDown;
});
```
- `client.getWindow().handle()` — correct (`long` GLFW pointer)
- `client.getWindow().getWindow()` — does NOT exist
