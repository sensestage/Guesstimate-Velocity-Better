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
	
	float[][] getStats(){
		double vart;
		double[] stds = {0.0, 0.0, 0,0};
		double[] means = {0.0, 0.0, 0,0};
		float[][] stats = { {0.0f, 0.0f, 0.0f}, {0.0f, 0.0f, 0.0f} };
		/*
	    for ( int axis = 0; axis < 3; axis++ ){
	    	stats[0][axis] = (float) 0.0;
	    }
	    */
	    
	    // mean
	    for (int i = 0; i < this.mSize; i++){
	    	for ( int axis = 0; axis < 3; axis++ ){
	    		means[axis] += this.mBuffer[i][axis];
	    	}
	    }
	    for ( int axis = 0; axis < 3; axis++ ){
	    	means[axis] = means[axis] / this.mSize;
	    }

	    // standard deviation
	    // std = sqrt(mean( abs(x - x.mean())**2) )
	    for (int i = 0; i < this.mSize; i++){
	    	for ( int axis = 0; axis < 3; axis++ ){
	    		vart = this.mBuffer[i][axis] - means[axis];
	    		stds[axis] += vart*vart;
	    	}
	    }

	    for ( int axis = 0; axis < 3; axis++ ){
	    	stats[0][axis] = (float) means[axis];
	    	stats[1][axis] = (float) Math.sqrt( stds[axis] / this.mSize ); 
	    }

	    /*
	    //float delta;
		//int n = 0;
	    //float[] M2 = { (float) 0.0,(float) 0.0, (float) 0.0 };

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
	    */
	    return stats;
	}

}
