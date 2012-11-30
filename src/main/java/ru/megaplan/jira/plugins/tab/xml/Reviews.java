package ru.megaplan.jira.plugins.tab.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: firfi
 * Date: 22.11.12
 * Time: 14:53
 * To change this template use File | Settings | File Templates.
 */

@XStreamAlias("reviews")
public class Reviews {
    private List<ReviewData> reviews;

    public List<ReviewData> getReviews() {
        return reviews;
    }

    public void setReviews(List<ReviewData> reviews) {
        this.reviews = reviews;
    }
}
