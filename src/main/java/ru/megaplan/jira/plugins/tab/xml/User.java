package ru.megaplan.jira.plugins.tab.xml;

/**
 * Created with IntelliJ IDEA.
 * User: firfi
 * Date: 22.11.12
 * Time: 14:57
 * To change this template use File | Settings | File Templates.
 */

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("user")
public class User {
    private String avatarUrl;
    private String displayName;
    private String userName;

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
