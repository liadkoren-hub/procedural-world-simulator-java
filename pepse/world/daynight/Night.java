package pepse.world.daynight;

import danogl.GameObject;
import danogl.components.CoordinateSpace;
import danogl.components.Transition;
import danogl.gui.rendering.RectangleRenderable;
import danogl.util.Vector2;
import pepse.world.WorldTags;

import java.awt.*;

/**
 * Creates a full screen overlay that simulates night by changing opacity over time.
 *
 * @author Liad Koren
 */
public class Night {
	private static final Float MIDNIGHT_OPACITY = 0.5f;
	private static final Float MIDDAY_OPACITY = 0.0f;
	private static final float DIVIDING_THE_DAY_FACTOR = 2f;

	private Night() {

	}

	/**
	 * Creates the night overlay and attaches a back-and-forth opacity transition.
	 *
	 * @param windowDimensions The window dimensions.
	 * @param cycleLength      The length  of the full day cycle.
	 * @return A game object representing the night overlay.
	 */
	public static GameObject create(Vector2 windowDimensions, float cycleLength) {
		GameObject night = new GameObject(Vector2.ZERO, windowDimensions,
				new RectangleRenderable(Color.BLACK));
		night.setCoordinateSpace(CoordinateSpace.CAMERA_COORDINATES);
		night.setTag(WorldTags.NIGHT_TAG);
		new Transition<Float>(
				night,
				night.renderer()::setOpaqueness,
				MIDDAY_OPACITY,
				MIDNIGHT_OPACITY,
				Transition.CUBIC_INTERPOLATOR_FLOAT,
				cycleLength / DIVIDING_THE_DAY_FACTOR,
				Transition.TransitionType.TRANSITION_BACK_AND_FORTH,
				null
		);
		return night;
	}
}
