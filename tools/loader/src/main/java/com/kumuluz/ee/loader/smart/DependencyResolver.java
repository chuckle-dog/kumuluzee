/*
 *  Copyright (c) 2014-2017 Kumuluz and/or its affiliates
 *  and other contributors as indicated by the @author tags and
 *  the contributor list.
 *
 *  Licensed under the MIT License (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/licenses/MIT
 *
 *  The software is provided "AS IS", WITHOUT WARRANTY OF ANY KIND, express or
 *  implied, including but not limited to the warranties of merchantability,
 *  fitness for a particular purpose and noninfringement. in no event shall the
 *  authors or copyright holders be liable for any claim, damages or other
 *  liability, whether in an action of contract, tort or otherwise, arising from,
 *  out of or in connection with the software or the use or other dealings in the
 *  software. See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.kumuluz.ee.loader.smart;

import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

public class DependencyResolver {

    private static final String DEPENDENCY_DIR = File.separator + "lib";

    private static final Logger log = Logger.getLogger(DependencyResolver.class.getName());

    public static boolean resolveDependencies(String[] repos, List<String> missingDeps){

        log.info("Checking local dependencies.");

        boolean dependenciesOk = false;

        if (missingDeps.size() > 0){

            log.info("A total of " + missingDeps.size() + " dependencies are missing:");
            for (String dep : missingDeps){
                log.info(dep.substring(dep.lastIndexOf("/") + 1));
            }

            if (!SmartPrompter.promptUserForBoolean("Download missing dependencies?")){
                return false;
            }

            missingDeps = downloadDependencies(repos, missingDeps);
            if (missingDeps.size() > 0){
                log.warning(missingDeps.size() + " could not be downloaded.");
            }
            else {
                log.info("All dependencies downloaded.");
                dependenciesOk = true;
            }
        }
        else {
            log.info("All dependencies are present.");
            dependenciesOk = true;
        }

        return dependenciesOk;

    }

    public static List<String> checkDependencies(String[] depPaths){

        File depsDir = new File(getJarDirectory() + DEPENDENCY_DIR);

        List<String> missingDeps = new LinkedList<>();
        if (depsDir.isDirectory()){
            for (String dep : depPaths){
                String depFilename = dep.substring(dep.lastIndexOf("/") + 1);
                if (!isFileInDirectory(depFilename, depsDir)){
                    missingDeps.add(dep);
                }
            }
        }
        else {
            return Arrays.asList(depPaths);
        }

        return missingDeps;
    }

    private static List<String> downloadDependencies(String[] repoPaths, List<String> depPaths){

        try {
            File depsDir = new File(getJarDirectory() + DEPENDENCY_DIR);

            List<String> missingDeps = new LinkedList<>();

            if (depsDir.exists() || depsDir.mkdir()) {
                for (String depPath : depPaths) {
                    String filename = depPath.substring(depPath.lastIndexOf("/"));

                    boolean downloaded = false;

                    for (String repoPath : repoPaths) {
                        URL url = new URL(String.join("/",repoPath, depPath));

                        HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
                        httpURLConnection.setInstanceFollowRedirects(false);
                        if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {

                            ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());

                            FileOutputStream fos = new FileOutputStream(depsDir.getAbsolutePath() + filename);

                            fos.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

                            log.info("Downloaded dependency: " + filename.substring(1));

                            downloaded = true;
                            break;
                        }
                    }
                    if (!downloaded){
                        log.warning("Could not find dependency: " + filename);
                        missingDeps.add(depPath);
                    }
                }
            }
            else {
                log.warning("Could not find nor create dependency directory at '" + getJarDirectory() + "'");
            }
            return missingDeps;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static String getJarDirectory(){

        try {
            String jarPath = new File(EeSmartLoader.class.getProtectionDomain().getCodeSource().getLocation()
                    .toURI()).getPath();

            return jarPath.substring(0, jarPath.lastIndexOf(File.separator));
        }
        catch (URISyntaxException e){
            e.printStackTrace();
        }

        return null;
    }

    public static boolean isFileInDirectory(String filename, File directory){
        if (directory.isDirectory()){
            for (File file : directory.listFiles()){
                if (filename.equals(file.getName())){
                    return true;
                }
            }
        }
        return false;
    }

}
