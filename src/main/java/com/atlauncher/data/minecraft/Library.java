/*
 * ATLauncher - https://github.com/ATLauncher/ATLauncher
 * Copyright (C) 2013-2019 ATLauncher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.atlauncher.data.minecraft;

import java.util.List;
import java.util.Map;

import com.atlauncher.utils.OS;

public class Library {
    public String name;
    public Downloads downloads;
    public Map<String, String> natives;
    public List<Rule> rules;
    public ExtractRule extract;

    public boolean shouldInstall() {
        if (this.rules == null) {
            return true; // No rules setup so we need it
        }

        return this.rules.stream().filter(rule -> rule.applies()).count() != 0
                && this.rules.stream().filter(rule -> rule.applies()).allMatch(rule -> rule.action == Action.ALLOW);
    }

    public boolean hasNativeForOS() {
        if (this.natives == null) {
            return false;
        }

        if (OS.isWindows() && this.natives.containsKey("windows")) {
            return true;
        }

        if (OS.isLinux() && this.natives.containsKey("linux")) {
            return true;
        }

        if (OS.isMac() && this.natives.containsKey("osx")) {
            return true;
        }

        return false;
    }

    public Download getNativeDownloadForOS() {
        if (OS.isWindows() && this.natives != null && this.natives.containsKey("windows")) {
            return this.downloads.classifiers
                    .get(this.natives.get("windows").replace("${arch}", OS.is64Bit() ? "64" : "32"));
        }

        if (OS.isLinux() && this.natives != null && this.natives.containsKey("osx")) {
            return this.downloads.classifiers
                    .get(this.natives.get("osx").replace("${arch}", OS.is64Bit() ? "64" : "32"));
        }

        if (OS.isMac() && this.natives != null && this.natives.containsKey("linux")) {
            return this.downloads.classifiers
                    .get(this.natives.get("linux").replace("${arch}", OS.is64Bit() ? "64" : "32"));
        }

        return null;
    }
}
