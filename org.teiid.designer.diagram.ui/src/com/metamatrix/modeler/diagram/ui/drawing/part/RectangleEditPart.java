/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package com.metamatrix.modeler.diagram.ui.drawing.part;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;

import com.metamatrix.modeler.diagram.ui.model.DiagramModelNode;

/**
 * RectangleEditPart
 */
public class RectangleEditPart  extends DrawingEditPart {

    /**
     * Construct an instance of DrawingEditPart.
     * 
     */
    public RectangleEditPart() {
        super();
    }

    /* (non-Javadoc)
     * @see org.eclipse.gef.editparts.AbstractGraphicalEditPart#createFigure()
     */
    @Override
    protected IFigure createFigure() {
        Point posn = new Point( ((DiagramModelNode)getModel()).getX(), ((DiagramModelNode)getModel()).getY());
        Figure newFigure = getFigureFactory().createFigure((DiagramModelNode)getModel());
        newFigure.setLocation(posn);
        return newFigure;
    }
}

