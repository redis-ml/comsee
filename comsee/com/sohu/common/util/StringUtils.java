package com.sohu.common.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.StringTokenizer;

import org.apache.oro.text.perl.Perl5Util;

/**
 * Utility class to peform common String manipulation algorithms.
 */
public class StringUtils {

//	public static Perl5Compiler compiler = new Perl5Compiler();
//	public static Perl5Matcher matcher = new Perl5Matcher();

    // Constants used by escapeHTMLTags
	/**
	 * 按照指定的长度，得到中英文组成的字符串的子串。
	 * 
	 * @param string 截取来源字符串
	 * @param length 显示长度
	 * @param noWrap 是否含有换行字符
	 * @param padTail 未使用
	 * @return
	 */
	public static final String chopAtWordChn(String string, int length, boolean noWrap, boolean padTail){
        if (string == null) {
            return string;
        }

         int tmpLength = 0;
         int strlen = string.length();
        for( int i =0; i< strlen; i++){
        	if(noWrap) {

                // First check if there is a newline character before length; if so,
                // chop word there.
                    if (string.charAt(i) == '\r' && string.charAt(i+1) == '\n') {
                        return string.substring(0, i+1);
                    }
                    // Unix
                    else if (string.charAt(i) == '\n') {
                        return string.substring(0, i);
                    }
        	}
        	
        	// 检查当前的字符的可能显示宽度
        	int localeLength = 1; // 默认的Ascii码字符宽度为1.
        	if( string.charAt(i) > 0xff) {
        		localeLength = 2;
        	}
        	
        	// 检查当前字符显示后是否会越界
        	int availableLength = tmpLength + localeLength;
        	if( availableLength >= length ){
        		// 此种情况为：插入当前字符前小于指定长度，插入后则大于指定长度，
        		// 必定是插入前比指定长度小一个字符，且当前字符一定是汉字，
        		if( i == strlen -1 ) {
        			return string;
        		} else if( i > 1 ) {
        			return string.substring( 0, i - 1 )+"...";
        		} else {
        			return "...";
        		}
        	} else {
        		tmpLength = availableLength;
        	}

        }
        if( noWrap ){
            // Also check boundary case of Unix newline
            if (string.charAt( strlen - 1 ) == '\n') {
                return string.substring(0, strlen - 1 );
            }
        }
        // Did not find word boundary so return original String chopped at
        // specified length.
        return string;
	}
    private static final char[] QUOTE_ENCODE = "&quot;".toCharArray();
    private static final char[] AMP_ENCODE = "&amp;".toCharArray();
    private static final char[] LT_ENCODE = "&lt;".toCharArray();
    private static final char[] GT_ENCODE = "&gt;".toCharArray();

    /**
     * Replaces all instances of oldString with newString in line.
     *
     * @param line the String to search to perform replacements on
     * @param oldString the String that should be replaced by newString
     * @param newString the String that will replace all instances of oldString
     *
     * @return a String will all instances of oldString replaced by newString
     */
    public static final String replace( String line, String oldString, String newString )
    {
        if (line == null) {
            return null;
        }
        int i=0;
        if ( ( i=line.indexOf( oldString, i ) ) >= 0 ) {
            char [] line2 = line.toCharArray();
            char [] newString2 = newString.toCharArray();
            int oLength = oldString.length();
            StringBuffer buf = new StringBuffer(line2.length);
            buf.append(line2, 0, i).append(newString2);
            i += oLength;
            int j = i;
            while( ( i=line.indexOf( oldString, i ) ) > 0 ) {
                buf.append(line2, j, i-j).append(newString2);
                i += oLength;
                j = i;
            }
            buf.append(line2, j, line2.length - j);
            return buf.toString();
        }
        return line;
    }

    /**
     * Replaces all instances of oldString with newString in line with the
     * added feature that matches of newString in oldString ignore case.
     *
     * @param line the String to search to perform replacements on
     * @param oldString the String that should be replaced by newString
     * @param newString the String that will replace all instances of oldString
     *
     * @return a String will all instances of oldString replaced by newString
     */
    public static final String replaceIgnoreCase(String line, String oldString,
            String newString)
    {
        if (line == null) {
            return null;
        }
        String lcLine = line.toLowerCase();
        String lcOldString = oldString.toLowerCase();
        int i=0;
        if ((i=lcLine.indexOf(lcOldString, i)) >= 0) {
            char [] line2 = line.toCharArray();
            char [] newString2 = newString.toCharArray();
            int oLength = oldString.length();
            StringBuffer buf = new StringBuffer(line2.length);
            buf.append(line2, 0, i).append(newString2);
            i += oLength;
            int j = i;
            while ((i=lcLine.indexOf(lcOldString, i)) > 0) {
                buf.append(line2, j, i-j).append(newString2);
                i += oLength;
                j = i;
            }
            buf.append(line2, j, line2.length - j);
            return buf.toString();
        }
        return line;
    }

    /**
     * Replaces all instances of oldString with newString in line with the
     * added feature that matches of newString in oldString ignore case.
     * The count paramater is set to the number of replaces performed.
     *
     * @param line the String to search to perform replacements on
     * @param oldString the String that should be replaced by newString
     * @param newString the String that will replace all instances of oldString
     * @param count a value that will be updated with the number of replaces
     *      performed.
     *
     * @return a String will all instances of oldString replaced by newString
     */
    public static final String replaceIgnoreCase(String line, String oldString,
            String newString, int [] count)
    {
        if (line == null) {
            return null;
        }
        String lcLine = line.toLowerCase();
        String lcOldString = oldString.toLowerCase();
        int i=0;
        if ((i=lcLine.indexOf(lcOldString, i)) >= 0) {
            int counter = 0;
            char [] line2 = line.toCharArray();
            char [] newString2 = newString.toCharArray();
            int oLength = oldString.length();
            StringBuffer buf = new StringBuffer(line2.length);
            buf.append(line2, 0, i).append(newString2);
            i += oLength;
            int j = i;
            while ((i=lcLine.indexOf(lcOldString, i)) > 0) {
                counter++;
                buf.append(line2, j, i-j).append(newString2);
                i += oLength;
                j = i;
            }
            buf.append(line2, j, line2.length - j);
            count[0] = counter;
            return buf.toString();
        }
        return line;
    }

   /**
    * Replaces all instances of oldString with newString in line.
    * The count Integer is updated with number of replaces.
    *
    * @param line the String to search to perform replacements on
    * @param oldString the String that should be replaced by newString
    * @param newString the String that will replace all instances of oldString
    *
    * @return a String will all instances of oldString replaced by newString
    */
    public static final String replace(String line, String oldString,
            String newString, int[] count)
    {
        if (line == null) {
            return null;
        }
        int i=0;
        if ((i=line.indexOf(oldString, i)) >= 0) {
            int counter = 0;
            counter++;
            char [] line2 = line.toCharArray();
            char [] newString2 = newString.toCharArray();
            int oLength = oldString.length();
            StringBuffer buf = new StringBuffer(line2.length);
            buf.append(line2, 0, i).append(newString2);
            i += oLength;
            int j = i;
            while ((i=line.indexOf(oldString, i)) > 0) {
                counter++;
                buf.append(line2, j, i-j).append(newString2);
                i += oLength;
                j = i;
            }
            buf.append(line2, j, line2.length-j);
            count[0] = counter;
            return buf.toString();
        }
        return line;
    }

    /**
     * This method takes a string which may contain HTML tags (ie, &lt;b&gt;,
     * &lt;table&gt;, etc) and converts the '&lt'' and '&gt;' characters to
     * their HTML escape sequences.
     *
     * @param in the text to be converted.
     * @return the input string with the characters '&lt;' and '&gt;' replaced
     *  with their HTML escape sequences.
     */
    public static final String escapeHTMLTags(String in) {
        if (in == null) {
            return null;
        }
        char ch;
        int i=0;
        int last=0;
        char[] input = in.toCharArray();
        int len = input.length;
        StringBuffer out = new StringBuffer((int)(len*1.3));
        for (; i < len; i++) {
            ch = input[i];
            if (ch > '>') {
                continue;
            } else if (ch == '<') {
                if (i > last) {
                    out.append(input, last, i - last);
                }
                last = i + 1;
                out.append(LT_ENCODE);
            } else if (ch == '>') {
                if (i > last) {
                    out.append(input, last, i - last);
                }
                last = i + 1;
                out.append(GT_ENCODE);
            }
        }
        if (last == 0) {
            return in;
        }
        if (i > last) {
            out.append(input, last, i - last);
        }
        return out.toString();
    }

    /**
     * Used by the hash method.
     */
    private static MessageDigest digest = null;

    /**
     * Hashes a String using the Md5 algorithm and returns the result as a
     * String of hexadecimal numbers. This method is synchronized to avoid
     * excessive MessageDigest object creation. If calling this method becomes
     * a bottleneck in your code, you may wish to maintain a pool of
     * MessageDigest objects instead of using this method.
     * <p>
     * A hash is a one-way function -- that is, given an
     * input, an output is easily computed. However, given the output, the
     * input is almost impossible to compute. This is useful for passwords
     * since we can store the hash and a hacker will then have a very hard time
     * determining the original password.
     * <p>
     * In Jive, every time a user logs in, we simply
     * take their plain text password, compute the hash, and compare the
     * generated hash to the stored hash. Since it is almost impossible that
     * two passwords will generate the same hash, we know if the user gave us
     * the correct password or not. The only negative to this system is that
     * password recovery is basically impossible. Therefore, a reset password
     * method is used instead.
     *
     * @param data the String to compute the hash of.
     * @return a hashed version of the passed-in String
     */
    public synchronized static final String hash(String data) {
        if (digest == null) {
            try {
                digest = MessageDigest.getInstance("MD5");
            }
            catch (NoSuchAlgorithmException nsae) {
                System.err.println("Failed to load the MD5 MessageDigest. " +
                "Jive will be unable to function normally.");
                nsae.printStackTrace();
            }
        }
        // Now, compute hash.
        byte[] buff;
        try {
			buff = data.getBytes("GBK");
		} catch (UnsupportedEncodingException e) {
			buff = data.getBytes();
		}
        
        digest.update(buff);
        return encodeHex(digest.digest());
    }

    /**
     * Turns an array of bytes into a String representing each byte as an
     * unsigned hex number.
     * <p>
     * Method by Santeri Paavolainen, Helsinki Finland 1996<br>
     * (c) Santeri Paavolainen, Helsinki Finland 1996<br>
     * Distributed under LGPL.
     *
     * @param bytes an array of bytes to convert to a hex-string
     * @return generated hex string
     */
    public static final String encodeHex(byte[] bytes) {
        StringBuffer buf = new StringBuffer(bytes.length * 2);
        int i;

        for (i = 0; i < bytes.length; i++) {
            if (((int) bytes[i] & 0xff) < 0x10) {
                buf.append("0");
            }
            buf.append(Long.toString((int) bytes[i] & 0xff, 16));
        }
        return buf.toString();
    }

    /**
     * Turns a hex encoded string into a byte array. It is specifically meant
     * to "reverse" the toHex(byte[]) method.
     *
     * @param hex a hex encoded String to transform into a byte array.
     * @return a byte array representing the hex String[
     */
    public static final byte[] decodeHex(String hex) {
        char [] chars = hex.toCharArray();
        byte[] bytes = new byte[chars.length/2];
        int byteCount = 0;
        for (int i=0; i<chars.length; i+=2) {
            byte newByte = 0x00;
            newByte |= hexCharToByte(chars[i]);
            newByte <<= 4;
            newByte |= hexCharToByte(chars[i+1]);
            bytes[byteCount] = newByte;
            byteCount++;
        }
        return bytes;
    }

    /**
     * Returns the the byte value of a hexadecmical char (0-f). It's assumed
     * that the hexidecimal chars are lower case as appropriate.
     *
     * @param ch a hexedicmal character (0-f)
     * @return the byte value of the character (0x00-0x0F)
     */
    private static final byte hexCharToByte(char ch) {
        switch(ch) {
            case '0': return 0x00;
            case '1': return 0x01;
            case '2': return 0x02;
            case '3': return 0x03;
            case '4': return 0x04;
            case '5': return 0x05;
            case '6': return 0x06;
            case '7': return 0x07;
            case '8': return 0x08;
            case '9': return 0x09;
            case 'a': return 0x0A;
            case 'b': return 0x0B;
            case 'c': return 0x0C;
            case 'd': return 0x0D;
            case 'e': return 0x0E;
            case 'f': return 0x0F;
        }
        return 0x00;
    }

    //*********************************************************************
    //* Base64 - a simple base64 encoder and decoder.
    //*
    //*     Copyright (c) 1999, Bob Withers - bwit@pobox.com
    //*
    //* This code may be freely used for any purpose, either personal
    //* or commercial, provided the authors copyright notice remains
    //* intact.
    //*********************************************************************

    /**
     * Encodes a String as a base64 String.
     *
     * @param data a String to encode.
     * @return a base64 encoded String.
     * @throws UnsupportedEncodingException 
     */
    public static String encodeBase64(String data) throws UnsupportedEncodingException {
        return encodeBase64(data.getBytes("GBK"));
    }

    /**
     * Encodes a byte array into a base64 String.
     *
     * @param data a byte array to encode.
     * @return a base64 encode String.
     */
    public static String encodeBase64(byte[] data) {
        int c;
        int len = data.length;
        StringBuffer ret = new StringBuffer(((len / 3) + 1) * 4);
        for (int i = 0; i < len; ++i) {
            c = (data[i] >> 2) & 0x3f;
            ret.append(cvt.charAt(c));
            c = (data[i] << 4) & 0x3f;
            if (++i < len)
                c |= (data[i] >> 4) & 0x0f;

            ret.append(cvt.charAt(c));
            if (i < len) {
                c = (data[i] << 2) & 0x3f;
                if (++i < len)
                    c |= (data[i] >> 6) & 0x03;

                ret.append(cvt.charAt(c));
            }
            else {
                ++i;
                ret.append((char) fillchar);
            }

            if (i < len) {
                c = data[i] & 0x3f;
                ret.append(cvt.charAt(c));
            }
            else {
                ret.append((char) fillchar);
            }
        }
        return ret.toString();
    }

    /**
     * Decodes a base64 String.
     *
     * @param data a base64 encoded String to decode.
     * @return the decoded String.
     * @throws UnsupportedEncodingException 
     */
    public static String decodeBase64(String data) throws UnsupportedEncodingException {
        return decodeBase64(data.getBytes("GBK"));
    }

    /**
     * Decodes a base64 aray of bytes.
     *
     * @param data a base64 encode byte array to decode.
     * @return the decoded String.
     */
    public static String decodeBase64(byte[] data) {
        int c, c1;
        int len = data.length;
        StringBuffer ret = new StringBuffer((len * 3) / 4);
        for (int i = 0; i < len; ++i) {
            c = cvt.indexOf(data[i]);
            ++i;
            c1 = cvt.indexOf(data[i]);
            c = ((c << 2) | ((c1 >> 4) & 0x3));
            ret.append((char) c);
            if (++i < len) {
                c = data[i];
                if (fillchar == c)
                    break;

                c = cvt.indexOf((char) c);
                c1 = ((c1 << 4) & 0xf0) | ((c >> 2) & 0xf);
                ret.append((char) c1);
            }

            if (++i < len) {
                c1 = data[i];
                if (fillchar == c1)
                    break;

                c1 = cvt.indexOf((char) c1);
                c = ((c << 6) & 0xc0) | c1;
                ret.append((char) c);
            }
        }
        return ret.toString();
    }

    private static final int fillchar = '=';
    private static final String cvt = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                                    + "abcdefghijklmnopqrstuvwxyz"
                                    + "0123456789+/";

    /**
     * Converts a line of text into an array of lower case words using a
     * BreakIterator.wordInstance(). <p>
     *
     * This method is under the Jive Open Source Software License and was
     * written by Mark Imbriaco.
     *
     * @param text a String of text to convert into an array of words
     * @return text broken up into an array of words.
     */
    public static final String [] toLowerCaseWordArray(String text) {
        if (text == null || text.length() == 0) {
                return new String[0];
        }

        ArrayList<String> wordList = new ArrayList<String>();
        BreakIterator boundary = BreakIterator.getWordInstance();
        boundary.setText(text);
        int start = 0;

        for (int end = boundary.next(); end != BreakIterator.DONE;
                start = end, end = boundary.next())
        {
            String tmp = text.substring(start,end).trim();
            // Remove characters that are not needed.
            tmp = replace(tmp, "+", "");
            tmp = replace(tmp, "/", "");
            tmp = replace(tmp, "\\", "");
            tmp = replace(tmp, "#", "");
            tmp = replace(tmp, "*", "");
            tmp = replace(tmp, ")", "");
            tmp = replace(tmp, "(", "");
            tmp = replace(tmp, "&", "");
            if (tmp.length() > 0) {
                wordList.add(tmp);
            }
        }
        return (String[]) wordList.toArray(new String[wordList.size()]);
    }

    /**
     * Pseudo-random number generator object for use with randomString().
     * The Random class is not considered to be cryptographically secure, so
     * only use these random Strings for low to medium security applications.
     */
    private static Random randGen = new Random();

    /**
     * Array of numbers and letters of mixed case. Numbers appear in the list
     * twice so that there is a more equal chance that a number will be picked.
     * We can use the array to get a random number or letter by picking a random
     * array index.
     */
    private static char[] numbersAndLetters = ("0123456789abcdefghijklmnopqrstuvwxyz" +
                    "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ").toCharArray();

    /**
     * Returns a random String of numbers and letters (lower and upper case)
     * of the specified length. The method uses the Random class that is
     * built-in to Java which is suitable for low to medium grade security uses.
     * This means that the output is only pseudo random, i.e., each number is
     * mathematically generated so is not truly random.<p>
     *
     * The specified length must be at least one. If not, the method will return
     * null.
     *
     * @param length the desired length of the random String to return.
     * @return a random String of numbers and letters of the specified length.
     */
    public static final String randomString(int length) {
        if (length < 1) {
            return null;
        }
        // Create a char buffer to put random letters and numbers in.
        char [] randBuffer = new char[length];
        for (int i=0; i<randBuffer.length; i++) {
            randBuffer[i] = numbersAndLetters[randGen.nextInt(71)];
        }
        return new String(randBuffer);
    }

   /**
    * Intelligently chops a String at a word boundary (whitespace) that occurs
    * at the specified index in the argument or before. However, if there is a
    * newline character before <code>length</code>, the String will be chopped
    * there. If no newline or whitespace is found in <code>string</code> up to
    * the index <code>length</code>, the String will chopped at <code>length</code>.
    * <p>
    * For example, chopAtWord("This is a nice String", 10) will return
    * "This is a" which is the first word boundary less than or equal to 10
    * characters into the original String.
    *
    * @param string the String to chop.
    * @param length the index in <code>string</code> to start looking for a
    *       whitespace boundary at.
    * @return a substring of <code>string</code> whose length is less than or
    *       equal to <code>length</code>, and that is chopped at whitespace.
    */
    public static final String chopAtWord(String string, int length, boolean noWrap) {
        if (string == null) {
            return string;
        }

        char [] charArray = string.toCharArray();
        int sLength = string.length();
        if (length < sLength) {
            sLength = length;
        }

	if(noWrap) {

        // First check if there is a newline character before length; if so,
        // chop word there.
        for (int i=0; i<sLength-1; i++) {
            // Windows
            if (charArray[i] == '\r' && charArray[i+1] == '\n') {
                return string.substring(0, i+1);
            }
            // Unix
            else if (charArray[i] == '\n') {
                return string.substring(0, i);
            }
        }
        // Also check boundary case of Unix newline
        if (charArray[sLength-1] == '\n') {
            return string.substring(0, sLength-1);
        }
	}

        // Done checking for newline, now see if the total string is less than
        // the specified chop point.
        if (string.length() < length) {
            return string;
        }
        // No newline, so chop at the first whitespace.
        for (int i = length-1; i > 0; i--) {
            if (charArray[i] == ' ') {
                return string.substring(0, i).trim();
            }
        }

        // Did not find word boundary so return original String chopped at
        // specified length.
        return string.substring(0, length);
    }

    // Create a regular expression engine that is used by the highlightWords
    // method below.
    private static Perl5Util perl5Util = new Perl5Util();

    /**
     * Highlights words in a string. Words matching ignores case. The actual
     * higlighting method is specified with the start and end higlight tags.
     * Those might be beginning and ending HTML bold tags, or anything else.<p>
     *
     * This method is under the Jive Open Source Software License and was
     * written by Mark Imbriaco.
     *
     * @param string the String to highlight words in.
     * @param words an array of words that should be highlighted in the string.
     * @param startHighlight the tag that should be inserted to start highlighting.
     * @param endHighlight the tag that should be inserted to end highlighting.
     * @return a new String with the specified words highlighted.
     */
    public static final String highlightWords(String string, String[] words,
        String startHighlight, String endHighlight)
    {
        if (string == null || words == null ||
                startHighlight == null || endHighlight == null)
        {
            return null;
        }

        StringBuffer regexp = new StringBuffer();

        // Iterate through each word and generate a word list for the regexp.
        for (int x=0; x<words.length; x++)
        {
            // Excape "|" and "/"  to keep us out of trouble in our regexp.
            words[x] = perl5Util.substitute("s#([\\|\\/\\.])#\\\\$1#g", words[x]);
            if (regexp.length() > 0)
            {
                regexp.append("|");
            }
            regexp.append(words[x]);
        }

        // Escape the regular expression delimiter ("/").
        startHighlight = perl5Util.substitute("s#\\/#\\\\/#g", startHighlight);
        endHighlight = perl5Util.substitute("s#\\/#\\\\/#g", endHighlight);

        // Build the regular expression. insert() the first part.
        regexp.insert(0, "s/\\b(");
        // The word list is here already, so just append the rest.
        regexp.append(")\\b/");
        regexp.append(startHighlight);
        regexp.append("$1");
        regexp.append(endHighlight);
        regexp.append("/igm");

        // Do the actual substitution via a simple regular expression.
        return perl5Util.substitute(regexp.toString(), string);
    }

    /**
     * Escapes all necessary characters in the String so that it can be used
     * in an XML doc.
     *
     * @param string the string to escape.
     * @return the string with appropriate characters escaped.
     */
    public static final String escapeForXML(String string) {
        if (string == null) {
            return null;
        }
        char ch;
        int i=0;
        int last=0;
        char[] input = string.toCharArray();
        int len = input.length;
        StringBuffer out = new StringBuffer((int)(len*1.3));
        for (; i < len; i++) {
            ch = input[i];
            if (ch > '>') {
                continue;
            } else if (ch == '>') {
                if (i > last) {
                    out.append(input, last, i - last);
                }
                last = i + 1;
                out.append(GT_ENCODE);
        	} else if (ch == '<') {
                if (i > last) {
                    out.append(input, last, i - last);
                }
                last = i + 1;
                out.append(LT_ENCODE);
            } else if (ch == '&') {
                if (i > last) {
                    out.append(input, last, i - last);
                }
                last = i + 1;
                out.append(AMP_ENCODE);
            } else if (ch == '"') {
                if (i > last) {
                    out.append(input, last, i - last);
                }
                last = i + 1;
                out.append(QUOTE_ENCODE);
            }
        }
        if (last == 0) {
            return string;
        }
        if (i > last) {
            out.append(input, last, i - last);
        }
        return out.toString();
    }

    /**
     * Unescapes the String by converting XML escape sequences back into normal
     * characters.
     *
     * @param string the string to unescape.
     * @return the string with appropriate characters unescaped.
     */
    public static final String unescapeFromXML(String string) {
        string = replace(string, "&lt;", "<");
        string = replace(string, "&gt;", ">");
        string = replace(string, "&quot;", "\"");
        return replace(string, "&amp;", "&");
    }

    private static final char[] zeroArray = "0000000000000000".toCharArray();

    /**
     * Pads the supplied String with 0's to the specified length and returns
     * the result as a new String. For example, if the initial String is
     * "9999" and the desired length is 8, the result would be "00009999".
     * This type of padding is useful for creating numerical values that need
     * to be stored and sorted as character data. Note: the current
     * implementation of this method allows for a maximum <tt>length</tt> of
     * 16.
     *
     * @param string the original String to pad.
     * @param length the desired length of the new padded String.
     * @return a new String padded with the required number of 0's.
     */
     public static final String zeroPadString(String string, int length) {
        if (string == null || string.length() > length) {
            return string;
        }
        StringBuffer buf = new StringBuffer(length);
        buf.append(zeroArray, 0, length-string.length()).append(string);
        return buf.toString();
     }

     public static final String unpadZeroString(String string) {
        if (string == null ) {
            return string;
        }
        StringBuffer buf = new StringBuffer(string);
	while(buf.charAt(0) == zeroArray[0]) {
	    buf = buf.deleteCharAt(0);
	}
        //buf.substring(,string.length()-1);
        return buf.toString();
     }

     public static String appendDecimal(String s) {
	if(s == null) {
	    return s;
	}
	if(s.lastIndexOf(".") < 0) {
	    return s + ".00";
	} else {	   
	    String postfix = s.substring(s.lastIndexOf("."),s.length()-1);
	    if(postfix.length() > 2) {
		 return s.substring(0, (s.lastIndexOf(".")+3));
	    }else if(postfix.length() < 2) {
		return s + "0";
	    }
	    return s;
	}
    }

     /**
      * Formats a Date as a fifteen character long String made up of the Date's
      * padded millisecond value.
      *
      * @return a Date encoded as a String.
      */
     public static final String dateToMillis(Date date) {
        return zeroPadString(Long.toString(date.getTime()), 15);
     }

     public static String iso2gb(String str) {
       try {
         return new String(str.getBytes("iso-8859-1"), "gb2312");
       }
       catch (Exception e) {
         return str;
       }
     }

     public static String iso2gbk(String str) {
       try {
         return new String(str.getBytes("iso-8859-1"), "GBK");
       }
       catch (Exception e) {
         return str;
       }
     }

     public static String gb2gbk(String str) {
       try {
         return new String(str.getBytes("GB2312"), "GBK");
       }
       catch (Exception e) {
         return str;
       }
     }

     public static String gb2iso(String str) {
       try {
         return new String(str.getBytes("gb2312"), "iso-8859-1");
       }
       catch (Exception e) {
         return str;
       }
     }

     public static String toLowerCase(String str) {
       char c;
       int toLowerCase_flag = 1;
       int i = 0;
       while (i < str.length()) {
         c = str.charAt(i);
         if (c > 0x7f) { //Str字串中若出现中文的双字节ascii编码值>0x7f（十六进制）
           toLowerCase_flag = 0; //则此Str字串因出现中文而不符合toLowerCase()的条件
           break;
         }
         i++;
       }
       if (toLowerCase_flag == 1) {
         str = str.toLowerCase();
       }
       return str;
     }

     public static String toUpperCase(String str) {
       char c;
       int toUpperCase_flag = 1;
       int i = 0;
       while (i < str.length()) {
         c = str.charAt(i);
         if (c > 0x7f) { //Str字串中若出现中文的双字节ascii编码值>0x7f（十六进制）
           toUpperCase_flag = 0; //则此Str字串因出现中文而不符合toUpperCase()的条件
           break;
         }
         i++;
       }
       if (toUpperCase_flag == 1) {
         str = str.toUpperCase();
       }
       return str;
     }

     public static boolean isAlphaOrNumber(String str) {
       char c;
       boolean checkLetterandNumber_flag = false;
       int i = 0;
       while (i < str.length()) {
         c = str.charAt(i);
         if (c > 0x40 && c < 0x5b
             || c > 0x60 && c < 0x7b
             || c > 0x2f && c < 0x3a) { //Str字串中的当前字符若为字母（A-Z,a-z）或数字（0-9）的ascii编码值（十六进制）
           checkLetterandNumber_flag = true; //则继续检查此Str字串的下一个字符
         }
         else {
           checkLetterandNumber_flag = false; //则此Str字串因出现非字母（A-Z,a-z）或数字（0-9）的字符而不符合checkLetterandNumber()的条件
           break;
         }
         i++;
       }
       return checkLetterandNumber_flag;
     }

     public static int str2int(String str) {
       return Integer.parseInt(str);
     }

     public static String int2str(int someint) {
       return new Integer(someint).toString();
     }

     public static String replace(String oristr, String[] targetstr,
                                  String[] descstr) throws Exception {
       String result = null;
       try {
         if (targetstr.length == descstr.length) {
           for (int i = 0; i < targetstr.length; i++) {
             oristr = oristr.substring(0, oristr.indexOf(targetstr[i])) +
                 descstr[i] + oristr.substring(targetstr[i].length() + 1);
           }
           result = oristr;
         }
       }
       catch (Exception e) {
         throw e;
       }
       return result;
     }

     public static String exmlength(String s1, int num) {
       String tmpstr = "";
       int total = 0;
       try {
         byte str[] = s1.getBytes("GBK");
         char ch[] = new char[str.length];

         if (str.length > num) {
           for (int i = 0; i < num; i++) {
             if (str[i] < 0) {
               if (i < (str.length - 1)) {
                 tmpstr = tmpstr + s1.substring( (i - total), (i - total + 1));
                 total++;
                 i++;
               }
             }
             else {
               ch[i] = (char) str[i];
               tmpstr = tmpstr + ch[i];
             }
           }
         }
         else
           tmpstr = s1;
       }
       catch (Exception e) {}
       return tmpstr;
     }

     public static String array2str(String[] arrayname, int length) {
       StringBuffer strbuf = new StringBuffer();
       for (int i = 0; i < length; i++) {
         strbuf.append("0");
       }
       for (int i = 0; i < arrayname.length; i++) {
         strbuf.replace(Integer.parseInt(arrayname[i]) - 1,
                        Integer.parseInt(arrayname[i]), "1");
       }
       return strbuf.toString();
     }

     public static String CreateDefaultStr(int strlength) {
       String str = "";
       for (int i = 0; i < strlength; i++) {
         str += "0";
       }
       return str;
     }

     public static String[] str2array(String originstr, String str, int limit) {
       StringTokenizer st = new StringTokenizer(originstr, str);
       int index = 0;
       String[] array = new String[limit];
       while (index < limit && st.hasMoreTokens()) {
         array[index] = st.nextToken();
         index++;
       }
       return array;
     }

     public static String null2String(String str) {
       String str2 = new String();
       if (str == null) {
         str2 = "";
       }
       else {
         str2 = str;
       }
       return str2;
     }

     public static boolean IsDigit(char cCheck) {
       return ( ('0' <= cCheck) && (cCheck <= '9'));
     }

     public static boolean IsAlpha(char cCheck) {
       return ( ( ('a' <= cCheck) && (cCheck <= 'z')) ||
               ( ('A' <= cCheck) && (cCheck <= 'Z')));
     }

     public static String Nick2Standard(String nick) { //GB
       nick = nick.toLowerCase();

       if (nick.length() > 1) {
         String nick_1 = nick.substring(0, 1);
         nick = nick_1.toUpperCase() + nick.substring(1, nick.length());
       }
       else {
         nick = nick.toUpperCase();
       }

       return nick;
     }

 	/**
 	 * 按照指定的长度，得到中英文组成的字符串的子串。
 	 * 
 	 * @param string 截取来源字符串
 	 * @param length 显示长度
 	 * @return
 	 */
 	public static final String chop(String string, int length ){
         if (string == null) {
             return string;
         }

          char[] charArray = string.toCharArray();
         
          int tmpLength = 0;
         for( int i =0; i< charArray.length; i++){
//         	if(noWrap) {
//
//                 // First check if there is a newline character before length; if so,
//                 // chop word there.
//                     if (charArray[i] == '\r' && charArray[i+1] == '\n') {
//                         return string.substring(0, i+1);
//                     }
//                     // Unix
//                     else if (charArray[i] == '\n') {
//                         return string.substring(0, i);
//                     }
//         	}
         	
         	// 检查当前的字符的可能显示宽度
         	int localeLength = 1; // 默认的Ascii码字符宽度为1.
         	if( charArray[i] > 0xff) {
         		localeLength = 2;
         	}
         	
         	// 检查当前字符显示后是否会越界
         	int availableLength = tmpLength + localeLength;
         	if( availableLength > length ){
         		// 此种情况为：插入当前字符前小于指定长度，插入后则大于指定长度，
         		// 必定是插入前比指定长度小一个字符，且当前字符一定是汉字，
         		return string.substring( 0, i)+'.';
         	}else if ( availableLength == length ) {
         		// 插入字符后，长度等于指定长度。
         		if( i== charArray.length - 1){
         			// 原字符串恰好满足要求
         			return string;
         		} else {
         			if( localeLength == 1 ){
         				return string.substring(0, i )+ '.';
         			} else {
         				return string.substring(0, i ) + "..";
         			}
         		}
         	} else {
         		
         		tmpLength = availableLength;
         	}

         }
//         if( noWrap ){
//             // Also check boundary case of Unix newline
//             if (charArray[ charArray.length - 1 ] == '\n') {
//                 return string.substring(0, charArray.length - 1 );
//             }
//
//         }
         // Did not find word boundary so return original String chopped at
         // specified length.
         return string;
 	}
 	public static int indexOf( CharSequence sb, char[] buff){
 		return indexOf( sb, buff, 0 );
 	}

 	public static int indexOf( CharSequence sb, char[] buff, int fromIndex){
 outer:	for( int i=fromIndex ;i<=sb.length()-buff.length;i ++){
 			for( int j=0; j< buff.length; j++ ){
 				if( sb.charAt( i+j ) != buff[j] ){
 					continue outer;
 				}
 			}
 			return i;
 		}
 		return -1;
 	}
 	
 	public static int indexOfEscapeQuote( CharSequence sb, char[] buff){
 		return indexOfWithEscapeChar( sb, buff, 0, '\"');
 	}
 	
 	public static int indexOfEscapeQuote( CharSequence sb, char[] buff, int fromIndex){
 		return indexOfWithEscapeChar( sb, buff, fromIndex, '\"');
 	}

 	public static int indexOfWithEscapeChar( CharSequence sb, char[] buff, int fromIndex, char esc){
 		int maxLen = sb.length() - buff.length;
 outer:	for( int i=fromIndex ;i<=maxLen;i ++){
 			char c = sb.charAt(i);
 			if( c=='\"'){
 				for( i=i+1; i<=maxLen; i++ ){
 					if( sb.charAt(i)=='\"' ){
 						break;
 					}
 				}
 				continue;
 			}
 				for( int j=0; j< buff.length; j++ ){
 					if( sb.charAt( i+j ) != buff[j] ){
 						continue outer;
 					}
 				}
 				return i;
 		}
 		return -1;
 	}
 	
 	/**
	 * 在所给字符串中(oStr)中删除某些字符(delChars)
	 * 每个字符不论出现多少次，全都删除
	 * @param oStr
	 * @param delChars
	 * @return
	 */
	public static final String deleteChars(String oStr,char []delChars) {
		if(oStr==null){
			return null;
		}
		if(delChars==null){
			return oStr;
		}
		
		//不含有待删字符时，直接返回
		boolean containDelChars = false;
		for(int i=0;i<delChars.length;i++){
			char c=delChars[i];
			//oStr中包含此字符
			if(oStr.indexOf(c)!=-1){
				containDelChars=true;
				break;
			}
		}
		if(!containDelChars){
			return oStr;
		}
		
		int len = oStr.length();
		char buf[] = new char[len];
		int j=0;
		for(int i=0;i<len;i++){
			char c = oStr.charAt(i);
			//判断是否需要删除
			boolean isDelChar=false;
			for(int k=0;k<delChars.length;k++){
				char delChar=delChars[k];
				if(c==delChar){
					isDelChar=true;
					break;
				}
			}
			
			if(isDelChar){
				continue;//无用字符过滤掉
			}else{
				buf[j]=c;
				j++;
			}
		}
		//返回有效字符
        return new String(buf, 0, j);
    }
	
	/**
	 * 替换oStr中的换行等字符
	 * @param oStr
	 * @return 替换后的字符串
	 */
	public static final String removeWrapChars(String oStr) {
		if(oStr==null){
			return null;
		}
		char[] wrapChars = {'\t','\n','\r'};
		oStr=deleteChars(oStr,wrapChars);

		return oStr;
    }
	/**
	 * 检验target是否全部由d中的字符序列组成
	 * @param target 被检查的串
	 * @param d 组成序列
	 * @return true - 表示全部由d中的字符组成；
	 */
	public static boolean spawn(CharSequence target, CharSequence d){
		if(  target == null || d == null ){
			return false;
		}
		int i = 0;
		for( ; i<target.length() ; i++){
			int j = 0;
			for(;j<d.length();j++){
				if(d.charAt(j) == target.charAt(i) )
					break;
			}
			// 找到一个不存在的字符，则返回false，退出
			if( j == d.length() ){
				return false;
			}
		}
		return true;
	}
	/**
	 * 检验target是否全部由d中的字符序列组成
	 * @param target 被检查的串
	 * @param d 组成序列
	 * @return true - 表示全部由d中的字符组成；
	 */
	public static boolean spawn(char target, CharSequence d){
		if(  d == null ){
			return false;
		}
		do{
			int j = 0;
			for(;j<d.length();j++){
				if(d.charAt(j) == target )
					break;
			}
			// 找到一个不存在的字符，则返回false，退出
			if( j == d.length() ){
				return false;
			}
		}while(false);
		return true;
	}

}
