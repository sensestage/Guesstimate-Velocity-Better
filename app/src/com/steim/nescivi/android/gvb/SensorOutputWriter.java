package com.steim.nescivi.android.gvb;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.os.Environment;

public class SensorOutputWriter {
	public static final String TYPE_ACCELEROMETER = "Accelerometer";
	public static final String TYPE_LINEARACCELERO = "LinearAccelerometer";
	public static final String TYPE_ORIENTATION = "Orientation";
	public static final String TYPE_MAGNETIC = "Magnetic";
	public static final String TYPE_GYRO = "Gyroscope";
	public static final String TYPE_LIGHT = "Light";
	public static final String TYPE_GVB = "GVB";
	public static final String TYPE_OTHER_UNKNOWN = "Other-unknown";	
	public static final String OUTPUT_DIRECTORY = "GVB";
	public static final int BUFFER_SIZE = 100;
	
	private File outputDirectory;
	private BufferedOutputStream output;
	private String type;
	private long startTime;
	private CircularFloatArrayBuffer mBuffer;
	
	public SensorOutputWriter(String type) throws StorageErrorException {
		this.type = type;
		
		ensureOutputDirectoryExists();
		
		mBuffer = new CircularFloatArrayBuffer(BUFFER_SIZE);
		
		try {
			startLog();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new StorageErrorException("Could not open file for writing:" + e.getLocalizedMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new StorageErrorException("Could not open file for writing:" + e.getLocalizedMessage());
		}
	}
	
	public void writeReadings(float readings[]) throws StorageErrorException {
		long timeOffset = System.currentTimeMillis() - startTime;
		StringBuilder sb = new StringBuilder();

		// Construct string to write in output and array to store in buffer
		sb.append(timeOffset);
		float toBuffer[] = new float[readings.length + 1];
		toBuffer[0] = timeOffset;
		for (int i = 0; i < readings.length; i ++) {
			sb.append(" " + readings[i]);
			toBuffer[i+1] = readings[i];
		}

		// Write string in output and add to buffer
		writeLine(sb.toString());
		mBuffer.add(toBuffer);
		
	}
	
	public void writeLines(String lines[]) throws StorageErrorException {
		for (String line : lines) {
			writeLine(line);
		}
	}
	
	public void writeLine(String line) throws StorageErrorException {
		String str = line + "\n";
		try {
			output.write(str.getBytes());
		} catch (IOException e) {
			throw new StorageErrorException("Could not write to output: " + e.getLocalizedMessage());
		}
		
	}
	
	public void close() throws StorageErrorException {
		endLog();
	}
	
	private void ensureOutputDirectoryExists() throws StorageErrorException {
		// See if external storage is present
		String storageStatus = Environment.getExternalStorageState();
		if (!storageStatus.equals(Environment.MEDIA_MOUNTED)) {
			// If not present, launch exception
			throw new StorageErrorException("External storage state is: " + storageStatus);
		}
		
		// Ensure output dir exists
		File dir = Environment.getExternalStorageDirectory();
		outputDirectory = new File(dir, OUTPUT_DIRECTORY);
		if (outputDirectory.exists())
			return;
		if (!outputDirectory.mkdir()) {
			throw new StorageErrorException("Could not create: " + outputDirectory.getAbsolutePath());
		}
	}

	private void startLog() throws StorageErrorException, IOException {
		// Get current time
		Date now = new Date();
		startTime = now.getTime();
				
		// Compute file name
		String fileName = "SensorOutput_" + formatDate(now, true) + "_" + type + ".txt";
		
		// Create file
		File out = new File(outputDirectory, fileName);
		out.createNewFile();
		output = new BufferedOutputStream(new FileOutputStream(out));
		
		// Insert initial data
		String lines[] = {
				"# === BEGINNING OUTPUT DUMP at " + formatDate(now, false) + " ===",
				"# SENSOR TYPE: " + type,
		};
		writeLines(lines);
		
//		try {
//			output.flush();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
	
	private void endLog() throws StorageErrorException {
		Date now = new Date();
		String line = "# === ENDING OUTPUT DUMP at " + formatDate(now, false) + " ===";
		writeLine(line);
		
		try {
			output.flush();
			output.close();
		} catch (IOException e) {
			throw new StorageErrorException("Could not close the output: " + e.getLocalizedMessage());
		}
	}

	private String formatDate(Date date, boolean forFilePath) {
		String format = forFilePath ? "yyyy-MM-dd_HH.mm.ss" : "yyyy/MM/dd HH:mm:ss";
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		return formatter.format(date); 	
	}
	
	public float[][] getBuffer() {
		if (mBuffer.getSize() == 0) {
			return new float[0][0];
		}
		else {
			float[][] result = (float[][]) mBuffer.getContents();
			return result;
		}
	}
	
}
