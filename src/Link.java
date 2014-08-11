/**
 * Created by Tarek on 07.08.2014.
 */
public class Link {

    private String linkText;
    private String linkHref;
    private String linkType;
    private String articleType;
    private String addedIn;

    public Link(String linkText, String linkHref, String addedIn) {
        this.linkText = linkText.replace("'", "\\'");
        this.linkHref = "http://en.wikipedia.org" + linkHref;
        this.addedIn = addedIn;
        if (linkHref.startsWith("/wiki/")) {
            if (linkHref.contains(":")) {
                if (linkHref.startsWith("/wiki/Category:")) {
                    articleType = "Category";
                    linkType = "hasCategory";
                    return;
                }
            } else {
                articleType = "Article";
                linkType = "refersToArticle";
                return;
            }
        }
        throw new IllegalArgumentException ("Not a parceable doc type: " + linkHref);
    }

    public String getLinkText() {
        return linkText;
    }

    public String getLinkHref() {
        return linkHref;
    }

    public String getLinkType() {
        return linkType;
    }

    public String getAddedIn() {
        return addedIn;
    }

    public String getArticleType() {
        return articleType;
    }
}
