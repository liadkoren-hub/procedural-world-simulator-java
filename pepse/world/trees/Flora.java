package pepse.world.trees;

import danogl.GameObject;
import pepse.world.Block;
import pepse.world.WorldConstants;

import java.util.*;
import java.util.function.Function;

/**
 * This class is a the flora planner in terms of constraints. randomised flora
 * creates trees in random regions, each region has or doesnt have a tree decided by random
 * situation. trees a planned to not be too close for good game experience.
 * Generates trees (trunks, leaves, and fruits) in a requested X-range.
 * Tree placement is deterministic based on a seed and region indices.
 * Makes sure that a tree will not be spawned in the avatar spawn position to ensure good game experience
 *
 * @author Liad Koren
 */
public class Flora {
	private static final float TREE_SPAWN_PROBABILITY_PER_REGION = 0.7f;
	private static final int TREE_PLANNING_REGION_WIDTH_IN_BLOCKS = 8;
	private static final int MAX_TREES_PER_REGION = 1;
	private static final int MIN_TRUNK_SPACING_IN_BLOCKS = 2;
	private static final int MAX_TRUNK_PLACEMENT_ATTEMPTS = 12;
	private static final int MIN_TOP_TREE_HALF_WIDTH_IN_BLOCKS = 2;
	private static final int MAX_TOP_TREE_HALF_WIDTH_IN_BLOCKS = 5;
	private static final int TOP_TREE_NEIGHBOR_MARGIN_IN_BLOCKS = 1;
	private static final int NEIGHBOR_PLANNING_SAFETY_BUFFER_IN_BLOCKS = 1;
	private static final int NO_COLUMN_FOUND = -1;
	private static final int NO_NEIGHBOR_COLUMN = -1;
	private static final int INCLUSIVE_LENGTH_OFFSET = 1;
	private static final int LAST_INDEX_OFFSET = -1;
	private static final int PREVIOUS_INDEX_OFFSET = -1;
	private static final int NEXT_INDEX_OFFSET = 1;
	private static final int DIVIDE_BY_TWO_FACTOR = 2;
	private static final int NO_SPACE_FOR_TOP_TREE_IN_CURRENT_DIRECTION = 0;
	private static final int GAP_BETWEEN_TRUNKS_FOR_TOP_TREE = 1;

	private final Function<Float, Float> groundHeightFunction;
	private final int seed;
	private final int trunkObjectsLayer;
	private final int leafObjectsLayer;
	private final int fruitObjectsLayer;
	private final int avatarStartTopLeftX;
	private final float cycleLength;

	/**
	 * Constructs a flora generator that can create trees based on ground height and seed.
	 *
	 * @param avatarStartTopLeftX  The avatar spawn X used to avoid placing a trunk at spawn.
	 * @param groundHeightFunction A function that returns ground height for a given X coordinate.
	 * @param seed                 The world seed.
	 * @param trunkObjectsLayer    The layer to assign trunk blocks.
	 * @param leafObjectsLayer     The layer to assign leaf objects.
	 * @param fruitObjectsLayer    The layer to assign fruit objects.
	 * @param cycleLength          The day cycle length used for fruit respawn timing.
	 */
	public Flora(int avatarStartTopLeftX,
				 Function<Float, Float> groundHeightFunction,
				 int seed,
				 int trunkObjectsLayer,
				 int leafObjectsLayer,
				 int fruitObjectsLayer,
				 float cycleLength) {
		this.groundHeightFunction = groundHeightFunction;
		this.seed = seed;
		this.trunkObjectsLayer = trunkObjectsLayer;
		this.leafObjectsLayer = leafObjectsLayer;
		this.fruitObjectsLayer = fruitObjectsLayer;
		this.avatarStartTopLeftX = avatarStartTopLeftX;
		this.cycleLength = cycleLength;
	}

	/**
	 * Creates trunk, leaf, and fruit objects in the given X-range.
	 *
	 * @param minX The minimum X coordinate.
	 * @param maxX The maximum X coordinate.
	 * @return A mapping from layer index to the list of created objects for that layer.
	 */
	public Map<Integer, List<GameObject>> createInRange(int minX, int maxX) {
		Map<Integer, List<GameObject>> objectsGroupedByLayer = new HashMap<>();
		objectsGroupedByLayer.put(trunkObjectsLayer, new ArrayList<>());
		objectsGroupedByLayer.put(leafObjectsLayer, new ArrayList<>());
		objectsGroupedByLayer.put(fruitObjectsLayer, new ArrayList<>());
		List<GameObject> trunkObjects = objectsGroupedByLayer.get(trunkObjectsLayer);
		List<GameObject> leafObjects = objectsGroupedByLayer.get(leafObjectsLayer);
		List<GameObject> fruitObjects = objectsGroupedByLayer.get(fruitObjectsLayer);
		int minXAligned = WorldConstants.closestSmallerToMultipliesOfBlockSize(minX);
		int maxXAligned = WorldConstants.closestBiggerToMultipliesOfBlockSize(maxX);
		int minColumnInRange = minXAligned / Block.SIZE;
		int maxColumnInRange = maxXAligned / Block.SIZE;
		int firstRegionIndexInRange = Math.floorDiv(minColumnInRange, TREE_PLANNING_REGION_WIDTH_IN_BLOCKS);
		int lastRegionIndexInRange = Math.floorDiv(maxColumnInRange, TREE_PLANNING_REGION_WIDTH_IN_BLOCKS);
		int planningBufferInBlocks = MAX_TOP_TREE_HALF_WIDTH_IN_BLOCKS
				+ TOP_TREE_NEIGHBOR_MARGIN_IN_BLOCKS
				+ NEIGHBOR_PLANNING_SAFETY_BUFFER_IN_BLOCKS;
		int planningBufferRegions = ceilDivPositiveByPlanningRegionWidth(planningBufferInBlocks);
		int firstRegionIndexToPlan = firstRegionIndexInRange - planningBufferRegions;
		int lastRegionIndexToPlan = lastRegionIndexInRange + planningBufferRegions;
		int avatarSpawnColumn = avatarStartTopLeftX / Block.SIZE;
		List<Integer> plannedTrunkColumns = new ArrayList<>();
		for (int regionIndex = firstRegionIndexToPlan; regionIndex <= lastRegionIndexToPlan;
			 regionIndex++) {
			int regionStartColumn = regionIndex * TREE_PLANNING_REGION_WIDTH_IN_BLOCKS;
			int regionEndColumn = regionStartColumn +
					TREE_PLANNING_REGION_WIDTH_IN_BLOCKS + LAST_INDEX_OFFSET;
			Random regionRandomGenerator = new Random(makeFloraSeed(seed, regionIndex));
			if (regionRandomGenerator.nextFloat() >= TREE_SPAWN_PROBABILITY_PER_REGION) {
				continue;
			}
			for (int treeIndexInRegion = 0; treeIndexInRegion < MAX_TREES_PER_REGION; treeIndexInRegion++) {
				int chosenTrunkColumn = chooseLegalTrunkColumnInsideRegion(regionRandomGenerator,
						regionStartColumn, regionEndColumn, plannedTrunkColumns, avatarSpawnColumn);
				if (chosenTrunkColumn == NO_COLUMN_FOUND) {
					continue;
				}
				plannedTrunkColumns.add(chosenTrunkColumn);
			}
		}
		if (plannedTrunkColumns.isEmpty()) {
			return objectsGroupedByLayer;
		}
		Collections.sort(plannedTrunkColumns);
		for (int trunkIndexInPlan = 0; trunkIndexInPlan < plannedTrunkColumns.size(); trunkIndexInPlan++) {
			int trunkColumn = plannedTrunkColumns.get(trunkIndexInPlan);
			if (trunkColumn < minColumnInRange || trunkColumn > maxColumnInRange) {
				continue;
			}
			int leftNeighborTrunkColumn = NO_NEIGHBOR_COLUMN;
			int rightNeighborTrunkColumn = NO_NEIGHBOR_COLUMN;
			if (trunkIndexInPlan > 0) {
				leftNeighborTrunkColumn = plannedTrunkColumns.get(trunkIndexInPlan + PREVIOUS_INDEX_OFFSET);
			}
			if (trunkIndexInPlan + NEXT_INDEX_OFFSET < plannedTrunkColumns.size()) {
				rightNeighborTrunkColumn = plannedTrunkColumns.get(trunkIndexInPlan + NEXT_INDEX_OFFSET);
			}
			int allowedTopTreeHalfWidthLeftInBlocks =
					computeAllowedTopTreeHalfWidthInBlocks(leftNeighborTrunkColumn, trunkColumn);
			int allowedTopTreeHalfWidthRightInBlocks =
					computeAllowedTopTreeHalfWidthInBlocks(trunkColumn, rightNeighborTrunkColumn);
			int trunkXPosition = trunkColumn * Block.SIZE;
			Random treeRandomGenerator = new Random(makeFloraSeed(seed, trunkColumn));
			Tree.create(trunkXPosition, treeRandomGenerator, seed, trunkObjects, leafObjects, fruitObjects,
					cycleLength, MIN_TOP_TREE_HALF_WIDTH_IN_BLOCKS, MAX_TOP_TREE_HALF_WIDTH_IN_BLOCKS,
					allowedTopTreeHalfWidthLeftInBlocks, allowedTopTreeHalfWidthRightInBlocks,
					groundHeightFunction);
		}
		return objectsGroupedByLayer;
	}

	/**
	 * Chooses a trunk column inside a region while respecting spacing constraints.
	 *
	 * @param random              A region-based random generator.
	 * @param regionStartColumn   Inclusive region start column.
	 * @param regionEndColumn     Inclusive region end column.
	 * @param plannedTrunkColumns Already planned trunk columns.
	 * @param avatarSpawnColumn   Column to avoid placing a trunk at.
	 * @return A valid trunk column, or {@link #NO_COLUMN_FOUND} if not found.
	 */
	private int chooseLegalTrunkColumnInsideRegion(Random random, int regionStartColumn, int regionEndColumn,
												   List<Integer> plannedTrunkColumns,
												   int avatarSpawnColumn) {
		int regionWidthInColumns = regionEndColumn - regionStartColumn + INCLUSIVE_LENGTH_OFFSET;
		if (regionWidthInColumns <= 0) {
			return NO_COLUMN_FOUND;
		}
		for (int currentPlacementAttempt = 0; currentPlacementAttempt <
				MAX_TRUNK_PLACEMENT_ATTEMPTS; currentPlacementAttempt++) {
			int candidateTrunkColumn = regionStartColumn + random.nextInt(regionWidthInColumns);
			if (candidateTrunkColumn == avatarSpawnColumn) {
				continue;
			}
			boolean respectsMinSpacing = true;
			for (int existingTrunkColumn : plannedTrunkColumns) {
				if (Math.abs(candidateTrunkColumn - existingTrunkColumn) < MIN_TRUNK_SPACING_IN_BLOCKS) {
					respectsMinSpacing = false;
					break;
				}
			}
			if (!respectsMinSpacing) {
				continue;
			}
			return candidateTrunkColumn;
		}
		return NO_COLUMN_FOUND;
	}

	/**
	 * Computes the maximum allowed top-tree half width given two neighboring trunks.
	 *
	 * @param leftTrunkColumn  Left trunk column, or {@link #NO_NEIGHBOR_COLUMN}.
	 * @param rightTrunkColumn Right trunk column, or {@link #NO_NEIGHBOR_COLUMN}.
	 * @return The allowed half width (in blocks) in the direction between the trunks.
	 */
	private int computeAllowedTopTreeHalfWidthInBlocks(int leftTrunkColumn, int rightTrunkColumn) {
		if (leftTrunkColumn == NO_NEIGHBOR_COLUMN || rightTrunkColumn == NO_NEIGHBOR_COLUMN) {
			return MAX_TOP_TREE_HALF_WIDTH_IN_BLOCKS;
		}

		int distanceBetweenTrunksInBlocks = Math.abs(rightTrunkColumn - leftTrunkColumn);
		int maxHalfWidthWithoutOverlap = (distanceBetweenTrunksInBlocks - GAP_BETWEEN_TRUNKS_FOR_TOP_TREE)
				/ DIVIDE_BY_TWO_FACTOR;
		int allowedHalfWidth = maxHalfWidthWithoutOverlap - TOP_TREE_NEIGHBOR_MARGIN_IN_BLOCKS;

		if (allowedHalfWidth < NO_SPACE_FOR_TOP_TREE_IN_CURRENT_DIRECTION) {
			return NO_SPACE_FOR_TOP_TREE_IN_CURRENT_DIRECTION;
		}
		if (allowedHalfWidth > MAX_TOP_TREE_HALF_WIDTH_IN_BLOCKS) {
			return MAX_TOP_TREE_HALF_WIDTH_IN_BLOCKS;
		}
		return allowedHalfWidth;
	}

	/**
	 * Computes a positive ceil-division by the fixed planning region width.
	 *
	 * @param numerator The numerator value.
	 * @return Ceil(numerator / TREE_PLANNING_REGION_WIDTH_IN_BLOCKS), or 0 for non-positive numerator.
	 */
	private int ceilDivPositiveByPlanningRegionWidth(int numerator) {
		if (numerator <= 0) {
			return 0;
		}
		return (numerator + TREE_PLANNING_REGION_WIDTH_IN_BLOCKS - 1) / TREE_PLANNING_REGION_WIDTH_IN_BLOCKS;
	}

	/**
	 * Creates a deterministic seed for region/column-based random generation.
	 *
	 * @param baseSeed          The world seed.
	 * @param regionOrColumnKey The region index or trunk column.
	 * @return A derived seed value.
	 */
	private int makeFloraSeed(int baseSeed, int regionOrColumnKey) {
		return Objects.hash(baseSeed, regionOrColumnKey);
	}
}
