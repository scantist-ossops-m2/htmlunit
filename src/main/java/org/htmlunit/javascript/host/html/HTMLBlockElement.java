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

import static org.htmlunit.javascript.configuration.SupportedBrowser.IE;

import org.htmlunit.html.HtmlAddress;
import org.htmlunit.html.HtmlBlockQuote;
import org.htmlunit.html.HtmlCenter;
import org.htmlunit.html.HtmlExample;
import org.htmlunit.html.HtmlListing;
import org.htmlunit.html.HtmlPlainText;
import org.htmlunit.javascript.configuration.JsxClass;
import org.htmlunit.javascript.configuration.JsxGetter;
import org.htmlunit.javascript.configuration.JsxSetter;

/**
 * The JavaScript object {@code HTMLBlockElement}.
 *
 * @author Ronald Brill
 * @author Ahmed Ashour
 */
@JsxClass(domClass = HtmlAddress.class, value = IE)
@JsxClass(domClass = HtmlBlockQuote.class, value = IE)
@JsxClass(domClass = HtmlCenter.class, value = IE)
@JsxClass(domClass = HtmlExample.class, value = IE)
@JsxClass(domClass = HtmlListing.class, value = IE)
@JsxClass(domClass = HtmlPlainText.class, value = IE)
public class HTMLBlockElement extends HTMLElement {

    /**
     * Returns the value of the {@code cite} property.
     * @return the value of the {@code cite} property
     */
    public String getCite() {
        return getDomNodeOrDie().getAttributeDirect("cite");
    }

    /**
     * Returns the value of the {@code cite} property.
     * @param cite the value
     */
    public void setCite(final String cite) {
        getDomNodeOrDie().setAttribute("cite", cite);
    }

    /**
     * Returns the value of the {@code dateTime} property.
     * @return the value of the {@code dateTime} property
     */
    public String getDateTime() {
        return getDomNodeOrDie().getAttributeDirect("datetime");
    }

    /**
     * Returns the value of the {@code dateTime} property.
     * @param dateTime the value
     */
    public void setDateTime(final String dateTime) {
        getDomNodeOrDie().setAttribute("datetime", dateTime);
    }

    /**
     * Returns whether the end tag is forbidden or not.
     * @see <a href="http://www.w3.org/TR/html4/index/elements.html">HTML 4 specs</a>
     * @return whether the end tag is forbidden or not
     */
    @Override
    protected boolean isEndTagForbidden() {
        return false;
    }

    /**
     * Returns the value of the {@code clear} property.
     * @return the value of the {@code clear} property
     */
    @JsxGetter
    public String getClear() {
        return getDomNodeOrDie().getAttributeDirect("clear");
    }

    /**
     * Returns the value of the {@code clear} property.
     * @param clear the value
     */
    @JsxSetter
    public void setClear(final String clear) {
        getDomNodeOrDie().setAttribute("clear", clear);
    }

    /**
     * Returns the value of the {@code width} property.
     * @return the value of the {@code width} property
     */
    @JsxGetter(propertyName = "width")
    public String getWidth_js() {
        return getWidthOrHeight("width", Boolean.TRUE);
    }

    /**
     * Sets the value of the {@code width} property.
     * @param width the value of the {@code width} property
     */
    @JsxSetter(propertyName = "width")
    public void setWidth_js(final String width) {
        setWidthOrHeight("width", width, true);
    }

}
