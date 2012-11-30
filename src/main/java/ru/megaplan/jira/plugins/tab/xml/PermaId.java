package ru.megaplan.jira.plugins.tab.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Created with IntelliJ IDEA.
 * User: firfi
 * Date: 22.11.12
 * Time: 14:59
 * To change this template use File | Settings | File Templates.
 */

@XStreamAlias("permaId")
public class PermaId {
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
