package pw.dedominic.bluetoothpong;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

public class MainActivity extends ActionBarActivity implements SensorEventListener
{
    // Constants
    public static final int BLUETOOTH_INIT_SERVER = 0;
    public static final int BLUETOOTH_CONN_TO_SERVER = 1;
    private PongView mPongView;
    private BluetoothPongService mBtPongService;

    private int bluetoothState = Consts.STATE_DOING_NOTHING;
    private int paddleSide = Consts.PLAYER_PADDLE_LEFT;
    private boolean isInit = false;
    private boolean listeningForPaddle = false;
    private boolean listeningForBallAng = false;

    private SensorManager senManage;
    private Sensor accelerometer;

    private BluetoothAdapter mBluetoothAdapter;

    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case Consts.READING:
                    String value = new String((byte[]) msg.obj, 0, msg.arg1);

                    if (value.equals("START"))
                    {
                        gameStart(false);
                        listeningForBallAng = true;
                    }
                    else if (value.equals("READY"))
                    {
                        mPongView.setWaitingForOpponent(false);
                    }
                    else {
                        if (listeningForPaddle) {
                            try
                            {
                                mPongView.enemy_tilt(Float.parseFloat(value));
                            }
                            catch (NumberFormatException e)
                            {
                            }
                        }
                        else
                        {
                            try
                            {
                                mPongView.setBallVel(Double.parseDouble(value));
                                listeningForBallAng = false;
                            }
                            catch (NumberFormatException e)
                            {
                            }
                        }
                    }
                    return;
                case Consts.CONNECT_STATE_CHANGE:
                    // handle changes in connection state
                    Log.e("BT-DEBUG", "" + msg.arg1);
                    bluetoothState = msg.arg1;
                    return;
                case Consts.BALL_ANGLE:
                    mBtPongService.write((byte[])msg.obj);
                    return;
                case Consts.READY:
                    mBtPongService.write("READY".getBytes());
                    listeningForPaddle = true;
                    return;
                case Consts.SEND_PADDLE_INFO:
                    mBtPongService.write((byte[])msg.obj);
                case Consts.GAME_OVER:
                    listeningForPaddle = false;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

         mBtPongService = new BluetoothPongService(mHandler);
        // keeps screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // Sensor change handler
    // called when accelerometer detects a tilt
    @Override
    public void onSensorChanged(SensorEvent e)
    {
        // only rotation around y axis needed.
        if (e.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
        {
            if (e.values[0] > 1 || -e.values[0] > 1)
            {
                mPongView.player_tilt(e.values[0]);
            }
        }
    }

    // ignores this
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }

    protected void connectToEnemy(int type) {
        int REQUEST_ENABLE_BT = 1;

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        if (type == BLUETOOTH_INIT_SERVER)
        {
            paddleSide = Consts.PLAYER_PADDLE_LEFT;

            mBtPongService.listen();
        }
        else
        {
            paddleSide = Consts.PLAYER_PADDLE_RIGHT;

            Intent getBtDevice = new Intent(this, PairedBluetoothActivity.class);
            startActivityForResult(getBtDevice, Consts.GET_MAC_ADDRESS);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Log.e("BT-DEBUG", "test");
        if (resultCode == Activity.RESULT_OK && requestCode == Consts.GET_MAC_ADDRESS)
        {
            String MAC_ADDRESS = data.getStringExtra("device");
            if (MAC_ADDRESS != null)
            {
                Log.e("BT-DEBUG", "CONNECTING TO: " + MAC_ADDRESS);
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(MAC_ADDRESS);
                mBtPongService.connectToHost(device);
            }
        }
    }

    // when connection is set, call this
    protected void gameStart(boolean originator)
    {
        // tell opponent to get ready
        if (originator)
        {
            mBtPongService.write("START".getBytes());
        }

        // sets accelerometer input device
        senManage = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = senManage.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senManage.registerListener(this, accelerometer , SensorManager.SENSOR_DELAY_GAME);

        // sets PongView to View field in activity
        mPongView = (PongView) findViewById(R.id.view);
        mPongView.setOtherConst(paddleSide, mHandler);
        mPongView.update(); // starts the view off
        isInit = true;
    }

    // app out of focus
    protected void onPause()
    {
        super.onPause();
        mBtPongService.stopAllConnections();
        if (!isInit)
        {
            return;
        }
        senManage.unregisterListener(this);
    }

    // app focus returns
    protected void onResume()
    {
        super.onResume();
        if (!isInit)
        {
            return;
        }
        senManage.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id)
        {
            case R.id.action_connect:
                connectToEnemy(BLUETOOTH_CONN_TO_SERVER);
                return true;
            case R.id.action_listen:
                connectToEnemy(BLUETOOTH_INIT_SERVER);
                return true;
            case R.id.action_start_game:
                Log.e("BT-DEBUG", "CONNECTED?");
                if (bluetoothState == Consts.STATE_IS_CONNECTED)
                {
                    Log.e("BT-DEBUG", "Starting");
                    gameStart(true);
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
