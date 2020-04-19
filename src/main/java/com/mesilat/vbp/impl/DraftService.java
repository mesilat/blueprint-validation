package com.mesilat.vbp.impl;

import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

public class DraftService {
    private static final Map<Long, Draft> DRAFTS = new HashMap<>();

    public static void addDraft(Draft draft) {
        DRAFTS.put(draft.getDraftId(), draft);
    }
    public static void addDraftKey(Long draftId, String spaceKey, String templateKey) {
        DRAFTS.put(draftId, new Draft(spaceKey, templateKey));
    }
    public static Draft getDraft(Long draftId) {
        return DRAFTS.get(draftId);
    }
    public static boolean hasDraftKey(Long draftId) {
        return DRAFTS.containsKey(draftId);
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static final class Draft {
        @XmlElement(name="draft-id")
        private Long draftId;
        @XmlElement(name="space-key")
        private String spaceKey;
        @XmlElement(name="template-key")
        private String templateKey;

        public Long getDraftId() {
            return draftId;
        }
        public String getSpaceKey() {
            return spaceKey;
        }
        public String getTemplateKey() {
            return templateKey;
        }

        public Draft() {
        }
        public Draft(String spaceKey, String templateKey) {
            this.spaceKey = spaceKey;
            this.templateKey = templateKey;
        }
    }
}
