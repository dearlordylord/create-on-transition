package org.swift.jira.cot.functions;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.config.StatusManager;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory;
import com.atlassian.jira.plugin.workflow.WorkflowPluginFunctionFactory;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.I18nHelper;
import com.opensymphony.workflow.loader.AbstractDescriptor;
import com.opensymphony.workflow.loader.FunctionDescriptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: firfi
 * Date: 16.10.12
 * Time: 11:04
 * To change this template use File | Settings | File Templates.
 */
public class ActionWithLinkedIssuesFunctionPluginFactory extends AbstractWorkflowPluginFactory implements WorkflowPluginFunctionFactory {

    protected static Log log = LogFactory.getLog(CreateSubIssueFunctionPluginFactory.class);
    protected final SubTaskManager subTaskManager;
    protected final IssueTypeManager issueTypeManager;
    protected final ConstantsManager constantsManager;
    protected final ApplicationProperties applicationProperties;
    protected final JiraAuthenticationContext authenticationContext;
    protected final I18nHelper.BeanFactory i18nBeanFactory;
    protected final EventPublisher eventPublisher;
    protected final IssueLinkTypeManager issueLinkTypeManager;
    private final StatusManager statusManager;

    public ActionWithLinkedIssuesFunctionPluginFactory(SubTaskManager subTaskManager, IssueTypeManager issueTypeManager, ConstantsManager constantsManager, ApplicationProperties applicationProperties, JiraAuthenticationContext authenticationContext, I18nHelper.BeanFactory i18nBeanFactory, EventPublisher eventPublisher, IssueLinkTypeManager issueLinkTypeManager, StatusManager statusManager) {
        this.subTaskManager = subTaskManager;
        this.issueTypeManager = issueTypeManager;
        this.constantsManager = constantsManager;
        this.applicationProperties = applicationProperties;
        this.authenticationContext = authenticationContext;
        this.i18nBeanFactory = i18nBeanFactory;
        this.eventPublisher = eventPublisher;
        this.issueLinkTypeManager = issueLinkTypeManager;
        this.statusManager = statusManager;
    }

    @Override
    public Map<String, ?> getDescriptorParams(Map<String, Object> conditionParams) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("field.issueLinkTypeId", extractSingleParam(conditionParams, "issueLinkTypeId"));
        params.put("field.issueLinkDirection", extractSingleParam(conditionParams, "issueLinkDirection"));
        params.put("field.linkedIssueComment", extractSingleParam(conditionParams, "linkedIssueComment"));
        params.put("field.issueCommentDirection", extractSingleParam(conditionParams, "issueCommentDirection"));
        params.put("field.commentUser", extractSingleParam(conditionParams, "commentUser"));
        params.put("field.linkedIssueMoveStatus", extractSingleParam(conditionParams, "linkedIssueMoveStatus"));
        params.put("field.moveUser", extractSingleParam(conditionParams, "moveUser"));
        params.put("field.specificCommentUser", extractSingleParam(conditionParams, "specificCommentUser"));
        params.put("field.specificMoveUser", extractSingleParam(conditionParams, "specificMoveUser"));
        return params;
    }

    @Override
    protected void getVelocityParamsForView(Map<String, Object> velocityParams, AbstractDescriptor descriptor) {
        FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
        velocityParams.put("issueLinkTypeId", functionDescriptor.getArgs().get("field.issueLinkTypeId"));
        velocityParams.put("issueLinkDirection", functionDescriptor.getArgs().get("field.issueLinkDirection"));
        velocityParams.put("linkedIssueComment", functionDescriptor.getArgs().get("field.linkedIssueComment"));
        velocityParams.put("issueCommentDirection", functionDescriptor.getArgs().get("field.issueCommentDirection"));
        velocityParams.put("commentUser", functionDescriptor.getArgs().get("field.commentUser"));
        velocityParams.put("linkedIssueMoveStatus", functionDescriptor.getArgs().get("field.linkedIssueMoveStatus"));
        velocityParams.put("moveUser", functionDescriptor.getArgs().get("field.moveUser"));

        String specificCommentUser = (String) functionDescriptor.getArgs().get("field.specificCommentUser");
        String specificMoveUser = (String) functionDescriptor.getArgs().get("field.specificMoveUser");
        velocityParams.put("specificCommentUser", specificCommentUser == null ? "" : specificCommentUser);
        velocityParams.put("specificMoveUser", specificMoveUser == null ? "" : specificMoveUser);

    }

    @Override
    protected void getVelocityParamsForInput(Map<String, Object> velocityParams) {
        velocityParams.put("issueLinkTypes", issueLinkTypeManager.getIssueLinkTypes());
        velocityParams.put("statuses", statusManager.getStatuses());
    }

    @Override
    protected void getVelocityParamsForEdit(Map<String, Object> velocityParams, AbstractDescriptor descriptor) {
        FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
        velocityParams.put("issueLinkTypes", issueLinkTypeManager.getIssueLinkTypes());
        velocityParams.put("statuses", statusManager.getStatuses());
        velocityParams.put("currentIssueLinkTypeId", functionDescriptor.getArgs().get("field.issueLinkTypeId"));
        velocityParams.put("currentIssueLinkDirection", functionDescriptor.getArgs().get("field.issueLinkDirection"));
        velocityParams.put("currentLinkedIssueComment", functionDescriptor.getArgs().get("field.linkedIssueComment"));
        velocityParams.put("currentIssueCommentDirection", functionDescriptor.getArgs().get("field.issueCommentDirection"));
        velocityParams.put("currentCommentUser", functionDescriptor.getArgs().get("field.commentUser"));
        String specificCommentUser = (String) functionDescriptor.getArgs().get("field.specificCommentUser");
        velocityParams.put("currentSpecificCommentUser", specificCommentUser == null ? "" : specificCommentUser);
        velocityParams.put("currentLinkedIssueMoveStatus", functionDescriptor.getArgs().get("field.linkedIssueMoveStatus"));
        velocityParams.put("currentMoveUser", functionDescriptor.getArgs().get("field.moveUser"));
        String specificMoveUser = (String) functionDescriptor.getArgs().get("field.specificMoveUser");
        velocityParams.put("currentSpecificMoveUser", specificMoveUser == null ? "" : specificMoveUser);
    }
}
