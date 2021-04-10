/*
 * Copyright 2020 FormDev Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.formdev.flatlaf.ui;

import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultDesktopManager;
import javax.swing.DesktopManager;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JInternalFrame.JDesktopIcon;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicDesktopIconUI;
import javax.swing.plaf.basic.BasicDesktopPaneUI;

/**
 * Provides the Flat LaF UI delegate for {@link javax.swing.JDesktopPane}.
 *
 * <!-- BasicDesktopPaneUI -->
 *
 * @uiDefault Desktop.background					Color
 * @uiDefault Desktop.minOnScreenInsets				Insets
 *
 * @author Karl Tauber
 */
public class FlatDesktopPaneUI
	extends BasicDesktopPaneUI
{
	private ComponentListener componentListener;

	// list of iconified internal frames, which define the order of icons in dock
	protected List<JInternalFrame> iconifiedFrames;

	public static ComponentUI createUI( JComponent c ) {
		return new FlatDesktopPaneUI();
	}

	@Override
	public void uninstallUI( JComponent c ) {
		super.uninstallUI( c );

		iconifiedFrames = null;
	}

	@Override
	protected void installListeners() {
		super.installListeners();

		componentListener = new ComponentAdapter() {
			@Override
			public void componentResized( ComponentEvent e ) {
				layoutDock();
			}
		};
		desktop.addComponentListener( componentListener );
	}

	@Override
	protected void uninstallListeners() {
		super.uninstallListeners();

		desktop.removeComponentListener( componentListener );
		componentListener = null;
	}

	@Override
	protected void installDesktopManager() {
		// Check current installed desktop manager to avoid recursive call
		// with property change event (will fire a stack overflow).
		// Do not handle install if already installed.
		DesktopManager dm = desktop.getDesktopManager();
		if( dm instanceof FlatDesktopManager || dm instanceof FlatWrapperDesktopManager )
			return;

		desktopManager = (dm != null)
			? new FlatWrapperDesktopManager( dm )
			: new FlatDesktopManager();
		desktop.setDesktopManager( desktopManager );
	}

	@Override
	protected void uninstallDesktopManager() {
		// uninstall wrapper
		DesktopManager dm = desktop.getDesktopManager();
		if( dm instanceof FlatWrapperDesktopManager )
			desktop.setDesktopManager( ((FlatWrapperDesktopManager)dm).parent );

		super.uninstallDesktopManager();
	}

	protected void layoutDock() {
		if( iconifiedFrames == null || iconifiedFrames.isEmpty() )
			return;

		Dimension desktopSize = desktop.getSize();
		int x = 0;
		int y = desktopSize.height;
		int rowHeight = 0;

		for( JInternalFrame f : iconifiedFrames ) {
			JDesktopIcon icon = f.getDesktopIcon();
			Dimension iconSize = icon.getPreferredSize();

			if( x + iconSize.width > desktopSize.width ) {
				// new row
				x = 0;
				y -= rowHeight;
				rowHeight = 0;
			}

			icon.setLocation( x, y - iconSize.height );

			x += iconSize.width;
			rowHeight = Math.max( iconSize.height, rowHeight );
		}
	}

	protected void addToDock( JInternalFrame f ) {
		if( iconifiedFrames == null )
			iconifiedFrames = new ArrayList<>();

		if( !iconifiedFrames.contains( f ) )
			iconifiedFrames.add( f );
		layoutDock();

		((FlatDesktopIconUI)f.getDesktopIcon().getUI()).updateDockIcon();
	}

	protected void removeFromDock( JInternalFrame f ) {
		if( iconifiedFrames == null )
			return;

		iconifiedFrames.remove( f );
		layoutDock();
	}

	//---- class FlatDesktopManager -------------------------------------------

	private class FlatDesktopManager
		extends DefaultDesktopManager
		implements UIResource
	{
		@Override
		public void iconifyFrame( JInternalFrame f ) {
			super.iconifyFrame( f );
			addToDock( f );
		}

		@Override
		public void deiconifyFrame( JInternalFrame f ) {
			super.deiconifyFrame( f );
			removeFromDock( f );
		}

		@Override
		public void closeFrame( JInternalFrame f ) {
			super.closeFrame( f );
			removeFromDock( f );
		}
	}

	//---- class FlatWrapperDesktopManager ------------------------------------

	/**
	 * For already installed desktop manager to use the flat desktop manager features.
	 * <p>
	 * Although this class extends {@link DefaultDesktopManager}, it does not invoke
	 * any method of the superclass.
	 * All methods are delegated to parent desktop manager.
	 * <p>
	 * This class extends {@link DefaultDesktopManager}
	 * (instead of implementing {@link DesktopManager}),
	 * because otherwise {@link DesktopManager#iconifyFrame(JInternalFrame)}
	 * and {@link DesktopManager#deiconifyFrame(JInternalFrame)} are not invoked
	 * from {@link BasicDesktopIconUI#installUI(JComponent)}
	 * and {@link BasicDesktopIconUI#uninstallUI(JComponent)}
	 * when switching Laf.
	 */
	private class FlatWrapperDesktopManager
		extends DefaultDesktopManager
	{
		private final DesktopManager parent;

		private FlatWrapperDesktopManager( DesktopManager parent ) {
			this.parent = parent;
		}

		@Override
		public void openFrame( JInternalFrame f ) {
			parent.openFrame( f );
		}

		@Override
		public void closeFrame( JInternalFrame f ) {
			parent.closeFrame( f );
			removeFromDock( f );
		}

		@Override
		public void maximizeFrame( JInternalFrame f ) {
			parent.maximizeFrame( f );
		}

		@Override
		public void minimizeFrame( JInternalFrame f ) {
			parent.minimizeFrame( f );
		}

		@Override
		public void activateFrame( JInternalFrame f ) {
			parent.activateFrame( f );
		}

		@Override
		public void deactivateFrame( JInternalFrame f ) {
			parent.deactivateFrame( f );
		}

		@Override
		public void iconifyFrame( JInternalFrame f ) {
			parent.iconifyFrame( f );
			addToDock( f );
		}

		@Override
		public void deiconifyFrame( JInternalFrame f ) {
			parent.deiconifyFrame( f );
			removeFromDock( f );
		}

		@Override
		public void beginDraggingFrame( JComponent f ) {
			parent.beginDraggingFrame( f );
		}

		@Override
		public void dragFrame( JComponent f, int newX, int newY ) {
			parent.dragFrame( f, newX, newY );
		}

		@Override
		public void endDraggingFrame( JComponent f ) {
			parent.endDraggingFrame( f );
		}

		@Override
		public void beginResizingFrame( JComponent f, int direction ) {
			parent.beginResizingFrame( f, direction );
		}

		@Override
		public void resizeFrame( JComponent f, int newX, int newY, int newWidth, int newHeight ) {
			parent.resizeFrame( f, newX, newY, newWidth, newHeight );
		}

		@Override
		public void endResizingFrame( JComponent f ) {
			parent.endResizingFrame( f );
		}

		@Override
		public void setBoundsForFrame( JComponent f, int newX, int newY, int newWidth, int newHeight ) {
			parent.setBoundsForFrame( f, newX, newY, newWidth, newHeight );
		}
	}
}
