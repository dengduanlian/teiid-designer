/* Generated By:JJTree: Do not edit this line. SubquerySetCriteria.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=true,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=,NODE_EXTENDS=,NODE_FACTORY=TeiidNodeFactory,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package org.teiid.query.sql.lang;

import org.teiid.designer.query.sql.lang.ISubquerySetCriteria;
import org.teiid.designer.runtime.version.spi.ITeiidServerVersion;
import org.teiid.query.parser.LanguageVisitor;
import org.teiid.query.sql.symbol.Expression;

/**
 *
 */
public class SubquerySetCriteria extends AbstractSetCriteria
implements SubqueryContainer<QueryCommand>, ISubquerySetCriteria<Expression, LanguageVisitor, QueryCommand>{

    private QueryCommand command;

    private SubqueryHint subqueryHint = new SubqueryHint();

    /**
     * @param p
     * @param id
     */
    public SubquerySetCriteria(ITeiidServerVersion p, int id) {
        super(p, id);
    }

    /**
     * Set the subquery command (either a SELECT or a procedure execution).
     * @param command Command to execute to get the values for the criteria
     */
    @Override
    public void setCommand(QueryCommand command) {
        this.command = command;
    }

    /**
     * @return subquery hint
     */
    public SubqueryHint getSubqueryHint() {
        return subqueryHint;
    }
    
    /**
     * @param subqueryHint
     */
    public void setSubqueryHint(SubqueryHint subqueryHint) {
        this.subqueryHint = subqueryHint;
    }

    /**
     * Get the subquery command used to produce the values for this SetCriteria.
     * @return Command Command to execute
     */
    @Override
    public QueryCommand getCommand() {
        return this.command;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.command == null) ? 0 : this.command.hashCode());
        result = prime * result + ((this.subqueryHint == null) ? 0 : this.subqueryHint.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        SubquerySetCriteria other = (SubquerySetCriteria)obj;
        if (this.command == null) {
            if (other.command != null) return false;
        } else if (!this.command.equals(other.command)) return false;
        if (this.subqueryHint == null) {
            if (other.subqueryHint != null) return false;
        } else if (!this.subqueryHint.equals(other.subqueryHint)) return false;
        return true;
    }

    /** Accept the visitor. **/
    @Override
    public void acceptVisitor(LanguageVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public SubquerySetCriteria clone() {
        SubquerySetCriteria clone = new SubquerySetCriteria(getTeiidVersion(), this.id);

        if(getCommand() != null)
            clone.setCommand(getCommand().clone());
        if(getSubqueryHint() != null)
            clone.setSubqueryHint(getSubqueryHint().clone());
        if(getExpression() != null)
            clone.setExpression(getExpression().clone());
        clone.setNegated(isNegated());

        return clone;
    }

}
/* JavaCC - OriginalChecksum=089107c07c615436629d5cb75f0cd12d (do not edit this line) */