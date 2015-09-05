package org.jboss.as.clustering.jgroups.subsystem;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.RequiredCapability;
import org.jboss.as.clustering.controller.SimpleAttribute;
import org.jboss.as.clustering.subsystem.AdditionalInitialization;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;

/**
* Base test case for testing management operations.
*
* @author Richard Achmatowicz (c) 2011 Red Hat Inc.
*/
public class OperationTestCaseBase extends AbstractSubsystemTest {

    static final String SUBSYSTEM_XML_FILE = JGroupsSchema.CURRENT.format("subsystem-jgroups-%d_%d.xml");

    public OperationTestCaseBase() {
        super(JGroupsExtension.SUBSYSTEM_NAME, new JGroupsExtension());
    }

    protected static ModelNode getSubsystemAddOperation(String defaultStack) {
        ModelNode operation = Util.createAddOperation(getSubsystemAddress());
        operation.get(JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_STACK.getDefinition().getName()).set(defaultStack);
        return operation;
    }

    protected static ModelNode getSubsystemReadOperation(Attribute attribute) {
        return Operations.createReadAttributeOperation(getSubsystemAddress(), attribute);
    }

    protected static ModelNode getSubsystemWriteOperation(Attribute attribute, String value) {
        return Operations.createWriteAttributeOperation(getSubsystemAddress(), attribute, new ModelNode(value));
    }

    protected static ModelNode getSubsystemRemoveOperation() {
        return Util.createRemoveOperation(getSubsystemAddress());
    }

    protected static ModelNode getProtocolStackAddOperation(String stackName) {
        return Util.createAddOperation(getProtocolStackAddress(stackName));
    }

    protected static ModelNode getProtocolStackAddOperationWithParameters(String stackName) {
        ModelNode[] operations = new ModelNode[] {
                getProtocolStackAddOperation(stackName),
                getTransportAddOperation(stackName, "UDP"),
                getProtocolAddOperation(stackName, "MPING"),
                getProtocolAddOperation(stackName, "pbcast.FLUSH"),
        };
        return Operations.createCompositeOperation(operations);
    }

    protected static ModelNode getProtocolStackRemoveOperation(String stackName) {
        return Util.createRemoveOperation(getProtocolStackAddress(stackName));
    }

    protected static ModelNode getTransportAddOperation(String stackName, String protocol) {
        return Util.createAddOperation(getTransportAddress(stackName, protocol));
    }

    protected static ModelNode getTransportAddOperationWithProperties(String stackName, String type) {
        ModelNode[] operations = new ModelNode[] {
                getTransportAddOperation(stackName, type),
                getProtocolPropertyAddOperation(stackName, type, "A", "a"),
                getProtocolPropertyAddOperation(stackName, type, "B", "b"),
        };
        return Operations.createCompositeOperation(operations);
    }

    protected static ModelNode getTransportRemoveOperation(String stackName, String type) {
        return Util.createRemoveOperation(getTransportAddress(stackName, type));
    }

    protected static ModelNode getTransportReadOperation(String stackName, String type, Attribute attribute) {
        return Operations.createReadAttributeOperation(getTransportAddress(stackName, type), attribute);
    }

    protected static ModelNode getTransportWriteOperation(String stackName, String type, Attribute attribute, String value) {
        return Operations.createWriteAttributeOperation(getTransportAddress(stackName, type), attribute, new ModelNode(value));
    }

    protected static ModelNode getTransportPropertyAddOperation(String stackName, String type, String propertyName, String propertyValue) {
        ModelNode operation = Util.createAddOperation(getTransportPropertyAddress(stackName, type, propertyName));
        operation.get(PropertyResourceDefinition.VALUE.getName()).set(propertyValue);
        return operation;
    }

    protected static ModelNode getTransportPropertyRemoveOperation(String stackName, String type, String propertyName) {
        return Util.createRemoveOperation(getTransportPropertyAddress(stackName, type, propertyName));
    }

    protected static ModelNode getTransportPropertyReadOperation(String stackName, String type, String propertyName) {
        return Operations.createReadAttributeOperation(getTransportPropertyAddress(stackName, type, propertyName), new SimpleAttribute(PropertyResourceDefinition.VALUE));
    }

    protected static ModelNode getTransportPropertyWriteOperation(String stackName, String type, String propertyName, String propertyValue) {
        return Operations.createWriteAttributeOperation(getTransportPropertyAddress(stackName, type, propertyName), new SimpleAttribute(PropertyResourceDefinition.VALUE), new ModelNode(propertyValue));
    }

    protected static ModelNode getTransportGetPropertyOperation(String stackName, String type, String propertyName) {
        return Operations.createMapGetOperation(getTransportAddress(stackName, type), ProtocolResourceDefinition.Attribute.PROPERTIES, propertyName);
    }

    protected static ModelNode getTransportPutPropertyOperation(String stackName, String type, String propertyName, String propertyValue) {
        return Operations.createMapPutOperation(getTransportAddress(stackName, type), ProtocolResourceDefinition.Attribute.PROPERTIES, propertyName, propertyValue);
    }

    protected static ModelNode getTransportRemovePropertyOperation(String stackName, String type, String propertyName) {
        return Operations.createMapRemoveOperation(getTransportAddress(stackName, type), ProtocolResourceDefinition.Attribute.PROPERTIES, propertyName);
    }

    protected static ModelNode getProtocolAddOperation(String stackName, String type) {
        return Util.createAddOperation(getProtocolAddress(stackName, type));
    }

    protected static ModelNode getProtocolAddOperationWithProperties(String stackName, String type) {
        ModelNode[] operations = new ModelNode[] {
                getProtocolAddOperation(stackName, type),
                getProtocolPropertyAddOperation(stackName, type, "A", "a"),
                getProtocolPropertyAddOperation(stackName, type, "B", "b"),
        };
        return Operations.createCompositeOperation(operations);
    }

    protected static ModelNode getProtocolReadOperation(String stackName, String protocolName, Attribute attribute) {
        return Operations.createReadAttributeOperation(getProtocolAddress(stackName, protocolName), attribute);
    }

    protected static ModelNode getProtocolWriteOperation(String stackName, String protocolName, Attribute attribute, String value) {
        return Operations.createWriteAttributeOperation(getProtocolAddress(stackName, protocolName), attribute, new ModelNode(value));
    }

    protected static ModelNode getProtocolPropertyAddOperation(String stackName, String protocolName, String propertyName, String propertyValue) {
        ModelNode operation = Util.createAddOperation(getProtocolPropertyAddress(stackName, protocolName, propertyName));
        operation.get(PropertyResourceDefinition.VALUE.getName()).set(propertyValue);
        return operation;
    }

    protected static ModelNode getProtocolPropertyRemoveOperation(String stackName, String protocolName, String propertyName) {
        return Util.createRemoveOperation(getProtocolPropertyAddress(stackName, protocolName, propertyName));
    }

    protected static ModelNode getProtocolPropertyReadOperation(String stackName, String protocolName, String propertyName) {
        return Operations.createReadAttributeOperation(getProtocolPropertyAddress(stackName, protocolName, propertyName), new SimpleAttribute(PropertyResourceDefinition.VALUE));
    }

    protected static ModelNode getProtocolPropertyWriteOperation(String stackName, String protocolName, String propertyName, String propertyValue) {
        return Operations.createWriteAttributeOperation(getProtocolPropertyAddress(stackName, protocolName, propertyName), new SimpleAttribute(PropertyResourceDefinition.VALUE), new ModelNode(propertyValue));
    }

    protected static ModelNode getProtocolGetPropertyOperation(String stackName, String protocolName, String propertyName) {
        return Operations.createMapGetOperation(getProtocolAddress(stackName, protocolName), ProtocolResourceDefinition.Attribute.PROPERTIES, propertyName);
    }

    protected static ModelNode getProtocolPutPropertyOperation(String stackName, String protocolName, String propertyName, String propertyValue) {
        return Operations.createMapPutOperation(getProtocolAddress(stackName, protocolName), ProtocolResourceDefinition.Attribute.PROPERTIES, propertyName, propertyValue);
    }

    protected static ModelNode getProtocolRemovePropertyOperation(String stackName, String protocolName, String propertyName) {
        return Operations.createMapRemoveOperation(getProtocolAddress(stackName, protocolName), ProtocolResourceDefinition.Attribute.PROPERTIES, propertyName);
    }

    protected static ModelNode getProtocolRemoveOperation(String stackName, String type) {
        return Util.createRemoveOperation(getProtocolAddress(stackName, type));
    }

    protected static PathAddress getSubsystemAddress() {
        return PathAddress.pathAddress(JGroupsSubsystemResourceDefinition.PATH);
    }

    protected static PathAddress getProtocolStackAddress(String stackName) {
        return getSubsystemAddress().append(StackResourceDefinition.pathElement(stackName));
    }

    protected static PathAddress getTransportAddress(String stackName, String type) {
        return getProtocolStackAddress(stackName).append(TransportResourceDefinition.pathElement(type));
    }

    protected static PathAddress getTransportPropertyAddress(String stackName, String type, String propertyName) {
        return getTransportAddress(stackName, type).append(PropertyResourceDefinition.pathElement(propertyName));
    }

    protected static PathAddress getProtocolAddress(String stackName, String type) {
        return getProtocolStackAddress(stackName).append(ProtocolResourceDefinition.pathElement(type));
    }

    protected static PathAddress getProtocolPropertyAddress(String stackName, String type, String propertyName) {
        return getProtocolAddress(stackName, type).append(PropertyResourceDefinition.pathElement(propertyName));
    }

    protected String getSubsystemXml() throws IOException {
        return readResource(SUBSYSTEM_XML_FILE) ;
    }

    protected KernelServices buildKernelServices() throws Exception {
        return createKernelServicesBuilder(new AdditionalInitialization().require(RequiredCapability.SOCKET_BINDING, "some-binding", "jgroups-diagnostics", "jgroups-mping", "jgroups-tcp-fd", "new-socket-binding")).setSubsystemXml(this.getSubsystemXml()).build();
    }
}