package ru.megaplan.jira.plugins.listener;

import com.atlassian.crowd.event.Events;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.JiraEvent;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.plugin.util.collect.Function;
import org.apache.log4j.Logger;
import org.ofbiz.core.entity.GenericValue;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import ru.megaplan.jira.plugins.history.search.HistorySearchManager;

/**
 * Created with IntelliJ IDEA.
 * User: firfi
 * Date: 20.11.12
 * Time: 17:00
 * To change this template use File | Settings | File Templates.
 */
public class CopyCommentListener implements InitializingBean, DisposableBean {

    private final static Logger log = Logger.getLogger(CopyCommentListener.class);

    // we use only storypapa custom field because se don't want enumerate all subtask types here
    private final static String fatherName = "Стори-папа";

    private final CustomField fatherCf;

    private final CustomFieldManager customFieldManager;

    private final EventPublisher eventPublisher;
    private final IssueManager issueManager;
    private final CommentManager commentManager;

    private final Function<HistorySearchManager.ChangeLogRequest, String> findChangeLogFunction;

    public CopyCommentListener(EventPublisher eventPublisher, HistorySearchManager historySearchManager, CustomFieldManager customFieldManager, IssueManager issueManager, CommentManager commentManager) {
        this.eventPublisher = eventPublisher;
        this.customFieldManager = customFieldManager;
        this.issueManager = issueManager;
        this.commentManager = commentManager;
        findChangeLogFunction = historySearchManager.getFindInChangeLogFunction();
        fatherCf = customFieldManager.getCustomFieldObjectByName(fatherName);
        if (fatherCf == null) {
            log.error("can't find fatherCf custom field with name : " + fatherName);
        }
    }

    @Override
    public void destroy() throws Exception {
        eventPublisher.unregister(this);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        eventPublisher.register(this);
    }

    @EventListener
    public void commentInSubtasks(IssueEvent issueEvent) {
        Comment comment = issueEvent.getComment();
        if (comment == null) return;
        Issue issue = issueEvent.getIssue();
        Object cfObject = issue.getCustomFieldValue(fatherCf);
        if (cfObject == null) return;
        String cfValue = cfObject.toString();
        Issue father = issueManager.getIssueObject(cfValue);
        if (father == null) {
            log.warn("perhaps issue : " + father + " deleted or moved");
            return;
        }
        boolean dispatchEvent = false;


        StringBuilder sb = new StringBuilder().append("Комментарий к позадаче ").append(issue.getKey()).
                append(" ").append("«").append(issue.getSummary()).append("»");
        sb.append("\n\n");
        sb.append(comment.getBody());
        commentManager.create(
                father,
                comment.getAuthor(),
                sb.toString(),
                comment.getGroupLevel(),
                comment.getRoleLevelId(),
                dispatchEvent
        );
        //and fuck you
    }

    private Boolean isStoryBugLink(GenericValue changeLog) {
        return false;
    }

    private String getLinkType(GenericValue changeLog) {
        HistorySearchManager.ChangeLogRequest changeLogRequest = new HistorySearchManager.ChangeLogRequest(changeLog, "Link");
        changeLogRequest.setLog(log);
        return findChangeLogFunction.get(changeLogRequest);
    }

}
