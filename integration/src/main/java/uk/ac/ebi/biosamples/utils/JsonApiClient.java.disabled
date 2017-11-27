package uk.ac.ebi.biosamples.utils;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.jayway.restassured.RestAssured.given;

@SuppressWarnings("rawtypes")
public class JsonApiClient
{
    private Response currentResponse;
    private String currentUrl;
    private boolean isLoggingEnabled = false;
    private boolean ignoreFailureResponse = false;

    // ---------------------------------
    // Links relation
    // ---------------------------------

    public JsonApiClient rel(String rel)
    {
        this.currentUrl = getRelHref("", rel);
        return this;
    }

    public JsonApiClient rel(String field, String rel)
    {
        this.currentUrl = getRelHref(field, rel);
        return this;
    }

    public JsonApiClient filterRel(String field, String condition, String rel)
    {
        this.currentUrl = getRelHref(field, condition, rel);
        return this;
    }

    public String getRelHref(String field, String rel)
    {
        if (!StringUtils.isEmpty(field))
        {
            field = field + ".";
        }
        final String path = field + "_links.find{_links -> _links.rel == '" + rel + "'}.href";
        logPath(path);
        String url = currentResponse.then().extract().path(path);
        logUrl(url);
        return url;
    }

    public String getRelHref(String field, String condition, String rel)
    {
        final String path = field + ".find{" + field + " -> " + field + "." + condition + "}";
        logPath(path);
        final Map jsonObject = currentResponse.then().extract().path(path);
        return selectSelfHrefFromMap(jsonObject, rel);
    }

    // ---------------------------------
    // Links convenience methods
    // ---------------------------------

    public List<String> getSelfLinks(String field)
    {
        final List<String> hrefs = new ArrayList<>();
        logPath(field);
        final List<Map> array = currentResponse.then().extract().path(field);
        for (Map item : array)
        {
            final String url = selectSelfHrefFromMap(item);
            hrefs.add(url);
            logUrl(url);
        }
        return hrefs;
    }

    private String selectSelfHrefFromMap(Map map)
    {
        return selectSelfHrefFromMap(map, "self");
    }

    private String selectSelfHrefFromMap(Map map, String rel)
    {
        List links = (List) map.get("_links");
        for (Object link : links)
        {
            Map linkMap = (Map) link;
            if ((linkMap.get("rel")).equals(rel))
            {
                return (String) linkMap.get("href");
            }
        }
        log();
        throw new IllegalArgumentException("Could not find self link");
    }


    // ---------------------------------
    // Http requests
    // ---------------------------------

    public JsonApiClient discovery()
    {
        return url("/").get();
    }

    public JsonApiClient url(String url)
    {
        this.currentUrl = url;
        return this;
    }

    public JsonApiClient post(String json)
    {
        currentResponse = given().contentType(ContentType.JSON).body(json).post(currentUrl);
        logResponse();
        assertOkResponse();
        return this;
    }

    public JsonApiClient put(String json)
    {
        currentResponse = given().contentType(ContentType.JSON).body(json).put(currentUrl);
        logResponse();
        assertOkResponse();
        return this;
    }

    public JsonApiClient delete()
    {
        currentResponse = given().delete(currentUrl);
        logResponse();
        assertOkResponse();
        return this;
    }

    public JsonApiClient get()
    {
        currentResponse = given().get(currentUrl);
        logResponse();
        assertOkResponse();
        return this;
    }

    public JsonApiClient patch(String json)
    {
        currentResponse = given().contentType(ContentType.JSON).body(json).patch(currentUrl);
        logResponse();
        assertOkResponse();
        return this;
    }

    // ------------------------
    // Logging
    // ------------------------

    public JsonApiClient log()
    {
        isLoggingEnabled = true;
        return this;
    }

    private void logResponse()
    {
        if (isLoggingEnabled)
        {
            System.out.println(currentResponse.prettyPeek());
        }
    }

    private void logUrl(String url)
    {
        if (isLoggingEnabled)
        {
            System.out.println("self url = " + url);
        }
    }

    private void logPath(String path)
    {
        if (isLoggingEnabled)
        {
            System.out.println("Extracting from response using path: " + path);
        }
    }

    // ------------------------
    // Assertion
    // ------------------------
    public void statusCode(int statusCode)
    {
        getResponse().then().statusCode(statusCode);
    }

    private void assertOkResponse()
    {
        if (!ignoreFailureResponse)
        {
            final int status = currentResponse.statusCode();
            Assert.assertTrue(Arrays.asList(200,201).contains(status));
        }
        ignoreFailureResponse = false;
    }

    public JsonApiClient ignoreResponseCode()
    {
        ignoreFailureResponse = true;
        return this;
    }

    // ---------------------------------
    // RestAssured wrapper methods
    // ---------------------------------

    public Response getResponse()
    {
        return currentResponse;
    }

    public JsonPath getJsonPath()
    {
        return getResponse().body().jsonPath();
    }

}
