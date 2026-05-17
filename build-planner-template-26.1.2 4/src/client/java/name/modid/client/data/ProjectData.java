package name.modid.client.data;

import com.google.gson.*;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProjectData {

    public static class TimelinePhase {
        public String name;
        public boolean done;

        public TimelinePhase(String name, boolean done) {
            this.name = name;
            this.done = done;
        }
    }

    public static class Project {
        public String name;
        public String theme;
        public String biome;
        public String notes;
        public String createdDate;
        public List<TimelinePhase> phases;

        public Project(String name) {
            this.name = name;
            this.theme = "Unset";
            this.biome = "Unset";
            this.notes = "";
            this.createdDate = LocalDate.now().toString();
            this.phases = new ArrayList<>();
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // ── File paths ────────────────────────────────────────────────────────────

    private static Path getProjectsDir() {
        Minecraft mc = Minecraft.getInstance();
        Path gameDir = mc.gameDirectory.toPath();
        return gameDir.resolve("architects_notebook").resolve("projects");
    }

    private static Path getProjectFile(String projectName) {
        return getProjectsDir().resolve(sanitize(projectName) + ".json");
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase();
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public static void saveProject(Project project) {
        try {
            Path dir = getProjectsDir();
            Files.createDirectories(dir);
            Path file = getProjectFile(project.name);
            Files.writeString(file, GSON.toJson(project));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Project loadProject(String name) {
        try {
            Path file = getProjectFile(name);
            if (Files.exists(file)) {
                String json = Files.readString(file);
                return GSON.fromJson(json, Project.class);
            }
        } catch (IOException | JsonSyntaxException e) {
            e.printStackTrace();
        }
        return new Project(name);
    }

    public static void deleteProject(String name) {
        try {
            Path file = getProjectFile(name);
            Files.deleteIfExists(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> listProjectNames() {
        List<String> names = new ArrayList<>();
        try {
            Path dir = getProjectsDir();
            if (!Files.exists(dir)) return names;
            try (var stream = Files.list(dir)) {
                stream
                    .filter(p -> p.toString().endsWith(".json"))
                    .sorted()
                    .forEach(p -> {
                        String fname = p.getFileName().toString();
                        names.add(fname.substring(0, fname.length() - 5));
                    });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return names;
    }

    public static boolean projectExists(String name) {
        return Files.exists(getProjectFile(name));
    }
}
