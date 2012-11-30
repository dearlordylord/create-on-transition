package ru.megaplan.jira.plugins.tab.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: firfi
 * Date: 22.11.12
 * Time: 14:54
 * To change this template use File | Settings | File Templates.
 */

@XStreamAlias("reviewData")
public class ReviewData {
    private String allowReviewersToJoin;
    private User author;
    private Date createDate;
    private User creator;
    private String description;
    private Date dueDate;
    private String jiraIssueKey;
    private Long metricsVersion;
    private User moderator;
    private String name;
    private PermaId permaId;
    private String projectKey;
    private String type;
    private String state;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getAllowReviewersToJoin() {
        return allowReviewersToJoin;
    }

    public void setAllowReviewersToJoin(String allowReviewersToJoin) {
        this.allowReviewersToJoin = allowReviewersToJoin;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public String getJiraIssueKey() {
        return jiraIssueKey;
    }

    public void setJiraIssueKey(String jiraIssueKey) {
        this.jiraIssueKey = jiraIssueKey;
    }

    public Long getMetricsVersion() {
        return metricsVersion;
    }

    public void setMetricsVersion(Long metricsVersion) {
        this.metricsVersion = metricsVersion;
    }

    public User getModerator() {
        return moderator;
    }

    public void setModerator(User moderator) {
        this.moderator = moderator;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PermaId getPermaId() {
        return permaId;
    }

    public void setPermaId(PermaId permaId) {
        this.permaId = permaId;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "ReviewData{" +
                "allowReviewersToJoin='" + allowReviewersToJoin + '\'' +
                ", author=" + author +
                ", createDate=" + createDate +
                ", creator=" + creator +
                ", description='" + description + '\'' +
                ", dueDate=" + dueDate +
                ", jiraIssueKey='" + jiraIssueKey + '\'' +
                ", metricsVersion=" + metricsVersion +
                ", moderator=" + moderator +
                ", name='" + name + '\'' +
                ", permaId=" + permaId +
                ", projectKey='" + projectKey + '\'' +
                ", type='" + type + '\'' +
                ", state='" + state + '\'' +
                '}';
    }
}
