package pw.dedominic.bluetoothpong;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothPongService
{
    private static final String mAppName = "BluetoothPong";
    private static final UUID mUUID = UUID.fromString("7ff29267-b894-486b-8eb3-403f682c3fa7");

    private final BluetoothAdapter mBtAdapter;
    private final Handler mHandler;

    private ListenThread listener;
    private JoinThread joiner;
    private ConnectedThread connected;
    private int connectionState = Consts.STATE_DOING_NOTHING;

    /**
     *
     * @param handler Handler that allows a way to communicate
     *                back with the activity
     */
    public BluetoothPongService(Handler handler)
    {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = handler;
    }

    public void changeState(int state)
    {
        connectionState = state;
        // call handler to generate a message to activity
        mHandler.obtainMessage(Consts.CONNECT_STATE_CHANGE, state, 0).sendToTarget();
    }

    public void listen()
    {
        if (connectionState != Consts.STATE_DOING_NOTHING)
        {
            stopAllConnections();
        }

        listener = new ListenThread();
        changeState(Consts.STATE_IS_LISTENING);
        listener.start();
    }

    public void connectToHost(BluetoothDevice device)
    {
        if (connectionState != Consts.STATE_DOING_NOTHING)
        {
            stopAllConnections();
        }

        joiner = new JoinThread(device);
        changeState(Consts.STATE_IS_CONNECTING);
        joiner.start();
    }

    public void connect(BluetoothSocket socket)
    {
//        if (listener != null)
//        {
//            listener.stop();
//        }
//
//        if (joiner != null)
//        {
//            joiner.stop();
//        }

        changeState(Consts.STATE_IS_CONNECTED);
        connected = new ConnectedThread(socket);
        connected.start();
    }

    public void stopAllConnections()
    {
        if (listener != null)
        {
            listener.cancel();
            listener = null;
        }

        if (joiner != null)
        {
            joiner.cancel();
            joiner = null;
        }

        if (connected != null)
        {
            connected.cancel();
            connected = null;
        }

        changeState(Consts.STATE_DOING_NOTHING);
    }

    public void write(byte[] bytes)
    {
        if (connectionState != Consts.STATE_IS_CONNECTED)
        {
            return;
        }

        connected.write(bytes);
    }

    /**
     * This thread will make the app listen and wait for a connection
     * will remain in this thread until connection is established
     */
    private class ListenThread extends Thread
    {
        private final BluetoothServerSocket srvSocket;

        public ListenThread()
        {
            BluetoothServerSocket tmp = null;
            try
            {
                tmp = mBtAdapter.listenUsingRfcommWithServiceRecord(mAppName, mUUID);
            }
            catch (IOException e) {}

            srvSocket = tmp;
        }

        public void run()
        {
            BluetoothSocket socket = null;

            while (connectionState != Consts.STATE_IS_CONNECTED)
            {
                try
                {
                    socket = srvSocket.accept();
                }
                catch (IOException e)
                {
                    break;
                }
                if (socket != null && connectionState != Consts.STATE_IS_CONNECTED)
                {
                    connect(socket);
                    break;
                }
            }
        }

        public void cancel()
        {
            try
            {
                srvSocket.close();
            }
            catch (IOException e) {}
        }
    }

    private class JoinThread extends Thread
    {
        private final BluetoothSocket socket;
        private final BluetoothDevice srvDevice;

        public JoinThread(BluetoothDevice device)
        {
            BluetoothSocket tmp = null;
            srvDevice = device;

            try
            {
                tmp = srvDevice.createRfcommSocketToServiceRecord(mUUID);
            }
            catch (IOException e) {}

            socket = tmp;
        }

        public void run()
        {
            mBtAdapter.cancelDiscovery();

            try
            {
                socket.connect();
            }
            catch (IOException e)
            {
                try
                {
                    changeState(Consts.STATE_DOING_NOTHING);
                    socket.close();
                } catch (IOException ee) { }
                stopAllConnections();
                return;
            }

            connect(socket);
        }

        public void cancel()
        {
            try
            {
                socket.close();
            }
            catch (IOException e) {}
        }
    }

    private class ConnectedThread extends Thread
    {
        private final BluetoothSocket connection;
        private final InputStream in;
        private final OutputStream out;

        public ConnectedThread(BluetoothSocket socket)
        {
            connection = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try
            {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            }
            catch (IOException e) { }

            in = tmpIn;
            out = tmpOut;
        }

        public void run()
        {
            byte[] buffer = new byte[1024];

            int bytes_ret;

            while (true)
            {
                try
                {
                    bytes_ret = in.read(buffer);
                    mHandler.obtainMessage(Consts.READING, bytes_ret, -1, buffer).sendToTarget();
                    // try to send bytes to activity using a message
                }
                catch (IOException e)
                {
                    Log.e("BT-DEBUG", e.getMessage());
                    stopAllConnections();
                    break;
                }
            }
        }

        public void write(byte[] bytes)
        {
            try
            {
                out.write(bytes);
            }
            catch (IOException e) {}
        }

        public void cancel()
        {
            try
            {
                changeState(Consts.STATE_DOING_NOTHING);
                connection.close();
            }
            catch (IOException e) {}
        }
    }
}
