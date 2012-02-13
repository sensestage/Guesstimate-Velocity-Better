package com.steim.nescivi.android.gvb;

//import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Environment;

public class SensorLog {
	private static final String NAME_PATTERN = "^SensorOutput_([0-9]{4})-([0-9]{2})-([0-9]{2})_([0-9]{2})\\.([0-9]{2})\\.([0-9]{2})_(.*)\\.txt$";
	private static Pattern PATTERN = Pattern.compile(NAME_PATTERN);
	private static final String OUTPUT_DIRECTORY = "SensorLogger";
	
	private String fileName, path;
	
	private SensorLog(String path, String fileName) {
		this.path = path;
		this.fileName = fileName;		
	}

	@Override
	public String toString() {
		return displayableName(false);
	}

	public float[][] getReadings() throws IOException {
		
		return fetchReadings();
		
//		return new float[][] {
//			{1, 3, 4, 5},
//			{2, 3, 4, 5},
//			{3, 1, 4, 5},
//			{4, 1, 4, 5},
//			{5, 3, 5, 5},
//			{6, 3, 5, 5}
//		};
	}
	
	public String displayableName(boolean includeType) {
		Matcher m = PATTERN.matcher(fileName);
		boolean matches = m.matches();

		if (matches) {
			return m.group(1) + "/" + m.group(2) + "/" + m.group(3) + " " +
				m.group(4) + ":" + m.group(5) + ":" + m.group(6) +
				((includeType) ? " (" + m.group(7) + ")" : "");
		}
		else {
			return fileName;
		}
	}
	
	public String getPath() {
		return path;
	}
	
	public String getFullPath() {
		return path + File.separator + fileName;
	}
	
	// ==== Open file and parse results ====
	private float[][] fetchReadings() throws IOException {
		// Open file
		BufferedReader input = new BufferedReader(new FileReader(getFullPath()));
		
		// Create output
		ArrayList<float[]> aux = new ArrayList<float[]>();
		
		// Parse lines
		for (String line = input.readLine(); line != null; line = input.readLine()) {
			// Ignore lines beginning with "#"
			if (line.startsWith("#")) continue;
			
			String tokens[] = line.split(" ");
			if(tokens.length < 4) continue; // Ignore lines with less than 4 columns
			
			// parse columns
			float read[] = new float[tokens.length];
			for (int i = 0; i < tokens.length; i++) {
				read[i] = Float.parseFloat(tokens[i]);
			}
			aux.add(read);			
		}
		
		// convert to normal array
		float result[][] = new float[aux.size()][];
		int i = 0;
		for (Iterator<float[]> iterator = aux.iterator(); iterator.hasNext();) {
			float[] fs = (float[]) iterator.next();
			result[i] = fs;
			i++;
		}
		
		return result;
	}	

	
	// ========= Static methods to browse the results ===========
	public static final String TYPE_ACCELEROMETER = "Accelerometer";
	public static final String TYPE_LINEARACCELEROMETER = "Linear Acceleration";
	public static final String TYPE_ORIENTATION = "Orientation";
	public static final String TYPE_MAGNETIC = "Magnetic";
	public static final String TYPE_GYRO = "Gyroscope";
	public static final String TYPE_LIGHT = "Light";
	
	public static final String[] SENSOR_TYPES = {
		TYPE_ACCELEROMETER,
		TYPE_LINEARACCELEROMETER,
		TYPE_ORIENTATION,
		TYPE_MAGNETIC,
		TYPE_GYRO,
		TYPE_LIGHT
	};	
	
	public static SensorLog[] fetchLogsForSensor(String sensorType) throws StorageErrorException {
		// Open output directory
		File dir = getSensorOutputDirectory();
		
		// Filter results
		String logs[] = dir.list(new SensorTypeFilter(sensorType));

		// Build results list
		String path = dir.getPath();
		SensorLog results[] = new SensorLog[logs.length];
		for (int i = 0; i < logs.length; i++) {
			results[i] = new SensorLog(path, logs[i]);
		}
		
		return results;
	}
	
	private static File getSensorOutputDirectory() throws StorageErrorException {
		// See if external storage is present
		String storageStatus = Environment.getExternalStorageState();
		if (!storageStatus.equals(Environment.MEDIA_MOUNTED)) {
			// If not present, launch exception
			throw new StorageErrorException("External storage state is: " + storageStatus);
		}
		
		// Ensure output dir exists
		File dir = Environment.getExternalStorageDirectory();
		return new File(dir, SensorOutputWriter.OUTPUT_DIRECTORY);
	}
	
	// ========= SubClass, used only to filter directory file listings ============
	private static class SensorTypeFilter implements FilenameFilter {
		private String sensorType;
		
		public SensorTypeFilter(String sensorType) {
			this.sensorType = sensorType;
		}
		
		//@Override
		public boolean accept(File dir, String filename) {
			return filename.endsWith(sensorType + ".txt");
		}
		
	}
	
}
