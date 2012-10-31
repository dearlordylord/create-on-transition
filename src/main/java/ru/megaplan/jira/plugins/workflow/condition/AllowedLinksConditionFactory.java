package ru.megaplan.jira.plugins.workflow.condition;

/*

!!!!!!!!!!!! This code partially sort of spizzheno. Beware and do not publish it on sort of github for example !!!!!!!!!!!!!!!!!!!!!!!!

 */

import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory;
import com.atlassian.jira.plugin.workflow.WorkflowPluginConditionFactory;
import com.opensymphony.workflow.loader.AbstractDescriptor;
import com.opensymphony.workflow.loader.ConditionDescriptor;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;
import ru.megaplan.jira.plugins.workflow.condition.util.LinkConditionUtil;

public class AllowedLinksConditionFactory extends AbstractWorkflowPluginFactory
        implements WorkflowPluginConditionFactory
{
    public static final String statusPrefix = "status_";
    public static final String issueTypePrefix = "issueType_";
    public static final String inwardLinkPrefix = "inward_";
    public static final String outwardLinkPrefix = "outward_";
    private static Logger log = Logger.getLogger(AllowedLinksConditionFactory.class);
    private final ConstantsManager constantsManager;
    private final IssueLinkTypeManager issueLinkTypeManager;

    public AllowedLinksConditionFactory(ConstantsManager constantsManager, IssueLinkTypeManager issueLinkTypeManager)
    {
        this.constantsManager = constantsManager;
        this.issueLinkTypeManager = issueLinkTypeManager;
    }

    protected void getVelocityParamsForInput(Map<String, Object> velocityParams)
    {
        Collection issueLinkTypes = this.issueLinkTypeManager.getIssueLinkTypes();
        Collection issueTypes = this.constantsManager.getAllIssueTypeObjects();
        Collection statuses = this.constantsManager.getStatusObjects();

        velocityParams.put("issueLinkTypes", Collections.unmodifiableCollection(issueLinkTypes));
        velocityParams.put("issueTypes", Collections.unmodifiableCollection(issueTypes));
        velocityParams.put("statuses", Collections.unmodifiableCollection(statuses));
    }

    protected void getVelocityParamsForEdit(Map<String, Object> velocityParams, AbstractDescriptor descriptor)
    {
        getVelocityParamsForInput(velocityParams);
        getVelocityParamsForView(velocityParams, descriptor);
    }

    protected void getVelocityParamsForView(Map<String, Object> velocityParams, AbstractDescriptor abstractDescriptor)
    {
        if (!(abstractDescriptor instanceof ConditionDescriptor))
        {
            throw new IllegalArgumentException("Descriptor must be a ConditionDescriptor.");
        }

        ConditionDescriptor descriptor = (ConditionDescriptor)abstractDescriptor;

        Collection<Long> selectedInwardLinkTypesIds = getSelectedLinkTypesIds(descriptor, "inwardIssueLinkTypes");
        Collection<Long> selectedOutwardLinkTypesIds = getSelectedLinkTypesIds(descriptor, "outwardIssueLinkTypes");
        Collection<String> selectedIssueTypesIds = getSelectedIssueTypesIds(descriptor);
        Collection<String> selectedStatusesIds = getSelectedStatusesIds(descriptor);

        List<IssueLinkType> selectedInwardIssueLinkTypes = new LinkedList<IssueLinkType>();
        List<IssueLinkType> selectedOutwardIssueLinkTypes = new LinkedList<IssueLinkType>();
        List<IssueType> selectedIssueTypes = new LinkedList<IssueType>();
        List<Status> selectedStatuses = new LinkedList<Status>();

        for (Long issueLinkTypeId : selectedInwardLinkTypesIds)
        {
            selectedInwardIssueLinkTypes.add(this.issueLinkTypeManager.getIssueLinkType(issueLinkTypeId));
        }

        for (Long issueLinkTypeId : selectedOutwardLinkTypesIds)
        {
            selectedOutwardIssueLinkTypes.add(this.issueLinkTypeManager.getIssueLinkType(issueLinkTypeId));
        }

        for (String issueTypeId : selectedIssueTypesIds)
        {
            selectedIssueTypes.add(this.constantsManager.getIssueTypeObject(issueTypeId));
        }

        for (String statusId : selectedStatusesIds)
        {
            selectedStatuses.add(this.constantsManager.getStatusObject(statusId));
        }

        String projectKeys = (String)descriptor.getArgs().get("projectKeys");

        if (projectKeys == null)
        {
            projectKeys = "";
        }

        String ignoreOtherProjects = (String)descriptor.getArgs().get("ignoreOtherProjects");

        if (ignoreOtherProjects == null)
        {
            ignoreOtherProjects = "false";
        }

        velocityParams.put("selectedIssueTypes", Collections.unmodifiableCollection(selectedIssueTypes));
        velocityParams.put("selectedInwardIssueLinkTypes", Collections.unmodifiableCollection(selectedInwardIssueLinkTypes));
        velocityParams.put("selectedOutwardIssueLinkTypes", Collections.unmodifiableCollection(selectedOutwardIssueLinkTypes));
        velocityParams.put("selectedStatuses", Collections.unmodifiableCollection(selectedStatuses));
        velocityParams.put("projectCondition", descriptor.getArgs().get("projectCondition"));
        velocityParams.put("projectKeys", projectKeys);
        velocityParams.put("projectKeysWithReplacedNames", LinkConditionUtil.replaceCustomFieldIdsWithNames(projectKeys));
        velocityParams.put("minLinks", getMinLinks(descriptor));
        velocityParams.put("maxLinks", getMaxLinks(descriptor));
        velocityParams.put("ignoreOtherProjects", ignoreOtherProjects);
        velocityParams.put("restOfLinkTypesAreAllowed", descriptor.getArgs().get("restOfLinkTypesAreAllowed"));
        velocityParams.put("restOfIssueTypesAreAllowed", descriptor.getArgs().get("restOfIssueTypesAreAllowed"));
        velocityParams.put("restOfStatusesAreAllowed", descriptor.getArgs().get("restOfStatusesAreAllowed"));
    }

    private Collection<Long> getSelectedLinkTypesIds(ConditionDescriptor descriptor, String param)
    {
        Collection selectedIssueLinkTypesIds = new LinkedList();

        String issueLinkTypes = (String)descriptor.getArgs().get(param);
        StringTokenizer st = new StringTokenizer(issueLinkTypes, ",");

        while (st.hasMoreTokens())
        {
            selectedIssueLinkTypesIds.add(Long.valueOf(st.nextToken()));
        }

        return selectedIssueLinkTypesIds;
    }

    private Collection<String> getSelectedIssueTypesIds(ConditionDescriptor descriptor)
    {
        Collection selectedIssueTypesIds = new LinkedList();

        String issueTypes = (String)descriptor.getArgs().get("issueTypes");
        StringTokenizer st = new StringTokenizer(issueTypes, ",");

        while (st.hasMoreTokens())
        {
            selectedIssueTypesIds.add(st.nextToken());
        }

        return selectedIssueTypesIds;
    }

    private Collection<String> getSelectedStatusesIds(ConditionDescriptor descriptor)
    {
        Collection selectedStatusesIds = new LinkedList();

        String statuses = (String)descriptor.getArgs().get("statuses");
        StringTokenizer st = new StringTokenizer(statuses, ",");

        while (st.hasMoreTokens())
        {
            selectedStatusesIds.add(st.nextToken());
        }

        return selectedStatusesIds;
    }

    private String getMinLinks(ConditionDescriptor descriptor)
    {
        String stringMaxLinks = (String)descriptor.getArgs().get("minLinks");
        Integer minLinks;
        try
        {
            minLinks = Integer.valueOf(stringMaxLinks);
        }
        catch (NumberFormatException e)
        {
            minLinks = Integer.valueOf(0);
        }

        return minLinks.toString();
    }

    private String getMaxLinks(ConditionDescriptor descriptor)
    {
        String stringMaxLinks = (String)descriptor.getArgs().get("maxLinks");
        Integer maxLinks;
        try
        {
            maxLinks = Integer.valueOf(stringMaxLinks);
        }
        catch (NumberFormatException e)
        {
            maxLinks = Integer.valueOf(100);
        }

        return maxLinks.toString();
    }

    public Map<String, String> getDescriptorParams(Map<String, Object> params)
    {
        String restOfLinkTypesAreAllowed = "false";
        String restOfIssueTypesAreAllowed = "false";
        String restOfStatusesAreAllowed = "false";
        String ignoreOtherProjects = "false";

        Collection<String> paramIds = params.keySet();

        StringBuffer issueTypesIds = new StringBuffer();
        StringBuffer inwardIssueLinkTypesIds = new StringBuffer();
        StringBuffer outwardIssueLinkTypesIds = new StringBuffer();
        StringBuffer statusIds = new StringBuffer();

        for (String element : paramIds)
        {
            if (element.matches("^inward_.*"))
            {
                inwardIssueLinkTypesIds.append(element.substring("inward_".length(), element.length()) + ",");
            }
            else if (element.matches("^outward_.*"))
            {
                outwardIssueLinkTypesIds.append(element.substring("outward_".length(), element.length()) + ",");
            }
            else if (element.matches("^issueType_.*"))
            {
                issueTypesIds.append(element.substring("issueType_".length(), element.length()) + ",");
            }
            else if (element.matches("^status_.*"))
            {
                statusIds.append(element.substring("status_".length(), element.length()) + ",");
            }
            else if (element.equals("restOfLinkTypesAreAllowed"))
            {
                restOfLinkTypesAreAllowed = "true";
            }
            else if (element.equals("restOfIssueTypesAreAllowed"))
            {
                restOfIssueTypesAreAllowed = "true";
            }
            else if (element.equals("restOfStatusesAreAllowed"))
            {
                restOfStatusesAreAllowed = "true";
            }
            else if (element.equals("ignoreOtherProjects"))
            {
                ignoreOtherProjects = "true";
            }
        }

        String projectKeys;
        try
        {
            projectKeys = extractSingleParam(params, "projectKeys");
        }
        catch (IllegalArgumentException e)
        {
            projectKeys = "";
        }

        String inwardRefinedIssueLinkTypes = "";
        String outwardRefinedIssueLinkTypes = "";
        String refinedIssueTypes = "";
        String refinedStatus = "";

        if (inwardIssueLinkTypesIds.length() > 0)
        {
            inwardRefinedIssueLinkTypes = inwardIssueLinkTypesIds.substring(0, inwardIssueLinkTypesIds.length() - 1);
        }

        if (outwardIssueLinkTypesIds.length() > 0)
        {
            outwardRefinedIssueLinkTypes = outwardIssueLinkTypesIds.substring(0, outwardIssueLinkTypesIds.length() - 1);
        }

        if (issueTypesIds.length() > 0)
        {
            refinedIssueTypes = issueTypesIds.substring(0, issueTypesIds.length() - 1);
        }

        if (statusIds.length() > 0)
        {
            refinedStatus = statusIds.substring(0, statusIds.length() - 1);
        }

        HashMap refinedParams = new HashMap();

        refinedParams.put("inwardIssueLinkTypes", inwardRefinedIssueLinkTypes);
        refinedParams.put("outwardIssueLinkTypes", outwardRefinedIssueLinkTypes);
        refinedParams.put("issueTypes", refinedIssueTypes);
        refinedParams.put("statuses", refinedStatus);
        refinedParams.put("minLinks", extractSingleParam(params, "minLinks"));
        refinedParams.put("maxLinks", extractSingleParam(params, "maxLinks"));
        refinedParams.put("projectCondition", extractSingleParam(params, "projectCondition"));
        refinedParams.put("projectKeys", projectKeys);
        refinedParams.put("ignoreOtherProjects", ignoreOtherProjects);
        refinedParams.put("restOfLinkTypesAreAllowed", restOfLinkTypesAreAllowed);
        refinedParams.put("restOfIssueTypesAreAllowed", restOfIssueTypesAreAllowed);
        refinedParams.put("restOfStatusesAreAllowed", restOfStatusesAreAllowed);

        return refinedParams;
    }
}