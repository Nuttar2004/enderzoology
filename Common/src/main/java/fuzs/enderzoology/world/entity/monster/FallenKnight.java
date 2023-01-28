package fuzs.enderzoology.world.entity.monster;

import fuzs.enderzoology.init.ModRegistry;
import fuzs.enderzoology.world.entity.ai.goal.RangedBowEasyAttackGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class FallenKnight extends AbstractSkeleton {
    private static final Predicate<Difficulty> DOOR_BREAKING_PREDICATE = difficulty -> difficulty == Difficulty.HARD;
    private static final float BREAK_DOOR_CHANCE = 0.1F;

    private final BreakDoorGoal breakDoorGoal = new BreakDoorGoal(this, DOOR_BREAKING_PREDICATE);
    private RangedBowAttackGoal<AbstractSkeleton> bowGoal;
    private MeleeAttackGoal meleeGoal;
    private boolean canBreakDoors;

    public FallenKnight(EntityType<? extends AbstractSkeleton> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.populateArmorEquipmentSlots(random);
        Item item;
        if (random.nextBoolean()) {
            item = ModRegistry.HUNTING_BOW.get();
        } else {
            if (random.nextFloat() < (this.level.getDifficulty() == Difficulty.HARD ? 0.6F : 0.2F)) {
                item = Items.IRON_SWORD;
            } else {
                item = Items.STONE_SWORD;
            }
        }
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item));
    }

    private void populateArmorEquipmentSlots(RandomSource random) {
        float selector = random.nextFloat();
        int quality;
        if (selector < 0.1F) {
            // iron
            quality = 3;
        } else if (selector < 0.35F) {
            // leather
            quality = 0;
        } else {
            // chain mail
            quality = 2;
        }

        List<EquipmentSlot> slots = Stream.of(EquipmentSlot.values()).filter(slot -> slot.getType() == EquipmentSlot.Type.ARMOR).sorted(Comparator.reverseOrder()).toList();
        for (EquipmentSlot slot : slots) {
            ItemStack itemStack = this.getItemBySlot(slot);
            if (slot != EquipmentSlot.HEAD && random.nextFloat() < (this.level.getDifficulty() == Difficulty.HARD ? 0.1F : 0.25F)) {
                break;
            }

            if (itemStack.isEmpty()) {
                Item item = getEquipmentForSlot(slot, quality);
                if (item != null) {
                    this.setItemSlot(slot, new ItemStack(item));
                }
            }
        }
    }

    @Override
    protected void enchantSpawnedWeapon(RandomSource random, float chanceMultiplier) {
        super.enchantSpawnedWeapon(random, chanceMultiplier);
        if (random.nextInt(10) == 0) {
            ItemStack itemstack = this.getMainHandItem();
            if (itemstack.is(ModRegistry.HUNTING_BOW.get())) {
                Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(itemstack);
                map.putIfAbsent(Enchantments.PIERCING, 1);
                EnchantmentHelper.setEnchantments(map, itemstack);
                this.setItemSlot(EquipmentSlot.MAINHAND, itemstack);
            }
        }
    }

    @Override
    public boolean canFireProjectileWeapon(ProjectileWeaponItem projectileWeapon) {
        return projectileWeapon instanceof BowItem;
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        spawnData = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
        if (level.getRandom().nextBoolean()) {
            Mob fallenMount = ModRegistry.FALLEN_MOUNT_ENTITY_TYPE.get().create(this.level);
            fallenMount.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
            fallenMount.finalizeSpawn(level, difficulty, MobSpawnType.JOCKEY, null, null);
            this.startRiding(fallenMount);
            level.addFreshEntity(fallenMount);
        }

        this.setCanBreakDoors(level.getRandom().nextFloat() < difficulty.getSpecialMultiplier() * BREAK_DOOR_CHANCE);

        return spawnData;
    }

    public boolean canBreakDoors() {
        return this.canBreakDoors;
    }

    public void setCanBreakDoors(boolean canBreakDoors) {
        if (GoalUtils.hasGroundPathNavigation(this)) {
            if (this.canBreakDoors != canBreakDoors) {
                this.canBreakDoors = canBreakDoors;
                ((GroundPathNavigation) this.getNavigation()).setCanOpenDoors(canBreakDoors);
                if (canBreakDoors) {
                    this.goalSelector.addGoal(1, this.breakDoorGoal);
                } else {
                    this.goalSelector.removeGoal(this.breakDoorGoal);
                }
            }
        } else if (this.canBreakDoors) {
            this.goalSelector.removeGoal(this.breakDoorGoal);
            this.canBreakDoors = false;
        }

    }

    @Override
    public void reassessWeaponGoal() {
        if (this.level != null && !this.level.isClientSide) {
            this.goalSelector.removeGoal(this.meleeGoal());
            this.goalSelector.removeGoal(this.bowGoal());
            ItemStack itemStack = this.getItemInHand(RangedBowEasyAttackGoal.getWeaponHoldingHand(this, stack -> stack.getItem() instanceof BowItem));
            if (itemStack.getItem() instanceof BowItem) {
                int i = 20;
                if (this.level.getDifficulty() != Difficulty.HARD) {
                    i = 40;
                }

                this.bowGoal().setMinAttackInterval(i);
                this.goalSelector.addGoal(4, this.bowGoal());
            } else {
                this.goalSelector.addGoal(4, this.meleeGoal());
            }
        }
    }

    @Override
    public ItemStack getProjectile(ItemStack weaponStack) {
        InteractionHand interactionHand = RangedBowEasyAttackGoal.getWeaponHoldingHand(this, stack -> stack.getItem() instanceof BowItem);
        return super.getProjectile(this.getItemInHand(interactionHand));
    }

    private RangedBowAttackGoal<AbstractSkeleton> bowGoal() {
        if (this.bowGoal == null) {
            this.bowGoal = new RangedBowEasyAttackGoal<>(this, 1.0, 40, 60, 15.0F);
        }
        return this.bowGoal;
    }

    private MeleeAttackGoal meleeGoal() {
        if (this.meleeGoal == null) {
            this.meleeGoal = new MeleeAttackGoal(this, 1.2, false) {

                @Override
                public void stop() {
                    super.stop();
                    FallenKnight.this.setAggressive(false);
                }

                @Override
                public void start() {
                    super.start();
                    FallenKnight.this.setAggressive(true);
                }
            };
        }
        return this.meleeGoal;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.ZOMBIE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_DEATH;
    }

    @Override
    protected SoundEvent getStepSound() {
        return SoundEvents.ZOMBIE_STEP;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("CanBreakDoors", this.canBreakDoors());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.setCanBreakDoors(compound.getBoolean("CanBreakDoors"));
    }
}
