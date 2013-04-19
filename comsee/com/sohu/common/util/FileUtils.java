package com.sohu.common.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.lang.reflect.Method;
//import java.util.Random;
import java.util.Stack;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class also encapsulates methods which allow Files to be
 * refered to using abstract path names which are translated to native
 * system file paths at runtime as well as copying files or setting
 * there last modification time.
 *
 * @author duncan@x180.com
 * @author Conor MacNeill
 * @author Stefan Bodewig
 * @author Magesh Umasankar
 * @author <a href="mailto:jtulley@novell.com">Jeff Tulley</a>
 *
 * @version $Revision: 1.1 $
 */

public class FileUtils {

  private static Log log = LogFactory.getLog(FileUtils.class);

  private static String  lineEnd = "";
  private static int     pushed = -2;
  private static boolean includeDelims = false;


//   private static Random rand = new Random(System.currentTimeMillis());
    private static Object lockReflection = new Object();
    private static java.lang.reflect.Method setLastModified = null;

    private static boolean onNetWare = Os.isFamily("netware");

    // for toURI
    private static boolean[] isSpecial = new boolean[256];
    private static char[] escapedChar1 = new char[256];
    private static char[] escapedChar2 = new char[256];

    /**
     * the granularity of timestamps under FAT
     */
    public static final long FAT_FILE_TIMESTAMP_GRANULARITY = 2000;


    // stolen from FilePathToURI of the Xerces-J team
    static {
        for (int i = 0; i <= 0x20; i++) {
            isSpecial[i] = true;
            escapedChar1[i] = Character.forDigit(i >> 4, 16);
            escapedChar2[i] = Character.forDigit(i & 0xf, 16);
        }
        isSpecial[0x7f] = true;
        escapedChar1[0x7f] = '7';
        escapedChar2[0x7f] = 'F';
        char[] escChs = {'<', '>', '#', '%', '"', '{', '}',
                         '|', '\\', '^', '~', '[', ']', '`'};
        int len = escChs.length;
        char ch;
        for (int i = 0; i < len; i++) {
            ch = escChs[i];
            isSpecial[ch] = true;
            escapedChar1[ch] = Character.forDigit(ch >> 4, 16);
            escapedChar2[ch] = Character.forDigit(ch & 0xf, 16);
        }
    }


   /**
    *   This class moves an input file to output file
    *
    *   @param String input file to move from
    *   @param String output file
    *
    */
    public static void move (String input, String output) throws Exception{
        File inputFile = new File(input);
        File outputFile = new File(output);
        try{
             inputFile.renameTo(outputFile);
        }catch (Exception ex) {
                     throw new Exception("Can not mv"+input+" to "+output+ex.getMessage());
       }
    }


    /**
    *  This class copies an input file to output file
    *
    *  @param String input file to copy from
    *  @param String output file
    */
    public static boolean copy(String input, String output) throws Exception{
        int BUFSIZE = 65536;
        FileInputStream fis = new FileInputStream(input);
        FileOutputStream fos = new FileOutputStream(output);

        try{
            int s;
            byte[] buf = new byte[BUFSIZE];
            while ((s = fis.read(buf)) > -1 ){
                    fos.write(buf, 0, s);
            }
       }catch (Exception ex) {
                        throw new Exception("makehome"+ex.getMessage());
       }finally{
           fis.close();
           fos.close();
       }
        return true;
    }

    public static void makehome(String home) throws Exception
        {
                File homedir=new File(home);
            if (!homedir.exists())
        {
                        try
                        {
                                homedir.mkdirs();
            }catch (Exception ex) {
                        throw new Exception("Can not mkdir :"+ home+" Maybe include special charactor!");
            }
                }
        }

    /**
    *  This class copies an input files of a directory to another directory not include subdir
    *
    *  @param String sourcedir   the directory to copy from such as:/home/bqlr/images
    *  @param String destdir      the target directory
    */
        public static void CopyDir(String sourcedir,String destdir) throws Exception
    {
        File dest = new File(destdir);
        File source = new File(sourcedir);

        String [] files= source.list();
        try
        {
              makehome(destdir);
        }catch (Exception ex) {
                      throw new Exception("CopyDir:"+ex.getMessage());
        }


        for (int i = 0; i < files.length; i++)
            {
                  String sourcefile = source+File.separator+files[i];
                  String  destfile = dest+File.separator+files[i];
              File temp = new File(sourcefile);
              if (temp.isFile()){
                              try{
                         copy(sourcefile,destfile);
                  }catch (Exception ex) {
                             throw new Exception("CopyDir:"+ex.getMessage());
                  }
                      }
        }
    }


    /**
    *  This class del a directory recursively,that means delete all files and directorys.
    *
    *  @param File directory      the directory that will be deleted.
    */
    public static void recursiveRemoveDir(File directory) throws Exception
    {
            if (!directory.exists())
                throw new IOException(directory.toString()+" do not exist!");

        String[] filelist = directory.list();
        File tmpFile = null;
        for (int i = 0; i < filelist.length; i++) {
            tmpFile = new File(directory.getAbsolutePath(),filelist[i]);
            if (tmpFile.isDirectory()) {
                  recursiveRemoveDir(tmpFile);
            } else if (tmpFile.isFile()) {
                  try{
                    tmpFile.delete();
                  }catch (Exception ex) {
                             throw new Exception(tmpFile.toString()+" can not be deleted "+ex.getMessage());
                  }
            }
        }
        try{
            directory.delete();
        }catch (Exception ex) {
                    throw new Exception(directory.toString()+" can not be deleted "+ex.getMessage());
        }finally{
            filelist=null;
        }
    }


    /**
     * &quot;normalize&quot; the given absolute path.
     *
     * <p>This includes:
     * <ul>
     *   <li>Uppercase the drive letter if there is one.</li>
     *   <li>Remove redundant slashes after the drive spec.</li>
     *   <li>resolve all ./, .\, ../ and ..\ sequences.</li>
     *   <li>DOS style paths that start with a drive letter will have
     *     \ as the separator.</li>
     * </ul>
     * Unlike <code>File#getCanonicalPath()</code> it specifically doesn't
     * resolve symbolic links.
     *
     * @param path the path to be normalized
     * @return the normalized version of the path.
     *
     * @throws java.lang.NullPointerException if the file path is
     * equal to null.
     */
    public static File normalize(String path) throws Exception {
        String orig = path;

        path = path.replace('/', File.separatorChar)
            .replace('\\', File.separatorChar);

        // make sure we are dealing with an absolute path
        int colon = path.indexOf(":");

        if (!onNetWare) {
            if (!path.startsWith(File.separator)
                && !(path.length() >= 2
                    && Character.isLetter(path.charAt(0))
                    && colon == 1)) {
                String msg = path + " is not an absolute path";
                throw new Exception(msg);
            }
        } else {
            if (!path.startsWith(File.separator)
                && (colon == -1)) {
                String msg = path + " is not an absolute path";
                throw new Exception(msg);
            }
        }

        boolean dosWithDrive = false;
        String root = null;
        // Eliminate consecutive slashes after the drive spec
        if ((!onNetWare && path.length() >= 2
                && Character.isLetter(path.charAt(0))
                && path.charAt(1) == ':')
            || (onNetWare && colon > -1)) {

            dosWithDrive = true;

            char[] ca = path.replace('/', '\\').toCharArray();
            StringBuffer sbRoot = new StringBuffer();
            for (int i = 0; i < colon; i++) {
                sbRoot.append(Character.toUpperCase(ca[i]));
            }
            sbRoot.append(':');
            if (colon + 1 < path.length()) {
                sbRoot.append(File.separatorChar);
            }
            root = sbRoot.toString();

            // Eliminate consecutive slashes after the drive spec
            StringBuffer sbPath = new StringBuffer();
            for (int i = colon + 1; i < ca.length; i++) {
                if ((ca[i] != '\\')
                    || (ca[i] == '\\' && ca[i - 1] != '\\')) {
                    sbPath.append(ca[i]);
                }
            }
            path = sbPath.toString().replace('\\', File.separatorChar);

        } else {
            if (path.length() == 1) {
                root = File.separator;
                path = "";
            } else if (path.charAt(1) == File.separatorChar) {
                // UNC drive
                root = File.separator + File.separator;
                path = path.substring(2);
            } else {
                root = File.separator;
                path = path.substring(1);
            }
        }

        Stack s = new Stack();
        s.push(root);
        StringTokenizer tok = new StringTokenizer(path, File.separator);
        while (tok.hasMoreTokens()) {
            String thisToken = tok.nextToken();
            if (".".equals(thisToken)) {
                continue;
            } else if ("..".equals(thisToken)) {
                if (s.size() < 2) {
                    throw new Exception("Cannot resolve path " + orig);
                } else {
                    s.pop();
                }
            } else { // plain component
                s.push(thisToken);
            }
        }

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.size(); i++) {
            if (i > 1) {
                // not before the filesystem root and not after it, since root
                // already contains one
                sb.append(File.separatorChar);
            }
            sb.append(s.elementAt(i));
        }


        path = sb.toString();
        if (dosWithDrive) {
            path = path.replace('/', '\\');
        }
        return new File(path);
    }

    /**
     * Emulation of File.createNewFile for JDK 1.1.
     *
     * <p>This method does <strong>not</strong> guarantee that the
     * operation is atomic.</p>
     *
     * @param f the file to be created
     * @return true if the file did not exist already.
     * @since Ant 1.5
     */
    public static boolean createNewFile(File f) throws IOException {
        if (f != null) {
            if (f.exists()) {
                return false;
            }

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(f);
                fos.write(new byte[0]);
            } finally {
                if (fos != null) {
                    fos.close();
                }
            }

            return true;
        }
        return false;
    }

    /**
     * Checks whether a given file is a symbolic link.
     *
     * <p>It doesn't really test for symbolic links but whether the
     * canonical and absolute paths of the file are identical - this
     * may lead to false positives on some platforms.</p>
     *
     * @param parent the parent directory of the file to test
     * @param name the name of the file to test.
     *
     * @return true if the file is a symbolic link.
     * @since Ant 1.5
     */
    public static boolean isSymbolicLink(File parent, String name)
        throws IOException {
        File resolvedParent = new File(parent.getCanonicalPath());
        File toTest = new File(resolvedParent, name);
        return !toTest.getAbsolutePath().equals(toTest.getCanonicalPath());
    }

    /**
     * Removes a leading path from a second path.
     *
     * @param leading The leading path, must not be null, must be absolute.
     * @param path The path to remove from, must not be null, must be absolute.
     *
     * @return path's normalized absolute if it doesn't start with
     * leading, path's path with leading's path removed otherwise.
     *
     * @since Ant 1.5
     */
    public static String removeLeadingPath(File leading, File path) throws Exception{
        String l = normalize(leading.getAbsolutePath()).getAbsolutePath();
        String p = normalize(path.getAbsolutePath()).getAbsolutePath();
        if (l.equals(p)) {
            return "";
        }

        // ensure that l ends with a /
        // so we never think /foo was a parent directory of /foobar
        if (!l.endsWith(File.separator)) {
            l += File.separator;
        }

        if (p.startsWith(l)) {
            return p.substring(l.length());
        } else {
            return p;
        }
    }

    /**
     * Compares two filenames.
     *
     * <p>Unlike java.io.File#equals this method will try to compare
     * the absolute paths and &quot;normalize&quot; the filenames
     * before comparing them.</p>
     *
     * @param f1 the file whose name is to be compared.
     * @param f2 the other file whose name is to be compared.
     *
     * @return true if the file are for the same file.
     *
     * @since Ant 1.5.3
     */
    public static boolean fileNameEquals(File f1, File f2) throws Exception{
        return normalize(f1.getAbsolutePath())
            .equals(normalize(f2.getAbsolutePath()));
    }

    /**
     * Renames a file, even if that involves crossing file system boundaries.
     *
     * <p>This will remove <code>to</code> (if it exists), ensure that
     * <code>to</code>'s parent directory exists and move
     * <code>from</code>, which involves deleting <code>from</code> as
     * well.</p>
     *
     * @throws IOException if anything bad happens during this
     * process.  Note that <code>to</code> may have been deleted
     * already when this happens.
     *
     * @param from the file to move
     * @param to the new file name
     *
     * @since Ant 1.6
     */
    public static void rename(File from, File to) throws IOException {
        if (to.exists() && !to.delete()) {
            throw new IOException("Failed to delete " + to
                                  + " while trying to rename " + from);
        }

        File parent = getParentFile(to);
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IOException("Failed to create directory " + parent
                                  + " while trying to rename " + from);
        }

        if (!from.renameTo(to)) {
            copyFile(from, to, false,false,null,null);
            if (!from.delete()) {
                throw new IOException("Failed to delete " + from
                                      + " while trying to rename it.");
            }
        }
    }
    /**
     * Emulation of File.getParentFile for JDK 1.1
     *
     *
     * @param f the file whose parent is required.
     * @return the given file's parent, or null if the file does not have a
     *         parent.
     * @since 1.10
     */
    public static File getParentFile(File f) {
        if (f != null) {
            String p = f.getParent();
            if (p != null) {
                return new File(p);
            }
        }
        return null;
    }

    public static void copyFile(File from,File to) throws IOException {
      copyFile(from, to ,false, false, null, null);
    }
    /**
     * Convienence method to copy a file from a source to a
     * destination specifying if token filtering must be used, if
     * filter chains must be used, if source files may overwrite
     * newer destination files and the last modified time of
     * <code>destFile</code> file should be made equal
     * to the last modified time of <code>sourceFile</code>.
     *
     * @param sourceFile the file to copy from.
     *                   Must not be <code>null</code>.
     * @param destFile the file to copy to.
     *                 Must not be <code>null</code>.
     * @param filters the collection of filters to apply to this copy
     * @param filterChains filterChains to apply during the copy.
     * @param overwrite Whether or not the destination file should be
     *                  overwritten if it already exists.
     * @param preserveLastModified Whether or not the last modified time of
     *                             the resulting file should be set to that
     *                             of the source file.
     * @param inputEncoding the encoding used to read the files.
     * @param outputEncoding the encoding used to write the files.
     * @param project the project instance
     *
     *
     * @throws IOException if the copying fails
     *
     * @since Ant 1.6
     */
    public static void copyFile(File sourceFile, File destFile,
                         boolean overwrite, boolean preserveLastModified,
                         String inputEncoding, String outputEncoding )
        throws IOException {

      if (overwrite || !destFile.exists()){
          //|| destFile.lastModified() < sourceFile.lastModified()) {

        if (destFile.exists() && destFile.isFile()) {
          destFile.delete();
        }

        // ensure that parent dir of dest file exists!
        // not using getParentFile method to stay 1.1 compat
        File parent = getParentFile(destFile);
        if (!parent.exists()) {
          parent.mkdirs();
        }

        BufferedReader in = null;
        BufferedWriter out = null;

        try {
          if (inputEncoding == null) {
            in = new BufferedReader(new FileReader(sourceFile));
          }
          else {
            InputStreamReader isr
                = new InputStreamReader(new FileInputStream(sourceFile),
                                        inputEncoding);
            in = new BufferedReader(isr);
          }

          if (outputEncoding == null) {
            out = new BufferedWriter(new FileWriter(destFile));
          }
          else {
            OutputStreamWriter osw
                = new OutputStreamWriter(new FileOutputStream(destFile),
                                         outputEncoding);
            out = new BufferedWriter(osw);
          }


          setIncludeDelims(true);
//          String newline = null;
          String line = getToken(in);
          while (line != null) {
            if (line.length() == 0) {
              // this should not happen, because the lines are
              // returned with the end of line delimiter
              out.newLine();
            }
            else {
              out.write(line);
            }
            line = getToken(in);
          }
        }
        finally {
          if (out != null) {
            out.close();
          }
          if (in != null) {
            in.close();
          }
        }

        if (preserveLastModified) {
          setFileLastModified(destFile, sourceFile.lastModified());
        }
      }
    }

    /**
     * Calls File.setLastModified(long time) in a Java 1.1 compatible way.
     *
     * @param file the file whose modified time is to be set
     * @param time the time to which the last modified time is to be set.
     *
     * @throws BuildException if the time cannot be set.
     */
    public static void setFileLastModified(File file, long time){
        if (JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1)) {
            return;
        }
        Long[] times = new Long[1];
        if (time < 0) {
            times[0] = new Long(System.currentTimeMillis());
        } else {
            times[0] = new Long(time);
        }

        try {
            getSetLastModified().invoke(file, times);
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable nested = ite.getTargetException();
            log.warn("Exception setting the modification time "
                                     + "of " + file, nested);
        } catch (Throwable other) {
            log.warn("Exception setting the modification time "
                                     + "of " + file, other);
        }
    }

    /**
     * see whether we have a setLastModified method in File and return it.
     *
     * @return a method to setLastModified.
     */
    public static final Method getSetLastModified() {
        if (JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1)) {
            return null;
        }
        synchronized (lockReflection) {
            if (setLastModified == null) {
                try {
                    setLastModified =
                        java.io.File.class.getMethod("setLastModified",
                                                     new Class[] {Long.TYPE});
                } catch (NoSuchMethodException nse) {

                }
            }
        }
        return setLastModified;
    }

    private static void setIncludeDelims(boolean includeDelim) {
            includeDelims = includeDelim;
        }

        /**
         * get the next line from the input
         *
         * @param in the input reader
         * @return the line excluding /r or /n, unless includedelims is set
         * @exception IOException if an error occurs reading
         */
        private static String getToken(Reader in) throws IOException {
            int ch = -1;
            if (pushed != -2) {
                ch = pushed;
                pushed = -2;
            } else {
                ch = in.read();
            }
            if (ch == -1) {
                return null;
            }

            lineEnd = "";
            StringBuffer line = new StringBuffer();

            int state = 0;
            while (ch != -1) {
                if (state == 0) {
                    if (ch == '\r') {
                        state = 1;
                    } else if (ch == '\n') {
                        lineEnd = "\n";
                        break;
                    } else {
                        line.append((char) ch);
                    }
                } else {
                    state = 0;
                    if (ch == '\n') {
                        lineEnd = "\r\n";
                    } else {
                        pushed = ch;
                        lineEnd = "\r";
                    }
                    break;
                }
                ch = in.read();
            }
            if (ch == -1 && state == 1) {
                lineEnd = "\r";
            }

            if (includeDelims) {
                line.append(lineEnd);
            }
            return line.toString();
        }

//        /**
//         * @return the line ending character(s) for the current line
//         */
//        private String getPostToken() {
//            if (includeDelims) {
//                return "";
//            }
//            return lineEnd;
//        }

        public static void main(String args[]) {
          try {
            copyFile(new File("c:\\1.txt"), new File("c:\\myolder\\3.txt"),true,false,null,null);
          }catch(Exception e ) {
            e.printStackTrace();
          }
          System.exit(0);
        }

}
