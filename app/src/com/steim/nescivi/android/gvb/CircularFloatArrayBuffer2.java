package com.steim.nescivi.android.gvb;

import java.lang.Math;

public class CircularFloatArrayBuffer2 {
	private int mSize, mNewestElement;
	private double mBuffer[][];
	private int mDim;
	
	public CircularFloatArrayBuffer2(int dim, int size) {
		if (size < 1) {
			throw new IllegalArgumentException();
		}
		if (dim < 1) {
			throw new IllegalArgumentException();
		}
		
		// Create buffer
		mBuffer = new double[dim][size];
		
		// Initialize pointers
		mNewestElement = size - 1;
		mSize = 0;
		mDim = dim;
	}
	
	public void add(float elem[]) {
		int victim = (mNewestElement + 1) % mBuffer[0].length;
		
		for ( int i=0; i<mDim; i++ ){
			mBuffer[i][victim] = elem[i];
		}
		
		mNewestElement = victim;
		
		if (mSize < mBuffer[0].length) {
			mSize++;
		}
		
	}
	
	public int getSize() {
		return mSize;
	}

	public int getDim() {
		return mDim;
	}

	public float[][] getContents() {
		float result[][] = new float[mDim][mSize];
		
		if (mSize == mBuffer[0].length) {
			int oldestElement = (mNewestElement + 1) % mBuffer[0].length;
			
			for ( int j=0; j<mDim; j++ ){
				for (int i = 0; i < result[j].length; i++) {
					result[j][i] = (float) mBuffer[j][oldestElement];
	
					if (++oldestElement == mBuffer.length)
						oldestElement = 0;
				}
			}
		}
		else {
			for ( int j=0; j<mDim; j++ ){
				//	special case here: buffer is not filled yet (so just dump the buffer)
				for (int i = 0; i < result[j].length; i++) {
					result[j][i] = (float) mBuffer[j][i];
				}
			}
		}
		
		return result;
	}
	
	public static double sum(double[] a) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i];
        }
        return sum;
    }
	
	public int getLastElement(){
		return mNewestElement;
	}
	
	public float[][] getStats(){
		double vart;
		double[] stds = {0.0, 0.0, 0,0};
		double[] means = {0.0, 0.0, 0,0};
		float[][] stats = { {0.0f, 0.0f, 0.0f}, {0.0f, 0.0f, 0.0f} };
		
	    // mean
		if ( this.mSize > 1 ){
			for ( int axis = 0; axis < 3; axis++ ){
				means[axis] = sum( this.mBuffer[axis] ) / this.mSize;			
			}		
			for ( int axis = 0; axis < 3; axis++ ){
				for (int i = 0; i < this.mSize; i++) {
					vart = this.mBuffer[axis][i] - means[axis];
					stds[axis] += vart * vart;
				}
				stds[axis] = stds[axis] / (this.mSize-1);
			}
		}

	    for ( int axis = 0; axis < 3; axis++ ){
	    	stats[0][axis] = (float) means[axis];
	    	stats[1][axis] = (float) Math.sqrt( stds[axis] ); 
	    }

		/*
	    for (int i = 0; i < this.mSize; i++){
	    	
	    		means[axis] += this.mBuffer[i][axis];
	    	}
	    }
	    for ( int axis = 0; axis < 3; axis++ ){
	    	means[axis] = means[axis] / (float) this.mSize;
	    }
	    */

	    // standard deviation
	    // std = sqrt(mean( abs(x - x.mean())**2) )
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
