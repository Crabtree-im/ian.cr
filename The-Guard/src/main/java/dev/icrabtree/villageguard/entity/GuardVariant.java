package dev.icrabtree.villageguard.entity;

public enum GuardVariant {
    SCOUT(0),    // Light armor, nimble — any weapon
    SOLDIER(1),  // Medium armor — any weapon
    CAPTAIN(2);  // Full iron armor — any weapon

    public final int id;

    GuardVariant(int id) {
        this.id = id;
    }

    public static GuardVariant byId(int id) {
        for (GuardVariant v : values()) {
            if (v.id == id) return v;
        }
        return SCOUT;
    }
}
