/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.teiid.designer.modelgenerator.wsdl.model.impl;

import org.teiid.designer.modelgenerator.wsdl.model.Binding;
import org.teiid.designer.modelgenerator.wsdl.model.Port;
import org.teiid.designer.modelgenerator.wsdl.model.Service;

/**
 * @since 8.0
 */
public class PortImpl extends WSDLElementImpl implements Port {

	private Service m_parent;
	private Binding m_binding;
	private String m_locationURI;
	private String namespaceURI;
	private String bindingTypeURI;

	public PortImpl(Service parent) {
		m_parent = parent;
	}

	@Override
	public Binding getBinding() {
		// defensive copy of bindings
		return (Binding) m_binding.copy();
	}

	@Override
	public void setBinding(Binding binding) {
		m_binding = binding;
	}

	@Override
	public Service getService() {
		return m_parent;
	}

	@Override
	public void setLocationURI(String uri) {
		m_locationURI = uri;
	}

	@Override
	public String getLocationURI() {
		return m_locationURI;
	}

	@Override
	public Port copy() {
		PortImpl impl = new PortImpl(getService());
		impl.setName(getName());
		impl.setId(getId());
		impl.setBinding(getBinding());
		impl.setBindingTypeURI(getBindingTypeURI());
		impl.setNamespaceURI(getNamespaceURI());
		impl.setLocationURI(getLocationURI());
		return impl;
	}

	@Override
	public String toString() {
		StringBuffer buff = new StringBuffer();
		buff.append("<port name='"); //$NON-NLS-1$
		buff.append(getName());
		buff.append("' id='"); //$NON-NLS-1$
		buff.append(getId());
		buff.append("'>"); //$NON-NLS-1$
		buff.append(m_binding.toString());
		buff.append("</port>"); //$NON-NLS-1$
		return buff.toString();
	}

	@Override
	public String getNamespaceURI() {
		return namespaceURI;
	}

	public void setNamespaceURI(String namespaceURI) {
		this.namespaceURI = namespaceURI;
	}

	@Override
	public void setBindingTypeURI(String bindingTypeURI) {
		this.bindingTypeURI = bindingTypeURI;
	}

	@Override
	public String getBindingTypeURI() {
		return this.bindingTypeURI;
	}

	@Override
	public String getBindingType() {

		if (Port.HTTP_TRANSPORT_URI.equals(this.bindingTypeURI)) {
			return Port.HTTP;
		} else {
			if (Port.SOAP12_TRANSPORT_URI.equals(this.bindingTypeURI)) {
				return Port.SOAP12;
			}
		}
		return Port.SOAP11;
	}
}