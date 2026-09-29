package pepse;

import danogl.collisions.Layer;

/**
 * Holds global game constants such as layers, cycle length, and default spawn values.
 *
 * @author Liad Koren
 */
public final class PepseGameConstants {
	/**
	 * Defult game frame rate.
	 */
	public static final int DEFULT_FRAME_RATE = 60;
	/**
	 * Full day cycle.
	 */
	public static final float FULL_DAY_CYCLE_LENGTH = 30f;
	/**
	 * Layer index for the sky background.
	 */
	public static final int SKY_LAYER = Layer.BACKGROUND;
	/**
	 * Layer index for terrain blocks and trunks
	 */
	public static final int BLOCK_LAYER = Layer.STATIC_OBJECTS;
	/**
	 * Layer index for the night overlay, darkening effect
	 */
	public static final int NIGHT_LAYER = Layer.FOREGROUND;
	/**
	 * Layer index for the sun halo object
	 */
	public static final int SUN_HALO_LAYER = SKY_LAYER + 1;
	/**
	 * Layer index for the sun object.
	 */
	public static final int SUN_LAYER = SUN_HALO_LAYER + 1;
	/**
	 * Layer index for UI energy percent display.
	 */
	public static final int AVATAR_ENERGY_PERCENT_DISPLAY_LAYER = Layer.UI;
	/**
	 * Layer index for the avatar.
	 */
	public static final int AVATAR_LAYER = Layer.DEFAULT + 1;
	/**
	 * Layer index for fruits
	 */
	public static final int FRUIT_LAYER = AVATAR_LAYER - 1;
	/**
	 * Layer index for leaves
	 */
	public static final int LEAF_LAYER = FRUIT_LAYER - 1;
	/**
	 * Default avatar spawn X coordinate in world space
	 */
	public static final int DEFAULT_AVATAR_SPAWN_X = 0;

	private PepseGameConstants() {

	}
}
