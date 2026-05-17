package dev.icrabtree.villageguard.entity;

public enum GuardWeapon {
    SWORD(0),
    AXE(1),
    SPEAR(2),    // Melee — treated as a long-reach sword
    BOW(3),
    CROSSBOW(4);

    public final int id;

    GuardWeapon(int id) {
        this.id = id;
    }

    public static GuardWeapon byId(int id) {
        for (GuardWeapon w : values()) {
            if (w.id == id) return w;
        }
        return SWORD;
    }

    public boolean isRanged() {
        return this == BOW || this == CROSSBOW;
    }

    public boolean isMelee() {
        return !isRanged();
    }
}
