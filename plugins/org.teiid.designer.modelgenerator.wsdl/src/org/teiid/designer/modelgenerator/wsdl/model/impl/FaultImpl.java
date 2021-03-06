/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.teiid.designer.modelgenerator.wsdl.model.impl;


import org.teiid.designer.modelgenerator.wsdl.model.Fault;
import org.teiid.designer.modelgenerator.wsdl.model.Message;
import org.teiid.designer.modelgenerator.wsdl.model.Operation;

/**
 * @since 8.0
 */
public class FaultImpl extends WSDLElementImpl implements Fault {

	private Message m_message;
	private Operation m_operation;
	
	public FaultImpl(Operation oper) {
		m_operation = oper;
	}
	
	@Override
	public Operation getOperation() {
		return m_operation;
	}
	
	@Override
	public void setMessage(Message message) {
		m_message = message;
	}

	@Override
	public Message getMessage() {
		return m_message;
	}

	@Override
	public Fault copy() {
		FaultImpl fault = new FaultImpl(getOperation());
		fault.setMessage(getMessage());
		fault.setName(getName());
		fault.setId(getId());
		return fault;
	}
	
	@Override
    public String toString() {
		StringBuffer buff = new StringBuffer();
		buff.append("<fault name='"); //$NON-NLS-1$
		buff.append(getName());
		buff.append("' id='"); //$NON-NLS-1$
		buff.append(getId());
		buff.append("'>"); //$NON-NLS-1$
		buff.append(getMessage().toString());
		buff.append("</fault>"); //$NON-NLS-1$
		return buff.toString();
	}
}
