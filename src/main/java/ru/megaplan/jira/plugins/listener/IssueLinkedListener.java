package ru.megaplan.jira.plugins.listener;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.plugin.util.collect.Function;
import org.apache.log4j.Logger;
import org.ofbiz.core.entity.GenericValue;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import ru.megaplan.jira.plugins.history.search.HistorySearchManager;

/**
 * Created with IntelliJ IDEA.
 * User: firfi
 * Date: 16.11.12
 * Time: 12:18
 * To change this template use File | Settings | File Templates.
 */
public class IssueLinkedListener implements InitializingBean, DisposableBean {

    private final static Logger log = Logger.getLogger(IssueLinkedListener.class);

    private final static String CAPTAINCFNAME = "Капитан команды";
    private final static String COMMANDCFNAME = "Команда";
    private final static String DEVELOPERCFNAME = "Разработчик";
    //and assignee too but it isn't CF

    private final EventPublisher eventPublisher;

    private final Function<HistorySearchManager.ChangeLogRequest, String> findChangeLogFunction;

    public IssueLinkedListener(EventPublisher eventPublisher, HistorySearchManager historySearchManager) {
        this.eventPublisher = eventPublisher;
        findChangeLogFunction = historySearchManager.getFindInChangeLogFunction();
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
    public void storyBugLink(IssueEvent issueEvent) {
        log.warn("issueEvent : " + issueEvent.getIssue().getKey());
        log.warn("ch : " + issueEvent.getChangeLog());
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
