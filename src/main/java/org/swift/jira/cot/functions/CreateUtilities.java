/*
 * Copyright (c) 2012 Bob Swift.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *              notice, this list of conditions and the following disclaimer in the
 *            documentation and/or other materials provided with the distribution.
 *     * The names of contributors may not be used to endorse or promote products
 *           derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

/*
 *  Created on: Mar 4, 2012
 *      Author: bob
 */

package org.swift.jira.cot.functions;

import java.util.Collection;
import java.util.Map;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ofbiz.core.entity.GenericEntity;

import com.atlassian.crowd.embedded.api.User;

import static org.swift.jira.cot.functions.util.ReplaceUtil.findReplace;

public class CreateUtilities {
    static protected Log log = LogFactory.getLog(CreateUtilities.class);

    public static final int USER_REPORTER = 0;
    public static final int USER_ASSIGNEE = 1;
    public static final int USER_PROJECTLEAD = 2;
    public static final int USER_UNASSIGNED = 3;
    public static final int USER_SPECIFIC = 4;
    public static final int USER_CURRENT = 5;

    /**
     * Safe get of int value from string
     * 
     * @param value
     * @param defaultValue
     * @return value converted to int or the default value
     */
    static protected int getInt(final String value, int defaultValue) {
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (Exception exception) {
                log.debug("invalid integer: " + value, exception);
            }
        }
        return defaultValue;
    }

    /**
     * Get a clean string value for an object. Blank is default. Collections go to comma separated list.
     * 
     * @param value
     * @return non-null string
     */
    static public String clean(final Object value) {
        String result = "";  // blank is default
        if (value != null) {
            if (value instanceof String) {
                result = (String) value;
            } else if (value instanceof Collection) {
                @SuppressWarnings("rawtypes")
                Collection collection = (Collection) value;
                if (collection.size() > 0) {
                    StringBuilder builder = new StringBuilder();
                    for (Object element : collection) {
                        builder.append(clean(element)).append(',');
                    }
                    builder.deleteCharAt(builder.length() - 1); // remove last ,
                    result = builder.toString();
                }
            } else if (value instanceof User) {
                result = ((User) value).getName();
            } else if (value instanceof GenericEntity) {
                result = ((GenericEntity) value).getString("name");
            } else {
                result = value.toString();
            }
            log.debug("clean value: " + value + ", class: " + value.getClass());
        }
        return result;
    }

    /**
     * Get user based on choice value from configuration
     *
     * @param specificUser - user when user choice is USER_SPECIFIC
     * @param parentIssue - needed to provide information for some choices
     * @return
     */
    public static User getUser(final int choice, final String specificUser, final Issue parentIssue, final Issue originalIssue,
                               final Map<String, Object> transientVariables) {
        User user = null; // unassigned
        switch (choice) {

            case USER_ASSIGNEE: // 0
                user = parentIssue.getAssigneeUser();
                break;

            case USER_REPORTER: // 1
                user = parentIssue.getReporterUser();
                break;

            case USER_PROJECTLEAD: // 2
                user = parentIssue.getProjectObject().getLeadUser();
                break;

            case USER_UNASSIGNED: // 3
                break;

            case USER_SPECIFIC: // 4
                user = ComponentAccessor.getUserUtil().getUserObject(findReplace(specificUser, parentIssue, originalIssue, null, transientVariables));  // allow replacement values
                if (user == null) {
                    log.error("User: " + specificUser + " not found. Field will be unassigned.");
                }
                break;

            case USER_CURRENT: // 5
                user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
                break;

        }
        return user;
    }

}
