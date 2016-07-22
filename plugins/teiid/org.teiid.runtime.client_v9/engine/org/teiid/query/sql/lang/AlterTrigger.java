/* Generated By:JJTree: Do not edit this line. Alter.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=true,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package org.teiid.query.sql.lang;

import org.teiid.designer.query.sql.lang.IAlterTrigger;
import org.teiid.metadata.Table.TriggerEvent;
import org.teiid.query.parser.LanguageVisitor;
import org.teiid.query.parser.TeiidParser;
import org.teiid.query.sql.proc.TriggerAction;
import org.teiid.query.sql.symbol.Expression;

/**
 *
 */
public class AlterTrigger extends Alter<TriggerAction> implements IAlterTrigger<Expression, LanguageVisitor> {

    private TriggerEvent event;

    private boolean create;

    private Boolean enabled;

    /**
     * @param p
     * @param id
     */
    public AlterTrigger(TeiidParser p, int id) {
        super(p, id);
    }

    @Override
    public int getType() {
        return TYPE_ALTER_TRIGGER;
    }

    /**
     * @return the event
     */
    public TriggerEvent getEvent() {
        return event;
    }

    /**
     * @param event the event to set
     */
    public void setEvent(TriggerEvent event) {
        this.event = event;
    }

    /**
     * @return the create
     */
    public boolean isCreate() {
        return create;
    }

    /**
     * @param create the create to set
     */
    public void setCreate(boolean create) {
        this.create = create;
    }

    /**
     * @return the enabled
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * @param enabled the enabled to set
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (this.create ? 1231 : 1237);
        result = prime * result + ((this.enabled == null) ? 0 : this.enabled.hashCode());
        result = prime * result + ((this.event == null) ? 0 : this.event.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        AlterTrigger other = (AlterTrigger)obj;
        if (this.create != other.create) return false;
        if (this.enabled == null) {
            if (other.enabled != null) return false;
        } else if (!this.enabled.equals(other.enabled)) return false;
        if (this.event != other.event) return false;
        return true;
    }

    /** Accept the visitor. **/
    @Override
    public void acceptVisitor(LanguageVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public AlterTrigger clone() {
        AlterTrigger clone = new AlterTrigger(this.parser, this.id);

        if(getDefinition() != null)
            clone.setDefinition(getDefinition().clone());
        if(getEvent() != null)
            clone.setEvent(getEvent());
        clone.setCreate(isCreate());
        clone.setEnabled(getEnabled());
        if(getTarget() != null)
            clone.setTarget(getTarget().clone());
        if(getSourceHint() != null)
            clone.setSourceHint(getSourceHint());
        if(getOption() != null)
            clone.setOption(getOption().clone());

        copyMetadataState(clone);

        return clone;
    }

}
/* JavaCC - OriginalChecksum=4c2a7e700d4af2b1569d4947a1d82223 (do not edit this line) */
