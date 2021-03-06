package com.hupubao.dockit

import com.hupubao.dockit.parser.CommentParser
import com.hupubao.dockit.template.MarkdownTemplate
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import org.apache.maven.project.MavenProject
import java.awt.Desktop
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Paths


/**
 * @goal dockit
 */
@Mojo(name = "dockit", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE)
class DockitMojo : AbstractMojo() {


    @Parameter(defaultValue = "DEFAULT", property = "template", required = true)
    lateinit var template: String

    @Parameter(defaultValue = "UTF-8", property = "templateCharset", required = true)
    lateinit var templateCharset: String

    @Parameter(defaultValue = "DEFAULT", property = "outDir", required = true)
    lateinit var outDir: String

    @Parameter(defaultValue = "false", property = "singleOutDir", required = false)
    lateinit var singleOutDir: String

    @Parameter(defaultValue = "false", property = "autoOpenOutDir", required = false)
    lateinit var autoOpenOutDir: String

    @Parameter(defaultValue = "\${project}", required = true, readonly = true)
    lateinit var project: MavenProject

    @Throws(MojoExecutionException::class, MojoFailureException::class)
    override fun execute() {
        log.info("[dockit]Start dockit......")
        log.info("[dockit]Project basedir:${project.basedir}")
        log.info("[dockit]Load template:$template")

        val templateText: String
        if (template == "DEFAULT") {
            template = "/template/DEFAULT.MD"
            templateText = DockitMojo::class.java.getResource(template).readText(Charset.forName(templateCharset))
        } else {
            templateText = File(template).readText(Charset.forName(templateCharset))
        }

        log.info("[dockit]Load template :$template success.")


        val classNodeList = CommentParser.parseComments(project, log)

        if (outDir == "DEFAULT") {
            outDir = Paths.get(project.build.directory, "dockit").toString()
        }

        outDir = outDir.replace("\\", "/")

        val outDirFifle = File(outDir)
        if (!outDirFifle.exists()) {
            outDirFifle.mkdirs();
        }

        if (classNodeList.isEmpty()) {
            log.info("[dockit]Nothing to generate, are you sure you have a \"@dockit\" label in your class java doc ?")
        }

        classNodeList.parallelStream().forEach { classNode ->

            val outDirectory = Paths.get(
                outDir,
                if ("true" == singleOutDir.toLowerCase()) "" else classNode.classNameOrClassDescription()
            ).toString()

            classNode.methodCommentNodeList.parallelStream().forEach { methodCommentNode ->

                var title = methodCommentNode.title

                if (title == null || title.isEmpty()) {
                    return@forEach
                }

                title = title.replace("/", "")

                val mdText = MarkdownTemplate(project, log, templateText, methodCommentNode).render()
                val pathOut = Paths.get(outDirectory, "$title.MD")
                val file = pathOut.toFile()
                /*if (file.exists()) {
                    file.delete()
                }*/

                if (!file.parentFile.exists()) {
                    file.parentFile.mkdirs()
                }
                file.createNewFile()
                log.info("[dockit]Generate doc to $pathOut")


                file.writeText(mdText)
            }

        }


        // 打开目录
        if (autoOpenOutDir.toBoolean() && Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(outDirFifle)
        }


    }

}