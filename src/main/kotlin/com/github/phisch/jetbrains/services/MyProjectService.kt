package com.github.phisch.jetbrains.services

import com.intellij.openapi.project.Project
import com.github.phisch.jetbrains.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
