package com.cavatapi.fishingminigame;

public class Fish
{
    private double y;
    private double velocity;

    private final double topLimit;
    private final double bottomLimit;

    private final int width;
    private final int height;

    private FishBehavior behavior;

    public Fish(
            double startingY,
            double topLimit,
            double bottomLimit,
            int width,
            int height,
            FishBehavior behavior)
    {
        this.y = startingY;
        this.topLimit = topLimit;
        this.bottomLimit = bottomLimit;

        this.width = width;
        this.height = height;

        this.behavior = behavior;

        velocity = 0;
    }

    public void update()
    {
        behavior.update(this);

        y += velocity;

        if (y < topLimit)
        {
            y = topLimit;
            velocity = 0;
        }

        if (y > bottomLimit)
        {
            y = bottomLimit;
            velocity = 0;
        }
    }

    public double getY()
    {
        return y;
    }

    public double getVelocity()
    {
        return velocity;
    }

    public void setVelocity(
            double velocity)
    {
        this.velocity =
                velocity;
    }

    public void addVelocity(
            double amount)
    {
        velocity += amount;
    }

    public double getTopLimit()
    {
        return topLimit;
    }

    public double getBottomLimit()
    {
        return bottomLimit;
    }

    public int getWidth()
    {
        return width;
    }

    public int getHeight()
    {
        return height;
    }
}