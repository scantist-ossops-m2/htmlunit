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
package org.htmlunit.javascript.host.event;

import static org.htmlunit.javascript.configuration.SupportedBrowser.CHROME;
import static org.htmlunit.javascript.configuration.SupportedBrowser.EDGE;

import org.htmlunit.corejs.javascript.ScriptableObject;
import org.htmlunit.corejs.javascript.Undefined;
import org.htmlunit.javascript.JavaScriptEngine;
import org.htmlunit.javascript.configuration.JsxClass;
import org.htmlunit.javascript.configuration.JsxConstructor;
import org.htmlunit.javascript.configuration.JsxGetter;

/**
 * A JavaScript object for {@code TextEvent}.
 *
 * @author Ahmed Ashour
 * @author Ronald Brill
 */
@JsxClass({CHROME, EDGE})
public class TextEvent extends UIEvent {

    private Object data_;

    /**
     * Default constructor.
     */
    public TextEvent() {
        data_ = Undefined.instance;
    }

    /**
     * JavaScript constructor.
     *
     * @param type the event type
     * @param details the event details (optional)
     */
    @Override
    @JsxConstructor({CHROME, EDGE})
    public void jsConstructor(final String type, final ScriptableObject details) {
        throw JavaScriptEngine.typeError("TextEvent ctor is not available");
    }

    /**
     * Retrieves the data contained.
     * @return the data contained
     */
    @JsxGetter({CHROME, EDGE})
    public Object getData() {
        return data_;
    }
}
