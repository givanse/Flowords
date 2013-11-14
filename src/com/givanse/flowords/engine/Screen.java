package com.givanse.flowords.engine;

public class Screen {

	/**
	 * Coordinates for eight possible movement directions within a square.
	 * This coordinates represent a square centered at the origin of
	 * a Cartesian plane (0, 0).
	 * 
	 */
	public static final byte[] BASE_COORDS = { 0,  1,                   /* Up */
		                                       1,  1,       
	                                           1,  0,                /* Right */
	                                           1, -1, 
	                                           0, -1,                 /* Down */
	                                          -1, -1, 
                                              -1,  0,                 /* Left */
                                              -1,  1 };
	
	public static final byte[] VERTICES_COORDS = { -1,  1,      /* North West */
		                                           -1, -1,      /* South West */
		                                            1,  1,      /* North East */
		                                            1, -1 };    /* South East */
	
	public static final int VERTICES_TOTAL = 4;
	public static final int EDGES_TOTAL = 4;
	public static final int DIRS_TOTAL = Screen.VERTICES_TOTAL + 
			                             Screen.EDGES_TOTAL;
}
