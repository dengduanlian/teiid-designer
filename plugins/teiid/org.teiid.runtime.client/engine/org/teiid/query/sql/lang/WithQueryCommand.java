/* Generated By:JJTree: Do not edit this line. WithQueryCommand.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=true,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package org.teiid.query.sql.lang;

import java.util.List;
import org.teiid.designer.query.sql.lang.IWithQueryCommand;
import org.teiid.query.parser.LanguageVisitor;
import org.teiid.query.parser.TeiidParser;
import org.teiid.query.sql.symbol.GroupSymbol;

/**
 *
 */
public class WithQueryCommand extends SimpleNode
    implements SubqueryContainer<QueryCommand>, IWithQueryCommand<LanguageVisitor, QueryCommand> {

    private GroupSymbol groupSymbol;

    private List<ElementSymbol> columns;

    private QueryCommand queryExpression;

    /**
     * @param p
     * @param id
     */
    public WithQueryCommand(TeiidParser p, int id) {
        super(p, id);
    }

    /**
     * @return group symbol
     */
    public GroupSymbol getGroupSymbol() {
        return groupSymbol;
    }

    /**
     * @param groupSymbol the groupSymbol to set
     */
    public void setGroupSymbol(GroupSymbol groupSymbol) {
        this.groupSymbol = groupSymbol;
    }

    /**
     * @return columns
     */
    public List<ElementSymbol> getColumns() {
        return columns;
    }

    /**
     * @param columns
     */
    public void setColumns(List<ElementSymbol> columns) {
        this.columns = columns;
    }

    /**
     * @return the queryExpression
     */
    public QueryCommand getQueryExpression() {
        return this.queryExpression;
    }

    /**
     * @param queryExpression the queryExpression to set
     */
    public void setQueryExpression(QueryCommand queryExpression) {
        this.queryExpression = queryExpression;
    }

    @Override
    public QueryCommand getCommand() {
        return getQueryExpression();
    }

    @Override
    public void setCommand(QueryCommand command) {
        setQueryExpression(command);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((this.columns == null) ? 0 : this.columns.hashCode());
        result = prime * result + ((this.groupSymbol == null) ? 0 : this.groupSymbol.hashCode());
        result = prime * result + ((this.queryExpression == null) ? 0 : this.queryExpression.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        WithQueryCommand other = (WithQueryCommand)obj;
        if (this.columns == null) {
            if (other.columns != null) return false;
        } else if (!this.columns.equals(other.columns)) return false;
        if (this.groupSymbol == null) {
            if (other.groupSymbol != null) return false;
        } else if (!this.groupSymbol.equals(other.groupSymbol)) return false;
        if (this.queryExpression == null) {
            if (other.queryExpression != null) return false;
        } else if (!this.queryExpression.equals(other.queryExpression)) return false;
        return true;
    }

    /** Accept the visitor. **/
    @Override
    public void acceptVisitor(LanguageVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public WithQueryCommand clone() {
        WithQueryCommand clone = new WithQueryCommand(this.parser, this.id);

        if(getColumns() != null)
            clone.setColumns(cloneList(getColumns()));
        if(getGroupSymbol() != null)
            clone.setGroupSymbol(getGroupSymbol().clone());
        if(getQueryExpression() != null)
            clone.setQueryExpression(getQueryExpression().clone());

        return clone;
    }

}
/* JavaCC - OriginalChecksum=78388b507c1605f75762901f62007b34 (do not edit this line) */
