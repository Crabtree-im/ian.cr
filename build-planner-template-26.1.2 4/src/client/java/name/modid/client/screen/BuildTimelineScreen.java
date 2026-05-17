package name.modid.client.screen;

import name.modid.client.data.ProjectData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class BuildTimelineScreen extends Screen {

    private final Screen parent;

    private List<String> projectNames;
    private int selectedProjectIndex = -1;
    private ProjectData.Project loadedProject = null;

    private EditBox newPhaseBox;
    private int phaseScrollOffset = 0;

    private static final int LEFT_W = 150;
    private static final int LEFT_X = 10;
    private static final int RIGHT_X = 168;
    private static final int PROJ_ENTRY_H = 20;
    private static final int PHASE_ENTRY_H = 22;
    private int projScrollOffset = 0;

    public BuildTimelineScreen(Screen parent) {
        super(Component.literal("Build Timeline"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.projectNames = ProjectData.listProjectNames();
        int panelTop = 44;
        int panelBottom = this.height - 28;
        int rightW = this.width - RIGHT_X - 10;

        // Project list scroll
        this.addRenderableWidget(Button.builder(
            Component.literal("▲"),
            btn -> { if (projScrollOffset > 0) projScrollOffset--; }
        ).bounds(LEFT_X, panelBottom - 20, LEFT_W / 2 - 2, 18).build());
        this.addRenderableWidget(Button.builder(
            Component.literal("▼"),
            btn -> { if (projScrollOffset < projectNames.size() - visibleProjRows()) projScrollOffset++; }
        ).bounds(LEFT_X + LEFT_W / 2 + 2, panelBottom - 20, LEFT_W / 2 - 2, 18).build());

        // New phase input
        this.newPhaseBox = new EditBox(this.font,
            RIGHT_X, panelTop, rightW - 72, 18,
            Component.literal("Phase name..."));
        this.newPhaseBox.setMaxLength(60);
        this.newPhaseBox.setHint(Component.literal("New phase or task..."));
        this.addRenderableWidget(this.newPhaseBox);

        this.addRenderableWidget(Button.builder(
            Component.literal("+ Add Phase"),
            btn -> addPhase()
        ).bounds(RIGHT_X + rightW - 70, panelTop, 70, 18).build());

        // Phase scroll
        this.addRenderableWidget(Button.builder(
            Component.literal("▲"),
            btn -> { if (phaseScrollOffset > 0) phaseScrollOffset--; }
        ).bounds(RIGHT_X, panelBottom - 20, rightW / 2 - 2, 18).build());
        this.addRenderableWidget(Button.builder(
            Component.literal("▼"),
            btn -> { if (loadedProject != null && phaseScrollOffset < loadedProject.phases.size() - visiblePhaseRows()) phaseScrollOffset++; }
        ).bounds(RIGHT_X + rightW / 2 + 2, panelBottom - 20, rightW / 2 - 2, 18).build());

        // Back
        this.addRenderableWidget(Button.builder(
            Component.literal("← Back"),
            btn -> this.onClose()
        ).bounds(8, this.height - 22, 80, 18).build());
    }

    private int visibleProjRows() {
        int listTop = 44;
        int listBottom = this.height - 50;
        return Math.max(1, (listBottom - listTop) / PROJ_ENTRY_H);
    }

    private int visiblePhaseRows() {
        int phaseTop = 44 + 22;
        int phaseBottom = this.height - 50;
        return Math.max(1, (phaseBottom - phaseTop) / PHASE_ENTRY_H);
    }

    private void addPhase() {
        if (loadedProject == null) return;
        String name = newPhaseBox.getValue().trim();
        if (name.isEmpty()) return;
        if (loadedProject.phases == null) loadedProject.phases = new ArrayList<>();
        loadedProject.phases.add(new ProjectData.TimelinePhase(name, false));
        ProjectData.saveProject(loadedProject);
        newPhaseBox.setValue("");
    }

    private void togglePhase(int idx) {
        if (loadedProject == null || loadedProject.phases == null) return;
        if (idx < 0 || idx >= loadedProject.phases.size()) return;
        loadedProject.phases.get(idx).done = !loadedProject.phases.get(idx).done;
        ProjectData.saveProject(loadedProject);
    }

    private void deletePhase(int idx) {
        if (loadedProject == null || loadedProject.phases == null) return;
        if (idx < 0 || idx >= loadedProject.phases.size()) return;
        loadedProject.phases.remove(idx);
        if (phaseScrollOffset >= loadedProject.phases.size()) phaseScrollOffset = Math.max(0, loadedProject.phases.size() - 1);
        ProjectData.saveProject(loadedProject);
    }

    private void selectProject(int idx) {
        selectedProjectIndex = idx;
        if (idx < 0 || idx >= projectNames.size()) {
            loadedProject = null;
            return;
        }
        loadedProject = ProjectData.loadProject(projectNames.get(idx));
        if (loadedProject.phases == null) loadedProject.phases = new ArrayList<>();
        phaseScrollOffset = 0;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int panelTop = 44;
        int panelBottom = this.height - 28;
        int listTop = panelTop;
        int listBottom = panelBottom - 22;
        int rightW = this.width - RIGHT_X - 10;
        int phaseTop = panelTop + 22;
        int phaseBottom = panelBottom - 22;

        // Header
        g.centeredText(this.font, "Build Timeline", this.width / 2, 8, 0xFFD700);

        // Left panel — project list
        g.fill(LEFT_X, listTop, LEFT_X + LEFT_W, panelBottom, 0x88000000);
        g.fill(LEFT_X, listTop, LEFT_X + LEFT_W, listTop + 1, 0x44FFFFFF);
        g.text(this.font, "Projects:", LEFT_X + 2, listTop - 10, 0xAAAAAA);

        int visible = visibleProjRows();
        for (int i = 0; i < visible; i++) {
            int idx = i + projScrollOffset;
            if (idx >= projectNames.size()) break;
            String name = projectNames.get(idx);
            int ey = listTop + i * PROJ_ENTRY_H;
            boolean selected = idx == selectedProjectIndex;
            boolean hovered = mouseX >= LEFT_X && mouseX <= LEFT_X + LEFT_W
                && mouseY >= ey && mouseY < ey + PROJ_ENTRY_H;
            if (selected) g.fill(LEFT_X + 1, ey, LEFT_X + LEFT_W - 1, ey + PROJ_ENTRY_H, 0x88336699);
            else if (hovered) g.fill(LEFT_X + 1, ey, LEFT_X + LEFT_W - 1, ey + PROJ_ENTRY_H, 0x44336699);
            String disp = name.length() > 18 ? name.substring(0, 17) + "…" : name;
            g.text(this.font, disp, LEFT_X + 4, ey + 6, selected ? 0xFFD700 : 0xCCCCCC);
        }

        if (projectNames.isEmpty()) {
            g.centeredText(this.font, "No projects.", LEFT_X + LEFT_W / 2, listTop + 30, 0x444444);
            g.centeredText(this.font, "Create one in", LEFT_X + LEFT_W / 2, listTop + 42, 0x444444);
            g.centeredText(this.font, "Project Notes.", LEFT_X + LEFT_W / 2, listTop + 54, 0x444444);
        }

        // Right panel — phases
        g.fill(RIGHT_X, panelTop, RIGHT_X + rightW, panelBottom, 0x88000000);
        g.fill(RIGHT_X, panelTop, RIGHT_X + rightW, panelTop + 1, 0x44FFFFFF);

        if (loadedProject == null) {
            g.centeredText(this.font, "Select a project to see its timeline.", RIGHT_X + rightW / 2, panelTop + 40, 0x555555);
        } else {
            // Project name header
            g.text(this.font, loadedProject.name, RIGHT_X + 4, panelTop - 10, 0xFFD700);

            List<ProjectData.TimelinePhase> phases = loadedProject.phases;
            int total = phases != null ? phases.size() : 0;
            int done = 0;
            if (phases != null) { for (var ph : phases) if (ph.done) done++; }

            // Progress bar
            g.text(this.font, done + "/" + total + " phases complete", RIGHT_X + 4, phaseTop - 14, 0xAAAAFF);
            if (total > 0) {
                int barW = rightW - 8;
                int filled = (int)(barW * ((float) done / total));
                g.fill(RIGHT_X + 4, phaseTop - 4, RIGHT_X + 4 + barW, phaseTop - 4 + 4, 0x55333333);
                g.fill(RIGHT_X + 4, phaseTop - 4, RIGHT_X + 4 + filled, phaseTop - 4 + 4, 0xFF44BB44);
            }

            // Phase entries
            g.fill(RIGHT_X, phaseTop, RIGHT_X + rightW, phaseBottom, 0x33000000);
            if (phases == null || phases.isEmpty()) {
                g.centeredText(this.font, "No phases yet. Add one above!", RIGHT_X + rightW / 2, phaseTop + 30, 0x555555);
            } else {
                int visRows = visiblePhaseRows();
                for (int i = 0; i < visRows; i++) {
                    int idx = i + phaseScrollOffset;
                    if (idx >= phases.size()) break;
                    ProjectData.TimelinePhase phase = phases.get(idx);
                    int py = phaseTop + i * PHASE_ENTRY_H;
                    boolean hovered = mouseX >= RIGHT_X && mouseX <= RIGHT_X + rightW
                        && mouseY >= py && mouseY < py + PHASE_ENTRY_H;
                    if (hovered) g.fill(RIGHT_X + 1, py, RIGHT_X + rightW - 1, py + PHASE_ENTRY_H, 0x22FFFFFF);

                    // Checkbox
                    int checkColor = phase.done ? 0xFF44BB44 : 0xFF666666;
                    g.fill(RIGHT_X + 6, py + 5, RIGHT_X + 16, py + 15, checkColor);
                    if (phase.done) {
                        g.text(this.font, "✓", RIGHT_X + 8, py + 5, 0xFFFFFF);
                    }

                    // Phase name
                    int nameColor = phase.done ? 0x668866 : 0xEEEEEE;
                    String nameText = phase.done ? "§m" + phase.name : phase.name;
                    g.text(this.font, phase.name, RIGHT_X + 20, py + 6, nameColor);

                    // Delete button area (small x on right)
                    g.text(this.font, "✕", RIGHT_X + rightW - 14, py + 6, 0x774444);
                }
            }
        }

        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        double mouseX = event.x();
        double mouseY = event.y();
        int panelTop = 44;
        int listTop = panelTop;
        int listBottom = this.height - 50;
        int rightW = this.width - RIGHT_X - 10;
        int phaseTop = panelTop + 22;
        int phaseBottom = this.height - 50;

        // Click on project list
        if (mouseX >= LEFT_X && mouseX <= LEFT_X + LEFT_W
            && mouseY >= listTop && mouseY < listBottom) {
            int row = (int)((mouseY - listTop) / PROJ_ENTRY_H);
            int idx = row + projScrollOffset;
            if (idx >= 0 && idx < projectNames.size()) {
                selectProject(idx);
                return true;
            }
        }

        // Click on phase list
        if (loadedProject != null && loadedProject.phases != null
            && mouseX >= RIGHT_X && mouseX <= RIGHT_X + rightW
            && mouseY >= phaseTop && mouseY < phaseBottom) {
            int row = (int)((mouseY - phaseTop) / PHASE_ENTRY_H);
            int idx = row + phaseScrollOffset;
            if (idx >= 0 && idx < loadedProject.phases.size()) {
                // Check if clicking the delete X
                if (mouseX >= RIGHT_X + rightW - 18) {
                    deletePhase(idx);
                } else {
                    // Toggle done
                    togglePhase(idx);
                }
                return true;
            }
        }

        return super.mouseClicked(event, consumed);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int panelTop = 44;
        int phaseTop = panelTop + 22;

        if (mouseX >= RIGHT_X && mouseY >= phaseTop) {
            if (scrollY < 0) { if (loadedProject != null && phaseScrollOffset < loadedProject.phases.size() - visiblePhaseRows()) phaseScrollOffset++; }
            else { if (phaseScrollOffset > 0) phaseScrollOffset--; }
            return true;
        }
        if (mouseX >= LEFT_X && mouseX <= LEFT_X + LEFT_W) {
            if (scrollY < 0) { if (projScrollOffset < projectNames.size() - visibleProjRows()) projScrollOffset++; }
            else { if (projScrollOffset > 0) projScrollOffset--; }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
