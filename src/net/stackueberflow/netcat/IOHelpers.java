package net.stackueberflow.netcat;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;


public final class IOHelpers {
	public static byte[] readFile (File file) throws IOException {
		// Open file
		RandomAccessFile f = new RandomAccessFile(file, "r");

		try {
			// Get and check length
			long longlength = f.length();
			int length = (int) longlength;
			if (length != longlength) throw new IOException("File size >= 2 GB");

			// Read file and return data
			byte[] data = new byte[length];
			f.readFully(data);
			return data;
		}
		finally {
			f.close();
		}
	}
	
	// Convert the image URI to the direct file system path of the image file
	public static String getRealPathFromURI(Uri contentUri, Activity activity) {
		//String [] proj={MediaStore.Images.Media.DATA};
		String[] proj={MediaStore.Files.FileColumns.DATA};
		Cursor cursor = activity.managedQuery( contentUri,
				proj, // Which columns to return
				null,       // WHERE clause; which rows to return (all rows)
				null,       // WHERE clause selection arguments (none)
				null); // Order-by clause (ascending by name)
		int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		return cursor.getString(column_index);
	}

}
