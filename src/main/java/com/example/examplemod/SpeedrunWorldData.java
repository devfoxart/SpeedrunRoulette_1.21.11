package com.example.examplemod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.core.HolderLookup;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import java.util.Optional;
import net.minecraft.world.level.saveddata.SavedDataType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class SpeedrunWorldData extends SavedData {

    public static final Codec<SpeedrunWorldData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Objective.CODEC.listOf().fieldOf("objectives").forGetter(SpeedrunWorldData::getObjectives),
        Codec.LONG.optionalFieldOf("totalTime", 0L).forGetter(SpeedrunWorldData::getTotalTime),
        Codec.BOOL.optionalFieldOf("isWon", false).forGetter(SpeedrunWorldData::isWon),
        Codec.BOOL.optionalFieldOf("isLost", false).forGetter(SpeedrunWorldData::isLost)
    ).apply(instance, (objectives, time, won, lost) -> {
        SpeedrunWorldData data = new SpeedrunWorldData();
        data.setObjectives(objectives);
        data.setTotalTime(time);
        data.setWon(won);
        data.setLost(lost);
        return data;
    }));

    private static final String DATA_NAME = "speedrun_world_data";
    private final List<Objective> objectives = new ArrayList<>();
    private long totalTime = 0;
    private boolean isWon = false;
    private boolean isLost = false;

    public SpeedrunWorldData() {}

    public static SpeedrunWorldData load(CompoundTag tag, HolderLookup.Provider provider) {
        SpeedrunWorldData data = new SpeedrunWorldData();
        data.objectives.clear();
        if (tag.contains("objectives")) {
            ListTag list = null;
            Object listObj = tag.getList("objectives"); // Assuming getList(String) returns ListTag or Object
            if (listObj instanceof ListTag) {
                list = (ListTag) listObj;
            } else if (listObj instanceof Optional) {
                list = (ListTag)((Optional)listObj).orElse(null);
            }
            
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    try {
                        Object objTagObj = list.getCompound(i);
                        CompoundTag objTag = null;
                        if (objTagObj instanceof Optional) {
                            objTag = ((Optional<CompoundTag>) objTagObj).orElse(null);
                        } else {
                            objTag = (CompoundTag) objTagObj;
                        }
                        
                        if (objTag != null) {
                            data.objectives.add(Objective.load(objTag, provider));
                        }
                    } catch (Throwable e) {
                    }
                }
            }
        }
        
        if (tag.contains("totalTime")) tag.getLong("totalTime").ifPresent(v -> data.totalTime = v);
        if (tag.contains("isWon")) tag.getBoolean("isWon").ifPresent(v -> data.isWon = v);
        if (tag.contains("isLost")) tag.getBoolean("isLost").ifPresent(v -> data.isLost = v);
        
        return data;
    }

    // Removed @Override just in case signature mismatches
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Objective obj : objectives) {
            list.add(obj.save(provider));
        }
        tag.put("objectives", list);
        tag.putLong("totalTime", totalTime);
        tag.putBoolean("isWon", isWon);
        tag.putBoolean("isLost", isLost);
        return tag;
    }
    
    // Add legacy save method just in case
    public CompoundTag save(CompoundTag tag) {
        return save(tag, null);
    }

    public static SpeedrunWorldData get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(new SavedDataType<SpeedrunWorldData>(
             DATA_NAME,
             SpeedrunWorldData::new,
             CODEC,
             null
        ));
    }

    public List<Objective> getObjectives() {
        return objectives;
    }

    public void setObjectives(List<Objective> objectives) {
        this.objectives.clear();
        this.objectives.addAll(objectives);
        setDirty();
    }
    
    public long getTotalTime() { return totalTime; }
    public void setTotalTime(long time) { this.totalTime = time; setDirty(); }
    
    public boolean isWon() { return isWon; }
    public void setWon(boolean won) { this.isWon = won; setDirty(); }
    
    public boolean isLost() { return isLost; }
    public void setLost(boolean lost) { this.isLost = lost; setDirty(); }
}
