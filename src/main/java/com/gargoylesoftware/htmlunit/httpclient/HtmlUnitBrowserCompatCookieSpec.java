/*
 * Copyright (c) 2002-2022 Gargoyle Software Inc.
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
package com.gargoylesoftware.htmlunit.httpclient;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieAttributeHandler;
import org.apache.hc.client5.http.cookie.CookieOrigin;
import org.apache.hc.client5.http.cookie.CookiePathComparator;
import org.apache.hc.client5.http.cookie.MalformedCookieException;
import org.apache.hc.client5.http.impl.cookie.BasicClientCookie;
import org.apache.hc.client5.http.impl.cookie.BasicMaxAgeHandler;
import org.apache.hc.client5.http.impl.cookie.BasicSecureHandler;
import org.apache.hc.client5.http.impl.cookie.CookieSpecBase;
import org.apache.hc.client5.http.utils.DateUtils;
import org.apache.hc.core5.http.FormattedHeader;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HeaderElement;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.message.BasicHeaderElement;
import org.apache.hc.core5.http.message.BasicHeaderValueParser;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.http.message.BufferedHeader;
import org.apache.hc.core5.http.message.ParserCursor;
import org.apache.hc.core5.http.message.TokenParser;
import org.apache.hc.core5.util.CharArrayBuffer;

import com.gargoylesoftware.htmlunit.BrowserVersion;

/**
 * Customized BrowserCompatSpec for HtmlUnit.
 * <p>
 * Workaround for <a href="https://issues.apache.org/jira/browse/HTTPCLIENT-1006">HttpClient bug 1006</a>:
 * quotes are wrongly removed in cookie's values.

 * Implementation is based on the HttpClient code.
 *
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 * @author Noboru Sinohara
 * @author David D. Kilzer
 * @author Marc Guillemot
 * @author Brad Clarke
 * @author Ahmed Ashour
 * @author Nicolas Belisle
 * @author Ronald Brill
 * @author John J Murdoch
 * @author Joerg Werner
 */
public class HtmlUnitBrowserCompatCookieSpec extends CookieSpecBase {

    /** The cookie name used for cookies with no name (HttpClient doesn't like empty names). */
    public static final String EMPTY_COOKIE_NAME = "HTMLUNIT_EMPTY_COOKIE";

    /** Workaround for domain of local files. */
    public static final String LOCAL_FILESYSTEM_DOMAIN = "LOCAL_FILESYSTEM";

    /**
     * Comparator for sending cookies in right order.
     * See specification:
     * - RFC2109 (#4.3.4) http://www.ietf.org/rfc/rfc2109.txt
     * - RFC2965 (#3.3.4) http://www.ietf.org/rfc/rfc2965.txt http://www.ietf.org/rfc/rfc2109.txt
     */
    private static final Comparator<Cookie> COOKIE_COMPARATOR = new CookiePathComparator();

    private static final NetscapeDraftHeaderParser DEFAULT_NETSCAPE_DRAFT_HEADER_PARSER
                            = new NetscapeDraftHeaderParser();

    static final Date DATE_1_1_1970;

    static {
        final Calendar calendar = Calendar.getInstance(Locale.ROOT);
        calendar.setTimeZone(DateUtils.GMT);
        calendar.set(1970, Calendar.JANUARY, 1, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        DATE_1_1_1970 = calendar.getTime();
    }

    /**
     * Constructor.
     *
     * @param browserVersion the {@link BrowserVersion} to simulate
     */
    public HtmlUnitBrowserCompatCookieSpec(final BrowserVersion browserVersion) {
        super(new HtmlUnitDomainHandler(browserVersion),
                new HtmlUnitPathHandler(browserVersion),
                new BasicMaxAgeHandler(),
                new BasicSecureHandler(),
                // TODO HC5 new BasicCommentHandler(),
                new HtmlUnitExpiresHandler(browserVersion),
                new HtmlUnitHttpOnlyHandler(),
                new HtmlUnitSameSiteHandler());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Cookie> parse(Header header, final CookieOrigin origin) throws MalformedCookieException {
        // first a hack to support empty headers
        final String text = header.getValue();
        int endPos = text.indexOf(';');
        if (endPos < 0) {
            endPos = text.indexOf('=');
        }
        else {
            final int pos = text.indexOf('=');
            if (pos > endPos) {
                endPos = -1;
            }
            else {
                endPos = pos;
            }
        }
        if (endPos < 0) {
            header = new BasicHeader(header.getName(), EMPTY_COOKIE_NAME + "=" + header.getValue());
        }
        else if (endPos == 0 || StringUtils.isBlank(text.substring(0, endPos))) {
            header = new BasicHeader(header.getName(), EMPTY_COOKIE_NAME + header.getValue());
        }

        final List<Cookie> cookies;

        final String headername = header.getName();
        if (!headername.equalsIgnoreCase(HttpHeaders.SET_COOKIE)) {
            throw new MalformedCookieException("Unrecognized cookie header '" + header + "'");
        }
        final String headerValue = header.getValue();
        final ParserCursor cursor2 = new ParserCursor(0, headerValue.length());
        final HeaderElement[] helems = BasicHeaderValueParser.INSTANCE.parseElements(headerValue, cursor2);

        boolean netscape = false;
        for (final HeaderElement helem: helems) {
            if (helem.getParameterByName("expires") != null) {
                netscape = true;
            }
        }

        if (netscape) {
            // Need to parse the header again, because Netscape style cookies do not correctly
            // support multiple header elements (comma cannot be treated as an element separator)
            final CharArrayBuffer buffer;
            final ParserCursor cursor;
            if (header instanceof FormattedHeader) {
                buffer = ((FormattedHeader) header).getBuffer();
                cursor = new ParserCursor(
                        ((FormattedHeader) header).getValuePos(),
                        buffer.length());
            }
            else {
                final String s = header.getValue();
                if (s == null) {
                    throw new MalformedCookieException("Header value is null");
                }
                buffer = new CharArrayBuffer(s.length());
                buffer.append(s);
                cursor = new ParserCursor(0, buffer.length());
            }

            // TODO HC5
            final HeaderElement elem;
            try {
                elem = DEFAULT_NETSCAPE_DRAFT_HEADER_PARSER.parseHeader(buffer, cursor);
            }
            catch (final ParseException e) {
                throw new RuntimeException(e);
            }
            final String name = elem.getName();
            final String value = elem.getValue();
            if (name == null || name.isEmpty()) {
                throw new MalformedCookieException("Cookie name may not be empty");
            }
            final BasicClientCookie cookie = new BasicClientCookie(name, value);
            cookie.setPath(getDefaultPath(origin));
            cookie.setDomain(getDefaultDomain(origin));

            // cycle through the parameters
            final NameValuePair[] attribs = elem.getParameters();
            for (int j = attribs.length - 1; j >= 0; j--) {
                final NameValuePair attrib = attribs[j];
                final String s = attrib.getName().toLowerCase(Locale.ROOT);
                cookie.setAttribute(s, attrib.getValue());
                final CookieAttributeHandler handler = findAttribHandler(s);
                if (handler != null) {
                    handler.parse(cookie, attrib.getValue());
                }
            }

            // Override version for Netscape style cookies
            cookies = Collections.singletonList(cookie);
        }
        else {
            cookies = parse(helems, origin);
        }

        for (final Cookie c : cookies) {
            // re-add quotes around value if parsing as incorrectly trimmed them
            if (header.getValue().contains(c.getName() + "=\"" + c.getValue())) {
                ((BasicClientCookie) c).setValue('"' + c.getValue() + '"');
            }
        }
        return cookies;
    }

    @Override
    public List<Header> formatCookies(final List<Cookie> cookies) {
        cookies.sort(COOKIE_COMPARATOR);

        final CharArrayBuffer buffer = new CharArrayBuffer(20 * cookies.size());
        buffer.append(HttpHeaders.COOKIE);
        buffer.append(": ");
        for (int i = 0; i < cookies.size(); i++) {
            final Cookie cookie = cookies.get(i);
            if (i > 0) {
                buffer.append("; ");
            }
            final String cookieName = cookie.getName();
            final String cookieValue = cookie.getValue();

            if (!isQuoteEnclosed(cookieValue)) {
                HtmlUnitBrowserCompatCookieHeaderValueFormatter.INSTANCE.formatHeaderElement(
                        buffer,
                        new BasicHeaderElement(cookieName, cookieValue),
                        false);
            }
            else {
                // Netscape style cookies do not support quoted values
                buffer.append(cookieName);
                buffer.append("=");
                if (cookieValue != null) {
                    buffer.append(cookieValue);
                }
            }
        }
        final List<Header> headers = new ArrayList<>(1);
        headers.add(BufferedHeader.create(buffer));
        return headers;
    }

    private static boolean isQuoteEnclosed(final String s) {
        return s != null
                && s.length() > 1
                && '\"' == s.charAt(0)
                && '\"' == s.charAt(s.length() - 1);
    }

    @Override
    public String toString() {
        return "compatibility";
    }

    private static final class NetscapeDraftHeaderParser {

        private static final char PARAM_DELIMITER = ';';

        // IMPORTANT!
        // These private static variables must be treated as immutable and never exposed outside this class
        private static final BitSet TOKEN_DELIMS = TokenParser.INIT_BITSET('=', PARAM_DELIMITER);
        private static final BitSet VALUE_DELIMS = TokenParser.INIT_BITSET(PARAM_DELIMITER);

        private final TokenParser tokenParser_ = TokenParser.INSTANCE;

        HeaderElement parseHeader(final CharArrayBuffer buffer, final ParserCursor cursor) throws ParseException {
            final NameValuePair nvp = parseNameValuePair(buffer, cursor);
            final List<NameValuePair> params = new ArrayList<>();
            while (!cursor.atEnd()) {
                final NameValuePair param = parseNameValuePair(buffer, cursor);
                params.add(param);
            }

            return new BasicHeaderElement(nvp.getName(), nvp.getValue(),
                    params.toArray(new NameValuePair[0]));
        }

        private NameValuePair parseNameValuePair(final CharArrayBuffer buffer, final ParserCursor cursor) {
            final String name = tokenParser_.parseToken(buffer, cursor, TOKEN_DELIMS);
            if (cursor.atEnd()) {
                return new BasicNameValuePair(name, null);
            }

            final int delim = buffer.charAt(cursor.getPos());
            cursor.updatePos(cursor.getPos() + 1);
            if (delim != '=') {
                return new BasicNameValuePair(name, null);
            }

            final String value = tokenParser_.parseToken(buffer, cursor, VALUE_DELIMS);
            if (!cursor.atEnd()) {
                cursor.updatePos(cursor.getPos() + 1);
            }

            return new BasicNameValuePair(name, value);
        }
    }
}
