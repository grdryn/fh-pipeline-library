#!/usr/bin/groovy
package org.feedhenry

import java.text.SimpleDateFormat

static def getReleaseBranch(version) {
    "RH_v${version}"
}

static def getReleaseTag(version) {
    "rh-release-${version}-rc1"
}

static def getBuildInfoFileName() {
    'build-info.json'
}

static def mapToList(depmap) {
    def dlist = []
    for (entry in depmap) {
        dlist.add([entry.key, entry.value])
    }
    dlist
}

static def mapToOptionsString(map) {
    def optionsArray = []
    for (def o in mapToList(map)) {
        optionsArray << "${o[0]}:${o[1]}"
    }
    optionsArray.join(" ")
}

static def getArtifactsDir(name) {
    return "${name}-artifacts"
}

static def gitRepoIsDirty(untrackedFiles='no') {
    return sh(returnStdout: true, script: "git status --porcelain --untracked-files=${untrackedFiles}").trim()
}

static def getDate() {
    Date now = new Date()
    SimpleDateFormat yearMonthDateHourMin = new SimpleDateFormat("yyyyMMddHHmm")
    return yearMonthDateHourMin.format(now)
}
