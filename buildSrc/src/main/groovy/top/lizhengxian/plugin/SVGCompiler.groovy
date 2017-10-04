package top.lizhengxian.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class SVGCompiler implements Plugin<Project> {

    @Override
    void apply(Project project) {
        println('SVGCompiler applied!')
    }
}