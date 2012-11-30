package ru.megaplan.jira.plugins.workflow.validator;

import com.atlassian.core.util.map.EasyMap;
import com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory;
import com.atlassian.jira.plugin.workflow.WorkflowPluginValidatorFactory;
import com.opensymphony.workflow.loader.AbstractDescriptor;
import com.opensymphony.workflow.loader.ValidatorDescriptor;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: firfi
 * Date: 19.11.12
 * Time: 12:36
 * To change this template use File | Settings | File Templates.
 */
public class JqlValidatorFactory extends AbstractWorkflowPluginFactory implements WorkflowPluginValidatorFactory {

    private final static Logger log = Logger.getLogger(JqlValidatorFactory.class);

    final static String JQL = "jql";
    final static String MIN = "min";
    final static String MAX = "max";
    final static String ERROR = "error";
    final static String IGNOREDROLES = "ignoredroles";

    final static String JQLDEFAULT = "";
    final static String MINDEFAULT = "0";
    final static String MAXDEFAULT = "3";
    final static String ERRORDEFAULT = "JQL Condition isn't suitable";
    final static String IGNOREDROLESDEFAULT = "";

    final static String NOTDEFINED = "NOT DEFINED";

    @SuppressWarnings("unchecked")
    final static Map<String, String> fieldsAndDefaultsEdit = EasyMap.build(
            JQL, JQLDEFAULT,
            MIN, MINDEFAULT,
            MAX, MAXDEFAULT,
            ERROR, ERRORDEFAULT,
            IGNOREDROLES, IGNOREDROLESDEFAULT
    );

    @SuppressWarnings("unchecked")
    final static Map<String, String> fieldsAndDefaultsView = new HashMap<String, String>();

    static {
        for (String k : fieldsAndDefaultsEdit.keySet()) {
            fieldsAndDefaultsView.put(k, NOTDEFINED);
        }
    }



    @Override
    protected void getVelocityParamsForInput(Map<String, Object> stringObjectMap) {
        stringObjectMap.putAll(fieldsAndDefaultsEdit);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void getVelocityParamsForEdit(Map<String, Object> velocityParams, AbstractDescriptor d) {
        bulkMutateVparams(velocityParams, d, fieldsAndDefaultsEdit);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void getVelocityParamsForView(Map<String, Object> velocityParams, AbstractDescriptor d) {
        bulkMutateVparams(velocityParams, d, fieldsAndDefaultsView);
    }

    private void bulkMutateVparams(Map<String, Object> velocityParams, AbstractDescriptor d, Map<String, String> fieldsAndDefaults) {
        for (Map.Entry<String,String> e : fieldsAndDefaults.entrySet()) {
            velocityParams.put(e.getKey(), getField(d, e.getKey(), e.getValue()));
        }
    }

    private void bulkCopy(Map<String, Object> conditionParams, Map<String, String> result, Set<String> keys) {
        for (String k : keys) {
            if (conditionParams.containsKey(k)) {
                result.put(k, extractSingleParam(conditionParams, k));
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String> getDescriptorParams(Map<String, Object> conditionParams) {
        Map<String, String> result = new HashMap<String, String>();
        if (conditionParams == null) return result;
        bulkCopy(conditionParams, result, fieldsAndDefaultsEdit.keySet());
        return result;
    }

    private String getField(AbstractDescriptor descriptor, String name, String defult) {
        if (!(descriptor instanceof ValidatorDescriptor)) {
            throw new IllegalArgumentException("Descriptor must be a ConditionDescriptor.");
        }

        ValidatorDescriptor validatorDescriptor = (ValidatorDescriptor) descriptor;

        String field = (String) validatorDescriptor.getArgs().get(name);
        if (field != null && field.trim().length() > 0)
            return field;
        else
            return defult;
    }

}
