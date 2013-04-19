package com.sohu.common.util.text;
//////////////////////////////////////////////////////////////////////
//
//		$Revision: 1.3 $
//		$Author: wangying $
//		$Date: 2006/08/06 02:58:33 $
//
//////////////////////////////////////////////////////////////////////


interface StringMatcher
{
	//匹配以字节为单位，因此字母表大小为256
	public static final int ALPHABET_SIZE = 256;

	/**
	 * 		初始化多关键词匹配器的函数
	 * @param keywords[]
	 *				关键词数组
	 * @param offset
	 *				第一个需要扫描的关键词在keywords[]中的起始位置
	 * @param keywordnum
	 * 				需要扫描的关键词个数
	 * 
	 * @return 成功返回1；否则返回-1
	 */
	public abstract int initialize(byte keywords[][], int offset, int keywordnum);

	/**
	 * 		销毁匹配器的函数
	 *
	 */
	public abstract void clear();

	/**
	 * 		多关键词扫描函数
	 * @param text
	 * 			待扫描的目标文本
	 * @param offset
	 * 			从text中开始扫描的位置
	 * @param scanlen
	 * 			从offset开始扫描的长度
	 * @return	扫描完毕返回1；否则返回负值
	 */
	public abstract int search(byte text[], int offset, int scanlen, Object obj);

	/**
	 * 		每次关键词命中时的处理函数，由search()调用，应用程序必须重写这个函数
	 * @param iPattern
	 * 			匹配的关键词ID，initialize[offset]的ID为0，依次递增
	 * @param iPos
	 * 			匹配的关键词位置，text[offset]的位置为0，依次递增
	 * @return
	 * 			如果返回负值，search()将马上终止后续文本扫描，并返回这个负值
	 */
	public abstract int hit_report(int iPattern, int iPos, Object obj);
}
