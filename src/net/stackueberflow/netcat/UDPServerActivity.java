package net.stackueberflow.netcat;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class UDPServerActivity extends Activity {

	private static final String TAG = "NetCat UDP Server activity";
	private TextView tv;
	private EditText input;
	private String host;
	private int port;
	private byte[] fileBytes = null;
	private UDPServerTask networkTask;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.connected);

		tv = (TextView)findViewById(R.id.output);
		input = (EditText)findViewById(R.id.input);

		Intent i = getIntent();
		port = i.getIntExtra("port", 0);

		final Uri uri;
		if ( (uri = (Uri)i.getParcelableExtra(Intent.EXTRA_STREAM)) != null) {
			Log.i(TAG, "received file uri: "+uri.toString());
			try {
				fileBytes = IOHelpers.readFile(new File(IOHelpers.getRealPathFromURI(uri, this)));
				appendToOutput("\nSending File "+IOHelpers.getRealPathFromURI(uri, this)+", size: "+fileBytes.length+" bytes");
				input.setFocusable(false);
				input.setHint("Sending file "+IOHelpers.getRealPathFromURI(uri, this));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		Log.i(TAG, "port: "+port);
		
		networkTask = new UDPServerTask();
		networkTask.execute();
		
		Button button = (Button)findViewById(R.id.button1);        

		button.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				networkTask.SendDataToNetwork(input.getText().toString().getBytes());
			}
		});
	}

	@Override 
	public void onPause() {
		super.onPause();
		if (networkTask != null)
			networkTask.cancel(true);
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

	public class UDPServerTask extends AsyncTask<byte[], byte[], Void> {
		private DatagramSocket socket;

		@Override
		protected Void doInBackground(byte[]... dataArray)  {			
			try {
				socket = new DatagramSocket(port);	
				Log.i(TAG, "doInBackground: Socket created, maximum Buffer size:" +socket.getSendBufferSize()+" bytes");
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			try {
				// Fragment the data into small chunks and send them sequentially:
				byte[] tmp = new byte[1024];
				
				while(true) {
					DatagramPacket packet = new DatagramPacket(tmp, tmp.length);
					socket.receive(packet);
					publishProgress(packet.getData());
				}
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				Log.i(TAG, "Unknown Host");
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.i(TAG, "IO Exception");
				e.printStackTrace();
			} finally {
				try {
					socket.close();
					return null;
				} catch (Exception e) {
					e.printStackTrace();
				}
				Log.i(TAG, "doInBackground: Finished");
			}
			return null;
		}
		
		public boolean SendDataToNetwork(final byte[] cmd) { //You run this from the main thread.
			// Wait until socket is open and ready to use

			Log.i(TAG, "SendDataToNetwork: Writing received message to socket");
			new Thread(new Runnable() {
				public void run() {
					try {
						DatagramPacket packet = new DatagramPacket(cmd, cmd.length);
						socket.receive(packet);
						appendToOutput(cmd);
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

		@Override
		protected void onProgressUpdate(byte[]... values) {
			byte[] data = values[0];
			appendToOutput(data);
		}

		@Override
		protected void onCancelled() {
			Log.i(TAG, "Cancelled.");

		}

		@Override
		protected void onPostExecute(Void v) {
			Log.i(TAG, "onPostExecute"); 
		}
	}
}