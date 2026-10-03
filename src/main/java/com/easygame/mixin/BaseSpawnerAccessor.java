package com.easygame.mixin;

import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.SpawnData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BaseSpawner.class)
public interface BaseSpawnerAccessor {
    @Accessor("spawnPotentials")
    WeightedList<SpawnData> getSpawnPotentials();

    @Accessor("spawnPotentials")
    void setSpawnPotentials(WeightedList<SpawnData> potentials);

    @Accessor("nextSpawnData")
    SpawnData getNextSpawnData();

    @Accessor("nextSpawnData")
    void setNextSpawnData(SpawnData nextSpawnData);

    @Accessor("spawnDelay")
    void setSpawnDelay(int delay);

    @Accessor("minSpawnDelay")
    void setMinSpawnDelay(int delay);

    @Accessor("maxSpawnDelay")
    void setMaxSpawnDelay(int delay);

    @Accessor("spawnCount")
    void setSpawnCount(int count);

    @Accessor("requiredPlayerRange")
    void setRequiredPlayerRange(int range);

    @Accessor("displayEntity")
    void setDisplayEntity(Entity entity);
}
