/***
 * 将>2G的数据load到内存的工具，类比ByteBuffer类，提供了put/get方法。
 * liumingzhu@sohu-rd.com
 */

package com.sohu.common.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channel;

public class LongByteBuffer {

	private static final int DEFAULT_MAX_BUFF_BITMASK_LEN = 29;
//	private static final int DEFAULT_MAX_BUFF_VALUE_INNER = 0x01 << DEFAULT_MAX_BUFF_BITMASK_LEN;
//	private static final int DEFAULT_MAX_BUFF_BITMASK = (DEFAULT_MAX_BUFF_VALUE_INNER - 1);

	private final int MAX_BUFF_BITMASK_LEN;
	private final int MAX_BUFF_VALUE_INNER;
	private final int MAX_BUFF_BITMASK;

	public static final long MAX_VALUE = 0x01l << 34l;

	private static final int MAX_RETRIES_BEFOR_OOME_FAIL = 4; 
	private static final long SLEEP_TIME_MILLIS_WHEN_OOME_FAIL = 1000l; 
	
	ByteBuffer[] innerBuffs = null; // 初始化后innerBuffs必定不是null
	private boolean explictlyCallGc = false; // 如果出现OOME，是否主动调用System.gc();
	private final boolean forceAlign; // 申请内存的时候是否强制按块申请。这样会有一些内存浪费。
	long length = -1;

	private ByteOrder order = ByteOrder.nativeOrder();

	public LongByteBuffer(String filename) throws IllegalArgumentException,
	IOException {
		this(filename, false);
	}
	public LongByteBuffer(String filename, boolean explictlyCallGc) throws IllegalArgumentException,
			IOException {
		this(filename, false, DEFAULT_MAX_BUFF_BITMASK_LEN, false);
	}
		
	public LongByteBuffer(String filename, boolean explictlyCallGc, int maxBuffBitmaskLen, boolean forceAlign) throws IllegalArgumentException,
	IOException {
		MAX_BUFF_BITMASK_LEN = maxBuffBitmaskLen;
		MAX_BUFF_VALUE_INNER = 1 << MAX_BUFF_BITMASK_LEN;
		MAX_BUFF_BITMASK = MAX_BUFF_VALUE_INNER -1;
		
		this.forceAlign = forceAlign;

		this.explictlyCallGc = explictlyCallGc;
		File file = new File(filename);
		long size = file.length();
		resize(size);
		boolean isSuccess = false;
		RandomAccessFile af = null;
		try {
			af = new RandomAccessFile(file, "r");
			int bad = 0;
			long remain = size;
			// 由于linux内核限制一次读操作不能超过2g，因此改成每次只读一块的形式.
			int currentBufferIdx = 0;
			while (true) {
				ByteBuffer bb = innerBuffs[currentBufferIdx];
				long a = af.getChannel().read(bb);
				if (a < 0)
					break;
				remain -= a;
				if (remain == 0) {
					isSuccess = true;
					break;
				}
				if( bb.remaining() == 0 ){
					currentBufferIdx ++;
				}
				if (a < 10)
					bad++;
				if (bad > 20)
					throw new IllegalArgumentException("Error reading File: "
							+ filename);
			}
		} finally {
			if (!isSuccess) { // 清理工作
				innerBuffs = null;
			}
			if (af != null) {
				try {
					af.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				Channel ch = af.getChannel();
				if (ch != null) {
					try {
						ch.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public LongByteBuffer(long size) throws IllegalArgumentException {
		this(size, false, false);
	}
	public LongByteBuffer(long size, boolean explictlyCallGc, boolean forceAlign) throws IllegalArgumentException {
		this( size, explictlyCallGc, DEFAULT_MAX_BUFF_BITMASK_LEN, forceAlign);
	}
	public LongByteBuffer(long size, boolean explictlyCallGc, int maxBuffBitmaskLen, boolean forceAlign) throws IllegalArgumentException {
		MAX_BUFF_BITMASK_LEN = maxBuffBitmaskLen;
		MAX_BUFF_VALUE_INNER = 1 << MAX_BUFF_BITMASK_LEN;
		MAX_BUFF_BITMASK = MAX_BUFF_VALUE_INNER -1;
		this.forceAlign = forceAlign;
		
		this.explictlyCallGc = explictlyCallGc;
		resize(size);
	}

	public final long size() {
		return length;
	}
	
	public void resize(long newSize){
		initBuffers(newSize, forceAlign);
	}

	private final void initBuffers(long size, boolean forceAlign) throws IllegalArgumentException {
		if (size < 0 || size >= MAX_VALUE)
			throw new IllegalArgumentException("Length Too large or Too small:"
					+ size);
		int num = (int) ((size - 1) / MAX_BUFF_VALUE_INNER) + 1;
		ByteBuffer[] data = new ByteBuffer[num];

		long tmpLen = size;
		for (int i = 0; i < num; i++) {
			int len = (forceAlign || tmpLen > MAX_BUFF_VALUE_INNER) ? MAX_BUFF_VALUE_INNER
					: (int) tmpLen;
			for(int reloadTimes = 0 ;reloadTimes < MAX_RETRIES_BEFOR_OOME_FAIL+1; reloadTimes++){
				if( innerBuffs != null && innerBuffs.length > i && innerBuffs[i].limit() >= len){
					data[i] = innerBuffs[i];
				} else {
					try {
						data[i] = ByteBuffer.allocateDirect(len);
						break;
					} catch (OutOfMemoryError e) {
						if (reloadTimes == MAX_RETRIES_BEFOR_OOME_FAIL) {
							IllegalArgumentException e1 = new IllegalArgumentException(
									"OOME for "
											+ MAX_RETRIES_BEFOR_OOME_FAIL
											+ " times when calling ByteBuffer.allocateDirect("
											+ len + "). ");
							e1.initCause(e);
							throw e1;
						} else {
							if (this.explictlyCallGc)
								System.gc();
							try {
								// 指数退避
								Thread.sleep(SLEEP_TIME_MILLIS_WHEN_OOME_FAIL
										* (1 << reloadTimes));
							} catch (InterruptedException x) {
								// Restore interrupt status
								Thread.currentThread().interrupt();
							}
						}
					}
				}
			}
			tmpLen -= len;
		}
		this.innerBuffs = data;
		this.length = size;
	}

	public final byte get(long idx) throws ArrayIndexOutOfBoundsException {
		// if( idx < 0 || idx >= size() )
		// throw new ArrayIndexOutOfBoundsException("Index: " + idx + ", real:"
		// + size());

		int x = (int) (idx >>> MAX_BUFF_BITMASK_LEN);
		// if( x < 0 || x >= innerBuffs.length )
		// throw new ArrayIndexOutOfBoundsException("Index: " + idx + ", real:"
		// + size());
		int y = (int) (idx & MAX_BUFF_BITMASK);
		// if( y >= innerBuffs[x].limit() )
		// throw new ArrayIndexOutOfBoundsException("Index: " + idx + ", real:"
		// + size());
		return innerBuffs[x].get(y);
	}

	public final void set(long idx, byte b) throws ArrayIndexOutOfBoundsException {
		// if( idx < 0 || idx >= size() )
		// throw new ArrayIndexOutOfBoundsException("Index: " + idx + ", real:"
		// + size());

		int x = (int) (idx >>> MAX_BUFF_BITMASK_LEN);
		// if( x < 0 || x >= innerBuffs.length )
		// throw new ArrayIndexOutOfBoundsException("Index: " + idx + ", real:"
		// + size());
		int y = (int) (idx & MAX_BUFF_BITMASK);
		// if( y >= innerBuffs[x].limit() )
		// throw new ArrayIndexOutOfBoundsException("Index: " + idx + ", real:"
		// + size());
		innerBuffs[x].put(y, b);
	}

	public final long indexOf(long start, long end, byte[] seq) {
		if (end - start < seq.length || seq.length <= 0) {
			return -2;
		}
		long seq_len = seq.length;
		long newend = end - seq_len + 1;

		long i = start;
		for (; i < newend; i++) {
			long k = 0;
			for (long j = i; (k < seq_len && get(j) == seq[(int) k]); j++, k++)
				;
			if (k == seq_len)
				break;
		}
		return i < newend ? i : -1;
	}

	public final long indexOf(long start, long end, byte d) {
		long newend = end;

		long i = start;
		for (; i < newend && get(i) != d; i++)
			;
		return i < end ? i : -1;
	}

	public final long startsWith(long start, long end, byte[] d) {
		if (d == null)
			return -2;
		if (start < 0)
			return -3;
		if (end > size())
			return -4;
		long newend = start + d.length;
		newend = newend < end ?  newend : end;
		
		long i = start;
		for (long j = 0; 
			(i < newend
					&& get(i) == d[(int) j]);
			i++, j++)
			;
		return (i == newend) ? 1 : 0;
	}

	/**
	 * 按照字典序，比较数据同数组d的大小。
	 * 
	 * @param start
	 * @param end
	 * @param d
	 * @return 2 - 指定数据大于给定参数，而且指定数据不以给定参数开头。 1 - 指定数据大于给定参数，而且指定数据以给定参数开头。 0 -
	 *         指定数据等于给定参数 -1 - 指定数据小于给定参数，而且给定参数以指定数据开头。 -2 -
	 *         指定数据小于给定参数，而且给定参数不以指定数据开头。 -3 - 参数出错
	 */
	public final int compareWith(long start, long end, byte[] d) {
		if (d == null)
			return -3;
		if (start < 0)
			return -3;
		if (end > size())
			return -3;

		long i = start;
		long j = 0;
		byte s = 0;
		for (; i < end && j < d.length && (s = get(i)) == d[(int) j]; i++, j++)
			;
		if (i == end) {
			return j == d.length ? 0 : -1;
		} else {
			if (j == d.length) {
				return 1;
			} else {
				return compareUbyte(s, d[(int) j]) < 0 ? -2 : 2;
			}
		}
	}
	
	/**
	 * 按照字典序，比较数据同数组d的大小。
	 * 
	 * @param start
	 * @param end
	 * @param d
	 * @return 0: same, <0: little than d; >0: large thand.   *NOTE*: different from 'compareWith'.
	 */
	public final int compareWithUnsafe(long start, long end, byte[] d) {
		long idx = start;
		int x = (int) (idx >>> MAX_BUFF_BITMASK_LEN);
		int y = (int) (idx & MAX_BUFF_BITMASK);
		
		int i = 0;
		ByteBuffer bb = this.innerBuffs[x];
		int cap = bb.limit();
		for(;idx < end;idx++, i++){
			byte s = bb.get(y++);
			if( s != d[(int)i]){
				return ((int)s & 0xFF) - ((int)d[(int)i]&0xff);
			}
			
			if( y >= cap ){
				bb = this.innerBuffs[++x];
				y = 0;
			}
		}
		return 0;
	}


	public static final int compareUbyte(byte s, byte d) {
		if (s == d)
			return 0;
		else if( s < 0 ){
			if( d < 0 ) return s-d;
			else return 1;
		} else {
			if( d >= 0 ) return s-d;
			else return -1;
		}
	}

	public final ByteOrder order() {
		return order;
	}

	public final void order(ByteOrder o) {
		if (o == null)
			order = ByteOrder.nativeOrder();
		else
			order = o;
	}

	public final long getLong(long idx) {
		long ret = 0;
		for (int i = 0; i < 8 && idx + i < size(); i++) {
			if (order == ByteOrder.BIG_ENDIAN) {
				ret = (ret << 8) | (((long) get(idx + i)) & 0xFF);
			} else {
				ret |= (((long) get(idx + i)) & 0xFF) << (i * 8);
			}
		}
		return ret;
	}

	public final int getInt(long idx) {
		int ret = 0;
		for (int i = 0; i < 4 && idx + i < size(); i++) {
			if (order == ByteOrder.BIG_ENDIAN) {
				ret = (ret << 8) | (((int) get(idx + i)) & 0xFF);
			} else {
				ret |= (((int) get(idx + i)) & 0xFF) << (i * 8);
			}
		}
		return ret;
	}

	public final short getShort(long idx) {
		short ret = 0;
		for (int i = 0; i < 2 && idx + i < size(); i++) {
			if (order == ByteOrder.BIG_ENDIAN) {
				ret = (short) ((ret << 8) | (((short) get(idx + i)) & 0xFF));
			} else {
				ret |= (((short) get(idx + i)) & 0xFF) << (i * 8);
			}
		}
		return ret;
	}

	public final long getUint(long idx) {
		long ret = 0;
		for (int i = 0; i < 4; i++) {
			if (order == ByteOrder.BIG_ENDIAN) {
				ret = (ret << 8) | (((int) get(idx + i)) & 0xFF);
			} else {
				ret |= (((long) get(idx + i)) & 0xFF) << (i << 3);
			}
		}
		return ret;
	}

	public final int dump(long idx, byte[] b) {
		return dump(idx, b, 0, b.length);
	}
	public final int dump(long idx, byte[] b, int start, int end) {
		int x = (int) (idx >>> MAX_BUFF_BITMASK_LEN);
		int y = (int) (idx & MAX_BUFF_BITMASK);
		for(;;){
			ByteBuffer bb = this.innerBuffs[x];
			int newEnd;
			int cap = bb.limit();
			synchronized(bb){
				bb.position(y);
				if( y + (end - start) <= cap ){
					bb.get(b, start, end-start);
					break;
				}
				newEnd = cap - y + start;
				bb.get(b, start, newEnd);
			}
			start = newEnd;
			x++;
			y = 0;
		}
		return 0;
	}

	public final int writeFully(DataOutput os ) throws IOException{
		byte buff[] = new byte[1024*1024];
		return writeFully(os, buff);
	}

	public final int writeFully(DataOutput os, byte[] buff ) throws IOException{
		return writeFully(0l, size(), os, buff);
	}

	public final int writeFully(long cur, long end, DataOutput os, byte[] buff ) throws IOException{
		while (cur < end){
			int len = (end - cur < buff.length) ? (int)( end - cur) : buff.length;
			dump(cur, buff, 0, len);
			os.write(buff, 0, len);
			cur += len;
		}
		return 0;
	}
	
	public final int dump_slow(long idx, byte[] b) {
		for (int i = 0; i < b.length; i++) {
			b[i] = get(idx + i);
		}
		return 0;
	}

	public final void setShort(long start, int d){
		for(int i = 0; i< 2; i++)
			set(start+i, (byte)((d>>(8*(order() == ByteOrder.BIG_ENDIAN?1-i:i)))&0xff) );
	}

	public final void setInt( long start, int d){
		for(int i = 0; i< 4; i++)
			set(start+i, (byte)((d>>(8*(order() == ByteOrder.BIG_ENDIAN?3-i:i)))&0xff) );
	}

	public final void setLong(long start, long d){
		for(int i = 0; i< 8; i++)
			set(start+i, (byte)((d>>(8*(order() == ByteOrder.BIG_ENDIAN?7-i:i)))&0xff) );
	}

	public final long readFully(DataInput is) throws IOException{
		byte[] buff = new byte[1024*1024];
		return readFully(is, buff);
	}
	public final long readFully(DataInput is, byte[] buff) throws IOException{
		long cur = 0;
		long size = size();
		return readFully(cur, size, is, buff);
	}
	public final long readFully(long cur, long end, DataInput is, byte[] buff) throws IOException{
		while( cur < end ){
			int len = (end - cur < buff.length) ? (int)( end - cur) : buff.length;
			is.readFully(buff, 0, len);
			fill(cur, buff, 0, len);
			cur += len;
		}
		return 0;
	}
	
	public final void fill(long offset, byte[] b, int start, int end){
		long idx = offset;
		int x = (int) (idx >>> MAX_BUFF_BITMASK_LEN);
		int y = (int) (idx & MAX_BUFF_BITMASK);
		for(;;){
			ByteBuffer bb = innerBuffs[x];
			int newEnd;
			int cap = bb.limit();
			synchronized(bb){
				bb.position(y);
				
				if( y + (end - start) <= cap){
					bb.put(b, start, end-start);
					break;
				}
				newEnd = cap - y + start;
				bb.put(b, start, newEnd );
			}
			x++; y = 0;
			start = newEnd;
		}
	}

	@Override public void finalize(){
		// 不要轻易调用下边的代码，很容易出现core。
		// 因为这种显示调用cleaner的方法可能会跟gc产生冲突。
//		for (ByteBuffer bb : innerBuffs) {
//			try {
//				Method buffer_cleaner = bb.getClass().getMethod("cleaner");
//				buffer_cleaner.setAccessible(true);
//				Object cleaner = buffer_cleaner.invoke(bb);
//				Method cleaner_clean = cleaner.getClass().getMethod("clean");
//				cleaner_clean.invoke(cleaner);
//			} catch (Exception e) {
//			}
//		}
	}
}
