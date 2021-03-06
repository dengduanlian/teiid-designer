/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.teiid.designer.metamodels.xsd;

import java.util.Collection;


/** 
 * Used to capture user preferences to pass to the XSD Builder.
 *
 * @since 8.0
 */
public class XsdBuilderOptions {
    private static final String XSD_EXT = ".xsd"; //$NON-NLS-1$
    
    private final boolean genOutput;
    private final boolean genXml;
    private final boolean doFlat;
    private final Collection roots;
    private final String modelName;
    private final boolean genSql;
    private final boolean genInput;
    private final String inputModelName;
    private final boolean genWs;
    private final String wsModelName;
    private final String rootModelName;
    private String parentPath;
    
    
    public XsdBuilderOptions(final boolean genOutput, final boolean genXml, final boolean doFlat, final Collection roots, final String modelName, 
                             final boolean genSQL, final boolean genInput, final String inputModelName, final boolean genWs, 
                             final String wsModelName, final String rootModelName) {
        this.genOutput = genOutput;
        this.genXml = genXml;
        this.doFlat = doFlat;
        this.genSql = genSQL;
        this.roots = roots;
        this.genInput = genInput;
        this.genWs = genWs;
        this.rootModelName = rootModelName;
        
        if(modelName.endsWith(XSD_EXT) ) {
            this.modelName = modelName;
        }else {
            this.modelName = modelName + XSD_EXT; 
        }
        
        if(inputModelName.endsWith(XSD_EXT) ) {
            this.inputModelName = inputModelName;
        }else {
            this.inputModelName = inputModelName + XSD_EXT; 
        }
        
        this.wsModelName = wsModelName;
    }

    public boolean genSql() {
        return this.genSql;
    }
    
    public boolean genWs() {
        return this.genWs;
    }
    
    public boolean genInput() {
        return this.genInput;
    }
    
    public boolean genXml() {
        return this.genXml;
    }
    
    public boolean isFlat() {
        return this.doFlat;
    }
    
    public String getInputModelName() {
        return this.inputModelName;
    }
    
    public String getWsModelName() {
        return this.wsModelName;
    }
    
    public String getOutputModelName() {
        return this.modelName;
    }
    
    public boolean genOutput() {
        return this.genOutput;
    }
    
    public Collection getRoots() {
        return this.roots;
    }
    
    public String getRootModelName() {
        return this.rootModelName;
    }

    
    /** 
     * @return Returns the parentPath.
     * @since 4.3
     */
    public String getParentPath() {
        return this.parentPath;
    }

    
    /** 
     * @param parentPath The parentPath to set.
     * @since 4.3
     */
    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }
    
    
}
