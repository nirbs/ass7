package animations;

import biuoop.DrawSurface;
import biuoop.KeyboardSensor;
import environment.Collidable;
import environment.GameEnvironment;
import game.Counter;
import game.Velocity;
import game.LevelInformation;
import geometry.Rectangle;
import geometry.Point;
import listeners.BallRemover;

import listeners.BlockRemover;
import listeners.ScoreTrackingListener;
import sprites.*;

import java.awt.Color;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import static java.lang.Math.abs;

/**
 * The GameLevel class contains a a SpriteCollection which will be all the sprites in
 * the game, a GameEnvironment which will include all of the Collidable objects,
 * and a GUI which will be our game windows.
 *
 * @author Matan Ben Noach Nir Ben Shalom
 * @version 1.0 9 April 2016
 */

public class GameLevel implements Animation {
    private SpriteCollection sprites; // All of the sprites in the game.
    private GameEnvironment environment; // The game environment.
    private Counter blockCounter; // The block counter of the game.
    private Counter ballCounter; // The ball counter of the game.
    private AnimationRunner runner; // The animation runner of the game/
    private boolean running; // A boolean variable if the game runs or not.
    private KeyboardSensor keyboard; // The keyboard sensor of the game.
    private LevelInformation myLevel; // The level information of the game.
    private Paddle paddle; // The paddle of the game.
    private LiveIndicator liveIndicator; // The live indicator of the game.
    private long startTime;
    /**
     * Constructor to create the GameLevel.
     *
     * @param level  the LevelInformation.
     * @param key    the keyboard sensor of the game.
     * @param runner the animation runner of the game.
     */
    public GameLevel(LevelInformation level, KeyboardSensor key, AnimationRunner runner) {
        sprites = new SpriteCollection();
        environment = new GameEnvironment();
        blockCounter = new Counter();
        ballCounter = new Counter();
        myLevel = level;
        this.keyboard = key;
        this.runner = runner;
    }

    /**
     * addCollidable method adds a Collidable object to the game.
     *
     * @param c the Collidable object to add.
     */
    public void addCollidable(Collidable c) {
        environment.addCollidable(c);
    }

    /**
     * addSprite method adds a Sprite object to the game.
     *
     * @param s the Sprite object to add.
     */
    public void addSprite(Sprite s) {
        sprites.addSprite(s);
    }

    /**
     * remove Collidable method removes a Collidable object from the game.
     *
     * @param c the Collidable object to remove.
     */
    public void removeCollidable(Collidable c) {
        environment.removeCollidable(c);
    }

    /**
     * removeSprite method removes a Sprite object from the game.
     *
     * @param s the Sprite object to add.
     */
    public void removeSprite(Sprite s) {
        sprites.removeSprite(s);
    }

    /**
     * initialize method draws the borders, ball, paddle and blocks on a new
     * gui.
     *
     * @param lives   is the number of live
     * @param myScore is number of the score.
     */
    public void initialize(LiveIndicator lives, ScoreIndicator myScore) {
        geometry.Rectangle borders = new Rectangle(800, 600);
        liveIndicator = lives;
        addSprite(myLevel.getBackground());

        // Create the paddle.
        Rectangle paddleRec = new Rectangle(360 - myLevel.paddleWidth() / 2, borders.getHeight() - 51,
                myLevel.paddleWidth(), 10);
        paddle = new Paddle(keyboard, paddleRec, borders, myLevel.paddleSpeed(),
                new ColorSprite(paddleRec, Color.GREEN));
        paddle.addToGame(this);
        addDeathBorder(borders.getMaxY(), borders.getMaxX(), 20);
        addDeathBorder(0, borders.getMaxX(), 20);

        // Create the score indicator
        Rectangle infoFrame = new Rectangle(0, 0, borders.getMaxX(), 20);
        Rectangle infoFrameFilled = new Rectangle(infoFrame.getUpperLeft(), borders.getMaxX(), 20,
                new ColorSprite(infoFrame, Color.white));
        Block playInfo = new Block(infoFrameFilled);
        playInfo.addToGame(this);


        List<Block> myBlocks = myLevel.blocks();
        for (Block block : myBlocks) {
            block.addHitListener(new BlockRemover(this, blockCounter));
            block.addHitListener(new ScoreTrackingListener(myScore.getScore()));
            block.addHitListener(new BallRemover(this, ballCounter));
            block.addToGame(this);
        }
        blockCounter.increase(myLevel.numberOfBlocksToRemove());

        addSprite(lives);
        addSprite(myScore);
        addSprite(new LevelIndicator(myLevel.levelName()));
        this.startTime = System.currentTimeMillis();
    }

    private void addDeathBorder(int y, int width, int height) {
        // Create the death border.
        Rectangle r = new Rectangle(0, y, width, height);
        Block deathBorder = new Block(0, y, width, height,
                new ColorSprite(r, Color.black));
        deathBorder.addHitListener(new BallRemover(this, ballCounter));
        addCollidable(deathBorder);
    }


    /**
     * createBall method creates a new ball to the game.
     *
     * @param p      the center point of the ball.
     * @param radius the radius of the ball.
     * @param v      the velocity of the ball.
     */
    public void createBall(Point p, int radius, Velocity v) {
        Ball ball = new Ball(p, radius, Color.WHITE, v, environment);
        ball.addToGame(this);
    }

    /**
     * playOneTurn method resets the game to the start position.
     */
    public void playOneTurn() {
        paddle.relocatePaddle(360 - myLevel.paddleWidth() / 2);
        this.runner.run(new CountdownAnimation(2, 3, sprites)); // countdown before turn starts.

        this.running = true;
        // use our runner to run the current animation -- which is one turn of
        // the game.
        this.runner.run(this);
    }

    /**
     * shouldStop method returns the not value of running.
     *
     * @return the not value of the running variable.
     */
    public boolean shouldStop() {
        return !this.running;
    }

    /**
     * doOneFrame method draws the game on the screen.
     *
     * @param d  the drawSurface to draw on.
     * @param dt specifies the amount of seconds passed since the last call.
     */
    public void doOneFrame(DrawSurface d, double dt) {
        // the logic from the previous playOneTurn method goes here.
        // the `return` or `break` statements should be replaced with
        // this.running = false;
        this.sprites.drawAllOn(d);
        this.sprites.notifyAllTimePassed(dt);
        if (this.keyboard.isPressed("p")) {
            this.runner.run(new StopScreenDecorator(keyboard, "j", new PauseScreen(keyboard)));
        }
        if (this.keyboard.isPressed(KeyboardSensor.SPACE_KEY)) {
            if(abs(System.currentTimeMillis() - startTime)>350) {
                Rectangle paddleRec = paddle.getCollisionRectangle();
                createBall(new Point(paddleRec.getX() + paddleRec.getWidth() / 2, paddleRec.getY() - 10),
                        3, new Velocity(0, -500));
                this.startTime= System.currentTimeMillis();
            }
        }
        if (blockCounter.getValue() == 0) {
            this.running = false;
        }
    }


    /**
     * getBlockCounter returns the current number of blocks.
     *
     * @return the current number of blocks.
     */
    public Counter getBlockCounter() {
        return blockCounter;
    }


}