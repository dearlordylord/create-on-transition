package ru.megaplan.jira.plugins.workflow.validator;

import com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory;
import com.atlassian.jira.plugin.workflow.WorkflowPluginValidatorFactory;
import com.opensymphony.workflow.loader.AbstractDescriptor;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: firfi
 * Date: 19.11.12
 * Time: 12:36
 * To change this template use File | Settings | File Templates.
 */
public class JqlValidatorFactory extends AbstractWorkflowPluginFactory implements WorkflowPluginValidatorFactory {
    @Override
    protected void getVelocityParamsForInput(Map<String, Object> stringObjectMap) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void getVelocityParamsForEdit(Map<String, Object> stringObjectMap, AbstractDescriptor abstractDescriptor) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void getVelocityParamsForView(Map<String, Object> stringObjectMap, AbstractDescriptor abstractDescriptor) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Map<String, ?> getDescriptorParams(Map<String, Object> stringObjectMap) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
