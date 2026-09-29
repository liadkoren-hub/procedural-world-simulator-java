package pepse.world.daynight;

import danogl.GameObject;
import danogl.components.CoordinateSpace;
import danogl.gui.rendering.OvalRenderable;
import danogl.util.Vector2;
import pepse.world.WorldTags;

import java.awt.*;

/**
 * Creates a halo that follows the sun position and wraps the sun as a circle and
 * moving along with the sun, in fact following the sun to always wrap it as an outside ring
 *
 * @author Liad Koren
 */
public class SunHalo {
	private static final Color SUN_HALO_COLOR = new Color(255, 255, 0, 20);
	private static final int SUN_HALO_SIZE_FACTOR_MULT_SUN = 2;

	private SunHalo() {

	}

	/**
	 * Creates a halo object that updates its center to match the sun each frame.
	 *
	 * @param sun The sun object to follow.
	 * @return A game object representing the sun halo.
	 */
	public static GameObject create(GameObject sun) {
		Vector2 sunHaloSize = sun.getDimensions().mult(SUN_HALO_SIZE_FACTOR_MULT_SUN);
		GameObject sunHalo = new GameObject(Vector2.ZERO, sunHaloSize, new OvalRenderable(SUN_HALO_COLOR));
		sunHalo.setCenter(sun.getCenter());
		sunHalo.setCoordinateSpace(CoordinateSpace.CAMERA_COORDINATES);
		sunHalo.setTag(WorldTags.SUN_HALO_TAG);
		sunHalo.addComponent((deltaTime -> sunHalo.setCenter(sun.getCenter())));
		return sunHalo;
	}
}
