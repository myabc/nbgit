/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.netbeans.test.git.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 *
 * @author alexbcoles
 */
final class RepositoryMaintenance {

    public static void deleteFolder(File folder) {
        if (folder.isDirectory()) {
            String[] files = folder.list();
            for (int i = 0; i < files.length; i++) {
                deleteFolder(new File(folder, files[i]));
            }    
        }
        folder.delete();
    }
    
    public static int loadRepositoryFromFile(File repo, String dumpPath){
        int value = -1;
        
        repo.mkdir();
        
        File tmpOutput = new File(repo.getParent() + File.separator + "output.txt");
                
        StreamHandler shError;
        StreamHandler shOutput;
        
        try {
            String[] cmd = {"unzip", "-d", repo.getCanonicalPath(), dumpPath};
            FileOutputStream fos = new FileOutputStream(tmpOutput);
            Process p = Runtime.getRuntime().exec(cmd);
            shError = new StreamHandler(p.getErrorStream(), System.err);
            shOutput = new StreamHandler(p.getInputStream(), fos);
            shError.start();
            shOutput.start();
            value = p.waitFor();
            shError.join();
            shOutput.join();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return value;
    }
    
    public static int updateRepository(File repo) {
        int value = -1;

        String[] cmd = {"hg", "update", "--repository", repo.getAbsolutePath()};

        File tmpOutput = new File(repo.getParent() + File.separator + "output.txt");

        StreamHandler shError;
        StreamHandler shOutput;
        
        try {
            FileOutputStream fos = new FileOutputStream(tmpOutput);
            Process p = Runtime.getRuntime().exec(cmd);
            shError = new StreamHandler(p.getErrorStream(), System.err);
            shOutput = new StreamHandler(p.getInputStream(), fos);
            shError.start();
            shOutput.start();
            value = p.waitFor();
            shError.join();
            shOutput.join();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return value;
    }
         
    public static String changeFileSeparator(String path, boolean backed) {
        String changedPath = "";
        if (!backed) {
            for (int i = 0; i < path.length(); i++) {
                if (path.charAt(i) == '\\') {
                    changedPath += '/';
                } else {
                    changedPath += path.charAt(i); 
                }       
            }
        } else {
            for (int i = 0; i < path.length(); i++) {
                if (path.charAt(i) == '/') {
                    changedPath += '\\' + '\\';
                } else {
                    changedPath += path.charAt(i); 
                }       
            }
        }
        if (changedPath.startsWith("/")) 
            changedPath = changedPath.substring(1, changedPath.length());
        return changedPath;
    }
    
}
