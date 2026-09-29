package pepse.world.trees;

import danogl.GameObject;
import danogl.gui.rendering.RectangleRenderable;
import danogl.util.Vector2;
import pepse.utils.ColorSupplier;
import pepse.world.Block;
import pepse.world.WorldConstants;
import pepse.world.WorldTags;

import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;

/**
 * Builds a single tree composed of trunk blocks, leaves, and fruits.
 * The top tree area is represented as a matrix,
 * where some cells are marked illegal due to neighbor limits or ground overlap.
 *
 * @author Liad Koren
 */
public class Tree {
	private static final Color BASE_TRUNK_COLOR = new Color(100, 50, 20);
	private static final int MIN_TRUNK_HEIGHT_IN_BLOCKS = 4;
	private static final int MAX_TRUNK_HEIGHT_IN_BLOCKS = 7;
	private static final int MIN_LEAF_COUNT = 12;
	private static final int MAX_LEAF_COUNT = 20;
	private static final int MIN_FRUIT_COUNT = 3;
	private static final int MAX_FRUIT_COUNT = 8;
	private static final int MAX_RANDOM_PLACEMENT_ATTEMPTS_MULTIPLIER = 10;
	private static final int SINGLE_STEP = 1;
	private static final int DIVIDE_BY_TWO_FACTOR = 2;
	private static final int MIN_NON_NEGATIVE = 0;

	private Tree() {

	}

	/**
	 * Creates a tree at the given trunk X position and adds created objects to the given output lists.
	 *
	 * @param trunkColumnWorldX             The world X coordinate of the trunk column (aligned to the grid).
	 * @param randomGenerator                      Random generator used for tree variability.
	 * @param seed                                 World seed used for leaf determinism.
	 * @param trunkBlocksOutput                    Output list for trunk blocks.
	 * @param leafBlocksOutput                     Output list for leaf objects.
	 * @param fruitBlocksOutput                    Output list for fruit objects.
	 * @param cycleLength                          Day cycle length used for fruit respawn timing.
	 * @param minTopTreeHalfWidthInBlocks          Minimum allowed half width for the top-tree.
	 * @param maxTopTreeHalfWidthInBlocks          Maximum allowed half width for the top-tree.
	 * @param leftAllowedTopTreeHalfWidthInBlocks  Allowed half width to the left, limited by neighbors.
	 * @param rightAllowedTopTreeHalfWidthInBlocks Allowed half width to the right, limited by neighbors.
	 * @param groundHeightFunction                 Function returning ground height for a given X coordinate.
	 */
	public static void create(int trunkColumnWorldX, Random randomGenerator, int seed,
							  List<GameObject> trunkBlocksOutput, List<GameObject> leafBlocksOutput,
							  List<GameObject> fruitBlocksOutput, float cycleLength,
							  int minTopTreeHalfWidthInBlocks, int maxTopTreeHalfWidthInBlocks,
							  int leftAllowedTopTreeHalfWidthInBlocks,
							  int rightAllowedTopTreeHalfWidthInBlocks,
							  Function<Float, Float> groundHeightFunction) {
		Tree treeBuilder = new Tree();
		float groundHeightAtTrunkX = groundHeightFunction.apply((float) trunkColumnWorldX);
		int groundTopYAlignedToGridAtTrunk = WorldConstants.floorToBlockSize(groundHeightAtTrunkX);
		int trunkHeightInBlocks = treeBuilder.generateRandomTrunkHeightInBlocks(randomGenerator);
		int trunkBottomWorldY = groundTopYAlignedToGridAtTrunk - Block.SIZE;
		int trunkTopWorldY = trunkBottomWorldY - (trunkHeightInBlocks - SINGLE_STEP) * Block.SIZE;
		treeBuilder.createTrunkBlocks(trunkColumnWorldX, trunkBottomWorldY, trunkHeightInBlocks,
				trunkBlocksOutput, seed);
		int topTreeHalfWidthInBlocks = treeBuilder.generateRandomTopTreeHalfWidthInBlocks(randomGenerator,
				minTopTreeHalfWidthInBlocks, maxTopTreeHalfWidthInBlocks);
		int topTreeFullWidthInBlocks = DIVIDE_BY_TWO_FACTOR * topTreeHalfWidthInBlocks + SINGLE_STEP;
		TopTreeCellState[][] topTreeGrid =
				new TopTreeCellState[topTreeFullWidthInBlocks][topTreeFullWidthInBlocks];
		treeBuilder.initializeTopTreeGridToEmpty(topTreeGrid, topTreeFullWidthInBlocks);
		treeBuilder.markIllegalTopTreeCells(topTreeGrid, topTreeFullWidthInBlocks, topTreeHalfWidthInBlocks,
				trunkColumnWorldX, trunkTopWorldY, leftAllowedTopTreeHalfWidthInBlocks,
				rightAllowedTopTreeHalfWidthInBlocks, groundHeightFunction);
		int targetLeafCount = treeBuilder.generateRandomLeafCount(randomGenerator,
				topTreeFullWidthInBlocks);
		treeBuilder.placeLeavesRandomly(topTreeGrid, targetLeafCount,
				randomGenerator, topTreeFullWidthInBlocks);
		treeBuilder.createLeavesFromTopTreeGrid(trunkColumnWorldX, trunkTopWorldY, topTreeGrid,
				leafBlocksOutput, topTreeHalfWidthInBlocks, topTreeFullWidthInBlocks, seed);
		int targetFruitCount = treeBuilder.generateRandomFruitCount(randomGenerator);
		treeBuilder.placeFruitsRandomly(trunkColumnWorldX, trunkTopWorldY, topTreeGrid, targetFruitCount,
				randomGenerator, fruitBlocksOutput, cycleLength, topTreeHalfWidthInBlocks,
				topTreeFullWidthInBlocks, seed);
	}

	/**
	 * Marks grid cells as illegal based on neighbor width limits and ground overlap.
	 *
	 * @param topTreeGrid                   The top-tree grid to update.
	 * @param topTreeFullWidthInBlocks      Full width of the grid (in cells).
	 * @param topTreeHalfWidthInBlocks      Half width of the grid (in cells).
	 * @param trunkColumnWorldX             Trunk world X coordinate.
	 * @param trunkTopWorldY                Trunk top world Y coordinate.
	 * @param leftAllowedHalfWidthInBlocks  Allowed half width to the left.
	 * @param rightAllowedHalfWidthInBlocks Allowed half width to the right.
	 * @param groundHeightFunction          Ground height function.
	 */
	private void markIllegalTopTreeCells(TopTreeCellState[][] topTreeGrid, int topTreeFullWidthInBlocks,
										 int topTreeHalfWidthInBlocks, int trunkColumnWorldX,
										 int trunkTopWorldY, int leftAllowedHalfWidthInBlocks,
										 int rightAllowedHalfWidthInBlocks,
										 Function<Float, Float> groundHeightFunction) {
		int trunkColumnInGrid = topTreeFullWidthInBlocks / DIVIDE_BY_TWO_FACTOR;
		for (int rowIndex = 0; rowIndex < topTreeFullWidthInBlocks; rowIndex++) {
			for (int colIndex = 0; colIndex < topTreeFullWidthInBlocks; colIndex++) {
				boolean isOutsideAllowedHorizontalRange =
						(colIndex < trunkColumnInGrid - leftAllowedHalfWidthInBlocks)
								|| (colIndex > trunkColumnInGrid + rightAllowedHalfWidthInBlocks);
				if (isOutsideAllowedHorizontalRange) {
					topTreeGrid[rowIndex][colIndex] = TopTreeCellState.ILLEGAL;
					continue;
				}
				Vector2 cellTopLeftWorld = topTreeCellToWorldTopLeft(trunkColumnWorldX, trunkTopWorldY,
						rowIndex, colIndex, topTreeHalfWidthInBlocks);
				float groundHeightAtCellX = groundHeightFunction.apply(cellTopLeftWorld.x());
				int groundTopYAligned = WorldConstants.floorToBlockSize(groundHeightAtCellX);
				boolean cellHitsGroundOrBelow = cellTopLeftWorld.y() >= (groundTopYAligned - Block.SIZE);
				if (cellHitsGroundOrBelow) {
					topTreeGrid[rowIndex][colIndex] = TopTreeCellState.ILLEGAL;
				}
			}
		}
	}

	/**
	 * Generates a random trunk height (in blocks).
	 *
	 * @param randomGenerator Random source.
	 * @return Trunk height in blocks.
	 */
	private int generateRandomTrunkHeightInBlocks(Random randomGenerator) {
		int possibleHeightsCount = MAX_TRUNK_HEIGHT_IN_BLOCKS - MIN_TRUNK_HEIGHT_IN_BLOCKS + SINGLE_STEP;
		return MIN_TRUNK_HEIGHT_IN_BLOCKS + randomGenerator.nextInt(possibleHeightsCount);
	}

	/**
	 * Creates trunk blocks and adds them to the output list.
	 *
	 * @param trunkColumnWorldX   Trunk world X coordinate.
	 * @param trunkBottomWorldY   Trunk bottom world Y coordinate.
	 * @param trunkHeightInBlocks Trunk height in blocks.
	 * @param trunkBlocksOutput   Output list for trunk blocks.
	 */
	private void createTrunkBlocks(int trunkColumnWorldX, int trunkBottomWorldY, int trunkHeightInBlocks,
								   List<GameObject> trunkBlocksOutput, int seed) {
		int trunkColorSeed = Objects.hash(seed, trunkColumnWorldX);
		Random trunkRandom = new Random(trunkColorSeed);
		for (int heightIndex = 0; heightIndex < trunkHeightInBlocks; heightIndex++) {
			int currentBlockWorldY = trunkBottomWorldY - heightIndex * Block.SIZE;
			RectangleRenderable trunkRenderable =
					new RectangleRenderable(ColorSupplier.approximateColor(BASE_TRUNK_COLOR, trunkRandom));
			Block trunkBlock = new Block(new Vector2(trunkColumnWorldX, currentBlockWorldY),
					trunkRenderable);
			trunkBlock.setTag(WorldTags.TRUNK_TAG);
			trunkBlocksOutput.add(trunkBlock);
		}
	}

	/**
	 * Generates a random half width for the top-tree grid.
	 *
	 * @param randomGenerator Random source.
	 * @param minHalfWidth    Minimum allowed half width.
	 * @param maxHalfWidth    Maximum allowed half width.
	 * @return Top-tree half width in blocks.
	 */
	private int generateRandomTopTreeHalfWidthInBlocks(Random randomGenerator, int minHalfWidth,
													   int maxHalfWidth) {
		int adjustedMin = minHalfWidth;
		int adjustedMax = maxHalfWidth;
		if (adjustedMin > adjustedMax) {
			int tempHolder = adjustedMin;
			adjustedMin = adjustedMax;
			adjustedMax = tempHolder;
		}
		if (adjustedMin == adjustedMax) {
			return adjustedMin;
		}
		int possibleSizesCount = adjustedMax - adjustedMin + SINGLE_STEP;
		return adjustedMin + randomGenerator.nextInt(possibleSizesCount);
	}

	/**
	 * Initializes all grid cells as {@link TopTreeCellState#EMPTY}.
	 *
	 * @param topTreeGrid              The grid to initialize.
	 * @param topTreeFullWidthInBlocks Full width of the grid (in cells).
	 */
	private void initializeTopTreeGridToEmpty(TopTreeCellState[][] topTreeGrid,
											  int topTreeFullWidthInBlocks) {
		for (int rowIndex = 0; rowIndex < topTreeFullWidthInBlocks; rowIndex++) {
			for (int colIndex = 0; colIndex < topTreeFullWidthInBlocks; colIndex++) {
				topTreeGrid[rowIndex][colIndex] = TopTreeCellState.EMPTY;
			}
		}
	}

	/**
	 * Generates a random target leaf count, capped by the grid size.
	 *
	 * @param randomGenerator          Random source.
	 * @param topTreeFullWidthInBlocks Grid width (in cells).
	 * @return Target leaf count.
	 */
	private int generateRandomLeafCount(Random randomGenerator, int topTreeFullWidthInBlocks) {
		int maxPossibleCells = topTreeFullWidthInBlocks * topTreeFullWidthInBlocks;
		int cappedMaxLeafCount = Math.min(MAX_LEAF_COUNT, maxPossibleCells);
		if (MIN_LEAF_COUNT >= cappedMaxLeafCount) {
			return cappedMaxLeafCount;
		}
		int possibleCounts = cappedMaxLeafCount - MIN_LEAF_COUNT + SINGLE_STEP;
		return MIN_LEAF_COUNT + randomGenerator.nextInt(possibleCounts);
	}

	/**
	 * Places leaves randomly into currently empty grid cells.
	 *
	 * @param topTreeGrid              The grid to update.
	 * @param targetLeafCount          Desired leaf count.
	 * @param randomGenerator          Random source.
	 * @param topTreeFullWidthInBlocks Grid width (in cells).
	 */
	private void placeLeavesRandomly(TopTreeCellState[][] topTreeGrid,
									 int targetLeafCount,
									 Random randomGenerator,
									 int topTreeFullWidthInBlocks) {
		int totalCells = topTreeFullWidthInBlocks * topTreeFullWidthInBlocks;
		int wantedLeafCounter = Math.min(targetLeafCount, totalCells);
		int placedLeafCounter = 0;
		int remainingPlacingAttempts = MAX_RANDOM_PLACEMENT_ATTEMPTS_MULTIPLIER * totalCells;
		while (placedLeafCounter < wantedLeafCounter && remainingPlacingAttempts > 0) {
			remainingPlacingAttempts = remainingPlacingAttempts - 1;
			int rowIndex = randomGenerator.nextInt(topTreeFullWidthInBlocks);
			int colIndex = randomGenerator.nextInt(topTreeFullWidthInBlocks);
			if (topTreeGrid[rowIndex][colIndex] != TopTreeCellState.EMPTY) {
				continue;
			}
			topTreeGrid[rowIndex][colIndex] = TopTreeCellState.LEAF;
			placedLeafCounter = placedLeafCounter + 1;
		}
	}

	/**
	 * Creates leaf objects for all leaf-marked cells and adds them to output.
	 *
	 * @param trunkColumnWorldX        Trunk world X coordinate.
	 * @param trunkTopWorldY           Trunk top world Y coordinate.
	 * @param topTreeGrid              The top-tree grid.
	 * @param leafBlocksOutput         Output list for leaf objects.
	 * @param topTreeHalfWidthInBlocks Grid half width (in cells).
	 * @param topTreeFullWidthInBlocks Grid full width (in cells).
	 * @param seed                     World seed used for deterministic leaves.
	 */
	private void createLeavesFromTopTreeGrid(int trunkColumnWorldX, int trunkTopWorldY,
											 TopTreeCellState[][] topTreeGrid,
											 List<GameObject> leafBlocksOutput,
											 int topTreeHalfWidthInBlocks,
											 int topTreeFullWidthInBlocks, int seed) {
		for (int rowIndex = 0; rowIndex < topTreeFullWidthInBlocks; rowIndex++) {
			for (int colIndex = 0; colIndex < topTreeFullWidthInBlocks; colIndex++) {
				if (topTreeGrid[rowIndex][colIndex] != TopTreeCellState.LEAF) {
					continue;
				}
				Vector2 leafTopLeftWorld = topTreeCellToWorldTopLeft(trunkColumnWorldX, trunkTopWorldY,
						rowIndex, colIndex, topTreeHalfWidthInBlocks);
				leafBlocksOutput.add(Leaf.create(leafTopLeftWorld, seed));
			}
		}
	}

	/**
	 * Generates a random target fruit count.
	 *
	 * @param randomGenerator Random source.
	 * @return Target fruit count.
	 */
	private int generateRandomFruitCount(Random randomGenerator) {
		int possibleCounts = MAX_FRUIT_COUNT - MIN_FRUIT_COUNT + SINGLE_STEP;
		return MIN_FRUIT_COUNT + randomGenerator.nextInt(possibleCounts);
	}

	/**
	 * Places fruits randomly into legal empty cells and adds created fruits to output.
	 *
	 * @param trunkColumnWorldX        Trunk world X coordinate.
	 * @param trunkTopWorldY           Trunk top world Y coordinate.
	 * @param topTreeGrid              The top-tree grid.
	 * @param targetFruitCount         Desired fruit count.
	 * @param randomGenerator          Random source.
	 * @param fruitBlocksOutput        Output list for fruit objects.
	 * @param cycleLength              Day cycle length for fruit respawn.
	 * @param topTreeHalfWidthInBlocks Grid half width (in cells).
	 * @param topTreeFullWidthInBlocks Grid full width (in cells).
	 */
	private void placeFruitsRandomly(int trunkColumnWorldX, int trunkTopWorldY,
									 TopTreeCellState[][] topTreeGrid, int targetFruitCount,
									 Random randomGenerator,
									 List<GameObject> fruitBlocksOutput,
									 float cycleLength,
									 int topTreeHalfWidthInBlocks,
									 int topTreeFullWidthInBlocks, int seed) {
		int legalEmptyCellsForFruit = countLegalFruitCells(topTreeGrid, topTreeFullWidthInBlocks);
		if (legalEmptyCellsForFruit <= MIN_NON_NEGATIVE) {
			return;
		}
		int wantedFruitCounter = Math.min(targetFruitCount, legalEmptyCellsForFruit);
		int placedFruitCounter = 0;
		int remainingAttempts = MAX_RANDOM_PLACEMENT_ATTEMPTS_MULTIPLIER *
				topTreeFullWidthInBlocks * topTreeFullWidthInBlocks;
		while (placedFruitCounter < wantedFruitCounter && remainingAttempts > MIN_NON_NEGATIVE) {
			remainingAttempts = remainingAttempts - 1;
			int rowIndex = randomGenerator.nextInt(topTreeFullWidthInBlocks);
			int colIndex = randomGenerator.nextInt(topTreeFullWidthInBlocks);
			if (topTreeGrid[rowIndex][colIndex] != TopTreeCellState.EMPTY) {
				continue;
			}
			if (!isLegalFruitCell(topTreeGrid, rowIndex, colIndex, topTreeFullWidthInBlocks)) {
				continue;
			}
			Vector2 fruitTopLeftWorld = topTreeCellToWorldTopLeft(trunkColumnWorldX, trunkTopWorldY,
					rowIndex, colIndex, topTreeHalfWidthInBlocks);
			topTreeGrid[rowIndex][colIndex] = TopTreeCellState.FRUIT;
			fruitBlocksOutput.add(new Fruit(fruitTopLeftWorld, cycleLength, seed));
			placedFruitCounter = placedFruitCounter + 1;
		}
	}

	/**
	 * Counts how many empty cells are valid for fruit placement.
	 *
	 * @param topTreeGrid              The top-tree grid.
	 * @param topTreeFullWidthInBlocks Grid width (in cells).
	 * @return The number of valid fruit cells.
	 */
	private int countLegalFruitCells(TopTreeCellState[][] topTreeGrid, int topTreeFullWidthInBlocks) {
		int legalCount = 0;
		for (int rowIndex = 0; rowIndex < topTreeFullWidthInBlocks; rowIndex++) {
			for (int colIndex = 0; colIndex < topTreeFullWidthInBlocks; colIndex++) {
				if (topTreeGrid[rowIndex][colIndex] != TopTreeCellState.EMPTY) {
					continue;
				}
				if (isLegalFruitCell(topTreeGrid, rowIndex, colIndex, topTreeFullWidthInBlocks)) {
					legalCount++;
				}
			}
		}

		return legalCount;
	}

	/**
	 * Checks whether a grid cell is valid for fruit placement.
	 *
	 * @param topTreeGrid              The top-tree grid.
	 * @param rowIndex                 Row index.
	 * @param colIndex                 Column index.
	 * @param topTreeFullWidthInBlocks Grid width (in cells).
	 * @return True if the cell is valid for fruit placement.
	 */
	private boolean isLegalFruitCell(TopTreeCellState[][] topTreeGrid, int rowIndex, int colIndex,
									 int topTreeFullWidthInBlocks) {
		int trunkColumnInGrid = topTreeFullWidthInBlocks / DIVIDE_BY_TWO_FACTOR;
		if (colIndex == trunkColumnInGrid) {
			return false;
		}
		boolean hasLeafBelow = isLeafAt(topTreeGrid, rowIndex + SINGLE_STEP,
				colIndex, topTreeFullWidthInBlocks);
		boolean hasLeafLeft = isLeafAt(topTreeGrid, rowIndex, colIndex - SINGLE_STEP,
				topTreeFullWidthInBlocks);
		boolean hasLeafRight = isLeafAt(topTreeGrid, rowIndex, colIndex + SINGLE_STEP,
				topTreeFullWidthInBlocks);
		boolean hasLeafAbove = isLeafAt(topTreeGrid, rowIndex - SINGLE_STEP, colIndex,
				topTreeFullWidthInBlocks);
		return hasLeafBelow || (hasLeafLeft && hasLeafRight) || (hasLeafAbove && hasLeafRight) ||
				(hasLeafAbove && hasLeafLeft);
	}

	/**
	 * Checks whether the given grid cell exists and contains a leaf.
	 *
	 * @param topTreeGrid              The top-tree grid.
	 * @param rowIndex                 Row index.
	 * @param colIndex                 Column index.
	 * @param topTreeFullWidthInBlocks Grid width (in cells).
	 * @return True if the cell exists and contains a leaf, otherwise false.
	 */
	private boolean isLeafAt(TopTreeCellState[][] topTreeGrid, int rowIndex, int colIndex,
							 int topTreeFullWidthInBlocks) {
		boolean rowOutOfBounds = rowIndex < MIN_NON_NEGATIVE || rowIndex >= topTreeFullWidthInBlocks;
		boolean colOutOfBounds = colIndex < MIN_NON_NEGATIVE || colIndex >= topTreeFullWidthInBlocks;
		if (rowOutOfBounds || colOutOfBounds) {
			return false;
		}
		return topTreeGrid[rowIndex][colIndex] == TopTreeCellState.LEAF;
	}

	/**
	 * Converts a top-tree grid cell to a world-space top-left coordinate.
	 *
	 * @param trunkColumnWorldX        Trunk world X coordinate.
	 * @param trunkTopWorldY           Trunk top world Y coordinate.
	 * @param rowIndex                 Row index.
	 * @param colIndex                 Column index.
	 * @param topTreeHalfWidthInBlocks Grid half width (in cells).
	 * @return The cell's top-left position in world coordinates.
	 */
	private Vector2 topTreeCellToWorldTopLeft(int trunkColumnWorldX, int trunkTopWorldY, int rowIndex,
											  int colIndex, int topTreeHalfWidthInBlocks) {
		int offsetXInBlocks = colIndex - topTreeHalfWidthInBlocks;
		int offsetYInBlocks = rowIndex - topTreeHalfWidthInBlocks;
		int cellWorldX = trunkColumnWorldX + offsetXInBlocks * Block.SIZE;
		int cellWorldY = trunkTopWorldY + offsetYInBlocks * Block.SIZE;
		return new Vector2(cellWorldX, cellWorldY);
	}

	/**
	 * Represents the state of a single cell in the top-tree grid.
	 */
	private enum TopTreeCellState {
		FRUIT,
		LEAF,
		EMPTY,
		ILLEGAL
	}
}
