package com.kumuluz.ee.common.utils;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;

public class MustacheWriter {

    private static MustacheFactory mf = new DefaultMustacheFactory();

    /**
     * Writes a file in the output directory with the given template and data
     *
     * @param templateFile name of template file to use (located in the resources directory)
     * @param finalName final name of written file (will be created in the target directory)
     * @param kumuluzProject an object of data for the mustache compiler to use in the templates
     * @param outputDirectory file output directory
     */
    public static void writeFileFromTemplate(String templateFile, String finalName, KumuluzProject kumuluzProject, File outputDirectory){

        log("INFO", "Writing file: " + finalName);

        Mustache m = mf.compile(templateFile);

        StringWriter writer = new StringWriter();

        try {
            m.execute(writer, kumuluzProject).flush();
            FileWriter fileWriter = new FileWriter(outputDirectory.getAbsolutePath() + File.separator + finalName);

            fileWriter.write(writer.toString());
            fileWriter.close();
        }
        catch (IOException e){
            e.printStackTrace();
            log("WARINING", "Error writing file: " + finalName);
        }
    }

    private static void log(String prefix, String message){
        System.out.println(String.format("[%s] %s", prefix, message));
    }

}
