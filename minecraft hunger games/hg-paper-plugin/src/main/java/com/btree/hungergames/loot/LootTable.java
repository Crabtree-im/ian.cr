package com.btree.hungergames.loot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LootTable {
    private final int minSlots;
    private final int maxSlots;
    private final List<LootEntry> entries;

    public LootTable(int minSlots, int maxSlots, List<LootEntry> entries) {
        this.minSlots = minSlots;
        this.maxSlots = maxSlots;
        this.entries = new ArrayList<>(entries);
    }

    public int getMinSlots() {
        return minSlots;
    }

    public int getMaxSlots() {
        return maxSlots;
    }

    public List<LootEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }
}
