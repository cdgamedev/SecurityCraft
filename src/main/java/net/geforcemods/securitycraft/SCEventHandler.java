package net.geforcemods.securitycraft;

import java.util.List;
import java.util.Random;

import net.geforcemods.securitycraft.api.IModuleInventory;
import net.geforcemods.securitycraft.api.INameSetter;
import net.geforcemods.securitycraft.api.IOwnable;
import net.geforcemods.securitycraft.api.IPasswordConvertible;
import net.geforcemods.securitycraft.api.IPasswordProtected;
import net.geforcemods.securitycraft.api.LinkableTileEntity;
import net.geforcemods.securitycraft.api.LinkedAction;
import net.geforcemods.securitycraft.api.SecurityCraftAPI;
import net.geforcemods.securitycraft.blocks.SecurityCameraBlock;
import net.geforcemods.securitycraft.blocks.reinforced.IReinforcedBlock;
import net.geforcemods.securitycraft.entity.SecurityCameraEntity;
import net.geforcemods.securitycraft.entity.SentryEntity;
import net.geforcemods.securitycraft.items.ModuleItem;
import net.geforcemods.securitycraft.items.UniversalBlockReinforcerItem;
import net.geforcemods.securitycraft.misc.CustomDamageSources;
import net.geforcemods.securitycraft.misc.ModuleType;
import net.geforcemods.securitycraft.misc.OwnershipEvent;
import net.geforcemods.securitycraft.misc.SCSounds;
import net.geforcemods.securitycraft.network.client.PlaySoundAtPos;
import net.geforcemods.securitycraft.network.client.SendTip;
import net.geforcemods.securitycraft.tileentity.PortableRadarTileEntity;
import net.geforcemods.securitycraft.tileentity.SecurityCameraTileEntity;
import net.geforcemods.securitycraft.util.PlayerUtils;
import net.geforcemods.securitycraft.util.Utils;
import net.geforcemods.securitycraft.util.WorldUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingDestroyBlockEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.network.PacketDistributor;

@EventBusSubscriber(modid=SecurityCraft.MODID)
public class SCEventHandler {
	private static final String PREVIOUS_PLAYER_POS_NBT = "SecurityCraftPreviousPlayerPos";

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerLoggedInEvent event){
		PlayerEntity player = event.getPlayer();

		if(player.getPersistentData().contains(PREVIOUS_PLAYER_POS_NBT))
		{
			BlockPos pos = BlockPos.fromLong(player.getPersistentData().getLong(PREVIOUS_PLAYER_POS_NBT));

			player.getPersistentData().remove(PREVIOUS_PLAYER_POS_NBT);
			player.setPositionAndUpdate(pos.getX(), pos.getY(), pos.getZ());
		}

		if(!ConfigHandler.SERVER.disableThanksMessage.get())
			SecurityCraft.channel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity)player), new SendTip());
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerLoggedOutEvent event)
	{
		PlayerEntity player = event.getPlayer();

		if(PlayerUtils.isPlayerMountedOnCamera(player))
		{
			BlockPos pos = new BlockPos(((SecurityCameraEntity)player.getRidingEntity()).getPreviousPlayerPos());

			player.getRidingEntity().remove();
			player.getPersistentData().putLong(PREVIOUS_PLAYER_POS_NBT, pos.toLong());
		}
	}

	@SubscribeEvent
	public static void onDamageTaken(LivingHurtEvent event)
	{
		if(event.getEntity() != null && PlayerUtils.isPlayerMountedOnCamera(event.getEntityLiving())){
			event.setCanceled(true);
			return;
		}

		if(event.getSource() == CustomDamageSources.ELECTRICITY)
			SecurityCraft.channel.send(PacketDistributor.ALL.noArg(), new PlaySoundAtPos(event.getEntity().getPosX(), event.getEntity().getPosY(), event.getEntity().getPosZ(), SCSounds.ELECTRIFIED.path, 0.25F, "blocks"));
	}

	//disallow rightclicking doors, fixes wrenches from other mods being able to switch their state
	//side effect for keypad door: it is now only openable with an empty hand
	@SubscribeEvent(priority=EventPriority.HIGHEST)
	public static void highestPriorityOnRightClickBlock(PlayerInteractEvent.RightClickBlock event)
	{
		ItemStack stack = event.getItemStack();
		Item item = stack.getItem();

		if(!stack.isEmpty() && item != SCContent.UNIVERSAL_BLOCK_REMOVER.get() && item != SCContent.UNIVERSAL_BLOCK_MODIFIER.get() && item != SCContent.UNIVERSAL_OWNER_CHANGER.get())
		{
			if(!(item instanceof BlockItem))
			{
				Block block = event.getWorld().getBlockState(event.getPos()).getBlock();

				if(block == SCContent.KEYPAD_DOOR.get() || block == SCContent.REINFORCED_DOOR.get() || block == SCContent.REINFORCED_IRON_TRAPDOOR.get() || block == SCContent.SCANNER_DOOR.get())
					event.setCanceled(true);
			}
		}
	}

	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event){
		if(PlayerUtils.isPlayerMountedOnCamera(event.getPlayer()))
		{
			event.setCanceled(true);
			return;
		}

		World world = event.getWorld();

		if(!world.isRemote){
			TileEntity te = world.getTileEntity(event.getPos());
			BlockState state  = world.getBlockState(event.getPos());
			Block block = state.getBlock();

			if(PlayerUtils.isHoldingItem(event.getPlayer(), SCContent.KEY_PANEL, event.getHand()))
			{
				for(IPasswordConvertible pc : SecurityCraftAPI.getRegisteredPasswordConvertibles())
				{
					if(pc.getOriginalBlock() == block)
					{
						event.setUseBlock(Result.DENY);
						event.setUseItem(Result.ALLOW);
					}
				}

				return;
			}

			if(PlayerUtils.isHoldingItem(event.getPlayer(), SCContent.CODEBREAKER, event.getHand()) && handleCodebreaking(event)) {
				event.setCanceled(true);
				return;
			}

			if(te instanceof INameSetter && (te instanceof SecurityCameraTileEntity || te instanceof PortableRadarTileEntity) && PlayerUtils.isHoldingItem(event.getPlayer(), Items.NAME_TAG, event.getHand()) && event.getPlayer().getHeldItem(event.getHand()).hasDisplayName()){
				ItemStack nametag = event.getPlayer().getHeldItem(event.getHand());
				INameSetter nameable = (INameSetter)te;

				event.setCanceled(true);
				event.setCancellationResult(ActionResultType.SUCCESS);

				if(nametag.getDisplayName().equals(nameable.getCustomName())) {
					PlayerUtils.sendMessageToPlayer(event.getPlayer(), Utils.localize(te.getBlockState().getBlock().getTranslationKey()), Utils.localize("messages.securitycraft:naming.alreadyMatches", nameable.getCustomName()), TextFormatting.RED);
					return;
				}

				if(!event.getPlayer().isCreative())
					nametag.shrink(1);

				nameable.setCustomName(nametag.getDisplayName());
				PlayerUtils.sendMessageToPlayer(event.getPlayer(), Utils.localize(te.getBlockState().getBlock().getTranslationKey()), Utils.localize("messages.securitycraft:naming.named", nameable.getCustomName()), TextFormatting.RED);
				return;
			}
		}

		//outside !world.isRemote for properly checking the interaction
		//all the sentry functionality for when the sentry is diguised
		List<SentryEntity> sentries = world.getEntitiesWithinAABB(SentryEntity.class, new AxisAlignedBB(event.getPos()));

		if(!sentries.isEmpty())
			event.setCanceled(sentries.get(0).processInteract(event.getPlayer(), event.getHand())); //cancel if an action was taken
	}

	@SubscribeEvent
	public static void onLeftClickBlock(LeftClickBlock event) {
		if(PlayerUtils.isPlayerMountedOnCamera(event.getPlayer())) {
			event.setCanceled(true);
			return;
		}

		ItemStack stack = event.getPlayer().getHeldItemMainhand();
		Item held = stack.getItem();

		if(held == SCContent.UNIVERSAL_BLOCK_REINFORCER_LVL_1.get() || held == SCContent.UNIVERSAL_BLOCK_REINFORCER_LVL_2.get() || held == SCContent.UNIVERSAL_BLOCK_REINFORCER_LVL_3.get())
			UniversalBlockReinforcerItem.convertBlock(stack, event.getPos(), event.getPlayer());
	}

	@SubscribeEvent
	public static void onAttackEntity(AttackEntityEvent event) {
		if(PlayerUtils.isPlayerMountedOnCamera(event.getPlayer())) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onEntityInteracted(EntityInteract event) {
		if(PlayerUtils.isPlayerMountedOnCamera(event.getPlayer())) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onBlockEventBreak(BlockEvent.BreakEvent event)
	{
		if(!(event.getWorld() instanceof World))
			return;

		if(!event.getWorld().isRemote()) {
			if(event.getWorld().getTileEntity(event.getPos()) instanceof IModuleInventory){
				IModuleInventory te = (IModuleInventory) event.getWorld().getTileEntity(event.getPos());

				for(int i = 0; i < te.getMaxNumberOfModules(); i++)
					if(!te.getInventory().get(i).isEmpty()){
						ItemStack stack = te.getInventory().get(i);
						ItemEntity item = new ItemEntity((World)event.getWorld(), event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(), stack);
						WorldUtils.addScheduledTask(event.getWorld(), () -> event.getWorld().addEntity(item));

						te.onModuleRemoved(stack, ((ModuleItem) stack.getItem()).getModuleType());

						if(te instanceof LinkableTileEntity)
							((LinkableTileEntity)te).createLinkedBlockAction(LinkedAction.MODULE_REMOVED, new Object[]{ stack, ((ModuleItem) stack.getItem()).getModuleType() }, (LinkableTileEntity)te);

						if(te instanceof SecurityCameraTileEntity)
						{
							SecurityCameraTileEntity cam = (SecurityCameraTileEntity)te;

							cam.getWorld().notifyNeighborsOfStateChange(cam.getPos().offset(cam.getWorld().getBlockState(cam.getPos()).get(SecurityCameraBlock.FACING), -1), cam.getWorld().getBlockState(cam.getPos()).getBlock());
						}
					}
			}
		}

		List<SentryEntity> sentries = ((World)event.getWorld()).getEntitiesWithinAABB(SentryEntity.class, new AxisAlignedBB(event.getPos()));

		//don't let people break the disguise block
		if(!sentries.isEmpty() && !sentries.get(0).getDisguiseModule().isEmpty())
		{
			ItemStack disguiseModule = sentries.get(0).getDisguiseModule();
			Block block = ((ModuleItem)disguiseModule.getItem()).getBlockAddon(disguiseModule.getTag());

			if(block == event.getWorld().getBlockState(event.getPos()).getBlock())
				event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onOwnership(OwnershipEvent event)
	{
		TileEntity te = event.getWorld().getTileEntity(event.getPos());

		if(te instanceof IOwnable) {
			String name = event.getPlayer().getName().getFormattedText();
			String uuid = event.getPlayer().getGameProfile().getId().toString();

			((IOwnable)te).setOwner(uuid, name);
		}
	}

	@SubscribeEvent
	public static void onLivingSetAttackTarget(LivingSetAttackTargetEvent event)
	{
		if((event.getTarget() instanceof PlayerEntity && PlayerUtils.isPlayerMountedOnCamera(event.getTarget())) || event.getTarget() instanceof SentryEntity)
			((MobEntity)event.getEntity()).setAttackTarget(null);
	}

	@SubscribeEvent
	public static void onLivingDestroyEvent(LivingDestroyBlockEvent event)
	{
		event.setCanceled(event.getEntity() instanceof WitherEntity && event.getState().getBlock() instanceof IReinforcedBlock);
	}

	@SubscribeEvent
	public void onEntityMount(EntityMountEvent event)
	{
		if(event.isDismounting() && event.getEntityBeingMounted() instanceof SecurityCameraEntity && event.getEntityMounting() instanceof PlayerEntity)
		{
			PlayerEntity player = (PlayerEntity)event.getEntityMounting();
			TileEntity te = event.getWorldObj().getTileEntity(event.getEntityBeingMounted().getPosition());

			if(PlayerUtils.isPlayerMountedOnCamera(player) && te instanceof SecurityCameraTileEntity && ((SecurityCameraTileEntity)te).hasModule(ModuleType.SMART))
			{
				((SecurityCameraTileEntity)te).lastPitch = player.rotationPitch;
				((SecurityCameraTileEntity)te).lastYaw = player.rotationYaw;
			}
		}
	}

	@SubscribeEvent
	public static void onRightClickItem(PlayerInteractEvent.RightClickItem event)
	{
		if(PlayerUtils.isPlayerMountedOnCamera(event.getPlayer()) && event.getItemStack().getItem() != SCContent.CAMERA_MONITOR.get())
			event.setCanceled(true);
	}

	private static boolean handleCodebreaking(PlayerInteractEvent.RightClickBlock event) {
		World world = event.getPlayer().world;
		TileEntity tileEntity = world.getTileEntity(event.getPos());

		if(tileEntity instanceof IPasswordProtected && ((IPasswordProtected)tileEntity).isCodebreakable())
		{
			if(ConfigHandler.SERVER.allowCodebreakerItem.get())
			{
				if(event.getPlayer().getHeldItem(event.getHand()).getItem() == SCContent.CODEBREAKER.get())
					event.getPlayer().getHeldItem(event.getHand()).damageItem(1, event.getPlayer(), p -> p.sendBreakAnimation(event.getHand()));

				if(event.getPlayer().isCreative() || new Random().nextInt(3) == 1)
					return ((IPasswordProtected) tileEntity).onCodebreakerUsed(world.getBlockState(event.getPos()), event.getPlayer());
				else {
					PlayerUtils.sendMessageToPlayer(event.getPlayer(), Utils.localize(SCContent.CODEBREAKER.get().getTranslationKey()), Utils.localize("messages.securitycraft:codebreaker.failed"), TextFormatting.RED);
					return true;
				}
			}
			else {
				Block block = world.getBlockState(event.getPos()).getBlock();

				PlayerUtils.sendMessageToPlayer(event.getPlayer(), Utils.localize(block.getTranslationKey()), Utils.localize("messages.securitycraft:codebreakerDisabled"), TextFormatting.RED);
			}
		}

		return false;
	}
}
