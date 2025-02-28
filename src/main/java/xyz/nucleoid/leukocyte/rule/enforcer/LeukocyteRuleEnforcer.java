package xyz.nucleoid.leukocyte.rule.enforcer;

import net.minecraft.block.Blocks;
import net.minecraft.block.TntBlock;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import xyz.nucleoid.leukocyte.rule.ProtectionRule;
import xyz.nucleoid.leukocyte.rule.ProtectionRuleMap;
import xyz.nucleoid.leukocyte.rule.RuleResult;
import xyz.nucleoid.stimuli.event.EventRegistrar;
import xyz.nucleoid.stimuli.event.block.BlockBreakEvent;
import xyz.nucleoid.stimuli.event.block.BlockDropItemsEvent;
import xyz.nucleoid.stimuli.event.block.BlockPlaceEvent;
import xyz.nucleoid.stimuli.event.block.BlockUseEvent;
import xyz.nucleoid.stimuli.event.entity.EntityUseEvent;
import xyz.nucleoid.stimuli.event.item.ItemCraftEvent;
import xyz.nucleoid.stimuli.event.item.ItemPickupEvent;
import xyz.nucleoid.stimuli.event.item.ItemThrowEvent;
import xyz.nucleoid.stimuli.event.item.ItemUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerAttackEntityEvent;
import xyz.nucleoid.stimuli.event.player.PlayerConsumeHungerEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.world.*;

import java.util.ArrayList;

public final class LeukocyteRuleEnforcer implements ProtectionRuleEnforcer {
    public static final LeukocyteRuleEnforcer INSTANCE = new LeukocyteRuleEnforcer();

    private LeukocyteRuleEnforcer() {
    }

    @Override
    public void applyTo(ProtectionRuleMap rules, EventRegistrar events) {
        this.applyBlockRules(rules, events);
        this.applyInteractionRules(rules, events);

        this.forRule(events, rules.test(ProtectionRule.ATTACK))
                .applySimple(PlayerAttackEntityEvent.EVENT, rule -> (attacker, hand, attacked, hitResult) -> rule);

        this.forRule(events, rules.test(ProtectionRule.ATTACK))
                .applySimple(PlayerAttackEntityEvent.EVENT, rule -> (attacker, hand, attacked, hitResult) -> {
                    return attacked instanceof PlayerEntity ? rule : ActionResult.PASS;
                });

        this.forRule(events, rules.test(ProtectionRule.CRAFTING))
                .applySimple(ItemCraftEvent.EVENT, rule -> (player, recipe) -> rule);

        this.forRule(events, rules.test(ProtectionRule.HUNGER))
                .applySimple(PlayerConsumeHungerEvent.EVENT, rule -> (player, foodLevel, saturation, exhaustion) -> rule);

        this.forRule(events, rules.test(ProtectionRule.FALL_DAMAGE))
                .applySimple(PlayerDamageEvent.EVENT, rule -> {
                    return (player, source, amount) -> source == DamageSource.FALL ? rule : ActionResult.PASS;
                });

        this.forRule(events, rules.test(ProtectionRule.THROW_ITEMS))
                .applySimple(ItemThrowEvent.EVENT, rule -> (player, slot, stack) -> rule);
        this.forRule(events, rules.test(ProtectionRule.PICKUP_ITEMS))
                .applySimple(ItemPickupEvent.EVENT, rule -> (player, entity, stack) -> rule);

        this.applyWorldRules(rules, events);
    }

    private void applyBlockRules(ProtectionRuleMap rules, EventRegistrar events) {
        this.forRule(events, rules.test(ProtectionRule.BREAK))
                .applySimple(BlockBreakEvent.EVENT, rule -> (player, world, pos) -> rule);

        this.forRule(events, rules.test(ProtectionRule.PLACE))
                .applySimple(BlockPlaceEvent.BEFORE, rule -> (player, world, pos, state, context) -> rule);

        this.forRule(events, rules.test(ProtectionRule.BLOCK_DROPS))
                .applySimple(BlockDropItemsEvent.EVENT, rule -> {
                    return (breaker, world, pos, state, dropStacks) -> {
                        if (rule == ActionResult.FAIL) {
                            return new TypedActionResult<>(rule, new ArrayList<>());
                        } else {
                            return new TypedActionResult<>(rule, dropStacks);
                        }
                    };
                });
    }

    private void applyInteractionRules(ProtectionRuleMap rules, EventRegistrar events) {
        RuleResult interact = rules.test(ProtectionRule.INTERACT);
        RuleResult interactBlocks = rules.test(ProtectionRule.INTERACT_BLOCKS).orElse(interact);
        RuleResult interactEntities = rules.test(ProtectionRule.INTERACT_ENTITIES).orElse(interact);
        RuleResult interactItems = rules.test(ProtectionRule.INTERACT_ITEMS).orElse(interact);

        this.forRule(events, interactBlocks)
                .applySimple(BlockUseEvent.EVENT, rule -> (player, hand, hitResult) -> rule);

        this.forRule(events, interactEntities)
                .applySimple(EntityUseEvent.EVENT, rule -> (player, entity, hand, hitResult) -> rule);

        this.forRule(events, interactItems)
                .applySimple(ItemUseEvent.EVENT, rule -> (player, hand) -> new TypedActionResult<>(rule, ItemStack.EMPTY));
    }

    private void applyWorldRules(ProtectionRuleMap rules, EventRegistrar events) {
        this.forRule(events, rules.test(ProtectionRule.UNSTABLE_TNT))
                .applySimple(BlockPlaceEvent.AFTER, rule -> (player, world, pos, state) -> {
                            if (rule == ActionResult.SUCCESS && state.getBlock() == Blocks.TNT) {
                                TntBlock.primeTnt(player.world, pos);
                                player.world.setBlockState(pos, Blocks.AIR.getDefaultState());
                            }
                        }
                );

        this.forRule(events, rules.test(ProtectionRule.PORTALS))
                .applySimple(NetherPortalOpenEvent.EVENT, rule -> (world, lowerCorner) -> rule);

        this.forRule(events, rules.test(ProtectionRule.IGNITE_TNT))
                .applySimple(TntIgniteEvent.EVENT, rule -> (world, pos, igniter) -> rule);

        this.forRule(events, rules.test(ProtectionRule.SPAWN_WITHER))
                .applySimple(WitherSummonEvent.EVENT, rule -> (world, pos) -> rule);

        this.forRule(events, rules.test(ProtectionRule.FIRE_TICK))
                .applySimple(FireTickEvent.EVENT, rule -> (world, pos) -> rule);

        this.forRule(events, rules.test(ProtectionRule.FLUID_FLOW))
                .applySimple(FluidFlowEvent.EVENT, rule -> {
                    return (world, fluidPos, fluidBlock, flowDirection, flowTo, flowToBlock) -> rule;
                });

        this.forRule(events, rules.test(ProtectionRule.ICE_MELT))
                .applySimple(IceMeltEvent.EVENT, rule -> (world, pos) -> rule);
    }
}
