package name.modid.client.data;

import java.util.*;

public class PromptData {

    public record ThemeData(String displayName, List<String> prompts) {}

    private static final Map<String, ThemeData> THEMES = new LinkedHashMap<>();

    static {
        THEMES.put("fantasy", new ThemeData("Fantasy", List.of(
            "A cliffside dwarven forge with water-powered machinery and glowing lava channels",
            "A floating wizard library with stained-glass observatories and enchanted bookshelves",
            "An ancient elven treehouse city connected by rope bridges through a dense forest",
            "A dark sorcerer's tower rising from a dead swamp, surrounded by corrupted vegetation",
            "A fairy ring village built inside a massive hollow mushroom",
            "A dragon's lair carved into a volcanic mountain with golden treasure halls",
            "A mystical portal hub floating in the clouds, connecting to other realms",
            "An underground gnome workshop filled with intricate machinery and gemstone veins",
            "A coral palace built into a reef beneath the ocean surface",
            "A goblin market maze carved into canyon walls with hundreds of tiny stalls",
            "A phoenix roost atop a lava mountain with fireproof obsidian perches",
            "An ice witch's castle frozen at the peak of a glacier",
            "A druid circle of ancient standing stones in a misty forest clearing",
            "A bard's college built inside a massive natural amphitheater",
            "A moonlit hedge maze surrounding a hidden shrine to a forgotten god"
        )));

        THEMES.put("medieval", new ThemeData("Medieval", List.of(
            "A fortified hilltop castle with a working drawbridge and a deep moat",
            "A bustling market town with a central cathedral and cobblestone streets",
            "A monastery perched on a seaside cliff with herb gardens and a scriptorium",
            "A blacksmith district in a walled city with forge smoke rising above rooftops",
            "A baron's manor surrounded by farmland and a defensive wooden palisade",
            "A harbor town with fishing docks, a lighthouse, and a mead hall on the waterfront",
            "A mountain pass fortress blocking a narrow valley with guard towers on both sides",
            "A thieves' guild hidden beneath a tavern's cellar in a seedy district",
            "An archery training ground attached to a royal hunting lodge in the forest",
            "A walled abbey with a cloister garden, bell tower, and pilgrimage route",
            "A siege encampment with catapults, supply wagons, and command tents",
            "A peasant village with a windmill, a well, and a cobblestone market square",
            "A watchtower network connected by beacon fires across rolling hills",
            "A jousting tournament ground with grand stands and colorful pavilion tents",
            "A toll bridge gatehouse spanning a deep river gorge"
        )));

        THEMES.put("sci-fi", new ThemeData("Sci-Fi", List.of(
            "A space station research lab with observation domes and solar panel arrays",
            "A cyberpunk megacity district with neon signs, sky bridges, and rain-slicked streets",
            "A terraforming colony on an alien plateau with biodomes and landing pads",
            "An underwater research station pressurized against crushing ocean depths",
            "A starship hangar bay with maintenance platforms and refueling lines",
            "A derelict hulk drifting through asteroid fields, partially reclaimed by nature",
            "A quantum computing facility with humming server towers and liquid cooling vents",
            "A generation ship city-block with hydroponic farms and artificial skylights",
            "A mech factory assembly line with robotic arms and live testing bays",
            "A cryosleep facility with rows of stasis pods and monitoring stations",
            "A satellite relay tower on a barren moon surface with a distant planet in the sky",
            "A gravity research platform floating above a singularity",
            "A battle-scarred war memorial starship turned into a museum and memorial",
            "An AI-governed district where buildings rearrange based on population flow",
            "A time research laboratory hidden inside a glacier"
        )));

        THEMES.put("cozy", new ThemeData("Cozy", List.of(
            "A cottage bakery in a sunflower meadow with a bubbling brook nearby",
            "A bookshop and cafe built into a converted old watermill",
            "A hobbit-style home dug into a hillside with a round door and kitchen garden",
            "A treehouse retreat with string lights, a hammock porch, and a rope ladder",
            "A seaside cottage with window boxes, a boat shed, and a kelp garden",
            "A mountain cabin with a wraparound porch and a stone fireplace chimney",
            "A flower shop on a cobblestone lane with overflowing window box planters",
            "A cozy library nook carved into the hollow of a giant old oak tree",
            "A tea house beside a koi pond with a moss-covered stepping stone path",
            "A shepherd's hut in a wildflower meadow with a small kitchen garden",
            "A village post office with a sorting room, letterboxes, and a pigeon coop",
            "A ceramics studio with a wood-fired kiln and a sunlit gallery room",
            "A patchwork of tiny garden allotments around a shared community greenhouse",
            "A candlemaker's workshop with wax pools, dipping racks, and a small shop front",
            "A honey farm with lavender fields and a mead-making cellar"
        )));

        THEMES.put("industrial", new ThemeData("Industrial", List.of(
            "A steam-powered factory with cooling towers and a rail loading dock",
            "A coal mine complex with shaft headframes and ore processing buildings",
            "An ironworks foundry with blast furnaces and cooling slag heaps",
            "A canal wharf district with warehouses, cranes, and barge slips",
            "A clockwork tower with exposed gear mechanisms and a resonant chime bell",
            "A railroad junction with a roundhouse and a locomotive repair workshop",
            "A gas lamp-lit factory district surrounded by rows of worker terrace housing",
            "A textile mill with spinning floors, dye vats, and a loading bay",
            "A quarry with blasting sites, stone crushers, and mine rail tracks",
            "A power station with turbine halls and overhead cable towers",
            "A shipyard with dry docks, slipways, and a rigging loft",
            "A chemical works with distillation columns and large storage tanks",
            "A brewery with fermentation towers, malt floors, and a tap room",
            "A newspaper building with a basement press hall and a rooftop antenna",
            "A gasworks with retort houses, purifiers, and a large gas holder tank"
        )));

        THEMES.put("gothic", new ThemeData("Gothic", List.of(
            "A cathedral with flying buttresses, a rose window, and a crypt beneath",
            "A vampire's mansion on a fog-shrouded moor with iron gates and dead trees",
            "A cemetery chapel with gargoyle waterspouts and an underground bone ossuary",
            "A haunted manor with a collapsed wing and overgrown hidden passage gardens",
            "A dark alchemist's tower with bubbling vials and a forbidden basement laboratory",
            "A ruined abbey reclaimed by ivy, inhabited by ravens and shadows",
            "A mausoleum district in a city of the dead built for a fallen empire",
            "A cursed bell tower with a phantom organist and broken stained glass windows",
            "A sewer network beneath a cursed city with rat warrens and lost treasures",
            "A dungeon complex with cells, interrogation chambers, and a throne room boss lair",
            "An inquisitor's courthouse with a public square and iron gallows",
            "A gothic greenhouse full of carnivorous plants and strange failed experiments",
            "A secret society lodge hidden behind a bookshelf door in a grand library",
            "A dead lighthouse on rocky shores with wrecked ships visible on the rocks below",
            "A shadow puppet theater with mechanical stage machinery and a ghostly audience"
        )));

        THEMES.put("nordic", new ThemeData("Nordic", List.of(
            "A longhouse settlement with a great mead hall and carved dragon-prow ships",
            "A mountain fortress carved from raw granite above the snowline",
            "A shipbuilding yard on a fjord with longships in various stages of construction",
            "A runic standing stone circle in a pine forest clearing under the northern lights",
            "A Norse trading post at a river mouth with a walled market district",
            "A valkyrie temple on a windswept plateau with towering flame braziers",
            "A coastal village with turf-roofed longhouses and a fish-smoking house on the docks",
            "A jarl's great hall with an open central hearth and antler chandeliers",
            "A mountain pass village sheltered from blizzards by a timber palisade wall",
            "An abandoned mine with runic warning inscriptions and collapsed tunnels",
            "A sea cliff oracle's cave with tide pools and rune-carved stone walls",
            "A frozen harbor with icebreaker longships and a whale-processing station",
            "A berserker training camp with weapon racks, sparring rings, and a sweat lodge",
            "A burial mound complex with a boat burial and carved stone grave markers",
            "A watchtower with a horn and a relay of signal fires across mountain peaks"
        )));

        THEMES.put("tropical", new ThemeData("Tropical", List.of(
            "A tiki village built on stilts over a turquoise lagoon",
            "A pirate cove hidden in sea caves with a fleet of ships and a treasure vault",
            "A jungle temple complex swallowed by vines and inhabited by parrots and monkeys",
            "A beach resort with open-air pavilions, hammocks, and a tiki bar on the sand",
            "A coral city built on an atoll reef with bridges spanning the channels between islands",
            "A canopy research station in a rainforest with rope walkways and observation decks",
            "A fishing village with colorful boats, net-drying racks, and a waterfront market",
            "A mangrove delta settlement built into the root systems of giant trees",
            "A volcanic island fortress with obsidian walls and lava moats",
            "A pearl diving village with drying sheds and a brass underwater diving bell",
            "A shipwreck island camp with salvaged materials and a signal fire on the peak",
            "A trade post on a river delta with canoe docks and fragrant spice warehouses",
            "A cliff-top surfing village with a secret tunnel descending to a private beach cove",
            "A floating market on bamboo rafts in a wide river delta",
            "A botanical garden greenhouse in the tropics filled with rare exotic plants"
        )));
    }

    public static List<String> getThemeKeys() {
        return new ArrayList<>(THEMES.keySet());
    }

    public static ThemeData getTheme(String key) {
        return THEMES.getOrDefault(key, THEMES.values().iterator().next());
    }

    public static String getRandomPrompt(String themeKey) {
        ThemeData theme = THEMES.get(themeKey);
        if (theme == null || theme.prompts().isEmpty()) return "Theme not found.";
        return theme.prompts().get((int) (Math.random() * theme.prompts().size()));
    }

    public static String getRandomPromptAny() {
        List<String> keys = getThemeKeys();
        String key = keys.get((int) (Math.random() * keys.size()));
        ThemeData theme = THEMES.get(key);
        return "[" + theme.displayName() + "] " + getRandomPrompt(key);
    }
}
