package pepse.world.daynight;

import danogl.GameObject;
import danogl.components.CoordinateSpace;
import danogl.components.Transition;
import danogl.gui.rendering.OvalRenderable;
import danogl.util.Vector2;
import pepse.world.WorldConstants;
import pepse.world.WorldTags;

import java.awt.*;

/**
 * Creates a sun object that moves along a circular path during the day cycle.
 *
 * @author Liad Koren
 */
public class Sun {
	private static final int SIZE = 60;
	private static final float MIDDLE_OF_THE_X_AXIS_FACTOR = 2f;
	private static final Color SUN_COLOR = Color.YELLOW;
	private static final Float INITIAL_SUN_DEGREE_IN_CYCLE = 0f;
	private static final Float FINAL_SUN_DEGREE_IN_CYCLE = 360f;

	private Sun() {

	}

	/**
	 * Creates the sun and attaches a looping transition that updates its center along an orbit.
	 *
	 * @param windowDimensions The window dimensions.
	 * @param cycleLength      The length of the full day cycle.
	 * @return A game object representing the sun.
	 */
	public static GameObject create(Vector2 windowDimensions, float cycleLength) {
		float groundHeightAtX0 = WorldConstants.groundHeightAtX0(windowDimensions);
		float middleOfTheSky = groundHeightAtX0 / MIDDLE_OF_THE_X_AXIS_FACTOR;
		float middleOfTheScreenX = windowDimensions.x() / MIDDLE_OF_THE_X_AXIS_FACTOR;
		Vector2 sunInitialPosition = new Vector2(middleOfTheScreenX, middleOfTheSky);
		Vector2 sunSize = Vector2.ONES.mult(SIZE);
		GameObject sun = new GameObject(Vector2.ZERO, sunSize, new OvalRenderable(SUN_COLOR));
		sun.setCenter(sunInitialPosition);
		sun.setCoordinateSpace(CoordinateSpace.CAMERA_COORDINATES);
		sun.setTag(WorldTags.SUN_TAG);
		Vector2 initialSunCenter = sun.getCenter();
		Vector2 cycleCenter = new Vector2(middleOfTheScreenX, groundHeightAtX0);
		new Transition<Float>(
				sun,
				(Float angle) -> sun.setCenter
						(initialSunCenter.subtract(cycleCenter).rotated(angle).add(cycleCenter)),
				INITIAL_SUN_DEGREE_IN_CYCLE,
				FINAL_SUN_DEGREE_IN_CYCLE,
				Transition.LINEAR_INTERPOLATOR_FLOAT,
				cycleLength,
				Transition.TransitionType.TRANSITION_LOOP,
				null
		);
		return sun;
	}
}
