import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.codec.binary.Hex;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Tarek on 07.08.2014.
 */
public class WikiReader {

    private static MessageDigest md;
    public static final String SERVER_ROOT_URI = "http://localhost:7474/";

    static {
        try {
             md = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {}
    }

    private String url;
    private ArrayList<Link> links = new ArrayList<Link>();
    private static HashMap<String, Link> existingNodes = new HashMap<String, Link>();

    public static void main(String[] args) throws Exception {
        /*System.out.println("CREATE (currentNode:Article {href:'http://en.wikipedia.org/wiki/Wikipedia'});");
        existingNodes.put("http://en.wikipedia.org/wiki/Wikipedia", new Link("", "/wiki/Wikipedia", "http://en.wikipedia.org/wiki/Wikipedia"));
        WikiReader reader = new WikiReader("http://en.wikipedia.org/wiki/Wikipedia");
        reader.run();
        System.out.println(reader.getCreationStatement());
        System.out.println();
        System.out.println();
        System.out.println();
        WikiReader reader2 = new WikiReader("http://en.wikipedia.org/wiki/Wikipedia_(disambiguation)");
        reader2.run();
        System.out.println(reader2.getCreationStatement());

        WikiReader reader3 = new WikiReader("http://en.wikipedia.org/wiki/Nature_(journal)");
        reader3.run();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println(reader3.getCreationStatement());*/

        sendCipher("MATCH (n) RETURN n LIMIT 100");
    }

    public WikiReader(String url) {
        this.url = url;
    }

    public boolean run() {
        links = new ArrayList<Link>();
        try {
            Document document = Jsoup.connect(this.url).get();
            Element bodyContent = document.getElementById("bodyContent");
            Elements links = bodyContent.getElementsByTag("a");
            for (Element element : links) {
                try {
                    this.links.add(new Link(element.text(), element.attr("href"), this.url));
                } catch (IllegalArgumentException e) {
                    //IGNORE
                }
            }
        } catch (IOException e) {
            System.err.println(url + " could not be read");
            return false;
        }
        return true;
    }

    public String getCreationStatement() {
        ArrayList<String> match = new ArrayList<String> ();
        ArrayList<String> matchWhere = new ArrayList<String> ();
        StringBuilder out = new StringBuilder();
        int linksSize = links.size();
        out.append("\nCREATE");
        for (int i=0; i < linksSize; i++) {
            Link l = links.get(i);
            if (!existingNodes.containsKey(l.getLinkHref())) {
                out.append("\n ("+sha256(l.getLinkHref())+":" + l.getArticleType() + " {href:'" + l.getLinkHref() + "'}),");
                existingNodes.put(l.getLinkHref(), l);
            } else {
                if (!existingNodes.get(l.getLinkHref()).getAddedIn().equals(this.url)) {
                    match.add("(" + sha256(l.getLinkHref()) + ":" + l.getArticleType() + ")");
                    matchWhere.add(sha256(l.getLinkHref()) + ".href = '" + l.getLinkHref() + "'");
                }
            }
            out.append("\n (currentNode)-[:" + l.getLinkType() + " {title:'" + l.getLinkText() + "'}]->(" + sha256(l.getLinkHref()) + ")");
            if (i+1 < linksSize) {
                out.append(",");
            }
        }
        out.append(";");
        StringBuilder finalString = new StringBuilder();
        finalString.append("MATCH (currentNode)");
        for (String s : match) {
            finalString.append(", " + s);
        }
        finalString.append(" WHERE currentNode.href = '" + this.url + "'");
        for (String s : matchWhere) {
            finalString.append(" AND " + s);
        }
        finalString.append(out);
        return finalString.toString();
    }

    public static String sha256(String text) {
        try {
            md.update(text.getBytes("UTF-8")); // Change this to "UTF-16" if needed
            byte[] digest = md.digest();
            String result = Hex.encodeHexString(digest);
            return "hash" + result;
        } catch (Exception e) {
            return "";
        }
    }

    public static void sendCipher(String query) {
        final String txUri = SERVER_ROOT_URI + "transaction/commit";
        WebResource resource = Client.create().resource( txUri );

        String payload = "{\"statements\" : [ {\"statement\" : \"" + query + "\"} ]}";
        ClientResponse response = resource
                .accept( "application/json" )
                .type( "application/json" )
                .entity( payload )
                .post( ClientResponse.class );

        System.out.println( String.format(
                "POST [%s] to [%s], status code [%d], returned data: "
                        + System.getProperty( "line.separator" ) + "%s",
                payload, txUri, response.getStatus(),
                response.getEntity( String.class ) ) );

        response.close();
    }
}