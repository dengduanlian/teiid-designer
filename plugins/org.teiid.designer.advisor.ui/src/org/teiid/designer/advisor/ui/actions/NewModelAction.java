/*
 * JBoss, Home of Professional Open Source.
*
* See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
*
* See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
*/
package org.teiid.designer.advisor.ui.actions;

import java.util.Properties;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.teiid.designer.advisor.ui.AdvisorUiConstants;
import org.teiid.designer.advisor.ui.AdvisorUiPlugin;
import org.teiid.designer.advisor.ui.Messages;
import org.teiid.designer.metamodels.core.ModelType;
import org.teiid.designer.ui.common.product.ProductCustomizerMgr;
import org.teiid.designer.ui.viewsupport.DesignerPropertiesUtil;
import org.teiid.designer.ui.viewsupport.IPropertiesContext;
import org.teiid.designer.ui.wizards.NewModelWizard;
import org.teiid.designer.ui.wizards.NewModelWizardInput;


/**
 * NewModelAction is a hook for the active help system to run the New Metadata Model wizard. The action is not exposed anywhere in
 * the Modeler ui.
 */
public final class NewModelAction extends Action implements AdvisorUiConstants {

    private NewModelWizardInput newModelInput;
    private Properties designerProperties;

    /**
     * Construct an instance of NewModelAction.
     */
    public NewModelAction() {
        super();
        setText("New Model Action"); //$NON-NLS-1$
        setToolTipText("New Model Action Tooltip"); //$NON-NLS-1$
        setImageDescriptor(AdvisorUiPlugin.getDefault().getImageDescriptor(Images.NEW_MODEL_ACTION));
    }
    
    /**
     * Construct an instance of NewModelAction.
     * 
     * @param modelType the model type
     * @param metamodelClass the metamodel class
     * @param builderType the builder type
     * @param properties the designer properties
     */
    public NewModelAction( ModelType modelType,
                           String metamodelClass,
                           String builderType,
                           Properties properties ) {
        this();
        setModelType(modelType);
        setMetamodelClass(metamodelClass);
        setBuilderType(builderType);
        this.designerProperties = properties;
    }

    /*
     * Defect 22363 - jh fix to Barry's previous fix:  Lazily creating the 'new model input'
     *   object ensures that it works in all cases.  It was default correctly for XML, but
     *   would not work correctly the second time you did a New Model View.
     */
    private NewModelWizardInput getNewModelInput() {
        if (newModelInput == null) {
            newModelInput = new NewModelWizardInput();
        }
        return newModelInput;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.action.IAction#run()
     */
    @Override
	public void run() {
        final IWorkbenchWindow iww = AdvisorUiPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
        boolean successful = false;
        try {
        	Properties props = this.designerProperties;
        	if( props == null ) {
        		props = new Properties();
        	}
            NewModelWizard wizard = new NewModelWizard(newModelInput, props);

            String viewId = ProductCustomizerMgr.getInstance().getProductCharacteristics().getPrimaryNavigationViewId();
            ISelection theSelection = iww.getSelectionService().getSelection(viewId);

            wizard.init(iww.getWorkbench(), (IStructuredSelection)theSelection);
            WizardDialog dialog = new WizardDialog(iww.getShell(), wizard);
            wizard.updateForProperties();
            
    		String openProjectStatus = DesignerPropertiesUtil.getProjectStatus(props);
    		if( openProjectStatus == null || !IPropertiesContext.NO_OPEN_PROJECT.equalsIgnoreCase(openProjectStatus) ){
	            int result = dialog.open();
	            if (result == Window.OK) {
	                successful = true;
	            }
    		} else {
    			return;
    		}
        } catch (Exception e) {
            AdvisorUiConstants.UTIL.log(IStatus.ERROR, e, e.getMessage());
            MessageDialog.openError(iww.getShell(), Messages.NewModelWizardErrorTitle, Messages.NewModelWizardErrorMessage);
        } finally {
            notifyResult(successful);
            this.newModelInput = null;
        }

    }

    /**
     * @param metamodelClass the metamodel class
     */
    public void setMetamodelClass( String metamodelClass ) {
        getNewModelInput().setMetamodelClass(metamodelClass);
    }

    /**
     * @param type the model type
     */
    public void setModelType( ModelType type ) {
        getNewModelInput().setModelType(type);
    }

    /**
     * @param type the builder type
     */
    public void setBuilderType( String type ) {
        getNewModelInput().setBuilderType(type);
    }

}
