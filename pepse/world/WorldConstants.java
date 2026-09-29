package pepse.world;

import danogl.util.Vector2;

/**
 * Provides shared constants and utility functions for world coordinate calculations.
 *
 * @author Liad Koren
 */
public final class WorldConstants {
	/**
	 * The ground height factor relative to the window height at x=0.
	 */
	public static final float GROUND_HEIGHT_FACTOR_AT_X0 = 2f / 3f;
	private static final int DIVIDABLE_NO_REMINDER = 0;

	private WorldConstants() {

	}

	/**
	 * Computes the base ground height at x=0.
	 *
	 * @param windowDimensions The window dimensions.
	 * @return The ground height at x=0.
	 */
	public static float groundHeightAtX0(Vector2 windowDimensions) {
		return windowDimensions.y() * GROUND_HEIGHT_FACTOR_AT_X0;
	}

	/**
	 * Returns the smallest multiple of {@link Block#SIZE} that is >= the given number.
	 *
	 * @param givenNum Input value.
	 * @return The closest bigger multiple of block size.
	 */
	public static int closestBiggerToMultipliesOfBlockSize(int givenNum) {
		int closest = givenNum;
		if (givenNum % Block.SIZE == DIVIDABLE_NO_REMINDER) {
			return givenNum;
		} else {
			for (int currentNum = givenNum; currentNum < givenNum + Block.SIZE; currentNum++) {
				if (currentNum % Block.SIZE == DIVIDABLE_NO_REMINDER) {
					closest = currentNum;
					break;
				}
			}
			return closest;
		}
	}

	/**
	 * Returns the largest multiple of {@link Block#SIZE} that is <= the given number.
	 *
	 * @param givenNum Input value.
	 * @return The closest smaller multiple of block size.
	 */
	public static int closestSmallerToMultipliesOfBlockSize(int givenNum) {
		int closest = givenNum;
		if (givenNum % Block.SIZE == DIVIDABLE_NO_REMINDER) {
			return givenNum;
		} else {
			for (int currentNum = givenNum; currentNum > givenNum - Block.SIZE; currentNum--) {
				if (currentNum % Block.SIZE == DIVIDABLE_NO_REMINDER) {
					closest = currentNum;
					break;
				}
			}
			return closest;
		}

	}

	/**
	 * Floors a Y value to the nearest lower multiple of {@link Block#SIZE}.
	 *
	 * @param y Input Y value.
	 * @return Floored Y aligned to block size.
	 */
	public static int floorToBlockSize(float y) {
		return ((int) Math.floor(y / Block.SIZE)) * Block.SIZE;
	}
}
