/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.teiid.designer.ui.editors;

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.teiid.designer.ui.UiPlugin;
import org.teiid.designer.ui.common.actions.ActionService;
import org.teiid.designer.ui.common.actions.GlobalActionsMap;

/**
 * Intended as the supertype for all {@link org.teiid.designer.ui.editors.ModelEditorPage}
 * action contributors.
 * @since 8.0
 */
public abstract class AbstractModelEditorPageActionBarContributor
extends EditorActionBarContributor
implements IMenuListener {
    //============================================================================================================================
    // Variables
    
    private ModelEditorPage editorPg;
    
    protected IActionBars actionBars;
    
    private IWorkbenchPage page;
    
    //============================================================================================================================
    // Constructors
    
    public AbstractModelEditorPageActionBarContributor(ModelEditorPage theEditorPage) {
        editorPg = theEditorPage;
    }
    
    //============================================================================================================================
	// Methods
    
    /**
     * Offers the editor page a chance to contribute actions which will be made available to context menus
     * from other model views. Subclasses must implement inorder to add to the menu.
     * <p>
     * An example implemention would look like this:<br>
     * <code>theMenuMgr.insertAfter(IModelerActionConstants.ContextMenu.ADDITIONS, new MyAction());</code>
     * @param theMenuMgr the context menu being contributed to
     */
    public void contributeExportedActions(IMenuManager theMenuMgr) {
    }
    
    /**
     *  
     * @see org.teiid.designer.ui.editors.IEditorActionExporter#getAdditionalModelingActions(org.eclipse.jface.viewers.ISelection)
     * @since 5.0
     */
    public List<IAction> getAdditionalModelingActions( ISelection selection ) {
        return Collections.EMPTY_LIST;
    }
    
    /**
     * Creates the associated {@link ModelEditorPage}s context menu. Subclasses should register the context
     * menu and hook up the menu listening.
     */
    public abstract void createContextMenu();
    
    /**
     * A helper method for subclasses. Creates the menu, registers it, and hooks up menu listening.
     * @param theMenuId the menu identifier
     * @param theControl the <code>Control</code> where the context menu will be a popup
     */
    protected IMenuManager createContextMenu(String theMenuId,
                                     Control theControl) {
        // create menu
        // jh fix: 2 arg ctor for MenuManager is: MenuManager(text, id)
        //         1 arg ctor only sets the text, not the id
        MenuManager mgr = new MenuManager(theMenuId, theMenuId);
        
        mgr.setRemoveAllWhenShown(true);
        Menu contextMenu = mgr.createContextMenu(theControl);
        
        theControl.setMenu(contextMenu);

        // wire up the listening
        mgr.addMenuListener(this);

        // register context menu so that other plugins can contribute
        ModelEditorPage page = getEditorPage();
        page.getSite().registerContextMenu(theMenuId, mgr, page.getModelObjectSelectionProvider());
        
        // Memory leak Defect 22290 requires that we unwire this class as a menu listener. So the Subclass needs to keep
        // track of the manager to do this, so we are returning it now.
        return mgr;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.part.EditorActionBarContributor#getActionBars()
     */
    @Override
    public IActionBars getActionBars() {
        return actionBars;
    }

    /**
     * Gets the <code>ActionService</code> associated with this contributor. Subclasses must override
     * if the service is not the <code>ModelerActionService</code>.
     * @return the requested service
     */
    public ActionService getActionService() {
        return UiPlugin.getDefault().getActionService(getEditorPage().getSite().getPage());
    }
    
    /**
     * Gets this contributor's {@link ModelEditorPage}.
     * @since 4.0
     */
    public ModelEditorPage getEditorPage() {
        return this.editorPg;
    }
    
    /**
     * Sets this contributor's {@link ModelEditorPage}.
     * @since 4.0
     */
    public void setEditorPage(ModelEditorPage newPage) {
        this.editorPg = newPage;
    }
    
    /**
     * Gets a <code>Map</code> of the global actions used by this contributor.
     * @return a map of actions or <code>null</code> if all default actions should be used
     */
    public GlobalActionsMap getGlobalActions() {
        return null;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.part.EditorActionBarContributor#init(org.eclipse.ui.IActionBars, org.eclipse.ui.IWorkbenchPage)
     */
    @Override
    public void init(IActionBars theActionBars,
                     IWorkbenchPage thePage) {
        // !!!!! DON'T CALL super.init(bars, page);
        // !!!!! super creates new coolbar contribution items for each editor and each editor has one toolbar.
        // !!!!! since ModelEditorActionContributor has already called super for this editor, don't call

        actionBars = theActionBars;
        page = thePage;

        // now do work that super normally does
        contributeToMenu(actionBars.getMenuManager());
        contributeToToolBar(actionBars.getToolBarManager());        
        contributeToStatusLine(actionBars.getStatusLineManager());
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.part.EditorActionBarContributor#getPage()
     */
    @Override
    public IWorkbenchPage getPage() {
        return page;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.action.IMenuListener#menuAboutToShow(org.eclipse.jface.action.IMenuManager)
     */
    @Override
	public void menuAboutToShow(IMenuManager theMenuMgr) {
        // first put in common menu items and groups
        ActionService service = getActionService();
        ISelection selection = getEditorPage().getModelObjectSelectionProvider().getSelection();
        service.contributeToContextMenu(theMenuMgr, getGlobalActions(), selection);

        // if needed, subclass put custom items in now
    }

    /**
     * Called by the action contributor for the parent editor whenever the editor page associated with this action contributor is
     * activated.
     * @since 4.0
     */
    public abstract void pageActivated();
    
    /**
     * Called by the action contributor for the parent editor whenever the editor page associated with this action contributor is
     * deactivated.  This method is responsible for removing any actions contributed dynamically via {@link #pageActivated}.
     * @since 4.0
     */
    public abstract void pageDeactivated();

    /**
     * Calls {@link #pageActivated()} or {@link #pageDeactivated()} depending upon whether the specified editor page is different
     * or the same, respectively, as the current active editor page.
     * @param editor The active editor page.
	 * @see org.eclipse.ui.IEditorActionBarContributor#setActiveEditor(org.eclipse.ui.IEditorPart)
	 * @since 4.0
	 */
	@Override
    public final void setActiveEditor(final IEditorPart editor) {
	    if ( editor != null && editor instanceof ModelEditorPage ) {
		    if (editorPg == editor) {
		        pageActivated();
		    } else {
		        pageDeactivated();
		    }
	    }
	}
}
