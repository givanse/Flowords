package com.givanse.flowords.engine;

public class Screen {

	public static final float[] COORDS = { 0,  1,  /* Cartesian square   */
		                                   1,  1,  /* centered at (0, 0) */
	                                       1,  0, 
	                                       1, -1, 
	                                       0, -1, 
	                                      -1, -1, 
                                          -1,  0, 
                                          -1,  1 };
	
	public static final byte[] VERTEX_COORDS = { -1,  1,        /* North West */
		                                         -1, -1,        /* South West */
		                                          1,  1,        /* North East */
		                                          1, -1 };      /* South East */
	
}
