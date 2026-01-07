package com.yardenzamir.personalmesystem.block;

import appeng.api.orientation.IOrientationStrategy;
import appeng.api.orientation.OrientationStrategies;
import appeng.api.orientation.RelativeSide;
import appeng.block.AEBaseEntityBlock;
import com.yardenzamir.personalmesystem.menu.CommunicationRelayMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Communication Relay block - provides virtual recipes from its NBT to connected ME networks.
 * Visually identical to the ME Wireless Access Point.
 */
public class CommunicationRelayBlock extends AEBaseEntityBlock<CommunicationRelayBlockEntity>
        implements SimpleWaterloggedBlock {

    public enum State implements StringRepresentable {
        OFF, ON, HAS_CHANNEL;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<State> STATE = EnumProperty.create("state", State.class);
    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public CommunicationRelayBlock() {
        super(glassProps().noOcclusion().forceSolidOn());
        registerDefaultState(defaultBlockState()
                .setValue(STATE, State.OFF)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected BlockState updateBlockStateFromBlockEntity(BlockState currentState, CommunicationRelayBlockEntity be) {
        State state = State.OFF;
        if (be.isActive()) {
            state = State.HAS_CHANNEL;
        } else if (be.isPowered()) {
            state = State.ON;
        }
        return currentState.setValue(STATE, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(STATE, WATERLOGGED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getVoxelShape(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getVoxelShape(state);
    }

    @NotNull
    private VoxelShape getVoxelShape(BlockState state) {
        var orientation = getOrientation(state);
        var forward = orientation.getSide(RelativeSide.FRONT);

        double minX = 0, minY = 0, minZ = 0;
        double maxX = 1, maxY = 1, maxZ = 1;

        switch (forward) {
            case DOWN -> {
                minZ = minX = 3.0 / 16.0;
                maxZ = maxX = 13.0 / 16.0;
                maxY = 1.0;
                minY = 5.0 / 16.0;
            }
            case EAST -> {
                minZ = minY = 3.0 / 16.0;
                maxZ = maxY = 13.0 / 16.0;
                maxX = 11.0 / 16.0;
                minX = 0.0;
            }
            case NORTH -> {
                minY = minX = 3.0 / 16.0;
                maxY = maxX = 13.0 / 16.0;
                maxZ = 1.0;
                minZ = 5.0 / 16.0;
            }
            case SOUTH -> {
                minY = minX = 3.0 / 16.0;
                maxY = maxX = 13.0 / 16.0;
                maxZ = 11.0 / 16.0;
                minZ = 0.0;
            }
            case UP -> {
                minZ = minX = 3.0 / 16.0;
                maxZ = maxX = 13.0 / 16.0;
                maxY = 11.0 / 16.0;
                minY = 0.0;
            }
            case WEST -> {
                minZ = minY = 3.0 / 16.0;
                maxZ = maxY = 13.0 / 16.0;
                maxX = 1.0;
                minX = 5.0 / 16.0;
            }
        }

        return Shapes.create(new AABB(minX, minY, minZ, maxX, maxY, maxZ));
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }

    @Override
    public IOrientationStrategy getOrientationStrategy() {
        return OrientationStrategies.facing();
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return super.getStateForPlacement(context)
                .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState,
                                  LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CommunicationRelayBlockEntity relay)) {
            return InteractionResult.PASS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            var menu = new CommunicationRelayMenu(-1, serverPlayer.getInventory(), relay);
            NetworkHooks.openScreen(serverPlayer,
                    new SimpleMenuProvider(
                            (id, inv, p) -> new CommunicationRelayMenu(id, inv, relay),
                            Component.translatable("block.personalmesystem.communication_relay")
                    ),
                    buf -> menu.writeToBuffer(buf, pos)
            );
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return CommunicationRelayBlockEntity.create(pos, state);
    }
}
