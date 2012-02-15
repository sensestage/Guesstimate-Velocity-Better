package com.steim.nescivi.android.gvb;

import java.lang.Math;

public class CircularFloatArrayBuffer {
	private int mSize, mNewestElement;
	private float mBuffer[][];
	
	public CircularFloatArrayBuffer(int size) {
		if (size < 1) {
			throw new IllegalArgumentException();
		}
		
		// Create buffer
		mBuffer = new float[size][];
		
		// Initialize pointers
		mNewestElement = size - 1;
		mSize = 0;
	}
	
	public void add(float elem[]) {
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
	
	public float[][] getContents() {
		float result[][] = new float[mSize][];
		
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
	
	void getStats( float[][] stats ){
		float delta;
	    int n = 0;
	    //float[] mean = { (float) 0.0,(float) 0.0, (float) 0.0 };
	    float[] M2 = { (float) 0.0,(float) 0.0, (float) 0.0 };
	    //float[] variance;
	    
	    //   float stats[][] = new float[2][3];
	    
	    for ( int axis = 0; axis < 3; axis++ ){
	    	stats[0][axis] = (float) 0.0;
	    }
	    
	    for (int i = 0; i < this.mSize; i++){
    		n = n + 1;
	    	for ( int axis = 0; axis < 3; axis++ ){
	    		delta = this.mBuffer[i][axis] - stats[0][axis];
	    		stats[0][axis] = stats[0][axis] + delta/n;
	    		if ( n > 1 ){
	    		    M2[axis] = M2[axis] + delta*(this.mBuffer[i][axis] - stats[0][axis]);
	    		}
	    	}
	    }
	    for ( int axis = 0; axis < 3; axis++ ){
	    	stats[1][axis] = (float) Math.sqrt( M2[axis]/(n - 1) );
	    }
	    //return stats;
	}

}
