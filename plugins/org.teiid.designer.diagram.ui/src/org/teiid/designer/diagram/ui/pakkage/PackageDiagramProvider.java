/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.teiid.designer.diagram.ui.pakkage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.ecore.EObject;
import org.teiid.designer.core.ModelerCore;
import org.teiid.designer.core.metamodel.aspect.MetamodelAspect;
import org.teiid.designer.core.metamodel.aspect.uml.UmlPackage;
import org.teiid.designer.core.workspace.ModelDiagrams;
import org.teiid.designer.core.workspace.ModelResource;
import org.teiid.designer.core.workspace.ModelWorkspaceException;
import org.teiid.designer.diagram.ui.DiagramUiConstants;
import org.teiid.designer.diagram.ui.DiagramUiPlugin;
import org.teiid.designer.diagram.ui.PluginConstants;
import org.teiid.designer.diagram.ui.util.DiagramUiUtilities;
import org.teiid.designer.metamodels.diagram.Diagram;
import org.teiid.designer.ui.util.DiagramProxy;
import org.teiid.designer.ui.viewsupport.ModelUtilities;


/**
 * <p>
 * PackageDiagramProvider is the specific Package Diagram Provider for basic package diagram model objects inside ModelResources.
 * </p>
 * <p>
 * PackageDiagramProvider implements IPackageDiagramProvider because this interface has the right methods to locate the package
 * diagram for an EObject or ModelResource
 * </p>
 *
 * @since 8.0
 */

public class PackageDiagramProvider implements IPackageDiagramProvider {

    /**
     * Construct an instance of MappingDiagramProvider.
     */
    public PackageDiagramProvider() {
        super();
    }

    private Diagram createPackageDiagram( EObject target,
                                          ModelResource modelResource,
                                          boolean forceCreate ) {
        if (forceCreate) {
            Diagram pDiagram = null;
            // Wrap in transaction so it doesn't result in Significant Undoable
            boolean started = ModelerCore.startTxn(false, false, "Creating Package Diagram", this); //$NON-NLS-1$
            boolean succeeded = false;
            try {
                pDiagram = PackageDiagramUtil.createPackageDiagram(target, modelResource);
                succeeded = true;
            } finally {
                if (started) {
                    if (succeeded) {
                        ModelerCore.commitTxn();
                    } else {
                        ModelerCore.rollbackTxn();
                    }
                }
            }
            return pDiagram;
        }
        if (target == null) {
            try {
                target = modelResource.getModelAnnotation();
            } catch (ModelWorkspaceException err) {
                DiagramUiConstants.Util.log(err);
            }
        }
        return new DiagramProxy(target, PluginConstants.PACKAGE_DIAGRAM_TYPE_ID, modelResource);
    }

    /* (non-Javadoc)
     * @See org.teiid.designer.diagram.ui.pakkage.IPackageDiagramProvider#getPackageDiagram(org.teiid.designer.core.workspace.ModelResource, org.eclipse.emf.ecore.EObject)
     */
    @Override
	public Diagram getPackageDiagram( ModelResource modelResource,
                                      EObject eObject,
                                      boolean forceCreate ) {

        ModelDiagrams modelDiagrams = null;
        Diagram packageDiagram = null;

        if (modelResource != null && ModelUtilities.supportsDiagrams(modelResource)) {
            // if eObject == null, then it's a 'Model' node, then we don't ask for a contributed package diagram.

            try {
                modelDiagrams = modelResource.getModelDiagrams();

                if (modelDiagrams != null && eObject != null) {
                    List diagramList = new ArrayList(modelDiagrams.getDiagrams(eObject));
                    Iterator iter = diagramList.iterator();
                    Diagram nextDiagram = null;
                    while (iter.hasNext() && packageDiagram == null) {
                        nextDiagram = (Diagram)iter.next();
                        if (nextDiagram.getType() != null
                            && nextDiagram.getType().equals(PluginConstants.PACKAGE_DIAGRAM_TYPE_ID)) packageDiagram = nextDiagram;
                    }
                } else if (modelDiagrams != null) {
                    // EObject == null, so it's supposed to be the package diagram under a model resource only!.
                    List diagramList = new ArrayList(modelDiagrams.getDiagrams(null));
                    if (!diagramList.isEmpty()) {
                        Iterator iter = diagramList.iterator();
                        Diagram nextDiagram = null;
                        while (iter.hasNext() && packageDiagram == null) {
                            nextDiagram = (Diagram)iter.next();
                            if (nextDiagram.getType() != null
                                && nextDiagram.getType().equals(PluginConstants.PACKAGE_DIAGRAM_TYPE_ID)) packageDiagram = nextDiagram;
                        }
                        //
                        // packageDiagram = (Diagram)diagramList.get(0);
                        // if( diagramList.size() > 1 ) {
                        // String warningMessage =
                        //                                " PackageDiagramProvider.getPackageDiagram(): More than one package diagram for resource. Should only be one. Resource = " //$NON-NLS-1$
                        // + modelResource.toString();
                        // DiagramUiConstants.Util.log(IStatus.WARNING, warningMessage);
                        // }
                    }
                }
            } catch (ModelWorkspaceException e) {
                if (!modelResource.hasErrors()) {
                    // Unexpected ...
                    String message = DiagramUiConstants.Util.getString("PackageDiagramContentProvider.getPackageDiagramError", modelResource.toString()); //$NON-NLS-1$
                    DiagramUiConstants.Util.log(IStatus.ERROR, e, message);
                }
            }

            if (packageDiagram == null) {
                // create one here.
                packageDiagram = createPackageDiagram(eObject, modelResource, forceCreate);
            }
        }

        return packageDiagram;
    }

    /* (non-Javadoc)
     * @See org.teiid.designer.diagram.ui.pakkage.IPackageDiagramProvider#getPackageDiagram(org.eclipse.emf.ecore.EObject)
     */
    @Override
	public Diagram getPackageDiagram( Object targetObject,
                                      boolean forceCreate ) {
        Diagram diagram = null;

        if (targetObject instanceof EObject) {
            EObject eObject = (EObject)targetObject;
            ModelResource modelResource = ModelUtilities.getModelResourceForModelObject(eObject);
            if (eObject.eContainer() != null) {
                if (eObject.eContainer() != null) {
                    EObject packageEObject = getPackage(eObject);
                    if (packageEObject != null && modelResource != null) {
                        diagram = getPackageDiagram(modelResource, packageEObject, forceCreate);
                    }
                }
            } else if (DiagramUiUtilities.isStandardUmlPackage(targetObject)) {
                // We have a package under a model, so it doesn't have a "eContainer"
                diagram = getPackageDiagram(modelResource, eObject, forceCreate);
            }
        }

        return diagram;
    }

    private EObject getPackage( EObject eObject ) {
        EObject packageEObject = null;
        Object parentObject = eObject.eContainer();
        if (parentObject instanceof EObject) {
            // Get aspect and see if it's a UmlPackage....
            MetamodelAspect aspect = DiagramUiPlugin.getDiagramAspectManager().getUmlAspect((EObject)parentObject);
            if (aspect != null && aspect instanceof UmlPackage) {
                packageEObject = (EObject)parentObject;
            } else {
                packageEObject = getPackage((EObject)parentObject);
            }
        } else {

        }

        return packageEObject;
    }

}
