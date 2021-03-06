/*
 * Copyright (C) 2017 Brockmann Consult GmbH
 * This code was developed for the EC project "Fidelity and Uncertainty in
 * Climate Data Records from Earth Observations (FIDUCEO)".
 * Grant Agreement: 638822
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * A copy of the GNU General Public License should have been supplied along
 * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package com.bc.fiduceo.post.plugin.nwp;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class BashTemplateResolver {

    private final Properties properties;
    private final Pattern pattern;

    static String resolve(String template, Properties properties) {
        final BashTemplateResolver templateResolver = new BashTemplateResolver(properties);
        final String resolved = templateResolver.resolve(template);

        if (templateResolver.isResolved(resolved)) {
            return resolved;
        }

        throw new RuntimeException("Unable to resolve template:" + template);
    }

    /**
     * Creates a new instance of this class, using the properties supplied as argument
     * for resolving templates.
     *
     * @param properties The properties used for resolving templates.
     */
    BashTemplateResolver(Properties properties) {
        this(properties, "\\$\\{[\\w\\-\\.]+\\}");
    }

    private BashTemplateResolver(Properties properties, String regex) {
        this.properties = properties;
        this.pattern = Pattern.compile(regex);
    }

    /**
     * Makes a two-pass replacement for all occurrences of <code>${property-name}</code>
     * in a given template string with the value of the corresponding property.
     *
     * @param template The template string.
     * @return the template string with replacements made.
     */
    final String resolve(String template) {
        return resolve(template, 2);
    }

    /**
     * Makes an n-pass replacement for all occurrences of <code>${property-name}</code>
     * in a given template string with the value of the corresponding property.
     *
     * @param template The template string.
     * @param n        The number of passes.
     * @return the template string with replacements made.
     */
    private String resolve(String template, int n) {
        if (template == null) {
            return null;
        }

        final StringBuilder sb = new StringBuilder(template);

        Matcher matcher = pattern.matcher(sb.toString());

        for (int i = 0; i < n; i++) {
            int start = 0;
            while (matcher.find(start)) {
                start = matcher.start();
                final int end = matcher.end();

                final String key = sb.substring(start + 2, end - 1);
                final String replacement = properties.getProperty(key, System.getProperty(key));

                if (replacement != null) {
                    sb.replace(start, end, replacement);
                    matcher = pattern.matcher(sb.toString());
                    start += replacement.length();
                } else {
                    start += key.length() + 3;
                }
            }
        }

        return sb.toString();
    }

    boolean isResolved(String string) {
        return !pattern.matcher(string).find();
    }
}
