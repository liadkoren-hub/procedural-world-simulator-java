package pepse.world;

import danogl.GameObject;
import danogl.components.CoordinateSpace;
import danogl.gui.rendering.RectangleRenderable;
import danogl.util.Vector2;

import java.awt.*;

/**
 * Creates a static sky background that follows the camera.
 *
 * @author Liad Koren
 */
public class Sky {
	private static final Color BASIC_SKY_COLOR = Color.decode("#80C6E5");

	private Sky() {

	}

	/**
	 * Creates a sky background that covers the entire window.
	 *
	 * @param windowDimensions The window dimensions.
	 * @return A game object representing the sky.
	 */
	public static GameObject create(Vector2 windowDimensions) {
		GameObject sky = new GameObject(Vector2.ZERO, windowDimensions,
				new RectangleRenderable(BASIC_SKY_COLOR));
		sky.setCoordinateSpace(CoordinateSpace.CAMERA_COORDINATES);
		sky.setTag(WorldTags.SKY_TAG);
		return sky;
	}
}
