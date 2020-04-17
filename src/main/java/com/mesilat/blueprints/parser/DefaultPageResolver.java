package com.mesilat.blueprints.parser;

import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;
import me.bvn.confluence.parser.impl.PageLink;
import me.bvn.confluence.parser.model.PageResolver;

public class DefaultPageResolver implements PageResolver {
    private final com.atlassian.confluence.pages.PageManager pageManager;
    private final String spaceKey;

    @Override
    public PageLink getPageInfo(String spaceKey, String title) {
        if (title == null) {
            return null;
        }
        if (spaceKey == null && this.spaceKey == null) {
            return null;
        }
        Page page = pageManager.getPage(spaceKey == null? this.spaceKey: spaceKey, title);
        return page == null? null: new PageLink(page.getId(), page.getTitle());
    }

    public DefaultPageResolver(PageManager pageManager, String spaceKey) {
        this.pageManager = pageManager;
        this.spaceKey = spaceKey;
    }

}
