package name.modid.client.data;

import java.util.*;

public class PaletteData {

    public record BlockEntry(String name, int color) {}

    public record Palette(
        String name,
        String style,
        String description,
        List<BlockEntry> primary,
        List<BlockEntry> accent,
        List<BlockEntry> roof,
        List<BlockEntry> trim
    ) {}

    // Approximate block colors (ARGB without alpha — alpha added at draw time)
    private static final int SPRUCE_LOG       = 0x4A2F1A;
    private static final int SPRUCE_PLANKS    = 0x6B4226;
    private static final int OAK_LOG          = 0x7A5C2E;
    private static final int OAK_PLANKS       = 0xC4A265;
    private static final int DARK_OAK_PLANKS  = 0x3D2209;
    private static final int BIRCH_PLANKS     = 0xD7C99A;
    private static final int JUNGLE_LOG       = 0x5C3A1A;
    private static final int JUNGLE_PLANKS    = 0x8C5C35;
    private static final int BAMBOO_PLANKS    = 0x8FB040;
    private static final int COBBLESTONE      = 0x7A7A7A;
    private static final int STONE_BRICKS     = 0x888888;
    private static final int MOSSY_STONE      = 0x6A7A5A;
    private static final int CRACKED_STONE    = 0x707070;
    private static final int DEEPSLATE        = 0x404040;
    private static final int BLACKSTONE       = 0x1C1820;
    private static final int ANDESITE         = 0x8A8A8A;
    private static final int DIORITE          = 0xC0C0B8;
    private static final int GRANITE          = 0x9A6050;
    private static final int NETHER_BRICKS    = 0x3A1010;
    private static final int RED_NETHER       = 0x5A1515;
    private static final int QUARTZ           = 0xF0EEEA;
    private static final int SANDSTONE        = 0xD4B96A;
    private static final int CUT_SANDSTONE    = 0xD0B460;
    private static final int SMOOTH_SANDSTONE = 0xD8BF70;
    private static final int RED_SANDSTONE    = 0xA0501A;
    private static final int TERRACOTTA       = 0xA05030;
    private static final int YELLOW_TERR      = 0xC09040;
    private static final int ORANGE_TERR      = 0xA06030;
    private static final int PRISMARINE       = 0x4D9A9A;
    private static final int PRISM_BRICKS     = 0x5AAA9A;
    private static final int DARK_PRISM       = 0x2A5A5A;
    private static final int SEA_LANTERN      = 0xADE0E0;
    private static final int END_STONE_BRICKS = 0xDDD8A4;
    private static final int PURPUR           = 0x9B5EA2;
    private static final int PURPUR_PILLAR    = 0xA06AAA;
    private static final int AMETHYST         = 0x8B5B9E;
    private static final int COPPER_BLOCK     = 0xB87333;
    private static final int OXIDIZED_COPPER  = 0x5F9970;
    private static final int CUT_COPPER       = 0xB87333;
    private static final int IRON_BLOCK       = 0xC8C8C8;
    private static final int GOLD_BLOCK       = 0xF5C942;
    private static final int WHITE_CONCRETE   = 0xCFCFCF;
    private static final int GRAY_CONCRETE    = 0x5A5A5A;
    private static final int ORANGE_CONCRETE  = 0xE07020;
    private static final int YELLOW_CONCRETE  = 0xF0D020;
    private static final int OBSIDIAN         = 0x120C1A;
    private static final int CRYING_OBSIDIAN  = 0x2A1040;
    private static final int SNOW_BLOCK       = 0xF8F8F8;
    private static final int PACKED_ICE       = 0xA0C0F0;
    private static final int BLUE_ICE         = 0x80B4FF;
    private static final int CALCITE          = 0xE0DDD8;
    private static final int MOSS_BLOCK       = 0x4A7A3A;
    private static final int PALE_OAK         = 0xE8DCC0;
    private static final int GLOW_LICHEN      = 0x70A070;
    private static final int VINE             = 0x3A6030;
    private static final int BASALT           = 0x4A4A50;
    private static final int SOUL_SAND        = 0x5A4535;
    private static final int IRON_BARS        = 0xAAAAAA;
    private static final int CHAIN            = 0x666666;
    private static final int LANTERN          = 0xF0A020;
    private static final int SOUL_LANTERN     = 0x5090FF;
    private static final int TORCH            = 0xFFCC44;
    private static final int END_ROD          = 0xE0E0FF;
    private static final int BONE_BLOCK       = 0xE8E4CC;
    private static final int HAY_BALE         = 0xD4AA44;
    private static final int CHISELED_SAND    = 0xD4B960;

    private static final Map<String, Palette> PALETTES = new LinkedHashMap<>();

    static {
        PALETTES.put("nordic", new Palette(
            "Nordic Longhouse", "Medieval / Nordic",
            "Dark wood and rough stone. Earthy and weathered.",
            List.of(
                new BlockEntry("Spruce Log", SPRUCE_LOG),
                new BlockEntry("Spruce Planks", SPRUCE_PLANKS),
                new BlockEntry("Stripped Spruce Log", 0x5C3A1E)
            ),
            List.of(
                new BlockEntry("Cobblestone", COBBLESTONE),
                new BlockEntry("Stone Bricks", STONE_BRICKS),
                new BlockEntry("Mossy Stone Bricks", MOSSY_STONE)
            ),
            List.of(
                new BlockEntry("Spruce Slab", SPRUCE_PLANKS),
                new BlockEntry("Spruce Stairs", 0x5C3A1E)
            ),
            List.of(
                new BlockEntry("Iron Bars", IRON_BARS),
                new BlockEntry("Chain", CHAIN),
                new BlockEntry("Lantern", LANTERN)
            )
        ));

        PALETTES.put("stone_castle", new Palette(
            "Stone Castle", "Medieval / Fortification",
            "Heavy, imposing stonework. Built to last centuries.",
            List.of(
                new BlockEntry("Stone Bricks", STONE_BRICKS),
                new BlockEntry("Cobblestone", COBBLESTONE),
                new BlockEntry("Mossy Stone Bricks", MOSSY_STONE)
            ),
            List.of(
                new BlockEntry("Deepslate Bricks", DEEPSLATE),
                new BlockEntry("Cracked Stone Bricks", CRACKED_STONE),
                new BlockEntry("Andesite", ANDESITE)
            ),
            List.of(
                new BlockEntry("Stone Slab", STONE_BRICKS),
                new BlockEntry("Stone Brick Stairs", 0x808080)
            ),
            List.of(
                new BlockEntry("Iron Bars", IRON_BARS),
                new BlockEntry("Chain", CHAIN),
                new BlockEntry("Torch", TORCH)
            )
        ));

        PALETTES.put("wizard_tower", new Palette(
            "Wizard Tower", "Fantasy / Arcane",
            "Dark stone meets magical accents. Mysterious and tall.",
            List.of(
                new BlockEntry("Deepslate Bricks", DEEPSLATE),
                new BlockEntry("Blackstone Bricks", BLACKSTONE),
                new BlockEntry("Dark Oak Planks", DARK_OAK_PLANKS)
            ),
            List.of(
                new BlockEntry("Amethyst Block", AMETHYST),
                new BlockEntry("Purpur Block", PURPUR),
                new BlockEntry("Tinted Glass", 0x4A3060)
            ),
            List.of(
                new BlockEntry("Dark Oak Slab", DARK_OAK_PLANKS),
                new BlockEntry("Blackstone Slab", BLACKSTONE)
            ),
            List.of(
                new BlockEntry("Chain", CHAIN),
                new BlockEntry("Soul Lantern", SOUL_LANTERN),
                new BlockEntry("Crying Obsidian", CRYING_OBSIDIAN)
            )
        ));

        PALETTES.put("elven_forest", new Palette(
            "Elven Forest", "Fantasy / Nature",
            "Light wood and leaf. Elegant and organic.",
            List.of(
                new BlockEntry("Oak Log", OAK_LOG),
                new BlockEntry("Oak Planks", OAK_PLANKS),
                new BlockEntry("Birch Planks", BIRCH_PLANKS)
            ),
            List.of(
                new BlockEntry("Moss Block", MOSS_BLOCK),
                new BlockEntry("Calcite", CALCITE),
                new BlockEntry("Pale Oak Planks", PALE_OAK)
            ),
            List.of(
                new BlockEntry("Oak Leaves", 0x3A7A30),
                new BlockEntry("Oak Slab", OAK_PLANKS)
            ),
            List.of(
                new BlockEntry("Glow Lichen", GLOW_LICHEN),
                new BlockEntry("Vine", VINE),
                new BlockEntry("Lantern", LANTERN)
            )
        ));

        PALETTES.put("desert_outpost", new Palette(
            "Desert Outpost", "Biome / Desert",
            "Warm sandstone tones. Sandy and sun-bleached.",
            List.of(
                new BlockEntry("Sandstone", SANDSTONE),
                new BlockEntry("Cut Sandstone", CUT_SANDSTONE),
                new BlockEntry("Smooth Sandstone", SMOOTH_SANDSTONE)
            ),
            List.of(
                new BlockEntry("Terracotta", TERRACOTTA),
                new BlockEntry("Yellow Terracotta", YELLOW_TERR),
                new BlockEntry("Orange Terracotta", ORANGE_TERR)
            ),
            List.of(
                new BlockEntry("Sandstone Slab", SANDSTONE),
                new BlockEntry("Sandstone Stairs", CUT_SANDSTONE)
            ),
            List.of(
                new BlockEntry("Chiseled Sandstone", CHISELED_SAND),
                new BlockEntry("Bone Block", BONE_BLOCK),
                new BlockEntry("Torch", TORCH)
            )
        ));

        PALETTES.put("ocean_palace", new Palette(
            "Ocean Palace", "Biome / Ocean",
            "Cool blues and teals. Watery and ancient.",
            List.of(
                new BlockEntry("Prismarine", PRISMARINE),
                new BlockEntry("Prismarine Bricks", PRISM_BRICKS),
                new BlockEntry("Dark Prismarine", DARK_PRISM)
            ),
            List.of(
                new BlockEntry("Sea Lantern", SEA_LANTERN),
                new BlockEntry("Quartz", QUARTZ),
                new BlockEntry("Calcite", CALCITE)
            ),
            List.of(
                new BlockEntry("Prismarine Slab", PRISMARINE),
                new BlockEntry("Prismarine Stairs", PRISM_BRICKS)
            ),
            List.of(
                new BlockEntry("Sea Lantern", SEA_LANTERN),
                new BlockEntry("Dark Prismarine", DARK_PRISM),
                new BlockEntry("Conduit", 0x8AB0C0)
            )
        ));

        PALETTES.put("nether_citadel", new Palette(
            "Nether Citadel", "Fantasy / Dark",
            "Fire and brimstone. Dark, oppressive, and dangerous.",
            List.of(
                new BlockEntry("Nether Bricks", NETHER_BRICKS),
                new BlockEntry("Red Nether Bricks", RED_NETHER),
                new BlockEntry("Blackstone Bricks", BLACKSTONE)
            ),
            List.of(
                new BlockEntry("Basalt", BASALT),
                new BlockEntry("Soul Sand", SOUL_SAND),
                new BlockEntry("Crying Obsidian", CRYING_OBSIDIAN)
            ),
            List.of(
                new BlockEntry("Nether Brick Slab", NETHER_BRICKS),
                new BlockEntry("Red Nether Slab", RED_NETHER)
            ),
            List.of(
                new BlockEntry("Soul Lantern", SOUL_LANTERN),
                new BlockEntry("Chain", CHAIN),
                new BlockEntry("Gilded Blackstone", 0x2A1820)
            )
        ));

        PALETTES.put("end_citadel", new Palette(
            "End Citadel", "Fantasy / End",
            "Pale and eerie. The architecture of another dimension.",
            List.of(
                new BlockEntry("End Stone Bricks", END_STONE_BRICKS),
                new BlockEntry("Purpur Block", PURPUR),
                new BlockEntry("Purpur Pillar", PURPUR_PILLAR)
            ),
            List.of(
                new BlockEntry("Obsidian", OBSIDIAN),
                new BlockEntry("Amethyst Block", AMETHYST),
                new BlockEntry("Tinted Glass", 0x4A3060)
            ),
            List.of(
                new BlockEntry("Purpur Slab", PURPUR),
                new BlockEntry("Purpur Stairs", PURPUR_PILLAR)
            ),
            List.of(
                new BlockEntry("Chain", CHAIN),
                new BlockEntry("End Rod", END_ROD),
                new BlockEntry("Amethyst Cluster", AMETHYST)
            )
        ));

        PALETTES.put("cozy_cottage", new Palette(
            "Cozy Cottage", "Cozy / Rustic",
            "Warm oak and white plaster. Inviting and homey.",
            List.of(
                new BlockEntry("Oak Planks", OAK_PLANKS),
                new BlockEntry("Oak Log", OAK_LOG),
                new BlockEntry("Stripped Oak Log", 0x9C7840)
            ),
            List.of(
                new BlockEntry("White Concrete", WHITE_CONCRETE),
                new BlockEntry("Diorite", DIORITE),
                new BlockEntry("Quartz", QUARTZ)
            ),
            List.of(
                new BlockEntry("Oak Slab", OAK_PLANKS),
                new BlockEntry("Spruce Stairs", SPRUCE_PLANKS)
            ),
            List.of(
                new BlockEntry("Lantern", LANTERN),
                new BlockEntry("Flower Pot", 0x884422),
                new BlockEntry("Hay Bale", HAY_BALE)
            )
        ));

        PALETTES.put("ice_palace", new Palette(
            "Ice Palace", "Nordic / Fantasy",
            "Cold blues and white. Frozen and crystalline.",
            List.of(
                new BlockEntry("Packed Ice", PACKED_ICE),
                new BlockEntry("Blue Ice", BLUE_ICE),
                new BlockEntry("Snow Block", SNOW_BLOCK)
            ),
            List.of(
                new BlockEntry("Ice", 0xBBD7FF),
                new BlockEntry("Light Blue Glass", 0x90C0FF),
                new BlockEntry("Calcite", CALCITE)
            ),
            List.of(
                new BlockEntry("Packed Ice Slab", PACKED_ICE),
                new BlockEntry("Snow Slab", SNOW_BLOCK)
            ),
            List.of(
                new BlockEntry("Soul Lantern", SOUL_LANTERN),
                new BlockEntry("Amethyst Block", AMETHYST),
                new BlockEntry("Chain", CHAIN)
            )
        ));

        PALETTES.put("steampunk", new Palette(
            "Industrial Steampunk", "Industrial",
            "Iron, copper, and stone. Functional and mechanical.",
            List.of(
                new BlockEntry("Stone Bricks", STONE_BRICKS),
                new BlockEntry("Deepslate Bricks", DEEPSLATE),
                new BlockEntry("Smooth Stone", 0x909090)
            ),
            List.of(
                new BlockEntry("Copper Block", COPPER_BLOCK),
                new BlockEntry("Oxidized Copper", OXIDIZED_COPPER),
                new BlockEntry("Iron Block", IRON_BLOCK)
            ),
            List.of(
                new BlockEntry("Stone Slab", STONE_BRICKS),
                new BlockEntry("Cut Copper Slab", CUT_COPPER)
            ),
            List.of(
                new BlockEntry("Lantern", LANTERN),
                new BlockEntry("Chain", CHAIN),
                new BlockEntry("Iron Bars", IRON_BARS)
            )
        ));

        PALETTES.put("tropical_bamboo", new Palette(
            "Tropical Bamboo", "Tropical / Jungle",
            "Bamboo, jungle wood, and bright colors. Lush and warm.",
            List.of(
                new BlockEntry("Bamboo Planks", BAMBOO_PLANKS),
                new BlockEntry("Jungle Log", JUNGLE_LOG),
                new BlockEntry("Jungle Planks", JUNGLE_PLANKS)
            ),
            List.of(
                new BlockEntry("Terracotta", TERRACOTTA),
                new BlockEntry("Orange Concrete", ORANGE_CONCRETE),
                new BlockEntry("Yellow Concrete", YELLOW_CONCRETE)
            ),
            List.of(
                new BlockEntry("Bamboo Slab", BAMBOO_PLANKS),
                new BlockEntry("Jungle Stairs", JUNGLE_PLANKS)
            ),
            List.of(
                new BlockEntry("Lantern", LANTERN),
                new BlockEntry("Bamboo", BAMBOO_PLANKS),
                new BlockEntry("Vine", VINE)
            )
        ));

        PALETTES.put("granite_estate", new Palette(
            "Granite Estate", "Medieval / Manor",
            "Warm pinks and grays. Grand and refined.",
            List.of(
                new BlockEntry("Polished Granite", 0xAA7060),
                new BlockEntry("Granite", GRANITE),
                new BlockEntry("Smooth Stone", 0x909090)
            ),
            List.of(
                new BlockEntry("Diorite", DIORITE),
                new BlockEntry("Quartz Bricks", 0xEAE8E0),
                new BlockEntry("White Concrete", WHITE_CONCRETE)
            ),
            List.of(
                new BlockEntry("Granite Slab", GRANITE),
                new BlockEntry("Stone Slab", STONE_BRICKS)
            ),
            List.of(
                new BlockEntry("Lantern", LANTERN),
                new BlockEntry("Iron Bars", IRON_BARS),
                new BlockEntry("Torch", TORCH)
            )
        ));
    }

    public static List<String> getPaletteKeys() {
        return new ArrayList<>(PALETTES.keySet());
    }

    public static Palette getPalette(String key) {
        return PALETTES.get(key);
    }

    public static Palette getRandomPalette() {
        List<String> keys = getPaletteKeys();
        return PALETTES.get(keys.get((int) (Math.random() * keys.size())));
    }
}
