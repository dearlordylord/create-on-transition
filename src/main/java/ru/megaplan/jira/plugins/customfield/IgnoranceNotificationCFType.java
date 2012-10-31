package ru.megaplan.jira.plugins.customfield;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.impl.CalculatedCFType;
import com.atlassian.jira.issue.customfields.impl.FieldValidationException;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.issue.link.LinkCollection;
import com.atlassian.jira.util.NotNull;
import org.apache.log4j.Logger;
import org.swift.jira.cot.functions.util.ReplaceUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: firfi
 * Date: 16.10.12
 * Time: 18:56
 * To change this template use File | Settings | File Templates.
 */
public class IgnoranceNotificationCFType extends CalculatedCFType<String, String> {

    private final static Logger log = Logger.getLogger(IgnoranceNotificationCFType.class);

    private final IssueLinkManager issueLinkManager;
    private final IssueLinkTypeManager issueLinkTypeManager;

    public IgnoranceNotificationCFType(IssueLinkManager issueLinkManager, IssueLinkTypeManager issueLinkTypeManager) {
        this.issueLinkManager = issueLinkManager;
        this.issueLinkTypeManager = issueLinkTypeManager;
    }

    @Override
    public String getStringFromSingularObject(String s) {
        return s;
    }

    @Override
    public String getSingularObjectFromString(String s) throws FieldValidationException {
        return s;
    }

    @Override
    public Map<String, Object> getVelocityParameters(Issue issue, CustomField cf, FieldLayoutItem fli) {
        Map<String, Object> result = super.getVelocityParameters(issue, cf, fli);
        result.put("values", getList(getValueFromIssue(cf, issue)));
        return result;
    }

    private List<String> getList(String valueFromIssue) {
        if (valueFromIssue == null) return new ArrayList<String>();
        else {
            return Arrays.asList(valueFromIssue.split("\n"));
        }
    }

    @Override
    public String getValueFromIssue(CustomField customField, Issue issue) {
        List<Issue> issues = getLinks(issue);
        StringBuilder res = new StringBuilder();
        for (Issue c : issues) {
            res.append(ReplaceUtil.getWorkStatus(c, false));
        }
        if (res.length() == 0) return null;
        else return res.toString();
    }

    private List<Issue> getLinks(Issue issue) {
        List<Issue> li = issueLinkManager.getLinkCollectionOverrideSecurity(issue).getInwardIssues("Иерархия");
        if (li == null) return new ArrayList<Issue>(); else return li;
    }

    @Override
    public boolean isRenderable() {
        return true;
    }



}
