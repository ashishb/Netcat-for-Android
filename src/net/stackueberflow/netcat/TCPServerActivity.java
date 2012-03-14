package net.stackueberflow.netcat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class TCPServerActivity extends Activity {
	private static final String TAG = "NetCat TCP Server activity";
	private TextView tv;
	private EditText input;
	private int port;
//	private byte[] fileBytes = null;
//	private Uri uri;
	private TCPServerTask networkTask;
	
	 /** Called when the activity is first created. */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connected);
        
        tv = (TextView)findViewById(R.id.output);
        input = (EditText)findViewById(R.id.input);
        Intent i = getIntent();
        port = i.getIntExtra("port", 6000);
        
        Log.i(TAG, "port: "+port);

        
        Button button = (Button)findViewById(R.id.button1);
        networkTask = new TCPServerTask();
        networkTask.execute();
        
        input.setOnKeyListener(new OnKeyListener() {
        	@Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                    (keyCode == KeyEvent.KEYCODE_ENTER)) {
                  // Perform action on key press
                	send();
                  return true;
                }
                return false;
            }
        });
        
        button.setOnClickListener(new Button.OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		send();
        	}
        });
	}
	
	@Override 
	public void onPause() {
		super.onPause();
		if (networkTask != null)
			networkTask.cancel(true);
	}

    private void send() {
    	String str = input.getText().toString() + "\r\n";
    	networkTask.SendDataToNetwork(str.getBytes());
    	input.setText("");
    	appendToOutput(str);
    }
    
	private void appendToOutput(String str) {
		tv.append(str);
		tv.setMovementMethod(new ScrollingMovementMethod());
	}
	
	private void appendToOutput(byte[] data) {
		String str;
		try {
			str = new String(data, "UTF8");
			appendToOutput(str);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

    
	private class TCPServerTask extends AsyncTask<Void, byte[], Boolean>  {
		private ServerSocket socket = null;	
		private Socket clientSocket = null; //Client  Socket
		private InputStream is; //Network Input Stream
		private OutputStream os; //Network Output Stream
		private byte[] buffer = new byte[4096];
		private Handler handler = new Handler();

		
		@Override
		protected Boolean doInBackground(Void... params) {
			try {
				socket = new ServerSocket(port); // Listen on all interfaces
				appendToOutput("Listening on: "+port+"\n");
				while(true) {
					clientSocket = socket.accept();
					handler.post(new Runnable() {
						@Override
						public void run() {
							appendToOutput("Connection from "+clientSocket.getInetAddress().toString()+"\n");
						}
					});
					is = clientSocket.getInputStream();
					os = clientSocket.getOutputStream();
					int read;
					while((read = is.read(buffer, 0, 4096)) > 0 ) {
						byte[] tempdata = new byte[read];
						System.arraycopy(buffer, 0, tempdata, 0, read);
						publishProgress(tempdata);
						Log.i(TAG, "doInBackground: Got some data");
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					is.close();
					os.close();
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
				Log.i(TAG, "doInBackground: Finished");
			}
			return true;
		}

		public boolean SendDataToNetwork(final byte[] cmd) { //You run this from the main thread.
			// Wait until socket is open and ready to use

			if (socket.isBound() && clientSocket.isConnected()) {
				Log.i(TAG, "SendDataToNetwork: Writing received message to socket");
				new Thread(new Runnable() {
					public void run() {
						try {
							os.write(cmd);
						}
						catch (Exception e) {
							e.printStackTrace();
							Log.i(TAG, "SendDataToNetwork: Message send failed. Caught an exception");
						}
					}
				}
						).start();
				return true;
			}
			else
				Log.i(TAG, "SendDataToNetwork: Cannot send message. Socket is closed");

			return false;
		}

		@Override
		protected void onProgressUpdate(byte[]... values) {
			if (values.length > 0) {
				Log.i(TAG, "onProgressUpdate: " + values[0].length + " bytes received.");
				appendToOutput(buffer);
			}
		}

		@Override
		protected void onCancelled() {
			Log.i(TAG, "Cancelled.");
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result) {
				Log.i(TAG, "onPostExecute: Completed with an Error.");

			} else {
				Log.i(TAG, "onPostExecute: Completed.");
			}
		}
	}
}
