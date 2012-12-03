/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.teiid.query.ui.sqleditor.component;

import org.teiid.designer.query.sql.lang.IFrom;

/**
 * The <code>FromDisplayNode</code> class is used to represent a Query's entire FROM clause.
 *
 * @since 8.0
 */
public class FromDisplayNode extends DisplayNode {

    // /////////////////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    // /////////////////////////////////////////////////////////////////////////

    /**
     * FromDisplayNode constructor
     * 
     * @param parentNode the parent DisplayNode of this.
     * @param from the query language object used to construct this display node.
     */
    public FromDisplayNode( DisplayNode parentNode,
                            IFrom from ) {
        this.parentNode = parentNode;
        this.languageObject = from;
    }

    // /////////////////////////////////////////////////////////////////////////
    // PUBLIC METHODS
    // /////////////////////////////////////////////////////////////////////////

    /**
     * From Clause supports Criteria
     */
    @Override
    public boolean supportsCriteria() {
        return false;
    }

    /**
     * From Clause supports Expressions
     */
    @Override
    public boolean supportsExpression() {
        return true;
    }

    /**
     * From Clause supports Groups
     */
    @Override
    public boolean supportsGroup() {
        return true;
    }

}
