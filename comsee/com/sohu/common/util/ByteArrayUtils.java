package com.sohu.common.util;

/**
 * 本类面向byte[]型对象, 提供与String类里定义的indexOf, startsWith等常用方法相类似的方法.
 * 
 * @author liumingzhu
 * @version 0.9
 *
 */
public class ByteArrayUtils {
	/**
	 * 类似于String.indexOf()的方法. 在byte数组中查找seq中包含的ascii码的位置.
	 * 
	 * @param data  可能包含被检索字符串的数组.
	 * @param start 检索起始位置
	 * @param end   检索结束位置
	 * @param seq   被检索的字符串
	 * @return  被检索字符串在byte数组中的起始位置.
	 * 
	 * 如果data或seq为null, 会抛出NullPointerException
	 */
	public static int indexOf( byte[] data, int start, int end, CharSequence seq ){
		if( end - start < seq.length()
				|| seq.length() <= 0 ){
			return -2;
		}
		int seq_len = seq.length();
		int newend = end - seq_len + 1;
		
		int i = start;
		for( ;
				( i < newend  );
			i++ ){
			int k = 0;
			for( int j = i ;
				( k < seq_len
						&& data[j] == (byte)seq.charAt(k) );
				j++, k++){
			}
			if( k == seq_len ){
				break;
			}
		}
			;
		if( i < end ){
			return i;
		} else {
			return -1;
		}
		
	}
	/**
	 * 类似于String.indexOf()的方法. 在byte数组中查找seq中包含的byte数组的位置.
	 * 
	 * @param data  可能包含被检索字符串的数组.
	 * @param start 检索起始位置
	 * @param end   检索结束位置
	 * @param seq   被检索的byte流
	 * @return  被检索字符串在byte数组中的起始位置.
	 * 
	 * 如果data或seq为null, 会抛出NullPointerException
	 */
	public static int indexOf( byte[] data, int start, int end, byte[] seq ){
		if( end - start < seq.length
				|| seq.length <= 0 ){
			return -2;
		}
		int seq_len = seq.length;
		int newend = end - seq_len + 1;
		
		int i = start;
		for( ;
				( i < newend  );
			i++ ){
			int k = 0;
			for( int j = i ;
				( k < seq_len
						&& data[j] == seq[k] );
				j++, k++){
			}
			if( k == seq_len ){
				break;
			}
		}
			;
		if( i < end ){
			return i;
		} else {
			return -1;
		}
		
	}

	/**
	 * 类似于String.startsWith()方法, 检查byte数组是否以某字符串开头.
	 * 
	 * @param data  可能以某字符串开头的byte数组 
	 * @param start 起始位置
	 * @param end   结束位置
	 * @param seq   被检索的字符串
	 * @return  1 - 表示为真
	 */
	public static int startsWith( byte[] data, int start, int end, CharSequence seq ){
		if( data == null ) {
			return -1;
		}
		if( seq == null ){
			return -2;
		}
		if( start < 0 ){
			return -3;
		}
		if( end > data.length ){
			return -4;
		}
		if( end - start < seq.length() ){
			return -5;
		}
		end = start + seq.length();
		
		int i = start;
		for( int j=0;
				( i < end
					 && data[i] == (byte)seq.charAt(j) );
			i++, j++ )
			;
		if( i == end ){
			return 1;
		} else {
			return 0;
		}
	}
	/**
	 * 见#@see startsWith( byte[], int, int, CharSequence )
	 * @param data  可能以某字符串开头的byte数组 
	 * @param start 起始位置
	 * @param end   结束位置
	 * @param d     被检索的byte串
	 * @return  1 - 表示为真
	 */
	public static int startsWith( byte[] data, int start, int end, byte[] d ){
		if( data == null ) {
			return -1;
		}
		if( d == null ){
			return -2;
		}
		if( start < 0 ){
			return -3;
		}
		if( end > data.length ){
			return -4;
		}
		if( end - start < d.length ){
			return -5;
		}
		
		int i= start;
		for( int j=0;
				( i < end
					 && data[i] == d[j] );
			i++, j++ )
			;
		if( i == end ){
			return 1;
		} else {
			return 0;
		}
	}
	private static char[] hexchars = { '0', '1', '2', '3', '4', '5', '6', '7',
		'8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	/**
	 * 将一串byte数组转化成可读的HEX ASCII码串.
	 * 
	 * @param bytes 要打印的byte串
	 * @param dir   打印的方向. 1 - 按byte数组顺序; 0 - 逆序
	 * @return 输出字串
	 */
	public static String BytesToHexString(byte[] bytes, int dir) {
		char[] buf = new char[bytes.length * 2];
		for (int i = 0; i < bytes.length; ++i) {
			int v = (bytes[i] > -1) ? bytes[i] : (bytes[i] + 0x100);
			if( dir > 0 ){
				buf[i * 2] = hexchars[v / 0x10];
				buf[i * 2 + 1] = hexchars[v % 0x10];
			} else {
				buf[bytes.length * 2 - i * 2 - 2] = hexchars[v / 0x10];
				buf[bytes.length * 2 - i * 2 - 1] = hexchars[v % 0x10];
			}
		}
		return new String(buf);
	}

	/**
	 * 模拟C语言中将比特数组转义成其他整数类型数值的操作。littleEndian
	 * @param ar
	 * @param start
	 * @return
	 */
    public static long byte2long(byte[] ar, int start){
        long ret = 0;
        for( int i=0;i<8;i++){
                ret |= ((long)ar[start+i] & 0xffl)<< (i<<3);
        }
        return ret;
    }
    
	public static final int htonl(int t){
		int t0 = t & 0xFF;
		int t1 = (t >> 8) & 0xFF;
		int t2 = (t >> 16) & 0xFF;
		int t3 = (t >> 24) & 0xFF;
		
		return (t0 << 24) + (t1 << 16) + (t2 << 8) + t3; 
	}
	public static final short htons(short t){
		int t0 = t & 0xFF;
		int t1 = (t >> 8) & 0xFF;
		
		return (short)((t0 << 8) + t1); 
	}

}
