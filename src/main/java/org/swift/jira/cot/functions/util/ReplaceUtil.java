package org.swift.jira.cot.functions.util;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.resolution.Resolution;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.util.concurrent.Nullable;
import org.apache.log4j.Logger;
import org.swift.jira.cot.functions.CreateUtilities;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: firfi
 * Date: 16.10.12
 * Time: 13:50
 * To change this template use File | Settings | File Templates.
 */
public class ReplaceUtil {

    public static final String METHOD_PREFIX = "method:"; // find/replace indicating to call a method

    public static final String PARENT_KEY = "parent_key"; // %parent_key% will use parent value
    public static final String PARENT_SUMMARY = "parent_summary"; // %parent_summary% will use parent value
    public static final String PARENT_DESCRIPTION = "parent_description"; // %parent_description% will use parent
    public static final String PARENT_AFFECTED_VERSIONS = "parent_affected_versions"; // %parent_affected_versions%
    public static final String PARENT_FIXED_VERSIONS = "parent_fixed_versions"; // %parent_fixed_versions%

    public static final String ORIGINAL_KEY = "original_key";
    public static final String ORIGINAL_SUMMARY = "original_summary";
    public static final String ORIGINAL_DESCRIPTION = "original_description";
    public static final String ORIGINAL_AFFECTED_VERSIONS = "original_affected_versions";
    public static final String ORIGINAL_FIXED_VERSIONS = "original_fixed_versions";

    public static final String ORIGINAL_REPORTER = "original_reporter";
    public static final String ORIGINAL_ASSIGNEE = "original_assignee";

    public static final String DUE_DATE = "dueDate"; // standard field, but need to format it as DMY instead of including time
    public static final String CREATED_DATE = "created"; // standard field, but need to format it as COMPLETE instead of including time
    public static final String UPDATED_DATE = "updated"; // standard field, but need to format it as COMPLETE instead of including time
    public static final String RESOLUTION_DATE = "resolutionDate"; // standard field, but need to format it as COMPLETE instead of including time

    public static final String TRANSITION_COMMENT = "transition_comment"; // %transition_comment%

    public static final String MINE_STATUS = "mine_status";
    public static final String THEIR_STATUS = "their_status";

    public static final String CURRENT_USER = "current_user";

    private final static Logger log = Logger.getLogger(ReplaceUtil.class);

    public static String findReplace(final String inputText, final Issue parentIssue, final Issue originalIssue, @Nullable List<Issue> additional, final Map<String, Object> transientVariables) {

        if (additional == null) additional = new ArrayList<Issue>();

        ApplicationProperties applicationProperties = ComponentAccessor.getApplicationProperties();
        CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
        JiraAuthenticationContext jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext();

        StringBuilder result = new StringBuilder();
        String input = inputText == null ? "" : inputText.trim();

        if (!input.contains("%")) {
            result.append(input);
        } else {

            // Look for custom fields or special values
            int index1 = 0;
            int index2 = 0;
            while (index1 != -1 && index2 != -1) {
                index1 = input.indexOf("%", index2);
                if (index1 != -1) {
                    result.append(input.substring(index2, index1));
                    index2 = input.indexOf("%", index1 + 1);
                    if (index2 != -1) {
                        String name = input.substring(index1 + 1, index2);
                        log.debug("find replace lookup for: " + name);
                        CustomField customField = customFieldManager.getCustomFieldObjectByName(name);
                        if (customField == null) { // if custom field not found, it will be null
                            customField = customFieldManager.getCustomFieldObject(name); // try by text id - customfield_10010
                        }
                        // Do not do lookup by long id here - it is too ambiguous !!!
                        if (customField != null) { // if custom field not found, it will be null
                            log.debug("custom field found: " + customField.getId() + ", type: " + customField.getCustomFieldType().getDescription());
                            String typeKey = customField.getCustomFieldType().getKey();
                            // Handle date fields differently so the that date representation is more like what the user expectes to see
                            if (typeKey.equals("com.atlassian.jira.plugin.system.customfieldtypes:datepicker")) {
                                result.append(getTimestampAsString((Timestamp) parentIssue.getCustomFieldValue(customField),
                                        applicationProperties.getDefaultBackedString(APKeys.JIRA_LF_DATE_DMY)));
                            } else if (typeKey.equals("com.atlassian.jira.plugin.system.customfieldtypes:datetime")) {
                                result.append(getTimestampAsString((Timestamp) parentIssue.getCustomFieldValue(customField),
                                        applicationProperties.getDefaultBackedString(APKeys.JIRA_LF_DATE_COMPLETE)));
                            } else {
                                result.append(CreateUtilities.clean(parentIssue.getCustomFieldValue(customField)));
                            }
                        } else { // look for special values

                            if (name.equalsIgnoreCase(PARENT_KEY)) {
                                result.append(parentIssue.getKey());
                            } else if (name.equalsIgnoreCase(PARENT_SUMMARY)) {
                                result.append(CreateUtilities.clean(parentIssue.getSummary()));
                            } else if (name.equalsIgnoreCase(PARENT_DESCRIPTION)) {
                                result.append(CreateUtilities.clean(parentIssue.getDescription()));
                            } else if (name.equalsIgnoreCase(PARENT_AFFECTED_VERSIONS)) {
                                result.append(CreateUtilities.clean(parentIssue.getAffectedVersions())); // gets a text representation of list
                            } else if (name.equalsIgnoreCase(PARENT_FIXED_VERSIONS)) {
                                result.append(CreateUtilities.clean(parentIssue.getFixVersions())); // gets a text representation of list

                            } else if (name.equalsIgnoreCase(ORIGINAL_KEY)) {
                                result.append(originalIssue.getKey());
                            } else if (name.equalsIgnoreCase(ORIGINAL_SUMMARY)) {
                                result.append(CreateUtilities.clean(originalIssue.getSummary()));
                            } else if (name.equalsIgnoreCase(ORIGINAL_DESCRIPTION)) {
                                result.append(CreateUtilities.clean(originalIssue.getDescription()));
                            } else if (name.equalsIgnoreCase(ORIGINAL_AFFECTED_VERSIONS)) {
                                result.append(CreateUtilities.clean(originalIssue.getAffectedVersions())); // gets a text representation of list
                            } else if (name.equalsIgnoreCase(ORIGINAL_FIXED_VERSIONS)) {
                                result.append(CreateUtilities.clean(originalIssue.getFixVersions())); // gets a text representation of list

                            } else if (name.equalsIgnoreCase(ORIGINAL_REPORTER)) {
                                result.append(originalIssue.getReporterId());
                            } else if (name.equalsIgnoreCase(ORIGINAL_ASSIGNEE)) {
                                result.append(originalIssue.getAssigneeId());

                            } else if (name.equalsIgnoreCase(DUE_DATE)) {
                                result.append(getTimestampAsString(originalIssue.getDueDate(),
                                        applicationProperties.getDefaultBackedString(APKeys.JIRA_LF_DATE_DMY)));
                            } else if (name.equalsIgnoreCase(CREATED_DATE)) {
                                result.append(getTimestampAsString(originalIssue.getCreated(),
                                        applicationProperties.getDefaultBackedString(APKeys.JIRA_LF_DATE_COMPLETE)));
                            } else if (name.equalsIgnoreCase(UPDATED_DATE)) {
                                result.append(getTimestampAsString(originalIssue.getUpdated(),
                                        applicationProperties.getDefaultBackedString(APKeys.JIRA_LF_DATE_COMPLETE)));
                            } else if (name.equalsIgnoreCase(RESOLUTION_DATE)) {
                                result.append(getTimestampAsString(originalIssue.getResolutionDate(),
                                        applicationProperties.getDefaultBackedString(APKeys.JIRA_LF_DATE_COMPLETE)));

                            } else if (name.equalsIgnoreCase(TRANSITION_COMMENT)) {
                                result.append(CreateUtilities.clean(transientVariables.get("comment"))); // gets a text representation of list
                            } else if (name.equalsIgnoreCase(MINE_STATUS)) {
                                result.append(getWorkStatus(originalIssue, true));
                            } else if (name.equalsIgnoreCase(THEIR_STATUS)) {
                                for (Issue add : additional) {
                                    result.append(getWorkStatus(add, true));
                                }
                                log.warn("result after theirstatus : " + result.toString());
                            } else if (name.equalsIgnoreCase(CURRENT_USER)) {
                                result.append(jiraAuthenticationContext.getLoggedInUser().getDisplayName());
                            } else { // handle field names and method names
                                String methodName = "";
                                try {
                                    if (name.startsWith(METHOD_PREFIX)) {
                                        methodName = name.substring(METHOD_PREFIX.length());
                                    } else { // assume it is a field name
                                        methodName = "get" + (name.substring(0, 1)).toUpperCase() + name.substring(1);
                                    }
                                    Method method = parentIssue.getClass().getMethod(methodName, new Class[] {});
                                    result.append(CreateUtilities.clean(method.invoke(parentIssue, (Object[]) null)));
                                    // ignore any errors trying to get values, just log errors
                                } catch (NoSuchMethodException e) {
                                    log.warn("Cannnot find method: " + methodName + ", ignore.", e);
                                } catch (InvocationTargetException e) {
                                    log.warn("InvocationTargetException: " + methodName + ", ignore.", e);
                                } catch (IllegalAccessException e) {
                                    log.warn("IllegalAccessException: " + methodName + ", ignore.", e);
                                } catch (Exception e) { // don't let anything mess this up, just ignore and go on!!!
                                    log.warn("Exception: " + methodName + ", ignore.", e);
                                }
                            }
                        }
                        index2++; // go past the ending %
                    }
                } else {
                    result.append(input.substring(index2));
                }
            }
        }
        return result.toString();
    }

    public static String getWorkStatus(Issue i, boolean showPositive) {
        String message;
        boolean showSigns = showPositive;
        if (!isResolutionFixed(i)) {
            boolean positive = false;
            message = getResolutionMessage(i, positive, showSigns);
        }
        else if (showPositive) {
            boolean positive = true;
            message = getResolutionMessage(i, positive, showSigns);
        } else message = "";
        return message;
    }

    public static Map<String, String> resolutionStatusTemplates = new HashMap<String, String>();
    public static Map<String, String> goodResolutionStatusTemplates = new HashMap<String, String>();

    static {
        resolutionStatusTemplates.put("ProductOwnerReview", "Продуктовый департамент историю не проверил или не принял");
        resolutionStatusTemplates.put("DesignReview", "Отдел дизайна историю не проверил или не принял");
        resolutionStatusTemplates.put("CompTest", "Компонентное тестирование истории не выполнено");
        resolutionStatusTemplates.put("DocTest", "Тестирование документации по истории не выполнено");
        resolutionStatusTemplates.put("CrossTest", "Перекрёстное тестирование истории не выполнено");
        resolutionStatusTemplates.put("CodeReview", "Код-ревью истории не выполнен");
        resolutionStatusTemplates.put("IntegrTest", "Интеграционное тестирование истории не выполнено");
        resolutionStatusTemplates.put("CreateTechDocument", "Техническая документация не подготовлена");
        resolutionStatusTemplates.put("CreateTechDocumentMB", "Техническая документация не подготовлена");
        resolutionStatusTemplates.put("CodeReviewMB", "Код ревью не готов");
        resolutionStatusTemplates.put("Вопрос", "Не получен ответ");
        resolutionStatusTemplates.put(null, "Отрицательная или отсутствующая резолюция");

        goodResolutionStatusTemplates.put("ProductOwnerReview", "Продуктовый департамент историю проверил и принял");
        goodResolutionStatusTemplates.put("DesignReview", "Отдел дизайна историю проверил и принял");
        goodResolutionStatusTemplates.put("CompTest", "Компонентное тестирование истории выполнено");
        goodResolutionStatusTemplates.put("DocTest", "Тестирование документации по истории выполнено");
        goodResolutionStatusTemplates.put("CrossTest", "Перекрёстное тестирование истории выполнено");
        goodResolutionStatusTemplates.put("CodeReview", "Код-ревью истории выполнен");
        goodResolutionStatusTemplates.put("IntegrTest", "Интеграционное тестирование истории выполнено");
        goodResolutionStatusTemplates.put("CreateTechDocument", "Техническая документация подготовлена");
        goodResolutionStatusTemplates.put("CreateTechDocumentMB", "Техническая документация подготовлена");
        goodResolutionStatusTemplates.put("CodeReviewMB", "Код ревью готов");
        goodResolutionStatusTemplates.put("Вопрос", "Ответ получен");
        goodResolutionStatusTemplates.put(null, "Положительная резолюция");

    }

    private static String getResolutionMessage(Issue i, boolean positive, boolean showSigns) {
        String t = i.getIssueTypeObject().getName();
        String message;
        StringBuilder prefix = new StringBuilder();
        if (!positive) {
            String p = resolutionStatusTemplates.get(t);
            if (p == null) p = resolutionStatusTemplates.get(null);
            if (showSigns) prefix.append("(!) ");
            prefix.append(p);

        } else {
            String p = goodResolutionStatusTemplates.get(t);
            if (p == null) p = goodResolutionStatusTemplates.get(null);
            if (showSigns) prefix.append("(+) ");
            prefix.append(p);
        }
        User loggedInUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        if (showSigns && "DocTest".equals(t)) prefix.append(" ").append(loggedInUser.getName()).append(": ");

        message = prefix + " " + i.getKey() + '\n';

        return message;
    }

    private static boolean isResolutionFixed(Issue i) {
        Resolution yoba = i.getResolutionObject();
        if (yoba == null || !"1".equals(yoba.getId())) return false;
        else return true;
    }

    public static String getTimestampAsString(final Timestamp timestamp, final String dateFormat) {
        return timestamp == null ? "" : getDateAsString(new Date(timestamp.getTime()), dateFormat);
    }

    public static String getDateAsString(final Date date, final String dateFormat) {
        return date == null ? "" : getDateFormat(dateFormat).format(date);
    }

    public static SimpleDateFormat getDateFormat(final String dateFormat) {
        return ((dateFormat == null || dateFormat.equals("")) ? new SimpleDateFormat() : new SimpleDateFormat(dateFormat));
    }

}
