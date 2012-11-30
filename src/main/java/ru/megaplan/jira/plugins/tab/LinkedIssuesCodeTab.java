package ru.megaplan.jira.plugins.tab;

import com.atlassian.applinks.api.*;
import com.atlassian.applinks.api.application.fecru.FishEyeCrucibleProjectEntityType;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.RendererManager;
import com.atlassian.jira.issue.fields.renderer.IssueRenderContext;
import com.atlassian.jira.issue.fields.renderer.JiraRendererPlugin;
import com.atlassian.jira.issue.fields.renderer.wiki.AtlassianWikiRenderer;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.plugin.issuetabpanel.*;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.basic.DateConverter;
import com.thoughtworks.xstream.converters.extended.ISO8601DateConverter;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import ru.megaplan.jira.plugins.tab.xml.ReviewData;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: firfi
 * Date: 20.11.12
 * Time: 18:36
 * To change this template use File | Settings | File Templates.
 */
public class LinkedIssuesCodeTab extends AbstractIssueTabPanel2 {

    private final static Logger log = Logger.getLogger(LinkedIssuesCodeTab.class);

    private final IssueLinkManager issueLinkManager;
    private final RendererManager rendererManager;
    private final EntityLinkService entityLinkService;
    private final IssueManager issueManager;

    private static final String linkTypeName = "Иерархия";

    public LinkedIssuesCodeTab(IssueLinkManager issueLinkManager, RendererManager rendererManager, EntityLinkService entityLinkService, IssueManager issueManager) {
        this.issueLinkManager = issueLinkManager;
        this.rendererManager = rendererManager;
        this.entityLinkService = entityLinkService;
        this.issueManager = issueManager;
    }

    @Override
    public ShowPanelReply showPanel(ShowPanelRequest showPanelRequest) {
        User u = showPanelRequest.remoteUser();
        Issue i = showPanelRequest.issue();
        boolean show = false;
        if (i.getIssueTypeObject().getName().startsWith("Mega")) {
            show = true;
        }
        return ShowPanelReply.create(show); // ohuet' prosto
    }

    JiraRendererPlugin rendererPlugin;

    @Override
    public GetActionsReply getActions(GetActionsRequest getActionsRequest) {

        XStream xstream = makeXstream();
        final List<IssueAction> results = new ArrayList<IssueAction>();
        rendererPlugin = rendererManager.getRendererForType(AtlassianWikiRenderer.RENDERER_TYPE);
        Issue issue = getActionsRequest.issue();
        Iterable<EntityLink> projects = entityLinkService.getEntityLinks(
                issue.getProjectObject(),
                FishEyeCrucibleProjectEntityType.class
        );
        if(!projects.iterator().hasNext()) {
            results.add(new GenericMessageAction("no projects linked here"));
        } else {
            ApplicationLinkRequestFactory requestFactory = projects.iterator().next().getApplicationLink().createAuthenticatedRequestFactory();

            List<IssueLink> allLinks = issueLinkManager.getInwardLinks(issue.getId());
            List<Issue> children = new ArrayList<Issue>();
            for (IssueLink in : allLinks) {
                if (linkTypeName.equals(in.getIssueLinkType().getName())) {
                    children.add(in.getSourceObject());
                }
            }

            for (Issue child : children) {
                String childKey = child.getKey();
                try {
                    ApplicationLinkRequest request = requestFactory.createRequest(Request.MethodType.GET, "/rest-service/search-v1/reviewsForIssue?jiraKey="+childKey);
                    try {
                        String result = request.execute(new ApplicationLinkResponseHandler<String>() {
                            @Override
                            public String credentialsRequired(Response response) throws ResponseException {
                                return response.getResponseBodyAsString();
                            }
                            @Override
                            public String handle(Response response) throws ResponseException {
                                InputStream shit = response.getResponseBodyAsStream();
                                StringWriter writer = new StringWriter();
                                try {
                                    IOUtils.copy(shit, writer, "UTF-8");
                                } catch (IOException e) {
                                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                                }
                                return writer.toString();
                            }
                        });

                        List<ReviewData> data = (List<ReviewData>) xstream.fromXML(result);
                        for (ReviewData reviewData : data) {
                            String actionData = buildActionData(reviewData);
                            results.add(new GenericMessageAction(actionData));
                        }

                    } catch (ResponseException e) {
                        results.add(new GenericMessageAction(e.getMessage()));
                    }
                } catch (CredentialsRequiredException e) {
                    results.add(new GenericMessageAction(e.getMessage()));
                } catch (Throwable t) {
                    log.error("error", t);
                    results.add(new GenericMessageAction("eggog:" + t.getMessage()));
                }
            }
        }

        return GetActionsReply.create(results);
    }

    private String buildActionData(ReviewData data) {
        StringBuilder sb = new StringBuilder();
        sb.append(data.getPermaId().getId()).append(" : ").append(data.getJiraIssueKey()).append(" : ").append(data.getState());
        Issue issue = issueManager.getIssueObject(data.getJiraIssueKey());
        if (issue != null) {
            return rendererPlugin.render(sb.toString(), new IssueRenderContext(issue));
        } else {
            log.warn("issue : " + data.getJiraIssueKey() + " doesn't exist");
            return sb.toString();
        }
    }

    private XStream makeXstream() {
        XStream xStream = new XStream(new DomDriver("UTF-8")) {
            @Override
            protected MapperWrapper wrapMapper(MapperWrapper next) {
                return new MapperWrapper(next) {
                    @Override
                    public boolean shouldSerializeMember(Class definedIn,
                                                         String fieldName) {
                        if (definedIn == Object.class) {
                            return false;
                        }
                        return super.shouldSerializeMember(definedIn, fieldName);
                    }
                };
            }
        };
        xStream.alias("reviews", List.class);
        xStream.alias("reviewData", ReviewData.class);
        xStream.registerConverter(new ISO8601DateConverter());

        return xStream;
    }

    class GenericMessageAction extends com.atlassian.jira.issue.tabpanels.GenericMessageAction {
        public GenericMessageAction(@javax.annotation.Nonnull String message) {
            super(message);
        }
        @Override
        public boolean isDisplayActionAllTab() {
            return false;
        }
    }

}
