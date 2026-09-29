package pepse.world;

import danogl.gui.rendering.RectangleRenderable;
import danogl.util.Vector2;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Generates ground blocks using a noise-based height function.
 *
 * @author Liad Koren
 */
public class Terrain {
	private static final Color BASE_GROUND_COLOR = new Color(212, 123, 74);
	private static final int TERRAIN_DEPTH = 20;
	private static final int NOISE_FACTOR = Block.SIZE * 7;

	private final float groundHeightAtX0;
	private final pepse.utils.NoiseGenerator noiseGenerator;
	private final int terrainSeed;

	/**
	 * Constructs a terrain generator for a given window and seed.
	 *
	 * @param windowDimensions The window dimensions.
	 * @param seed             The world seed.
	 */
	public Terrain(Vector2 windowDimensions, int seed) {
		this.groundHeightAtX0 = WorldConstants.groundHeightAtX0(windowDimensions);
		int startPoint = (int) groundHeightAtX0;
		this.terrainSeed = seed;
		this.noiseGenerator = new pepse.utils.NoiseGenerator(seed, startPoint);
	}

	/**
	 * Computes ground height at the given X coordinate.
	 *
	 * @param x World X coordinate.
	 * @return Ground height (Y) at X.
	 */
	public float groundHeightAt(float x) {
		float noise = (float) noiseGenerator.noise(x, NOISE_FACTOR);
		return groundHeightAtX0 + noise;
	}

	/**
	 * Creates terrain blocks within the given X-range.
	 *
	 * @param minX Minimum X coordinate.
	 * @param maxX Maximum X coordinate.
	 * @return A list of created terrain blocks.
	 */
	public List<Block> createInRange(int minX, int maxX) {

		int minXClosestMultipliesOfBlockSize = WorldConstants.closestSmallerToMultipliesOfBlockSize(minX);
		int maxXClosestMultipliesOfBlockSize = WorldConstants.closestBiggerToMultipliesOfBlockSize(maxX);
		RectangleRenderable rectangleRenderable;
		List<Block> blocks = new ArrayList<>();
		for (int currentX = minXClosestMultipliesOfBlockSize; currentX <= maxXClosestMultipliesOfBlockSize
				; currentX += Block.SIZE) {
			float currentXGroundHeightY = groundHeightAt(currentX);
			int currentTopY = WorldConstants.floorToBlockSize(currentXGroundHeightY);
			int currentColumnColorSeed = Objects.hash(terrainSeed, currentX);
			Random currentColumnRandom = new Random(currentColumnColorSeed);
			for (int currentDepth = 0; currentDepth < TERRAIN_DEPTH; currentDepth++) {
				int currentDepthInBlockSizes = Block.SIZE * currentDepth;
				int currentY = currentTopY + currentDepthInBlockSizes;
				rectangleRenderable = new
						RectangleRenderable(pepse.utils.ColorSupplier.
						approximateColor(BASE_GROUND_COLOR, currentColumnRandom));
				Block currentBlock = new Block(new Vector2(currentX, currentY), rectangleRenderable);
				currentBlock.setTag(WorldTags.TERRAIN_TAG);
				blocks.add(currentBlock);
			}
		}
		return blocks;
	}

}
