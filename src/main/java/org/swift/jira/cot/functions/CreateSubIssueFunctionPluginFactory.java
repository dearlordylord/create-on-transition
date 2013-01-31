/*
 * Copyright (c) 2006, 2011 Bob Swift and other contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.swift.jira.cot.functions;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ofbiz.core.entity.GenericValue;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.bc.issue.worklog.TimeTrackingConfiguration;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory;
import com.atlassian.jira.plugin.workflow.WorkflowPluginFunctionFactory;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.JiraDurationUtils;
import com.atlassian.jira.util.LocaleParser;
import com.opensymphony.workflow.loader.AbstractDescriptor;
import com.opensymphony.workflow.loader.FunctionDescriptor;

public class CreateSubIssueFunctionPluginFactory extends AbstractWorkflowPluginFactory implements WorkflowPluginFunctionFactory {

    protected static Log log = LogFactory.getLog(CreateSubIssueFunctionPluginFactory.class);
    protected final SubTaskManager subTaskManager;
    protected final IssueTypeManager issueTypeManager;
    protected final ConstantsManager constantsManager;
    protected final ApplicationProperties applicationProperties;
    protected final JiraAuthenticationContext authenticationContext;
    protected final I18nHelper.BeanFactory i18nBeanFactory;
    protected final EventPublisher eventPublisher;
    protected final IssueLinkTypeManager issueLinkTypeManager;

    /**
     * Constructor
     *
     * @param subTaskManager
     * @param issueTypeManager
     * @param constantsManager
     * @param issueLinkTypeManager
     */
    public CreateSubIssueFunctionPluginFactory(final SubTaskManager subTaskManager, IssueTypeManager issueTypeManager, final ConstantsManager constantsManager,
                                               final ApplicationProperties applicationProperties, final JiraAuthenticationContext authenticationContext,
                                               final I18nHelper.BeanFactory i18nBeanFactory, final EventPublisher eventPublisher, IssueLinkTypeManager issueLinkTypeManager) {
        this.subTaskManager = subTaskManager;
        this.issueTypeManager = issueTypeManager;
        this.constantsManager = constantsManager;
        this.applicationProperties = applicationProperties;
        this.authenticationContext = authenticationContext;
        this.i18nBeanFactory = i18nBeanFactory;
        this.eventPublisher = eventPublisher;
        this.issueLinkTypeManager = issueLinkTypeManager;
    }

    /**
     * Get descriptor parameters
     * 
     * @see com.atlassian.jira.plugin.workflow.WorkflowPluginFactory#getDescriptorParams(java.util.Map).
     */
    @Override
    public Map<String, String> getDescriptorParams(final Map<String, Object> conditionParams) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("field.subIssueTypeId", extractSingleParam(conditionParams, "subIssueTypeId"));
        params.put("field.createSibbling", extractSingleParam(conditionParams, "createSibbling"));
        params.put("field.subIssueSummary", extractSingleParam(conditionParams, "subIssueSummary"));
        params.put("field.subIssueDescription", extractSingleParam(conditionParams, "subIssueDescription"));
        params.put("field.subIssuePriorityId", extractSingleParam(conditionParams, "subIssuePriorityId"));

        params.put("field.issueLinkTypeId", extractSingleParam(conditionParams, "issueLinkTypeId"));
        params.put("field.issueLinkDirection", extractSingleParam(conditionParams, "issueLinkDirection"));

        params.put("field.linkDepth", extractSingleParam(conditionParams, "linkDepth"));
        params.put("field.notPerformIfJql", extractSingleParam(conditionParams, "notPerformIfJql"));

        // Reporter
        params.put("field.subIssueReporter", extractSingleParam(conditionParams, "subIssueReporter"));
        params.put("field.specificReporter", extractSingleParam(conditionParams, "specificReporter"));

        // Assignee
        params.put("field.subIssueAssignee", extractSingleParam(conditionParams, "subIssueAssignee"));
        params.put("field.specificAssignee", extractSingleParam(conditionParams, "specificAssignee"));

        // Affected versions
        params.put("field.subIssueAffectedVersions", extractSingleParam(conditionParams, "subIssueAffectedVersions"));
        params.put("field.specificAffectedVersions", extractSingleParam(conditionParams, "specificAffectedVersions"));

        // Fixed versions
        params.put("field.subIssueFixedVersions", extractSingleParam(conditionParams, "subIssueFixedVersions"));
        params.put("field.specificFixedVersions", extractSingleParam(conditionParams, "specificFixedVersions"));

        // Components
        params.put("field.subIssueComponents", extractSingleParam(conditionParams, "subIssueComponents"));
        params.put("field.specificComponents", extractSingleParam(conditionParams, "specificComponents"));

        // Due date
        params.put("field.subIssueDueDate", extractSingleParam(conditionParams, "subIssueDueDate"));
        params.put("field.specificDueDate", extractSingleParam(conditionParams, "specificDueDate"));
        params.put("field.dueDateOffset", extractSingleParam(conditionParams, "dueDateOffset"));

        // Original estimate
        params.put("field.subIssueOriginalEstimate", extractSingleParam(conditionParams, "subIssueOriginalEstimate"));

        // field.notPerformIfCustomFieldsIsNull
        params.put("field.notPerformIfCustomFieldsIsNull", extractSingleParam(conditionParams, "notPerformIfCustomFieldsIsNull"));

        // Copy parent fields
        params.put("field.copyParentFields", extractSingleParam(conditionParams, "copyParentFields"));

        // Set custom field 1
        params.put("field.customField1Name", extractSingleParam(conditionParams, "customField1Name"));
        params.put("field.customField1Value1", extractSingleParam(conditionParams, "customField1Value1"));
        params.put("field.customField1Value2", extractSingleParam(conditionParams, "customField1Value2"));

        // Set custom field 2
        params.put("field.customField2Name", extractSingleParam(conditionParams, "customField2Name"));
        params.put("field.customField2Value1", extractSingleParam(conditionParams, "customField2Value1"));
        params.put("field.customField2Value2", extractSingleParam(conditionParams, "customField2Value2"));

        // Set custom field 3
        params.put("field.customField3Name", extractSingleParam(conditionParams, "customField3Name"));
        params.put("field.customField3Value1", extractSingleParam(conditionParams, "customField3Value1"));
        params.put("field.customField3Value2", extractSingleParam(conditionParams, "customField3Value2"));

        return params;
    }

    /**
     * VIEW
     * 
     * @see com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory#getVelocityParamsForView(java.util.Map,
     *      com.opensymphony.workflow.loader.AbstractDescriptor).
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void getVelocityParamsForView(@SuppressWarnings("rawtypes") Map velocityParams, AbstractDescriptor descriptor) {
        if (!(descriptor instanceof FunctionDescriptor))
            throw new IllegalArgumentException("Descriptor must be a FunctionDescriptor.");
        FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
        String issueTypeId = (String) functionDescriptor.getArgs().get("field.subIssueTypeId");
        if (issueTypeId != null) {

            IssueType issueType = issueTypeManager.getIssueType(issueTypeId);
            velocityParams.put("subIssueTypeName", issueType.getName());
            velocityParams.put("isSubTask", issueType.isSubTask());
            String issueLinkTypeId = (String) functionDescriptor.getArgs().get("field.issueLinkTypeId");
            if (issueLinkTypeId != null && !"0".equals(issueLinkTypeId))
                velocityParams.put("issueLinkTypeName", issueLinkTypeManager.getIssueLinkType(Long.parseLong(issueLinkTypeId)).getName());
            velocityParams.put("linkDepth", functionDescriptor.getArgs().get("field.linkDepth"));
            velocityParams.put("notPerformIfJql", functionDescriptor.getArgs().get("field.notPerformIfJql"));
            velocityParams.put("createSibbling", functionDescriptor.getArgs().get("field.createSibbling"));
            velocityParams.put("issueLinkTypeId", functionDescriptor.getArgs().get("field.issueLinkTypeId"));
            velocityParams.put("issueLinkDirection", functionDescriptor.getArgs().get("field.issueLinkDirection"));
            velocityParams.put("subIssuePriorityId", functionDescriptor.getArgs().get("field.subIssuePriorityId"));
            velocityParams.put("subIssueSummary", functionDescriptor.getArgs().get("field.subIssueSummary"));
            velocityParams.put("subIssueDescription", functionDescriptor.getArgs().get("field.subIssueDescription"));

            int reporter = CreateUtilities.USER_CURRENT; // default to current user
            int assignTo = CreateUtilities.USER_ASSIGNEE; // default to assignee
            try {
                reporter = Integer.parseInt((String) functionDescriptor.getArgs().get("field.subIssueReporter"));
            } catch (Exception exception) {
            }
            try {
                assignTo = Integer.parseInt((String) functionDescriptor.getArgs().get("field.subIssueAssignee"));
            } catch (Exception exception) {
            }
            velocityParams.put("subIssueReporterName", CreateSubIssueFunction.USER_TEXT[reporter]);
            velocityParams.put("subIssueAssigneeName", CreateSubIssueFunction.USER_TEXT[assignTo]);

            String specificReporter = (String) functionDescriptor.getArgs().get("field.specificReporter");
            String specificAssignee = (String) functionDescriptor.getArgs().get("field.specificAssignee");
            velocityParams.put("specificReporter", specificReporter == null ? "" : specificReporter);
            velocityParams.put("specificAssignee", specificAssignee == null ? "" : specificAssignee);

            int affectedVersions = CreateSubIssueFunction.VERSIONS_NONE; // default to none
            int fixedVersions = CreateSubIssueFunction.VERSIONS_NONE; // default to none
            try {
                affectedVersions = Integer.parseInt((String) functionDescriptor.getArgs().get("field.subIssueAffectedVersions"));
            } catch (Exception exception) {
            }
            try {
                fixedVersions = Integer.parseInt((String) functionDescriptor.getArgs().get("field.subIssueFixedVersions"));
            } catch (Exception exception) {
            }
            velocityParams.put("subIssueAffectedVersionsName", CreateSubIssueFunction.VERSIONS_TEXT[affectedVersions]);
            velocityParams.put("subIssueFixedVersionsName", CreateSubIssueFunction.VERSIONS_TEXT[fixedVersions]);

            String specificAffectedVersions = (String) functionDescriptor.getArgs().get("field.specificAffectedVersions");
            String specificFixedVersions = (String) functionDescriptor.getArgs().get("field.specificFixedVersions");
            velocityParams.put("specificAffectedVersions", specificAffectedVersions == null ? "" : specificAffectedVersions);
            velocityParams.put("specificFixedVersions", specificFixedVersions == null ? "" : specificFixedVersions);

            int components = CreateSubIssueFunction.COMPONENTS_PARENT; // default to parent's like v1 of plugin
            try {
                components = Integer.parseInt((String) functionDescriptor.getArgs().get("field.subIssueComponents"));
            } catch (Exception exception) {
            }

            velocityParams.put("subIssueComponentsName", CreateSubIssueFunction.COMPONENTS_TEXT[components]);

            String specificComponents = (String) functionDescriptor.getArgs().get("field.specificComponents");
            velocityParams.put("specificComponents", specificComponents == null ? "" : specificComponents);

            // Due date
            int dueDate = CreateSubIssueFunction.DUE_DATE_DEFAULT;
            try {
                dueDate = Integer.parseInt((String) functionDescriptor.getArgs().get("field.subIssueDueDate"));
            } catch (Exception exception) {
            }
            velocityParams.put("subIssueDueDate", CreateSubIssueFunction.DUE_DATE_TEXT[dueDate]);

            String specificDueDate = (String) functionDescriptor.getArgs().get("field.specificDueDate");
            velocityParams.put("specificDueDate", specificDueDate == null ? "" : specificDueDate);

            String dueDateOffset = (String) functionDescriptor.getArgs().get("field.dueDateOffset");
            velocityParams.put("dueDateOffset", dueDateOffset == null ? "" : dueDateOffset);

            // Original estimate
            String subIssueOriginalEstimate = (String) functionDescriptor.getArgs().get("field.subIssueOriginalEstimate");
            velocityParams.put("subIssueOriginalEstimate", subIssueOriginalEstimate == null ? "" : subIssueOriginalEstimate);

            // notPerformIfCustomFieldsIsNull
            String notPerformIfCustomFieldsIsNull = (String) functionDescriptor.getArgs().get("field.notPerformIfCustomFieldsIsNull");
            velocityParams.put("notPerformIfCustomFieldsIsNull", notPerformIfCustomFieldsIsNull == null? "" : notPerformIfCustomFieldsIsNull);

            // Copy parent fields
            String copyParentFields = (String) functionDescriptor.getArgs().get("field.copyParentFields");
            velocityParams.put("copyParentFields", copyParentFields == null ? "" : copyParentFields);

            // Set custom field 1
            String customField1Name = (String) functionDescriptor.getArgs().get("field.customField1Name");
            velocityParams.put("customField1Name", customField1Name == null ? "" : customField1Name);
            String customField1Value1 = (String) functionDescriptor.getArgs().get("field.customField1Value1");
            velocityParams.put("customField1Value1", customField1Value1 == null ? "" : customField1Value1);
            String customField1Value2 = (String) functionDescriptor.getArgs().get("field.customField1Value2");
            velocityParams.put("customField1Value2", customField1Value2 == null ? "" : customField1Value2);

            // Set custom field 2
            String customField2Name = (String) functionDescriptor.getArgs().get("field.customField2Name");
            velocityParams.put("customField2Name", customField2Name == null ? "" : customField2Name);
            String customField2Value1 = (String) functionDescriptor.getArgs().get("field.customField2Value1");
            velocityParams.put("customField2Value1", customField2Value1 == null ? "" : customField2Value1);
            String customField2Value2 = (String) functionDescriptor.getArgs().get("field.customField2Value2");
            velocityParams.put("customField2Value2", customField2Value2 == null ? "" : customField2Value2);

            // Set custom field 3
            String customField3Name = (String) functionDescriptor.getArgs().get("field.customField3Name");
            velocityParams.put("customField3Name", customField3Name == null ? "" : customField3Name);
            String customField3Value1 = (String) functionDescriptor.getArgs().get("field.customField3Value1");
            velocityParams.put("customField3Value1", customField3Value1 == null ? "" : customField3Value1);
            String customField3Value2 = (String) functionDescriptor.getArgs().get("field.customField3Value2");
            velocityParams.put("customField3Value2", customField3Value2 == null ? "" : customField3Value2);

        }
    }

    /**
     * ADD
     * 
     * @see com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory#getVelocityParamsForInput(java.util.Map).
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void getVelocityParamsForInput(@SuppressWarnings("rawtypes") Map velocityParams) {
        velocityParams.put("createSibbling", "0");
        velocityParams.put("subIssueTypes", issueTypeManager.getIssueTypes());
        velocityParams.put("subIssuePriorities", constantsManager.getPriorityObjects());

        velocityParams.put("issueLinkTypes", issueLinkTypeManager.getIssueLinkTypes());

        velocityParams.put("currentSubIssueReporter", CreateUtilities.USER_CURRENT);
        velocityParams.put("currentSubIssueAssignee", CreateUtilities.USER_ASSIGNEE);

        velocityParams.put("currentSubIssueAffectedVersions", CreateSubIssueFunction.VERSIONS_NONE);
        velocityParams.put("currentSubIssueFixedVersions", CreateSubIssueFunction.VERSIONS_NONE);

        velocityParams.put("currentSubIssueComponents", CreateSubIssueFunction.COMPONENTS_PARENT);
        velocityParams.put("currentSubIssueSummary", "%parent_summary%");

        // Due date
        velocityParams.put("currentSubIssueDueDate", CreateSubIssueFunction.DUE_DATE_DEFAULT);
        velocityParams.put("dateFormat", getExampleDateFormat()); // help text

        // Original estimate
        velocityParams.put("currentSubIssueOriginalEstimate", "");
        velocityParams.put("timeDurationFormat", getExampleTimeDurationFormat()); // help text

        velocityParams.put("currentNotPerformIfCustomFieldsIsNull", "");

        // Copy parent fields
        velocityParams.put("currentCopyParentFields", "");

        // Set custom field 1
        velocityParams.put("customField1Name", "");
        velocityParams.put("customField1Value1", "");
        velocityParams.put("customField1Value2", "");

        // Set custom field 2
        velocityParams.put("customField2Name", "");
        velocityParams.put("customField2Value1", "");
        velocityParams.put("customField2Value2", "");

        // Set custom field 3
        velocityParams.put("customField3Name", "");
        velocityParams.put("customField3Value1", "");
        velocityParams.put("customField3Value2", "");
    }

    /**
     * EDIT
     * 
     * @see com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory#getVelocityParamsForEdit(java.util.Map,
     *      com.opensymphony.workflow.loader.AbstractDescriptor).
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void getVelocityParamsForEdit(@SuppressWarnings("rawtypes") Map velocityParams, AbstractDescriptor descriptor) {
        if (!(descriptor instanceof FunctionDescriptor)) {
            throw new IllegalArgumentException("Descriptor must be a FunctionDescriptor.");
        } else {

            FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
            velocityParams.put("currentSubIssueTypeId", functionDescriptor.getArgs().get("field.subIssueTypeId"));
            velocityParams.put("currentCreateSibbling", functionDescriptor.getArgs().get("field.createSibbling"));
            velocityParams.put("currentSubIssueSummary", functionDescriptor.getArgs().get("field.subIssueSummary"));
            velocityParams.put("currentSubIssueDescription", functionDescriptor.getArgs().get("field.subIssueDescription"));
            velocityParams.put("currentIssueLinkTypeId", functionDescriptor.getArgs().get("field.issueLinkTypeId"));
            velocityParams.put("currentIssueLinkDirection", functionDescriptor.getArgs().get("field.issueLinkDirection"));

            velocityParams.put("currentSubIssuePriorityId", functionDescriptor.getArgs().get("field.subIssuePriorityId"));
            velocityParams.put("subIssueTypes", issueTypeManager.getIssueTypes());
            velocityParams.put("subIssuePriorities", constantsManager.getPriorityObjects());

            velocityParams.put("issueLinkTypes", issueLinkTypeManager.getIssueLinkTypes());

            velocityParams.put("currentLinkDepth", functionDescriptor.getArgs().get("field.linkDepth"));
            velocityParams.put("currentNotPerformIfJql", functionDescriptor.getArgs().get("field.notPerformIfJql"));

            // Reporter
            int reporter = CreateUtilities.getInt((String) functionDescriptor.getArgs().get("field.subIssueReporter"), CreateUtilities.USER_CURRENT);
            velocityParams.put("currentSubIssueReporter", reporter);

            String specificReporter = (String) functionDescriptor.getArgs().get("field.specificReporter");
            velocityParams.put("currentSpecificReporter", specificReporter == null ? "" : specificReporter);

            // Assignee
            int assignee = CreateUtilities.getInt((String) functionDescriptor.getArgs().get("field.subIssueAssignee"), CreateUtilities.USER_ASSIGNEE);
            velocityParams.put("currentSubIssueAssignee", assignee);

            String specificAssignee = (String) functionDescriptor.getArgs().get("field.specificAssignee");
            velocityParams.put("currentSpecificAssignee", specificAssignee == null ? "" : specificAssignee);

            // Affected versions
            int affectedVersions = CreateUtilities.getInt((String) functionDescriptor.getArgs().get("field.subIssueAffectedVersions"),
                    CreateSubIssueFunction.VERSIONS_NONE); // default to none
            velocityParams.put("currentSubIssueAffectedVersions", affectedVersions);
            String specificAffectedVersions = (String) functionDescriptor.getArgs().get("field.specificAffectedVersions");
            velocityParams.put("currentSpecificAffectedVersions", specificAffectedVersions == null ? "" : specificAffectedVersions);

            // Fixed versions
            int fixedVersions = CreateUtilities.getInt((String) functionDescriptor.getArgs().get("field.subIssueFixedVersions"),
                    CreateSubIssueFunction.VERSIONS_NONE); // default to none
            velocityParams.put("currentSubIssueFixedVersions", fixedVersions);

            String specificFixedVersions = (String) functionDescriptor.getArgs().get("field.specificFixedVersions");
            velocityParams.put("currentSpecificFixedVersions", specificFixedVersions == null ? "" : specificFixedVersions);

            // Components
            int components = CreateUtilities.getInt((String) functionDescriptor.getArgs().get("field.subIssueComponents"),
                    CreateSubIssueFunction.COMPONENTS_PARENT); // default to parent
            velocityParams.put("currentSubIssueComponents", components);

            String specificComponents = (String) functionDescriptor.getArgs().get("field.specificComponents");
            velocityParams.put("currentSpecificComponents", specificComponents == null ? "" : specificComponents);

            // Due date
            int dueDate = CreateUtilities.getInt((String) functionDescriptor.getArgs().get("field.subIssueDueDate"), CreateSubIssueFunction.DUE_DATE_DEFAULT);
            velocityParams.put("currentSubIssueDueDate", dueDate);

            String specificDueDate = (String) functionDescriptor.getArgs().get("field.specificDueDate");
            velocityParams.put("currentSpecificDueDate", specificDueDate == null ? "" : specificDueDate);
            velocityParams.put("dateFormat", getExampleDateFormat()); // help text

            String dueDateOffset = (String) functionDescriptor.getArgs().get("field.dueDateOffset");
            velocityParams.put("currentDueDateOffset", dueDateOffset == null ? "" : dueDateOffset);

            // Original estimate
            String subIssueOriginalEstimate = (String) functionDescriptor.getArgs().get("field.subIssueOriginalEstimate");
            velocityParams.put("currentSubIssueOriginalEstimate", subIssueOriginalEstimate == null ? "" : subIssueOriginalEstimate);
            velocityParams.put("timeDurationFormat", getExampleTimeDurationFormat()); // help text

            // notPerformIfCustomFieldsIsNull
            String notPerformIfCustomFieldsIsNull = (String) functionDescriptor.getArgs().get("field.notPerformIfCustomFieldsIsNull");
            velocityParams.put("currentNotPerformIfCustomFieldsIsNull", notPerformIfCustomFieldsIsNull == null? "" : notPerformIfCustomFieldsIsNull);

            // Copy parent fields
            String copyParentFields = (String) functionDescriptor.getArgs().get("field.copyParentFields");
            velocityParams.put("currentCopyParentFields", copyParentFields == null ? "" : copyParentFields);

            // Set custom field 1
            String customField1Name = (String) functionDescriptor.getArgs().get("field.customField1Name");
            velocityParams.put("currentCustomField1Name", customField1Name == null ? "" : customField1Name);
            String customField1Value1 = (String) functionDescriptor.getArgs().get("field.customField1Value1");
            velocityParams.put("currentCustomField1Value1", customField1Name == null ? "" : customField1Value1);
            String customField1Value2 = (String) functionDescriptor.getArgs().get("field.customField1Value2");
            velocityParams.put("currentCustomField1Value2", customField1Name == null ? "" : customField1Value2);

            // Set custom field 2
            String customField2Name = (String) functionDescriptor.getArgs().get("field.customField2Name");
            velocityParams.put("currentCustomField2Name", customField2Name == null ? "" : customField2Name);
            String customField2Value1 = (String) functionDescriptor.getArgs().get("field.customField2Value1");
            velocityParams.put("currentCustomField2Value1", customField2Name == null ? "" : customField2Value1);
            String customField2Value2 = (String) functionDescriptor.getArgs().get("field.customField2Value2");
            velocityParams.put("currentCustomField2Value2", customField2Name == null ? "" : customField2Value2);

            // Set custom field 3
            String customField3Name = (String) functionDescriptor.getArgs().get("field.customField3Name");
            velocityParams.put("currentCustomField3Name", customField3Name == null ? "" : customField3Name);
            String customField3Value1 = (String) functionDescriptor.getArgs().get("field.customField3Value1");
            velocityParams.put("currentCustomField3Value1", customField3Name == null ? "" : customField3Value1);
            String customField3Value2 = (String) functionDescriptor.getArgs().get("field.customField3Value2");
            velocityParams.put("currentCustomField3Value2", customField3Name == null ? "" : customField3Value2);
        }
    }

    /**
     * Get example date format
     * 
     * @return example string like: 1d 2h 15m
     */
    protected String getExampleDateFormat() {
        return applicationProperties.getDefaultBackedString(APKeys.JIRA_LF_DATE_DMY); // help text
    }


    /**
     * Get example of time duration format in the JIRA default locale format
     * 
     * @return example string like: 1d 2h 15m
     */
    protected String getExampleTimeDurationFormat() {
        // For consistency across all users, we need the original estimate to be in the system default format and not user specific
        JiraDurationUtils durationUtils = new JiraDurationUtils(applicationProperties, authenticationContext, new TimeTrackingConfiguration.PropertiesAdaptor(
                applicationProperties), eventPublisher, i18nBeanFactory);
        // Locale defaultLocale = LocaleManager..DEFAULT_LOCALE when we are at JIRA 5 api level
        Locale defaultLocale = LocaleParser.parseLocale(applicationProperties.getString(APKeys.JIRA_I18N_DEFAULT_LOCALE));
        if (defaultLocale == null) {
            defaultLocale = Locale.ENGLISH;
        }
        // example format = 1d 2h 15m for default environment - convert to seconds
        return durationUtils.getShortFormattedDuration((((1L * 24 + 2) * 60) + 15) * 60, defaultLocale);
    }
}
