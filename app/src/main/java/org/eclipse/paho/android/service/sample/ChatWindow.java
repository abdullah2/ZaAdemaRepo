package org.eclipse.paho.android.service.sample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import junit.framework.Test;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.android.service.MqttService;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class ChatWindow extends Activity {

    private String ClientId = "TestId";
    private String clientHandle = "tcp://52.28.79.78:1883" + ClientId;
    private Connection c = null;
    private Context context = null;
    private ArrayList<String> MessagesArray = null;
    private StableArrayAdapter MessagesAdapter = null;
    private ArrayAdapter<Spanned> arrayAdapter = null;
    private ChatWindow chatWindow = this;
    private ChangeListener changeListener = null;
    private ArrayAdapter<Connection> arrayAdapterConnection = null;
    private boolean Connected = false;
    private static Connections instance = null;
    private Connection TestConnection = null;
    private boolean Subscribed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_window);

        c = Connections.getInstance(this).getConnection(clientHandle);

        context = this;

        //Set the arrayAdapter so the sent messages are displayed on the screen

        final ListView MessagesList = (ListView) findViewById(R.id.MessagesList);

        MessagesArray = new ArrayList<String>();
        MessagesArray.add("Just test");

        MessagesAdapter = new StableArrayAdapter(this, android.R.layout.simple_list_item_1, MessagesArray);
        MessagesList.setAdapter(MessagesAdapter);

        arrayAdapter = new ArrayAdapter<Spanned>(context, R.layout.list_view_text_view);

        changeListener = new ChangeListener();
        arrayAdapterConnection = new ArrayAdapter<Connection>(this, R.layout.connection_text_view);

        connectAction();
        Connected = true;
        c = Connections.getInstance(this).getConnection(clientHandle);

        Spanned[] history = c.history();
        arrayAdapter.addAll(history);

        c.getClient().setCallback(new MqttCallbackHandler(this, c.getClient().getServerURI() + c.getClient().getClientId()));
        c.getClient().registerResources(context);
        c.registerChangeListener(changeListener);

        MessagesArray = c.RealHistory();
        MessagesAdapter.notifyDataSetChanged();
    }


    public void refresh() {
        if (MessagesAdapter != null) {

            MessagesAdapter.clear();
            if (Connected)
                MessagesAdapter.addAll(Connections.getInstance(context).getConnection(clientHandle).RealHistory());

            MessagesAdapter.notifyDataSetChanged();
        }
    }

    public void Send(View view) {
        if (!Subscribed) {
            subscribe();
            Subscribed = true;
        } else {
            publish();
            refresh();
        }
    }

    @Override
    protected void onDestroy() {
        disconnect();
        super.onDestroy();
    }

    private void reconnect() {

        Connections.getInstance(context).getConnection(clientHandle).changeConnectionStatus(Connection.ConnectionStatus.CONNECTING);
        c = Connections.getInstance(context).getConnection(clientHandle);

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

    private void disconnect() {

        //if the client is not connected, process the disconnect
        if (!c.isConnected()) {
            Log.d("MyTag", "Not connected");
            return;
        }

        try {
            Log.d("MyTag", "Connected");
            c.getClient().disconnect(null, new ActionListener(context, ActionListener.Action.DISCONNECT, clientHandle, null));
            c.changeConnectionStatus(Connection.ConnectionStatus.DISCONNECTING);
        } catch (MqttException e) {
            Log.e(this.getClass().getCanonicalName(), "Failed to disconnect the client with the handle " + clientHandle, e);
            c.addAction("Client failed to disconnect");
        }
    }

    private void publish() {

        String topic = "Chat";
        String message = ClientId + "MESSAGE" +((EditText) findViewById(R.id.message)).getText().toString();
        ((EditText) findViewById(R.id.message)).getText().clear();
        int qos = ActivityConstants.defaultQos;
        boolean retained = false;

        String[] args = new String[2];
        args[0] = message;
        //args[1] = topic + ";qos:" + qos + ";retained:" + retained;

        try {
            Connections.getInstance(context).getConnection(clientHandle).getClient().publish(topic, message.getBytes(), qos, retained, null, new ActionListener(context, ActionListener.Action.PUBLISH, clientHandle, args));
        } catch (MqttSecurityException e) {
            Log.e(this.getClass().getCanonicalName(), "Failed to publish a message from the client with the handle " + clientHandle, e);
        } catch (MqttException e) {
            Log.e(this.getClass().getCanonicalName(), "Failed to publish a message from the client with the handle " + clientHandle, e);
        }
    }

    private class StableArrayAdapter extends ArrayAdapter<String> {

        HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

        public StableArrayAdapter(Context context, int textViewResourceId, List<String> objects) {
            super(context, textViewResourceId, objects);
            for (int i = 0; i < objects.size(); ++i) {
                mIdMap.put(objects.get(i), i);
            }
        }
    }

    private void subscribe() {
        String topic = "Chat";
        try {
            String[] topics = new String[1];
            int qos = ActivityConstants.defaultQos;
            topics[0] = topic;
            Connections.getInstance(context).getConnection(clientHandle).getClient().subscribe(topic, qos, null, new ActionListener(context, ActionListener.Action.SUBSCRIBE, clientHandle, topics));
        } catch (MqttSecurityException e) {
            Log.e(this.getClass().getCanonicalName(), "Failed to subscribe to" + topic + " the client with the handle " + clientHandle, e);
        } catch (MqttException e) {
            Log.e(this.getClass().getCanonicalName(), "Failed to subscribe to" + topic + " the client with the handle " + clientHandle, e);
        }
    }

    private class ChangeListener implements PropertyChangeListener {

        /**
         * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
         */
        @Override
        public void propertyChange(PropertyChangeEvent event) {
            // connection object has change refresh the UI

            chatWindow.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    //((HistoryFragment) connectionDetails.sectionsPagerAdapter.getItem(0)).refresh();
                    refresh();
                }
            });
        }
    }

    private void connectAction() {

        MqttConnectOptions conOpt = new MqttConnectOptions();

        // The basic client information
        String server = "52.28.79.78";
        String clientId = ClientId;
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
}