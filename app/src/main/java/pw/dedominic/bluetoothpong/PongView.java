package pw.dedominic.bluetoothpong;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.os.Handler;

import java.util.Random;

public class PongView extends View
{
    // Constants
    public float paddle_space = 0;
    public float paddle_half_height;
    public float paddle_half_thickness;
    public float paddle_thickness;
    public float ball_radius;
    public float aspect_ratio;
    public float ball_speed;
    private final Random rand = new Random();
    private int player_paddle_side;

    // redraws view
    private DrawHandler redraw = new DrawHandler();

    private Handler mHandler;

    // color of ball
    private Paint paint;

    // ball and paddle
    private Ball ball;
    private Paddle player_paddle;
    private Paddle enemy_paddle;

    // if initialized
    private boolean isInit = false;
    private boolean waitingForOpponent = true;

    // creates a thread that will update and draw the view
    // based on delay set by call to sleep in PongView.update() function
    private class DrawHandler extends Handler
    {
        // once message is received, calls update
        // and invalidates the view
        // this effectively redraws scene
        @Override
        public void handleMessage(Message msg)
        {
            PongView.this.update();
            PongView.this.invalidate(); // force redraw
        }

        // time in milliseconds to draw next frame
        public void sleep(long time)
        {
            this.removeMessages(0);
            this.sendMessageDelayed(obtainMessage(0), time);
        }
    }

    public PongView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        paint = new Paint();
        paint.setColor(Consts.PAINT_COLOR); // solid gray
    }

    // currently just makes ball bounce around view
    public void update()
    {
        if (getHeight() == 0 || getWidth() == 0)
        {
            redraw.sleep(1);
            return;
        }

        if (!isInit)
        {
            setConstants();
            newGame();
            redraw.sleep(1);
            return;
        }

        if (waitingForOpponent)
        {
            redraw.sleep(1000/Consts.FRAMES_PER_SECOND);
        }

        // ask for paddle changes from opponent
        mHandler.obtainMessage(Consts.SEND_PADDLE_INFO, getPlayerPaddleHeightPercent()).sendToTarget();

        if (ball.getLeft() <= 0
            || ball.getRight() >= getWidth())
        {
            newGame();
            waitingForOpponent = true;
            mHandler.obtainMessage(Consts.GAME_OVER).sendToTarget();
        }

        if (ball.getTop() <= 0
            || ball.getBottom() >= getHeight())
        {
            ball.yDeflect();
        }

        if (player_paddle_side * ball.getX_vel() < 0)
        {
            if (ball.getBottom()>= player_paddle.getTop()  &&
                ball.getLeft()  >= player_paddle.getLeft() &&
                ball.getRight() <= player_paddle.getRight()&&
                ball.getTop()   <= player_paddle.getBottom())
            {
                ball.yDeflect();
                //ball.xDeflect();
            }
            else if (ball.y >= player_paddle.getTop()    &&
                     ball.y <= player_paddle.getBottom() &&
                     ball.getLeft() <= paddle_space+paddle_thickness &&
                     ball.getLeft() >= paddle_space)
            {
                ball.xDeflect();
            }
        }
        else
        {
            if (ball.getBottom()>= enemy_paddle.getTop()  &&
                ball.getLeft()  >= enemy_paddle.getLeft() &&
                ball.getRight() <= enemy_paddle.getRight()&&
                ball.getTop()   <= enemy_paddle.getBottom())
            {
                ball.yDeflect();
                ball.xDeflect();
            }
            else if (ball.y >= enemy_paddle.getTop()    &&
                     ball.y <= enemy_paddle.getBottom() &&
                     ball.getRight() >= getWidth() - (paddle_space+paddle_thickness) &&
                     ball.getRight() <= getWidth() - (paddle_space))
            {
                ball.xDeflect();
            }
        }

        // time till next frame in milliseconds
        redraw.sleep(1000/Consts.FRAMES_PER_SECOND);
    }

    public void newGame()
    {
        ball = new Ball(getWidth()/2,getHeight()/2, ball_radius);
        if (player_paddle_side == Consts.PLAYER_PADDLE_LEFT) {
            player_paddle = new Paddle(paddle_space, getHeight() / 2, paddle_half_height, paddle_half_thickness);
            enemy_paddle = new Paddle(getWidth() - paddle_space, getHeight() / 2, paddle_half_height, paddle_half_thickness);
            serveBall();
        }
        else
        {
            player_paddle = new Paddle(getWidth() - paddle_space, getHeight() / 2, paddle_half_height, paddle_half_thickness);
            enemy_paddle = new Paddle(paddle_space, getHeight() / 2, paddle_half_height, paddle_half_thickness);
        }

        isInit = true;
    }

    public void serveBall()
    {
        double ball_ang = randomAngle();
        ball.setVel(ball_ang, ball_speed, aspect_ratio);

        String ball_ang_str = String.valueOf(ball_ang);
        byte[] angle_bytes = ball_ang_str.getBytes();
        mHandler.obtainMessage(Consts.BALL_ANGLE, angle_bytes);
    }

    public void setBallVel(double ball_ang)
    {
        ball.setVel(ball_ang, ball_speed, aspect_ratio);
        mHandler.obtainMessage(Consts.READY).sendToTarget();
        waitingForOpponent = false;
    }

    // returns random angle in radians
    public double randomAngle()
    {
        int max = 63;
        int min = -max;
        return (rand.nextInt((max - min) + 1) + min) * .1;
    }

    public void setConstants()
    {
        paddle_half_thickness = getWidth()/Consts.PADDLE_HALFWIDTH_FRACT;
        paddle_space = getWidth()/Consts.PADDLE_SPACE_FRACT + paddle_half_thickness;
        paddle_thickness = 2*paddle_half_thickness;
        paddle_half_height = getHeight()/Consts.PADDLE_HALFHEIGHT_FRACT;
        ball_radius = getWidth()/Consts.BALL_RADIUS_FRACT;
        aspect_ratio = ((float)getWidth())/getHeight();
        ball_speed = ball_radius/4;
    }

    public void player_tilt(float tilt_val)
    {
        if (!isInit || waitingForOpponent)
        {
            return;
        }

        tilt_val *= 2;
        // makes sure paddles don't go off screen
        if (tilt_val < 0)
        {
            if (player_paddle.getTop() >= 0)
            {
                player_paddle.paddleMove((float)tilt_val);
            }
            else
            {
                player_paddle.paddleMove(0);
            }
        }
        else
        {
            if (player_paddle.getBottom() <= getHeight())
            {
                player_paddle.paddleMove((float)tilt_val);
            }
            else
            {
                player_paddle.paddleMove(0);
            }
        }
    }

    public void enemy_tilt(float moving)
    {
        enemy_paddle.setY(moving*getHeight());
    }

    public void setOtherConst(int side, Handler handler)
    {
        player_paddle_side = side;
        mHandler = handler;
    }

    public void setWaitingForOpponent(boolean state)
    {
        waitingForOpponent = state;
    }

    public byte[] getPlayerPaddleHeightPercent()
    {
        float paddle_loc = player_paddle.getY() / getHeight();
        String writeString = String.valueOf(paddle_loc);
        return writeString.getBytes();
    }

    @Override
    public void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        if (!isInit)
        {
            return;
        }

        ball.update(canvas, paint);
        player_paddle.update(canvas, paint);
        enemy_paddle.update(canvas, paint);
    }
}
