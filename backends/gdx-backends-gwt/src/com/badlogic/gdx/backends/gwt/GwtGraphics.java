/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.badlogic.gdx.backends.gwt;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.dom.client.CanvasElement;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.webgl.client.WebGLContextAttributes;
import com.google.gwt.webgl.client.WebGLRenderingContext;

public class GwtGraphics implements Graphics {
	CanvasElement canvas;
	WebGLRenderingContext context;
	GL20 gl;
	String extensions;
	float fps = 0;
	long lastTimeStamp = System.currentTimeMillis();
	long frameId = -1;
	float deltaTime = 0;
	float time = 0;
	int frames;
	GwtApplicationConfiguration config;

	public GwtGraphics (Panel root, GwtApplicationConfiguration config) {
		Canvas canvasWidget = Canvas.createIfSupported();
		if (canvasWidget == null) throw new GdxRuntimeException("Canvas not supported");
		canvas = canvasWidget.getCanvasElement();
		root.add(canvasWidget);
		canvas.setWidth(config.width);
		canvas.setHeight(config.height);
		this.config = config;

		WebGLContextAttributes attributes = WebGLContextAttributes.create();
		attributes.setAntialias(config.antialiasing);
		attributes.setStencil(config.stencil);
		attributes.setAlpha(config.alpha);
		attributes.setPremultipliedAlpha(config.premultipliedAlpha);
		attributes.setPreserveDrawingBuffer(config.preserveDrawingBuffer);

		context = WebGLRenderingContext.getContext(canvas, attributes);
		context.viewport(0, 0, config.width, config.height);
		this.gl = config.useDebugGL ? new GwtGL20Debug(context) : new GwtGL20(context);
	}

	public WebGLRenderingContext getContext () {
		return context;
	}

	@Override
	public GL20 getGL20 () {
		return gl;
	}

	@Override
	public int getWidth () {
		return canvas.getWidth();
	}

	@Override
	public int getHeight () {
		return canvas.getHeight();
	}
	
	@Override
	public int getBackBufferWidth () {
		return canvas.getWidth();
	}

	@Override
	public int getBackBufferHeight () {
		return canvas.getHeight();
	}

	@Override
	public long getFrameId () {
		return frameId;
	}

	@Override
	public float getDeltaTime () {
		return deltaTime;
	}

	@Override
	public int getFramesPerSecond () {
		return (int)fps;
	}

	@Override
	public GraphicsType getType () {
		return GraphicsType.WebGL;
	}

	@Override
	public float getPpiX () {
		return 96;
	}

	@Override
	public float getPpiY () {
		return 96;
	}

	@Override
	public float getPpcX () {
		return 96 / 2.54f;
	}

	@Override
	public float getPpcY () {
		return 96 / 2.54f;
	}

	@Override
	public boolean supportsDisplayModeChange () {
		return supportsFullscreenJSNI();
	}

	private native boolean supportsFullscreenJSNI () /*-{
		if ("fullscreenEnabled" in $doc) {
			return $doc.fullscreenEnabled;
		}
		if ("webkitFullscreenEnabled" in $doc) {
			return $doc.webkitFullscreenEnabled;
		}
		if ("mozFullScreenEnabled" in $doc) {
			return $doc.mozFullScreenEnabled;
		}
		if ("msFullscreenEnabled" in $doc) {
			return $doc.msFullscreenEnabled;
		}
		return false;
	}-*/;

	@Override
	public DisplayMode[] getDisplayModes () {
		return new DisplayMode[] {new DisplayMode(getScreenWidthJSNI(), getScreenHeightJSNI(), 60, 8) {}};
	}

	private native int getScreenWidthJSNI () /*-{
		return $wnd.screen.width;
	}-*/;

	private native int getScreenHeightJSNI () /*-{
		return $wnd.screen.height;
	}-*/;

	private native boolean isFullscreenJSNI () /*-{
		// Standards compliant check for fullscreen
		if ("fullscreenElement" in $doc) {
			return $doc.fullscreenElement != null;
		}
		// Vendor prefixed versions of standard check
		if ("msFullscreenElement" in $doc) {
			return $doc.msFullscreenElement != null;
		}
		if ("webkitFullscreenElement" in $doc) {
			return $doc.webkitFullscreenElement != null;
		}
		if ("mozFullScreenElement" in $doc) { // Yes, with a capital 'S'
			return $doc.mozFullScreenElement != null;
		}
		// Older, non-standard ways of checking for fullscreen
		if ("webkitIsFullScreen" in $doc) {
			return $doc.webkitIsFullScreen;
		}
		if ("mozFullScreen" in $doc) {
			return $doc.mozFullScreen;
		}
		return false
	}-*/;

	private void fullscreenChanged () {
		if (!isFullscreen()) {
			canvas.setWidth(config.width);
			canvas.setHeight(config.height);
		}
	}

	private native boolean setFullscreenJSNI (GwtGraphics graphics, CanvasElement element) /*-{
		// Attempt to use the non-prefixed standard API (https://fullscreen.spec.whatwg.org)
		if (element.requestFullscreen) {
			element.width = $wnd.screen.width;
			element.height = $wnd.screen.height;
			element.requestFullscreen();
			$doc
					.addEventListener(
							"fullscreenchange",
							function() {
								graphics.@com.badlogic.gdx.backends.gwt.GwtGraphics::fullscreenChanged()();
							}, false);
			return true;
		}
		// Attempt to the vendor specific variants of the API
		if (element.webkitRequestFullScreen) {
			element.width = $wnd.screen.width;
			element.height = $wnd.screen.height;
			element.webkitRequestFullScreen(Element.ALLOW_KEYBOARD_INPUT);
			$doc
					.addEventListener(
							"webkitfullscreenchange",
							function() {
								graphics.@com.badlogic.gdx.backends.gwt.GwtGraphics::fullscreenChanged()();
							}, false);
			return true;
		}
		if (element.mozRequestFullScreen) {
			element.width = $wnd.screen.width;
			element.height = $wnd.screen.height;
			element.mozRequestFullScreen();
			$doc
					.addEventListener(
							"mozfullscreenchange",
							function() {
								graphics.@com.badlogic.gdx.backends.gwt.GwtGraphics::fullscreenChanged()();
							}, false);
			return true;
		}
		if (element.msRequestFullscreen) {
			element.width = $wnd.screen.width;
			element.height = $wnd.screen.height;
			element.msRequestFullscreen();
			$doc
					.addEventListener(
							"msfullscreenchange",
							function() {
								graphics.@com.badlogic.gdx.backends.gwt.GwtGraphics::fullscreenChanged()();
							}, false);
			return true;
		}

		return false;
	}-*/;

	private native void exitFullscreen () /*-{
		if ($doc.exitFullscreen)
			$doc.exitFullscreen();
		if ($doc.msExitFullscreen)
			$doc.msExitFullscreen();
		if ($doc.webkitExitFullscreen)
			$doc.webkitExitFullscreen();
		if ($doc.mozExitFullscreen)
			$doc.mozExitFullscreen();
		if ($doc.webkitCancelFullScreen) // Old WebKit
			$doc.webkitCancelFullScreen();
	}-*/;

	@Override
	public DisplayMode getDisplayMode () {
		return new DisplayMode(getScreenWidthJSNI(), getScreenHeightJSNI(), 60, 8) {};
	}

	@Override
	public boolean setFullscreenMode (DisplayMode displayMode) {
		if (displayMode.width != getScreenWidthJSNI() && displayMode.height != getScreenHeightJSNI()) return false;
		return setFullscreenJSNI(this, canvas);
	}

	@Override
	public boolean setWindowedMode (int width, int height) {		
		if (isFullscreenJSNI()) exitFullscreen();
		canvas.setWidth(width);
		canvas.setHeight(height);
		return true;
	}
	

	@Override
	public Monitor getPrimaryMonitor () {
		return new GwtMonitor(0, 0, "Primary Monitor");
	}

	@Override
	public Monitor getMonitor () {
		return getPrimaryMonitor();
	}

	@Override
	public Monitor[] getMonitors () {
		return new Monitor[] { getPrimaryMonitor() };
	}

	@Override
	public DisplayMode[] getDisplayModes (Monitor monitor) {
		return getDisplayModes();
	}

	@Override
	public DisplayMode getDisplayMode (Monitor monitor) {
		return getDisplayMode();
	}

	@Override
	public BufferFormat getBufferFormat () {
		return new BufferFormat(8, 8, 8, 0, 16, config.stencil ? 8 : 0, 0, false);
	}

	@Override
	public boolean supportsExtension (String extension) {
		if (extensions == null) extensions = Gdx.gl.glGetString(GL20.GL_EXTENSIONS);
		return extensions.contains(extension);
	}

	public void update () {
		long currTimeStamp = System.currentTimeMillis();
		deltaTime = (currTimeStamp - lastTimeStamp) / 1000.0f;
		lastTimeStamp = currTimeStamp;
		time += deltaTime;
		frames++;
		if (time > 1) {
			this.fps = frames;
			time = 0;
			frames = 0;
		}
	}

	@Override
	public void setTitle (String title) {
	}

	@Override
	public void setVSync (boolean vsync) {
	}

	@Override
	public float getDensity () {
		return 96.0f / 160;
	}

	@Override
	public void setContinuousRendering (boolean isContinuous) {
	}

	@Override
	public boolean isContinuousRendering () {
		return true;
	}

	@Override
	public void requestRendering () {
	}

	@Override
	public float getRawDeltaTime () {
		return getDeltaTime();
	}

	@Override
	public boolean isFullscreen () {
		return isFullscreenJSNI();
	}

	@Override
	public boolean isGL30Available () {
		return false;
	}

	@Override
	public GL30 getGL30 () {
		return null;
	}

	@Override
	public Cursor newCursor (Pixmap pixmap, int xHotspot, int yHotspot) {
		return new GwtCursor(pixmap, xHotspot, yHotspot);
	}

	@Override
	public void setCursor (Cursor cursor) {
		if (cursor == null) {
			GwtCursor.resetCursor();
		} else {
			cursor.setSystemCursor();
		}
	}
	
	static class GwtMonitor extends Monitor {
		protected GwtMonitor (int virtualX, int virtualY, String name) {
			super(virtualX, virtualY, name);
		}
	}
}
