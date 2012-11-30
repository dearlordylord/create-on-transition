package ru.megaplan.jira.plugins.workflow.validator;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.web.bean.PagerFilter;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.InvalidInputException;
import com.opensymphony.workflow.Validator;
import com.opensymphony.workflow.WorkflowException;
import org.apache.log4j.Logger;

import static ru.megaplan.jira.plugins.workflow.validator.JqlValidatorFactory.*;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: firfi
 * Date: 19.11.12
 * Time: 12:39
 * To change this template use File | Settings | File Templates.
 */
public class JqlValidator implements Validator {

    private final static Logger log = Logger.getLogger(JqlValidator.class);

    private final SearchService searchService;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final ProjectRoleManager projectRoleManager;

    public JqlValidator(SearchService searchService, JiraAuthenticationContext jiraAuthenticationContext, ProjectRoleManager projectRoleManager) {
        this.searchService = searchService;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.projectRoleManager = projectRoleManager;
    }

    @Override
    public void validate(Map transientVars, Map args, PropertySet ps) throws InvalidInputException, WorkflowException {
        Issue issue = (Issue) transientVars.get("issue");
        Project project = issue.getProjectObject();
        User user = jiraAuthenticationContext.getLoggedInUser();
        Collection<ProjectRole> roles = projectRoleManager.getProjectRoles(user, project);
        Set<String> rolesNames = new HashSet<String>();
        for(ProjectRole pr : roles) {
            rolesNames.add(pr.getName());
        }
        Map<String, String> fields = new HashMap<String, String>();//(fieldsAndDefaultsEdit);
        String jql = gets(args, JQL, fields);
        String error = gets(args, ERROR, fields);
        Integer min = Integer.parseInt(gets(args, MIN, fields));
        Integer max = Integer.parseInt(gets(args, MAX, fields));
        Set<String> ignoredRoles = new HashSet<String>(Arrays.asList(gets(args, IGNOREDROLES, fields).split(",")));
        boolean containsSome = !Collections.disjoint(rolesNames, ignoredRoles);
        if (containsSome) return;
        SearchService.ParseResult pr = searchService.parseQuery(user, jql);
        if (!pr.isValid()) {
            log.error("error parsing jql : " + jql + pr.getErrors().getErrorMessagesInEnglish());
        } else {
            try {
                SearchResults sr = searchService.search(user, pr.getQuery(), PagerFilter.newPageAlignedFilter(0, max+1));
                int n = sr.getTotal();
                if (n < min || n > max) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(error);
                    InvalidInputException e = new InvalidInputException(sb.toString());
                    for (Issue i : sr.getIssues()) {
                        e.addError(i.getKey());
                    }
                    throw e;
                }
            } catch (SearchException e) {
                log.error("error obtaining jql results", e);
            }
        }


    }

    public static String gets(Map pileofshit, String key, Map<String, String> otherPileOfShit) {
        Object o = pileofshit.get(key);
        if (o == null) {
            String shit = otherPileOfShit.get(key);
            if (shit == null) {
                return "";
            } else return shit;
        }
        else return o.toString();
    }
}
