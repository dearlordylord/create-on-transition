package org.swift.jira.cot.functions;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.config.StatusManager;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.issue.link.LinkCollection;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.atlassian.query.Query;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;
import org.apache.log4j.Logger;
import org.swift.jira.cot.functions.util.ReplaceUtil;
import ru.megaplan.jira.plugins.workflow.util.traveller.WorkflowTraveller;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: firfi
 * Date: 16.10.12
 * Time: 11:04
 * To change this template use File | Settings | File Templates.
 */
public class ActionWithLinkedIssuesFunction extends AbstractJiraFunctionProvider {

    private final static Logger log = Logger.getLogger(ActionWithLinkedIssuesFunction.class);

    private final Integer LINK_INWARD = 0;
    private final Integer LINK_OUTWARD = 1;

    private final Integer COMMENT_INWARD = 0;
    private final Integer COMMENT_OUTWARD = 1;

    private final IssueLinkManager issueLinkManager;
    private final CommentManager commentManager;
    private final IssueLinkTypeManager issueLinkTypeManager;
    private final WorkflowTraveller workflowTraveller;
    private final StatusManager statusManager;
    private final IssueManager issueManager;
    private final SearchService searchService;
    private final JiraAuthenticationContext jiraAuthenticationContext;

    ActionWithLinkedIssuesFunction(IssueLinkManager issueLinkManager, CommentManager commentManager, IssueLinkTypeManager issueLinkTypeManager, WorkflowTraveller workflowTraveller, StatusManager statusManager, UserManager userManager, IssueManager issueManager, SearchService searchService, JiraAuthenticationContext jiraAuthenticationContext) {

        this.issueLinkManager = issueLinkManager;
        this.commentManager = commentManager;
        this.issueLinkTypeManager = issueLinkTypeManager;
        this.workflowTraveller = workflowTraveller;
        this.statusManager = statusManager;
        this.issueManager = issueManager;
        this.searchService = searchService;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
    }

    public void act(Map transientVariables, Map<String, String> args, PropertySet ps) throws WorkflowException {
        Issue originalIssue = getIssue(transientVariables);
        String linkTypeId = args.get("field.issueLinkTypeId");
        String linkDirectionId = args.get("field.issueLinkDirection");
        String jqlFilter = args.get("field.jqlFilter");
        List<Issue> linked = getLinked(originalIssue, linkTypeId, LINK_INWARD.equals(Integer.parseInt(linkDirectionId)), jqlFilter);

        String commentDirectionId = args.get("field.issueCommentDirection");
        boolean commentInward = COMMENT_INWARD.equals(Integer.parseInt(commentDirectionId));
        List<Issue> issuesToComment = new ArrayList<Issue>();
        if (commentInward) {
            issuesToComment.add(originalIssue);
        } else {
            issuesToComment.addAll(linked);
        }


        String commentEncoded = args.get("field.linkedIssueComment");
        String comment = commentEncoded==null?null: ReplaceUtil.findReplace(commentEncoded, originalIssue, originalIssue, linked, transientVariables);
        if (comment != null && comment.trim().length() > 0)
            addComment(issuesToComment, comment);

        String linkedIssueMoveStatus = args.get("field.linkedIssueMoveStatus");
        if (linkedIssueMoveStatus != null && !linkedIssueMoveStatus.isEmpty() && !"0".equals(linkedIssueMoveStatus)) {
            Status to = statusManager.getStatus(linkedIssueMoveStatus);
            int userType = CreateUtilities.getInt(args.get("field.moveUser"), CreateUtilities.USER_CURRENT);
            User user = CreateUtilities.getUser(userType, args.get("field.specificMoveUser"), originalIssue, originalIssue, transientVariables);
            if (to != null) {
                for(Issue issue : linked) {
                    List<IssueService.TransitionValidationResult> errorResults = new ArrayList<IssueService.TransitionValidationResult>();
                    Map<String, Object> params = new HashMap<String, Object>();
                    // if (to.getId().equals("6")) params.put("resolution", "2"); this one not work if resolution set in some postfunction
                    boolean success = true;
                    try {
                        workflowTraveller.travel(user, issue, to, params, errorResults);
                    } catch (IllegalArgumentException e) {
                        success = false; // ignore this status
                    } catch (Exception e) {
                        log.error("travel exception", e);
                        success = false;
                    }
                    if (success && "6".equals(to.getId())) { // only on closed
                        MutableIssue mi;
                        if (issue instanceof MutableIssue) {
                            mi = (MutableIssue) issue;
                        } else {
                            mi = issueManager.getIssueObject(issue.getId());
                        }
                        mi.setResolutionId("2"); // not resolved
                        issueManager.updateIssue(user, mi, EventDispatchOption.DO_NOT_DISPATCH, false);
                    }

                }
            }
        }

    }

    private void addComment(List<Issue> issuesToComment, String comment) {
        for (Issue issue : issuesToComment) {
            commentManager.create(issue, "megaplan", comment, false);
        }
    }

    private List<Issue> getLinked(Issue issue, String type, boolean inward, String jqlFilter) {
        LinkCollection lc = issueLinkManager.getLinkCollectionOverrideSecurity(issue);
        String linkName = issueLinkTypeManager.getIssueLinkType(Long.parseLong(type)).getName();
        List<Issue> malyYoba;
        if (inward) malyYoba = lc.getInwardIssues(linkName); else malyYoba = lc.getOutwardIssues(linkName);
        List<Issue> ocheBolshoYoba = malyYoba==null?new ArrayList<Issue>():malyYoba;
        if (jqlFilter != null && !jqlFilter.isEmpty() && !ocheBolshoYoba.isEmpty()) {
            SearchService.ParseResult pr = searchService.parseQuery(jiraAuthenticationContext.getLoggedInUser(), jqlFilter);
            if (pr.isValid()) {
                Query q = pr.getQuery();
                log.warn("old query : " + q);
                JqlQueryBuilder builder = JqlQueryBuilder.newBuilder(q);
                List<String> yobaString = new ArrayList<String>();
                for (Issue yoba : ocheBolshoYoba) { // very nice map() method
                    String yobaKey = yoba.getKey();
                    yobaString.add(yobaKey);
                }
                builder.where().and().issue(yobaString.toArray(new String[yobaString.size()])).endWhere();
                q = builder.buildQuery();
                log.warn("new query : " + q);
                try {
                    SearchResults results = searchService.search(jiraAuthenticationContext.getLoggedInUser(), q, PagerFilter.getUnlimitedFilter());
                    ocheBolshoYoba = results.getIssues();
                } catch (SearchException e) {
                    log.error("exception in searching",e);
                }
            } else {
                log.error("jql filter : " + jqlFilter + " is not valid");
            }
        }
        return ocheBolshoYoba;
    }

    @Override
    public void execute(Map map, Map map1, PropertySet propertySet) throws WorkflowException {
        act(map, map1, propertySet);
    }
}
