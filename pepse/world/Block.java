package pepse.world;

import danogl.GameObject;
import danogl.components.GameObjectPhysics;
import danogl.gui.rendering.Renderable;
import danogl.util.Vector2;

/**
 * A single square world block with fixed size and immovable physics.
 * It the basic unit of the world
 *
 * @author Liad Koren
 */
public class Block extends GameObject {
	/**
	 * The size width and height of a block in pixels.
	 */
	public static final int SIZE = 30;

	/**
	 * Creates a new block at the given position.
	 *
	 * @param topLeftCorner The block's top-left corner.
	 * @param renderable    The block's renderable.
	 */
	public Block(Vector2 topLeftCorner, Renderable renderable) {
		super(topLeftCorner, Vector2.ONES.mult(SIZE), renderable);
		physics().preventIntersectionsFromDirection(Vector2.ZERO);
		physics().setMass(GameObjectPhysics.IMMOVABLE_MASS);
	}
}
