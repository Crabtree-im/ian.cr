package name.modid.client.screen;

import name.modid.client.data.ProjectData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ProjectNotesScreen extends Screen {

    private final Screen parent;

    // State
    private List<String> projectNames;
    private int selectedProjectIndex = -1;
    private ProjectData.Project loadedProject = null;

    // Widgets
    private EditBox newProjectNameBox;
    private EditBox notesBox;
    private EditBox themeBox;
    private EditBox biomeBox;

    private static final int LEFT_W = 150;
    private static final int LEFT_X = 10;
    private static final int RIGHT_X = 168;
    private int listScrollOffset = 0;
    private static final int ENTRY_H = 20;

    public ProjectNotesScreen(Screen parent) {
        super(Component.literal("Build Projects"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.projectNames = ProjectData.listProjectNames();
        int rightW = this.width - RIGHT_X - 10;
        int panelTop = 44;
        int panelBottom = this.height - 28;

        // ── Left panel: new project name box + create button ──────────────────
        this.newProjectNameBox = new EditBox(this.font,
            LEFT_X, panelTop, LEFT_W, 18,
            Component.literal("Project name..."));
        this.newProjectNameBox.setMaxLength(40);
        this.newProjectNameBox.setHint(Component.literal("New project name..."));
        this.addRenderableWidget(this.newProjectNameBox);

        this.addRenderableWidget(Button.builder(
            Component.literal("+ Create"),
            btn -> createProject()
        ).bounds(LEFT_X, panelTop + 20, LEFT_W / 2 - 2, 18).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("✕ Delete"),
            btn -> deleteProject()
        ).bounds(LEFT_X + LEFT_W / 2 + 2, panelTop + 20, LEFT_W / 2 - 2, 18).build());

        // Scroll buttons for list
        this.addRenderableWidget(Button.builder(
            Component.literal("▲"),
            btn -> { if (listScrollOffset > 0) listScrollOffset--; }
        ).bounds(LEFT_X, panelBottom - 20, LEFT_W / 2 - 2, 18).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("▼"),
            btn -> { if (listScrollOffset < projectNames.size() - visibleRows()) listScrollOffset++; }
        ).bounds(LEFT_X + LEFT_W / 2 + 2, panelBottom - 20, LEFT_W / 2 - 2, 18).build());

        // ── Right panel: detail fields ────────────────────────────────────────
        int fy = panelTop + 14;

        this.themeBox = new EditBox(this.font, RIGHT_X, fy, rightW, 16, Component.literal("Theme"));
        this.themeBox.setMaxLength(60);
        this.themeBox.setEditable(false);
        this.addRenderableWidget(this.themeBox);
        fy += 28;

        this.biomeBox = new EditBox(this.font, RIGHT_X, fy, rightW, 16, Component.literal("Biome"));
        this.biomeBox.setMaxLength(60);
        this.biomeBox.setEditable(false);
        this.addRenderableWidget(this.biomeBox);
        fy += 28;

        int notesH = panelBottom - fy - 26;
        this.notesBox = new EditBox(this.font, RIGHT_X, fy, rightW, notesH, Component.literal("Notes"));
        this.notesBox.setMaxLength(2000);
        this.notesBox.setEditable(false);
        this.addRenderableWidget(this.notesBox);

        // Save + Edit toggles
        this.addRenderableWidget(Button.builder(
            Component.literal("Enable Editing"),
            btn -> {
                if (loadedProject != null) {
                    themeBox.setEditable(true);
                    biomeBox.setEditable(true);
                    notesBox.setEditable(true);
                }
            }
        ).bounds(RIGHT_X, panelBottom - 22, rightW / 2 - 2, 18).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("Save Project"),
            btn -> saveCurrentProject()
        ).bounds(RIGHT_X + rightW / 2 + 2, panelBottom - 22, rightW / 2 - 2, 18).build());

        // Back button
        this.addRenderableWidget(Button.builder(
            Component.literal("← Back"),
            btn -> this.onClose()
        ).bounds(8, this.height - 22, 80, 18).build());
    }

    private int visibleRows() {
        int listTop = 44 + 42;
        int listBottom = this.height - 50;
        return Math.max(1, (listBottom - listTop) / ENTRY_H);
    }

    private void createProject() {
        String name = newProjectNameBox.getValue().trim();
        if (name.isEmpty()) return;
        ProjectData.Project p = new ProjectData.Project(name);
        ProjectData.saveProject(p);
        this.projectNames = ProjectData.listProjectNames();
        newProjectNameBox.setValue("");
        // Select newly created project
        String sanitized = name.replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase();
        selectedProjectIndex = projectNames.indexOf(sanitized);
        loadProject();
    }

    private void deleteProject() {
        if (selectedProjectIndex < 0 || selectedProjectIndex >= projectNames.size()) return;
        ProjectData.deleteProject(projectNames.get(selectedProjectIndex));
        this.projectNames = ProjectData.listProjectNames();
        selectedProjectIndex = -1;
        loadedProject = null;
        clearFields();
    }

    private void loadProject() {
        if (selectedProjectIndex < 0 || selectedProjectIndex >= projectNames.size()) return;
        String name = projectNames.get(selectedProjectIndex);
        loadedProject = ProjectData.loadProject(name);
        themeBox.setValue(loadedProject.theme != null ? loadedProject.theme : "");
        biomeBox.setValue(loadedProject.biome != null ? loadedProject.biome : "");
        notesBox.setValue(loadedProject.notes != null ? loadedProject.notes : "");
        themeBox.setEditable(false);
        biomeBox.setEditable(false);
        notesBox.setEditable(false);
    }

    private void saveCurrentProject() {
        if (loadedProject == null) return;
        loadedProject.theme = themeBox.getValue();
        loadedProject.biome = biomeBox.getValue();
        loadedProject.notes = notesBox.getValue();
        ProjectData.saveProject(loadedProject);
        themeBox.setEditable(false);
        biomeBox.setEditable(false);
        notesBox.setEditable(false);
    }

    private void clearFields() {
        themeBox.setValue("");
        biomeBox.setValue("");
        notesBox.setValue("");
        themeBox.setEditable(false);
        biomeBox.setEditable(false);
        notesBox.setEditable(false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int panelTop = 44;
        int panelBottom = this.height - 28;
        int listTop = panelTop + 42;
        int listBottom = panelBottom - 22;
        int rightW = this.width - RIGHT_X - 10;

        // Header
        g.centeredText(this.font, "Build Projects", this.width / 2, 8, 0xFFD700);

        // Left panel
        g.fill(LEFT_X, panelTop, LEFT_X + LEFT_W, panelBottom, 0x88000000);
        g.fill(LEFT_X, panelTop, LEFT_X + LEFT_W, panelTop + 1, 0x44FFFFFF);
        g.text(this.font, "Projects:", LEFT_X + 2, panelTop - 10, 0xAAAAAA);

        // Project list
        g.fill(LEFT_X, listTop, LEFT_X + LEFT_W, listBottom, 0x55000000);
        int visible = visibleRows();
        for (int i = 0; i < visible; i++) {
            int idx = i + listScrollOffset;
            if (idx >= projectNames.size()) break;
            String name = projectNames.get(idx);
            int ey = listTop + i * ENTRY_H;
            boolean selected = idx == selectedProjectIndex;
            boolean hovered = mouseX >= LEFT_X && mouseX <= LEFT_X + LEFT_W
                && mouseY >= ey && mouseY < ey + ENTRY_H;
            if (selected) {
                g.fill(LEFT_X + 1, ey, LEFT_X + LEFT_W - 1, ey + ENTRY_H, 0x88336699);
            } else if (hovered) {
                g.fill(LEFT_X + 1, ey, LEFT_X + LEFT_W - 1, ey + ENTRY_H, 0x44336699);
            }
            String displayName = name.length() > 18 ? name.substring(0, 17) + "…" : name;
            g.text(this.font, displayName, LEFT_X + 4, ey + 6, selected ? 0xFFD700 : 0xCCCCCC);
        }

        // Empty state
        if (projectNames.isEmpty()) {
            g.centeredText(this.font, "No projects yet.", LEFT_X + LEFT_W / 2, listTop + 30, 0x555555);
            g.centeredText(this.font, "Type a name above", LEFT_X + LEFT_W / 2, listTop + 42, 0x444444);
            g.centeredText(this.font, "and click + Create.", LEFT_X + LEFT_W / 2, listTop + 54, 0x444444);
        }

        // Right panel
        g.fill(RIGHT_X, panelTop, RIGHT_X + rightW, panelBottom, 0x88000000);
        g.fill(RIGHT_X, panelTop, RIGHT_X + rightW, panelTop + 1, 0x44FFFFFF);

        if (loadedProject != null) {
            g.text(this.font, "Project: " + loadedProject.name, RIGHT_X + 4, panelTop - 10, 0xFFD700);
            g.text(this.font, "Created: " + loadedProject.createdDate, RIGHT_X + 4 + this.font.width("Project: " + loadedProject.name) + 12, panelTop - 10, 0x555555);
        } else {
            g.centeredText(this.font, "Select a project", RIGHT_X + rightW / 2, panelTop + 30, 0x555555);
        }

        // Field labels
        int fy = panelTop + 4;
        g.text(this.font, "Theme:", RIGHT_X + 2, fy, 0x88CCFF);
        fy += 28;
        g.text(this.font, "Biome:", RIGHT_X + 2, fy, 0x88CCFF);
        fy += 28;
        g.text(this.font, "Notes:", RIGHT_X + 2, fy, 0x88CCFF);

        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        double mouseX = event.x();
        double mouseY = event.y();
        int listTop = 44 + 42;
        int listBottom = this.height - 50;

        if (mouseX >= LEFT_X && mouseX <= LEFT_X + LEFT_W
            && mouseY >= listTop && mouseY < listBottom) {
            int row = (int)((mouseY - listTop) / ENTRY_H);
            int idx = row + listScrollOffset;
            if (idx >= 0 && idx < projectNames.size()) {
                selectedProjectIndex = idx;
                loadProject();
                return true;
            }
        }
        return super.mouseClicked(event, consumed);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int listTop = 44 + 42;
        if (mouseX >= LEFT_X && mouseX <= LEFT_X + LEFT_W && mouseY >= listTop) {
            if (scrollY < 0) { if (listScrollOffset < projectNames.size() - visibleRows()) listScrollOffset++; }
            else { if (listScrollOffset > 0) listScrollOffset--; }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
