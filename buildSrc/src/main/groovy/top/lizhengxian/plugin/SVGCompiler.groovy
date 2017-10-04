package top.lizhengxian.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class SVGCompiler implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.extensions.create('svg',SVG)
        project.task('compileSVG')<<{
            def svg = project['svg']
            File dir = new File("${project.name}${File.separator}${svg.path}");
            dir.listFiles().each {
                println("file:${it.name}")
            }
        }
        project.preBuild.dependsOn 'compileSVG'
    }
}