package org.eclipse.paho.android.service.sample;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;


public class NewChat extends Activity {

    Context context;
    private String clientHandle = "tcp://52.28.79.78:1883TestId";
    private ChangeListener changeListener = null;
    private NewChat newChat = this;
    private ArrayAdapter<Connection> arrayAdapterConnection = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_chat);

        context = this;


        changeListener = new ChangeListener();
        arrayAdapterConnection = new ArrayAdapter<Connection>(this, R.layout.connection_text_view);

        connectAction();

        Map<String, Connection> connections = Connections.getInstance(this).getConnections();
        Log.e("MyTag", connections.toString());

        reconnect();

        connections = Connections.getInstance(this).getConnections();
        Log.e("MyTag", connections.toString());

        //context = this;
    }

    public void Publish(View view) {
        publish();
    }

    private void reconnect() {

        Connections.getInstance(context).getConnection(clientHandle).changeConnectionStatus(Connection.ConnectionStatus.CONNECTING);

        Connection c = Connections.getInstance(context).getConnection(clientHandle);
        Log.d("MyTag", clientHandle);
        try {
            c.getClient().connect(c.getConnectionOptions(), null, new ActionListener(context, ActionListener.Action.CONNECT, clientHandle, null));
        } catch (MqttSecurityException e) {
            Log.e(this.getClass().getCanonicalName(), "Failed to reconnect the client with the handle " + clientHandle, e);
            c.addAction("Client failed to connect");
        } catch (MqttException e) {
            Log.e(this.getClass().getCanonicalName(), "Failed to reconnect the client with the handle " + clientHandle, e);
            c.addAction("Client failed to connect");
        }
    }

    private void publish() {

        String topic = "Chat";
        //String message = ((EditText) findViewById(R.id.message)).getText().toString();
        String message = "Same";
        int qos = ActivityConstants.defaultQos;
        boolean retained = false;

        String[] args = new String[2];
        args[0] = message;
        args[1] = topic + ";qos:" + qos + ";retained:" + retained;

        try {
            Connections.getInstance(context).getConnection(clientHandle).getClient().publish(topic, message.getBytes(), qos, retained, null, new ActionListener(context, ActionListener.Action.PUBLISH, clientHandle, args));
        } catch (MqttSecurityException e) {
            Log.e(this.getClass().getCanonicalName(), "Failed to publish a messged from the client with the handle " + clientHandle, e);
        } catch (MqttException e) {
            Log.e(this.getClass().getCanonicalName(), "Failed to publish a messged from the client with the handle " + clientHandle, e);
        }
    }

    private void connectAction() {

        MqttConnectOptions conOpt = new MqttConnectOptions();

        // The basic client information
        String server = "52.28.79.78";
        String clientId = "TestId";
        int port = 1883;
        boolean cleanSession = true;
        boolean ssl = false;
        String uri = "tcp://";
        uri = uri + server + ":" + port;

        MqttAndroidClient client;
        client = Connections.getInstance(this).createClient(this, uri, clientId);

        // create a client handle
        String clientHandle = uri + clientId;

        // connection options
        int timeout = ActivityConstants.defaultTimeOut;
        int keepalive = ActivityConstants.defaultKeepAlive;

        Connection connection = new Connection(clientHandle, clientId, server, port, this, client, ssl);
        arrayAdapterConnection.add(connection);

        connection.registerChangeListener(changeListener);
        // connect client

        String[] actionArgs = new String[1];
        actionArgs[0] = clientId;
        connection.changeConnectionStatus(Connection.ConnectionStatus.CONNECTING);

        conOpt.setCleanSession(cleanSession);
        conOpt.setConnectionTimeout(timeout);
        conOpt.setKeepAliveInterval(keepalive);

        final ActionListener callback = new ActionListener(this, ActionListener.Action.CONNECT, clientHandle, actionArgs);

        client.setCallback(new MqttCallbackHandler(this, clientHandle));

        //set traceCallback
        client.setTraceCallback(new MqttTraceCallback());

        connection.addConnectionOptions(conOpt);
        Connections.getInstance(this).addConnection(connection);
        try {
            client.connect(conOpt, null, callback);
        } catch (MqttException e) {
            Log.e(this.getClass().getCanonicalName(), "MqttException Occured", e);
        }
    }

    private class ChangeListener implements PropertyChangeListener {

        /**
         * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
         */
        @Override
        public void propertyChange(PropertyChangeEvent event) {
            // connection object has change refresh the UI

            newChat.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    //((HistoryFragment) connectionDetails.sectionsPagerAdapter.getItem(0)).refresh();
                    // refresh();
                }
            });
        }
    }
}
