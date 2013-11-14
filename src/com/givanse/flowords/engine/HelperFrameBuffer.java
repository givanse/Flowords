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

import android.opengl.GLES20;

/**
 * Helper class for handling frame buffer objects.
 */
public final class HelperFrameBuffer {

	private int depthBufferHandle = -1;       /* Optional depth buffer handle */
	private int frameBufferHandle = -1;            /* The actual frame buffer */
	private int stencilBufferHandle = -1;   /* Optional stencil buffer handle */
	private int[] textureHandles = {};
	
	public void bindFrameBuffer() {
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, this.frameBufferHandle);
	}

	/**
	 * Bind certain texture into target texture. This method should be called
	 * only after call to bind().
	 * 
	 * @param textureIndex
	 *            Index of texture to bind.
	 */
	public void bindTexture(int textureIndex) {
		GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
									  GLES20.GL_COLOR_ATTACHMENT0, 
									  GLES20.GL_TEXTURE_2D,
									  this.textureHandles[textureIndex], 0);
	}

	/**
	 * Getter for texture ids.
	 * 
	 * @param index
	 *            Index of texture.
	 * @return Texture id.
	 */
	public int getTexture(int index) {
		return textureHandles[index];
	}

	/**
	 * Calls this.init(int, int, int, boolean, boolean) without render buffer 
	 * generations.
	 * 
	 * @param width
	 *            Width in pixels.
	 * @param height
	 *            Height in pixels.
	 * @param textureCount
	 *            Number of textures to generate.
	 */
	public void setTexturesPrefs(int width, int height, int textureCount) {
		this.setTexturesPrefs(width, height, textureCount, false, false);
	}

	/**
	 * Initializes FBO with given parameters. Width and height are used to
	 * generate textures out of which all are sized same to this FBO. If you
	 * give genRenderBuffer a value 'true', depth buffer will be generated also.
	 * 
	 * @param width
	 *            FBO width in pixels
	 * @param height
	 *            FBO height in pixels
	 * @param textureCount
	 *            Number of textures to generate
	 * @param genDepthBuffer
	 *            If true, depth buffer is allocated for this FBO @ param
	 *            genStencilBuffer If true, stencil buffer is allocated for this
	 *            FBO
	 */
	private void setTexturesPrefs(int width, int height, int textureCount,
			                      boolean genDepthBuffer, 
			                      boolean genStencilBuffer) {

		this.reset(); // Just in case.

		/* Generate the frame buffer */
		int handles[] = { 0 };
		GLES20.glGenFramebuffers(handles.length, handles, 0);
		this.frameBufferHandle = handles[0];
		this.bindFrameBuffer();
		
		/* Generate textures */
		this.textureHandles = new int[textureCount];
		GLES20.glGenTextures(textureCount, this.textureHandles, 0);
		for (int texture : this.textureHandles) {
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
					               GLES20.GL_TEXTURE_WRAP_S, 
					               GLES20.GL_CLAMP_TO_EDGE);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
								   GLES20.GL_TEXTURE_WRAP_T, 
								   GLES20.GL_CLAMP_TO_EDGE);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
					               GLES20.GL_TEXTURE_MIN_FILTER, 
					               GLES20.GL_NEAREST);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
								   GLES20.GL_TEXTURE_MAG_FILTER, 
								   GLES20.GL_LINEAR);
			GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, 
								GLES20.GL_RGBA, width, height, 0, 
								GLES20.GL_RGBA,
								GLES20.GL_UNSIGNED_BYTE, null);
		}

		/* Generate depth buffer */
		if (genDepthBuffer) {
			GLES20.glGenRenderbuffers(handles.length, handles, 0);
			this.depthBufferHandle = handles[0];
			GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER,
									  depthBufferHandle);
			GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER,
										 GLES20.GL_DEPTH_COMPONENT16, 
										 width, height);
			GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER,
											 GLES20.GL_DEPTH_ATTACHMENT, 
											 GLES20.GL_RENDERBUFFER,
											 depthBufferHandle);
		}
		
		/* Generate stencil buffer */
		if (genStencilBuffer) {
			GLES20.glGenRenderbuffers(handles.length, handles, 0);
			this.stencilBufferHandle = handles[0];
			GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER,
									  stencilBufferHandle);
			GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER,
										 GLES20.GL_STENCIL_INDEX8, 
										 width, height);
			GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER,
											 GLES20.GL_STENCIL_ATTACHMENT, 
											 GLES20.GL_RENDERBUFFER,
											 stencilBufferHandle);
		}
	}

	/**
	 * Resets this FBO into its initial state, releasing all resources that were
	 * allocated during a call to init.
	 */
	public void reset() {
		int[] handles = { this.frameBufferHandle };
		GLES20.glDeleteFramebuffers(handles.length, handles, 0);
		
		handles[0] = this.depthBufferHandle;
		GLES20.glDeleteRenderbuffers(handles.length, handles, 0);
		
		handles[0] = this.stencilBufferHandle;
		GLES20.glDeleteRenderbuffers(handles.length, handles, 0);
		
		GLES20.glDeleteTextures(this.textureHandles.length, 
				                this.textureHandles, 0);
		this.frameBufferHandle = 
		this.depthBufferHandle = this.stencilBufferHandle = -1;
		this.textureHandles = new int[0];
	}

}
