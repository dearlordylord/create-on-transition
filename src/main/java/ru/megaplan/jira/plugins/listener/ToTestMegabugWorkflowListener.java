package ru.megaplan.jira.plugins.listener;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.config.StatusManager;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.issue.label.LabelManager;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.LinkCollection;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugin.util.collect.Function;
import org.apache.log4j.Logger;
import org.ofbiz.core.entity.GenericValue;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import ru.megaplan.jira.plugins.history.search.HistorySearchManager;
import ru.megaplan.jira.plugins.workflow.util.traveller.WorkflowTraveller;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: firfi
 * Date: 19.11.12
 * Time: 16:29
 * To change this template use File | Settings | File Templates.
 */
public class ToTestMegabugWorkflowListener implements InitializingBean, DisposableBean {
    private final static Logger log = Logger.getLogger(ToTestMegabugWorkflowListener.class);

    private final static String MPSSUPPORT = "mps-support";
    private final static String MANDELBUGS = "Mandelbugs";
    private final static String STOPPEDSTATUSID = "10027"; // "Тестирование остановлено";
    private final static String MPS = "MPS";
    private final static String CLOSEDSTATUSID = "6";
    private final static String MPSCOMPLETEDID = "5"; // Сделано
    private final static Long ONTESTEVENTID = 10011L;
    private final static Long ONSTOPPEDEVENTID = 10012L; // ->Тестирование приостановлено (мандельбаг)

    private final Group mps;
    private final User bot;
    private final Status stoppedstatus;
    private final Status closedstatus;

    private final UserManager userManager;
    private final EventPublisher eventPublisher;
    private final GroupManager groupManager;
    private final LabelManager labelManager;
    private final WorkflowTraveller workflowTraveller;
    private final StatusManager statusManager;
    private final IssueLinkManager issueLinkManager;

    private final boolean hasErrors;

    ScheduledThreadPoolExecutor yourMomSchedule = new ScheduledThreadPoolExecutor(1);

    private final Function<HistorySearchManager.ChangeLogRequest, String> findChangeLogFunction;

    public ToTestMegabugWorkflowListener(EventPublisher eventPublisher, HistorySearchManager historySearchManager, UserManager userManager, GroupManager groupManager, LabelManager labelManager, WorkflowTraveller workflowTraveller, StatusManager statusManager, IssueLinkManager issueLinkManager) {

        this.eventPublisher = eventPublisher;
        this.userManager = userManager;
        this.groupManager = groupManager;
        this.labelManager = labelManager;
        this.workflowTraveller = workflowTraveller;
        this.statusManager = statusManager;
        this.issueLinkManager = issueLinkManager;
        findChangeLogFunction = historySearchManager.getFindInChangeLogFunction();
        mps = groupManager.getGroup(MPSSUPPORT);
        bot = this.userManager.getUser("megaplan");
        stoppedstatus = statusManager.getStatus(STOPPEDSTATUSID);
        closedstatus = statusManager.getStatus(CLOSEDSTATUSID);
        Map<Boolean, String> conditionsErrors = new HashMap<Boolean, String>();
        conditionsErrors.put(stoppedstatus == null, "can't find status with id : " + STOPPEDSTATUSID);
        conditionsErrors.put(closedstatus == null, "can't find status with id : " + CLOSEDSTATUSID);
        conditionsErrors.put(bot == null, "can't find bot user megaplan");
        conditionsErrors.put(mps == null, "can't find group: " + MPSSUPPORT);
        boolean error = false;
        for (Map.Entry<Boolean, String> e : conditionsErrors.entrySet()) {
            if (!e.getKey()) {
                log.error(e.getValue());
                error = true;
            }
        }
        hasErrors = error;
    }


    @EventListener
    public void toTestEvent(IssueEvent issueEvent) {
        if (hasErrors) return;
        boolean ontestevent = ONTESTEVENTID.equals(issueEvent.getEventTypeId());
        boolean onstoppedevent = ONSTOPPEDEVENTID.equals(issueEvent.getEventTypeId());
        if (!ontestevent && !onstoppedevent) return;
        final Issue issue = issueEvent.getIssue();
        User reporter = issue.getReporter();
        if (reporter == null || !groupManager.isUserInGroup(reporter, mps)) return;
        Set<Label> labels = labelManager.getLabels(issue.getId());
        Set<String> labelNames = new HashSet<String>();
        for (Label l : labels) {
            labelNames.add(l.getLabel());
        }
        if (!labelNames.contains(MANDELBUGS)) return;
        if (ontestevent) {
            yourMomSchedule.schedule(getYourMomRunnable(issue, stoppedstatus), 2, TimeUnit.SECONDS);
        } else if (onstoppedevent) {
            LinkCollection linkCollection = issueLinkManager.getLinkCollectionOverrideSecurity(issue);
            Collection<Issue> allLinks = linkCollection.getAllIssues();
            List<Issue> linkedMps = new LinkedList<Issue>();
            for (Issue a : allLinks) {
                if (MPS.equalsIgnoreCase(a.getProjectObject().getKey())) {
                    linkedMps.add(a);
                }
            }
            if (linkedMps.isEmpty()) return;
            List<Issue> linkedMpsInWork = new LinkedList<Issue>();
            for (Issue l : linkedMps) {
                String statusid = l.getStatusObject().getId();
                if (!CLOSEDSTATUSID.equals(statusid) && !MPSCOMPLETEDID.equals(statusid)) {
                    linkedMpsInWork.add(l);
                    //aah whatever
                    break;
                }
            }
            if (linkedMpsInWork.isEmpty()) {
                yourMomSchedule.schedule(getYourMomRunnable(issue, closedstatus), 2, TimeUnit.SECONDS);
            }
        }
    }

    private Runnable getYourMomRunnable(final Issue issue, final Status nextStatus) {
        return new Runnable() {
            @Override
            public void run() {
                log.warn("running your mom with status : " + nextStatus.getName());
                List<IssueService.TransitionValidationResult> errors = new ArrayList<IssueService.TransitionValidationResult>();
                workflowTraveller.travel(bot, issue, nextStatus, errors);
                if (!errors.isEmpty()) {
                    log.error("some errors on transition of issue : " + issue.getKey());
                    log.error(errors.iterator().next().getErrorCollection().getErrorMessages());
                    log.error(errors.iterator().next().getErrorCollection().getErrors().values());
                }
            }
        };
    }

    @Override
    public void destroy() throws Exception {
        eventPublisher.unregister(this);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        eventPublisher.register(this);
    }


}
