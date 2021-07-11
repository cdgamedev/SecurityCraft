package net.geforcemods.securitycraft.items;

import net.geforcemods.securitycraft.SecurityCraft;
import net.geforcemods.securitycraft.api.IPasswordConvertible;
import net.geforcemods.securitycraft.api.SecurityCraftAPI;
import net.geforcemods.securitycraft.misc.SCSounds;
import net.geforcemods.securitycraft.network.client.PlaySoundAtPos;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemKeyPanel extends Item {

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){
		ItemStack stack = player.getHeldItem(hand);

		for (IPasswordConvertible pc : SecurityCraftAPI.getRegisteredPasswordConvertibles()) {
			if(world.getBlockState(pos).getBlock() == pc.getOriginalBlock())
			{
				if(pc.convert(player, world, pos))
				{
					if(!player.capabilities.isCreativeMode)
						stack.shrink(1);

					if (!world.isRemote)
						SecurityCraft.network.sendToAll(new PlaySoundAtPos(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, SCSounds.LOCK.location.toString(), 1.0F, "block"));

					return EnumActionResult.SUCCESS;
				}
			}
		}


		return EnumActionResult.PASS;
	}
}
