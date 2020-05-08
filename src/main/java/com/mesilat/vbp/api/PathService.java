package com.mesilat.vbp.api;

public interface PathService {
    Object evaluate(String xpath);
    Object evaluate(Long pageId, String xpath);
    Object evaluate(String templateKey, String xpath);
    String evalCached(Args args);

    public static class Args {
        private String path;
        private String templateKey;
        private Long pageId;

        public String getPath() {
            return path;
        }
        public void setPath(String path) {
            this.path = path;
        }
        public String getTemplateKey() {
            return templateKey;
        }
        public void setTemplateKey(String templateKey) {
            this.templateKey = templateKey;
        }
        public Long getPageId() {
            return pageId;
        }
        public void setPageId(Long pageId) {
            this.pageId = pageId;
        }
    }
}