package pepse.world.trees;

import danogl.GameObject;
import danogl.components.ScheduledTask;
import danogl.components.Transition;
import danogl.gui.rendering.RectangleRenderable;
import danogl.util.Vector2;
import pepse.world.Block;
import pepse.world.WorldTags;

import java.awt.*;
import java.util.Objects;
import java.util.Random;

/**
 * Creates leaf objects and attaches wind animation using deterministic randomness.
 * Each leaf rotates and scales back and forth with parameters derived from its position and a base seed.
 *
 * @author Liad Koren
 */
public class Leaf {
	private static final Color BASE_LEAF_COLOR = new Color(50, 200, 30);
	private static final float MAX_LEAF_MOVEMENT_DELAY = 2f;
	private static final float MIN_LEAF_WIND_MOVING_ANGLE = 8f;
	private static final float MAX_LEAF_WIND_MOVING_ANGLE = 16f;
	private static final float MIN_LEAF_WIND_CYCLE = 1.8f;
	private static final float MAX_LEAF_WIND_CYCLE = 2.8f;
	private static final float MIN_LEAF_SHRINK_FACTOR = 0.92f;
	private static final float MAX_LEAF_GROW_FACTOR = 1.08f;
	private static final float LEAF_DEFAULT_SCALE = 1f;
	private static final float LEAF_SIZE = Block.SIZE;

	private Leaf() {
	}

	/**
	 * Creates a leaf at the given position with deterministic wind animation.
	 *
	 * @param topLeftCorner The leaf's top-left corner.
	 * @param seed          Base seed used to derive leaf specific randomness.
	 * @return A new leaf game object.
	 */
	public static GameObject create(Vector2 topLeftCorner, int seed) {
		int leafSeed = Objects.hash(seed, (int) topLeftCorner.x(), (int) topLeftCorner.y());
		Random leafRandom = new Random(leafSeed);
		GameObject leaf = new GameObject(
				topLeftCorner,
				Vector2.ONES.mult(LEAF_SIZE),
				new RectangleRenderable(pepse.utils.ColorSupplier.
						approximateColor(BASE_LEAF_COLOR, leafRandom)));
		leaf.setTag(WorldTags.LEAF_TAG);
		handleWindMovement(leaf, leafRandom);
		return leaf;
	}

	/**
	 * Putting wind transitions rotation and scaling to the given leaf.
	 *
	 * @param leaf       The leaf object to animate.
	 * @param leafRandom Random source used to choose animation parameters.
	 */
	private static void handleWindMovement(GameObject leaf, Random leafRandom) {
		float animationDelaySec = leafRandom.nextFloat() * MAX_LEAF_MOVEMENT_DELAY;
		float windMovingAngle =
				MIN_LEAF_WIND_MOVING_ANGLE + leafRandom.nextFloat() *
						(MAX_LEAF_WIND_MOVING_ANGLE - MIN_LEAF_WIND_MOVING_ANGLE);
		float windCycleDuration = MIN_LEAF_WIND_CYCLE +
				leafRandom.nextFloat() * (MAX_LEAF_WIND_CYCLE - MIN_LEAF_WIND_CYCLE);
		Vector2 leafBaseSize = leaf.getDimensions();
		float leafShrinkScale = MIN_LEAF_SHRINK_FACTOR +
				leafRandom.nextFloat() * (LEAF_DEFAULT_SCALE - MIN_LEAF_SHRINK_FACTOR);
		float leafGrowScale = LEAF_DEFAULT_SCALE + leafRandom.nextFloat() * (MAX_LEAF_GROW_FACTOR
				- LEAF_DEFAULT_SCALE);
		Vector2 leafMinSize = leafBaseSize.mult(leafShrinkScale);
		Vector2 leafMaxSize = leafBaseSize.mult(leafGrowScale);
		new ScheduledTask(leaf, animationDelaySec, false, () -> {
			new Transition<>(
					leaf,
					leaf.renderer()::setRenderableAngle,
					-windMovingAngle,
					windMovingAngle,
					Transition.CUBIC_INTERPOLATOR_FLOAT,
					windCycleDuration,
					Transition.TransitionType.TRANSITION_BACK_AND_FORTH,
					null
			);
			new Transition<>(
					leaf,
					leaf::setDimensions,
					leafMinSize,
					leafMaxSize,
					Transition.CUBIC_INTERPOLATOR_VECTOR,
					windCycleDuration,
					Transition.TransitionType.TRANSITION_BACK_AND_FORTH,
					null
			);
		});
	}
}
