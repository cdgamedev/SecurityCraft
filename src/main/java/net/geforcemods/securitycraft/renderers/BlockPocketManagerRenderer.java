package net.geforcemods.securitycraft.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;

import net.geforcemods.securitycraft.blockentities.BlockPocketManagerBlockEntity;
import net.geforcemods.securitycraft.blocks.BlockPocketManagerBlock;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

public class BlockPocketManagerRenderer implements BlockEntityRenderer<BlockPocketManagerBlockEntity> {
	public BlockPocketManagerRenderer(BlockEntityRendererProvider.Context ctx) {}

	@Override
	public void render(BlockPocketManagerBlockEntity be, float partialTicks, PoseStack pose, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
		// The code below draws the outline border of a block pocket.

		if (!be.showOutline)
			return;

		Matrix4f positionMatrix = pose.last().pose();
		Direction facing = be.getBlockState().getValue(BlockPocketManagerBlock.FACING);
		VertexConsumer builder = buffer.getBuffer(RenderType.lines());
		int offset = facing == Direction.NORTH || facing == Direction.EAST ? -be.autoBuildOffset : be.autoBuildOffset; //keep negative values moving the offset to the left consistent
		int size = be.size;
		int half = (size - 1) / 2;
		int leftX = -half + offset;
		int rightX = half + 1 + offset;
		int frontZ = facing == Direction.NORTH || facing == Direction.WEST ? 0 : 1;
		int backZ = facing == Direction.NORTH || facing == Direction.WEST ? size : 1 - size;

		//x- and z-values get switched when the manager's direction is west or east
		if (facing == Direction.EAST || facing == Direction.WEST) {
			leftX = frontZ;
			rightX = backZ;
			frontZ = -half + offset;
			backZ = half + 1 + offset;
		}

		//bottom lines
		builder.vertex(positionMatrix, leftX, 0.0F, frontZ).color(0, 0, 255, 255).normal(0.0F, 0.0F, 1.0F).endVertex();
		builder.vertex(positionMatrix, rightX, 0.0F, frontZ).color(0, 0, 255, 255).normal(0.0F, 0.0F, 1.0F).endVertex();
		builder.vertex(positionMatrix, leftX, 0.0F, backZ).color(0, 0, 255, 255).normal(1.0F, 1.0F, 1.0F).endVertex();
		builder.vertex(positionMatrix, rightX, 0.0F, backZ).color(0, 0, 255, 255).normal(1.0F, 1.0F, 1.0F).endVertex();
		builder.vertex(positionMatrix, leftX, 0.0F, frontZ).color(0, 0, 255, 255).normal(1.0F, 1.0F, 1.0F).endVertex();
		builder.vertex(positionMatrix, leftX, 0.0F, backZ).color(0, 0, 255, 255).normal(1.0F, 1.0F, 1.0F).endVertex();
		builder.vertex(positionMatrix, rightX, 0.0F, frontZ).color(0, 0, 255, 255).normal(1.0F, 1.0F, 1.0F).endVertex();
		builder.vertex(positionMatrix, rightX, 0.0F, backZ).color(0, 0, 255, 255).normal(1.0F, 1.0F, 1.0F).endVertex();
		//top lines
		builder.vertex(positionMatrix, leftX, size, frontZ).color(0, 0, 255, 255).normal(1.0F, 1.0F, 1.0F).endVertex();
		builder.vertex(positionMatrix, rightX, size, frontZ).color(0, 0, 255, 255).normal(1.0F, 1.0F, 1.0F).endVertex();
		builder.vertex(positionMatrix, leftX, size, backZ).color(0, 0, 255, 255).normal(1.0F, 1.0F, 1.0F).endVertex();
		builder.vertex(positionMatrix, rightX, size, backZ).color(0, 0, 255, 255).normal(1.0F, 1.0F, 1.0F).endVertex();
		builder.vertex(positionMatrix, leftX, size, frontZ).color(0, 0, 255, 255).normal(1.0F, 1.0F, 1.0F).endVertex();
		builder.vertex(positionMatrix, leftX, size, backZ).color(0, 0, 255, 255).normal(1.0F, 1.0F, 1.0F).endVertex();
		builder.vertex(positionMatrix, rightX, size, frontZ).color(0, 0, 255, 255).normal(1.0F, 1.0F, 1.0F).endVertex();
		builder.vertex(positionMatrix, rightX, size, backZ).color(0, 0, 255, 255).normal(1.0F, 1.0F, 1.0F).endVertex();
		//corner edge lines
		builder.vertex(positionMatrix, leftX, 0.0F, frontZ).color(0, 0, 255, 255).normal(1.0F, 1.0F, 1.0F).endVertex();
		builder.vertex(positionMatrix, leftX, size, frontZ).color(0, 0, 255, 255).normal(1.0F, 1.0F, 1.0F).endVertex();
		builder.vertex(positionMatrix, leftX, 0.0F, backZ).color(0, 0, 255, 255).normal(1.0F, 1.0F, 1.0F).endVertex();
		builder.vertex(positionMatrix, leftX, size, backZ).color(0, 0, 255, 255).normal(1.0F, 1.0F, 1.0F).endVertex();
		builder.vertex(positionMatrix, rightX, 0.0F, backZ).color(0, 0, 255, 255).normal(1.0F, 1.0F, 1.0F).endVertex();
		builder.vertex(positionMatrix, rightX, size, backZ).color(0, 0, 255, 255).normal(1.0F, 1.0F, 1.0F).endVertex();
		builder.vertex(positionMatrix, rightX, 0.0F, frontZ).color(0, 0, 255, 255).normal(1.0F, 1.0F, 1.0F).endVertex();
		builder.vertex(positionMatrix, rightX, size, frontZ).color(0, 0, 255, 255).normal(1.0F, 1.0F, 1.0F).endVertex();
	}

	@Override
	public boolean shouldRenderOffScreen(BlockPocketManagerBlockEntity be) {
		return be.showOutline;
	}
}
