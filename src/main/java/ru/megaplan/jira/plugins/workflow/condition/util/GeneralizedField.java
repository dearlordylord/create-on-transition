package ru.megaplan.jira.plugins.workflow.condition.util;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.IssueService.IssueResult;
import com.atlassian.jira.bc.issue.IssueService.TransitionValidationResult;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.bc.project.component.ProjectComponentManager;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.IssueInputParametersImpl;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.context.ProjectContext;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.impl.CascadingSelectCFType;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.issue.label.LabelManager;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.issue.resolution.Resolution;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.jira.workflow.JiraWorkflow;
import com.atlassian.jira.workflow.WorkflowManager;
import com.opensymphony.workflow.loader.ActionDescriptor;
import com.opensymphony.workflow.loader.ResultDescriptor;
import com.opensymphony.workflow.loader.StepDescriptor;
import com.opensymphony.workflow.loader.WorkflowDescriptor;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import org.apache.log4j.Logger;
import org.ofbiz.core.entity.GenericValue;

public class GeneralizedField
{
    public static final int PARENT = 0;
    public static final int SON = 1;
    private final CustomField customField;
    private final String name;
    private final Long idAsLong;
    private final int type;
    private final String typeName;
    private static Logger log = Logger.getLogger(GeneralizedField.class);

    private static final Map<String, Object> ephemeralStore = new HashMap();

    public GeneralizedField(CustomField customField, int relative)
    {
        this.customField = customField;

        if (relative == 0)
        {
            this.idAsLong = Long.valueOf(customField.getIdAsLong().longValue() + 40000L);
            this.name = ("Parent's " + customField.getName());
        }
        else
        {
            this.idAsLong = customField.getIdAsLong();
            this.name = customField.getName();
        }

        this.type = getCustomFieldType(customField);
        this.typeName = customField.getCustomFieldType().getName();
    }

    public GeneralizedField(Long idAsLong)
    {
        this.idAsLong = idAsLong;

        CustomFieldManager customFieldManager = ComponentManager.getInstance().getCustomFieldManager();

        if (idAsLong.longValue() >= 50000L)
        {
            this.customField = customFieldManager.getCustomFieldObject(Long.valueOf(idAsLong.longValue() - 40000L));
        }
        else
        {
            this.customField = customFieldManager.getCustomFieldObject(idAsLong);
        }

        if (this.customField != null)
        {
            if (idAsLong.longValue() >= 50000L)
            {
                this.name = ("Parent's " + this.customField.getName());
            }
            else
            {
                this.name = this.customField.getName();
            }

            this.type = getCustomFieldType(this.customField);
            this.typeName = this.customField.getCustomFieldType().getName();
        }
        else if (idAsLong.longValue() == 0L)
        {
            this.name = "Summary";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 1L)
        {
            this.name = "Description";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 3L)
        {
            this.name = "Assignee";
            this.type = 5;
            this.typeName = "JIRA User";
        }
        else if (idAsLong.longValue() == 2L)
        {
            this.name = "Assignee's full name";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 4L)
        {
            this.name = "Assignee's email";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 6L)
        {
            this.name = "Reporter";
            this.type = 5;
            this.typeName = "JIRA User";
        }
        else if (idAsLong.longValue() == 5L)
        {
            this.name = "Reporter's full name";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 7L)
        {
            this.name = "Reporter's email";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 9L)
        {
            this.name = "Date and time of creation";
            this.type = 4;
            this.typeName = "Date and time";
        }
        else if (idAsLong.longValue() == 11L)
        {
            this.name = "Date and time of last update";
            this.type = 4;
            this.typeName = "Date and time";
        }
        else if (idAsLong.longValue() == 12L)
        {
            this.name = "Due date";
            this.type = 3;
            this.typeName = "Date";
        }
        else if (idAsLong.longValue() == 14L)
        {
            this.name = "Issue type";
            this.type = 12;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 15L)
        {
            this.name = "Issue key";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 16L)
        {
            this.name = "Issue status";
            this.type = 10;
            this.typeName = "Issue status";
        }
        else if (idAsLong.longValue() == 17L)
        {
            this.name = "Priority";
            this.type = 9;
            this.typeName = "Issue priority";
        }
        else if (idAsLong.longValue() == 18L)
        {
            this.name = "Project key";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 19L)
        {
            this.name = "Project name";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 49L)
        {
            this.name = "Project description";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 50L)
        {
            this.name = "Project URL";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 51L)
        {
            this.name = "Project category";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 52L)
        {
            this.name = "Project leader";
            this.type = 5;
            this.typeName = "JIRA User";
        }
        else if (idAsLong.longValue() == 53L)
        {
            this.name = "Project leader's full name";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 54L)
        {
            this.name = "Project leader's email";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 102L)
        {
            this.name = "Keys of other issues in current project";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 20L)
        {
            this.name = "Current user";
            this.type = 5;
            this.typeName = "JIRA User";
        }
        else if (idAsLong.longValue() == 21L)
        {
            this.name = "Current user's full name";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 22L)
        {
            this.name = "Current user's email";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 68L)
        {
            this.name = "Original estimate (minutes)";
            this.type = 2;
            this.typeName = "Number";
        }
        else if (idAsLong.longValue() == 24L)
        {
            this.name = "Remaining estimate (minutes)";
            this.type = 2;
            this.typeName = "Number";
        }
        else if (idAsLong.longValue() == 25L)
        {
            this.name = "Total time spent (minutes)";
            this.type = 2;
            this.typeName = "Number";
        }
        else if (idAsLong.longValue() == 26L)
        {
            this.name = "Number of votes received";
            this.type = 2;
            this.typeName = "Number";
        }
        else if (idAsLong.longValue() == 96L)
        {
            this.name = "Keys of subtasks";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 27L)
        {
            this.name = "Number of subtasks";
            this.type = 2;
            this.typeName = "Number";
        }
        else if (idAsLong.longValue() == 98L)
        {
            this.name = "Keys of linked issues";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 99L)
        {
            this.name = "Number of linked issues";
            this.type = 2;
            this.typeName = "Number";
        }
        else if (idAsLong.longValue() == 28L)
        {
            this.name = "Issue resolution";
            this.type = 11;
            this.typeName = "Issue resolution";
        }
        else if (idAsLong.longValue() == 70L)
        {
            this.name = "Environment";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 71L)
        {
            this.name = "Attachments";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 72L)
        {
            this.name = "Attachments with details";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 73L)
        {
            this.name = "Number of attachments";
            this.type = 2;
            this.typeName = "Number";
        }
        else if (idAsLong.longValue() == 74L)
        {
            this.name = "Fixed versions";
            this.type = 14;
            this.typeName = "Versions";
        }
        else if (idAsLong.longValue() == 75L)
        {
            this.name = "Fixed versions with details";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 76L)
        {
            this.name = "Number of fixed versions";
            this.type = 2;
            this.typeName = "Number";
        }
        else if (idAsLong.longValue() == 77L)
        {
            this.name = "Affected versions";
            this.type = 14;
            this.typeName = "Versions";
        }
        else if (idAsLong.longValue() == 78L)
        {
            this.name = "Affected versions with details";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 79L)
        {
            this.name = "Number of affected versions";
            this.type = 2;
            this.typeName = "Number";
        }
        else if (idAsLong.longValue() == 80L)
        {
            this.name = "Labels";
            this.type = 15;
            this.typeName = "Labels";
        }
        else if (idAsLong.longValue() == 81L)
        {
            this.name = "Number of labels";
            this.type = 2;
            this.typeName = "Number";
        }
        else if (idAsLong.longValue() == 94L)
        {
            this.name = "Components";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 29L)
        {
            this.name = "Parent's summary";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 30L)
        {
            this.name = "Parent's description";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 31L)
        {
            this.name = "Parent's assignee";
            this.type = 5;
            this.typeName = "JIRA User";
        }
        else if (idAsLong.longValue() == 32L)
        {
            this.name = "Parent's assignee's full name";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 33L)
        {
            this.name = "Parent's assignee's email";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 34L)
        {
            this.name = "Parent's reporter";
            this.type = 5;
            this.typeName = "JIRA User";
        }
        else if (idAsLong.longValue() == 35L)
        {
            this.name = "Parent's reporter's full name";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 36L)
        {
            this.name = "Parent's reporter's email";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 55L)
        {
            this.name = "Parent's date and time of creation";
            this.type = 4;
            this.typeName = "Date and time";
        }
        else if (idAsLong.longValue() == 56L)
        {
            this.name = "Parent's date and time of last update";
            this.type = 4;
            this.typeName = "Date and time";
        }
        else if (idAsLong.longValue() == 39L)
        {
            this.name = "Parent's due date";
            this.type = 3;
            this.typeName = "Date";
        }
        else if (idAsLong.longValue() == 40L)
        {
            this.name = "Parent's issue type";
            this.type = 12;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 41L)
        {
            this.name = "Parent's issue key";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 42L)
        {
            this.name = "Parent's issue status";
            this.type = 10;
            this.typeName = "Issue status";
        }
        else if (idAsLong.longValue() == 43L)
        {
            this.name = "Parent's priority";
            this.type = 9;
            this.typeName = "Issue priority";
        }
        else if (idAsLong.longValue() == 69L)
        {
            this.name = "Parent's original estimate (minutes)";
            this.type = 2;
            this.typeName = "Number";
        }
        else if (idAsLong.longValue() == 44L)
        {
            this.name = "Parent's remaining estimate (minutes)";
            this.type = 2;
            this.typeName = "Number";
        }
        else if (idAsLong.longValue() == 45L)
        {
            this.name = "Parent's total time spent (minutes)";
            this.type = 2;
            this.typeName = "Number";
        }
        else if (idAsLong.longValue() == 46L)
        {
            this.name = "Parent's number of votes received";
            this.type = 2;
            this.typeName = "Number";
        }
        else if (idAsLong.longValue() == 97L)
        {
            this.name = "Parent's other subtask's keys";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 47L)
        {
            this.name = "Parent's number of subtasks";
            this.type = 2;
            this.typeName = "Number";
        }
        else if (idAsLong.longValue() == 100L)
        {
            this.name = "Linked issues keys";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 101L)
        {
            this.name = "Number of linked issues";
            this.type = 2;
            this.typeName = "Number";
        }
        else if (idAsLong.longValue() == 48L)
        {
            this.name = "Parent's issue resolution";
            this.type = 11;
            this.typeName = "Issue resolution";
        }
        else if (idAsLong.longValue() == 82L)
        {
            this.name = "Parent's environment";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 83L)
        {
            this.name = "Parent's attachments";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 84L)
        {
            this.name = "Parent's attachments with details";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 85L)
        {
            this.name = "Parent's number of attachments";
            this.type = 2;
            this.typeName = "Number";
        }
        else if (idAsLong.longValue() == 85L)
        {
            this.name = "Parent's fixed versions";
            this.type = 14;
            this.typeName = "Versions";
        }
        else if (idAsLong.longValue() == 87L)
        {
            this.name = "Parent's fixed versions with details";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 88L)
        {
            this.name = "Parent's number of fixed versions";
            this.type = 2;
            this.typeName = "Number";
        }
        else if (idAsLong.longValue() == 89L)
        {
            this.name = "Parent's affected versions";
            this.type = 14;
            this.typeName = "Versions";
        }
        else if (idAsLong.longValue() == 90L)
        {
            this.name = "Parent's affected versions with details";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 91L)
        {
            this.name = "Parent's number of affected versions";
            this.type = 2;
            this.typeName = "Number";
        }
        else if (idAsLong.longValue() == 92L)
        {
            this.name = "Parent's labels";
            this.type = 15;
            this.typeName = "Labels";
        }
        else if (idAsLong.longValue() == 93L)
        {
            this.name = "Parent's number of labels";
            this.type = 2;
            this.typeName = "Number";
        }
        else if (idAsLong.longValue() == 95L)
        {
            this.name = "Parent's components";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 57L)
        {
            this.name = "Current date and time";
            this.type = 4;
            this.typeName = "Date and time";
        }
        else if (idAsLong.longValue() == 58L)
        {
            this.name = "Ephemeral number 1";
            this.type = 2;
            this.typeName = "Number";
        }
        else if (idAsLong.longValue() == 59L)
        {
            this.name = "Ephemeral number 2";
            this.type = 2;
            this.typeName = "Number";
        }
        else if (idAsLong.longValue() == 60L)
        {
            this.name = "Ephemeral number 3";
            this.type = 2;
            this.typeName = "Number";
        }
        else if (idAsLong.longValue() == 64L)
        {
            this.name = "Ephemeral number 4";
            this.type = 2;
            this.typeName = "Number";
        }
        else if (idAsLong.longValue() == 65L)
        {
            this.name = "Ephemeral number 5";
            this.type = 2;
            this.typeName = "Number";
        }
        else if (idAsLong.longValue() == 61L)
        {
            this.name = "Ephemeral string 1";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 62L)
        {
            this.name = "Ephemeral string 2";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 63L)
        {
            this.name = "Ephemeral string 3";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 66L)
        {
            this.name = "Ephemeral string 4";
            this.type = 1;
            this.typeName = "Text";
        }
        else if (idAsLong.longValue() == 67L)
        {
            this.name = "Ephemeral string 5";
            this.type = 1;
            this.typeName = "Text";
        }
        else
        {
            this.name = null;
            this.type = 0;
            this.typeName = "Invalid type";
        }
    }

    public CustomField getCustomField()
    {
        return this.customField;
    }

    public String getName()
    {
        return this.name;
    }

    public Long getIdAsLong()
    {
        return this.idAsLong;
    }

    public String getIdAsString()
    {
        NumberFormat formatter = new DecimalFormat("00000");
        return formatter.format(this.idAsLong);
    }

    public int getType()
    {
        return this.type;
    }

    public String getTypeName()
    {
        return this.typeName;
    }

    public boolean isSubtaskOnlyField()
    {
        return (this.idAsLong.longValue() == 29L) || (this.idAsLong.longValue() == 30L) || (this.idAsLong.longValue() == 31L) || (this.idAsLong.longValue() == 32L) || (this.idAsLong.longValue() == 33L) || (this.idAsLong.longValue() == 34L) || (this.idAsLong.longValue() == 35L) || (this.idAsLong.longValue() == 36L) || (this.idAsLong.longValue() == 55L) || (this.idAsLong.longValue() == 56L) || (this.idAsLong.longValue() == 39L) || (this.idAsLong.longValue() == 40L) || (this.idAsLong.longValue() == 41L) || (this.idAsLong.longValue() == 42L) || (this.idAsLong.longValue() == 43L) || (this.idAsLong.longValue() == 69L) || (this.idAsLong.longValue() == 44L) || (this.idAsLong.longValue() == 45L) || (this.idAsLong.longValue() == 46L) || (this.idAsLong.longValue() == 47L) || (this.idAsLong.longValue() == 48L) || (this.idAsLong.longValue() == 82L) || (this.idAsLong.longValue() == 83L) || (this.idAsLong.longValue() == 84L) || (this.idAsLong.longValue() == 85L) || (this.idAsLong.longValue() == 85L) || (this.idAsLong.longValue() == 87L) || (this.idAsLong.longValue() == 88L) || (this.idAsLong.longValue() == 89L) || (this.idAsLong.longValue() == 90L) || (this.idAsLong.longValue() == 91L) || (this.idAsLong.longValue() == 92L) || (this.idAsLong.longValue() == 93L) || (this.idAsLong.longValue() == 95L) || (this.idAsLong.longValue() == 97L) || (this.idAsLong.longValue() == 100L) || (this.idAsLong.longValue() == 101L);
    }

    public Object getValue(Issue inputIssue)
    {
        Issue issue = inputIssue;

        if ((issue != null) && ((this.idAsLong.longValue() >= 50000L) || (isSubtaskOnlyField())))
        {
            issue = issue.getParentObject();
        }

        if (issue == null)
        {
            return null;
        }

        if (this.customField != null)
        {
            return issue.getCustomFieldValue(this.customField);
        }
        if ((this.idAsLong.longValue() == 0L) || (this.idAsLong.longValue() == 29L))
        {
            return issue.getSummary();
        }
        if ((this.idAsLong.longValue() == 1L) || (this.idAsLong.longValue() == 30L))
        {
            return issue.getDescription();
        }
        if ((this.idAsLong.longValue() == 3L) || (this.idAsLong.longValue() == 31L))
        {
            return issue.getAssignee();
        }
        if ((this.idAsLong.longValue() == 2L) || (this.idAsLong.longValue() == 32L))
        {
            User assignee = issue.getAssignee();

            if (assignee != null)
            {
                return assignee.getDisplayName();
            }

            return "";
        }

        if ((this.idAsLong.longValue() == 4L) || (this.idAsLong.longValue() == 33L))
        {
            User assignee = issue.getAssignee();

            if (assignee != null)
            {
                return assignee.getEmailAddress();
            }

            return "";
        }

        if ((this.idAsLong.longValue() == 6L) || (this.idAsLong.longValue() == 34L))
        {
            return issue.getReporter();
        }
        if ((this.idAsLong.longValue() == 5L) || (this.idAsLong.longValue() == 35L))
        {
            return issue.getReporter().getDisplayName();
        }
        if ((this.idAsLong.longValue() == 7L) || (this.idAsLong.longValue() == 36L))
        {
            return issue.getReporter().getEmailAddress();
        }
        if ((this.idAsLong.longValue() == 9L) || (this.idAsLong.longValue() == 55L))
        {
            return issue.getCreated();
        }
        if ((this.idAsLong.longValue() == 11L) || (this.idAsLong.longValue() == 56L))
        {
            return issue.getUpdated();
        }
        if ((this.idAsLong.longValue() == 12L) || (this.idAsLong.longValue() == 39L))
        {
            return issue.getDueDate();
        }
        if ((this.idAsLong.longValue() == 14L) || (this.idAsLong.longValue() == 40L))
        {
            return issue.getIssueTypeObject().getNameTranslation();
        }
        if ((this.idAsLong.longValue() == 15L) || (this.idAsLong.longValue() == 41L))
        {
            return issue.getKey();
        }
        if ((this.idAsLong.longValue() == 16L) || (this.idAsLong.longValue() == 42L))
        {
            return issue.getStatusObject();
        }
        if ((this.idAsLong.longValue() == 17L) || (this.idAsLong.longValue() == 43L))
        {
            return issue.getPriorityObject();
        }
        if ((this.idAsLong.longValue() == 68L) || (this.idAsLong.longValue() == 69L))
        {
            Long l = issue.getOriginalEstimate();

            if (l != null)
            {
                l = Long.valueOf(l.longValue() / 60L);
            }
            else
            {
                l = Long.valueOf(0L);
            }

            return l;
        }
        if ((this.idAsLong.longValue() == 24L) || (this.idAsLong.longValue() == 44L))
        {
            Long l = issue.getEstimate();

            if (l != null)
            {
                l = Long.valueOf(l.longValue() / 60L);
            }
            else
            {
                l = Long.valueOf(0L);
            }

            return l;
        }
        if ((this.idAsLong.longValue() == 25L) || (this.idAsLong.longValue() == 45L))
        {
            Long l = issue.getTimeSpent();

            if (l != null)
            {
                l = Long.valueOf(l.longValue() / 60L);
            }
            else
            {
                l = Long.valueOf(0L);
            }

            return l;
        }
        if ((this.idAsLong.longValue() == 26L) || (this.idAsLong.longValue() == 46L))
        {
            return issue.getVotes();
        }
        if ((this.idAsLong.longValue() == 96L) || (this.idAsLong.longValue() == 97L))
        {
            Collection<Issue> subtasks = issue.getSubTaskObjects();

            StringBuffer output = new StringBuffer();

            for (Issue s : subtasks)
            {
                String key = s.getKey();

                if (!key.equals(inputIssue.getKey()))
                {
                    output.append(key);
                    output.append(", ");
                }
            }

            if (output.length() > 0)
            {
                output.delete(output.length() - 2, output.length());
            }

            return new String(output);
        }
        if ((this.idAsLong.longValue() == 27L) || (this.idAsLong.longValue() == 47L))
        {
            return Integer.valueOf(issue.getSubTaskObjects().size());
        }
        if ((this.idAsLong.longValue() == 98L) || (this.idAsLong.longValue() == 100L))
        {
            IssueLinkManager issueLinkManager = ComponentManager.getInstance().getIssueLinkManager();

            Long issueId = issue.getId();

            List<IssueLink> inwardLinks = issueLinkManager.getInwardLinks(issueId);
            List<IssueLink> outwardLinks = issueLinkManager.getOutwardLinks(issueId);

            StringBuffer linkedIssues = new StringBuffer();

            for (IssueLink issueLink : inwardLinks)
            {
                if (!issueLink.getIssueLinkType().isSubTaskLinkType())
                {
                    linkedIssues.append(issueLink.getSourceObject().getKey());
                    linkedIssues.append(", ");
                }
            }

            for (IssueLink issueLink : outwardLinks)
            {
                if (!issueLink.getIssueLinkType().isSubTaskLinkType())
                {
                    linkedIssues.append(issueLink.getDestinationObject().getKey());
                    linkedIssues.append(", ");
                }
            }

            if (linkedIssues.length() > 0)
            {
                linkedIssues.delete(linkedIssues.length() - 2, linkedIssues.length());
            }

            return linkedIssues;
        }
        if ((this.idAsLong.longValue() == 99L) || (this.idAsLong.longValue() == 101L))
        {
            IssueLinkManager issueLinkManager = ComponentManager.getInstance().getIssueLinkManager();

            Long issueId = issue.getId();

            List<IssueLink> inwardLinks = issueLinkManager.getInwardLinks(issueId);
            List<IssueLink> outwardLinks = issueLinkManager.getOutwardLinks(issueId);

            int numberOfLinkedIssues = 0;

            for (IssueLink issueLink : inwardLinks)
            {
                if (!issueLink.getIssueLinkType().isSubTaskLinkType())
                {
                    numberOfLinkedIssues++;
                }
            }

            for (IssueLink issueLink : outwardLinks)
            {
                if (!issueLink.getIssueLinkType().isSubTaskLinkType())
                {
                    numberOfLinkedIssues++;
                }
            }

            return Integer.valueOf(numberOfLinkedIssues);
        }
        if ((this.idAsLong.longValue() == 28L) || (this.idAsLong.longValue() == 48L))
        {
            return issue.getResolutionObject();
        }
        if ((this.idAsLong.longValue() == 70L) || (this.idAsLong.longValue() == 82L))
        {
            return issue.getEnvironment();
        }
        if ((this.idAsLong.longValue() == 71L) || (this.idAsLong.longValue() == 83L))
        {
            Collection<Attachment> attachments = issue.getAttachments();

            StringBuffer output = new StringBuffer();

            for (Attachment a : attachments)
            {
                output.append(a.getFilename());
                output.append(", ");
            }

            if (output.length() > 0)
            {
                output.delete(output.length() - 2, output.length());
            }

            return new String(output);
        }
        if ((this.idAsLong.longValue() == 72L) || (this.idAsLong.longValue() == 84L))
        {
            Collection<Attachment> attachments = issue.getAttachments();

            StringBuffer output = new StringBuffer();

            for (Attachment a : attachments)
            {
                output.append(a.getFilename());
                output.append(" (");
                output.append(a.getFilesize().toString());
                output.append(") bytes, ");
            }

            if (output.length() > 0)
            {
                output.delete(output.length() - 2, output.length());
            }

            return new String(output);
        }
        if ((this.idAsLong.longValue() == 73L) || (this.idAsLong.longValue() == 85L))
        {
            return Integer.valueOf(issue.getAttachments().size());
        }
        if ((this.idAsLong.longValue() == 74L) || (this.idAsLong.longValue() == 85L))
        {
            Collection<Version> versions = issue.getFixVersions();

            StringBuffer output = new StringBuffer();

            for (Version v : versions)
            {
                output.append(v.getName());
                output.append(", ");
            }

            if (output.length() > 0)
            {
                output.delete(output.length() - 2, output.length());
            }

            return new String(output);
        }
        if ((this.idAsLong.longValue() == 75L) || (this.idAsLong.longValue() == 87L))
        {
            Collection<Version> versions = issue.getFixVersions();

            StringBuffer output = new StringBuffer();

            for (Version v : versions)
            {
                output.append(v.getName());
                output.append(" : ");
                output.append(v.getDescription());
                output.append(", ");
            }

            if (output.length() > 0)
            {
                output.delete(output.length() - 2, output.length());
            }

            return new String(output);
        }
        if ((this.idAsLong.longValue() == 76L) || (this.idAsLong.longValue() == 88L))
        {
            return Integer.valueOf(issue.getFixVersions().size());
        }
        if ((this.idAsLong.longValue() == 77L) || (this.idAsLong.longValue() == 89L))
        {
            Collection<Version> versions = issue.getAffectedVersions();

            StringBuffer output = new StringBuffer();

            for (Version v : versions)
            {
                output.append(v.getName());
                output.append(", ");
            }

            if (output.length() > 0)
            {
                output.delete(output.length() - 2, output.length());
            }

            return new String(output);
        }
        if ((this.idAsLong.longValue() == 78L) || (this.idAsLong.longValue() == 90L))
        {
            Collection<Version> versions = issue.getAffectedVersions();

            StringBuffer output = new StringBuffer();

            for (Version v : versions)
            {
                output.append(v.getName());
                output.append(" : ");
                output.append(v.getDescription());
                output.append(", ");
            }

            if (output.length() > 0)
            {
                output.delete(output.length() - 2, output.length());
            }

            return new String(output);
        }
        if ((this.idAsLong.longValue() == 79L) || (this.idAsLong.longValue() == 91L))
        {
            return Integer.valueOf(issue.getAffectedVersions().size());
        }
        if ((this.idAsLong.longValue() == 80L) || (this.idAsLong.longValue() == 92L))
        {
            Collection<Label> labels = issue.getLabels();

            StringBuffer output = new StringBuffer();

            for (Label l : labels)
            {
                output.append(l.getLabel());
                output.append(" ");
            }

            if (output.length() > 0)
            {
                output.delete(output.length() - 1, output.length());
            }

            return new String(output);
        }
        if ((this.idAsLong.longValue() == 81L) || (this.idAsLong.longValue() == 93L))
        {
            return Integer.valueOf(issue.getLabels().size());
        }
        if ((this.idAsLong.longValue() == 94L) || (this.idAsLong.longValue() == 95L))
        {
            Collection<ProjectComponent> components = issue.getComponentObjects();

            StringBuffer output = new StringBuffer();

            for (ProjectComponent c : components)
            {
                output.append(c.getName());
                output.append(", ");
            }

            if (output.length() > 0)
            {
                output.delete(output.length() - 2, output.length());
            }

            return new String(output);
        }
        if (this.idAsLong.longValue() == 18L)
        {
            return issue.getProjectObject().getKey();
        }
        if (this.idAsLong.longValue() == 19L)
        {
            return issue.getProjectObject().getName();
        }
        if (this.idAsLong.longValue() == 49L)
        {
            return issue.getProjectObject().getDescription();
        }
        if (this.idAsLong.longValue() == 50L)
        {
            return issue.getProjectObject().getUrl();
        }
        if (this.idAsLong.longValue() == 51L)
        {
            GenericValue projectCategoryGV = issue.getProjectObject().getProjectCategory();

            if (projectCategoryGV != null)
            {
                return projectCategoryGV.getString("name");
            }

            return "";
        }

        if (this.idAsLong.longValue() == 52L)
        {
            return issue.getProjectObject().getLead();
        }
        if (this.idAsLong.longValue() == 53L)
        {
            return issue.getProjectObject().getLead().getDisplayName();
        }
        if (this.idAsLong.longValue() == 54L)
        {
            return issue.getProjectObject().getLead().getEmailAddress();
        }
        if (this.idAsLong.longValue() == 102L)
        {
            StringBuffer output = new StringBuffer();
            try
            {
                List<Issue> issues = LinkConditionUtil.getIssuesFromProject(issue.getProjectObject());

                for (Issue i : issues)
                {
                    String key = i.getKey();

                    if (!key.equals(issue.getKey()))
                    {
                        output.append(key);
                        output.append(", ");
                    }
                }

                if (issues.size() > 0)
                {
                    try
                    {
                        output.delete(output.length() - 2, output.length());
                    }
                    catch (StringIndexOutOfBoundsException e)
                    {
                        log.error("Bad index for delete when obtaining ID_Issues_in_project value.");
                        output = null;
                    }
                }
            }
            catch (SearchException e)
            {
                output = null;
            }

            return new String(output);
        }
        if (this.idAsLong.longValue() == 57L)
        {
            return new Timestamp(new Date().getTime());
        }
        if (this.idAsLong.longValue() == 20L)
        {
            return ComponentManager.getInstance().getJiraAuthenticationContext().getLoggedInUser();
        }
        if (this.idAsLong.longValue() == 21L)
        {
            return ComponentManager.getInstance().getJiraAuthenticationContext().getLoggedInUser().getDisplayName();
        }
        if (this.idAsLong.longValue() == 22L)
        {
            return ComponentManager.getInstance().getJiraAuthenticationContext().getLoggedInUser().getDisplayName();
        }
        if ((this.idAsLong.longValue() == 58L) || (this.idAsLong.longValue() == 59L) || (this.idAsLong.longValue() == 60L) || (this.idAsLong.longValue() == 64L) || (this.idAsLong.longValue() == 65L))
        {
            String key = issue.getKey() + this.name;

            Double d = (Double)ephemeralStore.get(key);
            ephemeralStore.remove(key);

            return d;
        }
        if ((this.idAsLong.longValue() == 61L) || (this.idAsLong.longValue() == 62L) || (this.idAsLong.longValue() == 63L) || (this.idAsLong.longValue() == 66L) || (this.idAsLong.longValue() == 67L))
        {
            String key = issue.getKey() + this.name;

            String s = (String)ephemeralStore.get(key);
            ephemeralStore.remove(key);

            return s;
        }

        log.warn("Can't provide value to field " + getName() + " because getValue() method don't know how to get the value.");

        return null;
    }

    public String getStringValue(Issue issue)
    {
        return getStringValue(issue, null);
    }

    public String getStringValue(Issue issue, DateTimeFormatter dateTimeFormatter)
    {
        return getStringValue(getValue(issue), dateTimeFormatter);
    }

    public void setValue(Object newValue, Issue issueToBeModified)
    {
        User user = ComponentManager.getInstance().getJiraAuthenticationContext().getLoggedInUser();

        setValue(newValue, issueToBeModified, user);
    }

    public void setValue(Object newValue, Issue issueToBeModified, User user)
    {
        setValue(newValue, issueToBeModified, user, null);
    }

    public void setValue(Object newValue, Issue issueToBeModified, DateTimeFormatter dateTimeFormatter)
    {
        User user = ComponentManager.getInstance().getJiraAuthenticationContext().getLoggedInUser();

        setValue(newValue, issueToBeModified, user, dateTimeFormatter);
    }

    public void setValue(Object valueToAssign, Issue issueToBeModified, User user, DateTimeFormatter dateTimeFormatter)
    {
        MutableIssue issue;
        if ((issueToBeModified != null) && ((this.idAsLong.longValue() >= 50000L) || (isSubtaskOnlyField())))
        {
            issue = (MutableIssue)issueToBeModified.getParentObject();
        }
        else
        {
            issue = (MutableIssue)issueToBeModified;
        }

        if (issue != null)
        {
            Object newValue = valueToAssign;

            if ((valueToAssign instanceof GeneralizedField))
            {
                newValue = ((GeneralizedField)valueToAssign).getValue(issueToBeModified);
            }

            if (this.customField != null)
            {
                log.debug("Custom field \"" + getName() + "\" will be set to value \"" + newValue.toString() + "\".");

                newValue = processValueToBeAssigned(newValue, dateTimeFormatter, issue);
                this.customField.getCustomFieldType().updateValue(this.customField, issue, newValue);
            }
            else if ((this.idAsLong.longValue() == 0L) || (this.idAsLong.longValue() == 29L))
            {
                issue.setSummary(getStringValue(newValue, dateTimeFormatter));
            }
            else if ((this.idAsLong.longValue() == 1L) || (this.idAsLong.longValue() == 30L))
            {
                issue.setDescription(getStringValue(newValue, dateTimeFormatter));
            }
            else if ((this.idAsLong.longValue() == 3L) || (this.idAsLong.longValue() == 31L))
            {
                User newUser = (User)processValueToBeAssigned(newValue, dateTimeFormatter, issue);

                if (newUser != null)
                {
                    issue.setAssignee(newUser);
                }
            }
            else if ((this.idAsLong.longValue() == 6L) || (this.idAsLong.longValue() == 34L))
            {
                User newUser = (User)processValueToBeAssigned(newValue, dateTimeFormatter, issue);

                if (newUser != null)
                {
                    issue.setReporter(newUser);
                }
            }
            else if ((this.idAsLong.longValue() == 12L) || (this.idAsLong.longValue() == 39L))
            {
                Timestamp t = (Timestamp)processValueToBeAssigned(newValue, dateTimeFormatter, issue);

                if (t != null)
                {
                    issue.setDueDate(t);
                }
            }
            else if ((this.idAsLong.longValue() == 28L) || (this.idAsLong.longValue() == 48L))
            {
                Resolution r = (Resolution)processValueToBeAssigned(newValue, dateTimeFormatter, issue);

                if (r != null)
                {
                    issue.setResolutionObject(r);
                }
            }
            else if ((this.idAsLong.longValue() == 70L) || (this.idAsLong.longValue() == 82L))
            {
                issue.setEnvironment(getStringValue(newValue, dateTimeFormatter));
            }
            else
            {
                Status targetStatus;
                IssueService issueService;
                IssueInputParameters issueInputParamenters;
                JiraWorkflow workflow;
                if ((this.idAsLong.longValue() == 16L) || (this.idAsLong.longValue() == 42L))
                {
                    targetStatus = (Status)processValueToBeAssigned(newValue, dateTimeFormatter, issue);

                    if (targetStatus != null)
                    {
                        issueService = ComponentManager.getInstance().getIssueService();
                        issueInputParamenters = new IssueInputParametersImpl();

                        workflow = ComponentManager.getInstance().getWorkflowManager().getWorkflow(issue);
                        Status currentStatus = issue.getStatusObject();

                        StepDescriptor currentStep = workflow.getLinkedStep(currentStatus);

                        List<ActionDescriptor> actions = currentStep.getActions();

                        for (ActionDescriptor action : actions)
                        {
                            ResultDescriptor result = action.getUnconditionalResult();
                            StepDescriptor targetStep = workflow.getDescriptor().getStep(result.getStep());

                            if (workflow.getLinkedStatusObject(targetStep).getId() == targetStatus.getId())
                            {
                                IssueService.TransitionValidationResult trasitionValidationResult = issueService.validateTransition(user, issue.getId(), action.getId(), issueInputParamenters);

                                if (trasitionValidationResult.isValid())
                                {
                                    IssueService.IssueResult r = issueService.transition(user, trasitionValidationResult);

                                    if (r.isValid() == true)
                                    {
                                        issue.setStatusObject(targetStatus);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                else if ((this.idAsLong.longValue() == 17L) || (this.idAsLong.longValue() == 43L))
                {
                    Priority p = (Priority)processValueToBeAssigned(newValue, dateTimeFormatter, issue);

                    if (p != null)
                    {
                        issue.setPriorityObject(p);
                    }
                }
                else if ((this.idAsLong.longValue() == 68L) || (this.idAsLong.longValue() == 69L))
                {
                    Double newOrigEstimate = (Double)processValueToBeAssigned(newValue, dateTimeFormatter, issue);

                    if (newOrigEstimate != null)
                    {
                        newOrigEstimate = Double.valueOf(newOrigEstimate.doubleValue() * 60.0D);

                        issue.setOriginalEstimate(new Long(newOrigEstimate.longValue()));
                    }
                }
                else if ((this.idAsLong.longValue() == 24L) || (this.idAsLong.longValue() == 44L))
                {
                    Double newEstimate = (Double)processValueToBeAssigned(newValue, dateTimeFormatter, issue);

                    if (newEstimate != null)
                    {
                        newEstimate = Double.valueOf(newEstimate.doubleValue() * 60.0D);

                        issue.setEstimate(new Long(newEstimate.longValue()));
                    }
                }
                else if ((this.idAsLong.longValue() == 25L) || (this.idAsLong.longValue() == 45L))
                {
                    Double newTimeSpent = (Double)processValueToBeAssigned(newValue, dateTimeFormatter, issue);

                    if (newTimeSpent != null)
                    {
                        newTimeSpent = Double.valueOf(newTimeSpent.doubleValue() * 60.0D);

                        issue.setTimeSpent(new Long(newTimeSpent.longValue()));
                    }
                }
                else if ((this.idAsLong.longValue() == 74L) || (this.idAsLong.longValue() == 85L))
                {
                    ArrayList versions = (ArrayList)processValueToBeAssigned(newValue, dateTimeFormatter, issue);
                    issue.setFixVersions(versions);
                }
                else if ((this.idAsLong.longValue() == 77L) || (this.idAsLong.longValue() == 89L))
                {
                    ArrayList versions = (ArrayList)processValueToBeAssigned(newValue, dateTimeFormatter, issue);
                    issue.setAffectedVersions(versions);
                }
                else if ((this.idAsLong.longValue() == 94L) || (this.idAsLong.longValue() == 95L))
                {
                    if ((newValue instanceof String))
                    {
                        ProjectComponentManager projectComponentManager = ComponentManager.getInstance().getProjectComponentManager();

                        Collection<ProjectComponent> components = projectComponentManager.findAllForProject(issue.getProjectObject().getId());

                        StringTokenizer st = new StringTokenizer((String)newValue, ";,");

                        ArrayList<ProjectComponent> newComponents = new ArrayList();

                        Locale locale = Locale.getDefault();
                        String token;
                        while (st.hasMoreTokens())
                        {
                            token = st.nextToken().trim().toUpperCase(locale);

                            for (ProjectComponent c : components)
                            {
                                String cName = c.getName().trim().toUpperCase(locale);

                                if (token.equals(cName))
                                {
                                    newComponents.add(c);
                                }
                            }
                        }

                        issue.setComponentObjects(newComponents);
                    }
                }
                else if ((this.idAsLong.longValue() == 80L) || (this.idAsLong.longValue() == 92L))
                {
                    String newLabels = (String)processValueToBeAssigned(newValue, dateTimeFormatter, issue);

                    LabelManager labelManager = (LabelManager)ComponentManager.getComponentInstanceOfType(LabelManager.class);

                    StringTokenizer st = new StringTokenizer(newLabels);

                    HashSet labels = new HashSet();

                    while (st.hasMoreTokens())
                    {
                        labels.add(st.nextToken());
                    }

                    labelManager.setLabels(user, issue.getId(), labels, false, true);
                }
                else if ((this.idAsLong.longValue() == 58L) || (this.idAsLong.longValue() == 59L) || (this.idAsLong.longValue() == 60L) || (this.idAsLong.longValue() == 64L) || (this.idAsLong.longValue() == 65L))
                {
                    String key = issue.getKey() + this.name;

                    newValue = processValueToBeAssigned(newValue, dateTimeFormatter, issue);

                    if (newValue != null)
                    {
                        if (ephemeralStore.containsKey(key))
                        {
                            ephemeralStore.remove(key);
                        }

                        ephemeralStore.put(key, newValue);
                    }
                }
                else if ((this.idAsLong.longValue() == 61L) || (this.idAsLong.longValue() == 62L) || (this.idAsLong.longValue() == 63L) || (this.idAsLong.longValue() == 66L) || (this.idAsLong.longValue() == 67L))
                {
                    String key = issue.getKey() + this.name;

                    if (ephemeralStore.containsKey(key))
                    {
                        ephemeralStore.remove(key);
                    }

                    ephemeralStore.put(key, getStringValue(newValue, dateTimeFormatter));
                }
                else
                {
                    log.warn("Can't assign new value to " + getName() + " because this GeneralizedField is not assignable.");
                }
            }

            IssueManager issueManager = ComponentManager.getInstance().getIssueManager();
            issueManager.updateIssue(user, issue, EventDispatchOption.DO_NOT_DISPATCH, false);
        }
        else
        {
            log.warn("Can't assign new value to " + getName() + " because issue object is null.");
        }
    }

    private int getCustomFieldType(CustomField cf)
    {
        String key = cf.getCustomFieldType().getKey();
        int cfType;
        if ((key.equals("com.atlassian.jira.plugin.system.customfieldtypes:userpicker")) || (key.equals("com.atlassian.jira.plugin.pre-conf-userpicker:pre-conf-userpicker")) || (key.equals("com.iamhuy.jira.plugin.issue-alternative-assignee:userselectbox-customfield")) || (key.equals("com.iamhuy.jira.plugin.customfield:userselectbox-customfield")))
        {
            cfType = 5;
        }
        else
        {
            if (key.equals("com.atlassian.jira.plugin.system.customfieldtypes:float"))
            {
                cfType = 2;
            }
            else
            {
                if (key.equals("com.atlassian.jira.plugin.system.customfieldtypes:datepicker"))
                {
                    cfType = 3;
                }
                else
                {
                    if (key.equals("com.atlassian.jira.plugin.system.customfieldtypes:datetime"))
                    {
                        cfType = 4;
                    }
                    else
                    {
                        if ((key.equals("com.atlassian.jira.plugin.system.customfieldtypes:select")) || (key.equals("com.atlassian.jira.plugin.system.customfieldtypes:radiobuttons")))
                        {
                            cfType = 6;
                        }
                        else
                        {
                            if (key.equals("com.atlassian.jira.plugin.system.customfieldtypes:cascadingselect"))
                            {
                                cfType = 7;
                            }
                            else
                            {
                                if (key.equals("org.deblauwe.jira.plugin.database-values-plugin:databasevaluesselectionfield"))
                                {
                                    cfType = 8;
                                }
                                else
                                {
                                    if ((key.equals("com.atlassian.jira.plugin.system.customfieldtypes:multiselect")) || (key.equals("com.atlassian.jira.plugin.system.customfieldtypes:multicheckboxes")))
                                    {
                                        cfType = 13;
                                    }
                                    else
                                    {
                                        if (key.equals("com.atlassian.jira.plugin.system.customfieldtypes:multiuserpicker"))
                                        {
                                            cfType = 18;
                                        }
                                        else
                                        {
                                            if (key.equals("com.atlassian.jira.plugin.system.customfieldtypes:grouppicker"))
                                            {
                                                cfType = 19;
                                            }
                                            else
                                            {
                                                if (key.equals("com.atlassian.jira.plugin.system.customfieldtypes:multigrouppicker"))
                                                {
                                                    cfType = 20;
                                                }
                                                else
                                                {
                                                    if ((key.equals("org.deblauwe.jira.plugin.database-values-plugin:textarea")) || (key.equals("org.deblauwe.jira.plugin.database-values-plugin:textfield")) || (key.equals("org.deblauwe.jira.plugin.database-values-plugin:readonlyfield")))
                                                    {
                                                        cfType = 1;
                                                    }
                                                    else
                                                    {
                                                        cfType = 1;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return cfType;
    }

    private String getStringValue(Object value, DateTimeFormatter dateTimeFormatter)
    {
        String output;
        if (value != null)
        {
            if ((value instanceof User))
            {
                output = ((User)value).getName();
            }
            else
            {
                if ((value instanceof Timestamp))
                {
                    output = dateTimeFormatter.format((Timestamp)value);
                }
                else
                {
                    if ((value instanceof Priority))
                    {
                        output = ((Priority)value).getNameTranslation();
                    }
                    else
                    {
                        if ((value instanceof Status))
                        {
                            output = ((Status)value).getNameTranslation();
                        }
                        else
                        {
                            if ((value instanceof IssueType))
                            {
                                output = ((IssueType)value).getNameTranslation();
                            }
                            else
                            {
                                if ((value instanceof Resolution))
                                {
                                    output = ((Resolution)value).getNameTranslation();
                                }
                                else
                                {
                                    if ((value instanceof Double))
                                    {
                                        output = ((Double)value).toString().replaceFirst(".0\\z", "");
                                    }
                                    else
                                    {
                                        if ((value instanceof Option))
                                        {
                                            output = ((Option)value).getValue();
                                        }
                                        else
                                        {
                                            if ((value instanceof User))
                                            {
                                                output = ((User)value).getName();
                                            }
                                            else
                                            {
                                                if ((value instanceof Group))
                                                {
                                                    output = ((Group)value).getName();
                                                }
                                                else
                                                {
                                                    if ((value instanceof Collection))
                                                    {
                                                        StringBuffer s = new StringBuffer();

                                                        for (Iterator i$ = ((Collection)value).iterator(); i$.hasNext(); ) { Object o = i$.next();

                                                            if ((o instanceof Option))
                                                            {
                                                                s.append(((Option)o).getValue());
                                                                s.append(", ");
                                                            }
                                                            else if ((o instanceof User))
                                                            {
                                                                s.append(((User)o).getName());
                                                                s.append(", ");
                                                            }
                                                            else if ((o instanceof Group))
                                                            {
                                                                s.append(((Group)o).getName());
                                                                s.append(", ");
                                                            }
                                                        }

                                                        if (s.length() > 0)
                                                        {
                                                            s.delete(s.length() - 2, s.length());
                                                        }

                                                        output = new String(s);
                                                    }
                                                    else
                                                    {
                                                        if ((value instanceof Map))
                                                        {
                                                            StringBuffer s = new StringBuffer();

                                                            s.append(((Option)((Map)value).get(CascadingSelectCFType.PARENT_KEY)).getValue());
                                                            s.append(" - ");
                                                            s.append(((Option)((Map)value).get("1")).getValue());

                                                            output = new String(s);
                                                        }
                                                        else
                                                        {
                                                            output = value.toString();
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            output = "";
        }

        return output;
    }

    private Object processValueToBeAssigned(Object input, DateTimeFormatter dateTimeFormatter, Issue issue)
    {
        Object output = input;

        if ((getType() == 5) && ((input instanceof String)))
        {
            UserUtil userUtil = ComponentManager.getInstance().getUserUtil();
            output = userUtil.getUser((String)input);
        }
        else if (getType() == 2)
        {
            if ((input instanceof String))
            {
                try
                {
                    output = Double.valueOf(((String)input).trim());
                }
                catch (NumberFormatException e)
                {
                    log.warn("Impossible to convert string \"" + input + "\" to number.");
                    log.warn(e.getStackTrace());
                    output = null;
                }
            }
            else if ((input instanceof Number))
            {
                output = new Double(((Number)input).doubleValue());
            }
        }
        else if (((getType() == 1) || (getType() == 15)) && (!(input instanceof String)))
        {
            output = getStringValue(input, dateTimeFormatter);
        }
        else if (((getType() == 3) || (getType() == 4)) && ((input instanceof String)))
        {
            if (dateTimeFormatter != null)
            {
                try
                {
                    Date d = getSuitableDateTimeFormatter(dateTimeFormatter).parse((String)input);
                    output = new Timestamp(d.getTime());
                }
                catch (Exception e)
                {
                    log.warn("Impossible to convert string \"" + input + "\" to Date.");
                    log.warn(e.getStackTrace());
                    output = null;
                }
            }
            else
            {
                output = null;
            }
        }
        else if (((getType() == 3) || (getType() == 4)) && ((input instanceof Number)))
        {
            Number n = (Number)input;

            output = new Timestamp(n.longValue());
        }
        else if ((getType() == 10) && ((input instanceof String)))
        {
            ConstantsManager constantsManager = ComponentManager.getInstance().getConstantsManager();

            output = constantsManager.getStatusByTranslatedName((String)input);

            if (output == null)
            {
                output = constantsManager.getStatusObject((String)input);
            }
        }
        else if ((getType() == 9) && ((input instanceof String)))
        {
            ConstantsManager constantsManager = ComponentManager.getInstance().getConstantsManager();

            output = constantsManager.getPriorityObject((String)input);

            if (output == null)
            {
                Collection<Priority> priorities = constantsManager.getPriorityObjects();

                for (Priority p : priorities)
                {
                    if ((p.getName().equals(input)) || (p.getNameTranslation().equals(input)))
                    {
                        output = p;
                    }
                }
            }
        }
        else if ((getType() == 11) && ((input instanceof String)))
        {
            ConstantsManager constantsManager = ComponentManager.getInstance().getConstantsManager();

            output = constantsManager.getResolutionObject((String)input);

            if (output == null)
            {
                Collection<Resolution> resolutions = constantsManager.getResolutionObjects();

                for (Resolution r : resolutions)
                {
                    if ((r.getName().equals(input)) || (r.getNameTranslation().equals(input)))
                    {
                        output = r;
                    }
                }
            }
        }
        else if (getType() == 18)
        {
            if ((input instanceof String))
            {
                UserManager userManager = ComponentAccessor.getUserManager();
                GroupManager groupManager = ComponentAccessor.getGroupManager();

                StringTokenizer st = new StringTokenizer((String)input, ",;");

                Vector users = new Vector();

                while (st.hasMoreTokens())
                {
                    String token = st.nextToken().trim();

                    User u = userManager.getUserObject(token);

                    if (u != null)
                    {
                        users.add(u);
                    }
                    else
                    {
                        Group g = groupManager.getGroupObject(token);

                        if (g != null)
                        {
                            users.addAll(LinkConditionUtil.getUsersFromGroup(g));
                        }
                    }
                }

                output = users;
            }
            else if ((input instanceof User))
            {
                output = Boolean.valueOf(new Vector().add((User)input));
            }
            else if ((input instanceof Group))
            {
                output = LinkConditionUtil.getUsersFromGroup((Group)input);
            }
            else if ((input instanceof Collection))
            {
                Collection c = (Collection)input;
                Vector users = new Vector();

                for (Iterator i$ = c.iterator(); i$.hasNext(); ) { Object o = i$.next();

                    if ((o instanceof Group))
                    {
                        users.addAll(LinkConditionUtil.getUsersFromGroup((Group)o));
                    }
                    else if ((o instanceof User))
                    {
                        users.add((User)o);
                    }
                }

                output = users;
            }
        }
        else if ((getType() == 19) && ((input instanceof String)))
        {
            GroupManager groupManager = ComponentAccessor.getGroupManager();

            output = groupManager.getGroupObject((String)input);
        }
        else if ((getType() == 20) && ((input instanceof String)))
        {
            GroupManager groupManager = ComponentAccessor.getGroupManager();

            StringTokenizer st = new StringTokenizer((String)input, ",;");

            Vector groups = new Vector();

            while (st.hasMoreTokens())
            {
                String token = st.nextToken().trim();

                Group g = groupManager.getGroupObject(token);

                if (g != null)
                {
                    groups.add(g);
                }
            }

            output = groups;
        }
        else if ((getType() == 20) && ((input instanceof Group)))
        {
            output = Boolean.valueOf(new Vector().add((Group)input));
        }
        else if ((getType() == 6) && ((input instanceof String)))
        {
            if (inputAsRegularExpression((String)input) == true)
            {
                output = valuesFromRegularExpression((String)input, issue);
            }
            else
            {
                OptionsManager optionsManager = (OptionsManager)ComponentManager.getComponentInstanceOfType(OptionsManager.class);

                FieldConfig fieldConfig = getCustomField().getRelevantConfig(issue);
                Options options = optionsManager.getOptions(fieldConfig);

                String stringInput = (String)input;

                output = options.getOptionForValue(stringInput, null);

                if (output == null)
                {
                    output = options.getOptionForValue(stringInput.trim(), null);

                    if ((output == null) && (log.isInfoEnabled()))
                    {
                        log.info("Can't set field value to " + stringInput + " because it doesn't correspond to a valid field option for field " + getName());
                    }
                }
            }

            if ((output != null) && (log.isDebugEnabled()))
            {
                log.debug("Updated field '" + getName() + "' for issue '" + issue.getKey() + "' to '" + ((Option)output).getValue() + "'.");
            }

        }
        else if ((getType() == 13) && ((input instanceof String)))
        {
            if (inputAsRegularExpression((String)input) == true)
            {
                output = valuesFromRegularExpression((String)input, issue);
            }
            else
            {
                OptionsManager optionsManager = (OptionsManager)ComponentManager.getComponentInstanceOfType(OptionsManager.class);

                FieldConfig fieldConfig = getCustomField().getRelevantConfig(issue);
                Options options = optionsManager.getOptions(fieldConfig);

                String stringValue = (String)input;

                StringTokenizer st = new StringTokenizer(stringValue, ",;");

                Vector selectedOptions = new Vector();

                while (st.hasMoreTokens())
                {
                    String token = st.nextToken().trim();

                    Option option = options.getOptionForValue(token, null);

                    if (option != null)
                    {
                        selectedOptions.add(option);

                        if (log.isDebugEnabled())
                        {
                            log.debug("Updated field '" + getName() + "' for issue '" + issue.getKey() + "' setting '" + token + "'.");
                        }
                    }
                    else if (log.isInfoEnabled())
                    {
                        log.info("Can't set field value to " + token + " because it doesn't correspond to a valid field option for field " + getName());
                    }
                }

                output = selectedOptions;
            }

        }
        else if ((getType() == 14) && ((input instanceof Version)))
        {
            ArrayList newVersions = new ArrayList();

            newVersions.add((Version)input);

            output = newVersions;
        }
        else if ((getType() == 14) && ((input instanceof String)))
        {
            if (inputAsRegularExpression((String)input) == true)
            {
                output = valuesFromRegularExpression((String)input, issue);
            }
            else
            {
                VersionManager versionManager = ComponentManager.getInstance().getVersionManager();

                List<Version> versions = versionManager.getVersions(issue.getProjectObject());

                StringTokenizer st = new StringTokenizer((String)input, ";,");

                ArrayList newVersions = new ArrayList();

                Locale locale = Locale.getDefault();
                String token;
                while (st.hasMoreTokens())
                {
                    token = st.nextToken();

                    for (Version v : versions)
                    {
                        if (token.trim().toUpperCase(locale).equals(v.getName().trim().toUpperCase(locale)))
                        {
                            newVersions.add(v);
                        }
                    }
                }

                output = newVersions;
            }
        }
        else if ((getType() == 7) && ((input instanceof String)))
        {
            StringTokenizer st = new StringTokenizer((String)input, ",;");

            Option rootOption = null;
            Option childOption = null;
            String secondToken;
            if (st.hasMoreTokens() == true)
            {
                ProjectContext projectContext = new ProjectContext(issue.getProjectObject().getId());
                Options options = getCustomField().getOptions("", projectContext);

                List<Option> rootOptions = options.getRootOptions();

                String firstToken = st.nextToken().trim();

                for (Option ro : rootOptions)
                {
                    if (ro.getValue().equals(firstToken))
                    {
                        rootOption = ro;
                        break;
                    }
                }

                if ((rootOption != null) && (st.hasMoreTokens() == true))
                {
                    secondToken = st.nextToken().trim();
                    List<Option> childOptions = rootOption.getChildOptions();

                    for (Option co : childOptions)
                    {
                        if (co.getValue().equals(secondToken))
                        {
                            childOption = co;
                            break;
                        }
                    }
                }
            }

            HashMap outputMap = new HashMap(2);

            outputMap.put(CascadingSelectCFType.PARENT_KEY, rootOption);
            outputMap.put("1", childOption);

            output = outputMap;
        }

        return output;
    }

    public DateTimeFormatter getSuitableDateTimeFormatter(DateTimeFormatter inputFormatter)
    {
        DateTimeFormatter outputFormatter;
        if (getType() == 3)
        {
            outputFormatter = inputFormatter.withStyle(DateTimeStyle.DATE_PICKER);
        }
        else
        {
            if (getType() == 4)
            {
                outputFormatter = inputFormatter.withStyle(DateTimeStyle.DATE_TIME_PICKER);
            }
            else
            {
                outputFormatter = inputFormatter.withStyle(DateTimeStyle.COMPLETE);
            }
        }
        return outputFormatter;
    }

    private boolean inputAsRegularExpression(String s)
    {
        return s.trim().matches("^(\\/|\\/\\/).*\\/$");
    }

    private Object valuesFromRegularExpression(String regexp, Issue issue)
    {
        Object output = null;

        String refinedRegExp = regexp.trim();

        boolean negation = false;

        if (refinedRegExp.matches("^\\/\\/.*\\/$"))
        {
            refinedRegExp = refinedRegExp.substring(2, refinedRegExp.length() - 1);
            negation = true;
        }
        else
        {
            refinedRegExp = refinedRegExp.substring(1, refinedRegExp.length() - 1);
        }

        if (getType() == 6)
        {
            ProjectContext projectContext = new ProjectContext(issue.getProjectObject().getId());
            Options options = getCustomField().getOptions("", projectContext);

            List<Option> rootOptions = options.getRootOptions();

            for (Option o : rootOptions)
            {
                boolean matchingResult = o.getValue().trim().matches(refinedRegExp);

                if (((matchingResult) && (!negation)) || ((!matchingResult) && (negation)))
                {
                    output = o;
                    break;
                }
            }
        }
        else if (getType() == 13)
        {
            Vector auxOutput = new Vector();

            ProjectContext projectContext = new ProjectContext(issue.getProjectObject().getId());
            Options options = getCustomField().getOptions("", projectContext);

            List<Option> rootOptions = options.getRootOptions();

            for (Option o : rootOptions)
            {
                boolean matchingResult = o.getValue().trim().matches(refinedRegExp);

                if (((matchingResult) && (!negation)) || ((!matchingResult) && (negation)))
                {
                    auxOutput.add(o);
                }
            }

            output = auxOutput;
        }
        if (getType() == 14)
        {
            VersionManager versionManager = ComponentManager.getInstance().getVersionManager();

            List<Version> versions = versionManager.getVersions(issue.getProjectObject());

            ArrayList<Version> newVersions = new ArrayList();

            for (Version v : versions)
            {
                boolean matching = v.getName().matches(refinedRegExp);

                if (((matching) && (!negation)) || ((!matching) && (negation)))
                {
                    newVersions.add(v);
                }
            }

            output = newVersions;
        }

        return output;
    }
}