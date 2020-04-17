package com.mesilat.blueprints.parser;

import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserProfile;
import me.bvn.confluence.parser.impl.User;
import me.bvn.confluence.parser.model.UserResolver;

public class DefaultUserResolver implements UserResolver {
    private final com.atlassian.sal.api.user.UserManager userManager;

    @Override
    public User getUserInfo(String userKey) {
        UserProfile user = userManager.getUserProfile(new UserKey(userKey));
        return user == null? null: new User(userKey, user.getFullName());
    }

    public DefaultUserResolver(com.atlassian.sal.api.user.UserManager userManager) {
        this.userManager = userManager;
    }
}
