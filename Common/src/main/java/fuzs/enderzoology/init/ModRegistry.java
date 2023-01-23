package fuzs.enderzoology.init;

import fuzs.enderzoology.EnderZoology;
import fuzs.enderzoology.world.entity.monster.ConcussionCreeper;
import fuzs.enderzoology.world.entity.monster.EnderInfestedZombie;
import fuzs.puzzleslib.core.CommonAbstractions;
import fuzs.puzzleslib.core.CommonFactories;
import fuzs.puzzleslib.core.ModLoader;
import fuzs.puzzleslib.init.RegistryManager;
import fuzs.puzzleslib.init.RegistryReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.*;

public class ModRegistry {
    static final CreativeModeTab CREATIVE_MODE_TAB = CommonAbstractions.INSTANCE.creativeModeTab(EnderZoology.MOD_ID, () -> new ItemStack(Items.ENDER_PEARL));
    private static final RegistryManager REGISTRY = CommonFactories.INSTANCE.registration(EnderZoology.MOD_ID);
    public static final RegistryReference<EntityType<ConcussionCreeper>> CONCUSSION_CREEPER_ENTITY_TYPE = REGISTRY.registerEntityTypeBuilder("concussion_creeper", () -> EntityType.Builder.of(ConcussionCreeper::new, MobCategory.MONSTER).sized(0.6F, 1.7F).clientTrackingRange(8));
    public static final RegistryReference<EntityType<EnderInfestedZombie>> ENDER_INFESTED_ZOMBIE_ENTITY_TYPE = REGISTRY.registerEntityTypeBuilder("ender_infested_zombie", () -> EntityType.Builder.of(EnderInfestedZombie::new, MobCategory.MONSTER).sized(0.6F, 1.95F).clientTrackingRange(8));
    public static final RegistryReference<Item> CONCUSSION_CREEPER_SPAWN_EGG_ITEM = REGISTRY.whenNotOn(ModLoader.FORGE).registerItem("concussion_creeper_spawn_egg", () -> new SpawnEggItem(CONCUSSION_CREEPER_ENTITY_TYPE.get(), 5701518, 16714274, new Item.Properties().tab(CREATIVE_MODE_TAB)));
    public static final RegistryReference<Item> ENDER_INFESTED_ZOMBIE_SPAWN_EGG_ITEM = REGISTRY.whenNotOn(ModLoader.FORGE).registerItem("ender_infested_zombie_spawn_egg", () -> new SpawnEggItem(ENDER_INFESTED_ZOMBIE_ENTITY_TYPE.get(), 1257301, 2829596, new Item.Properties().tab(CREATIVE_MODE_TAB)));

    public static void touch() {

    }
}
