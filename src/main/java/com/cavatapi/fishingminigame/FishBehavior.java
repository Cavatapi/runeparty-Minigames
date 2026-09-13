package com.cavatapi.fishingminigame;

import java.util.Random;

public abstract class FishBehavior
{
    protected final Random random;

    public FishBehavior(long seed)
    {
        random = new Random(seed);
    }

    public abstract void update(Fish fish);

    // =========================================================
    // RANDOM BEHAVIOR SELECTION
    // =========================================================

    public static FishBehavior randomBehavior(long roundSeed)
    {
        Random selector =
                new Random(roundSeed);

        int choice =
                selector.nextInt(6);

        switch (choice)
        {
            case 0:
                return new Jumper(roundSeed);

            case 1:
                return new Sinker(roundSeed);

            case 2:
                return new Mixed(roundSeed);

            case 3:
                return new Floater(roundSeed);

            case 4:
                return new Darter(roundSeed);

            default:
                return new Lazy(roundSeed);
        }
    }

    // =========================================================
    // JUMPER
    //
    // Makes strong upward jumps but is forced to come back
    // down instead of spending the entire game at the top.
    // =========================================================

    public static class Jumper extends FishBehavior
    {
        private int jumpCooldown = 0;

        public Jumper(long seed)
        {
            super(seed);
        }

        @Override
        public void update(Fish fish)
        {
            double range =
                    fish.getBottomLimit() -
                            fish.getTopLimit();

            double positionPercent =
                    (fish.getY() -
                            fish.getTopLimit()) /
                            range;

            /*
             * Constant downward pull.
             *
             * This makes the fish sink again
             * after performing a jump.
             */
            fish.addVelocity(0.12);

            if (fish.getVelocity() > 2.2)
            {
                fish.setVelocity(2.2);
            }

            if (jumpCooldown > 0)
            {
                jumpCooldown--;
            }

            /*
             * If the fish is in the top 20%
             * of the fishing bar, prevent it
             * from jumping upward again.
             *
             * It also has a chance to make
             * a stronger downward dive.
             */
            if (positionPercent < 0.20)
            {
                if (random.nextInt(100) < 5)
                {
                    fish.setVelocity(
                            1.5 +
                                    random.nextDouble() *
                                            1.5
                    );

                    jumpCooldown =
                            20 +
                                    random.nextInt(25);
                }

                return;
            }

            /*
             * Normal upward jump.
             */
            if (jumpCooldown <= 0)
            {
                if (random.nextInt(100) < 4)
                {
                    fish.setVelocity(
                            -2.8 -
                                    random.nextDouble() *
                                            1.8
                    );

                    jumpCooldown =
                            30 +
                                    random.nextInt(45);
                }
            }
        }
    }

    // =========================================================
    // SINKER
    //
    // Naturally moves toward the bottom but occasionally
    // makes small upward movements to throw the player off.
    // =========================================================

    public static class Sinker extends FishBehavior
    {
        private int movementCooldown = 0;

        public Sinker(long seed)
        {
            super(seed);
        }

        @Override
        public void update(Fish fish)
        {
            /*
             * Constant downward movement.
             */
            fish.addVelocity(0.06);

            if (fish.getVelocity() > 2.0)
            {
                fish.setVelocity(2.0);
            }

            if (movementCooldown > 0)
            {
                movementCooldown--;
            }

            /*
             * Occasionally fake an upward move.
             */
            if (movementCooldown <= 0)
            {
                if (random.nextInt(100) < 5)
                {
                    double smallRise =
                            -0.7 -
                                    random.nextDouble() *
                                            0.8;

                    fish.setVelocity(
                            smallRise
                    );

                    movementCooldown =
                            15 +
                                    random.nextInt(25);
                }
            }
        }
    }

    // =========================================================
    // MIXED
    //
    // Can jump, sink, or make smaller movements.
    // Less predictable than Jumper or Sinker.
    // =========================================================

    public static class Mixed extends FishBehavior
    {
        private int actionCooldown = 0;

        public Mixed(long seed)
        {
            super(seed);
        }

        @Override
        public void update(Fish fish)
        {
            /*
             * Slight downward tendency.
             */
            fish.addVelocity(0.04);

            if (fish.getVelocity() > 2.0)
            {
                fish.setVelocity(2.0);
            }

            if (fish.getVelocity() < -4.5)
            {
                fish.setVelocity(-4.5);
            }

            if (actionCooldown > 0)
            {
                actionCooldown--;
                return;
            }

            if (random.nextInt(100) < 5)
            {
                int action =
                        random.nextInt(3);

                if (action == 0)
                {
                    /*
                     * Strong upward movement.
                     */
                    fish.setVelocity(
                            -2.5 -
                                    random.nextDouble() *
                                            2.0
                    );
                }
                else if (action == 1)
                {
                    /*
                     * Downward movement.
                     */
                    fish.setVelocity(
                            1.0 +
                                    random.nextDouble() *
                                            1.5
                    );
                }
                else
                {
                    /*
                     * Smaller upward movement.
                     */
                    fish.setVelocity(
                            -0.5 -
                                    random.nextDouble()
                    );
                }

                actionCooldown =
                        20 +
                                random.nextInt(40);
            }
        }
    }

    // =========================================================
    // FLOATER
    //
    // Prefers the upper half of the fishing bar without
    // constantly sticking to the very top.
    // =========================================================

    public static class Floater extends FishBehavior
    {
        private int movementCooldown = 0;

        public Floater(long seed)
        {
            super(seed);
        }

        @Override
        public void update(Fish fish)
        {
            double middle =
                    (
                            fish.getTopLimit() +
                                    fish.getBottomLimit()
                    ) / 2.0;

            /*
             * Below the middle:
             * encourage movement upward.
             *
             * Above the middle:
             * allow a slight downward drift.
             */
            if (fish.getY() > middle)
            {
                fish.addVelocity(-0.05);
            }
            else
            {
                fish.addVelocity(0.02);
            }

            if (fish.getVelocity() < -1.5)
            {
                fish.setVelocity(-1.5);
            }

            if (fish.getVelocity() > 1.0)
            {
                fish.setVelocity(1.0);
            }

            if (movementCooldown > 0)
            {
                movementCooldown--;
            }

            if (movementCooldown <= 0)
            {
                if (random.nextInt(100) < 4)
                {
                    fish.setVelocity(
                            -1.0 -
                                    random.nextDouble()
                    );

                    movementCooldown =
                            20 +
                                    random.nextInt(30);
                }
            }
        }
    }

    // =========================================================
    // DARTER
    //
    // Makes sudden, sharp movements in either direction.
    // =========================================================

    public static class Darter extends FishBehavior
    {
        private int dartCooldown = 0;

        public Darter(long seed)
        {
            super(seed);
        }

        @Override
        public void update(Fish fish)
        {
            /*
             * Gradually slow down after a dart.
             */
            fish.setVelocity(
                    fish.getVelocity() *
                            0.94
            );

            if (dartCooldown > 0)
            {
                dartCooldown--;
                return;
            }

            if (random.nextInt(100) < 7)
            {
                boolean dartUp =
                        random.nextBoolean();

                if (dartUp)
                {
                    fish.setVelocity(
                            -2.5 -
                                    random.nextDouble() *
                                            2.5
                    );
                }
                else
                {
                    fish.setVelocity(
                            2.0 +
                                    random.nextDouble() *
                                            2.5
                    );
                }

                dartCooldown =
                        15 +
                                random.nextInt(30);
            }
        }
    }

    // =========================================================
    // LAZY
    //
    // Calmer than the other fish, but no longer stays almost
    // completely stationary like the original version.
    // =========================================================

    public static class Lazy extends FishBehavior
    {
        private int movementCooldown = 0;

        public Lazy(long seed)
        {
            super(seed);
        }

        @Override
        public void update(Fish fish)
        {
            /*
             * Gradually slow down between movements.
             */
            fish.setVelocity(
                    fish.getVelocity() *
                            0.98
            );

            if (movementCooldown > 0)
            {
                movementCooldown--;
                return;
            }

            /*
             * More frequent movement than the
             * original Lazy behavior.
             */
            if (random.nextInt(100) < 6)
            {
                int action =
                        random.nextInt(4);

                if (action == 0)
                {
                    /*
                     * Slow upward movement.
                     */
                    fish.setVelocity(
                            -1.0 -
                                    random.nextDouble() *
                                            0.7
                    );
                }
                else if (action == 1)
                {
                    /*
                     * Slow downward movement.
                     */
                    fish.setVelocity(
                            1.0 +
                                    random.nextDouble() *
                                            0.7
                    );
                }
                else if (action == 2)
                {
                    /*
                     * Occasional stronger
                     * upward movement.
                     */
                    fish.setVelocity(
                            -1.8 -
                                    random.nextDouble() *
                                            0.8
                    );
                }
                else
                {
                    /*
                     * Occasional stronger
                     * downward movement.
                     */
                    fish.setVelocity(
                            1.8 +
                                    random.nextDouble() *
                                            0.8
                    );
                }

                movementCooldown =
                        25 +
                                random.nextInt(45);
            }
        }
    }
}