/*******************************************************************************
 * Copyright (c) 2010 György Orosz.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     György Orosz - initial API and implementation
 ******************************************************************************/
package org.erlide.wrangler.refactoring.codeinspection;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.erlide.core.erlang.IErlElement;
import org.erlide.wrangler.refactoring.codeinspection.view.CodeInspectionResultsView;
import org.erlide.wrangler.refactoring.codeinspection.view.GraphImageView;

/**
 * Manages the displaying of the Graph View
 * 
 * @author Gyorgy Orosz
 * 
 */
public class CodeInspectionViewsManager {
	public static final String GRAPH_VIEW = org.erlide.wrangler.refactoring.codeinspection.view.GraphImageView.VIEW_ID;
	public static final String CODE_INSPECTION_VIEW = org.erlide.wrangler.refactoring.codeinspection.view.CodeInspectionResultsView.VIEW_ID;

	/**
	 * Shows the image in the graph view with the given title.
	 * 
	 * @param img
	 *            image
	 * @param title
	 *            view title
	 * @param dotFile
	 *            .dot file which is displayed
	 */
	static public void showDotImage(Image img, String title, File dotFile) {
		GraphImageView view = (GraphImageView) showView(GRAPH_VIEW);
		view.setViewTitle(title);
		view.setImage(img);
		view.setDotFile(dotFile);
	}

	/**
	 * Shows Erlang elements in a list view
	 * 
	 * @param title
	 *            view title
	 * @param e
	 *            Erlang elements
	 */
	static public void showErlElements(String title, ArrayList<IErlElement> e,
			String secId) {
		try {
			CodeInspectionResultsView v = (CodeInspectionResultsView) showView(
					CODE_INSPECTION_VIEW, secId);
			v.addElements(e);
			v.setViewTitle(title);
			v.refresh();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Shows a view.
	 * 
	 * @param viewId
	 *            view id, which is shown
	 * 
	 * @return view which is shown
	 */
	static public IViewPart showView(String viewId) {

		IWorkbench workbench = PlatformUI.getWorkbench();

		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		try {
			IViewPart view = window.getActivePage().showView(viewId);
			return view;
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Shows a an instance of a view
	 * 
	 * @param viewId
	 *            view id
	 * @param secondaryID
	 *            view secondary id, to handle multiple instances
	 * @return view object
	 */
	static public IViewPart showView(String viewId, String secondaryID) {
		IWorkbench workbench = PlatformUI.getWorkbench();

		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		try {
			IViewPart view = window.getActivePage().showView(viewId,
					secondaryID, IWorkbenchPage.VIEW_ACTIVATE);
			return view;
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Hides a view.
	 * 
	 * @param viewId
	 *            view, which will be hidden
	 */
	static public void hideView(String viewId) {
		hideView(viewId, null);
	}

	/**
	 * Hides an instance of a view
	 * 
	 * @param viewId
	 *            view id
	 * @param secondaryId
	 *            secondary id of a view instance
	 */
	static public void hideView(String viewId, String secondaryId) {
		IWorkbench workbench = PlatformUI.getWorkbench();

		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		IViewPart view;
		IViewReference viewr = window.getActivePage().findViewReference(viewId,
				secondaryId);
		if (viewr != null) {
			view = viewr.getView(false);
			if (view != null)
				window.getActivePage().hideView(view);
		}

	}
}
