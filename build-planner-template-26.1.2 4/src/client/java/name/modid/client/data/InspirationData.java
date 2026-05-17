package name.modid.client.data;

import java.util.List;

public class InspirationData {

    public record InspirationEntry(String category, String name, String description, List<String> tips) {}

    // ── ROOF TYPES ────────────────────────────────────────────────────────────
    public static final List<InspirationEntry> ROOF_TYPES = List.of(
        new InspirationEntry("Roof", "Gabled Roof",
            "The classic A-frame shape. Two sloping sides meet at a central ridge.",
            List.of("Use stair blocks for the slope", "Add a small overhang (1 block) for realism", "Spruce or dark oak stairs work great for rustic builds")),
        new InspirationEntry("Roof", "Hipped Roof",
            "All four sides slope downward. More complex but very polished.",
            List.of("Build the ridge shorter than the base", "Each corner needs a diagonal stair run", "Good for manor houses and large builds")),
        new InspirationEntry("Roof", "Mansard Roof",
            "Two slopes per side — steep lower, shallow upper. Classic French style.",
            List.of("Lower slope is nearly vertical, use full blocks or steep stairs", "Upper roof is nearly flat", "Great with blackstone or dark oak")),
        new InspirationEntry("Roof", "Gambrel Roof",
            "Like a barn roof — two pitches per side, flared at the bottom.",
            List.of("Lower section is steeper, upper is shallower", "Use a different material for each section", "Perfect for farmhouses and barns")),
        new InspirationEntry("Roof", "Flat Roof",
            "No pitch. Common in desert, modern, and sci-fi builds.",
            List.of("Add parapet walls around the edge", "Use slabs to give a slight visual thickness", "Place details on top: skylights, vents, gardens")),
        new InspirationEntry("Roof", "Dome",
            "A rounded roof. Requires stacking rings of decreasing diameter.",
            List.of("Use a sphere calculator to plan ring sizes", "Quartz or white concrete looks great", "Small domes work best under 12 blocks diameter")),
        new InspirationEntry("Roof", "Onion Dome",
            "Bulging dome that narrows at the top. Common in Russian/Ottoman architecture.",
            List.of("Widest point is about ⅓ up from the base", "Taper more steeply above the widest ring", "Gold or terracotta works well")),
        new InspirationEntry("Roof", "Pagoda Roof",
            "Multi-tiered with upward-curving eaves at each level.",
            List.of("Each tier is smaller than the one below", "Curve eaves by stepping stairs outward then back", "Dark oak or jungle wood with terracotta accents")),
        new InspirationEntry("Roof", "Shed Roof",
            "Single slope, one direction only. Simple and modern.",
            List.of("Higher on one side, lower on the other", "Good for extensions, workshops, lean-tos", "Combine with a flat roof on the main building")),
        new InspirationEntry("Roof", "Butterfly Roof",
            "Two slopes rising outward like wings — a V shape in the middle.",
            List.of("Lowest point is in the center", "Works best for modern or sci-fi builds", "Use a channel of glass or slabs at the center valley"))
    );

    // ── WINDOW STYLES ─────────────────────────────────────────────────────────
    public static final List<InspirationEntry> WINDOW_STYLES = List.of(
        new InspirationEntry("Window", "Arrow Slit",
            "Narrow vertical slot. Medieval and fortress style.",
            List.of("1 wide × 3–5 tall", "Recess 1 block into the wall for depth", "Use iron bars or no pane for authenticity")),
        new InspirationEntry("Window", "Arched Window",
            "Rectangle with a curved top. Gothic and classical.",
            List.of("Build the arch with stair blocks pointing inward", "Frame with a contrasting stone for definition", "Glass pane inside, full glass block for the arch cap")),
        new InspirationEntry("Window", "Rose Window",
            "Circular decorative window. Gothic cathedrals and wizard towers.",
            List.of("Build as a circle using stained glass blocks", "Frame with stone bricks around the outside", "Mix 2–3 complementary stained glass colors")),
        new InspirationEntry("Window", "Dormer Window",
            "A window that protrudes from a sloped roof with its own small gabled top.",
            List.of("Place a small box structure on the roof slope", "Add a tiny gabled roof over each dormer", "Works best on gabled or gambrel main roofs")),
        new InspirationEntry("Window", "Bay Window",
            "Window that projects outward from the wall with a flat or angled front.",
            List.of("Extend 1–2 blocks outward from the wall face", "Add a small roof over the projection", "Use full glass blocks for the widest view")),
        new InspirationEntry("Window", "Clerestory",
            "A row of windows high on a wall, above an adjoining roofline.",
            List.of("Place windows near the top of a tall wall", "Works best when a lower structure abuts the wall", "Great for cathedrals, great halls, and warehouses")),
        new InspirationEntry("Window", "Porthole",
            "Round window. Nautical, industrial, or hobbit-hole style.",
            List.of("Build as a 3×3 or 5×5 circle of glass blocks", "Frame with stripped logs or iron blocks", "Recess 1 block into the wall")),
        new InspirationEntry("Window", "Transom Window",
            "Horizontal window above a door. Adds light and architectural detail.",
            List.of("Same width as the door, 1–2 blocks tall above it", "Use trapdoors as horizontal dividers inside", "Works well above double doors")),
        new InspirationEntry("Window", "Shuttered Window",
            "Standard window flanked by decorative shutters.",
            List.of("Use trapdoors as open shutters on each side", "Spruce or dark oak trapdoors for rustic style", "Open inward or pinned flat to the wall face"))
    );

    // ── STRUCTURAL SHAPES ─────────────────────────────────────────────────────
    public static final List<InspirationEntry> STRUCTURAL_SHAPES = List.of(
        new InspirationEntry("Shape", "Round Tower",
            "A cylindrical tower. The backbone of castles and wizard builds.",
            List.of("Use a circle chart for each floor ring", "Diameter 7 or 9 looks best", "Taper slightly every 8–10 blocks for a tapered tower effect")),
        new InspirationEntry("Shape", "Square Donjon",
            "A large square keep. Powerful and imposing.",
            List.of("Keep height at least 2× the base width", "Add corner buttresses for visual weight", "Battlements at the top with merlons and crenels")),
        new InspirationEntry("Shape", "L-Shaped Building",
            "Two rectangles joined at an angle. Breaks up boring box shapes.",
            List.of("Different heights on each wing add interest", "Fill the inner corner with a courtyard or garden", "Stagger the rooflines for a complex silhouette")),
        new InspirationEntry("Shape", "Courtyard Layout",
            "Buildings arranged around a central open space.",
            List.of("Enclose 3 or 4 sides with buildings of varying height", "Add a well, fountain, or garden in the center", "Connect buildings with archways or covered walkways")),
        new InspirationEntry("Shape", "Octagonal Tower",
            "Eight-sided tower. More complex than round but striking.",
            List.of("Use the 8-sided polygon as your floor plan template", "Each wall face is 3–5 blocks wide", "Great for wizard towers or fancy keeps")),
        new InspirationEntry("Shape", "Stepped Pyramid",
            "Tiered structure that narrows as it rises.",
            List.of("Each tier is 2–3 blocks shorter in width per side", "Leave a flat platform at each tier for detail", "Sandstone or terracotta for a Mesoamerican feel")),
        new InspirationEntry("Shape", "Cliff-Integrated Build",
            "Structure carved into or built against a natural cliff face.",
            List.of("Let the stone cliff be the back wall", "Carve rooms back into the cliff", "Use natural overhangs as roof sections")),
        new InspirationEntry("Shape", "Bridge Build",
            "A structure spanning a gap — river, canyon, or road.",
            List.of("Arch bridges: build the arch first, then the deck", "Tower bridges: tall pylons with suspension chains (chain blocks)", "Add lighting and railings (iron bars or fences)")),
        new InspirationEntry("Shape", "Floating Island Base",
            "A build on a raised or floating landmass.",
            List.of("Taper the underside of the island to a point or flat bottom", "Use different stone types for layering", "Waterfalls from the edges look excellent")),
        new InspirationEntry("Shape", "Underground Burrow",
            "A build that descends into the ground.",
            List.of("Start with a hillside entrance", "Use natural cave shapes, not just rectangular rooms", "Light with lanterns on chains or glow lichen"))
    );

    // ── WALL PATTERNS ─────────────────────────────────────────────────────────
    public static final List<InspirationEntry> WALL_PATTERNS = List.of(
        new InspirationEntry("Wall", "Timber Framing",
            "A pattern of dark wood beams set into a lighter plaster or stone wall.",
            List.of("Use dark oak logs as vertical and horizontal beams", "Fill panels with white concrete or smooth quartz", "Add diagonal corner braces with stairs")),
        new InspirationEntry("Wall", "Rusticated Stone",
            "Large, blocky stone with exaggerated joints. Heavy and grand.",
            List.of("Alternate between smooth and rough stone textures", "Use deepslate bricks with cracked stone bricks", "Works well on lower floors with smoother upper floors")),
        new InspirationEntry("Wall", "Brick Patterning",
            "Alternating or decorative brick arrangements.",
            List.of("Mix normal and cracked bricks for age", "Add a course of a contrasting block every 4–5 rows", "Use brick stairs for a soldier course")),
        new InspirationEntry("Wall", "Striped Wall",
            "Alternating bands of two different materials.",
            List.of("Alternate 1–2 block rows of contrasting stone", "Sandstone + red sandstone is a classic combination", "Works great on tall towers")),
        new InspirationEntry("Wall", "Wattle and Daub",
            "A rough, organic wall style. Peasant cottages and village builds.",
            List.of("Use hay bales, oak planks, and stripped logs as the frame", "Smooth sandstone or diorite as the plaster panels", "Keep walls thin — 1 block — and low")),
        new InspirationEntry("Wall", "Ashlar Blocks",
            "Large, evenly cut stone blocks. Classical and refined.",
            List.of("Use polished stone variants: diorite, andesite, granite", "Keep the pattern regular and grid-like", "Add pilasters (decorative columns) every 8–10 blocks")),
        new InspirationEntry("Wall", "Patterned Terracotta",
            "Colorful geometric designs using glazed terracotta.",
            List.of("Each glazed terracotta face rotates — use this for symmetry", "Combine 4 of the same block for a pinwheel pattern", "Works best as accent panels, not entire walls"))
    );

    // ── INTERIOR IDEAS ────────────────────────────────────────────────────────
    public static final List<InspirationEntry> INTERIOR_IDEAS = List.of(
        new InspirationEntry("Interior", "Great Hall",
            "A large communal space with a central hearth, long tables, and high ceiling.",
            List.of("Central fireplace or campfire pit with chains above", "Long oak tables (pressure plates or slabs)", "Banners and antler-style chandeliers (chains + lanterns)")),
        new InspirationEntry("Interior", "Library",
            "Floor-to-ceiling bookshelves with reading alcoves and a study area.",
            List.of("Stack bookshelves 3–4 blocks high with ladders for access", "Add a lectern and candles in each alcove", "Secret door in a bookshelf section for a hidden room")),
        new InspirationEntry("Interior", "Throne Room",
            "A grand formal room with a raised dais and imposing throne.",
            List.of("Raise the throne platform 2–3 steps", "Red carpet (red wool or carpet) down the center aisle", "Tall banners flanking the throne")),
        new InspirationEntry("Interior", "Kitchen",
            "A functional-looking kitchen with prep areas, storage, and a hearth.",
            List.of("Smoker and campfire as the cooking area", "Barrels and chests for storage", "Flower pots with herbs on the windowsill")),
        new InspirationEntry("Interior", "Workshop / Forge",
            "A crafting and production space with tools, materials, and fire.",
            List.of("Blast furnace and anvil as centerpieces", "Chains hanging from the ceiling with lanterns", "Grindstone and smithing table for detail")),
        new InspirationEntry("Interior", "Observatory",
            "A high tower room with telescopes, star charts, and glass domes.",
            List.of("End rods and lodestones as telescope props", "Glass dome ceiling — blue stained glass works well", "Bookshelves and lecterns for star charts")),
        new InspirationEntry("Interior", "Dungeon Cell Block",
            "A dark corridor of cells with iron doors and grim furnishings.",
            List.of("Iron bars as cell walls (not iron doors)", "Soul torches for an eerie blue light", "Chains hanging from the ceiling in each cell")),
        new InspirationEntry("Interior", "Tavern Common Room",
            "A warm, busy inn interior with bar, tables, and fireplace.",
            List.of("Bar counter using slabs and trapdoors", "Barrel stacks in the corner", "Fireplace on one wall with a mantle")),
        new InspirationEntry("Interior", "Greenhouse / Garden Room",
            "A glass-roofed interior space filled with plants and growing things.",
            List.of("Glass or glass pane ceiling and walls", "Raised flower beds using farmland and fences", "Hanging vines and potted plants on every surface"))
    );

    public static List<InspirationEntry> getCategory(String category) {
        return switch (category) {
            case "Roofs" -> ROOF_TYPES;
            case "Windows" -> WINDOW_STYLES;
            case "Shapes" -> STRUCTURAL_SHAPES;
            case "Walls" -> WALL_PATTERNS;
            case "Interiors" -> INTERIOR_IDEAS;
            default -> ROOF_TYPES;
        };
    }

    public static final List<String> CATEGORIES = List.of("Roofs", "Windows", "Shapes", "Walls", "Interiors");
}
