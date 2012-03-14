package net.stackueberflow.netcat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

// TODO: Handle not connected

public class TCPClientActivity extends Activity {
	private static final String TAG = "NetCat TCP Client activity";
	private TextView tv;
	private EditText input;
	private String host;
	private int port;
	private byte[] fileBytes = null;
	private Uri uri;
	private TCPClientTask networkTask;
	
	 /** Called when the activity is first created. */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connected);
        
        tv = (TextView)findViewById(R.id.output);
        input = (EditText)findViewById(R.id.input);
        
        Intent i = getIntent();
        host = i.getStringExtra("host");
        port = i.getIntExtra("port", 6000);
        

		if ( (uri = (Uri)i.getParcelableExtra(Intent.EXTRA_STREAM)) != null) {
			Log.i(TAG, "received file uri: "+uri.toString());
			try {
				fileBytes = IOHelpers.readFile(new File(IOHelpers.getRealPathFromURI(uri, this)));
				input.setFocusable(false);
				input.setHint("Sending file "+IOHelpers.getRealPathFromURI(uri, this));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
        
        Log.i(TAG, "host: "+host+", port: "+port);

        
        Button button = (Button)findViewById(R.id.button1);
        networkTask = new TCPClientTask();
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
    	if (fileBytes != null)
    		sendBlobTCP(uri);
    	else {
    		String str = input.getText().toString() + "\r\n";
    		networkTask.SendDataToNetwork(str.getBytes());
    		input.setText("");
    		appendToOutput(str);
    	}
    }
	
    private void sendBlobTCP(Uri uri) {
		networkTask.SendDataToNetwork(fileBytes);
		tv.append("\nSending File "+IOHelpers.getRealPathFromURI(uri, this));
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

    
	private class TCPClientTask extends AsyncTask<Void, byte[], Boolean> {
		Socket socket; //Network Socket
		InputStream is; //Network Input Stream
		OutputStream os; //Network Output Stream
		byte[] buffer = new byte[4096];

		@Override
		protected void onPreExecute() {
			Log.i(TAG, "onPreExecute");
		}

		@Override
		protected Boolean doInBackground(Void... params) { //This runs on a different thread
			boolean result = false;
			try {
				// Connect to address
				Log.i(TAG, "doInBackground: Creating socket");
				SocketAddress sockaddr = new InetSocketAddress(host, port);
				socket = new Socket();
				socket.connect(sockaddr, 5000); //10 second connection timeout
				if (socket.isConnected()) {
					is = socket.getInputStream();
					os = socket.getOutputStream();
					Log.i("AsyncTask", "doInBackground: Socket created, streams assigned");
					Log.i("AsyncTask", "doInBackground: Waiting for inital data...");
					int read;
					//This is blocking
					while((read = is.read(buffer, 0, 4096)) > 0 ) {
						byte[] tempdata = new byte[read];
						System.arraycopy(buffer, 0, tempdata, 0, read);
						publishProgress(tempdata);
						Log.i(TAG, "doInBackground: Got some data");
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				Log.i(TAG, "doInBackground: IOException");
				result = true;
			} catch (Exception e) {
				e.printStackTrace();
				Log.i(TAG, "doInBackground: Exception");
				result = true;
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
			return result;
		}

		public boolean SendDataToNetwork(final byte[] cmd) { //You run this from the main thread.
			// Wait until socket is open and ready to use

			if (socket.isConnected()) {
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
