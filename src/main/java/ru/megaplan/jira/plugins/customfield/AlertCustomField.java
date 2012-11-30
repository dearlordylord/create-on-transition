package ru.megaplan.jira.plugins.customfield;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.impl.CalculatedCFType;
import com.atlassian.jira.issue.customfields.impl.FieldValidationException;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: firfi
 * Date: 19.11.12
 * Time: 19:49
 * To change this template use File | Settings | File Templates.
 */
// типа заглушка на будущий функционал

public class AlertCustomField extends CalculatedCFType<String, String> {
    @Override
    public String getStringFromSingularObject(String s) {
        return null;
    }

    @Override
    public String getSingularObjectFromString(String s) throws FieldValidationException {
        return null;
    }

    @Override
    public Map<String, Object> getVelocityParameters(Issue issue, CustomField cf, FieldLayoutItem fli) {
        Map<String, Object> result = super.getVelocityParameters(issue, cf, fli);
        List<String> l = new ArrayList<String>();
        l.add("Убедитесь, что исправления вылиты на выбранный хост!");
        result.put("values", l);
        result.put("value", "Убедитесь, что исправления вылиты на выбранный хост");
        return result;
    }

    @Override
    public String getValueFromIssue(CustomField customField, Issue issue) {
        return null;
    }

    @Override
    public boolean isRenderable() {
        return true;
    }

}
