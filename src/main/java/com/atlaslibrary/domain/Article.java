package com.atlaslibrary.domain;

import java.util.UUID;

public class Article {
    public String id = UUID.randomUUID().toString();
    public String slug;
    public String title;
    public String markdownBody;
}
