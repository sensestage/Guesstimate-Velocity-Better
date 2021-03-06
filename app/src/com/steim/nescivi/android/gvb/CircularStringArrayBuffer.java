package com.steim.nescivi.android.gvb;

public class CircularStringArrayBuffer {
	private int mSize, mNewestElement;
	private String mBuffer[][];
	
	public CircularStringArrayBuffer(int size) {
		if (size < 1) {
			throw new IllegalArgumentException();
		}
		
		// Create buffer
		mBuffer = new String[size][];
		
		// Initialize pointers
		mNewestElement = size - 1;
		mSize = 0;
	}
	
	public void add(String elem[]) {
		int victim = (mNewestElement + 1) % mBuffer.length;
		mBuffer[victim] = elem;
		mNewestElement = victim;
		
		if (mSize < mBuffer.length) {
			mSize++;
		}
		
	}
	
	public int getSize() {
		return mSize;
	}
	
	public String[][] getContents() {
		String result[][] = new String[mSize][];
		
		if (mSize == mBuffer.length) {
			int oldestElement = (mNewestElement + 1) % mBuffer.length;
			
			for (int i = 0; i < result.length; i++) {
				result[i] = mBuffer[oldestElement];
	
				if (++oldestElement == mBuffer.length)
					oldestElement = 0;
			}
		}
		else {
			//special case here: buffer is not filled yet (so just dump the buffer)
			for (int i = 0; i < result.length; i++) {
				result[i] = mBuffer[i];
			}
		}
		
		return result;
	}	
}
