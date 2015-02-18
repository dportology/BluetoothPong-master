package pw.dedominic.bluetoothpong;

public interface Consts
{
    // messages
    public static final int READING = 0;
    public static final int GAME_OVER = 1;
    public static final int CONNECT_STATE_CHANGE = 2;
//    public static final int DEFLECTION_X = 3;
//    public static final int DEFLECTION_Y = 4;
    public static final int SEND_PADDLE_INFO = 4;
//    public static final int PADDLE_DOWN = 5;
//    public static final int PADDLE_UP = 6;
    public static final int BALL_ANGLE = 7;
    public static final int READY = 8;

    // game constants
    public static final int PLAYER_PADDLE_LEFT  = 1;
    public static final int PLAYER_PADDLE_RIGHT =-1;
    public static final int FRAMES_PER_SECOND   =60;

    public static final float PADDLE_SPACE_FRACT  = 100;
    public static final float PADDLE_HALFWIDTH_FRACT  = 100;
    public static final float PADDLE_HALFHEIGHT_FRACT = 8;
    public static final float BALL_RADIUS_FRACT = 100;

    public static final int PAINT_COLOR = 0xff666666; // grey

    // bluetooth services
    public static final int STATE_DOING_NOTHING = 0;
    public static final int STATE_IS_LISTENING  = 1;
    public static final int STATE_IS_CONNECTING = 2;
    public static final int STATE_IS_CONNECTED  = 3;

    public static final int GET_MAC_ADDRESS = 0;
}
