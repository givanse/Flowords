/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.givanse.flowords.engine;

import java.util.HashMap;
import android.opengl.GLES20;
import android.util.Log;

/**
 * Helper class for handling shaders.
 */
public final class HelperShader {

	private int mProgram = -1;                       /* Shader program handle */ 
	private final HashMap<String, Integer> /* Store uniform/attribute handles */
	                          shaderHandlesMap = new HashMap<String, Integer>();

	/**
	 * Get id for given handle name. This method checks for both attribute and
	 * uniform handles.
	 * 
	 * @param handleName
	 *            Name of handle.
	 * @return Id for given handle or -1 if none found.
	 */
	public int getAUHandleId(String handleName) {
		if (this.shaderHandlesMap.containsKey(handleName)) {
			return this.shaderHandlesMap.get(handleName);
		}
		
		int handle = GLES20.glGetAttribLocation(this.mProgram, handleName);
		if (handle == -1) {
			handle = GLES20.glGetUniformLocation(this.mProgram, handleName);
		}
		
		if (handle == -1) {
			// TODO: log disable this line.
			// One should never leave log messages but am not going to follow
			// this rule. This line comes handy if you see repeating 'not found'
			// messages on LogCat - usually for typos otherwise annoying to
			// spot from shader code.
			Log.d("GlslShader", "Could not get attrib location for " + handleName);
		} else {
			this.shaderHandlesMap.put(handleName, handle);
		}
		
		return handle;
	}

	/**
	 * Get array of ids with given names. Returned array is sized to given
	 * amount name elements.
	 * 
	 * @param names
	 *            List of handle names.
	 * @return array of handle ids.
	 */
	public int[] getHandlesIds(String... names) {
		int[] res = new int[names.length];
		for (int i = 0; i < names.length; ++i) {
			res[i] = getAUHandleId(names[i]);
		}
		return res;
	}

	/**
	 * Helper method for compiling a shader.
	 * 
	 * @param shaderType
	 *            Type of shader to compile
	 * @param shaderSource
	 *            String presentation for shader
	 * @return id for compiled shader
	 */
	private int compileShader(int shaderType, String shaderSource) {
		int shaderHandle = GLES20.glCreateShader(shaderType);
		if (shaderHandle != 0) {
			GLES20.glShaderSource(shaderHandle, shaderSource);
			GLES20.glCompileShader(shaderHandle);
			int[] compileStatus = new int[1];
			GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, 
								 compileStatus, 0);
			if (compileStatus[0] == 0) {
				String error = GLES20.glGetShaderInfoLog(shaderHandle);
				GLES20.glDeleteShader(shaderHandle);
				throw new RuntimeException(error);
			}
		} else if (shaderHandle == 0) {
		    throw new RuntimeException("Error creating vertex shader.");
		}
		return shaderHandle;
	}

	/**
	 * Compiles vertex and fragment shaders and links them into a program one
	 * can use for rendering. Once OpenGL context is lost and onSurfaceCreated()
	 * is called, there is no need to reset existing GlslShader objects but one
	 * can simply reload shader.
	 * 
	 * @param vertexSource
	 *            String presentation for vertex shader
	 * @param fragmentSource
	 *            String presentation for fragment shader
	 */
	public void setProgram(String vertexSource, String fragmentSource) {
		int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER,
				                         vertexSource);
		int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, 
				                           fragmentSource);
		
		int programHandle = GLES20.glCreateProgram();
		if (programHandle != 0) {
			GLES20.glAttachShader(programHandle, vertexShader);
			GLES20.glAttachShader(programHandle, fragmentShader);
			GLES20.glLinkProgram(programHandle);
			
			int[] linkStatus = new int[1];
			GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, 
								  linkStatus, 0);
			if (linkStatus[0] != GLES20.GL_TRUE) {
				String error = GLES20.glGetProgramInfoLog(programHandle);
				GLES20.glDeleteProgram(programHandle);
				throw new RuntimeException(error);
			}
		} else if (programHandle == 0) {
		    throw new RuntimeException("Error creating program.");
		}
		
		this.mProgram = programHandle;
		this.shaderHandlesMap.clear();
	}

	/**
	 * Activates this shader program.
	 */
	public void useProgram() {
		GLES20.glUseProgram(this.mProgram);
	}

}
