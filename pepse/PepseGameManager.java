package pepse;

import danogl.GameManager;
import danogl.GameObject;
import danogl.components.CoordinateSpace;
import danogl.gui.ImageReader;
import danogl.gui.SoundReader;
import danogl.gui.UserInputListener;
import danogl.gui.WindowController;
import danogl.gui.rendering.Camera;
import danogl.util.Vector2;
import pepse.display.AvatarEnergyPercentDisplay;
import pepse.world.Block;
import pepse.world.Sky;
import pepse.world.Terrain;
import pepse.world.WorldConstants;
import pepse.world.avatar.Avatar;
import pepse.world.daynight.Night;
import pepse.world.daynight.Sun;
import pepse.world.daynight.SunHalo;
import pepse.world.trees.Flora;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main game manager responsible for creating the world and streaming terrain/flora by regions.
 * The game loads regions around the camera and unloads regions that are far away to limit active objects.
 *
 * @author Liad Koren
 */
public class PepseGameManager extends GameManager {
	private static final int REGION_WIDTH_IN_BLOCKS = 32;
	private static final int TERRAIN_KEEP_NUM_REGIONS_EACH_SIDE = 2;
	private static final int FLORA_KEEP_NUM_REGIONS_EACH_SIDE = 1;
	private static final int WORLD_SEED = 11;
	private static final Vector2 ENERGY_DISPLAY_PERCENT_POSITION = new Vector2(20, 20);

	private final Map<Integer, List<GameObject>> loadedTerrainRegions = new HashMap<>();
	private final Map<Integer, Map<GameObject, Integer>> loadedFloraRegions = new HashMap<>();

	private Terrain terrain;
	private Flora flora;
	private Avatar avatar;


	/**
	 * Runs the game.
	 *
	 * @param args Command line arguments.
	 */
	public static void main(String[] args) {
		new PepseGameManager().run();
	}

	/**
	 * Initializes the game world and creates the initial set of game objects.
	 *
	 * @param imageReader      Used to load images.
	 * @param soundReader      Used to load sounds.
	 * @param inputListener    Used to read user input.
	 * @param windowController Window controller for window operations.
	 */
	@Override
	public void initializeGame(ImageReader imageReader, SoundReader soundReader,
							   UserInputListener inputListener, WindowController windowController) {
		super.initializeGame(imageReader, soundReader, inputListener, windowController);
		windowController.setTargetFramerate(PepseGameConstants.DEFULT_FRAME_RATE);
		Vector2 windowDimensions = windowController.getWindowDimensions();

		//create sky
		GameObject sky = Sky.create(windowDimensions);
		gameObjects().addGameObject(sky, PepseGameConstants.SKY_LAYER);

		int seed = WORLD_SEED;
		//create terrain and flora
		terrain = new Terrain(windowDimensions, seed);
		flora = new Flora(PepseGameConstants.DEFAULT_AVATAR_SPAWN_X, terrain::groundHeightAt, seed,
				PepseGameConstants.BLOCK_LAYER, PepseGameConstants.LEAF_LAYER,
				PepseGameConstants.FRUIT_LAYER, PepseGameConstants.FULL_DAY_CYCLE_LENGTH);
		//collisions between layers
		gameObjects().layers().shouldLayersCollide(PepseGameConstants.AVATAR_LAYER,
				PepseGameConstants.BLOCK_LAYER, true);
		gameObjects().layers().shouldLayersCollide(PepseGameConstants.BLOCK_LAYER,
				PepseGameConstants.BLOCK_LAYER, false);
		gameObjects().layers().shouldLayersCollide(PepseGameConstants.AVATAR_LAYER,
				PepseGameConstants.FRUIT_LAYER, true);
		gameObjects().layers().shouldLayersCollide(PepseGameConstants.AVATAR_LAYER,
				PepseGameConstants.LEAF_LAYER, false);
		gameObjects().layers().shouldLayersCollide(PepseGameConstants.LEAF_LAYER,
				PepseGameConstants.LEAF_LAYER, false);
		gameObjects().layers().shouldLayersCollide(PepseGameConstants.BLOCK_LAYER,
				PepseGameConstants.LEAF_LAYER, false);
		gameObjects().layers().shouldLayersCollide(PepseGameConstants.FRUIT_LAYER,
				PepseGameConstants.BLOCK_LAYER, false);
		gameObjects().layers().shouldLayersCollide(PepseGameConstants.FRUIT_LAYER,
				PepseGameConstants.LEAF_LAYER, false);
		gameObjects().layers().shouldLayersCollide(PepseGameConstants.FRUIT_LAYER,
				PepseGameConstants.FRUIT_LAYER, false);

		//create night
		GameObject night = Night.create(windowDimensions, PepseGameConstants.FULL_DAY_CYCLE_LENGTH);
		night.setCoordinateSpace(CoordinateSpace.CAMERA_COORDINATES);
		gameObjects().addGameObject(night, PepseGameConstants.NIGHT_LAYER);
		//create sun
		GameObject sun = Sun.create(windowDimensions, PepseGameConstants.FULL_DAY_CYCLE_LENGTH);
		gameObjects().addGameObject(sun, PepseGameConstants.SUN_LAYER);
		gameObjects().addGameObject(SunHalo.create(sun), PepseGameConstants.SUN_HALO_LAYER);

		float groundY = terrain.groundHeightAt(PepseGameConstants.DEFAULT_AVATAR_SPAWN_X);
		avatar = new Avatar(new Vector2(PepseGameConstants.DEFAULT_AVATAR_SPAWN_X,
				groundY - Avatar.AVATAR_SIZE),
				inputListener, imageReader);
		gameObjects().addGameObject(avatar, PepseGameConstants.AVATAR_LAYER);
		setCamera(new Camera(avatar, Vector2.ZERO, windowDimensions, windowDimensions));
		//create avatar energy display
		AvatarEnergyPercentDisplay energyDisplay =
				AvatarEnergyPercentDisplay.create(ENERGY_DISPLAY_PERCENT_POSITION);
		energyDisplay.setCoordinateSpace(CoordinateSpace.CAMERA_COORDINATES);
		gameObjects().addGameObject(energyDisplay, PepseGameConstants.AVATAR_ENERGY_PERCENT_DISPLAY_LAYER);
		avatar.setEnergyChangedCallback(energyDisplay::setEnergy);

		updateWorldStreaming();
	}

	/**
	 * Updates the game and performs world streaming.
	 *
	 * @param deltaTime Time elapsed since last update.
	 */
	@Override
	public void update(float deltaTime) {
		super.update(deltaTime);
		updateWorldStreaming();
	}

	/**
	 * Loads and unloads terrain and flora regions around the current camera position.
	 */
	private void updateWorldStreaming() {
		float cameraCenterX = camera().getCenter().x();
		int currentRegionIndex = worldXToRegionIndex(cameraCenterX);
		loadTerrainAround(currentRegionIndex);
		unloadTerrainOutside(currentRegionIndex);
		loadFloraAround(currentRegionIndex);
		unloadFloraOutside(currentRegionIndex);
	}

	/**
	 * Loads missing terrain regions around the given region index.
	 *
	 * @param currentRegionIndex The current region index.
	 */
	private void loadTerrainAround(int currentRegionIndex) {
		for (int regionIndex = currentRegionIndex - TERRAIN_KEEP_NUM_REGIONS_EACH_SIDE;
			 regionIndex <= currentRegionIndex + TERRAIN_KEEP_NUM_REGIONS_EACH_SIDE; regionIndex++) {
			if (loadedTerrainRegions.containsKey(regionIndex)) {
				continue;
			}

			List<GameObject> terrainObjectsInRegion = new ArrayList<>();
			for (Block currentBlock : terrain.createInRange(regionStartX(regionIndex),
					regionEndXInclusive(regionIndex))) {
				gameObjects().addGameObject(currentBlock, PepseGameConstants.BLOCK_LAYER);
				terrainObjectsInRegion.add(currentBlock);
			}
			loadedTerrainRegions.put(regionIndex, terrainObjectsInRegion);
		}
	}

	/**
	 * Unloads terrain regions outside the keep-range around the given region index.
	 *
	 * @param currentRegionIndex The current region index.
	 */
	private void unloadTerrainOutside(int currentRegionIndex) {
		int minKeep = currentRegionIndex - TERRAIN_KEEP_NUM_REGIONS_EACH_SIDE;
		int maxKeep = currentRegionIndex + TERRAIN_KEEP_NUM_REGIONS_EACH_SIDE;
		List<Integer> regionsToRemove = new ArrayList<>();
		for (Integer regionIndex : loadedTerrainRegions.keySet()) {
			if (regionIndex < minKeep || regionIndex > maxKeep) {
				regionsToRemove.add(regionIndex);
			}
		}
		for (Integer regionIndex : regionsToRemove) {
			List<GameObject> terrainObjects = loadedTerrainRegions.get(regionIndex);
			for (GameObject currentTerrainObject : terrainObjects) {
				gameObjects().removeGameObject(currentTerrainObject, PepseGameConstants.BLOCK_LAYER);
			}
			loadedTerrainRegions.remove(regionIndex);
		}
	}

	/**
	 * Loads missing flora regions around the given region index.
	 *
	 * @param currentRegionIndex The current region index.
	 */
	private void loadFloraAround(int currentRegionIndex) {
		for (int regionIndex = currentRegionIndex - FLORA_KEEP_NUM_REGIONS_EACH_SIDE;
			 regionIndex <= currentRegionIndex + FLORA_KEEP_NUM_REGIONS_EACH_SIDE; regionIndex++) {
			if (loadedFloraRegions.containsKey(regionIndex)) {
				continue;
			}
			Map<Integer, List<GameObject>> floraObjectsByLayer =
					flora.createInRange(regionStartX(regionIndex), regionEndXInclusive(regionIndex));
			Map<GameObject, Integer> regionData = new HashMap<>();
			for (var entry : floraObjectsByLayer.entrySet()) {
				int layer = entry.getKey();
				for (GameObject currentFloraObject : entry.getValue()) {
					gameObjects().addGameObject(currentFloraObject, layer);
					regionData.put(currentFloraObject, layer);
				}
			}
			loadedFloraRegions.put(regionIndex, regionData);
		}
	}

	/**
	 * Unloads flora regions outside the keep-range around the given region index.
	 *
	 * @param currentRegionIndex The current region index.
	 */
	private void unloadFloraOutside(int currentRegionIndex) {
		int minKeep = currentRegionIndex - FLORA_KEEP_NUM_REGIONS_EACH_SIDE;
		int maxKeep = currentRegionIndex + FLORA_KEEP_NUM_REGIONS_EACH_SIDE;
		List<Integer> regionsToRemove = new ArrayList<>();
		for (Integer regionIndex : loadedFloraRegions.keySet()) {
			if (regionIndex < minKeep || regionIndex > maxKeep) {
				regionsToRemove.add(regionIndex);
			}
		}

		for (Integer regionIndex : regionsToRemove) {
			Map<GameObject, Integer> regionData = loadedFloraRegions.get(regionIndex);
			for (var objEntry : regionData.entrySet()) {
				gameObjects().removeGameObject(objEntry.getKey(), objEntry.getValue());
			}
			loadedFloraRegions.remove(regionIndex);
		}
	}

	/**
	 * Converts a world X coordinate to a region index.
	 *
	 * @param worldX World X coordinate.
	 * @return Region index containing the given X coordinate.
	 */
	private int worldXToRegionIndex(float worldX) {
		int xAligned = WorldConstants.closestSmallerToMultipliesOfBlockSize((int) worldX);
		return Math.floorDiv(xAligned / Block.SIZE, REGION_WIDTH_IN_BLOCKS);
	}

	/**
	 * Returns the world X coordinate of the start of the region.
	 *
	 * @param regionIndex Region index.
	 * @return Region start X coordinate.
	 */
	private int regionStartX(int regionIndex) {
		return regionIndex * REGION_WIDTH_IN_BLOCKS * Block.SIZE;
	}

	/**
	 * Returns the world X coordinate of the end of the region (inclusive).
	 *
	 * @param regionIndex Region index.
	 * @return Region end X coordinate (inclusive).
	 */
	private int regionEndXInclusive(int regionIndex) {
		return ((regionIndex + 1) * REGION_WIDTH_IN_BLOCKS - 1) * Block.SIZE;
	}
}
