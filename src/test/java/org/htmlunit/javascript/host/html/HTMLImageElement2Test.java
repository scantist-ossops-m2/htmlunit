/*
 * Copyright (c) 2002-2024 Gargoyle Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.htmlunit.javascript.host.html;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.htmlunit.CollectingAlertHandler;
import org.htmlunit.MockWebConnection;
import org.htmlunit.SimpleWebTestCase;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.junit.BrowserRunner;
import org.htmlunit.util.MimeType;
import org.htmlunit.util.NameValuePair;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link HTMLImageElement}.
 *
 * @author <a href="mailto:george@murnock.com">George Murnock</a>
 * @author Marc Guillemot
 * @author Ronald Brill
 */
@RunWith(BrowserRunner.class)
public class HTMLImageElement2Test extends SimpleWebTestCase {

    /**
     * Verifies that if an image has an <tt>onload</tt> attribute but javascript is disabled,
     * the image is not downloaded.
     * Issue: 3123380
     * @throws Exception if an error occurs
     */
    @Test
    public void onLoad_notDownloadedWhenJavascriptDisabled() throws Exception {
        final String html = "<html><body><img src='" + URL_SECOND + "' onload='alert(1)'></body></html>";

        final WebClient client = getWebClientWithMockWebConnection();
        client.getOptions().setJavaScriptEnabled(false);

        final MockWebConnection conn = getMockWebConnection();
        conn.setResponse(URL_SECOND, "foo", MimeType.IMAGE_PNG);

        loadPageWithAlerts(html);
        assertEquals(URL_FIRST, conn.getLastWebRequest().getUrl());
    }

    /**
     * Make sure this works without an exception.
     *
     * @throws Exception on test failure
     */
    @Test
    public void onload_complex_JavascriptDisabled() throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("testfiles/tiny-jpg.img")) {
            final byte[] directBytes = IOUtils.toByteArray(is);

            final List<NameValuePair> emptyList = Collections.emptyList();
            getMockWebConnection().setResponse(URL_SECOND, directBytes, 200, "ok", MimeType.IMAGE_JPEG, emptyList);
        }

        final String html =
                "<html>\n"
                + "<head>\n"
                + "<script>\n"
                + "  function test(i) {\n"
                + "    alert('in');\n"
                + "    var image = new Image();\n"
                + "    image.onload = function () { alert(\"Image.onload(\" + i + \")\") };\n"
                + "    image.src = '" + URL_SECOND + "';\n"
                + "    alert('out');\n"
                + "  }\n"
                + "</script>\n"
                + "</head>\n"
                + "<body onload='test(0)'>\n"
                + "<button id='myId'"
                +           "onmousedown=\"alert('mousedown'); test(1)\" "
                +           "onmouseup=\"alert('mouseup'); test(2)\" "
                +           "onclick=\"alert('click'); test(3)\"></button>\n"
                + "</body>\n"
                + "</html>\n";

        final WebClient client = getWebClientWithMockWebConnection();
        client.getOptions().setJavaScriptEnabled(false);

        final List<String> collectedAlerts = new ArrayList<>();
        client.setAlertHandler(new CollectingAlertHandler(collectedAlerts));

        final HtmlPage page = loadPage(html);
        page.getElementById("myId").click();
        assertEquals(getExpectedAlerts(), collectedAlerts);
        assertEquals(URL_FIRST, getMockWebConnection().getLastWebRequest().getUrl());
    }

    /**
     * Make sure this works without an exception.
     *
     * @throws Exception on test failure
     */
    @Test
    public void onload_complex_JavascriptEngineDisabled() throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("testfiles/tiny-jpg.img")) {
            final byte[] directBytes = IOUtils.toByteArray(is);

            final List<NameValuePair> emptyList = Collections.emptyList();
            getMockWebConnection().setResponse(URL_SECOND, directBytes, 200, "ok", MimeType.IMAGE_JPEG, emptyList);
        }

        final String html =
                "<html>\n"
                + "<head>\n"
                + "<script>\n"
                + "  function test(i) {\n"
                + "    alert('in');\n"
                + "    var image = new Image();\n"
                + "    image.onload = function () { alert(\"Image.onload(\" + i + \")\") };\n"
                + "    image.src = '" + URL_SECOND + "';\n"
                + "    alert('out');\n"
                + "  }\n"
                + "</script>\n"
                + "</head>\n"
                + "<body onload='test(0)'>\n"
                + "<button id='myId'"
                +           "onmousedown=\"alert('mousedown'); test(1)\" "
                +           "onmouseup=\"alert('mouseup'); test(2)\" "
                +           "onclick=\"alert('click'); test(3)\"></button>\n"
                + "</body>\n"
                + "</html>\n";

        try (WebClient webClient = new WebClient(getBrowserVersion(), false, null, -1)) {
            final List<String> collectedAlerts = new ArrayList<>();
            webClient.setAlertHandler(new CollectingAlertHandler(collectedAlerts));

            final MockWebConnection webConnection = getMockWebConnection();
            webConnection.setResponse(URL_FIRST, html);

            webClient.setWebConnection(webConnection);

            final HtmlPage page = loadPage(html);
            page.getElementById("myId").click();
            assertEquals(getExpectedAlerts(), collectedAlerts);
            assertEquals(URL_SECOND, webConnection.getLastWebRequest().getUrl());
        }
    }

}
