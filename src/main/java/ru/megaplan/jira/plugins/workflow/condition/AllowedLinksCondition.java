package ru.megaplan.jira.plugins.workflow.condition;

import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.project.Project;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.Condition;
import com.opensymphony.workflow.WorkflowException;
import ru.megaplan.jira.plugins.workflow.condition.util.LinkConditionUtil;

import java.util.List;
import java.util.Map;

public class AllowedLinksCondition
        implements Condition
{
    private final IssueLinkManager issueLinkManager;
    private final DateTimeFormatterFactory dateTimeFormatterFactory;

    public AllowedLinksCondition(IssueLinkManager issueLinkManager, DateTimeFormatterFactory dateTimeFormatterFactory)
    {
        this.issueLinkManager = issueLinkManager;
        this.dateTimeFormatterFactory = dateTimeFormatterFactory;
    }

    public boolean passesCondition(Map transientVars, Map args, PropertySet ps)
            throws WorkflowException
    {
        Issue issue = (Issue)transientVars.get("issue");

        DateTimeFormatter dateTimeFormatter = this.dateTimeFormatterFactory.formatter();

        String inwardIssueLinkTypes = (String)args.get("inwardIssueLinkTypes");
        String outwardIssueLinkTypes = (String)args.get("outwardIssueLinkTypes");
        String issueTypes = (String)args.get("issueTypes");
        String statuses = (String)args.get("statuses");
        int minLinks = Integer.valueOf((String)args.get("minLinks")).intValue();
        int maxLinks = Integer.valueOf((String)args.get("maxLinks")).intValue();
        String projectCondition = (String)args.get("projectCondition");
        String restOfLinkTypesAreAllowed = (String)args.get("restOfLinkTypesAreAllowed");
        String restOfIssueTypesAreAllowed = (String)args.get("restOfIssueTypesAreAllowed");
        String restOfStatusesAreAllowed = (String)args.get("restOfStatusesAreAllowed");

        List<IssueLink> inwardIssueLinks = this.issueLinkManager.getInwardLinks(issue.getId());
        List<IssueLink> outwardIssueLinks = this.issueLinkManager.getOutwardLinks(issue.getId());

        inwardIssueLinks = LinkConditionUtil.removeSubtaskLinks(inwardIssueLinks);
        outwardIssueLinks = LinkConditionUtil.removeSubtaskLinks(outwardIssueLinks);

        String projectKeys = (String)args.get("projectKeys");

        if (projectKeys == null)
        {
            projectKeys = "";
        }

        String ignoreOtherProjects = (String)args.get("ignoreOtherProjects");

        if (ignoreOtherProjects == null)
        {
            ignoreOtherProjects = "false";
        }

        Project project = issue.getProjectObject();

        int i = 0;

        for (IssueLink link : inwardIssueLinks)
        {
            Long issueLinkType = link.getIssueLinkType().getId();
            if ((!LinkConditionUtil.contains(inwardIssueLinkTypes, issueLinkType)) && ((!restOfLinkTypesAreAllowed.equals("true")) || (!inwardIssueLinkTypes.equals("")) || (!outwardIssueLinkTypes.equals(""))))
            {
                if (restOfLinkTypesAreAllowed.equals("false"))
                {
                    return false;
                }
            }
            else
            {
                Issue issueObject = link.getSourceObject();

                int evaluation = LinkConditionUtil.evaluateRestOfConditions(issueObject, issueTypes, statuses, restOfIssueTypesAreAllowed, restOfLinkTypesAreAllowed, restOfStatusesAreAllowed, projectCondition, project, projectKeys, ignoreOtherProjects, dateTimeFormatter);

                if (evaluation == -1)
                {
                    return false;
                }
                if (evaluation == 1)
                {
                    i++;
                }
            }
        }

        for (IssueLink link : outwardIssueLinks)
        {
            Long issueLinkType = link.getIssueLinkType().getId();
            if ((!LinkConditionUtil.contains(outwardIssueLinkTypes, issueLinkType)) && ((!restOfLinkTypesAreAllowed.equals("true")) || (!inwardIssueLinkTypes.equals("")) || (!outwardIssueLinkTypes.equals(""))))
            {
                if (restOfLinkTypesAreAllowed.equals("false"))
                {
                    return false;
                }
            }
            else
            {
                Issue issueObject = link.getDestinationObject();

                int evaluation = LinkConditionUtil.evaluateRestOfConditions(issueObject, issueTypes, statuses, restOfIssueTypesAreAllowed, restOfLinkTypesAreAllowed, restOfStatusesAreAllowed, projectCondition, project, projectKeys, ignoreOtherProjects, dateTimeFormatter);

                if (evaluation == -1)
                {
                    return false;
                }
                if (evaluation == 1)
                {
                    i++;
                }

            }

        }

        return (i >= minLinks) && (i <= maxLinks);
    }
}