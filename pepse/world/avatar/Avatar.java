package pepse.world.avatar;

import danogl.GameObject;
import danogl.collisions.Collision;
import danogl.gui.ImageReader;
import danogl.gui.UserInputListener;
import danogl.gui.rendering.AnimationRenderable;
import danogl.gui.rendering.Renderable;
import danogl.util.Vector2;
import pepse.world.Block;
import pepse.world.WorldTags;

import java.awt.event.KeyEvent;
import java.util.function.Consumer;

/**
 * A controllable avatar that can move and jump using keyboard input.
 * The avatar maintains an energy value that is consumed by actions and restored by collecting fruit
 * Or from idling on the ground/trunk
 * The avatar has changing animations logic for each defined moving state : IDLING,RUNNING,JUMPING
 *
 * @author Liad Koren
 */
public class Avatar extends GameObject {
	/**
	 * The avatar's width and height.
	 */
	public static final int AVATAR_SIZE = Block.SIZE;

	private static final float HORIZONTAL_SPEED_IN_BLOCKS = 10f;
	private static final float VELOCITY_X_MOVING_HORIZONTAL = HORIZONTAL_SPEED_IN_BLOCKS * Block.SIZE;
	private static final float JUMP_HEIGHT_IN_NUMBER_OF_BLOCKS = 4f;
	private static final float JUMP_HEIGHT_IN_MULTIPLE_OF_BLOCKS =
			JUMP_HEIGHT_IN_NUMBER_OF_BLOCKS * Block.SIZE;
	private static final float GRAVITY = 600f;
	private static final float VELOCITY_Y_JUMPING = -(float) Math.sqrt(2f * GRAVITY *
			JUMP_HEIGHT_IN_MULTIPLE_OF_BLOCKS);
	private static final String INITIAL_AVATAR_IMAGE = "assets/idle_0.png";
	private static final boolean AVATAR_IMAGES_TRANSPARENCY = true;
	private static final float IDLE_FRAME_TIME = 0.12f;
	private static final float RUN_FRAME_TIME = 0.08f;
	private static final float JUMP_FRAME_TIME = 0.10f;
	private static final String[] IDLE_IMAGES = {
			"assets/idle_0.png", "assets/idle_1.png", "assets/idle_2.png", "assets/idle_3.png"};
	private static final String[] JUMP_IMAGES = {
			"assets/jump_0.png", "assets/jump_1.png", "assets/jump_2.png", "assets/jump_3.png"};
	private static final String[] RUN_IMAGES = {
			"assets/run_0.png", "assets/run_1.png", "assets/run_2.png",
			"assets/run_3.png", "assets/run_4.png", "assets/run_5.png"};
	private static final float MAX_AVATAR_ENERGY = 100f;
	private static final float MIN_AVATAR_ENERGY = 0f;
	private static final float IDLING_ON_GROUND_ENERGY_EXTRA = 1f;
	private static final float EATING_FRUIT_ENERGY_EXTRA = 10f;
	private static final float RUNNING_ON_GROUND_ENERGY_COST = 2f;
	private static final float RUNNING_IN_AIR_ENERGY_COST = 0f;
	private static final float JUMPING_FROM_GROUND_ENERGY_COST = 20f;
	private static final float JUMPING_FROM_AIR_WHILE_FALLING_ENERGY_COST = 50f;
	private static final float JUMPING_FROM_AIR_WHILE_RISING_ENERGY_COST = 0f;
	private static final float MIN_ENERGY_TO_START_RUNNING = 10f;
	private static final float NO_HORIZONTAL_MOVEMENT_X_VELOCITY = 0f;
	private static final float NO_VERTICAL_MOVEMENT_Y_VELOCITY = 0f;
	//found the right value after trial and error
	private static final float LANDING_FROM_ABOVE_NORMAL_THRESHOLD = -0.8f;

	private final UserInputListener inputListener;
	private final AnimationRenderable idleAnimation;
	private final AnimationRenderable runAnimation;
	private final AnimationRenderable jumpAnimation;
	private final java.util.Set<GameObject> floorObjects = new java.util.HashSet<>();
	private float currentAvatarEnergy = MAX_AVATAR_ENERGY;
	private boolean wasJumpPressedLastUpdate = false;
	private Consumer<Float> energyChangedCallback = null;
	private MovementState lastMovementState = null;
	private boolean isAvatarLookingLeft = false;
	/**
	 * Creates a new avatar object at the given position.
	 *
	 * @param topLeftCorner The avatar's initial top-left corner.
	 * @param inputListener Input source for movement controls.
	 * @param imageReader   Image loader used for animation frames.
	 */
	public Avatar(Vector2 topLeftCorner, UserInputListener inputListener, ImageReader imageReader) {
		super(topLeftCorner,
				Vector2.ONES.mult(AVATAR_SIZE),
				imageReader.readImage(INITIAL_AVATAR_IMAGE, AVATAR_IMAGES_TRANSPARENCY));
		setTag(WorldTags.AVATAR_TAG);
		physics().preventIntersectionsFromDirection(Vector2.ZERO);
		transform().setAccelerationY(GRAVITY);
		this.inputListener = inputListener;
		this.idleAnimation = new AnimationRenderable(IDLE_IMAGES,
				imageReader, AVATAR_IMAGES_TRANSPARENCY, IDLE_FRAME_TIME);
		this.runAnimation = new AnimationRenderable(RUN_IMAGES,
				imageReader, AVATAR_IMAGES_TRANSPARENCY, RUN_FRAME_TIME);
		this.jumpAnimation = new AnimationRenderable(JUMP_IMAGES,
				imageReader, AVATAR_IMAGES_TRANSPARENCY, JUMP_FRAME_TIME);
	}

	/**
	 * Updates movement, energy, and animation state according to input and collisions.
	 *
	 * @param deltaTime Time elapsed since the last update.
	 */
	@Override
	public void update(float deltaTime) {
		super.update(deltaTime);
		boolean pressedLeft = inputListener.isKeyPressed(KeyEvent.VK_LEFT);
		boolean pressedRight = inputListener.isKeyPressed(KeyEvent.VK_RIGHT);
		boolean pressedJump = inputListener.isKeyPressed(KeyEvent.VK_SPACE);
		boolean jumpJustPressed = pressedJump && !wasJumpPressedLastUpdate;
		wasJumpPressedLastUpdate = pressedJump;
		AvatarPosition avatarPosition = findAvatarPosition();
		MovementRequest horizontalRequest =
				defineHorizontalMovementRequest(pressedLeft, pressedRight);
		updateAvatarFacingDirection(horizontalRequest);
		boolean didJumpThisUpdate = false;
		if (jumpJustPressed) {
			MovementRequest jumpRequest = defineJumpRequest(avatarPosition);
			if (jumpRequest == MovementRequest.JUMP_FROM_GROUND ||
					jumpRequest == MovementRequest.JUMP_FROM_AIR_WHILE_FALLING) {
				didJumpThisUpdate = handleJumpRequest(avatarPosition, jumpRequest);
				if (didJumpThisUpdate) {
					avatarPosition = AvatarPosition.IN_THE_AIR;
				}
			}
		}
		handleHorizontalMovementRequest(avatarPosition, horizontalRequest);
		MovementState movementState = findMovementStateAfterMovement(avatarPosition, horizontalRequest);
		handlePerUpdateMovementEnergy(avatarPosition, movementState, horizontalRequest);
		switch (movementState) {
			case RUN:
				runAnimation.update(deltaTime);
				break;
			case JUMP:
				jumpAnimation.update(deltaTime);
				break;
			case IDLE:
				idleAnimation.update(deltaTime);
				break;
		}
		updateAvatarAnimationByState(movementState);
	}

	/**
	 * Handles collisions with terrain/trunks and fruit.
	 *
	 * @param other     The collided object.
	 * @param collision Collision details.
	 */
	@Override
	public void onCollisionEnter(GameObject other, Collision collision) {
		super.onCollisionEnter(other, collision);
		boolean isFruit = checkIsFruit(other);
		boolean isTerrainOrTrunk = checkIsTerrainOrTrunk(other);
		if (!isTerrainOrTrunk && !isFruit) {
			return;
		}
		if (isFruit) {
			gainEnergy(EATING_FRUIT_ENERGY_EXTRA);
			return;
		}
		if (collision.getNormal().y() < LANDING_FROM_ABOVE_NORMAL_THRESHOLD
				&& getVelocity().y() >= NO_VERTICAL_MOVEMENT_Y_VELOCITY) {
			floorObjects.add(other);
			if (getVelocity().y() > NO_VERTICAL_MOVEMENT_Y_VELOCITY) {
				this.transform().setVelocityY(NO_VERTICAL_MOVEMENT_Y_VELOCITY);
			}
		}
	}

	/**
	 * Updates ground-contact tracking when leaving a terrain/trunk collision.
	 *
	 * @param other The object the avatar stopped colliding with.
	 */
	@Override
	public void onCollisionExit(GameObject other) {
		super.onCollisionExit(other);
		if (!checkIsTerrainOrTrunk(other)) {
			return;
		}
		floorObjects.remove(other);
	}

	/**
	 * Sets a callback that is invoked whenever the avatar energy value changes.
	 * The callback is also invoked immediately with the current value.
	 *
	 * @param energyChangedCallback Callback to notify energy changes .
	 */
	public void setEnergyChangedCallback(Consumer<Float> energyChangedCallback) {
		this.energyChangedCallback = energyChangedCallback;
		if (this.energyChangedCallback != null) {
			this.energyChangedCallback.accept(currentAvatarEnergy);
		}
	}

	/**
	 * Computes whether the avatar is currently supported by ground objects.
	 *
	 * @return {@link AvatarPosition#ON_THE_GROUND} if the avatar is supported,
	 * otherwise {@link AvatarPosition#IN_THE_AIR}.
	 */
	private AvatarPosition findAvatarPosition() {
		if (!floorObjects.isEmpty() || getVelocity().y() == 0) {
			return AvatarPosition.ON_THE_GROUND;
		}
		return AvatarPosition.IN_THE_AIR;
	}

	/**
	 * Determines whether the avatar is currently rising or falling in the air.
	 *
	 * @return The current air situation.
	 */
	private AvatarAirSituation findAirSituation() {
		if (getVelocity().y() > NO_VERTICAL_MOVEMENT_Y_VELOCITY) {
			return AvatarAirSituation.FALLING;
		}
		return AvatarAirSituation.RISING;
	}

	/**
	 * Resolves the horizontal movement request from left/right key states.
	 *
	 * @param pressedLeft  Whether left is pressed.
	 * @param pressedRight Whether right is pressed.
	 * @return The resolved horizontal movement request.
	 */
	private MovementRequest defineHorizontalMovementRequest(boolean pressedLeft, boolean pressedRight) {
		if (pressedLeft && pressedRight) {
			return MovementRequest.MOVE_RIGHT_AND_LEFT;
		} else if (pressedLeft) {
			return MovementRequest.MOVE_LEFT;
		} else if (pressedRight) {
			return MovementRequest.MOVE_RIGHT;
		} else {
			return MovementRequest.MOVE_NONE;
		}
	}

	/**
	 * Resolves the jump request based on whether the avatar is on the ground and its vertical velocity.
	 *
	 * @param avatarPosition Whether the avatar is on ground or in air.
	 * @return The resolved jump request.
	 */
	private MovementRequest defineJumpRequest(AvatarPosition avatarPosition) {
		if (avatarPosition == AvatarPosition.ON_THE_GROUND) {
			return MovementRequest.JUMP_FROM_GROUND;
		}
		AvatarAirSituation airSituation = findAirSituation();
		if (airSituation == AvatarAirSituation.FALLING) {
			return MovementRequest.JUMP_FROM_AIR_WHILE_FALLING;
		}
		return MovementRequest.JUMP_REQUESTED_WHILE_RISING;
	}

	// from here its private methods not part of api ----------------------------------------------

	/**
	 * Returns the energy cost associated with a movement request in the current situation.
	 *
	 * @param movementRequest The requested action.
	 * @param avatarPosition  Whether the avatar is on ground or in air.
	 * @return Energy cost for performing the request.
	 */
	private float findEnergyCost(MovementRequest movementRequest, AvatarPosition avatarPosition) {
		switch (movementRequest) {
			case MOVE_LEFT:
			case MOVE_RIGHT:
				if (avatarPosition == AvatarPosition.ON_THE_GROUND) {
					return RUNNING_ON_GROUND_ENERGY_COST;
				}
				return RUNNING_IN_AIR_ENERGY_COST;
			case JUMP_FROM_GROUND:
				return JUMPING_FROM_GROUND_ENERGY_COST;
			case JUMP_FROM_AIR_WHILE_FALLING:
				return JUMPING_FROM_AIR_WHILE_FALLING_ENERGY_COST;
			default:
				return JUMPING_FROM_AIR_WHILE_RISING_ENERGY_COST;
		}
	}

	/**
	 * Checks whether the avatar is allowed to perform the given request based on energy and state.
	 *
	 * @param movementRequest The requested action.
	 * @param avatarPosition  Whether the avatar is on ground or in air.
	 * @return True if the request may be executed, otherwise false.
	 */
	private boolean isEnoughEnergyForRequest(MovementRequest movementRequest,
											 AvatarPosition avatarPosition) {
		float energyCost = findEnergyCost(movementRequest, avatarPosition);

		if (movementRequest == MovementRequest.MOVE_LEFT || movementRequest == MovementRequest.MOVE_RIGHT) {
			if (avatarPosition != AvatarPosition.ON_THE_GROUND) {
				return true;
			}
			boolean isAvatarNotMovingNow = (getVelocity().x() == NO_HORIZONTAL_MOVEMENT_X_VELOCITY);
			if (isAvatarNotMovingNow) {
				return currentAvatarEnergy >= MIN_ENERGY_TO_START_RUNNING;
			}
			return currentAvatarEnergy >= energyCost;
		}
		if (movementRequest == MovementRequest.JUMP_FROM_GROUND) {
			if (avatarPosition != AvatarPosition.ON_THE_GROUND) {
				return false;
			}
			return currentAvatarEnergy >= energyCost;
		}

		if (movementRequest == MovementRequest.JUMP_FROM_AIR_WHILE_FALLING) {
			if (avatarPosition != AvatarPosition.IN_THE_AIR) {
				return false;
			}
			AvatarAirSituation airSituation = findAirSituation();
			if (airSituation != AvatarAirSituation.FALLING) {
				return false;
			}
			return currentAvatarEnergy >= energyCost;
		}
		return true;
	}

	/**
	 * Applies the energy cost of the performed jump request.
	 *
	 * @param jumpRequest    The jump request performed.
	 * @param avatarPosition The avatar's position when jumping.
	 */
	private void handleJumpEnergyUpdate(MovementRequest jumpRequest, AvatarPosition avatarPosition) {
		float energyCost = findEnergyCost(jumpRequest, avatarPosition);
		spendEnergy(energyCost);
	}

	/**
	 * Updates horizontal velocity based on a movement request and current energy constraints.
	 *
	 * @param avatarPosition    Whether the avatar is on ground or in air.
	 * @param horizontalRequest The requested horizontal movement.
	 */
	private void handleHorizontalMovementRequest(AvatarPosition avatarPosition,
												 MovementRequest horizontalRequest) {
		if (horizontalRequest == MovementRequest.MOVE_NONE ||
				horizontalRequest == MovementRequest.MOVE_RIGHT_AND_LEFT) {
			transform().setVelocityX(NO_HORIZONTAL_MOVEMENT_X_VELOCITY);
			return;
		}
		if (avatarPosition == AvatarPosition.ON_THE_GROUND) {
			boolean isAvatarRunningNow = (getVelocity().x() != NO_HORIZONTAL_MOVEMENT_X_VELOCITY);
			if (!isAvatarRunningNow) {
				if (currentAvatarEnergy < MIN_ENERGY_TO_START_RUNNING) {
					transform().setVelocityX(NO_HORIZONTAL_MOVEMENT_X_VELOCITY);
					return;
				}
			} else {
				if (currentAvatarEnergy < RUNNING_ON_GROUND_ENERGY_COST) {
					transform().setVelocityX(NO_HORIZONTAL_MOVEMENT_X_VELOCITY);
					return;
				}
			}
		}
		float xVel = NO_HORIZONTAL_MOVEMENT_X_VELOCITY;
		if (horizontalRequest == MovementRequest.MOVE_LEFT) {
			xVel -= VELOCITY_X_MOVING_HORIZONTAL;
		}
		if (horizontalRequest == MovementRequest.MOVE_RIGHT) {
			xVel += VELOCITY_X_MOVING_HORIZONTAL;
		}
		transform().setVelocityX(xVel);
	}

	/**
	 * Executes a jump request if conditions and energy allow it.
	 *
	 * @param avatarPosition The current avatar position.
	 * @param jumpRequest    The jump request to execute.
	 * @return True if a jump was performed, otherwise false.
	 */
	private boolean handleJumpRequest(AvatarPosition avatarPosition, MovementRequest jumpRequest) {
		if (!isEnoughEnergyForRequest(jumpRequest, avatarPosition)) {
			return false;
		}
		floorObjects.clear();
		handleJumpEnergyUpdate(jumpRequest, avatarPosition);
		transform().setVelocityY(VELOCITY_Y_JUMPING);
		return true;
	}

	/**
	 * Applies per-update energy changes when the avatar is on the ground.
	 *
	 * @param avatarPosition    Whether the avatar is on ground or in air.
	 * @param movementState     The resolved movement state.
	 * @param horizontalRequest The horizontal movement request.
	 */
	private void handlePerUpdateMovementEnergy(AvatarPosition avatarPosition,
											   MovementState movementState,
											   MovementRequest horizontalRequest) {
		if (avatarPosition != AvatarPosition.ON_THE_GROUND) {
			return;
		}
		if (horizontalRequest == MovementRequest.MOVE_RIGHT_AND_LEFT) {
			gainEnergy(IDLING_ON_GROUND_ENERGY_EXTRA);
			return;
		}
		switch (movementState) {
			case IDLE:
				gainEnergy(IDLING_ON_GROUND_ENERGY_EXTRA);
				break;

			case RUN:
				spendEnergy(RUNNING_ON_GROUND_ENERGY_COST);
				break;

			default:
				break;
		}
	}

	/**
	 * Resolves the current movement state after movement logic.
	 *
	 * @param avatarPosition    Whether the avatar is on ground or in air.
	 * @param horizontalRequest The horizontal movement request.
	 * @return The resolved movement state.
	 */
	private MovementState findMovementStateAfterMovement(AvatarPosition avatarPosition,
														 MovementRequest horizontalRequest) {
		if (avatarPosition == AvatarPosition.IN_THE_AIR) {
			if (horizontalRequest == MovementRequest.MOVE_LEFT ||
					horizontalRequest == MovementRequest.MOVE_RIGHT) {
				return MovementState.RUN;
			}
			return MovementState.JUMP;
		}
		boolean askedToMove = (horizontalRequest == MovementRequest.MOVE_LEFT ||
				horizontalRequest == MovementRequest.MOVE_RIGHT);
		boolean actuallyMoving = (getVelocity().x() != NO_HORIZONTAL_MOVEMENT_X_VELOCITY);
		if (askedToMove && actuallyMoving) {
			return MovementState.RUN;
		}
		return MovementState.IDLE;
	}

	/**
	 * Decreases the current energy by the given amount.
	 *
	 * @param energyAmount Energy to subtract.
	 */
	private void spendEnergy(float energyAmount) {
		float beforeEnergy = currentAvatarEnergy;
		float updatedAvatarEnergy = currentAvatarEnergy - energyAmount;
		if (updatedAvatarEnergy <= MIN_AVATAR_ENERGY) {
			currentAvatarEnergy = MIN_AVATAR_ENERGY;
		} else {
			currentAvatarEnergy = updatedAvatarEnergy;
		}
		notifyEnergyChanged(beforeEnergy);
	}

	/**
	 * Increases the current energy by the given amount.
	 *
	 * @param energyAmount Energy to add.
	 */
	private void gainEnergy(float energyAmount) {
		float beforeEnergy = currentAvatarEnergy;
		float updatedAvatarEnergy = currentAvatarEnergy + energyAmount;
		if (updatedAvatarEnergy >= MAX_AVATAR_ENERGY) {
			currentAvatarEnergy = MAX_AVATAR_ENERGY;
		} else {
			currentAvatarEnergy = updatedAvatarEnergy;
		}
		notifyEnergyChanged(beforeEnergy);
	}

	/**
	 * Notifies the energy callback if the energy value changed.
	 *
	 * @param beforeEnergy The energy value before the update.
	 */
	private void notifyEnergyChanged(float beforeEnergy) {
		if (energyChangedCallback == null) {
			return;
		}
		if (beforeEnergy == currentAvatarEnergy) {
			return;
		}
		energyChangedCallback.accept(currentAvatarEnergy);
	}

	/**
	 * Updates the rendered animation based on movement state and current facing direction.
	 *
	 * @param movementState The current movement state.
	 */
	private void updateAvatarAnimationByState(MovementState movementState) {
		if (movementState == lastMovementState) {
			renderer().setIsFlippedHorizontally(isAvatarLookingLeft);
			return;
		}
		lastMovementState = movementState;
		Renderable currentAnimation = idleAnimation;
		switch (movementState) {
			case RUN:
				currentAnimation = runAnimation;
				break;
			case JUMP:
				currentAnimation = jumpAnimation;
				break;
			default:
				break;
		}
		renderer().setRenderable(currentAnimation);
		renderer().setIsFlippedHorizontally(isAvatarLookingLeft);
	}

	/**
	 * Updates the avatar's facing direction based on horizontal movement input.
	 *
	 * @param horizontalRequest The current horizontal movement request.
	 */
	private void updateAvatarFacingDirection(MovementRequest horizontalRequest) {
		if (horizontalRequest == MovementRequest.MOVE_LEFT) {
			isAvatarLookingLeft = true;
		} else if (horizontalRequest == MovementRequest.MOVE_RIGHT) {
			isAvatarLookingLeft = false;
		}
	}

	/**
	 * Checks whether a collided object is terrain or a trunk.
	 *
	 * @param other The object to check.
	 * @return True if the object is terrain or trunk, otherwise false.
	 */
	private boolean checkIsTerrainOrTrunk(GameObject other) {
		String otherObjectTag = other.getTag();
		boolean isGroundOrTrunk = false;
		if (otherObjectTag.equals(WorldTags.TERRAIN_TAG) || otherObjectTag.equals(WorldTags.TRUNK_TAG)) {
			isGroundOrTrunk = true;
		}
		return isGroundOrTrunk;
	}

	/**
	 * Checks whether a collided object is a fruit.
	 *
	 * @param other The object to check.
	 * @return True if the object is a fruit, otherwise false.
	 */
	private boolean checkIsFruit(GameObject other) {
		String otherObjectTag = other.getTag();
		boolean isFruit = false;
		if (otherObjectTag.equals(WorldTags.FRUIT_TAG)) {
			isFruit = true;
		}
		return isFruit;
	}

	/**
	 * Indicates whether the avatar is currently supported by the ground.
	 */
	private enum AvatarPosition {
		ON_THE_GROUND,
		IN_THE_AIR
	}

	/**
	 * Indicates whether the avatar is moving upward or downward while in the air.
	 */
	private enum AvatarAirSituation {
		RISING,
		FALLING
	}

	/**
	 * Represents a high-level movement request derived from user input.
	 */
	private enum MovementRequest {
		MOVE_LEFT,
		MOVE_RIGHT,
		MOVE_NONE,
		MOVE_RIGHT_AND_LEFT,
		JUMP_FROM_GROUND,
		JUMP_FROM_AIR_WHILE_FALLING,
		JUMP_REQUESTED_WHILE_RISING
	}

	/**
	 * Represents the avatar's current movement state for animation and energy logic.
	 */
	private enum MovementState {
		IDLE,
		RUN,
		JUMP
	}
}
