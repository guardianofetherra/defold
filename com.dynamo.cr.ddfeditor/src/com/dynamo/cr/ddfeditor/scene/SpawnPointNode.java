package com.dynamo.cr.ddfeditor.scene;

import org.eclipse.core.runtime.IStatus;

import com.dynamo.cr.go.core.ComponentTypeNode;
import com.dynamo.cr.properties.Property;
import com.dynamo.cr.properties.Resource;

public class SpawnPointNode extends ComponentTypeNode {

    @Property(isResource=true)
    @Resource
    private String prototype = "";

    public SpawnPointNode() {
        super();
    }

    public String getPrototype() {
        return this.prototype;
    }

    public void setPrototype(String prototype) {
        if (!this.prototype.equals(prototype)) {
            this.prototype = prototype;
            notifyChange();
        }
    }

    @Override
    public String getTypeId() {
        return "spawnpoint";
    }

    @Override
    public String getTypeName() {
        return "Spawn Point";
    }

    @Override
    protected IStatus doValidate() {
        return validateProperties(new String[] {"prototype"});
    }
}
