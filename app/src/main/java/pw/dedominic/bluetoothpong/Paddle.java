package pw.dedominic.bluetoothpong;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * Created by prussian on 1/31/15.
 */
public class Paddle
{
    private float x;
    private float y;
    private float paddle_half_width;
    private float paddle_half_height;
    private float paddle_vel;

    public Paddle(float pos1, float pos2, float h, float w)
    {
        x = pos1;
        y = pos2;

        paddle_half_width = w;

        paddle_half_height = h;

        paddle_vel = 0;
    }

    public void setY(float newY)
    {
        y = newY;
    }

    public void paddleMove(float tilt_val)
    {
        paddle_vel = tilt_val;
    }

    public float getY()
    {
        return y;
    }

    public float getX()
    {
        return x;
    }

    public float getBottom()
    {
        return y + paddle_half_height;
    }

    public float getTop()
    {
        return y - paddle_half_height;
    }

    public float getLeft()
    {
        return x - paddle_half_width;
    }

    public float getRight()
    {
        return x + paddle_half_width;
    }

    public void update(Canvas screen, Paint color)
    {
        screen.drawRect(getLeft(),
                        getTop(),
                        getRight(),
                        getBottom(),
                        color);
        y += paddle_vel;
    }
}
