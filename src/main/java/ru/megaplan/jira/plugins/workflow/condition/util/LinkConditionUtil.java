package ru.megaplan.jira.plugins.workflow.condition.util;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.impl.CascadingSelectCFType;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: firfi
 * Date: 17.10.12
 * Time: 14:57
 * To change this template use File | Settings | File Templates.
 */
public class LinkConditionUtil {

    private final static Logger log = Logger.getLogger(LinkConditionUtil.class);

    public static List<IssueLink> removeSubtaskLinks(List<IssueLink> issueLinks)
    {
        ArrayList output = new ArrayList();

        for (IssueLink issueLink : issueLinks)
        {
            if (!issueLink.getIssueLinkType().isSubTaskLinkType())
            {
                output.add(issueLink);
            }
        }

        return output;
    }
    public static String replaceCustomFieldIdsWithNames(String textToParse)
    {
        StringBuffer input = new StringBuffer(textToParse);
        StringBuffer output = new StringBuffer(input.length() + 32);
        StringBuffer generalizedFieldId = new StringBuffer("");

        int l = input.length();
        int i = 0;

        int estado = 0;

        while (i < l)
        {
            char c = input.charAt(i);

            if (estado == 0)
            {
                if ((i + 2 <= l) && (input.substring(i, i + 2).equals("%{")))
                {
                    i++;
                    estado = 1;
                }
                else
                {
                    output.append(c);
                }
            }
            else if (estado == 1)
            {
                if (Character.isDigit(c))
                {
                    generalizedFieldId.append(c);
                }
                else if (c == '.')
                {
                    if ((i + 2 < l) && ((input.charAt(i + 1) == '0') || (input.charAt(i + 1) == '1')) && (input.charAt(i + 2) == '}'))
                    {
                        Long generalizedFieldLongId = Long.valueOf(generalizedFieldId.toString());
                        GeneralizedField gf = new GeneralizedField(generalizedFieldLongId);

                        if (gf.getName() != null)
                        {
                            int type = gf.getType();

                            if ((type == 7) || (type == 8))
                            {
                                output.append("<b><i>#" + gf.getName() + '.' + input.charAt(i + 1) + "#</i></b>");
                            }
                            else
                            {
                                output.append("<b>*INDEX NOT AVAILABLE FOR FIELD %{" + generalizedFieldId + "}*</b>");
                            }
                        }
                        else
                        {
                            output.append("<b>*FIELD %{" + generalizedFieldId + "} DOESN'T EXIST*</b>");
                        }

                        i += 2;
                    }
                    else
                    {
                        output.append("<b>*NOT VALID INDEX FORMAT IN FIELD %{" + generalizedFieldId + "}*</b>");
                    }

                    generalizedFieldId = new StringBuffer("");
                    estado = 0;
                }
                else
                {
                    if (c == '}')
                    {
                        String fieldName = "";

                        Long generalizedFieldLongId = Long.valueOf(generalizedFieldId.toString());
                        GeneralizedField gf = new GeneralizedField(generalizedFieldLongId);

                        if (gf.getName() != null)
                        {
                            fieldName = "<b><i>#" + gf.getName() + "#</i></b>";
                        }
                        else
                        {
                            output.append("<b>*FIELD %{" + generalizedFieldId + "} DOESN'T EXIST*</b>");
                        }

                        output.append(fieldName);
                    }
                    else
                    {
                        output.append("%{" + generalizedFieldId + c);
                    }

                    generalizedFieldId = new StringBuffer("");
                    estado = 0;
                }
            }

            i++;
        }

        return output.toString();
    }

    public static Collection<User> getUsersFromGroup(Group g)
    {
        return ComponentAccessor.getGroupManager().getUsersInGroup(g);
    }

    public static List<Issue> getIssuesFromProject(Project project) throws SearchException
    {
        JqlQueryBuilder builder = JqlQueryBuilder.newBuilder();

        builder.where().project(new Long[] { project.getId() });
        Query query = builder.buildQuery();
        SearchResults results = ComponentManager.getInstance().getSearchService().search(ComponentManager.getInstance().getJiraAuthenticationContext().getLoggedInUser(), query, PagerFilter.getUnlimitedFilter());

        return results.getIssues();
    }

    public static boolean contains(String list, String element)
    {
        StringTokenizer lt = new StringTokenizer(list, ",");

        while (lt.hasMoreTokens())
        {
            String token = lt.nextToken();

            if (element.equals(token))
            {
                return true;
            }
        }

        return false;
    }

    public static boolean contains(String list, Long element)
    {
        StringTokenizer lt = new StringTokenizer(list, ",");

        while (lt.hasMoreTokens())
        {
            String token = lt.nextToken();

            if (element.equals(Long.valueOf(token)))
            {
                return true;
            }
        }

        return false;
    }

    public static int evaluateRestOfConditions(Issue issueObject, String issueTypes, String statuses, String restOfIssueTypesAreAllowed, String restOfLinkTypesAreAllowed, String restOfStatusesAreAllowed, String projectCondition, Project project, String projectKeys, String ignoreOtherProjects, DateTimeFormatter dateTimeFormatter)
    {
        String issueType = issueObject.getIssueTypeObject().getId();

        log.warn("checking issue : " + issueObject.getKey());

        if ((!contains(issueTypes, issueType)) && ((!restOfIssueTypesAreAllowed.equals("true")) || (!issueTypes.equals(""))))
        {
            if (restOfIssueTypesAreAllowed.equals("false"))
            {
                return -1;
            }
        }
        else
        {
            String issueStatus = issueObject.getStatusObject().getId();
            if ((!contains(statuses, issueStatus)) && ((!restOfStatusesAreAllowed.equals("true")) || (!statuses.equals(""))))
            {
                if (restOfStatusesAreAllowed.equals("false"))
                {
                    return -1;
                }
            }
            else
            {
                if (projectCondition.equals("introducedProjects"))
                {
                    String keys = parse(projectKeys, issueObject, dateTimeFormatter);
                    StringTokenizer tokenizer = new StringTokenizer(keys, ";,:\t\r\n ");

                    int output = 0;

                    if (ignoreOtherProjects.equals("false"))
                    {
                        output = -1;
                    }

                    while ((tokenizer.hasMoreTokens()) && (output != 1))
                    {
                        String token = tokenizer.nextToken();

                        if (token.equals(issueObject.getProjectObject().getKey()))
                        {
                            output = 1;
                        }
                    }

                    return output;
                }
                if (project.getId().equals(issueObject.getProjectObject().getId()))
                {
                    if (projectCondition.equals("anyButCurrent"))
                    {
                        return -1;
                    }

                    return 1;
                }

                if (projectCondition.equals("current"))
                {
                    return -1;
                }
                if (projectCondition.equals("any"))
                {
                    return 1;
                }
            }
        }

        return 0;
    }

    public static String parse(String textToParse, Issue issue, DateTimeFormatter dateTimeFormatter)
    {
        StringBuffer input = new StringBuffer(textToParse);
        StringBuffer output = new StringBuffer(input.length() + 32);

        int l = input.length();
        int i = 0;

        int estado = 0;
        StringBuffer generalizedFieldId = new StringBuffer("");

        while (i < l)
        {
            char c = input.charAt(i);

            if (estado == 0)
            {
                if ((i + 2 <= l) && (input.substring(i, i + 2).equals("%{")))
                {
                    i++;
                    estado = 1;
                }
                else
                {
                    output.append(c);
                }
            }
            else if (estado == 1)
            {
                if (Character.isDigit(c))
                {
                    generalizedFieldId.append(c);
                }
                else if (c == '.')
                {
                    if ((i + 2 < l) && ((input.charAt(i + 1) == '0') || (input.charAt(i + 1) == '1')) && (input.charAt(i + 2) == '}'))
                    {
                        Long generalizedFieldLongId = Long.valueOf(generalizedFieldId.toString());
                        GeneralizedField gf = new GeneralizedField(generalizedFieldLongId);

                        if (gf.getName() != null)
                        {
                            if (gf.getType() == 7)
                            {
                                Map valueMap = (Map)gf.getValue(issue);
                                String level;
                                if (input.charAt(i + 1) == '0')
                                {
                                    level = CascadingSelectCFType.PARENT_KEY;
                                }
                                else
                                {
                                    level = "1";
                                }

                                Option option = (Option)valueMap.get(level);
                                String storedValue = option.getValue();

                                if (storedValue != null)
                                {
                                    output.append(storedValue.toString());
                                }
                            }
                            else if (gf.getType() == 8)
                            {
                                String value = gf.getStringValue(issue, gf.getSuitableDateTimeFormatter(dateTimeFormatter));

                                if (value != null)
                                {
                                    String v0 = ""; String v1 = "";

                                    StringTokenizer st = new StringTokenizer(value, ";");

                                    if (st.hasMoreTokens())
                                    {
                                        v0 = st.nextToken();

                                        if (st.hasMoreTokens())
                                        {
                                            v1 = st.nextToken();
                                        }
                                    }

                                    if (input.charAt(i + 1) == '0')
                                    {
                                        value = v0;
                                    }
                                    else
                                    {
                                        value = v1;
                                    }

                                    output.append(value);
                                }
                            }
                            else
                            {
                                output.append("INDEX NOT AVAILABLE FOR FIELD %{" + generalizedFieldId + "}");
                            }
                        }
                        else
                        {
                            output.append("FIELD %{" + generalizedFieldId + "} DOESN'T EXIST");
                        }

                        i += 2;
                    }
                    else
                    {
                        output.append("NOT VALID INDEX FORMAT IN FIELD %{" + generalizedFieldId + "}");
                    }

                    generalizedFieldId = new StringBuffer("");
                    estado = 0;
                }
                else
                {
                    if (c == '}')
                    {
                        Long generalizedFieldLongId = Long.valueOf(generalizedFieldId.toString());
                        GeneralizedField gf = new GeneralizedField(generalizedFieldLongId);
                        String fieldValue;
                        if (gf.getName() != null)
                        {
                            fieldValue = gf.getStringValue(issue, gf.getSuitableDateTimeFormatter(dateTimeFormatter));
                        }
                        else
                        {
                            fieldValue = "FIELD %{" + generalizedFieldId + "} DOESN'T EXIST";
                        }

                        output.append(fieldValue);
                    }
                    else
                    {
                        output.append("%{" + generalizedFieldId + c);
                    }

                    generalizedFieldId = new StringBuffer("");
                    estado = 0;
                }
            }

            i++;
        }

        return output.toString();
    }

}
