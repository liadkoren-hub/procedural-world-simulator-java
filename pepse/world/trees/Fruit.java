package pepse.world.trees;

import danogl.GameObject;
import danogl.collisions.Collision;
import danogl.components.ScheduledTask;
import danogl.gui.rendering.OvalRenderable;
import danogl.util.Vector2;
import pepse.utils.ColorSupplier;
import pepse.world.Block;
import pepse.world.WorldTags;

import java.awt.*;
import java.util.Objects;
import java.util.Random;

/**
 * A collectible fruit that becomes temporarily unavailable after being collected.
 * When collected by the avatar, the fruit is hidden and reappears after a full cycle length.
 *
 * @author Liad Koren
 */
public class Fruit extends GameObject {
	private static final Color BASE_FRUIT_COLOR = new Color(193, 92, 15);
	private static final float FRUIT_SIZE_FACTOR = 0.8f;
	private static final float FRUIT_VISIBLE_OPACITY = 1f;
	private static final float FRUIT_INVISIBLE_OPACITY = 0f;
	private static final float FRUIT_SIZE = Block.SIZE * FRUIT_SIZE_FACTOR;

	private final float cycleLength;
	private boolean isFruitExist = true;

	/**
	 * Creates a fruit at the given position.
	 *
	 * @param topLeftCorner The fruit's top-left corner.
	 * @param cycleLength   The respawn delay (in seconds).
	 */
	public Fruit(Vector2 topLeftCorner, float cycleLength, int seed) {
		super(topLeftCorner, Vector2.ONES.mult(FRUIT_SIZE),
				new OvalRenderable(ColorSupplier.approximateColor(BASE_FRUIT_COLOR,
						new Random(Objects.hash(seed, (int) topLeftCorner.x(),
								(int) topLeftCorner.y(), WorldTags.FRUIT_TAG)))));
		setTag(WorldTags.FRUIT_TAG);
		this.cycleLength = cycleLength;
	}

	/**
	 * Handles collection by the avatar and schedules a respawn.
	 *
	 * @param other     The colliding object.
	 * @param collision Collision details.
	 */
	@Override
	public void onCollisionEnter(GameObject other, Collision collision) {
		super.onCollisionEnter(other, collision);
		if (!isFruitExist) {
			return;
		}
		if (!WorldTags.AVATAR_TAG.equals(other.getTag())) {
			return;
		}
		isFruitExist = false;
		renderer().setOpaqueness(FRUIT_INVISIBLE_OPACITY);
		new ScheduledTask(this, cycleLength, false, this::respawnFruit);
	}

	/**
	 * Makes the fruit visible and collectible again.
	 */
	private void respawnFruit() {
		isFruitExist = true;
		renderer().setOpaqueness(FRUIT_VISIBLE_OPACITY);
	}

	/**
	 * Restricts collisions to the avatar while the fruit is available.
	 *
	 * @param other The potential colliding object.
	 * @return True if collision should occur, otherwise false.
	 */
	@Override
	public boolean shouldCollideWith(GameObject other) {
		return isFruitExist && WorldTags.AVATAR_TAG.equals(other.getTag());
	}
}
