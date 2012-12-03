/*
 * JBoss, Home of Professional Open Source.
*
* See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
*
* See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
*/
package org.teiid8.sql.impl.visitor;

import java.util.Collection;
import org.teiid.designer.query.sql.IFunctionCollectorVisitor;
import org.teiid.designer.query.sql.lang.ILanguageObject;
import org.teiid.designer.query.sql.symbol.IFunction;
import org.teiid.query.sql.LanguageObject;
import org.teiid.query.sql.symbol.Function;
import org.teiid.query.sql.visitor.FunctionCollectorVisitor;
import org.teiid8.sql.impl.SyntaxFactory;

/**
 *
 */
public class FunctionCollectorVisitorImpl implements IFunctionCollectorVisitor {

    private final SyntaxFactory factory = new SyntaxFactory();
    
    @Override
    public Collection<IFunction> getFunctions(ILanguageObject obj,
                                              boolean removeDuplicates,
                                              boolean deep) {
        LanguageObject languageObject = factory.convert(obj);
        Collection<Function> functions = FunctionCollectorVisitor.getFunctions(languageObject, removeDuplicates, deep);
        return factory.wrap(functions);
    }

}
