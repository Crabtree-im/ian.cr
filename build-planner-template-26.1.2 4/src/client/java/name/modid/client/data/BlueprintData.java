package name.modid.client.data;

import java.util.List;

public class BlueprintData {

    public record Blueprint(
        String category,
        String name,
        String dimensions,   // e.g. "7×7×15"
        String material,     // primary material suggestion
        int blockEstimate,   // rough total blocks
        String difficulty,   // Beginner / Intermediate / Advanced
        String description,
        List<String> steps
    ) {}

    // ── TOWERS ────────────────────────────────────────────────────────────────
    public static final List<Blueprint> TOWERS = List.of(
        new Blueprint("Tower", "Round Keep", "7×7×14", "Stone Bricks", 420, "Beginner",
            "A solid cylindrical tower. The standard castle keep — simple to build, looks great at any scale.",
            List.of(
                "Draw a 7-wide circle on the ground (3 center + arc). Standard 7-circle: place center block, 3 out N/S/E/W, then fill corners to round.",
                "Build straight up for 14 layers. Every layer is the same circle.",
                "At layer 12, add corbels: stick 1 block out from the wall face all the way around.",
                "Build the battlement ring at layer 13–14 with alternating merlon/crenel (solid block, gap, solid block...).",
                "Cap the top with a conical or flat roof. For a cone: each ring narrows by 1 each layer.",
                "Add an arrow slit on each face: break out 1×3 tall gaps at layers 4, 7, and 10.",
                "Place a door at ground level on one face. Use an iron door or oak door with a button."
            )
        ),
        new Blueprint("Tower", "Square Watchtower", "5×5×12", "Cobblestone + Oak Planks", 280, "Beginner",
            "A compact square tower with timber framing. Good for village outposts and smaller builds.",
            List.of(
                "Mark a 5×5 square footprint. Place walls 1 block thick (hollow inside is 3×3).",
                "Build walls up to layer 8 using cobblestone.",
                "At layer 8, add a timber-frame ring: place oak logs at each corner, connecting beams of planks.",
                "Continue 3 more layers for the upper room with larger windows (2×2 openings).",
                "Add a small overhanging platform at the top using slabs jutting out 1 block.",
                "Place a trapdoor ladder inside for vertical access between floors.",
                "Finish with a low-pitched gabled or flat slab roof."
            )
        ),
        new Blueprint("Tower", "Octagonal Mage Tower", "11×11×20", "Quartz + Purpur", 980, "Intermediate",
            "An elegant 8-sided wizard tower. Best built in white quartz or end materials with a pointed spire.",
            List.of(
                "Plan an 11×11 bounding box. The 8-sided shape: 3 wide per flat face, diagonal corners cut by 1 block.",
                "Build the base 4 layers tall as a thick-walled octagon using smooth quartz.",
                "Taper the walls from 2 thick to 1 thick at layer 5 to create a ledge.",
                "Build the main shaft — single-block-thick walls — straight up to layer 18.",
                "At layer 17, flare outward 1 block for an overhanging turret ring.",
                "Build the spire: stack narrowing circles above layer 19 using purpur pillars, converging to a single block at the peak.",
                "Add spiral staircase inside using slabs and stairs in a circular pattern.",
                "Decorate with arched windows using stair blocks every 5 layers."
            )
        ),
        new Blueprint("Tower", "Ruined Tower Stump", "9×9×7", "Cracked Stone Bricks + Mossy Cobble", 260, "Beginner",
            "A deliberately broken-down tower remnant. Great for abandoned fortresses and dungeon builds.",
            List.of(
                "Build a 9×9 round or rough-square base, 5 layers tall, using a mix of stone bricks, cracked bricks, and mossy cobblestone.",
                "Vary the top edge: don't make it flat. Stagger the height by 1–3 blocks randomly.",
                "Remove random blocks from the middle layers to create holes and damage.",
                "Break out sections of the wall to leave partial arches using stair blocks.",
                "Plant vines on the outer walls — place them at the top and let them hang.",
                "Fill the interior with rubble: place slabs, stairs, and full blocks in chaotic arrangements.",
                "Optional: add a partial wooden floor mid-height using rotted-looking jungle planks + slabs."
            )
        ),
        new Blueprint("Tower", "Corner Flanking Tower", "5×5×10", "Stone Bricks", 230, "Beginner",
            "A small tower designed to sit at the corner of a wall circuit. Provides overlapping fields of coverage.",
            List.of(
                "Position at a wall corner so two wall segments connect into it.",
                "Build a 5×5 solid-walled tower up to 2 blocks taller than the adjacent wall height.",
                "The two exterior faces of the tower should project 1 block beyond the wall faces (so it can fire along the wall).",
                "Top with a flat battlement: alternate merlon / crenel all the way around.",
                "Add a wooden floor inside at wall-walk height.",
                "Leave arrow slits on all 4 faces at 3 heights."
            )
        )
    );

    // ── WALLS & BATTLEMENTS ───────────────────────────────────────────────────
    public static final List<Blueprint> WALLS = List.of(
        new Blueprint("Wall", "Castle Curtain Wall", "Varies × 3 × 8", "Stone Bricks", 0, "Beginner",
            "A standard defensive curtain wall. Connect flanking towers with this repeating segment.",
            List.of(
                "Set wall thickness to 2 blocks wide, height to 8 blocks tall.",
                "Build straight runs of wall between towers.",
                "Leave a wall-walk at the top (walkable flat space, 1 block wide behind the merlons).",
                "Top course: alternate full-block merlon (2 wide) and 1-block-wide crenel gaps all the way across.",
                "Add a wall-walk staircase inside every 20 blocks: a simple staircase down into the courtyard.",
                "Optional: add small recessed arrow-slit windows on the exterior face every 6 blocks."
            )
        ),
        new Blueprint("Wall", "Timber Palisade", "Varies × 1 × 5", "Oak Logs + Fence", 0, "Beginner",
            "A quick defensive perimeter made of upright logs. Perfect for early settlements.",
            List.of(
                "Place oak logs vertically, side by side, for the length of the wall.",
                "Vary log height randomly between 4 and 6 blocks for a natural look.",
                "Top with a row of oak fence posts (they create a pointy silhouette).",
                "Every 10 blocks, add a slightly taller log cluster (7 blocks) as a watchtower nub.",
                "Add a simple wooden gate using fence gates in a 3-wide opening."
            )
        ),
        new Blueprint("Wall", "Decorative Garden Wall", "Varies × 1 × 3", "Stone Brick + Flower Pots", 0, "Beginner",
            "A low ornamental wall for estates, gardens, and manor grounds.",
            List.of(
                "Build a 1-block-wide, 2-block-tall row of smooth stone or polished andesite.",
                "Cap with stone brick slabs along the top.",
                "Every 4 blocks, replace 1 full block with a stone brick pillar (1×1, 3 tall).",
                "Place flower pots on top of the pillar caps.",
                "Optional: add iron fencing between pillars for a grander look."
            )
        ),
        new Blueprint("Wall", "Retaining Wall & Embankment", "Varies × 2 × 6", "Deepslate Bricks + Grass", 0, "Intermediate",
            "A reinforced wall that holds back a raised earth embankment. Useful for terracing hillside builds.",
            List.of(
                "Dig into the hillside or raise earth 4–5 blocks on one side.",
                "Build a 2-block-thick deepslate brick face on the lower side.",
                "Step the wall face outward 1 block at the base (wider footing) for authenticity.",
                "Backfill the upper side with dirt up to the wall top.",
                "Grass over the top and plant flowers or shrubs along the upper edge.",
                "Add drainage arches at ground level: 1×2 openings every 8 blocks."
            )
        )
    );

    // ── GATEHOUSES ────────────────────────────────────────────────────────────
    public static final List<Blueprint> GATEHOUSES = List.of(
        new Blueprint("Gatehouse", "Simple Arched Gate", "7×4×8", "Stone Bricks", 180, "Beginner",
            "A single-arch gateway. The most basic fortified entrance.",
            List.of(
                "Build two 2-wide wall piers, 7 blocks apart (exterior gap of 3 blocks for the opening).",
                "Build both piers 6 blocks tall.",
                "Bridge across the top: place a 1-block lintel at layer 6.",
                "Create the arch: place stair blocks in the opening at layer 4–5 curving up (stairs pointing inward).",
                "Fill above the arch to the top of the piers with solid wall.",
                "Add a portcullis: use iron bars hanging down from the lintel, 2 blocks tall.",
                "Add a gate using iron doors or fence gates at ground level."
            )
        ),
        new Blueprint("Gatehouse", "Twin-Tower Gatehouse", "15×6×12", "Stone Bricks + Iron", 860, "Intermediate",
            "A full gatehouse flanked by two towers. The classic castle entrance.",
            List.of(
                "Place two 4×4 flanking towers, 7 blocks apart (opening in the middle).",
                "Build both towers to height 12.",
                "Bridge the towers at height 8 with a 2-block-deep gatehouse block spanning the gap.",
                "Cut a 3-wide × 6-tall arched passage through the bridge block at ground level.",
                "Add a portcullis slot in the passage ceiling (1 block recess) for the iron bar portcullis.",
                "Top the bridge with a fighting platform and battlements.",
                "Add a murder hole in the passage ceiling (1×1 opening from the room above).",
                "Put heavy oak doors at both ends of the tunnel passage.",
                "Connect the flanking tower tops to the bridge walkway."
            )
        ),
        new Blueprint("Gatehouse", "Village Palisade Gate", "9×2×6", "Oak Logs + Fence Gates", 120, "Beginner",
            "A wide wooden gate for a village palisade or farmstead perimeter.",
            List.of(
                "Build two 2-wide log piers, 5 blocks apart.",
                "Extend the piers 6 blocks tall.",
                "Bridge across the top with 3 horizontal oak logs.",
                "Add a 2-layer angled roof over the bridge using oak slabs.",
                "Hang 2 pairs of fence gates in the opening (4 gates total, 2 per side, for a 4-wide opening).",
                "Optionally add a small guard room above by enclosing the bridge area."
            )
        )
    );

    // ── BRIDGES ───────────────────────────────────────────────────────────────
    public static final List<Blueprint> BRIDGES = List.of(
        new Blueprint("Bridge", "Stone Arch Bridge", "Varies × 5 × 6", "Stone Bricks", 0, "Intermediate",
            "A classic arched bridge spanning a river or gorge. Works for any span up to ~20 blocks.",
            List.of(
                "Mark the two bank anchor points. Span width = distance between them.",
                "Build the arch first: start from both sides, using stair blocks and slabs to curve upward meeting in the middle.",
                "The arch should rise 3–4 blocks above the deck height at its peak.",
                "Build the spandrel fill above the arch up to deck level (solid blocks between arch and deck).",
                "Lay the deck: 3 blocks wide of stone brick slabs.",
                "Add parapet walls on each side: 1 block tall of full stone bricks.",
                "Place stone brick wall blocks on top of the parapets for a capped railing.",
                "For a long span, add a central pier in the water and two arches."
            )
        ),
        new Blueprint("Bridge", "Rope Suspension Bridge", "Varies × 3 × 5", "Oak Planks + Chain", 0, "Beginner",
            "A rustic suspension bridge using chains as cables. Perfect for jungle, pirate, or adventurer builds.",
            List.of(
                "Build two tall anchor pylons at each end (4×2 oak log columns, 6 blocks tall).",
                "Hang chains down from each pylon top in a catenary curve (step 1 block out, down 1, out 1, down 1, flattens in middle).",
                "Lay the deck planks between the pylons: 3 wide oak planks.",
                "Connect the deck to the chain every 3 blocks with a vertical chain segment.",
                "Add fence railing on both sides of the deck.",
                "Optional: add a small roof cover over the pylons with oak slabs."
            )
        ),
        new Blueprint("Bridge", "Drawbridge", "5×8×1", "Oak Planks + Iron", 80, "Intermediate",
            "A hinged bridge that can be raised. Primarily decorative in vanilla.",
            List.of(
                "Set the drawbridge opening to 5 wide, spanning a 4-block-deep moat.",
                "Build the bridge deck from oak planks, 5×4 blocks.",
                "At the gatehouse end, add a winch room above the gate tunnel.",
                "Hang 2 chains from the winch room down to the far end of the lowered deck.",
                "For the raised position: remove the deck and rebuild it vertically against the gate face (this is a manually-toggled decorative drawbridge).",
                "Surround with a moat of water in a 2-block-wide channel."
            )
        )
    );

    // ── FOUNDATIONS ───────────────────────────────────────────────────────────
    public static final List<Blueprint> FOUNDATIONS = List.of(
        new Blueprint("Foundation", "Flat Slab Foundation", "Varies × Varies × 1", "Polished Stone / Granite", 0, "Beginner",
            "A simple raised platform. Lifts the build off the ground and gives a clean base.",
            List.of(
                "Mark the building footprint plus 1–2 extra blocks on each side.",
                "Dig down 1–2 blocks or raise the area up 1–2 blocks depending on terrain.",
                "Fill with your primary foundation material (polished granite, smooth stone, deepslate).",
                "Add a 1-block step-down border using slabs around the perimeter for a defined edge.",
                "Optional: add a bottom course of a darker material for visual weight."
            )
        ),
        new Blueprint("Foundation", "Arched Undercroft", "Varies × Varies × 4", "Stone Bricks + Deepslate", 0, "Intermediate",
            "A vaulted basement level. Raises the main building and creates usable underground space.",
            List.of(
                "Excavate or build a 4-block-tall base under the building footprint.",
                "Build thick (2-wide) perimeter walls of deepslate brick.",
                "Divide the interior with a row of pillars spaced 4 blocks apart.",
                "Span between pillars with arches: stair blocks curving from pillar to pillar at the ceiling.",
                "Fill the vaulted ceiling above the arches with solid stone.",
                "Add narrow windows or gratings at ground-outside level for light.",
                "Use the space for a cellar, dungeon, or storage room."
            )
        ),
        new Blueprint("Foundation", "Stilted Platform", "Varies × Varies × 6", "Oak Logs + Planks", 0, "Beginner",
            "A raised wooden platform on stilts. For overwater buildings, cliff-face structures, and treehouses.",
            List.of(
                "Place vertical log columns from ground/water up to the desired height (every 4 blocks apart).",
                "Connect column tops with horizontal log beams in a grid.",
                "Lay planks on top of the beam grid as the floor.",
                "Add diagonal brace struts between columns and beams for visual stability.",
                "Build cross-bracing (X shape) between lower column pairs.",
                "Add a ladder or staircase from ground to the platform."
            )
        )
    );

    // ── INTERIOR MODULES ──────────────────────────────────────────────────────
    public static final List<Blueprint> INTERIORS = List.of(
        new Blueprint("Interior", "Spiral Staircase (Round)", "5×5 per floor", "Stone Bricks + Slabs", 0, "Intermediate",
            "A spiral stair inside a round tower. Steps curve around a central pole.",
            List.of(
                "Place a 1×1 center pole of stone brick pillar for the newel post.",
                "At each floor level, the staircase completes one 360° rotation.",
                "Layer 1: place a stair block pointing N at the 6-o'clock position.",
                "Layer 2: rotate 90° clockwise — stair block pointing E at 3-o'clock.",
                "Layer 3: rotate 90° — stair block pointing S at 12-o'clock.",
                "Layer 4: rotate 90° — stair block pointing W at 9-o'clock.",
                "Continue until you reach the next floor. Add a slab landing at each floor entry point.",
                "Optional: add stone brick steps filling the gap between the pole and outer wall."
            )
        ),
        new Blueprint("Interior", "Barrel-Vaulted Corridor", "3 wide × Varies × 4 tall", "Stone Bricks", 0, "Intermediate",
            "An arched tunnel corridor. Used in dungeons, castle undercrofts, and grand passages.",
            List.of(
                "Build straight walls 2 blocks tall on each side, 3 blocks apart (interior clear).",
                "At layer 3 (top of wall), place stair blocks on each side pointing inward (curving up).",
                "At layer 4, place a single row of full blocks as the crown of the arch.",
                "Repeat this cross-section for the full length of the corridor.",
                "Light with lanterns on chains hung from the crown block every 4 blocks.",
                "Use different stone types for floor vs. walls vs. vault for visual contrast."
            )
        ),
        new Blueprint("Interior", "Grand Fireplace", "5 wide × 2 deep × 4 tall", "Stone Bricks + Nether Brick", 60, "Beginner",
            "A large decorative fireplace with mantle. Anchor piece for any great hall or throne room.",
            List.of(
                "Build a 5-wide × 2-deep alcove recess into the wall.",
                "Line the alcove floor and back wall with nether bricks (fire-proof look).",
                "Place a campfire or regular fire in the alcove center.",
                "Build the mantle: a 5-wide × 1-deep shelf of stone brick slabs at height 3.",
                "Build pilasters (decorative columns) on each side of the opening using stone brick walls.",
                "Above the mantle, add a 5×3 decorative overmantle panel — use chiseled stone bricks or a banner.",
                "Optional: flank with 2 iron lanterns on chains for a cozy glow."
            )
        ),
        new Blueprint("Interior", "Portcullis Gate Mechanism", "5 wide × 1 deep × 6 tall", "Iron Bars + Stone", 55, "Beginner",
            "A decorative portcullis. Creates the look of a heavy iron gate that can be lowered.",
            List.of(
                "Build the gate opening: 3 wide × 5 tall.",
                "Fill the opening with iron bars (they auto-connect vertically and horizontally).",
                "At the top of the opening, place a 1-block lintel of stone brick.",
                "Recess a 1-block-deep slot in the ceiling above the opening (to simulate where the portcullis 'retracts').",
                "Add 2 iron bars extending up into the ceiling slot on each outer edge.",
                "Optional: add a winch on the wall beside the gate using an anvil or grindstone."
            )
        )
    );

    // ── ROOFS ─────────────────────────────────────────────────────────────────
    public static final List<Blueprint> ROOFS = List.of(
        new Blueprint("Roof", "Simple Gabled Roof (10-wide)", "10 wide × Varies × 5 tall", "Spruce Stairs + Slabs", 0, "Beginner",
            "A step-by-step breakdown for building a clean gabled roof on a 10-wide building.",
            List.of(
                "Your building is 10 blocks wide. The roof ridge will run along the center length.",
                "Row 1 (base): place stair blocks along both long edges, steps pointing inward and upward.",
                "Row 2: move 1 block in from each edge, place another stair row 1 block higher.",
                "Row 3: move 1 block in again, 1 block higher. Repeat until you reach the center.",
                "For a 10-wide building, the roof is 5 layers tall and 5 steps from each side.",
                "Cap the very top (ridge) with upside-down slab blocks for a clean peak.",
                "Fill the triangular gable ends with matching plank or stone material.",
                "Add a 1-block overhang on the gable ends by extending the last stair row 1 block."
            )
        ),
        new Blueprint("Roof", "Hipped Roof (12×8)", "12×8 footprint × 4 tall", "Spruce Stairs", 0, "Intermediate",
            "A hipped roof where all four sides slope. Cleaner than gabled but more geometry to manage.",
            List.of(
                "Building footprint is 12 long × 8 wide. Start with the 8-wide dimension.",
                "Layer 1: step in 1 block on all four sides. This gives a 10×6 ring of stairs.",
                "Layer 2: step in 1 block on all four sides again. Now 8×4.",
                "Layer 3: step in 1 block on the two short ends only. Now a 6×4 ridge.",
                "The 6-block ridge is flat — cap it with slabs or a row of upside-down stairs.",
                "Use the same stair material throughout for consistency.",
                "The corner junctions need a diagonal stair cut — use a stair block rotated to the corner angle."
            )
        ),
        new Blueprint("Roof", "Conical Tower Roof", "7-diameter × 6 tall", "Stone Brick Stairs + Slabs", 0, "Intermediate",
            "A pointed cone roof for a round tower. Ring-by-ring construction working upward.",
            List.of(
                "Start from the top of a 7-diameter round tower.",
                "Layer 1: place stair blocks around the full 7-circle perimeter, pointing outward.",
                "Layer 2: reduce to a 5-circle. Place stair blocks pointing outward.",
                "Layer 3: reduce to a 3-circle. Stair blocks pointing outward.",
                "Layer 4: place 4 stair blocks in a + pattern (N/S/E/W), pointing outward.",
                "Layer 5: place a single full block or slab as the tip.",
                "Optional: place a fence post + banner on the very top as a flag."
            )
        )
    );

    public static List<Blueprint> getCategory(String cat) {
        return switch (cat) {
            case "Towers"      -> TOWERS;
            case "Walls"       -> WALLS;
            case "Gatehouses"  -> GATEHOUSES;
            case "Bridges"     -> BRIDGES;
            case "Foundations" -> FOUNDATIONS;
            case "Interiors"   -> INTERIORS;
            case "Roofs"       -> ROOFS;
            default            -> TOWERS;
        };
    }

    public static final List<String> CATEGORIES =
        List.of("Towers", "Walls", "Gatehouses", "Bridges", "Foundations", "Interiors", "Roofs");
}
