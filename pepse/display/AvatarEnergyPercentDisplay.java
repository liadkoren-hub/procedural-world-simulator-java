package pepse.display;

import danogl.GameObject;
import danogl.gui.rendering.TextRenderable;
import danogl.util.Vector2;

/**
 * A display element that shows the avatar current energy as a percentage.
 * The text is refreshed only when the displayed value changes.
 *
 * @author Liad Koren
 */
public class AvatarEnergyPercentDisplay extends GameObject {
	private static final String PERCENT_POST_FIX = "%";
	private static final Vector2 DISPLAY_SIZE = new Vector2(80, 30);
	private static final String EMPTY_STRING = "";
	private static final int INITIAL_ENERGY_VALUE_OUT_OF_RANGE = -1;

	private final TextRenderable textRenderable;
	private int lastEnergyShown = INITIAL_ENERGY_VALUE_OUT_OF_RANGE;

	/**
	 * Creates a new energy display positioned at the given top-left corner.
	 *
	 * @param topLeftCorner The display's top-left corner in world coordinates.
	 */
	private AvatarEnergyPercentDisplay(Vector2 topLeftCorner) {
		super(topLeftCorner, DISPLAY_SIZE, new TextRenderable(EMPTY_STRING));
		textRenderable = (TextRenderable) renderer().getRenderable();
	}

	/**
	 * Creates an energy display object.
	 *
	 * @param topLeftCorner The display's top left corner.
	 * @return A new {@link AvatarEnergyPercentDisplay}.
	 */
	public static AvatarEnergyPercentDisplay create(Vector2 topLeftCorner) {
		return new AvatarEnergyPercentDisplay(topLeftCorner);
	}

	/**
	 * Sets the energy value to display in percentage units.
	 *
	 * @param energy The avatar's current energy.
	 */
	public void setEnergy(float energy) {
		int energyAsInt = (int) energy;
		if (energyAsInt == lastEnergyShown) {
			return;
		}
		lastEnergyShown = energyAsInt;
		textRenderable.setString(energyAsInt + PERCENT_POST_FIX);
	}
}
