package com.yang.yangaiagent.entity;


import java.util.List;

/**
 * 结构化输出类
 */
public class LoveReport {

    private String title;

    private List<String> suggestions;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }
}
