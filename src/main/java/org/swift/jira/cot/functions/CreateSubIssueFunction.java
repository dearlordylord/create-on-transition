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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.fields.config.manager.FieldConfigSchemeManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.searchers.IssueSearcher;
import com.atlassian.jira.web.bean.PagerFilter;
import com.opensymphony.workflow.spi.Step;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ofbiz.core.entity.GenericValue;

import com.atlassian.core.util.InvalidDurationException;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bc.issue.worklog.TimeTrackingConfiguration;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFactory;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.index.IndexException;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.ImportUtils;
import com.atlassian.jira.util.JiraDurationUtils;
import com.atlassian.jira.util.LocaleParser;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;

import static org.swift.jira.cot.functions.util.ReplaceUtil.*;

public class CreateSubIssueFunction extends AbstractJiraFunctionProvider {
    protected static Log log = LogFactory.getLog(CreateSubIssueFunction.class);


    public static final String USER_TEXT[] = {"parent issue's reporter", "parent issue's assignee", "project lead", "unassigned", "specific user",
            "current user"}; // words used on the view screen

    public static final int VERSIONS_NONE = 0;
    public static final int VERSIONS_AFFECTED_VERSIONS = 1;
    public static final int VERSIONS_FIXED_VERSIONS = 2;
    public static final int VERSIONS_SPECIFIC = 3;

    // words used on the view screen
    public static final String VERSIONS_TEXT[] = {"none", "parent issue's affected versions", "parent issue's fixed versions", "specific versions"};

    public static final int COMPONENTS_NONE = 0;
    public static final int COMPONENTS_PARENT = 1;
    public static final int COMPONENTS_SPECIFIC = 3;

    // words used on the view screen
    public static final String COMPONENTS_TEXT[] = {"none", "parent issue's components", "not used", "specific components"};

    // Due date
    public static final int DUE_DATE_NONE = 0;
    public static final int DUE_DATE_PARENT = 1;
    public static final int DUE_DATE_SPECIFIC = 2;
    public static final int DUE_DATE_DEFAULT = DUE_DATE_NONE;
    public static final String DUE_DATE_TEXT[] = {"none", "parent issue's due date", "specific due date"}; // words used on the view screen
    public static final long DAY_MILLISECONDS = 1000 * 60 * 60 * 24; // milliseconds in a day

    protected final CustomFieldManager customFieldManager;
    protected final SubTaskManager subTaskManager;
    protected final IssueManager issueManager;
    protected final IssueFactory issueFactory;
    protected final ConstantsManager constantsManager;
    protected final ApplicationProperties applicationProperties;
    protected final UserUtil userUtil;
    protected final JiraAuthenticationContext authenticationContext;
    protected final I18nHelper.BeanFactory i18nBeanFactory;
    protected final EventPublisher eventPublisher;
    protected final IssueLinkManager issueLinkManager;
    private final FieldConfigSchemeManager schemeManager;
    private final SearchService searchService;

    private final ScheduledThreadPoolExecutor poolExecutor = new ScheduledThreadPoolExecutor(3);

    public CreateSubIssueFunction(final CustomFieldManager customFieldManager, final SubTaskManager subTaskManager, final IssueManager issueManager,
                                  final IssueFactory issueFactory, final ConstantsManager constantsManager, final ApplicationProperties applicationProperties,
                                  final UserUtil userUtil, final JiraAuthenticationContext authenticationContext, final I18nHelper.BeanFactory i18nBeanFactory,
                                  final EventPublisher eventPublisher, IssueLinkManager issueLinkManager, FieldConfigSchemeManager schemeManager, SearchService searchService) {
        this.customFieldManager = customFieldManager;
        this.subTaskManager = subTaskManager;
        this.issueManager = issueManager;
        this.issueFactory = issueFactory;
        this.constantsManager = constantsManager;
        this.applicationProperties = applicationProperties;
        this.userUtil = userUtil;
        this.authenticationContext = authenticationContext;
        this.i18nBeanFactory = i18nBeanFactory;
        this.eventPublisher = eventPublisher;
        this.issueLinkManager = issueLinkManager;
        this.schemeManager = schemeManager;
        this.searchService = searchService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(@SuppressWarnings("rawtypes") final Map transientVariables, @SuppressWarnings("rawtypes") final Map args, final PropertySet ps)
            throws WorkflowException {
        createSubTask(transientVariables, args);
    }

    /**
     * Create sub task
     * 
     * @param transientVariables - variables set during transition
     * @param args - createSubTask specific variables
     */
    protected void createSubTask(final Map<String, Object> transientVariables, final Map<String, String> args) {

        Issue originalIssue = getIssue(transientVariables); // issue that initiated the workflow action
        Issue parentIssue = originalIssue; // parent of subtask normally is the initiating issue, may be adjusted later

        final String notPerformJql = args.get("field.notPerformIfJql");
        log.warn("notPerformJql : " + notPerformJql);
        if (notPerformJql != null && !notPerformJql.isEmpty()) {
            final String npJqlParsed = findReplace(notPerformJql, parentIssue, originalIssue, null, transientVariables);
            log.warn("parsed jql : " + npJqlParsed);
            SearchService.ParseResult parseResult = searchService.parseQuery(authenticationContext.getLoggedInUser(), npJqlParsed);
            if (!parseResult.isValid()) {
                String err = "not valid jql notperform query : " + npJqlParsed + " for issue : " + originalIssue.getKey() + " : " + parseResult.getErrors().getErrorMessages();
                log.error(err);
                throw new RuntimeException(err);
            }
            try {
                long count = searchService.searchCount(authenticationContext.getLoggedInUser(), parseResult.getQuery());
                if (count > 0) return;
            } catch (SearchException e) {
                String err = "can't perform jql search";
                log.error(err,e);
                throw new RuntimeException("can't perform jql search", e);
            }
        }


        Step createdStep =(Step) transientVariables.get("createdStep");
        List<? extends Step> currentSteps = (List<? extends Step>) transientVariables.get("currentSteps");
        IssueType subIssueType = constantsManager.getIssueTypeObject(args.get("field.subIssueTypeId"));
        IssueType parentIssueType = parentIssue.getIssueTypeObject();
        boolean parentIsCreated = true;
        if (currentSteps != null && createdStep != null) {
            for (Step currentStep : currentSteps) {
                if (currentStep.getId() == createdStep.getId()) {
                    parentIsCreated = false;
                    break;
                }
            }
        }

        if (!parentIsCreated && subIssueType.equals(parentIssueType)) {
            log.error("presumably loop here, ignore for issue : " + parentIssue);
            return;
        }

        // if parent is already a subtask, then create subtask should create sibbling instead when this is true otherwise ignore
        boolean allowSibblingCreate = (1 == CreateUtilities.getInt(args.get("field.createSibbling"), 0));

        // if (parentIssue.isSubTask()) { // doesn't work for 'createIssue' transition, use issuetype instead
        if (parentIssueType.isSubTask()) { // cut off trying to subtask a subtask, no error
            if (allowSibblingCreate) {
                parentIssue = issueManager.getIssueObject(parentIssue.getParentId());
            } else {
                log.debug("Parent issue is a subtask, so can't create subtask for: " + parentIssue.getKey());
                return;
            }
        }
        log.debug("Create a sub task for parent issue: " + parentIssue.getKey() + ", initiating issue was: " + originalIssue.getKey());

        final MutableIssue issueObject = issueFactory.getIssue();
        issueObject.setProjectId(parentIssue.getProjectObject().getId());
        issueObject.setIssueTypeObject(subIssueType);
        issueObject.setSecurityLevelId(parentIssue.getSecurityLevelId());

        // Priority
        int priority = CreateUtilities.getInt(args.get("field.subIssuePriorityId"), 0);
        if (priority == 0) {
            issueObject.setPriority(parentIssue.getPriority());
        } else {
            issueObject.setPriorityId(constantsManager.getPriorityObject(args.get("field.subIssuePriorityId")).getId());
        }

        // Reporter
        int reporter = CreateUtilities.getInt(args.get("field.subIssueReporter"), CreateUtilities.USER_CURRENT); // default to current user
        issueObject.setReporter(CreateUtilities.getUser(reporter, args.get("field.specificReporter"), parentIssue, originalIssue, transientVariables));

        // Assignee
        int assignee = CreateUtilities.getInt(args.get("field.subIssueAssignee"), CreateUtilities.USER_ASSIGNEE); // default to assignee
        issueObject.setAssignee(CreateUtilities.getUser(assignee, args.get("field.specificAssignee"), parentIssue, originalIssue, transientVariables));

        // Summary
        issueObject.setSummary(findReplace(args.get("field.subIssueSummary"), parentIssue, originalIssue, null, transientVariables));

        // Description
        issueObject.setDescription(findReplace(args.get("field.subIssueDescription"), parentIssue, originalIssue, null, transientVariables));

        // Affected versions
        int affectedVersions = CreateUtilities.getInt(args.get("field.subIssueAffectedVersions"), CreateSubIssueFunction.VERSIONS_NONE); // default to none
        issueObject.setAffectedVersions(getVersions(affectedVersions, args.get("field.specificAffectedVersions"), parentIssue, originalIssue,
                transientVariables));

        // Fixed versions
        int fixedVersions = CreateUtilities.getInt(args.get("field.subIssueFixedVersions"), CreateSubIssueFunction.VERSIONS_NONE); // default to none
        issueObject.setFixVersions(getVersions(fixedVersions, args.get("field.specificFixedVersions"), parentIssue, originalIssue, transientVariables));

        // Components - default to copy parents this was the original default so keep it
        int components = CreateUtilities.getInt(args.get("field.subIssueComponents"), CreateSubIssueFunction.COMPONENTS_PARENT);
        issueObject.setComponents(getComponents(components, args.get("field.specificComponents"), parentIssue, originalIssue, transientVariables));

        // Due date
        int dueDateOffset = CreateUtilities.getInt(args.get("field.dueDateOffset"), 0);
        int dueDate = CreateUtilities.getInt(args.get("field.subIssueDueDate"), DUE_DATE_DEFAULT);
        issueObject.setDueDate(getDueDate(dueDate, args.get("field.specificDueDate"), dueDateOffset, parentIssue, originalIssue, transientVariables));

        // Original estimate
        Long originalEstimate = getTimeDuration(args.get("field.subIssueOriginalEstimate"), parentIssue, originalIssue, transientVariables);
        if (originalEstimate != null) {
            issueObject.setOriginalEstimate(originalEstimate);
            issueObject.setEstimate(originalEstimate);
        }

        // Copy parent custom fields - comma separated list of custom field names or ids
        copyParentFields(issueObject, args.get("field.copyParentFields"), parentIssue, originalIssue, transientVariables);

        // Set custom field value
        setCustomFieldValue(issueObject, args.get("field.customField1Name"), args.get("field.customField1Value1"), args.get("field.customField1Value2"),
                parentIssue, originalIssue, transientVariables);
        setCustomFieldValue(issueObject, args.get("field.customField2Name"), args.get("field.customField2Value1"), args.get("field.customField2Value2"),
                parentIssue, originalIssue, transientVariables);
        setCustomFieldValue(issueObject, args.get("field.customField3Name"), args.get("field.customField3Value1"), args.get("field.customField3Value2"),
                parentIssue, originalIssue, transientVariables);

        final Issue finalParent = parentIssue;
        final User creator = authenticationContext.getLoggedInUser();

        // Now create the subtask
        poolExecutor.schedule(new Runnable() {

            @Override
            public void run() {
                Map<String, Object> params = new HashMap<String, Object>();
                params.put("issue", issueObject);

                boolean previousIndexSetting = ImportUtils.isIndexIssues();
                try {
                    Issue subTask = issueManager.createIssueObject(creator, params);
                    if (issueObject.getIssueTypeObject().isSubTask())
                        subTaskManager.createSubTaskIssueLink(finalParent, subTask, creator);
                    final String issueLinkTypeIdS = args.get("field.issueLinkTypeId");
                    if (issueLinkTypeIdS != null && !"0".equals(issueLinkTypeIdS)) {

                        int direction = CreateUtilities.getInt(args.get("field.issueLinkDirection"), 0);
                        Direction dir;
                        if (direction == 0) {
                            dir = Direction.To;
                        } else {
                            dir = Direction.From;
                        }
                        final FromTo fromTo = getFromTo(finalParent, dir, subTask);
                        final Long issueLinkTypeId = Long.parseLong(issueLinkTypeIdS);
                        final Integer linkDepth = CreateUtilities.getInt(args.get("field.linkDepth"), 1);
                        if (linkDepth > 1) {
                            Integer addDepth = linkDepth - 1;
                            createParentsLinks(finalParent, subTask, issueLinkTypeId, dir, addDepth, new HashSet<Issue>());
                        }
                        Runnable createLink = createLinkRunnable(fromTo.getFrom().getId(), fromTo.getTo().getId(), issueLinkTypeId);
                        if (finalParent.getId() != null) {
                            createLink.run();
                        } else {
                            //maybe add run service later
                            log.error("can't create issue link because parent issue isn't created yet; " +
                                    "please move postfunction below 'create issue' function in transition settings");
                        }

                    }


                    // Not sure about this part - here are some references
                    // - http://forums.atlassian.com/thread.jspa?messageID=257362255
                    // - http://wiki.customware.net/repository/pages/viewpage.action?pageId=8093744
                    // - http://forums.atlassian.com/thread.jspa?messageID=257362762

                    ImportUtils.setIndexIssues(true);  // temporarily set

                    // This is gone in 4.3, doesn't seem to have a replacement
                    // ManagerFactory.getCacheManager().flush(CacheManager.ISSUE_CACHE, subTask);
                    ComponentManager.getInstance().getIndexManager().reIndex(subTask);
                    ImportUtils.setIndexIssues(previousIndexSetting);
                } catch (CreateException e) {
                    log.error("Could not create sub-task", e);
                } catch (IndexException e) {
                    log.error("Index exception", e);
                } catch (Exception e) {
                    log.error("Unexpected exception", e);
                } finally {
                    ImportUtils.setIndexIssues(previousIndexSetting); // return to previous state
                }
            }
        }, 3, TimeUnit.SECONDS);

    }

    private Runnable createLinkRunnable(final Long from, final Long to, final Long linkType) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    issueLinkManager.createIssueLink(from, to, linkType, 0L, authenticationContext.getLoggedInUser());
                } catch (CreateException e) {
                    log.error("error on creating subissue", e);
                }
            }
        };
    }

    private void createParentsLinks(Issue parentIssue, Issue subtask, Long issueLinkTypeId, Direction dir, int addDepth, Set<Issue> linked) {
        if (addDepth == 0) return;
        // else if addDepth < 0 recurse until the end
        List<IssueLink> allLinks = null;
        switch (dir) {
            case To: allLinks = issueLinkManager.getInwardLinks(parentIssue.getId()); break;
            case From: allLinks = issueLinkManager.getOutwardLinks(parentIssue.getId()); break;
        }
        // ------------ filter
        List<IssueLink> links = new LinkedList<IssueLink>(); // links should ALWAYS be in linked list!
        for (IssueLink il : allLinks) {
            if (il.getIssueLinkType().getId().equals(issueLinkTypeId)) {
                links.add(il);
            }
        }
        List<Runnable> createLinks = new LinkedList<Runnable>(); // why not linked list dude
        List<Issue> nextParents = new LinkedList<Issue>();
        // ------------ create links on this layer
        for (IssueLink il : links) {
            Issue newParent = null;
            switch(dir) {
                case To: newParent = il.getSourceObject(); break;
                case From: newParent = il.getDestinationObject(); break;
            }
            if (linked.contains(newParent)) {
                log.warn("maybe loop here, continuing");
                continue;
            }
            FromTo fromTo = getFromTo(newParent, dir, subtask);
            Runnable createLink = createLinkRunnable(fromTo.getFrom().getId(), fromTo.getTo().getId(), issueLinkTypeId);
            createLinks.add(createLink);
            nextParents.add(newParent);
        }
        for (Runnable r : createLinks) {
            r.run();
        }
        // ------------- loop defence
        linked.add(parentIssue);
        // -------------- recursion
        for (Issue nextParent : nextParents) {
            createParentsLinks(nextParent, subtask, issueLinkTypeId, dir, addDepth - 1, linked);
        }
    }

    private enum Direction {
        To, From
    }

    private static FromTo getFromTo(Issue parentIssue, Direction direction, Issue subTask) {
        Issue from;
        Issue to;
        switch (direction) {
            case To: from = parentIssue; to = subTask; break;
            case From: from = subTask; to = parentIssue; break;
            default: throw new RuntimeException("some sort of problem with directions");
        }
        return new FromTo(from, to);
    }

    private static class FromTo {
        Issue from;
        Issue to;

        private FromTo(Issue from, Issue to) {
            this.from = from;
            this.to = to;
        }

        public Issue getFrom() {
            return from;
        }

        public void setFrom(Issue from) {
            this.from = from;
        }

        public Issue getTo() {
            return to;
        }

        public void setTo(Issue to) {
            this.to = to;
        }
    }



    /**
     * Get versions based on choice value from configuration
     * 
     * @param choice - choice value
     * @param specificVersions - versions when choice is VERSIONS_SPECIFIC
     * @param parentIssue - needed to provide information for some choices
     * @return versions or null
     */
    protected Collection<Version> getVersions(final int choice, final String specificVersions, final Issue parentIssue, final Issue originalIssue,
            final Map<String, Object> transientVariables) {
        Collection<Version> versions = null; // none
        switch (choice) {

        case VERSIONS_NONE: // 0
            break;

        case VERSIONS_AFFECTED_VERSIONS: // 1
            versions = parentIssue.getAffectedVersions();
            break;

        case VERSIONS_FIXED_VERSIONS: // 2
            versions = parentIssue.getFixVersions();
            break;

        case VERSIONS_SPECIFIC: // 3
            versions = getVersionList(findReplace(specificVersions, parentIssue, originalIssue, null, transientVariables), parentIssue);
            break;
        }
        return versions;
    }

    /**
     * Get version list from comma separated string of version names - warn on invalid entries
     * 
     * @param versions
     * @param issue
     * @return
     */
    protected List<Version> getVersionList(final String versions, final Issue issue) {
        List<Version> list = new ArrayList<Version>();
        Collection<Version> projectVersions = issue.getProjectObject().getVersions(); // valid for project
        String versionArray[] = versions.split(",");
        for (int i = 0; i < versionArray.length; i++) {
            String versionString = versionArray[i].trim();
            Version version = findVersion(versionString, projectVersions);
            if (version == null) {
                log.warn("Could not find version: '" + versionString + "' for project: " + issue.getProjectObject().getName());
            } else {
                list.add(version);
            }
        }
        return list;
    }

    /**
     * Find version by name from a list
     */
    protected Version findVersion(final String name, final Collection<Version> list) {
        for (Version item : list) {
            if (name.equals(item.getName())) {
                return item;
            }
        }
        return null;
    }

    /**
     * Get components based on choice value from configuration
     * 
     * @param choice - choice value
     * @param specificComponents - components when choice is VERSIONS_SPECIFIC
     * @param parentIssue - needed to provide information for some choices
     * @return versions or null
     */
    protected Collection<GenericValue> getComponents(final int choice, final String specificComponents, final Issue parentIssue, final Issue originalIssue,
            final Map<String, Object> transientVariables) {
        Collection<GenericValue> components = null; // none

        log.debug("component choice: " + choice);

        switch (choice) {

        case COMPONENTS_NONE: // 0
            break;

        case COMPONENTS_PARENT: // 1
            components = parentIssue.getComponents();
            break;

        // case 2: // 2 not used

        case COMPONENTS_SPECIFIC: // 3
            components = new ArrayList<GenericValue>();
            String componentsArray[] = findReplace(specificComponents, parentIssue, originalIssue, null, transientVariables).split(",");
            @SuppressWarnings("deprecation")
            // need API change, see getComponentsNew for new implementation
            Collection<GenericValue> projectComponents = parentIssue.getProjectObject().getComponents(); // valid for project
            for (int i = 0; i < componentsArray.length; i++) {
                String string = componentsArray[i].trim();
                GenericValue component = findComponent(string, projectComponents);
                if (component == null) {
                    log.warn("Could not find component: '" + string + "' for project: " + parentIssue.getProjectObject().getName());
                } else {
                    components.add(component);
                }
            }
            break;
        }
        return components;
    }

    /**
     * Find version by name from a list
     */
    protected GenericValue findComponent(final String name, final Collection<GenericValue> list) {
        for (GenericValue item : list) {
            if (name.equals(item.getString("name"))) {
                return item;
            }
        }
        return null;
    }

    /**
     * CAN'T USE projectComponents until API support setting components with this collection. Get components based on choice value from configuration
     * 
     * @param choice - choice value
     * @param specificComponents - components when choice is VERSIONS_SPECIFIC
     * @param parentIssue - needed to provide information for some choices
     * @return versions or null
     */
    protected Collection<ProjectComponent> getComponentsNew(final int choice, final String specificComponents, final Issue parentIssue,
            final Issue originalIssue, final Map<String, Object> transientVariables) {
        Collection<ProjectComponent> components = null; // none

        log.debug("component choice: " + choice);

        switch (choice) {

        case COMPONENTS_NONE: // 0
            break;

        case COMPONENTS_PARENT: // 1
            components = parentIssue.getComponentObjects();
            break;

        // case 2: // 2 not used

        case COMPONENTS_SPECIFIC: // 3
            components = new ArrayList<ProjectComponent>();
            String componentsArray[] = findReplace(specificComponents, parentIssue, originalIssue, null, transientVariables).split(",");
            Collection<ProjectComponent> projectComponents = parentIssue.getProjectObject().getProjectComponents(); // valid for project
            for (int i = 0; i < componentsArray.length; i++) {
                String string = componentsArray[i].trim();
                ProjectComponent component = findComponentNew(string, projectComponents);
                if (component == null) {
                    log.warn("Could not find component: '" + string + "' for project: " + parentIssue.getProjectObject().getName());
                } else {
                    components.add(component);
                }
            }
            break;
        }
        return components;
    }

    /**
     * Find version by name from a list
     */
    protected ProjectComponent findComponentNew(final String name, final Collection<ProjectComponent> list) {
        for (ProjectComponent item : list) {
            if (name.equals(item.getName())) {
                return item;
            }
        }
        return null;
    }

    /**
     * Get due date
     * 
     * @param choice - choice value
     * @param specificValue
     * @param offset in days
     * @param parentIssue - needed to provide information for some choices
     * @return due date in timestamp format
     */
    protected Timestamp getDueDate(final int choice, final String specificValue, final int offset, final Issue parentIssue, final Issue originalIssue,
            final Map<String, Object> transientVariables) {

        log.debug("get due date choice: " + choice + ", specific: " + specificValue + ", parent due date: " + parentIssue.getDueDate());

        Timestamp value = null;

        switch (choice) {

        case DUE_DATE_NONE: // 0 - default
            break;

        case DUE_DATE_PARENT: // 1
            value = parentIssue.getDueDate();
            if ((value != null) && (offset != 0)) {
                value = new Timestamp(parentIssue.getDueDate().getTime() + (offset * DAY_MILLISECONDS));
            }
            break;

        case DUE_DATE_SPECIFIC: // 2

            String string = findReplace(specificValue, parentIssue, originalIssue, null, transientVariables);
            value = getTimestamp(string, offset, applicationProperties.getDefaultBackedString(APKeys.JIRA_LF_DATE_DMY));
            break;
        }
        return value;
    }

    /**
     * Get a timestamp (or null) based on string and offset
     * 
     * @param string - date in application default format
     * @param offset - offset in days
     * @return timestamp or null if anything was invalid
     */
    protected Timestamp getTimestamp(final String string, final int offset, final String format) {
        Timestamp result = null;
        try {
            Date dueDate = new SimpleDateFormat(format).parse(string);
            result = new Timestamp(dueDate.getTime() + (offset * DAY_MILLISECONDS));
        } catch (ParseException exception) {
            log.debug("Date parse error for: " + string + ", format: " + format);
        }
        return result;
    }








    /**
     * Get duration value from value in time tracking format
     * 
     * @param value - user provided value in system default time tracking format
     * @param parentIssue
     * @param transientVariables
     * @return duration in seconds or null
     */
    protected Long getTimeDuration(final String value, final Issue parentIssue, final Issue originalIssue, final Map<String, Object> transientVariables) {
        Long duration = null;
        if ((value != null) && !value.trim().isEmpty()) {
            String durationString = findReplace(value, parentIssue, originalIssue, null, transientVariables);

            JiraDurationUtils durationUtils = new JiraDurationUtils(applicationProperties, authenticationContext,
                    new TimeTrackingConfiguration.PropertiesAdaptor(applicationProperties), eventPublisher, i18nBeanFactory);
            try {
                // For consistency across all users, we need the original estimate to be in the system default format and not user specific
                // Locale defaultLocale = LocaleManager..DEFAULT_LOCALE when we are at JIRA 5 api level
                Locale defaultLocale = LocaleParser.parseLocale(applicationProperties.getString(APKeys.JIRA_I18N_DEFAULT_LOCALE));
                if (defaultLocale == null) {
                    defaultLocale = Locale.ENGLISH;
                }
                // Convert time tracking string to seconds
                duration = durationUtils.parseDuration(durationString, defaultLocale);
            } catch (InvalidDurationException e) {
                log.warn("Invalid duration specified for original estimate: " + durationString + ", ignore.", e);
            }
        }
        return duration;
    }

    /**
     * Copy parent custom fields to subtask for each field requested
     * 
     * @param value - user provided comma separated list of custom field names or ids (customfield_10010) or id (long)
     * @param parentIssue
     * @param transientVariables
     */
    protected void copyParentFields(final MutableIssue issue, final String value, final Issue parentIssue, final Issue originalIssue,
            final Map<String, Object> transientVariables) {
        if ((value != null) && !value.trim().isEmpty()) {
            String fields[] = findReplace(value, parentIssue, originalIssue, null, transientVariables).split(",");
            for (String name : fields) {
                CustomField customField = getCustomField(name);
                if (customField != null) {
                    Object v = parentIssue.getCustomFieldValue(customField);
                    if (v != null) {
                        issue.setCustomFieldValue(customField, v);
                    }
                }
            }
        }
    }

    /**
     * Set custom field value
     * 
     * @param issue
     * @param customFieldName
     * @param inValue1 - custom field value 1
     * @param inValue2 - custom field value 2
     * @param parentIssue
     * @param transientVariables
     */
    protected void setCustomFieldValue(final MutableIssue issue, final String customFieldName, final String inValue1, final String inValue2,
            final Issue parentIssue, final Issue originalIssue, final Map<String, Object> transientVariables) {
        if ((customFieldName != null) && !customFieldName.trim().isEmpty()) {
            String value1 = findReplace(inValue1, parentIssue, originalIssue, null, transientVariables);
            String value2 = findReplace(inValue2, parentIssue, originalIssue, null, transientVariables);

            CustomField customField = getCustomField(customFieldName);
            if (customField != null) {
                CustomFieldType type = customField.getCustomFieldType();
                String key = type.getKey();
                log.debug("set custom field: " + customField.getName() + ", type: " + key + ", description: " + type.getDescription() + ", class: "
                        + type.getClass().getName());

                // userpicker types
                if (key.equals("com.atlassian.jira.plugin.system.customfieldtypes:userpicker")) {
                    issue.setCustomFieldValue(customField, userUtil.getUserObject(findReplace(value1, parentIssue, originalIssue, null, transientVariables)));
                    log.debug("custom field value set: " + userUtil.getUserObject(findReplace(value1, parentIssue, originalIssue, null, transientVariables)));

                    // datepicker types
                } else if (key.equals("com.atlassian.jira.plugin.system.customfieldtypes:datepicker")) {
                    int offset = CreateUtilities.getInt(value2, 0);
                    issue.setCustomFieldValue(customField, getTimestamp(value1, offset, applicationProperties.getDefaultBackedString(APKeys.JIRA_LF_DATE_DMY)));

                    // datetime
                } else if (key.equals("com.atlassian.jira.plugin.system.customfieldtypes:datetime")) {
                    int offset = CreateUtilities.getInt(value2, 0);
                    issue.setCustomFieldValue(customField,
                            getTimestamp(value1, offset, applicationProperties.getDefaultBackedString(APKeys.JIRA_LF_DATE_COMPLETE)));

                    // version
                } else if (key.equals("com.atlassian.jira.plugin.system.customfieldtypes:version")) {

                    List<Version> versions = getVersionList(value1, parentIssue);
                    // issue.setCustomFieldValue(customField, (versions.size() == 0) ? null : versions.get(0)); // fails on 5.1
                    issue.setCustomFieldValue(customField, (versions.size() == 0) ? null : versions); // this works on 4.3.3/4.4.4/5.1
                    log.debug("custom field value set: " + ((versions.size() == 0) ? null : versions.get(0)));

                    // multi version
                } else if (key.equals("com.atlassian.jira.plugin.system.customfieldtypes:multiversion")) {

                    List<Version> versions = getVersionList(value1, parentIssue);
                    issue.setCustomFieldValue(customField, versions);
                    log.debug("custom field value set: " + versions);

                    // multiselect fields
                } else if (key.equals("com.atlassian.jira.plugin.system.customfieldtypes:multiselect")) {
                    // issue.setCustomFieldValue(customField, value1.split(",")); // need to convert text to id
                    log.warn("multi-select fields not supported");
                    // cascade select
                } else if (key.equals("com.atlassian.jira.plugin.system.customfieldtypes:cascadeselect")) {
                    log.warn("cascade select fields not supported");

                    // single values
                } else {
                    try {
                        issue.setCustomFieldValue(customField, customField.getCustomFieldType().getSingularObjectFromString(value1));
                    } catch (Exception exception) {
                        log.warn("custom field type: " + type.getName() + " not supported.");
                    }
                }
            }
        }
    }

    /**
     * Lookup custom field by name, id name (customfield_10100) or id (long). If not found log a warning.
     * 
     * @param name
     * @return custom field or null
     */
    protected CustomField getCustomField(String name) {
        CustomField customField = null;
        if (name != null) {
            name = name.trim();
            customField = customFieldManager.getCustomFieldObjectByName(name);
            if (customField == null) { // if custom field not found, it will be null
                customField = customFieldManager.getCustomFieldObject(name); // try by text id - customfield_10010
            }
            if (customField == null) { // if custom field not found, it will be null
                try {
                    Long id = Long.parseLong(name);
                    customField = customFieldManager.getCustomFieldObject(id); // try by id - 10010
                } catch (NumberFormatException ignore) {
                }
            }
            if (customField == null) {
                log.warn("Custom field: '" + name + "' not found. Field was ignored.");
            }
        }
        return customField;
    }

    /**
     * find replace special values in text. Values should be strings or generic values or collections of same
     * 
     * <pre>
     * - %customfieldname% with value,
     * - %parent_...% with parent value,
     * - %fieldName% with field value - these are Issue fields that have a simple get method - see http://docs.atlassian.com/jira/4.2/
     * - %method:methodname% - issue method to run - I don't really see much use for this, but, it was in old code - probably will not document it
     * </pre>
     * 
     * Note there is some overlap with specific values (PARENT ...) and just using field names for the same. This is left over from earlier work, so just going
     * to leave it as is.
     * 
     * @param inputText with optional special fields %xxxx%
     * @param parentIssue
     * @return string with special fields replaced with values
     */
}
